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

package tech.qiantong.qdata.mdm.merge.service.detect.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectScopeRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectStartReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskCreatorVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMemberMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskInstanceMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterMapper;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectContextBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectScopeBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectExecuteService;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectScopeService;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectService;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectScheduleService;
import tech.qiantong.qdata.mdm.merge.support.MergeAudit;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.MergeResultFactory;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.merge.utils.detect.MergeDetectConfigUtils;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * 重复检测任务管理服务，负责任务维护、行锁控制及候选组转入后续治理。
 *
 * @author chentangwen
 */
@Service
public class MergeDetectServiceImpl
        extends ServiceImpl<MergeDuplicateTaskMapper, MergeDuplicateTaskDO> implements IMergeDetectService {

    @Resource
    private MergeDuplicateTaskMapper mergeDuplicateTaskMapper;

    @Resource
    private MergeDuplicateTaskInstanceMapper mergeDuplicateTaskInstanceMapper;

    @Resource
    private MergeCandidateGroupMapper mergeCandidateGroupMapper;

    @Resource
    private MergeCandidateGroupMemberMapper mergeCandidateGroupMemberMapper;

    @Resource
    private IMergeSourceTableService sourceTableService;


    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Resource
    private MergeResultFactory mergeResultFactory;

    @Resource
    private MergePendingMasterMapper mergePendingMasterMapper;

    @Resource
    private IMergeDetectScopeService scopeService;

    @Resource
    private IMergeDetectExecuteService executeService;

    @Resource
    private IMergeDetectScheduleService scheduleService;

    /** 分页查询重复检测任务。 */
    @Override
    public PageResult<MergeDuplicateTaskDO> pageTasks(MergeTaskPageReqVO reqVO) {
        return mergeDuplicateTaskMapper.selectPage(reqVO);
    }

    /** 分页查询重复检测实例，并按需要补充来源快照。 */
    @Override
    public PageResult<MergeDuplicateTaskInstanceDO> pageInstances(MergeTaskPageReqVO reqVO) {
        PageResult<MergeDuplicateTaskInstanceDO> result = mergeDuplicateTaskInstanceMapper.selectPage(reqVO);
        if (Boolean.TRUE.equals(reqVO.getIncludeSnapshot())) {
            fillSnapshotIds(result.getRows());
        }
        return result;
    }

    /** 历史快照缺少展示 ID 时补充序号，仅用于展示，不作为业务主键。 */
    private void fillSnapshotIds(List<MergeDuplicateTaskInstanceDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (MergeDuplicateTaskInstanceDO row : rows) {
            row.setSourceDataSnapshot(fillSnapshotIds(row.getSourceDataSnapshot()));
            row.setMatchRuleSnapshot(fillSnapshotIds(row.getMatchRuleSnapshot()));
        }
    }

    /** 为历史数组快照补充展示序号，不改变已有业务编号。 */
    private String fillSnapshotIds(String snapshot) {
        JsonNode root = MergeJson.read(snapshot);
        if (!root.isArray()) {
            return snapshot;
        }
        boolean changed = false;
        for (int i = 0; i < root.size(); i++) {
            JsonNode item = root.get(i);
            if (item != null && item.isObject() && (!item.has("id") || item.get("id").isNull())) {
                ((ObjectNode) item).put("id", i + 1L);
                changed = true;
            }
        }
        return changed ? MergeJson.toJson(root) : snapshot;
    }

    /** 补充任务列表统计和每条任务创建账号的电话。 */
    @Override
    public void fillTaskMetrics(List<MergeTaskRespVO> rows) {
        scopeService.fillTaskMetrics(rows);
        List<Long> creatorIds = rows.stream().map(MergeTaskRespVO::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (creatorIds.isEmpty()) {
            return;
        }
        Map<Long, String> phones = mergeDuplicateTaskMapper.selectCreators(creatorIds).stream()
                .filter(creator -> creator.getPhonenumber() != null)
                .collect(Collectors.toMap(MergeTaskCreatorVO::getUserId, MergeTaskCreatorVO::getPhonenumber,
                        (first, second) -> first));
        rows.forEach(row -> row.setCreatePhoneNumber(phones.get(row.getCreatorId())));
    }

    /** 分页查询重复检测产生的候选组。 */
    @Override
    public PageResult<MergeCandidateGroupDO> pageGroups(MergeGroupPageReqVO reqVO) {
        return mergeCandidateGroupMapper.selectPage(reqVO);
    }

    /** 保存重复检测任务配置，不立即执行。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(MergeDetectStartReqVO reqVO, Long operatorId, String operatorName) {
        MergeDuplicateTaskDO task = createTask(reqVO, operatorId, operatorName);
        scheduleService.syncAfterCommit(task.getId());
        return task.getId();
    }

    /** 更新非运行中任务的范围，不影响已有实例的固定快照。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateTask(Long taskId, MergeDetectStartReqVO reqVO, Long operatorId, String operatorName) {
        MergeDuplicateTaskDO current = editableTask(taskId, "修改");
        MergeDetectScopeBO scope = scopeService.prepareDetection(reqVO).getScope();
        String triggerType = triggerType(reqVO);
        // 审计字段通过实体映射写入，兼容公共实体 updaterId 字段与 getUpdatorId 方法的命名差异。
        MergeDuplicateTaskDO audit = new MergeDuplicateTaskDO();
        MergeAudit.touch(audit, operatorId, operatorName);
        int changed = mergeDuplicateTaskMapper.update(audit, new LambdaUpdateWrapper<MergeDuplicateTaskDO>()
                .eq(MergeDuplicateTaskDO::getId, taskId)
                .set(reqVO.getTaskName() != null, MergeDuplicateTaskDO::getTaskName, reqVO.getTaskName())
                .set(reqVO.getResponsibleUserId() != null, MergeDuplicateTaskDO::getResponsibleUserId,
                        reqVO.getResponsibleUserId())
                .set(reqVO.getEnableStatus() != null, MergeDuplicateTaskDO::getEnableStatus,
                        normalizeEnableStatus(reqVO.getEnableStatus()))
                .set(reqVO.getRemark() != null, MergeDuplicateTaskDO::getRemark, reqVO.getRemark())
                .set(MergeDuplicateTaskDO::getTriggerType, triggerType)
                .set(MergeDuplicateTaskDO::getCronExpression, cronExpression(reqVO, triggerType))
                .set(MergeDuplicateTaskDO::getSchedulerStatus,
                        schedulerStatus(current, reqVO, triggerType))
                .set(MergeDuplicateTaskDO::getModelId, reqVO.getModelId())
                .set(MergeDuplicateTaskDO::getScopeType,
                        scope.isCustom() ? MergeCodes.SCOPE_CUSTOM : MergeCodes.SCOPE_MODEL)
                .set(MergeDuplicateTaskDO::getSourceSystemIds,
                        scope.isCustom() ? MergeJson.toJson(scope.getSystemIds()) : null)
                .set(MergeDuplicateTaskDO::getReceiveStartTime, scope.isCustom() ? scope.getStart() : null)
                .set(MergeDuplicateTaskDO::getReceiveEndTime, scope.isCustom() ? scope.getEnd() : null));
        scheduleService.syncAfterCommit(taskId);
        return changed;
    }

    /** 保留独立调度开关；切换为人工执行或停用任务时关闭调度。 */
    private String schedulerStatus(MergeDuplicateTaskDO current, MergeDetectStartReqVO reqVO, String triggerType) {
        String enabled = reqVO.getEnableStatus() == null ? current.getEnableStatus() : reqVO.getEnableStatus();
        if (!"ENABLED".equals(enabled) || !MergeCodes.TRIGGER_SCHEDULED.equals(triggerType)) {
            return MergeCodes.NO;
        }
        return MergeCodes.TRIGGER_SCHEDULED.equals(current.getTriggerType())
                ? current.getSchedulerStatus() : MergeCodes.YES;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer changeStatus(Long taskId, String status, Long operatorId, String operatorName) {
        validateSwitchStatus(status);
        editableTask(taskId, "切换状态");
        MergeDuplicateTaskDO update = new MergeDuplicateTaskDO();
        update.setId(taskId);
        update.setEnableStatus(MergeCodes.YES.equals(status) ? "ENABLED" : "DISABLED");
        if (MergeCodes.NO.equals(status)) {
            update.setSchedulerStatus(MergeCodes.NO);
        }
        MergeAudit.touch(update, operatorId, operatorName);
        int changed = mergeDuplicateTaskMapper.updateById(update);
        scheduleService.syncAfterCommit(taskId);
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer changeScheduleStatus(Long taskId, String status, Long operatorId, String operatorName) {
        validateSwitchStatus(status);
        MergeDuplicateTaskDO task = editableTask(taskId, "切换调度状态");
        if (MergeCodes.YES.equals(status)) {
            if (!"ENABLED".equals(task.getEnableStatus())
                    || !MergeCodes.TRIGGER_SCHEDULED.equals(task.getTriggerType())) {
                throw new ServiceException("仅已启用的定时任务可以启用调度");
            }
            MergeDetectScheduleServiceImpl.validateCron(task.getCronExpression());
        }
        MergeDuplicateTaskDO update = new MergeDuplicateTaskDO();
        update.setId(taskId);
        update.setSchedulerStatus(status);
        MergeAudit.touch(update, operatorId, operatorName);
        int changed = mergeDuplicateTaskMapper.updateById(update);
        scheduleService.syncAfterCommit(taskId);
        return changed;
    }

    private void validateSwitchStatus(String status) {
        if (!MergeCodes.YES.equals(status) && !MergeCodes.NO.equals(status)) {
            throw new ServiceException("状态只能为0或1");
        }
    }

    /** 删除非运行中的重复检测任务。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteTask(Long taskId) {
        editableTask(taskId, "删除");
        int changed = mergeDuplicateTaskMapper.deleteById(taskId);
        scheduleService.syncAfterCommit(taskId);
        return changed;
    }

    /** 批量删除重复检测任务。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteTasks(List<Long> taskIds) {
        List<Long> ids = MergeDetectConfigUtils.distinctIds(taskIds);
        if (ids.isEmpty()) {
            return 0;
        }
        // 同一批任务按固定顺序取锁，避免反向编号请求交叉持锁。
        ids.sort(Long::compareTo);
        for (Long taskId : ids) {
            editableTask(taskId, "删除");
        }
        int deleted = 0;
        for (Long taskId : ids) {
            deleted += mergeDuplicateTaskMapper.deleteById(taskId);
            scheduleService.syncAfterCommit(taskId);
        }
        return deleted;
    }

    /**
     * 创建并执行任务，使用同一份固定数据避免立即启动时重复加载范围。
     *
     * @param reqVO 检测配置
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 运行实例编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(MergeDetectStartReqVO reqVO, Long operatorId, String operatorName) {
        MergeDetectContextBO context = scopeService.prepareDetection(reqVO);
        MergeDuplicateTaskDO task = createTask(reqVO, context.getScope(), operatorId, operatorName);
        scheduleService.syncAfterCommit(task.getId());
        // 新任务在当前事务内创建，其他事务不可见，无需再次读取和锁定。
        return executeService.executePrepared(task, context, operatorId, operatorName);
    }

    /** 执行重复检测任务，生成候选组和匹配明细。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long execute(Long taskId, Long operatorId, String operatorName) {
        return executeTask(taskId, operatorId, operatorName, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long executeScheduled(Long taskId, Long operatorId, String operatorName) {
        return executeTask(taskId, operatorId, operatorName, true);
    }

    private Long executeTask(Long taskId, Long operatorId, String operatorName, boolean scheduled) {
        // 同一任务先加行锁再校验状态，使并发请求串行处理。
        MergeDuplicateTaskDO task = mergeDuplicateTaskMapper.selectForUpdate(taskId);
        if (task == null || !MergeCodes.YES.equals(task.getValidFlag())) {
            throw new ServiceException("重复检测任务不存在或已失效");
        }
        // 已触发的 Quartz 作业也须读取持久化开关，避免暂停后继续执行。
        if (scheduled && (!"ENABLED".equals(task.getEnableStatus())
                || !MergeCodes.YES.equals(task.getSchedulerStatus())
                || !MergeCodes.TRIGGER_SCHEDULED.equals(task.getTriggerType()))) {
            return null;
        }
        if (!"ENABLED".equals(task.getEnableStatus())) {
            throw new ServiceException("任务已停用，不能执行");
        }
        if (MergeCodes.TASK_RUNNING.equals(task.getStatus())) {
            throw new ServiceException("重复检测任务正在执行，请勿重复提交");
        }
        // 执行服务固定模型、规则、来源版本和范围，保证本次检测可追溯。
        return executeService.execute(task, operatorId, operatorName);
    }

    /** 校验范围后保存任务配置。 */
    private MergeDuplicateTaskDO createTask(MergeDetectStartReqVO reqVO, Long operatorId, String operatorName) {
        MergeDetectScopeBO scope = scopeService.prepareDetection(reqVO).getScope();
        return createTask(reqVO, scope, operatorId, operatorName);
    }

    /** 持久化已校验的任务范围，初始状态为待执行。 */
    private MergeDuplicateTaskDO createTask(MergeDetectStartReqVO reqVO, MergeDetectScopeBO scope,
            Long operatorId, String operatorName) {
        MergeDuplicateTaskDO task = new MergeDuplicateTaskDO();
        task.setTaskNo(MergeAudit.temporaryTaskNo());
        task.setTaskName(reqVO.getTaskName() == null || reqVO.getTaskName().trim().isEmpty()
                ? "重复检测任务-" + reqVO.getModelId() : reqVO.getTaskName().trim());
        task.setResponsibleUserId(reqVO.getResponsibleUserId() == null ? operatorId : reqVO.getResponsibleUserId());
        task.setEnableStatus(normalizeEnableStatus(reqVO.getEnableStatus()));
        task.setRemark(reqVO.getRemark());
        String triggerType = triggerType(reqVO);
        task.setTriggerType(triggerType);
        task.setSchedulerStatus("ENABLED".equals(task.getEnableStatus())
                && MergeCodes.TRIGGER_SCHEDULED.equals(triggerType) ? MergeCodes.YES : MergeCodes.NO);
        task.setCronExpression(cronExpression(reqVO, triggerType));
        task.setModelId(reqVO.getModelId());
        task.setScopeType(scope.isCustom() ? MergeCodes.SCOPE_CUSTOM : MergeCodes.SCOPE_MODEL);
        if (scope.isCustom()) {
            task.setSourceSystemIds(MergeJson.toJson(scope.getSystemIds()));
            task.setReceiveStartTime(scope.getStart());
            task.setReceiveEndTime(scope.getEnd());
        }
        task.setStatus(MergeCodes.TASK_PENDING);
        task.setValidFlag(MergeCodes.YES);
        task.setDelFlag(MergeCodes.NO);
        MergeAudit.fill(task, operatorId, operatorName);
        mergeDuplicateTaskMapper.insert(task);
        task.setTaskNo(MergeAudit.taskNo(task.getCreateTime(), task.getId()));
        int updated = mergeDuplicateTaskMapper.update(null, new LambdaUpdateWrapper<MergeDuplicateTaskDO>()
                .eq(MergeDuplicateTaskDO::getId, task.getId())
                .set(MergeDuplicateTaskDO::getTaskNo, task.getTaskNo()));
        if (updated != 1) {
            throw new ServiceException("生成重复检测任务编号失败");
        }
        return task;
    }

    private String triggerType(MergeDetectStartReqVO reqVO) {
        String triggerType = reqVO.getTriggerType();
        if (triggerType == null || triggerType.trim().isEmpty()) {
            triggerType = MergeCodes.TRIGGER_MANUAL;
        }
        if (!MergeCodes.TRIGGER_MANUAL.equals(triggerType)
                && !MergeCodes.TRIGGER_SCHEDULED.equals(triggerType)) {
            throw new ServiceException("执行方式不正确");
        }
        if (MergeCodes.TRIGGER_SCHEDULED.equals(triggerType)) {
            MergeDetectScheduleServiceImpl.validateCron(reqVO.getCronExpression());
        }
        return triggerType;
    }

    private String cronExpression(MergeDetectStartReqVO reqVO, String triggerType) {
        return MergeCodes.TRIGGER_SCHEDULED.equals(triggerType) ? reqVO.getCronExpression().trim() : null;
    }

    private String normalizeEnableStatus(String enableStatus) {
        if (enableStatus == null || enableStatus.trim().isEmpty()) {
            return "ENABLED";
        }
        if (!"ENABLED".equals(enableStatus) && !"DISABLED".equals(enableStatus)) {
            throw new ServiceException("任务状态不正确");
        }
        return enableStatus;
    }

    /** 在调用方事务内锁定任务，禁止修改或删除运行中的任务。 */
    private MergeDuplicateTaskDO editableTask(Long taskId, String action) {
        MergeDuplicateTaskDO task = mergeDuplicateTaskMapper.selectForUpdate(taskId);
        if (task == null || !MergeCodes.YES.equals(task.getValidFlag())) {
            throw new ServiceException("重复检测任务不存在或已失效");
        }
        if (MergeCodes.TASK_RUNNING.equals(task.getStatus())) {
            throw new ServiceException("重复检测任务正在执行，不能" + action);
        }
        return task;
    }

    /** 重新检测模型下尚未进入结果处理的候选数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long redetect(Long modelId, Long operatorId, String operatorName) {
        List<MergeCandidateGroupDO> groups =
                mergeCandidateGroupMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupDO>()
                        .eq(MergeCandidateGroupDO::getModelId, modelId)
                        .eq(MergeCandidateGroupDO::getValidFlag, MergeCodes.YES)
                        .in(MergeCandidateGroupDO::getStatus, Arrays.asList(MergeCodes.GROUP_STATUS_WAIT,
                        MergeCodes.GROUP_STATUS_REVIEW, MergeCodes.GROUP_STATUS_SUSPEND)));
        if (!groups.isEmpty()) {
            // 重新检测前必须确认旧候选组尚未进入待提交结果，避免覆盖人工处理中的数据。
            List<Long> groupIds = new ArrayList<>();
            for (MergeCandidateGroupDO group : groups) {
                groupIds.add(group.getId());
            }
            Long pendingCount = mergePendingMasterMapper.selectCount(new LambdaQueryWrapper<MergePendingMasterDO>()
                    .in(MergePendingMasterDO::getObjectGroupId, groupIds)
                    .eq(MergePendingMasterDO::getValidFlag, MergeCodes.YES));
            if (pendingCount != null && pendingCount.longValue() > 0L) {
                throw new ServiceException("候选组已进入结果处理，不能重新检测");
            }
            mergeCandidateGroupMapper.update(null, new LambdaUpdateWrapper<MergeCandidateGroupDO>()
                    .set(MergeCandidateGroupDO::getValidFlag, MergeCodes.NO)
                    .in(MergeCandidateGroupDO::getId, groupIds)
                    .eq(MergeCandidateGroupDO::getValidFlag, MergeCodes.YES));
            releaseTargets(modelId, groupIds);
        }
        MergeDetectStartReqVO reqVO = new MergeDetectStartReqVO();
        reqVO.setModelId(modelId);
        reqVO.setScopeType(MergeCodes.SCOPE_MODEL);
        return start(reqVO, operatorId, operatorName);
    }

    /** 将候选组转入匹配审核或单条结果处理。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer transfer(List<Long> groupIds, Long operatorId, String operatorName) {
        // 按候选组成员数分流：单成员直接生成结果，多成员进入匹配审核。
        List<Long> ids = MergeDetectConfigUtils.distinctIds(groupIds);
        if (ids.isEmpty()) {
            throw new ServiceException("请选择需要转入的候选对象组");
        }
        List<MergeCandidateGroupDO> groups =
                mergeCandidateGroupMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupDO>()
                        .in(MergeCandidateGroupDO::getId, ids)
                        .eq(MergeCandidateGroupDO::getValidFlag, MergeCodes.YES));
        if (groups.size() != ids.size()) {
            throw new ServiceException("候选对象组不存在或已失效，不能转入后续处理");
        }
        Date now = new Date();
        int count = 0;
        // 单条组直接生成待确认结果，多条组进入匹配审核；两者都只能转入一次。
        for (MergeCandidateGroupDO group : groups) {
            if (!canTransfer(group)) {
                throw new ServiceException("候选对象组已转入后续处理，不能重复转入");
            }
            if (MergeCodes.GROUP_TYPE_SINGLE.equals(group.getType())) {
                createSingleResult(group, operatorId, operatorName);
                group.setStatus(MergeCodes.GROUP_STATUS_SINGLE);
            } else if (MergeCodes.GROUP_STATUS_WAIT.equals(group.getStatus())) {
                group.setStatus(MergeCodes.GROUP_STATUS_REVIEW);
            }
            group.setTransferTime(now);
            MergeAudit.touch(group, operatorId, operatorName);
            mergeCandidateGroupMapper.updateById(group);
            count++;
        }
        return count;
    }

    /** 待转入组可转入；兼容审核组仅在尚无转入时间时允许一次转入。 */
    private boolean canTransfer(MergeCandidateGroupDO group) {
        if (group == null || !MergeCodes.YES.equals(group.getValidFlag())) {
            return false;
        }
        if (MergeCodes.GROUP_STATUS_WAIT.equals(group.getStatus())) {
            return true;
        }
        return MergeCodes.GROUP_STATUS_REVIEW.equals(group.getStatus())
                && group.getTransferTime() == null
                && !MergeCodes.GROUP_TYPE_SINGLE.equals(group.getType());
    }

    /** 单条组使用成员固定版本生成结果，不采用当前最新版本或生存规则。 */
    private void createSingleResult(MergeCandidateGroupDO group, Long operatorId, String operatorName) {
        Long pendingCount = mergePendingMasterMapper.selectCount(new LambdaQueryWrapper<MergePendingMasterDO>()
                .eq(MergePendingMasterDO::getObjectGroupId, group.getId())
                .eq(MergePendingMasterDO::getValidFlag, MergeCodes.YES));
        if (pendingCount != null && pendingCount.longValue() > 0L) {
            return;
        }
        List<MergeCandidateGroupMemberDO> members =
                mergeCandidateGroupMemberMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                        .eq(MergeCandidateGroupMemberDO::getGroupId, group.getId())
                        .eq(MergeCandidateGroupMemberDO::getMemberRole, MergeCodes.MEMBER_ROLE_TARGET)
                        .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES)
                        .orderByAsc(MergeCandidateGroupMemberDO::getId));
        if (members.isEmpty()) {
            throw new ServiceException("候选对象组没有可转入的来源数据");
        }
        List<Long> sourceIds = new ArrayList<>();
        List<Long> versionIds = new ArrayList<>();
        for (MergeCandidateGroupMemberDO member : members) {
            if (member.getSourceRecordId() != null) {
                sourceIds.add(member.getSourceRecordId());
            }
            if (member.getSourceVersionId() != null) {
                versionIds.add(member.getSourceVersionId());
            }
        }
        if (sourceIds.isEmpty() || versionIds.isEmpty()) {
            throw new ServiceException("候选对象组缺少来源快照，不能转入单条结果确认");
        }
        List<MergeSourceRecordBO> sources = sourceTableService.load(group.getModelId(), sourceIds);
        Map<Long, MergeSourceRecordBO> sourceMap = new HashMap<>();
        for (MergeSourceRecordBO source : sources) {
            sourceMap.put(source.getId(), source);
        }
        Map<Long, IngestSourceDO> systems = scopeService.loadSystems(sources);
        ModelBaseDO model = scopeService.requirePublishedModel(group.getModelId());
        List<ModelColumnDO> columns = modelColumnMapper.selectList(new LambdaQueryWrapper<ModelColumnDO>()
                .eq(ModelColumnDO::getModelId, group.getModelId()));
        List<SourceSnapshot> snapshots = new ArrayList<>();
        for (MergeCandidateGroupMemberDO member : members) {
            MergeSourceRecordBO source = sourceMap.get(member.getSourceVersionId());
            IngestSourceDO system = source == null ? null : systems.get(source.getSourceSystemId());
            SourceSnapshot snapshot = SourceSnapshot.from(source, system, columns, true);
            if (snapshot == null) {
                throw new ServiceException("候选对象组来源快照无法解析，不能转入单条结果确认");
            }
            snapshots.add(snapshot);
        }
        mergeResultFactory.create(group.getId(), group.getModelId(), model.getVersionId(), MergeCodes.PROCESS_SINGLE,
                snapshots, columns, new ArrayList<>(), operatorId, operatorName);
    }

    /** 释放失效组的目标来源，其他有效组仍占用的来源保持原状态。 */
    private void releaseTargets(Long modelId, List<Long> groupIds) {
        List<MergeCandidateGroupMemberDO> members =
                mergeCandidateGroupMemberMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                        .in(MergeCandidateGroupMemberDO::getGroupId, groupIds)
                        .eq(MergeCandidateGroupMemberDO::getMemberRole, MergeCodes.MEMBER_ROLE_TARGET)
                        .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES));
        List<Long> sourceIds = new ArrayList<>();
        for (MergeCandidateGroupMemberDO member : members) {
            sourceIds.add(member.getSourceRecordId());
        }
        Set<Long> stillOccupied = scopeService.occupiedSourceIds(modelId, sourceIds);
        List<Long> release = new ArrayList<>();
        for (Long sourceId : sourceIds) {
            if (!stillOccupied.contains(sourceId)) {
                release.add(sourceId);
            }
        }
        if (release.isEmpty()) {
            return;
        }
        sourceTableService.transition(modelId, release,
                MergeCodes.GOVERNANCE_PROCESSING, MergeCodes.GOVERNANCE_PENDING);
    }

    /** 预览可参与检测的目标来源，不改变治理状态。 */
    @Override
    public MergeDetectScopeRespVO previewScope(MergeDetectStartReqVO reqVO) {
        return scopeService.previewScope(reqVO);
    }
}
