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

package tech.qiantong.qdata.mdm.merge.service.manage.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergeFieldAdjustItemVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergeFieldAdjustReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingActionReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingApprovalRecordRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingLogPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterFieldDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterLogDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeQualityLogDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeRecordDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterFieldMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterLogMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergeQualityLogMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergeRecordMapper;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.service.manage.IMergeManageQualityService;
import tech.qiantong.qdata.mdm.merge.service.manage.IMergeManageQueryService;
import tech.qiantong.qdata.mdm.merge.service.manage.IMergeManageService;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceSnapshotService;
import tech.qiantong.qdata.mdm.merge.support.MergeAudit;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.merge.utils.manage.MergeManageFieldUtils;
import tech.qiantong.qdata.mdm.master.change.dto.MasterChangeRequestSubmitReqDTO;
import tech.qiantong.qdata.mdm.master.change.dto.MasterChangeRequestSubmitRespDTO;
import tech.qiantong.qdata.mdm.master.service.change.service.MasterChangeRequestApi;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 合并管理写操作编排，负责状态流转、事务和业务审计。
 *
 * @author chentangwen
 */
@Service
public class MergeManageServiceImpl implements IMergeManageService {

    @Resource
    private MergePendingMasterMapper mergePendingMasterMapper;

    @Resource
    private MergePendingMasterFieldMapper mergePendingMasterFieldMapper;

    @Resource
    private MergePendingMasterLogMapper mergePendingMasterLogMapper;

    @Resource
    private MergeQualityLogMapper mergeQualityLogMapper;

    @Resource
    private MergeRecordMapper mergeRecordMapper;

    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Resource
    private IMergeManageQueryService mergeManageQueryService;

    @Resource
    private IMergeManageQualityService mergeManageQualityService;

    @Resource
    private IMergeSourceSnapshotService mergeSourceSnapshotService;

    @Resource
    private MasterChangeRequestApi masterChangeRequestApi;

    @Resource
    private ModelPublishedApi modelPublishedApi;

    /**
     * 分页查询合并结果及关联展示数据。
     */
    @Override
    public PageResult<MergePendingRespVO> page(MergePendingPageReqVO reqVO) {
        return mergeManageQueryService.page(reqVO);
    }

    /**
     * 查询合并结果详情及固定来源候选值。
     */
    @Override
    public MergePendingDetailRespVO detail(Long pendingId) {
        return mergeManageQueryService.detail(pendingId);
    }

    /**
     * 查询合并结果的审批与流转记录分页。
     *
     * @param pendingId 待合并结果编号
     * @param reqVO 分页参数
     * @return 审批与流转记录分页数据
     */
    @Override
    public PageResult<MergePendingApprovalRecordRespVO> approvalRecords(
            Long pendingId, MergePendingLogPageReqVO reqVO) {
        return mergeManageQueryService.approvalRecords(pendingId, reqVO);
    }

    /**
     * 调整结果字段，递增结果版本并使已有质量结论失效。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(MergeFieldAdjustReqVO reqVO, Long operatorId, String operatorName) {
        // 结果调整会递增版本并使旧质量结论失效，后续提交必须基于新版本重新质检。
        MergePendingMasterDO pending = lockPendingOrThrow(reqVO.getPendingId());
        if (!adjustable(pending.getStatus())) {
            throw new ServiceException("当前结果已进入审批或生效，不能修改");
        }
        List<MergePendingMasterFieldDO> fields = fields(pending.getId());
        Map<Long, MergePendingMasterFieldDO> byId = new HashMap<>();
        for (MergePendingMasterFieldDO field : fields) {
            byId.put(field.getId(), field);
        }
        Map<Long, ModelColumnDO> columns = columns(pending.getModelId());
        MergeRecordDO record = pending.getMergeRecordId() == null ? null
                : mergeRecordMapper.selectById(pending.getMergeRecordId());
        String processType = record == null ? null : record.getProcessType();
        List<SourceSnapshot> snapshots = mergeSourceSnapshotService.loadGroupSnapshots(
                pending.getObjectGroupId(), new ArrayList<>(columns.values()));
        Set<Long> seen = new HashSet<>();
        boolean changed = false;
        boolean requireAdjustReason = requiresAdjustReason(reqVO, pending);
        // 首次处理允许直接选择其他值；重新处理时，偏离系统推荐值必须说明原因。
        for (MergeFieldAdjustItemVO item : reqVO.getFields()) {
            if (!seen.add(item.getFieldId())) {
                throw new ServiceException("字段不能重复调整");
            }
            MergePendingMasterFieldDO field = byId.get(item.getFieldId());
            if (field == null) {
                throw new ServiceException("字段不属于当前待提交结果");
            }
            String finalValue = MergeAudit.text(item.getFinalValue());
            ModelColumnDO column = columns.get(field.getModelColumnId());
            String valueError = MergeManageFieldUtils.valueError(column, finalValue);
            if (valueError != null) {
                throw new ServiceException(column.getName() + "：" + valueError);
            }
            boolean systemRecommended = isSystemRecommended(item, field, finalValue, column);
            boolean sameChoice = systemRecommended
                    ? MergeCodes.NO.equals(field.getManualOverrideFlag())
                    : MergeCodes.YES.equals(field.getManualOverrideFlag())
                            && Objects.equals(field.getValueSourceType(), item.getValueSourceType())
                            && Objects.equals(field.getSourceVersionId(), item.getSourceVersionId());
            if (MergeAudit.same(field.getFinalValue(), finalValue)
                    && MergeAudit.same(field.getAdjustReason(), MergeAudit.text(item.getAdjustReason()))
                    && sameChoice) {
                continue;
            }
            if (!MergeManageFieldUtils.editable(column)) {
                throw new ServiceException("字段不可人工修改：" + (column == null ? field.getFieldPath() : column.getName()));
            }
            applyFieldValue(field, column, item, finalValue, snapshots, processType, requireAdjustReason);
            boolean conflict = MergeCodes.YES.equals(field.getConflictFlag());
            field.setResolvedFlag(!conflict || !finalValue.isEmpty() ? MergeCodes.YES : MergeCodes.NO);
            MergeAudit.touch(field, operatorId, operatorName);
            mergePendingMasterFieldMapper.updateById(field);
            changed = true;
        }
        if (changed) {
            saveAdjustedResult(pending, fields, operatorId, operatorName);
        }
    }

    /**
     * 恢复推荐值或记录人工覆盖；选源必须匹配固定版本，重新处理覆盖推荐值时必须填写原因。
     */
    private void applyFieldValue(MergePendingMasterFieldDO field, ModelColumnDO column,
            MergeFieldAdjustItemVO item, String finalValue,
            List<SourceSnapshot> snapshots, String processType, boolean requireAdjustReason) {
        String recommended = MergeAudit.text(field.getRecommendedValue());
        boolean systemRecommended = isSystemRecommended(item, field, finalValue, column);
        if (systemRecommended) {
            if (!sameFieldValue(column, finalValue, recommended)) {
                throw new ServiceException("系统推荐项与最终值不一致");
            }
            field.setFinalValue(finalValue.isEmpty() ? null : finalValue);
            field.setValueSourceType(systemType(field, processType));
            field.setManualOverrideFlag(MergeCodes.NO);
            field.setAdjustReason(null);
            field.setSourceVersionId(versionOf(snapshots, field.getFieldPath(), recommended, column));
            return;
        }
        if (requireAdjustReason && MergeAudit.text(item.getAdjustReason()).isEmpty()) {
            throw new ServiceException("覆盖默认结果必须填写调整原因");
        }
        if (MergeCodes.VALUE_PICK_SOURCE.equals(item.getValueSourceType())) {
            if (!matchesSource(snapshots, field.getFieldPath(), item.getSourceVersionId(), finalValue)) {
                throw new ServiceException("选择的来源值与来源记录不一致");
            }
            field.setValueSourceType(MergeCodes.VALUE_PICK_SOURCE);
            field.setSourceVersionId(item.getSourceVersionId());
        } else {
            field.setValueSourceType(MergeCodes.VALUE_MANUAL);
            field.setSourceVersionId(null);
        }
        field.setFinalValue(finalValue);
        field.setManualOverrideFlag(MergeCodes.YES);
        field.setAdjustReason(MergeAudit.cut(MergeAudit.text(item.getAdjustReason()), 1000));
    }

    /** 显式选择标记优先；旧调用按最终值是否等于推荐值兼容处理。 */
    private boolean isSystemRecommended(MergeFieldAdjustItemVO item, MergePendingMasterFieldDO field,
            String finalValue, ModelColumnDO column) {
        return item.getSystemRecommended() != null
                ? item.getSystemRecommended()
                : sameFieldValue(column, finalValue, field.getRecommendedValue());
    }

    /** 布尔字段按语义比较数据库值与“是/否”展示值。 */
    private boolean sameFieldValue(ModelColumnDO column, String left, String right) {
        String dataType = column == null || column.getDataType() == null
                ? "" : column.getDataType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!"BOOLEAN".equals(dataType) && !"BOOL".equals(dataType)) {
            return MergeAudit.same(left, right);
        }
        return normalizeBooleanValue(left).equals(normalizeBooleanValue(right));
    }

    private String normalizeBooleanValue(String value) {
        String normalized = MergeAudit.text(value).toLowerCase(java.util.Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "是".equals(normalized)
                || "yes".equals(normalized) || "y".equals(normalized)) {
            return "true";
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "否".equals(normalized)
                || "no".equals(normalized) || "n".equals(normalized)) {
            return "false";
        }
        return normalized;
    }

    /**
     * 字段发生变化后递增结果版本，使原质量结论失效并记录调整前后的内容。
     */
    private void saveAdjustedResult(MergePendingMasterDO pending, List<MergePendingMasterFieldDO> fields,
            Long operatorId, String operatorName) {
        String before = pending.getResultData();
        String after = resultJson(fields);
        int beforeVersion = pending.getResultVersionNo() == null ? 1 : pending.getResultVersionNo();
        pending.setResultData(after);
        pending.setResultVersionNo(beforeVersion + 1);
        if (!MergeCodes.PENDING_WAIT_CONFIRM.equals(pending.getStatus())) {
            pending.setStatus(MergeCodes.PENDING_WAIT_QUALITY);
        }
        if (MergeCodes.QUALITY_PASS.equals(pending.getQualityStatus())
                || MergeCodes.QUALITY_FAIL.equals(pending.getQualityStatus())
                || MergeCodes.QUALITY_STALE.equals(pending.getQualityStatus())
                || MergeCodes.QUALITY_RUNNING.equals(pending.getQualityStatus())) {
            pending.setQualityStatus(MergeCodes.QUALITY_STALE);
        }
        pending.setErrorMessage(null);
        MergeAudit.touch(pending, operatorId, operatorName);
        mergePendingMasterMapper.updateById(pending);
        writeLog(pending, MergeCodes.LOG_ADJUST, beforeVersion, beforeVersion + 1,
                before, after, operatorId, operatorName, null);
    }

    /**
     * 确认已解决冲突的结果，并在同一事务内执行质量检测。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(MergePendingActionReqVO reqVO, Long operatorId, String operatorName) {
        // 确认只推进到待质量检测状态，质量规则全部通过后才能提交审批。
        MergePendingMasterDO pending = lockPendingOrThrow(reqVO.getPendingId());
        if (!adjustable(pending.getStatus())) {
            throw new ServiceException("当前状态不允许确认合并");
        }
        List<MergePendingMasterFieldDO> fields = fields(pending.getId());
        Map<Long, ModelColumnDO> columns = columns(pending.getModelId());
        for (MergePendingMasterFieldDO field : fields) {
            if (MergeManageFieldUtils.autoGenerated(columns.get(field.getModelColumnId()))) {
                // 自动生成字段由系统处理，不阻塞人工确认；业务字段仍须解决冲突。
                if (!MergeCodes.NO.equals(field.getConflictFlag()) || !MergeCodes.YES.equals(field.getResolvedFlag())) {
                    field.setConflictFlag(MergeCodes.NO);
                    field.setResolvedFlag(MergeCodes.YES);
                    MergeAudit.touch(field, operatorId, operatorName);
                    mergePendingMasterFieldMapper.updateById(field);
                }
                continue;
            }
            ModelColumnDO column = columns.get(field.getModelColumnId());
            String valueError = MergeManageFieldUtils.valueError(column, field.getFinalValue());
            if (valueError != null) {
                throw new ServiceException(column.getName() + "：" + valueError);
            }
            if (!MergeCodes.YES.equals(field.getResolvedFlag())) {
                throw new ServiceException("仍存在未解决的字段冲突");
            }
        }
        String before = pending.getResultData();
        pending.setResultData(resultJson(fields));
        pending.setStatus(MergeCodes.PENDING_WAIT_QUALITY);
        pending.setConfirmTime(new Date());
        pending.setConfirmerId(operatorId);
        MergeAudit.touch(pending, operatorId, operatorName);
        mergePendingMasterMapper.updateById(pending);
        writeLog(pending, MergeCodes.LOG_CONFIRM, pending.getResultVersionNo(), pending.getResultVersionNo(),
                before, pending.getResultData(), operatorId, operatorName, reqVO.getReason());
        mergeManageQualityService.check(pending, fields, columns, operatorId, operatorName);
    }

    /**
     * 重新检测已确认且未进入审批生效流程的当前结果，包括驳回、撤回和取消结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recheck(MergePendingActionReqVO reqVO, Long operatorId, String operatorName) {
        MergePendingMasterDO pending = lockPendingOrThrow(reqVO.getPendingId());
        if (!MergeCodes.PENDING_WAIT_QUALITY.equals(pending.getStatus())
                && !MergeCodes.PENDING_QUALITY_FAIL.equals(pending.getStatus())
                && !MergeCodes.PENDING_WAIT_SUBMIT.equals(pending.getStatus())
                && !MergeCodes.PENDING_REJECTED.equals(pending.getStatus())
                && !MergeCodes.PENDING_WITHDRAWN.equals(pending.getStatus())
                && !MergeCodes.PENDING_CANCELLED.equals(pending.getStatus())) {
            throw new ServiceException("当前结果尚未确认或已进入审批生效流程，不能执行质量检测");
        }
        mergeManageQualityService.check(pending, fields(pending.getId()), columns(pending.getModelId()),
                operatorId, operatorName);
    }

    /**
     * 提交质检通过且快照版本与当前结果一致的结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(MergePendingActionReqVO reqVO, Long operatorId, String operatorName) {
        // 提交前再次校验结果版本，防止质检期间调整后仍提交旧结果。
        MergePendingMasterDO pending = lockPendingOrThrow(reqVO.getPendingId());
        if (!Objects.equals(pending.getStatus(), MergeCodes.PENDING_WAIT_SUBMIT)
                && !Objects.equals(pending.getStatus(), MergeCodes.PENDING_WITHDRAWN)
                && !Objects.equals(pending.getStatus(), MergeCodes.PENDING_CANCELLED)) {
            throw new ServiceException("质量未通过，不能提交审批");
        }
        MergeQualityLogDO qualityLog = pending.getQualityLogId() == null ? null
                : mergeQualityLogMapper.selectById(pending.getQualityLogId());
        if (qualityLog == null || !MergeCodes.QUALITY_PASS.equals(qualityLog.getStatus())
                || !MergeAudit.same(qualityLog.getDataSnapshot(), pending.getResultData())
                || qualityLog.getResultVersionNo() == null
                || !qualityLog.getResultVersionNo().equals(pending.getResultVersionNo())) {
            throw new ServiceException("质量结果已失效，请重新检测");
        }
        ModelPublishedRespDTO published = modelPublishedApi.getPublishedModel(pending.getModelId());
        if (published == null || published.getInstanceId() == null
                || !Objects.equals(published.getVersionId(), pending.getModelVersionId())) {
            throw new ServiceException("当前模型没有可用的成功发布版本，不能提交合并申请");
        }
        MasterChangeRequestSubmitReqDTO request = new MasterChangeRequestSubmitReqDTO();
        request.setInstanceId(published.getInstanceId());
        request.setMode("CREATE");
        request.setApplicantId(operatorId);
        request.setApplyReason(reqVO.getReason() == null || reqVO.getReason().trim().isEmpty()
                ? "多源融合结果提交" : reqVO.getReason().trim());
        Map<String, String> values = new LinkedHashMap<>();
        for (MergePendingMasterFieldDO field : fields(pending.getId())) {
            ModelColumnDO column = columns(pending.getModelId()).get(field.getModelColumnId());
            if (!MergeManageFieldUtils.autoGenerated(column)) {
                values.put(field.getFieldPath(), MergeAudit.text(field.getFinalValue()));
            }
        }
        request.setValues(values);
        MasterChangeRequestSubmitRespDTO submitted = masterChangeRequestApi.submit(request);
        if (submitted == null || submitted.getRequestId() == null
                || submitted.getRequestId().trim().isEmpty()) {
            throw new ServiceException("主数据申请创建失败");
        }
        Long requestId;
        try {
            requestId = Long.valueOf(submitted.getRequestId());
        } catch (NumberFormatException ex) {
            throw new ServiceException("主数据申请编号无效");
        }
        String before = pending.getResultData();
        pending.setRequestId(requestId);
        pending.setStatus(MergeCodes.PENDING_WAIT_EFFECT);
        pending.setErrorMessage(null);
        MergeAudit.touch(pending, operatorId, operatorName);
        mergePendingMasterMapper.updateById(pending);
        String reason = reqVO.getReason() == null || reqVO.getReason().trim().isEmpty()
                ? "已提交主数据维护申请：" + submitted.getRequestNo() : reqVO.getReason();
        writeLog(pending, MergeCodes.LOG_SUBMIT, pending.getResultVersionNo(), pending.getResultVersionNo(),
                before, pending.getResultData(), operatorId, operatorName, reason);
    }

    /**
     * 撤回审批中或待生效结果，同时记录操作人和业务日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(MergePendingActionReqVO reqVO, Long operatorId, String operatorName) {
        MergePendingMasterDO pending = lockPendingOrThrow(reqVO.getPendingId());
        if (!Objects.equals(pending.getStatus(), MergeCodes.PENDING_APPROVING)
                && !Objects.equals(pending.getStatus(), MergeCodes.PENDING_WAIT_EFFECT)) {
            throw new ServiceException("只有审批中或待生效结果可以撤回");
        }
        pending.setStatus(MergeCodes.PENDING_WITHDRAWN);
        MergeAudit.touch(pending, operatorId, operatorName);
        mergePendingMasterMapper.updateById(pending);
        writeLog(pending, MergeCodes.LOG_WITHDRAW, pending.getResultVersionNo(),
                pending.getResultVersionNo(), pending.getResultData(), pending.getResultData(),
                operatorId, operatorName, reqVO.getReason());
    }

    /**
     * 取消审批中或待生效结果，同时记录操作人和业务日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(MergePendingActionReqVO reqVO, Long operatorId, String operatorName) {
        MergePendingMasterDO pending = lockPendingOrThrow(reqVO.getPendingId());
        if (!Objects.equals(pending.getStatus(), MergeCodes.PENDING_APPROVING)
                && !Objects.equals(pending.getStatus(), MergeCodes.PENDING_WAIT_EFFECT)) {
            throw new ServiceException("只有审批中或待生效结果可以取消");
        }
        pending.setStatus(MergeCodes.PENDING_CANCELLED);
        MergeAudit.touch(pending, operatorId, operatorName);
        mergePendingMasterMapper.updateById(pending);
        writeLog(pending, MergeCodes.LOG_CANCEL, pending.getResultVersionNo(),
                pending.getResultVersionNo(), pending.getResultData(), pending.getResultData(),
                operatorId, operatorName, reqVO.getReason());
    }

    /**
     * 重新提交结果，使用与首次提交相同的状态和版本校验。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(MergePendingActionReqVO reqVO, Long operatorId, String operatorName) {
        submit(reqVO, operatorId, operatorName);
    }

    /**
     * 按有效字段构造结果 JSON，供调整和确认使用。
     */
    private String resultJson(List<MergePendingMasterFieldDO> fields) {
        Map<String, String> data = new LinkedHashMap<>();
        for (MergePendingMasterFieldDO field : fields) {
            data.put(field.getFieldPath(), MergeAudit.text(field.getFinalValue()));
        }
        return MergeJson.toJson(data);
    }

    /**
     * 人工选源必须匹配当前候选组的固定版本和字段值。
     */
    private boolean matchesSource(List<SourceSnapshot> snapshots, String fieldPath, Long versionId, String value) {
        if (versionId == null) {
            return false;
        }
        for (SourceSnapshot snapshot : snapshots) {
            if (versionId.equals(snapshot.getVersionId())
                    && MergeAudit.same(snapshot.getValues().get(fieldPath), value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 恢复推荐值时，从固定快照中恢复对应的来源版本。
     */
    private Long versionOf(List<SourceSnapshot> snapshots, String fieldPath, String value, ModelColumnDO column) {
        for (SourceSnapshot snapshot : snapshots) {
            if (sameFieldValue(column, snapshot.getValues().get(fieldPath), value)) {
                return snapshot.getVersionId();
            }
        }
        return null;
    }

    /**
     * 恢复推荐值时按生存规则及原处理类型恢复值来源类型。
     */
    private String systemType(MergePendingMasterFieldDO field, String processType) {
        if (field.getSurvivorRuleId() != null) {
            return MergeCodes.VALUE_SURVIVOR;
        }
        if (MergeCodes.PROCESS_SINGLE.equals(processType)) {
            return MergeCodes.VALUE_SINGLE_DEFAULT;
        }
        return MergeCodes.VALUE_FALLBACK;
    }

    /**
     * 允许调整尚未进入审批生效流程的结果，包括退回、撤回和取消结果。
     */
    private boolean adjustable(String status) {
        return (MergeCodes.PENDING_WAIT_CONFIRM.equals(status) || MergeCodes.PENDING_WAIT_QUALITY.equals(status)
                || MergeCodes.PENDING_QUALITY_FAIL.equals(status) || MergeCodes.PENDING_WAIT_SUBMIT.equals(status)
                || MergeCodes.PENDING_REJECTED.equals(status))
                || Objects.equals(status, MergeCodes.PENDING_WITHDRAWN)
                || Objects.equals(status, MergeCodes.PENDING_CANCELLED);
    }

    /**
     * 首次处理不要求填写调整原因；重新处理入口要求对偏离系统推荐值的字段填写原因。
     * 未传入口模式的旧调用按当前处理状态兼容判断。
     */
    private boolean requiresAdjustReason(MergeFieldAdjustReqVO reqVO, MergePendingMasterDO pending) {
        String mode = MergeAudit.text(reqVO.getProcessingMode());
        String status = pending.getStatus();
        boolean reprocessStatus = MergeCodes.PENDING_QUALITY_FAIL.equals(status)
                || MergeCodes.QUALITY_FAIL.equals(pending.getQualityStatus())
                || MergeCodes.PENDING_REJECTED.equals(status)
                || Objects.equals(status, MergeCodes.PENDING_WITHDRAWN)
                || Objects.equals(status, MergeCodes.PENDING_CANCELLED);
        if (!mode.isEmpty() && !"start".equalsIgnoreCase(mode) && !"reprocess".equalsIgnoreCase(mode)) {
            throw new ServiceException("字段调整处理模式无效");
        }
        return reprocessStatus || "reprocess".equalsIgnoreCase(mode);
    }

    /**
     * 一次加载当前模型字段并按编号索引。
     */
    private Map<Long, ModelColumnDO> columns(Long modelId) {
        Map<Long, ModelColumnDO> result = new HashMap<>();
        List<ModelColumnDO> rows = modelColumnMapper.selectList(
                new LambdaQueryWrapper<ModelColumnDO>()
                        .eq(ModelColumnDO::getModelId, modelId));
        for (ModelColumnDO row : rows) {
            result.put(row.getId(), row);
        }
        return result;
    }

    /**
     * 按字段编号顺序加载有效结果字段。
     */
    private List<MergePendingMasterFieldDO> fields(Long pendingId) {
        return mergePendingMasterFieldMapper.selectList(
                new LambdaQueryWrapper<MergePendingMasterFieldDO>()
                        .eq(MergePendingMasterFieldDO::getPendingId, pendingId)
                .eq(MergePendingMasterFieldDO::getValidFlag, MergeCodes.YES)
                .orderByAsc(MergePendingMasterFieldDO::getId));
    }

    /**
     * 与结果更新同一事务保存操作、前后版本、内容及操作人。
     */
    private void writeLog(MergePendingMasterDO pending, String action, Integer beforeVersion, Integer afterVersion,
            String before, String after, Long operatorId, String operatorName, String reason) {
        MergePendingMasterLogDO log = new MergePendingMasterLogDO();
        log.setPendingId(pending.getId());
        log.setActionType(action);
        log.setBeforeVersionNo(beforeVersion);
        log.setAfterVersionNo(afterVersion);
        log.setBeforeData(before);
        log.setAfterData(after);
        log.setOperatorId(operatorId == null ? 0L : operatorId);
        log.setOperatorName(operatorName);
        log.setOperateTime(new Date());
        log.setReason(MergeAudit.cut(reason, 500));
        log.setValidFlag(MergeCodes.YES);
        log.setDelFlag(MergeCodes.NO);
        MergeAudit.fill(log, operatorId, operatorName);
        mergePendingMasterLogMapper.insert(log);
    }

    /**
     * 写操作在事务内获取结果行锁，后续状态及版本校验均使用最新记录。
     */
    private MergePendingMasterDO lockPendingOrThrow(Long pendingId) {
        MergePendingMasterDO pending = mergePendingMasterMapper.selectByIdForUpdate(pendingId);
        if (pending == null || !MergeCodes.YES.equals(pending.getValidFlag())) {
            throw new ServiceException("待提交结果不存在");
        }
        return pending;
    }
}
