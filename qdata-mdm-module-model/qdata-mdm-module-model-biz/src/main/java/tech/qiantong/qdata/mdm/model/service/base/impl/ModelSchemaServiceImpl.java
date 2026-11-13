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

package tech.qiantong.qdata.mdm.model.service.base.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedColumnDefinition;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedIndexDefinition;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedStructure;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedTableDefinition;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedTableIdentity;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishContext;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishSnapshot;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishStructureDiff;
import tech.qiantong.qdata.mdm.model.domain.ModelSchemaObjectChange;
import tech.qiantong.qdata.mdm.model.domain.ModelSchemaObjectRef;
import tech.qiantong.qdata.mdm.model.domain.ModelSchemaPropertyChange;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelIndexType;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishChangeStatus;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishStage;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSchemaChangeKind;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSchemaObjectType;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSystemField;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelValidationStage;
import tech.qiantong.qdata.mdm.model.service.base.IModelPublishService;
import tech.qiantong.qdata.mdm.model.service.base.IModelSchemaService;
import tech.qiantong.qdata.mdm.model.util.ModelPublishUtils;
import tech.qiantong.qdata.mdm.model.util.ModelPublishTimingUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 统一构建来源、权威主子表结构，并比较上次成功结构；这里只整理结构和差异，不执行SQL。
 */
@Service
public class ModelSchemaServiceImpl implements IModelSchemaService {

    /**
     * 构建前调用统一发布服务完成CONFIG校验。
     */
    @Resource
    private IModelPublishService modelPublishService;

    /**
     * 整体构建主模型及直接子模型的双表，全部完成后才把结果放入发布上下文。
     *
     * <p>
     * 构建的仍是计划定义，尚未创建真实表；两套表业务列一致，但来源业务列可空且不建业务唯一索引。
     *
     * @param context 已读取本次配置和上次成功结构的发布数据
     * @return 本次完整结构，包含必须保留的停用物理对象
     */
    @Override
    public ModelMaterializedStructure build(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("BUILD", context);
        try {
            if (context == null) {
                throw new ServiceException("缺少发布数据");
            }
            context.setTargetStructure(null);
            context.setCurrentStage(ModelPublishStage.CONFIG);
            // 构建前先查配置合法性；CONFIG失败立即停止，避免错误字段继续生成结构和SQL。
            modelPublishService.validate(ModelValidationStage.CONFIG, context);
            context.setCurrentStage(ModelPublishStage.BUILD);
            // 旧表按“模型ID＋来源/权威角色”查找，不按表名猜身份，才能识别非法改名。
            Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> previous = previousTables(context);
            List<JSONObject> models = ModelPublishUtils.models(context);
            Set<Long> presentModels = new HashSet<>();
            ModelMaterializedStructure result = new ModelMaterializedStructure();
            JSONObject root = context.getSnapshot().getModel();
            for (JSONObject model : models) {
                presentModels.add(model.getLong("id"));
                boolean active = ModelPublishUtils.active(model);
                // 每个模型分别构建来源表和权威表，每次都创建独立列对象，防止来源放宽约束影响权威表。
                for (ModelTableRole role : ModelTableRole.values()) {
                    ModelMaterializedTableIdentity identity = ModelMaterializedTableIdentity.of(model.getLong("id"), role);
                    ModelMaterializedTableDefinition old = previous.get(identity);
                    if (!active) {
                        // 未发布过就停用的模型不用建表；以前已经建过的表必须完整保留，只改变业务使用标记。
                        if (old != null) {
                            ModelMaterializedTableDefinition retained = copyTable(old);
                            retained.setEnabledFlag(false);
                            result.getTables().add(retained);
                        }
                        continue;
                    }
                    result.getTables().add(buildTable(root, model, role, old));
                }
            }
            for (ModelMaterializedTableDefinition old : previous.values()) {
                if (!presentModels.contains(old.getModelId())) {
                    // 本次配置没有列出的历史模型仍沿用旧表，不把配置缺失当成物理删除。
                    result.getTables().add(copyTable(old));
                }
            }
            // 不同模型编码组合也可能算出同一个表名，例如a_b+c与a+b_c，不能只检查模型编码是否重复。
            Set<String> tableNames = new HashSet<>();
            for (ModelMaterializedTableDefinition table : result.getTables()) {
                if (!tableNames.add(table.getTableName())) {
                    throw new ServiceException("来源、权威或主子模型计算出的物理表名重复：" + table.getTableName());
                }
            }
            // 主子模型和两套表全部构建完成后再放入上下文，后续比较始终拿到完整的一组表。
            context.setTargetStructure(result);
            return result;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 将上次成功的表按模型ID和角色放入Map，发现不完整配对时停止，不能偷偷补建另一角色。
     *
     * @param context 本次发布数据
     * @return 上次成功的表清单
     */
    private Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> previousTables(ModelPublishContext context) {
        Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> result = new LinkedHashMap<>();
        if (context.getBaseStructure() == null) {
            if (context.getBaseInstanceId() != null) {
                throw new ServiceException("已发布模型缺少上次成功的完整结构");
            }
            return result;
        }
        for (ModelMaterializedTableDefinition table : context.getBaseStructure().getTables()) {
            ModelMaterializedTableIdentity key = ModelMaterializedTableIdentity.of(table.getModelId(), table.getTableRole());
            if (result.put(key, table) != null) {
                throw new ServiceException("上次成功结构中同一模型、同一角色的表重复");
            }
        }
        if (context.getBaseInstanceId() != null && result.isEmpty()) {
            throw new ServiceException("上次成功发布记录没有物化表，不能按首次发布重建");
        }
        for (ModelMaterializedTableDefinition table : result.values()) {
            ModelTableRole other = table.getTableRole() == ModelTableRole.SOURCE ? ModelTableRole.AUTHORITY : ModelTableRole.SOURCE;
            if (!result.containsKey(ModelMaterializedTableIdentity.of(table.getModelId(), other))) {
                throw new ServiceException("上次成功结构缺少配对的来源表或权威表");
            }
        }
        return result;
    }

    /**
     * 单独构建一张表，同一字段在来源和权威中使用不同对象，不互相修改可空及唯一设置。
     *
     * @param root  主模型配置
     * @param model 当前节点配置
     * @param role  当前表角色
     * @param old   上次成功的同一张表，首次为空
     * @return 完整表定义
     */
    private ModelMaterializedTableDefinition buildTable(JSONObject root, JSONObject model, ModelTableRole role,
                                                        ModelMaterializedTableDefinition old) {
        ModelMaterializedTableDefinition table = new ModelMaterializedTableDefinition();
        table.setModelId(model.getLong("id"));
        boolean child = !root.getLong("id").equals(model.getLong("id"));
        table.setParentId(child ? root.getLong("id") : 0L);
        table.setTableRole(role);
        String prefix = role == ModelTableRole.SOURCE ? "mdm_src_" : "mdm_";
        // 已经成功物化的表沿用数据库真实名称，Oracle等数据库可能调整过名称大小写。
        table.setTableName(old == null ? prefix + root.getString("code") + (child ? "_" + model.getString("code") : "")
                : old.getTableName());
        // 中文名称或描述修改不应该变成ALTER COMMENT，老表继续使用原来的真实物理注释。
        table.setTableComment(old == null ? model.getString("name") : old.getTableComment());
        // 系统字段有配置时直接使用配置编码；后台补充的系统字段使用字典默认编码。
        Map<ModelSystemField, String> systemCodes = new HashMap<>();
        for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
            if ("SYSTEM".equals(field.getString("columnCategory"))) {
                ModelSystemField system = ModelSystemField.getByCode(field.getString("code"));
                if (system != null) systemCodes.put(system, field.getString("code"));
            }
        }
        // 先按角色补系统列，再补业务列；父级ID只关联同角色主表。
        for (ModelSystemField system : systemFields(role, child, "CATEGORY".equals(root.getString("type")))) {
            ModelMaterializedColumnDefinition column = systemColumn(system);
            column.setColumnName(systemCodes.getOrDefault(system, system.getColumnName()));
            table.getColumns().add(column);
        }
        Map<Long, JSONObject> configuredFields = new HashMap<>();
        for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
            configuredFields.put(field.getLong("id"), field);
            if ("SYSTEM".equals(field.getString("columnCategory")) || Boolean.TRUE.equals(ModelPublishUtils.flag(field, "delFlag"))) {
                continue;
            }
            ModelMaterializedColumnDefinition column = businessColumn(field, role);
            table.getColumns().add(column);
            // 业务唯一仅在权威表生成；来源表接收不同系统的数据，不套业务唯一限制。
            if (role == ModelTableRole.AUTHORITY && Boolean.TRUE.equals(ModelPublishUtils.flag(field, "uniqueFlag"))) {
                table.getIndexes().add(index(table, "UNIQUE:FIELD:" + field.getLong("id"), ModelIndexType.UNIQUE,
                        "uk_" + table.getTableName() + "_f_" + field.getLong("id"), "FIELD:" + field.getLong("id"), column.getColumnName()));
            }
        }
        table.getIndexes().add(index(table, "PRIMARY_KEY", ModelIndexType.PRIMARY_KEY,
                "pk_" + table.getTableName(), "SYSTEM:ID", systemCodes.getOrDefault(ModelSystemField.ID, ModelSystemField.ID.getColumnName())));
        if (child) {
            table.getIndexes().add(index(table, "PARENT_ID", ModelIndexType.NORMAL,
                    "idx_" + table.getTableName() + "_parent_id", "SYSTEM:PARENT_ID", systemCodes.getOrDefault(ModelSystemField.PARENT_ID, ModelSystemField.PARENT_ID.getColumnName())));
        }
        if ("CATEGORY".equals(root.getString("type"))) {
            table.getIndexes().add(index(table, "CATEGORY_ID", ModelIndexType.NORMAL,
                    "idx_" + table.getTableName() + "_category_id", "SYSTEM:CATEGORY_ID", systemCodes.getOrDefault(ModelSystemField.CATEGORY_ID, ModelSystemField.CATEGORY_ID.getColumnName())));
        }
        // 最后合并旧物理信息：已停用列、历史索引、实际顺序与注释都按既定保留规则处理。
        preserveExisting(table, old, configuredFields);
        return table;
    }

    /**
     * 按角色列出系统字段，来源追踪字段只进来源表，权威审计字段只进权威表。
     *
     * @param role     表角色
     * @param child    是否子模型
     * @param category 是否分类模型
     * @return 按固定顺序排列的系统字段
     */
    private List<ModelSystemField> systemFields(ModelTableRole role, boolean child, boolean category) {
        List<ModelSystemField> result = new ArrayList<>();
        result.add(ModelSystemField.ID);
        if (child) {
            result.add(ModelSystemField.PARENT_ID);
        }
        if (category) {
            result.add(ModelSystemField.CATEGORY_ID);
        }
        if (role == ModelTableRole.SOURCE) {
            result.addAll(Arrays.asList(ModelSystemField.SOURCE_SYSTEM_ID, ModelSystemField.ACCESS_TASK_ID,
                    ModelSystemField.BATCH_ID, ModelSystemField.SOURCE_RECORD_ID, ModelSystemField.COLLECTION_TIME, ModelSystemField.STATUS));
        } else {
            result.addAll(Arrays.asList(ModelSystemField.DATA_SOURCE, ModelSystemField.CREATED_BY, ModelSystemField.CREATED_TIME,
                    ModelSystemField.CREATED_BY_ID, ModelSystemField.UPDATED_BY, ModelSystemField.UPDATED_BY_ID,
                    ModelSystemField.UPDATED_TIME, ModelSystemField.REMARK, ModelSystemField.STATUS));
        }
        result.add(ModelSystemField.DEL_FLAG);
        return result;
    }

    /**
     * 从固定字典创建系统列，两个角色的id都由应用生成，不使用数据库自增。
     *
     * @param system 预制字段
     * @return 独立的系统列定义
     */
    private ModelMaterializedColumnDefinition systemColumn(ModelSystemField system) {
        ModelMaterializedColumnDefinition column = new ModelMaterializedColumnDefinition();
        column.setSystemFieldKey(system.name());
        column.setColumnName(system.getColumnName());
        // 中文说明随列定义交给common，由数据库方言生成对应的字段注释SQL。
        column.setColumnComment(system.getColumnComment());
        column.setLogicalType(system.getLogicalType());
        column.setCharacterLength(system.getCharacterLength());
        column.setLengthUnit(system.getCharacterLength() == null ? null : "CHAR");
        column.setNullable(system.isNullable());
        column.setDefaultValue(system.getDefaultValue());
        column.setPrimaryKeyFlag(system == ModelSystemField.ID);
        column.setIdentityFlag(system == ModelSystemField.ID);
        column.setSystemFlag(true);
        return column;
    }

    /**
     * 将业务字段转为一列，来源只放宽业务必填，不放宽系统主键约束。
     *
     * @param field 字段配置
     * @param role  来源或权威
     * @return 独立业务列
     */
    private ModelMaterializedColumnDefinition businessColumn(JSONObject field, ModelTableRole role) {
        ModelMaterializedColumnDefinition column = new ModelMaterializedColumnDefinition();
        column.setModelFieldId(field.getLong("id"));
        column.setColumnName(field.getString("code"));
        column.setLogicalType(field.getString("dataType"));
        if ("STRING".equals(column.getLogicalType())) {
            column.setCharacterLength(field.getLong("dataLength") == null ? 255L : field.getLong("dataLength"));
            column.setLengthUnit("CHAR");
        }
        if ("DECIMAL".equals(column.getLogicalType())) {
            column.setNumericPrecision(Math.toIntExact(field.getLongValue("dataLength")));
            column.setNumericScale(Math.toIntExact(field.getLongValue("dataScale")));
        }
        // 来源业务列统一可空；权威业务列才根据requiredFlag决定是否必填。
        column.setNullable(role == ModelTableRole.SOURCE || !Boolean.TRUE.equals(ModelPublishUtils.flag(field, "requiredFlag")));
        String defaultValue = field.getString("defaultValue");
        column.setDefaultValue(defaultValue == null || defaultValue.isEmpty() ? null : defaultValue);
        column.setPrimaryKeyFlag(false);
        column.setIdentityFlag(false);
        column.setSystemFlag(false);
        column.setColumnComment(field.getString("name"));
        return column;
    }

    /**
     * 整理单列主键或索引的结构；这里只记录定义，不生成SQL或重复创建主键索引。
     *
     * @param table      所属表
     * @param stableKey  不随索引名字变化的固定标识
     * @param type       主键、唯一或普通索引
     * @param name       根据模型计算出的索引名
     * @param columnKey  字段稳定标识
     * @param columnName 实际列名
     * @return 索引定义
     */
    private ModelMaterializedIndexDefinition index(ModelMaterializedTableDefinition table, String stableKey, ModelIndexType type,
                                                   String name, String columnKey, String columnName) {
        ModelMaterializedIndexDefinition index = new ModelMaterializedIndexDefinition();
        index.setStableKey(stableKey);
        // 自动生成的长索引名使用确定的短名，兼容Oracle11等30字符限制；历史名称在保留结构时继续沿用。
        String indexName = name;
        if (name.length() > 30) {
            String suffix = DigestUtils.md5DigestAsHex(name.getBytes(StandardCharsets.UTF_8)).substring(0, 12);
            indexName = name.substring(0, 17) + "_" + suffix;
        }
        index.setIndexName(indexName);
        index.setIndexType(type);
        index.getColumnKeys().add(columnKey);
        index.getColumnNames().add(columnName);
        index.setIncludedInCreateTable(type == ModelIndexType.PRIMARY_KEY);
        return index;
    }

    /**
     * 老列的物理顺序及注释保留；本次配置未列出的历史字段也沿用旧结构。
     *
     * @param table            本次完整表
     * @param old              上次成功的同一张表
     * @param configuredFields 本次配置中的全部字段，包含逻辑删除记录
     */
    private void preserveExisting(ModelMaterializedTableDefinition table, ModelMaterializedTableDefinition old,
                                  Map<Long, JSONObject> configuredFields) {
        Map<String, ModelMaterializedColumnDefinition> current = new LinkedHashMap<>();
        for (ModelMaterializedColumnDefinition column : table.getColumns()) {
            if (current.put(columnKey(column), column) != null) {
                throw new ServiceException("同一张表的字段固定标识重复");
            }
        }
        int position = 0;
        if (old != null) {
            for (ModelMaterializedColumnDefinition previous : old.getColumns()) {
                position = Math.max(position, previous.getColumnPosition() == null ? 0 : previous.getColumnPosition());
                String key = columnKey(previous);
                ModelMaterializedColumnDefinition existing = current.get(key);
                // 同一个字段继续使用时，沿用旧位置、真实类型和物理注释；页面顺序变化不调整数据库列顺序。
                if (existing != null) {
                    existing.setColumnPosition(previous.getColumnPosition());
                    existing.setColumnComment(previous.getColumnComment());
                    existing.setPhysicalType(previous.getPhysicalType());
                    // 历史物化列沿用原自增属性，不隐式迁移普通主键。
                    existing.setIdentityFlag(previous.getIdentityFlag());
                } else {
                    JSONObject config = configuredFields.get(previous.getModelFieldId());
                    ModelMaterializedColumnDefinition retained = JSON.parseObject(JSON.toJSONString(previous), ModelMaterializedColumnDefinition.class);
                    // 只有配置明确逻辑删除时才停用；配置没有返回时继续保留上次的使用标记。
                    if (config != null && Boolean.TRUE.equals(ModelPublishUtils.flag(config, "delFlag"))) {
                        retained.setEnabledFlag(false);
                    }
                    current.put(key, retained);
                }
            }
            for (ModelMaterializedIndexDefinition previous : old.getIndexes()) {
                // 历史发布管理的索引完整保留，本次未重新生成也不表示可以删除。
                ModelMaterializedIndexDefinition generated = table.getIndexes().stream()
                        .filter(candidate -> previous.getStableKey().equals(candidate.getStableKey())).findFirst().orElse(null);
                if (generated != null) {
                    generated.setIndexName(previous.getIndexName());
                } else {
                    table.getIndexes().add(JSON.parseObject(JSON.toJSONString(previous), ModelMaterializedIndexDefinition.class));
                }
            }
        }
        Set<String> names = new HashSet<>();
        table.setColumns(new ArrayList<>(current.values()));
        for (ModelMaterializedColumnDefinition column : table.getColumns()) {
            if (!names.add(column.getColumnName())) {
                throw new ServiceException("业务字段不能复用仍在数据库中的旧列名：" + column.getColumnName());
            }
            if (column.getColumnPosition() == null) {
                column.setColumnPosition(++position);
            }
        }
        // 显示顺序不改变物理列位置；按真实位置排列，避免仅调整页面顺序导致完整结构JSON无意义地变化。
        table.getColumns().sort(Comparator.comparingInt(ModelMaterializedColumnDefinition::getColumnPosition));
    }

    /**
     * 统一产生用于Map匹配的列标识，不用列名判断是否同一个字段。
     *
     * @param column 业务列或系统列
     * @return FIELD:字段ID或SYSTEM:系统标识
     */
    private String columnKey(ModelMaterializedColumnDefinition column) {
        return column.getSystemFieldKey() == null ? "FIELD:" + column.getModelFieldId() : "SYSTEM:" + column.getSystemFieldKey();
    }

    /**
     * 复制整张旧表，包括字段和索引，避免本次停用把上次成功记录在内存里也改掉。
     *
     * @param old 旧成功表
     * @return 与原表没有共享可变对象的副本
     */
    private ModelMaterializedTableDefinition copyTable(ModelMaterializedTableDefinition old) {
        return JSON.parseObject(JSON.toJSONString(old), ModelMaterializedTableDefinition.class);
    }

    // 比较本次完整结构和上次成功结构，只记录差异，不在这里执行DDL。

    /**
     * 先在独立副本上完成比较，再写入上下文，避免后续修改配置把本次比较记录一起改掉。
     *
     * <p>
     * 先复制完整新旧结构，再比较表、列、索引与配置；compareObject区分新增、修改、停用和意外缺失。
     *
     * @param context 已构建完整新结构的发布数据
     * @return 包含全部对象及逐项属性的比较结果
     */
    @Override
    public ModelPublishStructureDiff compare(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("DIFF", context);
        try {
            if (context == null || context.getSnapshot() == null || context.getTargetStructure() == null) {
                throw new ServiceException("结构比较缺少本次完整配置或目标结构");
            }
            context.setDiff(null);
            context.setCurrentStage(ModelPublishStage.DIFF);
            if (context.getBaseInstanceId() != null && (context.getBaseStructure() == null || context.getBaseSnapshot() == null)) {
                throw new ServiceException("结构比较缺少上次成功发布的完整配置或结构");
            }
            if (context.getBaseInstanceId() == null && (context.getBaseSnapshot() != null
                    || (context.getBaseStructure() != null && !context.getBaseStructure().getTables().isEmpty()))) {
                throw new ServiceException("首次发布不能混入没有成功实例归属的旧结构或配置");
            }
            if (!Objects.equals(context.getModelId(), context.getSnapshot().getModelId())
                    || (context.getBaseSnapshot() != null && !Objects.equals(context.getModelId(), context.getBaseSnapshot().getModelId()))
                    || (context.getBaseInstanceId() != null && context.getBaseStructure().getTables().isEmpty())) {
                throw new ServiceException("结构比较的模型归属不一致，或上次成功结构没有物化表");
            }
            ModelPublishStructureDiff result = new ModelPublishStructureDiff();
            result.setBaseInstanceId(context.getBaseInstanceId());
            // 先复制新旧结构，后续修改上下文不会悄悄改变已经形成的差异记录。
            result.setBeforeStructure(context.getBaseStructure() == null ? new ModelMaterializedStructure()
                    : JSON.parseObject(JSON.toJSONString(context.getBaseStructure()), ModelMaterializedStructure.class));
            result.setAfterStructure(JSON.parseObject(JSON.toJSONString(context.getTargetStructure()), ModelMaterializedStructure.class));
            Map<String, ModelMaterializedTableDefinition> before = tables(result.getBeforeStructure());
            Map<String, ModelMaterializedTableDefinition> after = tables(result.getAfterStructure());
            // 取新旧表身份的并集：只在新结构出现是新增，只在旧结构出现是缺失，两边都有才比较属性。
            for (String key : keys(before, after)) {
                ModelMaterializedTableDefinition old = before.get(key);
                ModelMaterializedTableDefinition current = after.get(key);
                ModelMaterializedTableDefinition table = current == null ? old : current;
                compareObject(result, reference(table, ModelSchemaObjectType.TABLE, "TABLE"),
                        tableProperties(old), tableProperties(current), false);
                // 新表的列和索引也逐项记录，生成计划时按表归并，不能把它们又作为旧表加列执行。
                compareMembers(result, table, old, current, true);
                compareMembers(result, table, old, current, false);
            }
            // 物理结构之外还比较名称、描述、显示顺序、引用数据和规则配置，确保零DDL修改也留下发布记录。
            compareConfigurations(result, context.getBaseSnapshot(), context.getSnapshot());
            result.setChangeStatus(summarize(result, context.getBaseInstanceId() == null));
            context.setDiff(result);
            return result;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 用模型ID加角色匹配表，发现重复直接报错，不能让Map覆盖旧对象。
     */
    private Map<String, ModelMaterializedTableDefinition> tables(ModelMaterializedStructure structure) {
        Map<String, ModelMaterializedTableDefinition> result = new LinkedHashMap<>();
        if (structure.getTables() == null) {
            throw new ServiceException("完整结构缺少表清单");
        }
        for (ModelMaterializedTableDefinition table : structure.getTables()) {
            if (table == null || table.getModelId() == null || table.getTableRole() == null
                    || result.put(table.getModelId() + ":" + table.getTableRole(), table) != null) {
                throw new ServiceException("物化表缺少固定ID、角色或存在重复记录");
            }
        }
        return result;
    }

    /**
     * 表本身只比较表属性；列和索引在各自的对象记录里完整保存，避免同一变化重复计数。
     */
    private JSONObject tableProperties(ModelMaterializedTableDefinition table) {
        if (table == null) {
            return null;
        }
        JSONObject result = object(table);
        result.remove("columns");
        result.remove("indexes");
        return result;
    }

    /**
     * 业务列按字段ID、系统列按固定标识、索引按用途匹配，编码变化因此会被识别为修改。
     */
    private void compareMembers(ModelPublishStructureDiff result, ModelMaterializedTableDefinition table,
                                ModelMaterializedTableDefinition old, ModelMaterializedTableDefinition current, boolean columns) {
        Map<String, JSONObject> before = members(old, columns);
        Map<String, JSONObject> after = members(current, columns);
        for (String key : keys(before, after)) {
            ModelSchemaObjectRef ref = reference(table, columns ? ModelSchemaObjectType.COLUMN : ModelSchemaObjectType.INDEX, key);
            JSONObject definition = after.containsKey(key) ? after.get(key) : before.get(key);
            ref.setColumnName(columns ? definition.getString("columnName") : null);
            ref.setIndexName(columns ? null : definition.getString("indexName"));
            compareObject(result, ref, before.get(key), after.get(key), false);
        }
    }

    /**
     * 建立单张表内的对象Map，对缺失ID或重复ID明确失败，不用物理名称代替身份。
     */
    private Map<String, JSONObject> members(ModelMaterializedTableDefinition table, boolean columns) {
        Map<String, JSONObject> result = new LinkedHashMap<>();
        if (table == null) {
            return result;
        }
        if (table.getColumns() == null || table.getIndexes() == null) {
            throw new ServiceException("物化表缺少完整字段或索引清单");
        }
        if (columns) {
            for (ModelMaterializedColumnDefinition column : table.getColumns()) {
                String key = comparisonColumnKey(column);
                if (result.put(key, object(column)) != null) {
                    throw new ServiceException("物化表中的字段固定ID重复：" + key);
                }
            }
        } else {
            for (ModelMaterializedIndexDefinition index : table.getIndexes()) {
                if (index == null || index.getStableKey() == null || index.getStableKey().isEmpty()
                        || result.put(index.getStableKey(), object(index)) != null) {
                    throw new ServiceException("物化表中的索引固定标识缺失或重复");
                }
            }
        }
        return result;
    }

    /**
     * 配置按模型、字段ID拆开比较；数组换顺序本身不算删对象，显示顺序等属性仍逐项记录。
     */
    private void compareConfigurations(ModelPublishStructureDiff result, ModelPublishSnapshot old, ModelPublishSnapshot current) {
        Map<String, JSONObject> before = configurations(old);
        Map<String, JSONObject> after = configurations(current);
        for (String key : keys(before, after)) {
            JSONObject definition = after.containsKey(key) ? after.get(key) : before.get(key);
            ModelSchemaObjectRef ref = new ModelSchemaObjectRef();
            ref.setObjectType(ModelSchemaObjectType.MODEL);
            ref.setObjectKey(key);
            ref.setModelId(key.startsWith("FIELD:") ? definition.getLong("modelId") : definition.getLong("id"));
            compareObject(result, ref, before.get(key), after.get(key), true);
        }
        // 参考数据、编码方案和规则也属于成功配置，不能只比较数据库表列。
        ModelSchemaObjectRef ref = new ModelSchemaObjectRef();
        ref.setObjectType(ModelSchemaObjectType.MODEL);
        ref.setModelId(current.getModelId());
        ref.setObjectKey("SNAPSHOT_BINDINGS");
        compareObject(result, ref, bindings(old), bindings(current), true);
    }

    /**
     * 去掉已单独比较的嵌套对象，保留其余全部配置属性；不能丢掉已逻辑删除的记录。
     */
    private Map<String, JSONObject> configurations(ModelPublishSnapshot snapshot) {
        Map<String, JSONObject> result = new LinkedHashMap<>();
        if (snapshot == null) {
            return result;
        }
        List<JSONObject> models = new ArrayList<>();
        models.add(snapshot.getModel());
        models.addAll(ModelPublishUtils.objects(snapshot.getModel(), "subModels"));
        for (JSONObject model : models) {
            JSONObject copy = object(model);
            copy.remove("fields");
            copy.remove("subModels");
            putConfiguration(result, "MODEL:", copy);
            for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
                putConfiguration(result, "FIELD:", object(field));
            }
        }
        return result;
    }

    /**
     * 保存一条带稳定ID的配置，坏数据不能通过覆盖Map中的旧值被隐藏。
     */
    private void putConfiguration(Map<String, JSONObject> result, String prefix, JSONObject config) {
        // 配置读取可能返回0/1或true/false，两种写法含义相同，不制造假变化。
        for (String property : new ArrayList<>(config.keySet())) {
            if (property.endsWith("Flag") && ModelPublishUtils.flag(config, property) != null) {
                config.put(property, ModelPublishUtils.flag(config, property));
            }
        }
        Long id = config.getLong("id");
        if (id == null || result.put(prefix + id, config) != null) {
            throw new ServiceException("模型或字段配置的固定ID缺失或重复");
        }
    }

    /**
     * 单独保留与模型关联的完整辅助配置，不复制连接信息。
     */
    private JSONObject bindings(ModelPublishSnapshot snapshot) {
        return snapshot == null ? null : JSONObject.of("referenceData", snapshot.getReferenceData(),
                "encodingSchemes", snapshot.getEncodingSchemes(), "rules", snapshot.getRules());
    }

    /**
     * 比较单个对象，新增、缺失、停用和物理属性修改分别记录；缺失永远不表示可以删除数据库对象。
     */
    private void compareObject(ModelPublishStructureDiff result, ModelSchemaObjectRef ref, JSONObject before,
                               JSONObject after, boolean configuration) {
        List<ModelSchemaPropertyChange> changes = new ArrayList<>();
        properties("", before, after, changes);
        if (changes.isEmpty()) {
            return;
        }
        ModelSchemaObjectChange change = new ModelSchemaObjectChange();
        change.setObject(ref);
        change.setBeforeDefinition(before);
        change.setAfterDefinition(after);
        change.setProperties(changes);
        // 变化类型用于说明这次改了什么；是否允许执行仍交给CHANGE校验，缺失绝不自动转换成DROP。
        if (after == null) {
            change.setChangeKind(ModelSchemaChangeKind.UNEXPECTED_MISSING);
        } else if (before == null) {
            change.setChangeKind(ModelSchemaChangeKind.ADDED);
        } else if (configuration && Boolean.TRUE.equals(ModelPublishUtils.flag(after, "delFlag"))
                && !Boolean.TRUE.equals(ModelPublishUtils.flag(before, "delFlag"))) {
            change.setChangeKind(ModelSchemaChangeKind.LOGICALLY_DELETED);
        } else if ((!configuration && Boolean.FALSE.equals(after.getBoolean("enabledFlag"))
                && !Boolean.FALSE.equals(before.getBoolean("enabledFlag")))
                || (configuration && ref.getObjectKey().startsWith("MODEL:")
                && ModelPublishUtils.active(before) && !ModelPublishUtils.active(after))) {
            change.setChangeKind(ModelSchemaChangeKind.DISABLED);
        } else {
            boolean metadata = configuration ? changes.stream().anyMatch(property -> metadataProperty(ref, property.getProperty()))
                    : changes.stream().allMatch(property -> "enabledFlag".equals(property.getProperty()));
            change.setChangeKind(metadata ? ModelSchemaChangeKind.METADATA_ONLY : ModelSchemaChangeKind.MODIFIED);
        }
        // 停用标记不改变数据库；同一次还改了其他物理属性时，仍然要标成有物理变化并交CHANGE逐项判断。
        change.setDdlRequired(!configuration && (before == null || after == null
                || changes.stream().anyMatch(property -> !"enabledFlag".equals(property.getProperty()))));
        result.getChanges().add(change);
    }

    /**
     * 编码、类型和约束在物理对象中已有记录，配置副本不再把它们重复算作元数据；其余属性完整保留。
     */
    private boolean metadataProperty(ModelSchemaObjectRef ref, String property) {
        if ("SNAPSHOT_BINDINGS".equals(ref.getObjectKey())) {
            return true;
        }
        return !Arrays.asList("code", "type", "parentId", "modelId", "dataType", "dataLength", "dataScale",
                "columnCategory", "defaultValue", "requiredFlag", "uniqueFlag", "primaryKeyFlag").contains(property);
    }

    /**
     * 对JSON对象逐层展开属性；有序数组整体保存，保留索引列顺序或展示配置顺序。
     */
    private void properties(String path, Object before, Object after, List<ModelSchemaPropertyChange> result) {
        if (equivalent(before, after)) {
            return;
        }
        // 对象继续逐层展开成属性路径；数组作为整体比较，保留索引列顺序等有业务意义的信息。
        if (before instanceof JSONObject && after instanceof JSONObject) {
            JSONObject old = (JSONObject) before;
            JSONObject current = (JSONObject) after;
            for (String key : keys(old, current)) {
                properties(path.isEmpty() ? key : path + "." + key, old.get(key), current.get(key), result);
            }
            return;
        }
        ModelSchemaPropertyChange property = new ModelSchemaPropertyChange();
        property.setProperty(path.isEmpty() ? "$" : path);
        property.setBeforeValue(before);
        property.setAfterValue(after);
        result.add(property);
    }

    /**
     * JSON读取可能把同一个数值表示为Integer或Long，比较时忽略这种Java包装类型差异。
     */
    private boolean equivalent(Object before, Object after) {
        if (before instanceof Number && after instanceof Number) {
            return new BigDecimal(before.toString()).compareTo(new BigDecimal(after.toString())) == 0;
        }
        return Objects.equals(before, after);
    }

    /**
     * 用固定顺序遍历两份Map里的全部标识，保证重复比较输出顺序一致。
     */
    private Set<String> keys(Map<String, ?> before, Map<String, ?> after) {
        Set<String> result = new TreeSet<>(before.keySet());
        result.addAll(after.keySet());
        return result;
    }

    /**
     * 产生一个不与原始对象共享可变值的JSON副本。
     */
    private JSONObject object(Object value) {
        return JSON.parseObject(JSON.toJSONString(value));
    }

    /**
     * 获取业务字段或系统字段的固定身份，非法定义交给调用方纠正。
     */
    private String comparisonColumnKey(ModelMaterializedColumnDefinition column) {
        if (column == null || (column.getModelFieldId() == null) == (column.getSystemFieldKey() == null)) {
            throw new ServiceException("物化列必须且只能有业务字段ID或系统固定标识");
        }
        return column.getSystemFieldKey() == null ? "FIELD:" + column.getModelFieldId() : "SYSTEM:" + column.getSystemFieldKey();
    }

    /**
     * 定位一个表内对象；名称只帮助排查问题，不参与对象身份匹配。
     */
    private ModelSchemaObjectRef reference(ModelMaterializedTableDefinition table, ModelSchemaObjectType type, String key) {
        ModelSchemaObjectRef result = new ModelSchemaObjectRef();
        result.setObjectType(type);
        result.setModelId(table.getModelId());
        result.setParentId(table.getParentId());
        result.setTableRole(table.getTableRole());
        result.setTableName(table.getTableName());
        result.setObjectKey(key);
        return result;
    }

    /**
     * 新增但尚未建表的停用配置也要算作元数据变化；新物理对象已有对应记录时不重复计算。
     */
    private boolean configurationAdditionOnly(ModelSchemaObjectChange change, ModelPublishStructureDiff diff) {
        ModelSchemaObjectRef ref = change.getObject();
        if (ref.getObjectType() != ModelSchemaObjectType.MODEL || change.getChangeKind() != ModelSchemaChangeKind.ADDED) {
            return false;
        }
        return diff.getChanges().stream().noneMatch(candidate -> Boolean.TRUE.equals(candidate.getDdlRequired())
                && Objects.equals(ref.getModelId(), candidate.getObject().getModelId())
                && ((ref.getObjectKey().startsWith("MODEL:") && candidate.getObject().getObjectType() == ModelSchemaObjectType.TABLE)
                || (ref.getObjectKey().startsWith("FIELD:") && ref.getObjectKey().equals(candidate.getObject().getObjectKey()))));
    }

    /**
     * 汇总物理、元数据和停用变化。物理对象对应的普通配置副本不重复算作另一类变化。
     */
    private ModelPublishChangeStatus summarize(ModelPublishStructureDiff diff, boolean first) {
        if (first) {
            return ModelPublishChangeStatus.FIRST_PUBLISH;
        }
        boolean physical = diff.getChanges().stream().anyMatch(change -> Boolean.TRUE.equals(change.getDdlRequired())
                || change.getChangeKind() == ModelSchemaChangeKind.MODIFIED
                || change.getChangeKind() == ModelSchemaChangeKind.UNEXPECTED_MISSING);
        boolean disabled = diff.getChanges().stream().anyMatch(change -> change.getChangeKind() == ModelSchemaChangeKind.DISABLED
                || change.getChangeKind() == ModelSchemaChangeKind.LOGICALLY_DELETED);
        boolean metadata = diff.getChanges().stream().anyMatch(change -> change.getChangeKind() == ModelSchemaChangeKind.METADATA_ONLY
                || configurationAdditionOnly(change, diff));
        if ((physical && disabled) || (physical && metadata) || (disabled && metadata)) {
            return ModelPublishChangeStatus.MIXED;
        }
        return physical ? ModelPublishChangeStatus.STRUCTURE_CHANGE : disabled ? ModelPublishChangeStatus.DISABLE
                                                                      : metadata ? ModelPublishChangeStatus.METADATA_ONLY : ModelPublishChangeStatus.NO_CHANGE;
    }
}
