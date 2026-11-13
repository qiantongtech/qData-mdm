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

import tech.qiantong.qdata.mdm.master.dal.mapper.data.MasterDataVersionMapper;
import tech.qiantong.qdata.mdm.master.dal.dataobject.data.MasterDataVersionDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterFieldDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeQualityLogDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergeQualityLogMapper;
import tech.qiantong.qdata.mdm.merge.domain.manage.MergeQualityCheckItemBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.service.manage.IMergeManageQualityService;
import tech.qiantong.qdata.mdm.merge.support.MergeAudit;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.MergeQualityChecker;
import tech.qiantong.qdata.mdm.merge.utils.manage.MergeManageFieldUtils;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.RuleQualityMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 合并结果质检，执行模型约束和配置规则并记录固定版本快照。
 *
 * @author chentangwen
 */
@Service
public class MergeManageQualityServiceImpl implements IMergeManageQualityService {

    /**
     * 唯一约束检查中仍占用业务值的有效结果状态。
     */
    private static final List<String> ACTIVE_STATUS = Arrays.asList(
            MergeCodes.PENDING_WAIT_CONFIRM, MergeCodes.PENDING_WAIT_QUALITY, MergeCodes.PENDING_QUALITY_FAIL,
            MergeCodes.PENDING_WAIT_SUBMIT, MergeCodes.PENDING_APPROVING, MergeCodes.PENDING_REJECTED,
            MergeCodes.PENDING_WAIT_EFFECT, MergeCodes.PENDING_EFFECTIVE);

    @Resource
    private MergePendingMasterMapper mergePendingMasterMapper;

    @Resource
    private MergeQualityLogMapper mergeQualityLogMapper;

    @Resource
    private RuleQualityMapper ruleQualityMapper;

    @Resource
    private MasterDataVersionMapper masterDataVersionMapper;

    @Resource
    private ModelPublishedApi modelPublishedApi;

    /**
     * 在已锁定结果的事务内执行质量检查，并保存当前结果版本的质量快照。
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void check(MergePendingMasterDO pending, List<MergePendingMasterFieldDO> fields,
            Map<Long, ModelColumnDO> columns, Long operatorId, String operatorName) {
        // 同时执行模型约束和质量规则，并保存规则快照与明细，保证结果可追溯。
        List<RuleQualityDO> rules = new ArrayList<>();
        for (RuleQualityDO rule : ruleQualityMapper.selectList(new LambdaQueryWrapper<RuleQualityDO>()
                .eq(RuleQualityDO::getModelId, pending.getModelId())
                .eq(RuleQualityDO::getValidFlag, Boolean.TRUE))) {
            if (!"1".equals(rule.getAbnormalFlag())) {
                rules.add(rule);
            }
        }
        Map<String, String> finals = new HashMap<>();
        for (MergePendingMasterFieldDO field : fields) {
            finals.put(field.getFieldPath(), MergeAudit.text(field.getFinalValue()));
            if (field.getModelColumnId() != null) {
                finals.put(String.valueOf(field.getModelColumnId()), MergeAudit.text(field.getFinalValue()));
            }
        }
        Set<String> generated = new HashSet<>();
        for (ModelColumnDO column : columns.values()) {
            if (MergeManageFieldUtils.autoGenerated(column)) {
                generated.add(String.valueOf(column.getId()));
                generated.add(column.getCode());
            }
        }
        List<MergeQualityCheckItemBO> items = MergeQualityChecker.check(finals, rules,
                uniquePeers(pending, rules, columns), generated, referenceValues(pending.getModelId()));
        items.addAll(constraintItems(pending, fields, columns));
        String failureMessage = failureMessage(items);
        saveQualityResult(pending, rules, items, failureMessage, operatorId, operatorName);
    }

    /** 从同一次成功发布的参考数据快照构建字段允许值，避免把草稿或其他集合混入检查。 */
    private Map<String, Set<String>> referenceValues(Long modelId) {
        return MergeQualityChecker.referenceValues(modelPublishedApi.getPublishedModelStructure(modelId));
    }

    /**
     * 汇总规则结论，模型约束不能替代配置规则；null 表示全部通过。
     */
    private String failureMessage(List<MergeQualityCheckItemBO> items) {
        boolean hasExecutedRule = false;
        boolean unsupported = false;
        boolean failed = false;
        List<String> messages = new ArrayList<>();
        for (MergeQualityCheckItemBO item : items) {
            if (!item.isExecuted()) {
                unsupported = true;
                messages.add(item.getRuleName() + "：" + item.getMessage());
            } else if (!item.isModelConstraint()) {
                hasExecutedRule = true;
            }
            if (item.isExecuted() && !item.isPassed()) {
                failed = true;
                messages.add(item.getRuleName() + "：" + item.getMessage());
            }
        }
        if (unsupported) {
            return MergeAudit.cut(join(messages), 500);
        }
        if (!hasExecutedRule) {
            return "未配置可执行的质量规则，不能提交审批";
        }
        return failed ? MergeAudit.cut(join(messages), 500) : null;
    }

    /**
     * 保存检测结论及快照，再更新同一版本结果的质量状态。
     */
    private void saveQualityResult(MergePendingMasterDO pending, List<RuleQualityDO> rules,
            List<MergeQualityCheckItemBO> items, String failureMessage, Long operatorId, String operatorName) {
        boolean pass = failureMessage == null;
        Date now = new Date();
        MergeQualityLogDO qualityLog = new MergeQualityLogDO();
        qualityLog.setPendingMasterId(pending.getId());
        qualityLog.setResultVersionNo(pending.getResultVersionNo());
        qualityLog.setDataSnapshot(pending.getResultData());
        qualityLog.setRuleSnapshot(MergeJson.toJson(ruleSnapshot(rules)));
        List<Map<String, Object>> details = new ArrayList<>();
        for (MergeQualityCheckItemBO item : items) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("ruleName", item.getRuleName());
            detail.put("executed", item.isExecuted());
            detail.put("passed", item.isPassed());
            detail.put("message", item.getMessage());
            details.add(detail);
        }
        qualityLog.setResultDetail(MergeJson.toJson(details));
        qualityLog.setStatus(pass ? MergeCodes.QUALITY_PASS : MergeCodes.QUALITY_FAIL);
        qualityLog.setStartTime(now);
        qualityLog.setEndTime(now);
        qualityLog.setOperatorId(operatorId == null ? 0L : operatorId);
        qualityLog.setResultMessage(pass ? "质量通过" : failureMessage);
        qualityLog.setValidFlag(MergeCodes.YES);
        qualityLog.setDelFlag(MergeCodes.NO);
        MergeAudit.fill(qualityLog, operatorId, operatorName);
        mergeQualityLogMapper.insert(qualityLog);
        pending.setQualityLogId(qualityLog.getId());
        pending.setQualityStatus(pass ? MergeCodes.QUALITY_PASS : MergeCodes.QUALITY_FAIL);
        pending.setStatus(pass ? MergeCodes.PENDING_WAIT_SUBMIT : MergeCodes.PENDING_QUALITY_FAIL);
        pending.setErrorMessage(failureMessage);
        MergeAudit.touch(pending, operatorId, operatorName);
        mergePendingMasterMapper.updateById(pending);
    }

    /**
     * 校验业务字段必填、长度和唯一约束，系统生成字段不参与人工结果校验。
     */
    private List<MergeQualityCheckItemBO> constraintItems(MergePendingMasterDO pending,
            List<MergePendingMasterFieldDO> fields, Map<Long, ModelColumnDO> columns) {
        List<MergeQualityCheckItemBO> items = new ArrayList<>();
        Map<String, Set<String>> occupiedValues = occupiedValues(pending, fields, columns);
        for (MergePendingMasterFieldDO field : fields) {
            ModelColumnDO column = columns.get(field.getModelColumnId());
            if (column == null || MergeManageFieldUtils.autoGenerated(column)) {
                continue;
            }
            String value = MergeAudit.text(field.getFinalValue());
            String valueError = MergeManageFieldUtils.valueError(column, value);
            if (valueError != null) items.add(constraint(column.getName(), valueError));
            if (Boolean.TRUE.equals(column.getRequiredFlag()) && value.isEmpty()) {
                items.add(constraint(column.getName(), "必填字段不能为空"));
            }
            if (column.getDataLength() != null && !value.isEmpty() && !numeric(column.getDataType())
                    && value.length() > column.getDataLength().longValue()) {
                items.add(constraint(column.getName(), "超过模型允许长度"));
            }
            if ((Boolean.TRUE.equals(column.getPrimaryKeyFlag()) || Boolean.TRUE.equals(column.getUniqueFlag()))
                    && !value.isEmpty()
                    && occupiedValues.getOrDefault(field.getFieldPath(), Collections.emptySet()).contains(value)) {
                items.add(constraint(column.getName(), "与其他待提交结果重复"));
            }
        }
        return items;
    }

    /**
     * 仅有非空唯一字段时查询其他结果，每条结果 JSON 只解析一次。
     */
    private Map<String, Set<String>> occupiedValues(MergePendingMasterDO pending,
            List<MergePendingMasterFieldDO> fields, Map<Long, ModelColumnDO> columns) {
        Map<String, Set<String>> values = new HashMap<>();
        for (MergePendingMasterFieldDO field : fields) {
            ModelColumnDO column = columns.get(field.getModelColumnId());
            if (column != null && !MergeManageFieldUtils.autoGenerated(column)
                    && (Boolean.TRUE.equals(column.getPrimaryKeyFlag()) || Boolean.TRUE.equals(column.getUniqueFlag()))
                    && !MergeAudit.text(field.getFinalValue()).isEmpty()) {
                values.put(field.getFieldPath(), new HashSet<>());
            }
        }
        if (values.isEmpty()) {
            return values;
        }
        List<MergePendingMasterDO> others = mergePendingMasterMapper.selectList(
                new LambdaQueryWrapper<MergePendingMasterDO>()
                        .select(MergePendingMasterDO::getResultData)
                        .eq(MergePendingMasterDO::getModelId, pending.getModelId())
                        .eq(MergePendingMasterDO::getValidFlag, MergeCodes.YES)
                        .ne(MergePendingMasterDO::getId, pending.getId())
                        .in(MergePendingMasterDO::getStatus, ACTIVE_STATUS));
        for (MergePendingMasterDO other : others) {
            JsonNode root = MergeJson.read(other.getResultData());
            for (Map.Entry<String, Set<String>> entry : values.entrySet()) {
                entry.getValue().add(MergeAudit.text(root.path(entry.getKey()).asText("")));
            }
        }
        return values;
    }

    /** 唯一性同时比较其他有效融合结果和当前正式版本，自身目标记录除外。 */
    private List<Map<String, String>> uniquePeers(MergePendingMasterDO pending, List<RuleQualityDO> rules,
            Map<Long, ModelColumnDO> columns) {
        List<Map<String, String>> peers = new ArrayList<>();
        if (rules.stream().noneMatch(rule -> "UNIQUE".equalsIgnoreCase(rule.getRuleType()))) {
            return peers;
        }
        List<MergePendingMasterDO> others = mergePendingMasterMapper.selectList(
                new LambdaQueryWrapper<MergePendingMasterDO>()
                        .eq(MergePendingMasterDO::getModelId, pending.getModelId())
                        .eq(MergePendingMasterDO::getValidFlag, MergeCodes.YES)
                        .ne(MergePendingMasterDO::getId, pending.getId())
                        .in(MergePendingMasterDO::getStatus, ACTIVE_STATUS));
        for (MergePendingMasterDO other : others) peers.add(peerValues(other.getResultData(), columns));
        LambdaQueryWrapper<MasterDataVersionDO> query =
                new LambdaQueryWrapper<MasterDataVersionDO>()
                        .eq(MasterDataVersionDO::getModelId, pending.getModelId())
                        .eq(MasterDataVersionDO::getCurrentFlag, "1")
                        .eq(MasterDataVersionDO::getValidFlag, true);
        if (pending.getTargetRecordId() != null) query.ne(
                MasterDataVersionDO::getMasterRecordId, pending.getTargetRecordId());
        for (MasterDataVersionDO version : masterDataVersionMapper.selectList(query)) {
            peers.add(peerValues(version.getSnapshotData(), columns));
        }
        return peers;
    }

    /** 兼容字段编码、大写编码及字段编号快照，同时建立编码和编号索引供规则读取。 */
    private Map<String, String> peerValues(String json, Map<Long, ModelColumnDO> columns) {
        JsonNode root = MergeJson.read(json);
        Map<String, String> values = new HashMap<>();
        for (ModelColumnDO column : columns.values()) {
            JsonNode value = root.get(column.getCode());
            if (value == null) value = root.get(column.getCode().toUpperCase(Locale.ROOT));
            if (value == null) value = root.get(String.valueOf(column.getId()));
            String text = value == null || value.isNull() ? "" : value.asText("");
            values.put(column.getCode(), text);
            values.put(String.valueOf(column.getId()), text);
        }
        return values;
    }

    /**
     * 记录不通过的模型约束，模型约束不替代配置的质量规则。
     */
    private MergeQualityCheckItemBO constraint(String name, String message) {
        MergeQualityCheckItemBO item = new MergeQualityCheckItemBO();
        item.setRuleName("模型约束:" + name);
        item.setExecuted(true);
        item.setPassed(false);
        item.setMessage(message);
        item.setModelConstraint(true);
        return item;
    }

    /**
     * 保存执行时的规则配置，确保后续可追溯。
     */
    private List<Map<String, Object>> ruleSnapshot(List<RuleQualityDO> rules) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RuleQualityDO rule : rules) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rule.getId());
            row.put("ruleName", rule.getRuleName());
            row.put("ruleType", rule.getRuleType());
            row.put("modelFieldId", rule.getModelFieldId());
            row.put("ruleConfig", rule.getRuleConfig());
            rows.add(row);
        }
        return rows;
    }

    /**
     * 数值类型不按文本长度校验，保持原有模型类型兼容规则。
     */
    private boolean numeric(String dataType) {
        String type = dataType == null ? "" : dataType.toUpperCase(Locale.ROOT);
        return type.contains("INT") || type.contains("NUM") || type.contains("DEC")
                || type.contains("FLOAT") || type.contains("DOUBLE");
    }

    /**
     * 合并单项质量失败说明。
     */
    private String join(List<String> messages) {
        StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            if (builder.length() > 0) {
                builder.append('；');
            }
            builder.append(message);
        }
        return builder.toString();
    }
}
