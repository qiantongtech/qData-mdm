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
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.security.web.service.SysPermissionService;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.util.MasterFieldValueValidator;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.master.util.MasterPublishedModelUtils;
//import tech.qiantong.qdata.mdm.auth.api.dataauthorization.service.DataAuthorizationApi;

import java.util.*;
import java.util.stream.Collectors;

/** 新增、修改共用的字段校验与执行权限复核。 */
@Component
@RequiredArgsConstructor
public class MasterMaintenanceValidator {
    private final ModelPublishedApi modelPublishedApi;
    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository repository;
    private final ISysUserService userService;
    private final SysPermissionService permissionService;
//    private final DataAuthorizationApi dataAuthorizationApi;

    /** 授权模块以模型及字段为范围；恢复动作使用其 RESTORE 权限编码。 */
    public void validateDataPermission(Long applicantId, MasterModelRespDTO model, String operation, Set<String> fields) {
        Map<String, String> errors = dataPermissionErrors(applicantId, model, operation, fields);
        if (!errors.isEmpty()) throw new ServiceException(errors.values().iterator().next());
    }

    public Map<String, String> dataPermissionErrors(Long applicantId, MasterModelRespDTO model, String operation, Set<String> fields) {
        Map<String, String> errors = new LinkedHashMap<>();
        SysUser user = userService.selectUserById(applicantId);
        if (user == null) {
            errors.put("permission", "维护申请人不存在");
            return errors;
        }
        if (permissionService.getMenuPermission(user).contains("*:*:*")) return errors;
        String permission = "RECOVER".equals(operation) ? "RESTORE" : operation;
//        if (!dataAuthorizationApi.hasOperationPermission(applicantId, model.getModelId(), permission)) {
//            errors.put("permission", "申请人无当前模型的操作权限：" + operation);
//            return errors;
//        }
        for (String code : fields) {
            if (isAllowedCategorySystemValue(model, code)) continue;
            MasterFieldRespDTO field = model.getFields().stream().filter(value -> code.equals(value.getCode()))
                    .findFirst().orElse(null);
            if (field == null) {
                errors.put(code, "申请包含未发布业务字段：" + code);
                continue;
            }
//            if (field.getId() == null || !dataAuthorizationApi.hasFieldPermission(applicantId, model.getModelId(), field.getId(), true))
//                errors.put(code, "申请人无字段修改权限：" + code);
        }
        return errors;
    }

    /** 按申请单类型复核；导入明细的新增、修改统一校验 BATCH_IMPORT 菜单权限。 */
    public void validateApplicant(Long applicantId, String operation) {
        if (applicantId == null || applicantId <= 0) {
            throw new ServiceException("维护申请缺少申请人");
        }
        SysUser user = userService.selectUserById(applicantId);
        if (user == null || !"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag())) {
            throw new ServiceException("维护申请人不存在或已停用");
        }
        Set<String> permissions = permissionService.getMenuPermission(user);
        String required;
        switch (operation == null ? "" : operation) {
            case "CREATE":
            case "COPY":
                required = "master:instance:add";
                break;
            case "UPDATE":
            case "BATCH_UPDATE":
                required = "master:instance:edit";
                break;
            case "BATCH_IMPORT":
                required = "master:instance:import";
                break;
            case "BATCH_STATUS":
            case "FREEZE":
            case "UNFREEZE":
            case "INVALIDATE":
            case "RECOVER":
            case "ARCHIVE":
                required = "master:instance:status";
                break;
            default:
                required = null;
        }
        if (required == null || (!permissions.contains("*:*:*") && !permissions.contains(required))) {
            throw new ServiceException("申请人已无主数据维护权限");
        }
    }

    public Map<String, String> withDefaults(MasterModelRespDTO model, Map<String, String> submitted) {
        Map<String, String> values = submitted == null ? new LinkedHashMap<>() : new LinkedHashMap<>(submitted);
        for (MasterFieldRespDTO field : model.getFields()) {
            if (!"SYSTEM".equalsIgnoreCase(field.getCategory()) && !values.containsKey(field.getCode())
                    && field.getDefaultValue() != null) {
                values.put(field.getCode(), field.getDefaultValue());
            }
        }
        return values;
    }

    /** 新增表单只接受自动编码字段之外的默认值和用户输入。 */
    public Map<String, String> withDefaultsForCreate(MasterModelRespDTO model, Map<String, String> submitted) {
        Set<String> automaticFields = automaticFieldCodes(model);
        Map<String, String> values = submitted == null ? new LinkedHashMap<>() : new LinkedHashMap<>(submitted);
        values.keySet().removeIf(code -> code != null && automaticFields.contains(code.toUpperCase(Locale.ROOT)));
        for (MasterFieldRespDTO field : model.getFields()) {
            if (!"SYSTEM".equalsIgnoreCase(field.getCategory()) && field.getCode() != null
                    && !automaticFields.contains(field.getCode().toUpperCase(Locale.ROOT))
                    && !values.containsKey(field.getCode()) && field.getDefaultValue() != null) {
                values.put(field.getCode(), field.getDefaultValue());
            }
        }
        return values;
    }

    /** 返回当前成功发布版本中由编码方案维护的字段编码（统一大写）。 */
    public Set<String> automaticFieldCodes(MasterModelRespDTO model) {
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(model.getModelId());
        if (structure == null || !Objects.equals(model.getModelId(), structure.getModelId())
                || !Objects.equals(model.getInstanceId(), structure.getInstanceId())
                || !Objects.equals(model.getModelVersionId(), structure.getVersionId())) {
            throw new ServiceException("维护申请与当前成功发布版本不一致");
        }
        return structure.getFields().stream()
                .filter(field -> "AUTOMATIC".equalsIgnoreCase(field.getEncodingMode()))
                .filter(field -> field.getCode() != null)
                .map(field -> field.getCode().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public Map<String, String> validate(MasterModelRespDTO model, Map<String, String> values, Long recordId) {
        return validate(model, values, recordId, false);
    }

    public Map<String, String> validate(MasterModelRespDTO model, Map<String, String> values, Long recordId,
            boolean allowUnassignedAutomatic) {
        boolean create = recordId == null;
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(model.getModelId());
        if (structure == null || !Objects.equals(model.getModelId(), structure.getModelId())
                || !Objects.equals(model.getInstanceId(), structure.getInstanceId())
                || !Objects.equals(model.getModelVersionId(), structure.getVersionId())) {
            throw new ServiceException("维护申请与当前成功发布版本不一致");
        }
        Map<String, ModelPublishedColumnRespDTO> definitions = structure.getFields().stream()
                .filter(field -> field.getCode() != null)
                .collect(Collectors.toMap(field -> field.getCode().toUpperCase(Locale.ROOT), field -> field));
        Map<String, MasterFieldRespDTO> business = model.getFields().stream()
                .filter(field -> !"SYSTEM".equalsIgnoreCase(field.getCategory()))
                .collect(Collectors.toMap(field -> field.getCode().toUpperCase(Locale.ROOT), field -> field));
        Map<String, String> errors = new LinkedHashMap<>();
        for (String code : values.keySet()) {
            if (code == null) continue;
            String normalizedCode = code.toUpperCase(Locale.ROOT);
            if (!business.containsKey(normalizedCode) && !isAllowedCategorySystemValue(model, normalizedCode)) {
                errors.put(String.valueOf(code), "包含当前模型未配置的业务字段");
            }
        }
        for (MasterFieldRespDTO field : model.getFields()) {
            if (field.getCode() == null || !business.containsKey(field.getCode().toUpperCase(Locale.ROOT))) {
                continue;
            }
            String code = field.getCode();
            String raw = values.get(code);
            String value = raw == null ? "" : raw.trim();
            ModelPublishedColumnRespDTO definition = definitions.get(code.toUpperCase(Locale.ROOT));
            if (definition == null) {
                throw new ServiceException("维护字段缺少发布配置：" + code);
            }
            if (value.isEmpty()) {
                if (create && allowUnassignedAutomatic
                        && "AUTOMATIC".equalsIgnoreCase(definition.getEncodingMode())) continue;
                if (Boolean.TRUE.equals(field.getRequired()) || Boolean.TRUE.equals(field.getNameField())) {
                    errors.put(code, field.getName() + "不能为空");
                }
                continue;
            }
            if (raw.length() > 65535 || !MasterFieldValueValidator.valid(field.getDataType(), raw,
                    field.getDataLength(), field.getDataScale())) {
                errors.put(code, field.getName() + "不符合模型字段类型、长度或精度");
                continue;
            }
            Object typed;
            try {
                typed = MaintenanceData.convert(raw, field.getDataType());
            } catch (ServiceException ex) {
                errors.put(code, field.getName() + "不符合模型字段类型");
                continue;
            }
            if (Boolean.TRUE.equals(field.getUnique()) && repository.countByColumn(model.getDatasourceId(),
                    MaintenanceData.table(model), code, typed, recordId, model.getColumnMappings()) > 0) {
                errors.put(code, field.getName() + "已存在");
                continue;
            }
            if (definition.getDictTypeId() != null && structure.getReferenceItems().stream().noneMatch(item ->
                    definition.getDictTypeId().equals(item.getTypeId()) && raw.equals(item.getCode())
                            && Boolean.TRUE.equals(item.getValidFlag()) && !Boolean.TRUE.equals(item.getDelFlag()))) {
                errors.put(code, field.getName() + "请选择有效的参考数据");
                continue;
            }
            if ("RELATION".equalsIgnoreCase(field.getCategory()) || "CLASS".equalsIgnoreCase(field.getCategory())) {
                try {
                    if (!validRelation(definition, typed)) {
                        errors.put(code, field.getName() + "关联数据不存在、已停用或不符合节点规则");
                    }
                } catch (ServiceException ex) {
                    errors.put(code, field.getName() + "关联数据校验失败：" + ex.getMessage());
                }
            }
        }
        return errors;
    }

    /** 分类父级属于系统关系字段，不参与业务字段值校验，但执行前必须重新确认其有效性。 */
    public void validateCategoryParent(MasterModelRespDTO model, String parentId) {
        if (!"CATEGORY".equalsIgnoreCase(model.getModelType())) return;
        if (model.getColumnMappings() == null || !model.getColumnMappings().containsKey("CATEGORY_ID"))
            throw new ServiceException("分类模型父级字段绑定不完整");
        if (parentId == null || parentId.trim().isEmpty()) return;
        long id;
        try {
            id = Long.parseLong(parentId.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException("父级节点ID格式无效");
        }
        if (id <= 0) throw new ServiceException("父级节点ID格式无效");
        long count = repository.countActiveByColumn(model.getDatasourceId(), MaintenanceData.table(model),
                "ID", id, model.getColumnMappings());
        if (count == 0) throw new ServiceException("父级节点不存在或不是生效状态");
        if (count > 1) throw new ServiceException("父级节点ID存在重复数据");
    }

    private boolean isAllowedCategorySystemValue(MasterModelRespDTO model, String code) {
        return "CATEGORY".equalsIgnoreCase(model.getModelType()) && "CATEGORY_ID".equals(code);
    }

    private boolean validRelation(ModelPublishedColumnRespDTO field, Object value) {
        if (field.getRelatedModelId() == null || field.getRelatedCode() == null) {
            return false;
        }
        ModelPublishedRespDTO published = modelPublishedApi.getPublishedModel(field.getRelatedModelId());
        MasterPublishedModelUtils.requirePublication(published, field.getRelatedModelId(), null, null);
        MasterModelRespDTO related = metadataService.getModel(published.getInstanceId(), field.getRelatedModelId());
        String code = field.getRelatedCode().toUpperCase(Locale.ROOT);
        if (!related.getColumnMappings().containsKey(code)) {
            return false;
        }
        String table = MaintenanceData.table(related);
        if (repository.countActiveByColumn(related.getDatasourceId(), table, code, value, related.getColumnMappings()) != 1) {
            return false;
        }
        if ("CLASS".equalsIgnoreCase(field.getColumnCategory())) {
            if (!"CATEGORY".equalsIgnoreCase(related.getModelType())) {
                return false;
            }
            if ("LEAF".equalsIgnoreCase(field.getNodeRule())) {
                List<Long> ids = repository.selectIdsByColumn(related.getDatasourceId(), table, code, value, related.getColumnMappings());
                return ids.size() == 1 && repository.selectIdsByColumn(related.getDatasourceId(), table,
                        "CATEGORY_ID", ids.get(0), related.getColumnMappings()).isEmpty();
            }
        }
        return true;
    }
}
