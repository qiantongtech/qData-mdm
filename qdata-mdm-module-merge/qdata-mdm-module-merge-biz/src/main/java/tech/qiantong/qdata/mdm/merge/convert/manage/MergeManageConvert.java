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

package tech.qiantong.qdata.mdm.merge.convert.manage;

import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergeFieldCandidateRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingApprovalRecordRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingFieldRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterFieldDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterLogDO;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.merge.utils.manage.MergeManageFieldUtils;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

import java.util.ArrayList;
import java.util.List;

/**
 * 合并管理展示对象转换，仅复制字段及展示属性，不查询数据或执行状态流转。
 *
 * @author chentangwen
 */
public final class MergeManageConvert {

    private MergeManageConvert() {
    }

    /**
     * 转换合并结果列表的主表字段。
     */
    public static MergePendingRespVO toPage(MergePendingMasterDO pending) {
        MergePendingRespVO row = new MergePendingRespVO();
        row.setId(pending.getId());
        row.setModelId(pending.getModelId());
        row.setModelVersionId(pending.getModelVersionId());
        row.setObjectGroupId(pending.getObjectGroupId());
        row.setMergeRecordId(pending.getMergeRecordId());
        row.setStatus(pending.getStatus());
        row.setQualityStatus(pending.getQualityStatus());
        row.setResultVersionNo(pending.getResultVersionNo());
        row.setConfirmTime(pending.getConfirmTime());
        row.setErrorMessage(pending.getErrorMessage());
        row.setCreateTime(pending.getCreateTime());
        row.setUpdateTime(pending.getUpdateTime());
        return row;
    }

    /**
     * 转换合并结果详情的主表字段。
     */
    public static MergePendingDetailRespVO toDetail(MergePendingMasterDO pending) {
        MergePendingDetailRespVO detail = new MergePendingDetailRespVO();
        detail.setId(pending.getId());
        detail.setModelId(pending.getModelId());
        detail.setModelVersionId(pending.getModelVersionId());
        detail.setObjectGroupId(pending.getObjectGroupId());
        detail.setMergeRecordId(pending.getMergeRecordId());
        detail.setStatus(pending.getStatus());
        detail.setQualityStatus(pending.getQualityStatus());
        detail.setResultVersionNo(pending.getResultVersionNo());
        detail.setResultData(pending.getResultData());
        detail.setQualityLogId(pending.getQualityLogId());
        detail.setErrorMessage(pending.getErrorMessage());
        detail.setFlowInstanceId(pending.getFlowInstanceId());
        detail.setRequestId(pending.getRequestId());
        detail.setTargetRecordId(pending.getTargetRecordId());
        detail.setResultRecordId(pending.getResultRecordId());
        detail.setResultVersionId(pending.getResultVersionId());
        detail.setConfirmTime(pending.getConfirmTime());
        detail.setUpdateTime(pending.getUpdateTime());
        return detail;
    }

    /**
     * 转换待合并结果操作日志为审批记录响应对象。
     *
     * @param log 待合并结果操作日志
     * @return 审批记录展示对象
     */
    public static MergePendingApprovalRecordRespVO toApprovalRecord(MergePendingMasterLogDO log) {
        MergePendingApprovalRecordRespVO row = new MergePendingApprovalRecordRespVO();
        row.setId(log.getId());
        row.setOperatorName(log.getOperatorName());
        row.setActionType(log.getActionType());
        row.setOperateTime(log.getOperateTime());
        row.setReason(log.getReason());
        row.setBeforeVersionNo(log.getBeforeVersionNo());
        row.setAfterVersionNo(log.getAfterVersionNo());
        row.setFlowInstanceId(log.getFlowInstanceId());
        row.setRequestId(log.getRequestId());
        row.setBeforeData(log.getBeforeData());
        row.setAfterData(log.getAfterData());
        return row;
    }

    /**
     * 转换字段取值、规则名称及模型展示属性。
     */
    public static MergePendingFieldRespVO toField(MergePendingMasterFieldDO field, ModelColumnDO column,
            String survivorRuleName) {
        MergePendingFieldRespVO view = new MergePendingFieldRespVO();
        view.setId(field.getId());
        view.setModelColumnId(field.getModelColumnId());
        view.setFieldPath(field.getFieldPath());
        view.setRecommendedValue(field.getRecommendedValue());
        view.setFinalValue(field.getFinalValue());
        view.setValueSourceType(field.getValueSourceType());
        view.setSourceVersionId(field.getSourceVersionId());
        view.setSurvivorRuleId(field.getSurvivorRuleId());
        view.setSurvivorRuleName(survivorRuleName);
        view.setConflictFlag(field.getConflictFlag());
        view.setResolvedFlag(field.getResolvedFlag());
        view.setManualOverrideFlag(field.getManualOverrideFlag());
        view.setAdjustReason(field.getAdjustReason());
        if (column != null) {
            view.setColumnName(column.getName());
            view.setDataType(column.getDataType());
            view.setColumnCategory(column.getColumnCategory());
            view.setPrimaryKeyFlag(column.getPrimaryKeyFlag());
            view.setUniqueFlag(column.getUniqueFlag());
            view.setRequiredFlag(column.getRequiredFlag());
            view.setNameFlag(column.getNameFlag());
            view.setEditable(MergeManageFieldUtils.editable(column));
            view.setAutoGenerated(MergeManageFieldUtils.autoGenerated(column));
            view.setAutomaticEncoding(column.getEncodingVersionId() != null);
        } else {
            view.setEditable(Boolean.FALSE);
            view.setAutoGenerated(Boolean.FALSE);
            view.setAutomaticEncoding(Boolean.FALSE);
        }
        return view;
    }

    /**
     * 将固定快照转换为指定字段的候选来源值。
     */
    public static List<MergeFieldCandidateRespVO> candidates(List<SourceSnapshot> snapshots, String fieldPath) {
        List<MergeFieldCandidateRespVO> rows = new ArrayList<>();
        for (SourceSnapshot snapshot : snapshots) {
            MergeFieldCandidateRespVO row = new MergeFieldCandidateRespVO();
            row.setSourceRecordId(snapshot.getSourceRecordId());
            row.setSourceVersionId(snapshot.getVersionId());
            row.setStableRecordId(snapshot.getStableRecordId());
            row.setSystemCode(snapshot.getSystemCode());
            row.setSystemName(snapshot.getSystemName());
            row.setBusinessName(snapshot.getBusinessName());
            row.setValue(snapshot.getValues().get(fieldPath));
            rows.add(row);
        }
        return rows;
    }
}
