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

package tech.qiantong.qdata.mdm.master.service.change.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterExecuteTaskDO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataService;
import tech.qiantong.qdata.mdm.master.util.MasterLifecycleRules;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.RecordMutation;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 单条生命周期操作仅写主记录状态和审计字段，不改业务字段或子表。 */
@Component
@RequiredArgsConstructor
public class ChangeMasterStatusStrategy implements MaintenanceStrategy {
    private final MasterDataQueryRepository repository;
    private final IMasterDataService dataService;
    private final MasterMaintenanceValidator validator;

    @Override
    public String changeType() {
        return "STATUS_CHANGE";
    }

    @Override
    public MaintenanceResult execute(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model) {
        if (item.getMasterRecordId() == null || item.getMasterRecordId() <= 0) {
            throw new ServiceException("状态申请缺少有效的主数据ID");
        }
        String target = MasterLifecycleRules.target(item.getChangeType(), item.getBeforeStatus());
        if (!MasterLifecycleRules.isAction(item.getChangeType()) || target == null
                || !target.equals(item.getTargetStatus())) {
            throw new ServiceException("状态申请的操作或目标状态不符合生命周期规则");
        }
        if (request.getApplyReason() == null || request.getApplyReason().trim().isEmpty()) {
            throw new ServiceException("状态申请缺少操作原因");
        }
        Map<String, String> submitted = MaintenanceData.snapshot(item.getBeforeData());
        Map<String, Object> before = new LinkedHashMap<>(submitted);
        before.put("STATUS", item.getBeforeStatus());
        MaintenanceWritePlan plan = plan(task, request, item, model);
        RecordMutation mutation = plan.getMutation();
        if (repository.updateRecordWithChildren(model.getDatasourceId(), MaintenanceData.table(model), item.getMasterRecordId(),
                mutation.getValues(), model.getColumnMappings(), mutation.getExpectedStatus(), mutation.getChildren()) != 1) {
            throw new ServiceException("主数据状态未更新，记录可能已删除或状态已变化");
        }
        return plan.getResult();
    }

    @Override
    public MaintenanceWritePlan plan(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model) {
        String target = MasterLifecycleRules.target(item.getChangeType(), item.getBeforeStatus());
        if (target == null || !target.equals(item.getTargetStatus()))
            throw new ServiceException("状态申请的操作或目标状态不符合生命周期规则");
        if (request.getApplyReason() == null || request.getApplyReason().trim().isEmpty())
            throw new ServiceException("状态申请缺少操作原因");
        Map<String, String> submitted = MaintenanceData.snapshot(item.getBeforeData());
        Map<String, Object> before = new LinkedHashMap<>(submitted);
        before.put("STATUS", item.getBeforeStatus());
        validator.validateApplicant(request.getApplicantId(), request.getChangeType());
        validator.validateDataPermission(request.getApplicantId(), model, item.getChangeType(), Collections.emptySet());
        Map<String, Object> current = dataService.getRecord(task.getInstanceId(), item.getMasterRecordId());
        String currentStatus = MaintenanceData.text(MaintenanceData.value(current, "STATUS"));
        if (!Objects.equals(item.getBeforeStatus(), currentStatus)
                || !target.equals(MasterLifecycleRules.target(item.getChangeType(), currentStatus))) {
            throw new ServiceException("主数据状态已变化或不允许此操作，请刷新后重新提交");
        }
        for (MasterFieldRespDTO field : model.getFields()) {
            if (field.getCode() == null || "SYSTEM".equalsIgnoreCase(field.getCategory())) continue;
            String actual = MaintenanceData.fieldText(MaintenanceData.value(current, field.getCode()), field.getDataType());
            if (!MaintenanceData.sameValue(submitted.get(field.getCode()), actual, field.getDataType())) {
                throw new ServiceException("主数据已发生变化，请重新提交状态申请：" + field.getName());
            }
            before.put(field.getCode(), actual);
        }
        if ("CATEGORY".equalsIgnoreCase(model.getModelType())) {
            String category = MaintenanceData.text(MaintenanceData.value(current, "CATEGORY_ID"));
            if (!Objects.equals(submitted.get("CATEGORY_ID"), category)) {
                throw new ServiceException("分类父级已变化，请重新提交状态申请");
            }
            before.put("CATEGORY_ID", category);
        }
        Map<String, Object> after = new LinkedHashMap<>(before);
        after.put("STATUS", target);
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("STATUS", target);
        systemValue(model, updates, "UPDATED_BY", request.getApplicantName());
        systemValue(model, updates, "UPDATED_BY_ID", String.valueOf(request.getApplicantId()));
        systemValue(model, updates, "UPDATED_TIME", new Timestamp(System.currentTimeMillis()));
        return new MaintenanceWritePlan(new MaintenanceResult(item.getMasterRecordId(), before, after, currentStatus, target),
                new RecordMutation(item.getId(), item.getMasterRecordId(), updates, currentStatus,
                        MaintenanceData.json(after), Collections.emptyList()));
    }

    private void systemValue(MasterModelRespDTO model, Map<String, Object> values, String key, Object value) {
        if (model.getColumnMappings().containsKey(key)) values.put(key, value);
    }
}
