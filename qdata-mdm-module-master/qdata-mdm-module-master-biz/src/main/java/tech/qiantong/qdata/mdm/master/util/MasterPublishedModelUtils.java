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

package tech.qiantong.qdata.mdm.master.util;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 成功发布标识和权威物化绑定的统一校验，不查询数据库或生成SQL。 */
public final class MasterPublishedModelUtils {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");

    private MasterPublishedModelUtils() {
    }

    /** 多次公共接口调用必须对应同一个模型、成功实例和版本。 */
    public static void requirePublication(ModelPublishedRespDTO published, Long modelId,
            Long instanceId, Long versionId) {
        if (published == null || !positive(published.getId()) || !positive(published.getInstanceId())
                || !positive(published.getVersionId())) {
            throw new ServiceException("成功发布模型标识不完整");
        }
        if ((modelId != null && !modelId.equals(published.getId()))
                || (instanceId != null && !instanceId.equals(published.getInstanceId()))
                || (versionId != null && !versionId.equals(published.getVersionId()))) {
            throw new ServiceException("模型已重新发布或当前实例不可用，请返回卡片列表后重新进入");
        }
    }

    public static void requireStructure(ModelPublishedRespDTO published, ModelPublishedStructureRespDTO structure) {
        requirePublication(published, null, null, null);
        if (structure == null || !Objects.equals(published.getId(), structure.getModelId())
                || !Objects.equals(published.getInstanceId(), structure.getInstanceId())
                || !Objects.equals(published.getVersionId(), structure.getVersionId())) {
            throw new ServiceException("模型字段与成功发布记录不一致，请返回卡片列表后重新进入");
        }
    }

    /** 主表必须存在，且只能有一个启用的权威绑定。 */
    public static ModelPublishedMaterializedTableRespDTO requireAuthorityTable(
            ModelPublishedMaterializationRespDTO binding, Long modelId, Long parentId) {
        ModelPublishedMaterializedTableRespDTO table = findAuthorityTable(binding, modelId, parentId);
        if (table == null) throw new ServiceException("模型没有可用的权威物化表");
        return table;
    }

    /** 未物化的已配置子模型允许返回空；重复绑定、数据源或实例不一致必须拒绝。 */
    public static ModelPublishedMaterializedTableRespDTO findAuthorityTable(
            ModelPublishedMaterializationRespDTO binding, Long modelId, Long parentId) {
        requirePublication(binding, null, null, null);
        if (!positive(modelId) || parentId == null || parentId < 0 || !positive(binding.getDatasourceId())
                || binding.getTables() == null) {
            throw new ServiceException("成功发布的物化绑定不完整");
        }
        if ((parentId == 0 && !modelId.equals(binding.getId()))
                || (parentId != 0 && !parentId.equals(binding.getId()))) {
            throw new ServiceException("权威物化表与发布主模型归属不一致");
        }
        List<ModelPublishedMaterializedTableRespDTO> tables = binding.getTables().stream()
                .filter(table -> table != null && "AUTHORITY".equals(table.getTableRole())
                        && modelId.equals(table.getModelId()) && parentId.equals(table.getParentId())
                        && Boolean.TRUE.equals(table.getEnabledFlag()) && Boolean.TRUE.equals(table.getValidFlag())
                        && Boolean.FALSE.equals(table.getDelFlag()))
                .collect(Collectors.toList());
        if (tables.size() > 1) throw new ServiceException("模型存在多个可用的权威物化表");
        if (tables.isEmpty()) return null;
        ModelPublishedMaterializedTableRespDTO table = tables.get(0);
        if (!positive(table.getId()) || !Objects.equals(binding.getInstanceId(), table.getInstanceId())
                || !Objects.equals(binding.getDatasourceId(), table.getDatasourceId())) {
            throw new ServiceException("权威物化表与成功发布目标不一致");
        }
        if ((parentId == 0 && table.getParentTableId() != null && table.getParentTableId() != 0)
                || (parentId != 0 && !positive(table.getParentTableId()))) {
            throw new ServiceException("权威主子表绑定不完整");
        }
        qualifiedTable(binding.getSchemaName(), table.getTableName());
        return table;
    }

    /** 保留数据库标识符大小写；具体数据库的引用和SQL模板仍交给common。 */
    public static String qualifiedTable(String schemaName, String tableName) {
        if (tableName == null) throw new ServiceException("模型物理表名缺失");
        String schema = schemaName == null ? "" : schemaName.trim();
        if (!schema.isEmpty() && !IDENTIFIER.matcher(schema).matches()) {
            throw new ServiceException("模型Schema名称无效");
        }
        String[] parts = tableName.split("\\.", -1);
        if (parts.length < 1 || parts.length > 2) throw new ServiceException("模型物理表名无效");
        for (String part : parts) {
            if (!IDENTIFIER.matcher(part).matches()) throw new ServiceException("模型物理表名无效");
        }
        if (parts.length == 2) {
            if (!schema.isEmpty() && !schema.equals(parts[0])) {
                throw new ServiceException("模型物理表与成功发布Schema不一致");
            }
            return tableName;
        }
        return schema.isEmpty() ? tableName : schema + "." + tableName;
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }
}
