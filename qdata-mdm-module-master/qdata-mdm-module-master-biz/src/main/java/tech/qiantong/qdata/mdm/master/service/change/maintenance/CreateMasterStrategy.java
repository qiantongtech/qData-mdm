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
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.RecordMutation;
import tech.qiantong.qdata.mdm.master.service.data.IMasterSubmodelService;

import java.sql.Timestamp;
import java.util.*;

/** 普通及分类模型新增；主子数据沿用同一目标库事务。 */
@Component
@RequiredArgsConstructor
public class CreateMasterStrategy implements MaintenanceStrategy {
    private final MasterDataQueryRepository repository;
    private final MasterMaintenanceValidator validator;
    private final IMasterSubmodelService submodelService;

    @Override
    public String changeType() {
        return "CREATE";
    }

    @Override
    public MaintenanceResult execute(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model) {
        MaintenanceWritePlan plan = plan(task, request, item, model);
        RecordMutation mutation = plan.getMutation();
        Long recordId = repository.insertRecordWithChildren(model.getDatasourceId(), MaintenanceData.table(model),
                mutation.getRecordId(), mutation.getValues(), model.getColumnMappings(), mutation.getChildren());
        if (recordId == null) {
            throw new ServiceException("主数据正式表新增失败");
        }
        return plan.getResult().withRecordId(recordId);
    }

    @Override
    public MaintenanceWritePlan plan(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model) {
        Long recordId = MaintenanceData.targetRecordId(item.getProposedData());
        if (item.getMasterRecordId() != null) {
            throw new ServiceException("新增申请不能指定已有主数据ID");
        }
        validator.validateApplicant(request.getApplicantId(), request.getChangeType());
        Map<String, String> submittedValues = MaintenanceData.values(item.getProposedData());
        validator.validateDataPermission(request.getApplicantId(), model, "CREATE", submittedValues.keySet());
        validator.validateCategoryParent(model, submittedValues.get("CATEGORY_ID"));
        Map<String, Object> after = new LinkedHashMap<>(submittedValues);
        Map<String, String> errors = validator.validate(model, submittedValues, null);
        if (!errors.isEmpty()) {
            throw new ServiceException("新增执行校验未通过：" + errors.values().iterator().next());
        }
        IMasterSubmodelService.WritePlan children = submodelService.planChanges(model, "CREATE", recordId,
                item.getProposedData(), request);
        if (!children.getWrites().isEmpty()) {
            after.put("submodelChanges", children.getAfter());
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (MasterFieldRespDTO field : model.getFields()) {
            if (!"SYSTEM".equalsIgnoreCase(field.getCategory())) {
                values.put(field.getCode(), MaintenanceData.convert(submittedValues.get(field.getCode()), field.getDataType()));
            }
        }
        if ("CATEGORY".equalsIgnoreCase(model.getModelType())) {
            MasterFieldRespDTO parentField = model.getFields().stream()
                    .filter(field -> "CATEGORY_ID".equalsIgnoreCase(field.getCode()))
                    .findFirst().orElse(null);
            String parentType = parentField == null ? "BIGINT" : parentField.getDataType();
            values.put("CATEGORY_ID", MaintenanceData.convert(submittedValues.get("CATEGORY_ID"), parentType));
            after.put("CATEGORY_ID", submittedValues.get("CATEGORY_ID"));
        }
        // 历史待执行申请仍携带预分配 ID；新申请省略 ID，由数据库自增列生成。
        if (recordId != null) values.put("ID", recordId);
        values.put("STATUS", "1");
        systemValue(model, values, "DEL_FLAG", false);
        systemValue(model, values, "DATA_SOURCE", "MERGE".equals(request.getChangeSource()) ? "MERGE" : "MANUAL");
        systemValue(model, values, "CREATED_BY", request.getApplicantName());
        systemValue(model, values, "CREATED_BY_ID", String.valueOf(request.getApplicantId()));
        systemValue(model, values, "UPDATED_BY", request.getApplicantName());
        systemValue(model, values, "UPDATED_BY_ID", String.valueOf(request.getApplicantId()));
        if (request.getSubmitTime() == null) throw new ServiceException("新增申请缺少提交时间");
        Timestamp submitted = new Timestamp(request.getSubmitTime().getTime());
        systemValue(model, values, "CREATED_TIME", submitted);
        systemValue(model, values, "UPDATED_TIME", submitted);
        RecordMutation mutation = RecordMutation.insert(item.getId(), recordId, values,
                MaintenanceData.json(after), children.getWrites());
        return new MaintenanceWritePlan(new MaintenanceResult(recordId, Collections.emptyMap(), after, "0", "1"), mutation);
    }

    private void systemValue(MasterModelRespDTO model, Map<String, Object> values, String key, Object value) {
        if (model.getColumnMappings().containsKey(key)) {
            values.put(key, value);
        }
    }

}
