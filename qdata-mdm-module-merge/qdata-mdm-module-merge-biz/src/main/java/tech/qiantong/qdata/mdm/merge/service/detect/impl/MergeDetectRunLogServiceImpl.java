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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.merge.service.detect.impl;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectRunLogRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeInstanceRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskInstanceMapper;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectRunLogBO;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectRunLogService;

import javax.annotation.Resource;
import java.util.*;

/**
 * Redis 保存实时日志，结束时把完整事件随原运行实例一起提交。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class MergeDetectRunLogServiceImpl implements IMergeDetectRunLogService {
    private static final String PREFIX = "merge:{detect-run-logs}:";
    private static final int BATCH_SIZE = 200;
    private static final int CACHE_SECONDS = 86400;

    /**
     * 原子生成实例内序号、追加日志及更新进度，兼容 Redis 3。
     */
    private static final DefaultRedisScript<Long> APPEND = new DefaultRedisScript<>(
            "local event = cjson.decode(ARGV[1])\n"
                    + "local seq = redis.call('incr', KEYS[1])\n"
                    + "event.eventSeq = seq\n"
                    + "local value = cjson.encode(event)\n"
                    + "redis.call('zadd', KEYS[2], seq, value)\n"
                    + "redis.call('set', KEYS[3], value)\n"
                    + "for i = 1, 3 do redis.call('expire', KEYS[i], " + CACHE_SECONDS + ") end\n"
                    + "return seq", Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MergeDuplicateTaskInstanceMapper instanceMapper;

    private List<String> keys(Long instanceId) {
        String base = PREFIX + instanceId;
        return Arrays.asList(base + ":seq", base + ":events", base + ":snapshot");
    }

    @Override
    public void publish(MergeDuplicateTaskInstanceDO instance, String level, String stage, String message) {
        try {
            Long seq = stringRedisTemplate.execute(APPEND, keys(instance.getId()),
                    JSON.toJSONString(event(instance, level, stage, message)));
            if (seq == null) throw new IllegalStateException("Redis 未返回日志序号");
        } catch (Exception ex) {
            log.error("暂存重复检测过程日志失败，instanceId={}, stage={}", instance.getId(), stage, ex);
        }
    }

    @Override
    public void archive(MergeDuplicateTaskInstanceDO instance, String level, String stage, String message) {
        List<MergeDetectRunLogBO> events = new ArrayList<>();
        try {
            Set<String> cached = stringRedisTemplate.opsForZSet().range(keys(instance.getId()).get(1), 0, -1);
            if (cached != null) {
                for (String value : cached) events.add(JSON.parseObject(value, MergeDetectRunLogBO.class));
            }
        } catch (Exception ex) {
            log.error("读取重复检测待归档日志失败，仍保存实例终态，instanceId={}", instance.getId(), ex);
            MergeDetectRunLogBO warning = event(instance, "WARN", "LOG_ARCHIVE", "部分过程日志读取失败，保留可用日志及最终执行结果");
            warning.setEventSeq(events.isEmpty() ? 1L : events.get(events.size() - 1).getEventSeq() + 1);
            events.add(warning);
        }
        MergeDetectRunLogBO terminal = event(instance, level, stage, message);
        terminal.setLogTime(instance.getEndTime() == null ? new Date() : instance.getEndTime());
        terminal.setEventSeq(events.isEmpty() ? 1L : events.get(events.size() - 1).getEventSeq() + 1);
        events.add(terminal);
        instance.setLogContent(JSON.toJSONString(events));
    }

    @Override
    public void clear(Long instanceId) {
        try {
            stringRedisTemplate.delete(keys(instanceId));
        } catch (Exception ex) {
            log.warn("清理重复检测实时日志失败，缓存将自动过期，instanceId={}", instanceId, ex);
        }
    }

    @Override
    public MergeDetectRunLogRespVO read(Long taskId, Long instanceId, long afterSeq) {
        if (afterSeq < 0) throw new ServiceException("日志游标不正确", HttpStatus.WARN);
        MergeDuplicateTaskInstanceDO stored = instanceMapper.selectProgress(instanceId);
        if (stored == null || !Objects.equals(taskId, stored.getTaskId())) {
            throw new ServiceException("运行记录不存在", HttpStatus.WARN);
        }
        MergeInstanceRespVO run = BeanUtils.toBean(stored, MergeInstanceRespVO.class);
        List<MergeDetectRunLogBO> rows = new ArrayList<>();
        long lastSeq = 0L;
        if (stored.getEndTime() != null) {
            // 终态及完整日志同事务落库，查询不再依赖 Redis，也不覆盖已回滚的计数。
            List<MergeDetectRunLogBO> archived = stored.getLogContent() == null
                    ? new ArrayList<>() : JSON.parseArray(stored.getLogContent(), MergeDetectRunLogBO.class);
            for (MergeDetectRunLogBO row : archived) {
                lastSeq = Math.max(lastSeq, row.getEventSeq());
                if (row.getEventSeq() > afterSeq && rows.size() <= BATCH_SIZE) rows.add(row);
            }
        } else {
            List<String> redisKeys = keys(instanceId);
            String snapshot = stringRedisTemplate.opsForValue().get(redisKeys.get(2));
            if (snapshot != null) {
                MergeDetectRunLogBO latest = JSON.parseObject(snapshot, MergeDetectRunLogBO.class);
                lastSeq = latest.getEventSeq();
                run.setSourceCount(latest.getSourceCount());
                run.setHistorySourceCount(latest.getHistorySourceCount());
                run.setGroupCount(latest.getGroupCount());
                run.setSingleCount(latest.getSingleCount());
                Set<String> cached = stringRedisTemplate.opsForZSet().rangeByScore(redisKeys.get(1),
                        afterSeq + 1D, lastSeq, 0, BATCH_SIZE + 1);
                if (cached != null) {
                    for (String value : cached) rows.add(JSON.parseObject(value, MergeDetectRunLogBO.class));
                }
            } else {
                // 可能刚从运行态提交为终态并清理缓存，再读一次避免丢失最后一批日志。
                MergeDuplicateTaskInstanceDO latest = instanceMapper.selectProgress(instanceId);
                if (latest != null && latest.getEndTime() != null) return read(taskId, instanceId, afterSeq);
            }
        }
        MergeDetectRunLogRespVO result = new MergeDetectRunLogRespVO();
        result.setRun(run);
        result.setHasMore(rows.size() > BATCH_SIZE);
        if (rows.size() > BATCH_SIZE) rows = new ArrayList<>(rows.subList(0, BATCH_SIZE));
        result.setLogs(rows);
        result.setLastSeq(lastSeq);
        result.setNextSeq(rows.isEmpty() ? afterSeq : rows.get(rows.size() - 1).getEventSeq());
        return result;
    }

    private MergeDetectRunLogBO event(MergeDuplicateTaskInstanceDO instance, String level, String stage, String message) {
        MergeDetectRunLogBO row = new MergeDetectRunLogBO();
        row.setLogLevel(level);
        row.setStage(stage);
        row.setMessage(message == null ? "" : message.substring(0, Math.min(2000, message.length())));
        row.setLogTime(new Date());
        row.setSourceCount(instance.getSourceCount());
        row.setHistorySourceCount(instance.getHistorySourceCount());
        row.setGroupCount(instance.getGroupCount());
        row.setSingleCount(instance.getSingleCount());
        return row;
    }
}
