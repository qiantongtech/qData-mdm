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
import tech.qiantong.qdata.mdm.master.service.data.IMasterSubmodelService;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.RecordMutation;

import java.util.*;
import java.util.stream.Collectors;

/** 普通模型单条修改，沿用原有快照复核和局部更新规则。 */
@Component
@RequiredArgsConstructor
public class UpdateMasterStrategy implements MaintenanceStrategy {
    private final MasterDataQueryRepository repository;
    private final IMasterDataService dataService;
    private final IMasterSubmodelService submodelService;
    private final MasterMaintenanceValidator validator;

    @Override
    public String changeType() {
        return "UPDATE";
    }

    @Override
    public MaintenanceResult execute(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model) {
        if (item.getMasterRecordId() == null) {
            throw new ServiceException("申请明细缺少待修改主数据ID");
        }
        MaintenanceWritePlan plan = plan(task, request, item, model);
        RecordMutation mutation = plan.getMutation();
        if (repository.updateRecordWithChildren(model.getDatasourceId(), MaintenanceData.table(model), item.getMasterRecordId(),
                mutation.getValues(), model.getColumnMappings(), mutation.getExpectedStatus(), mutation.getChildren()) != 1) {
            throw new ServiceException("主数据正式表未更新，可能已冻结或已失效");
        }
        return plan.getResult();
    }

    @Override
    public MaintenanceWritePlan plan(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model) {
        Map<String, String> before = MaintenanceData.snapshot(item.getBeforeData());
        Map<String, Object> beforeResult = MaintenanceData.beforeSnapshot(before, item.getProposedData());
        validator.validateApplicant(request.getApplicantId(), request.getChangeType());
        String operation = "BATCH_UPDATE".equals(request.getChangeType()) ? "BATCH_UPDATE" : "UPDATE";
        validator.validateDataPermission(request.getApplicantId(), model, operation,
                MaintenanceData.values(item.getProposedData()).keySet());
        Map<String, Object> current = dataService.getRecord(task.getInstanceId(), item.getMasterRecordId());
        String status = MaintenanceData.text(MaintenanceData.value(current, "STATUS"));
        if (!"1".equals(status)) {
            throw new ServiceException("主数据当前状态不允许修改");
        }
        Map<String, MasterFieldRespDTO> fields = model.getFields().stream()
                .filter(field -> field.getCode() != null && !"SYSTEM".equalsIgnoreCase(field.getCategory()))
                .collect(Collectors.toMap(field -> field.getCode().toUpperCase(Locale.ROOT), field -> field,
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, Object> after = new LinkedHashMap<>();
        Map<String, Object> updates = new LinkedHashMap<>();
        Map<String, String> effectiveValues = new LinkedHashMap<>();
        for (MasterFieldRespDTO field : fields.values()) {
            String actual = MaintenanceData.fieldText(MaintenanceData.value(current, field.getCode()), field.getDataType());
            if (!MaintenanceData.sameValue(before.get(field.getCode()), actual, field.getDataType())) {
                throw new ServiceException("主数据已发生变化，请重新提交申请：" + field.getName());
            }
            after.put(field.getCode(), actual);
            effectiveValues.put(field.getCode(), actual);
        }
        Map<String, String> proposed = MaintenanceData.values(item.getProposedData());
        if ("CATEGORY".equalsIgnoreCase(model.getModelType())) {
            String category = MaintenanceData.text(MaintenanceData.value(current, "CATEGORY_ID"));
            if (!Objects.equals(before.get("CATEGORY_ID"), category))
                throw new ServiceException("分类父级已变化，请重新提交申请");
            after.put("CATEGORY_ID", category);
            if (proposed.containsKey("CATEGORY_ID")) {
                validator.validateCategoryParent(model, proposed.get("CATEGORY_ID"));
                MasterFieldRespDTO parentField = model.getFields().stream()
                        .filter(field -> "CATEGORY_ID".equalsIgnoreCase(field.getCode()))
                        .findFirst().orElse(null);
                String parentType = parentField == null ? "BIGINT" : parentField.getDataType();
                String requestedParent = proposed.get("CATEGORY_ID");
                if (!MaintenanceData.sameValue(category, requestedParent, parentType)) {
                    updates.put("CATEGORY_ID", MaintenanceData.convert(requestedParent, parentType));
                    after.put("CATEGORY_ID", requestedParent == null ? null : requestedParent.trim());
                }
            }
        }
        effectiveValues.putAll(proposed);
        Map<String, String> errors = validator.validate(model, effectiveValues, item.getMasterRecordId());
        if (!errors.isEmpty()) {
            throw new ServiceException("修改执行校验未通过：" + errors.values().iterator().next());
        }
        for (Map.Entry<String, String> entry : proposed.entrySet()) {
            if ("CATEGORY_ID".equalsIgnoreCase(entry.getKey())
                    && "CATEGORY".equalsIgnoreCase(model.getModelType())) {
                continue;
            }
            MasterFieldRespDTO field = fields.get(entry.getKey().toUpperCase(Locale.ROOT));
            if (field == null) {
                throw new ServiceException("申请包含未发布业务字段：" + entry.getKey());
            }
            String old = MaintenanceData.fieldText(MaintenanceData.value(current, field.getCode()), field.getDataType());
            if (!MaintenanceData.sameValue(old, entry.getValue(), field.getDataType())) {
                updates.put(field.getCode(), MaintenanceData.convert(entry.getValue(), field.getDataType()));
                after.put(field.getCode(), entry.getValue());
            }
        }
        IMasterSubmodelService.WritePlan children = submodelService.planChanges(model, "UPDATE", item.getMasterRecordId(),
                item.getProposedData(), request);
        if (updates.isEmpty() && children.getWrites().isEmpty()) {
            throw new ServiceException("申请没有实际业务字段变更");
        }
        if (!children.getWrites().isEmpty()) {
            after.put("submodelChanges", children.getAfter());
            // 仅子表变化时也校验主表状态，并通过统一结果回写生成新的主记录版本。
            if (updates.isEmpty()) {
                updates.put("STATUS", "1");
            }
        }
        systemValue(model, updates, "UPDATED_BY", request.getApplicantName());
        systemValue(model, updates, "UPDATED_BY_ID", String.valueOf(request.getApplicantId()));
        if (model.getColumnMappings().containsKey("UPDATED_TIME")) {
            if (request.getSubmitTime() == null) {
                throw new ServiceException("修改申请缺少提交时间");
            }
            systemValue(model, updates, "UPDATED_TIME", new java.sql.Timestamp(request.getSubmitTime().getTime()));
        }
        return new MaintenanceWritePlan(new MaintenanceResult(item.getMasterRecordId(), beforeResult, after, status, status),
                new RecordMutation(item.getId(), item.getMasterRecordId(), updates, "1", MaintenanceData.json(after), children.getWrites()));
    }

    private void systemValue(MasterModelRespDTO model, Map<String, Object> values, String key, Object value) {
        if (model.getColumnMappings().containsKey(key)) {
            values.put(key, value);
        }
    }
}
