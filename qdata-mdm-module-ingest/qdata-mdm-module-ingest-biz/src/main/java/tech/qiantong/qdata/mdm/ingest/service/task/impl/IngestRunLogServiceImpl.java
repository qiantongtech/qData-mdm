/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This file is part of qData-mdm Master Data Platform (Open Source Edition).
 *
 * qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
 * You may use qData-mdm for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData-mdm logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData-mdm as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.service.task.impl;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestRunLogRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceLogDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.ingest.IngestTaskInstanceLogMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.ingest.IngestTaskInstanceMapper;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestRunLogService;
import tech.qiantong.qdata.mdm.ingest.service.task.dto.IngestRunEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Resource;

/**
 * 未确认入库的事件不设置过期时间；数据库提交后才确认删除，支持失败重试。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class IngestRunLogServiceImpl implements IIngestRunLogService {

    /**
     * 固定哈希标签保证同一 Lua 调用涉及的键位于同一 Redis 集群槽。
     */
    static final String PREFIX = "ingest:{run-logs}:";
    /**
     * 存在待归档事件的运行实例 ID 集合。
     */
    static final String PENDING = PREFIX + "pending";
    /**
     * 一次归档或一次增量读取返回的最多事件数。
     */
    static final int BATCH_SIZE = 200;
    /**
     * 单实例未归档普通事件的积压上限，终态事件不受此限制。
     */
    private static final int MAX_PENDING_EVENTS = 10000;
    /**
     * 单条日志正文的最大字符数。
     */
    private static final int MAX_MESSAGE_LENGTH = 1024;
    /**
     * 终态事件全部归档后，序号与快照继续保留的秒数。
     */
    private static final int COMPLETED_CACHE_SECONDS = 86400;

    /**
     * 原子追加脚本：KEYS 依次对应序号、快照、事件集合、待归档实例集合。
     * ARGV 依次为事件 JSON、实例 ID、终态标志；返回正序号、0（已有终态）或 -1（积压）。
     * 未归档事件不设置过期时间，避免数据库不可用时被自动淘汰。
     */
    private static final DefaultRedisScript<Long> APPEND = new DefaultRedisScript<>(
            "if redis.call('hget', KEYS[2], 'finished') == '1' then\n"
                    + "  return 0\n"
                    + "end\n"
                    + "if ARGV[3] == '0' and redis.call('zcard', KEYS[3]) >= " + MAX_PENDING_EVENTS + " then\n"
                    + "  return -1\n"
                    + "end\n"
                    + "local seq = redis.call('incr', KEYS[1])\n"
                    + "local event = tostring(seq) .. '|' .. ARGV[1]\n"
                    + "redis.call('hset', KEYS[2], 'event', event, 'finished', ARGV[3])\n"
                    + "redis.call('persist', KEYS[1])\n"
                    + "redis.call('persist', KEYS[2])\n"
                    + "redis.call('zadd', KEYS[3], seq, event)\n"
                    + "redis.call('sadd', KEYS[4], ARGV[2])\n"
                    + "return seq", Long.class);

    /**
     * 归档确认脚本：KEYS 与追加脚本一致，ARGV 为已提交最大序号和实例 ID。
     * 仅移除已提交事件；集合清空时移出待归档集合，并为终态序号和快照设置保留期限。
     * 返回 1 表示本次确认执行完成。
     */
    private static final DefaultRedisScript<Long> ACK = new DefaultRedisScript<>(
            "redis.call('zremrangebyscore', KEYS[3], '-inf', ARGV[1])\n"
                    + "if redis.call('zcard', KEYS[3]) == 0 then\n"
                    + "  redis.call('srem', KEYS[4], ARGV[2])\n"
                    + "  if redis.call('hget', KEYS[2], 'finished') == '1' then\n"
                    + "    redis.call('expire', KEYS[1], " + COMPLETED_CACHE_SECONDS + ")\n"
                    + "    redis.call('expire', KEYS[2], " + COMPLETED_CACHE_SECONDS + ")\n"
                    + "  end\n"
                    + "end\n"
                    + "return 1", Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IngestTaskInstanceMapper instanceMapper;

    @Resource
    private IngestTaskInstanceLogMapper logMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

    /**
     * 构造 Lua 使用的 Redis 键，返回顺序与脚本 KEYS 下标一致。
     *
     * @param runId 运行实例 ID
     * @return 依次为序号、最新快照、待归档事件集合、待归档实例集合
     */
    private List<String> keys(Long runId) {
        String base = PREFIX + runId;
        return Arrays.asList(base + ":seq", base + ":snapshot", base + ":events", PENDING);
    }

    /**
     * 将进度快照和日志作为一个原子事件写入 Redis，不直接写日志表。
     * 终态之后的事件会被忽略；积压达到上限时拒绝普通事件，但允许失败终态入队。
     *
     * @param instance 当前实例及进度，endTime 非空表示终态
     * @param level 日志级别
     * @param stage 事件所属执行阶段
     * @param message 日志正文，超长部分按数据库字段限制截断
     * @throws ServiceException 暂存无结果或待归档事件超过上限
     */
    @Override
    public void publish(IngestTaskInstanceDO instance, String level, String stage, String message) {
        // Redis 缓存丢失或过期后从已归档序号续接，避免序号回退造成去重误丢。
        String sequenceKey = keys(instance.getId()).get(0);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(sequenceKey))) {
            stringRedisTemplate.opsForValue().setIfAbsent(sequenceKey,
                    Long.toString(logMapper.maxSequence(instance.getId())));
        }
        IngestTaskInstanceLogDO record = new IngestTaskInstanceLogDO();
        record.setTaskInstanceId(instance.getId());
        record.setLogLevel(level);
        record.setStage(stage);
        record.setMessage(message == null ? "" : message.substring(0, Math.min(MAX_MESSAGE_LENGTH, message.length())));
        record.setLogTime(new Date());
        IngestRunEvent event = new IngestRunEvent();
        event.setSnapshot(instance);
        event.setLog(record);
        Long seq = stringRedisTemplate.execute(APPEND, keys(instance.getId()), JSON.toJSONString(event),
                instance.getId().toString(), instance.getEndTime() == null ? "0" : "1");
        if (seq == null) {
            throw new ServiceException("执行日志暂存失败");
        }
        if (seq < 0) {
            log.error("接入日志积压超过上限，taskId={}, runId={}, limit={}",
                    instance.getTaskId(), instance.getId(), MAX_PENDING_EVENTS);
            throw new ServiceException("执行日志积压过多，本次运行停止，请检查日志归档服务");
        }
        if (seq == 0) {
            log.debug("忽略已结束实例的后续日志，taskId={}, runId={}, stage={}",
                    instance.getTaskId(), instance.getId(), stage);
            return;
        }
        log.debug("接入日志暂存完成，taskId={}, runId={}, stage={}, seq={}",
                instance.getTaskId(), instance.getId(), stage, seq);
    }

    /**
     * 用 Redis 快照补全尚未结束的数据库实例；已落库终态不被缓存覆盖。
     *
     * @param stored 数据库实例，可为空
     * @return 实时快照；缓存缺失或读取失败时返回传入实例
     */
    @Override
    public IngestTaskInstanceDO latest(IngestTaskInstanceDO stored) {
        if (stored == null || stored.getEndTime() != null) {
            return stored;
        }
        try {
            Object event = stringRedisTemplate.opsForHash().get(keys(stored.getId()).get(1), "event");
            return event == null ? stored : decode(event.toString()).getSnapshot();
        } catch (Exception ex) {
            log.warn("读取实时接入进度失败，runId={}", stored.getId(), ex);
            return stored;
        }
    }

    /**
     * 校验实例归属后，按序号合并 Redis 与数据库中的日志并去重。
     * 每次返回至多一个批次，多读取一条用于判断是否还有后续日志。
     *
     * @param taskId 当前接入任务 ID
     * @param runId 待查询实例 ID
     * @param afterSeq 排他起点，零表示从头读取
     * @return 运行快照、日志、下次游标和本次可见最大序号
     * @throws ServiceException 游标不合法、实例不属于任务或运行中的实时日志不可读
     */
    @Override
    public IngestRunLogRespVO read(Long taskId, Long runId, long afterSeq) {
        if (afterSeq < 0) {
            throw new ServiceException("日志游标不正确");
        }
        IngestTaskInstanceDO stored = instanceMapper.selectById(runId);
        if (stored == null || !Objects.equals(taskId, stored.getTaskId())) {
            throw new ServiceException("运行记录不存在");
        }
        // 先取状态再取日志，避免页面看到终态后停刷，却未拿到对应的结束事件。
        IngestTaskInstanceDO current = latest(stored);
        long lastSeq = logMapper.maxSequence(runId);
        TreeMap<Long, IngestTaskInstanceLogDO> rows = new TreeMap<>();
        // 先读待落库事件，再读数据库，覆盖两次读取之间的归档窗口。
        try {
            Set<String> cached = stringRedisTemplate.opsForZSet().rangeByScore(keys(runId).get(2),
                    afterSeq + 1D, Double.POSITIVE_INFINITY, 0, BATCH_SIZE + 1);
            if (cached != null) {
                for (String value : cached) {
                    IngestRunEvent event = decode(value);
                    rows.put(event.getLog().getEventSeq(), event.getLog());
                }
            }
            Object snapshotEvent = stringRedisTemplate.opsForHash().get(keys(runId).get(1), "event");
            if (snapshotEvent != null) {
                lastSeq = Math.max(lastSeq, decode(snapshotEvent.toString()).getLog().getEventSeq());
            }
        } catch (Exception ex) {
            if (stored.getEndTime() == null) {
                log.error("实时接入日志读取失败，taskId={}, runId={}, afterSeq={}", taskId, runId, afterSeq, ex);
                throw new ServiceException("实时日志读取失败，请稍后重试");
            }
            log.warn("Redis不可用，读取已归档运行日志，runId={}", runId, ex);
        }
        PageHelper.startPage(1, BATCH_SIZE + 1, false);
        try {
            for (IngestTaskInstanceLogDO record : logMapper.selectAfter(runId, afterSeq)) {
                rows.put(record.getEventSeq(), record);
            }
        } finally {
            PageHelper.clearPage();
        }
        IngestRunLogRespVO result = new IngestRunLogRespVO();
        IngestTaskRunRespVO run = BeanUtils.toBean(current, IngestTaskRunRespVO.class);
        run.setDurationMillis(Math.max(0, (current.getEndTime() == null ? System.currentTimeMillis()
                : current.getEndTime().getTime()) - current.getStartTime().getTime()));
        result.setRun(run);
        result.setHasMore(rows.size() > BATCH_SIZE);
        result.setLastSeq(rows.isEmpty() ? lastSeq : Math.max(lastSeq, rows.lastKey()));
        List<IngestTaskInstanceLogDO> logs = new ArrayList<>(rows.values());
        if (logs.size() > BATCH_SIZE) {
            logs = new ArrayList<>(logs.subList(0, BATCH_SIZE));
        }
        result.setLogs(logs);
        result.setNextSeq(logs.isEmpty() ? afterSeq : logs.get(logs.size() - 1).getEventSeq());
        return result;
    }

    /**
     * 扫描待归档实例，每个实例处理一个批次。
     * 单个实例失败时保留 Redis 事件并继续处理其他实例。
     */
    @Override
    public void flush() {
        Set<String> runs = stringRedisTemplate.opsForSet().members(PENDING);
        if (runs == null || runs.isEmpty()) {
            return;
        }
        log.debug("开始归档接入日志，pendingRunCount={}", runs.size());
        for (String id : runs) {
            try {
                flushRun(Long.valueOf(id));
            } catch (Exception ex) {
                log.error("接入日志批量入库失败，保留 Redis 事件待重试，runId={}", id, ex);
            }
        }
    }

    /**
     * 使用实例行锁串行化多节点归档，按已入库最大序号跳过重复事件。
     * 数据库事务提交后才确认 Redis；确认失败时下一轮可安全重试。
     *
     * @param runId 需要归档的实例 ID
     * @throws ServiceException 实例不存在或数据库提交后的 Redis 确认失败
     */
    void flushRun(Long runId) {
        Long acknowledged = new TransactionTemplate(transactionManager).execute(status -> {
            // 数据库行锁串行化多个服务节点的归档；重试按事件序号去重。
            IngestTaskInstanceDO stored = instanceMapper.lockRun(runId);
            if (stored == null) {
                throw new ServiceException("日志所属运行实例不存在");
            }
            Set<String> batch = stringRedisTemplate.opsForZSet().range(keys(runId).get(2), 0, BATCH_SIZE - 1);
            if (batch == null || batch.isEmpty()) {
                return 0L;
            }
            long persisted = logMapper.maxSequence(runId);
            long last = 0;
            IngestTaskInstanceDO snapshot = null;
            for (String value : batch) {
                IngestRunEvent event = decode(value);
                last = event.getLog().getEventSeq();
                if (last > persisted) {
                    logMapper.insert(event.getLog());
                    snapshot = event.getSnapshot();
                }
            }
            // 已入库终态具有优先级，旧批次不能覆盖恢复流程或后续归档产生的最终状态。
            if (snapshot != null && stored.getEndTime() == null) {
                instanceMapper.updateById(snapshot);
            }
            return last;
        });
        if (acknowledged != null && acknowledged > 0) {
            Long confirmed = stringRedisTemplate.execute(ACK, keys(runId), acknowledged.toString(), runId.toString());
            if (!Long.valueOf(1L).equals(confirmed)) {
                throw new ServiceException("日志已入库但 Redis 确认失败，等待幂等重试");
            }
            log.debug("接入日志归档确认完成，runId={}, acknowledgedSeq={}", runId, acknowledged);
        }
    }

    /**
     * 解析“序号|事件 JSON”，把独立序号补回日志对象。
     * Lua 仅拼接 JSON 字符串，避免解析数据库大整数 ID 时损失精度。
     *
     * @param value Redis 中保存的完整事件文本
     * @return 含进度快照和日志序号的事件对象
     */
    static IngestRunEvent decode(String value) {
        int separator = value.indexOf('|');
        IngestRunEvent event = JSON.parseObject(value.substring(separator + 1), IngestRunEvent.class);
        event.getLog().setEventSeq(Long.parseLong(value.substring(0, separator)));
        return event;
    }
}
