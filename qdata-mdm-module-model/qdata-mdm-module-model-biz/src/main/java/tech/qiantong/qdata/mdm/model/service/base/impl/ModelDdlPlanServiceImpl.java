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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbIndex;
import tech.qiantong.qdata.mdm.common.database.dialect.DM8Dialect;
import tech.qiantong.qdata.mdm.common.database.dialect.OracleDialect;
import tech.qiantong.qdata.mdm.common.database.dialect.SQLServer2008Dialect;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;
import tech.qiantong.qdata.mdm.model.domain.*;
import tech.qiantong.qdata.mdm.model.enums.materialization.*;
import tech.qiantong.qdata.mdm.model.service.base.*;
import tech.qiantong.qdata.mdm.model.util.ModelMaterializationDbUtils;
import tech.qiantong.qdata.mdm.model.util.ModelPublishDatabaseSession;
import tech.qiantong.qdata.mdm.model.util.ModelPublishTimingUtils;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * 统一选择DDL动作、生成计划、执行和恢复SQL、评估安全补偿，并在物理成功后保存正式结果。
 */
@Service
public class ModelDdlPlanServiceImpl implements IModelDdlPlanService {

    /**
     * 重新构建来源、权威主子表，并与上次成功结构比较，不信任调用方手工传入的差异。
     */
    @Resource
    private IModelSchemaService schemaService;

    /**
     * 执行发布各阶段校验、打开目标连接，并保存比较、执行和回撤记录。
     */
    @Resource
    private IModelPublishService modelPublishService;

    /**
     * 只有准备中的实例可以生成计划。零SQL发布同样执行TARGET，结果保存后仍不标记发布成功。
     * <p>
     * 阅读顺序：prepareStructureAndChanges构建并校验变更 → TARGET核实业务库 → selectActions选动作 → fillPhysicalTypes补方言类型 → appendAction拆SQL及核验条件 → 保存完整计划。
     */
    @Override
    public ModelPublishExecutionPlan preparePlan(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("PLAN", context);
        try {
            ModelInstanceDO record = new ModelInstanceDO();
            record.setId(context.getInstanceId());
            record.setModelId(context.getModelId());
            record.setModelVersionId(context.getModelVersionId());
            record.setDatasourceId(context.getDatasourceId());
            record.setBaseInstanceId(context.getBaseInstanceId());
            try {
                // 先构建、比较和校验变更；TARGET尚未确认物理表是否缺失，此时不提前生成动作列表。
                prepareStructureAndChanges(context);
                // 连接实际业务库完成TARGET校验，确认允许操作后才调用common生成最终SQL。
                modelPublishService.validate(ModelValidationStage.TARGET, context);
                // TARGET已排除实际丢失的表和字段，按实际存在情况重新选动作，不修改原DIFF和历史记录。
                List<ModelDdlAction> actions = selectActions(context, context.getExistingStructure());
                context.setCurrentStage(ModelPublishStage.PLAN);
                ModelPublishExecutionPlan plan = new ModelPublishExecutionPlan();
                // 先持久化完整计划，逐条执行结果暂存内存，结束或失败时统一写回。
                plan.setDeferredProgress(Boolean.TRUE);
                plan.setExistingStructure(copy(context.getExistingStructure(), ModelMaterializedStructure.class));
                // 使用独立结构，补充方言类型时不改变原DIFF或上次成功的结构。
                plan.setTargetStructure(JSON.parseObject(JSON.toJSONString(context.getTargetStructure()), ModelMaterializedStructure.class));
                try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
                    plan.setTarget(session.getTarget());
                    // 用目标方言补齐计划中的物理类型；执行成功后仍会重新读取数据库真实类型。
                    fillPhysicalTypes(session, plan.getTargetStructure());
                    Map<String, Integer> lastSteps = new LinkedHashMap<>();
                    // 按动作依赖逐个展开SQL；一个建表动作可能还包含后续表注释、字段注释等独立步骤。
                    for (ModelDdlAction action : actions) {
                        appendAction(context, session, plan, action, lastSteps);
                    }
                }

                // 完整计划一次保存：每条SQL、顺序、前后核验条件及安全补偿条件一起入库。
                modelPublishService.saveExecutionPlan(record, plan);
                // 只有数据库记录保存成功，才把计划交给后续执行器使用。
                context.setExecutionPlan(plan);
                return plan;
            } catch (RuntimeException ex) {
                context.setExecutionPlan(null);
                String reason = ex instanceof ServiceException ? ex.getMessage() : "目标校验或SQL计划生成失败，请检查目标数据库适配和连接";
                try {
                    modelPublishService.failPlanning(record, context.getCurrentStage(), reason);
                    context.setStatus(ModelPublishStatus.FAILED);
                } catch (RuntimeException recordFailure) {
                    ex.addSuppressed(recordFailure);
                    throw ex;
                }
                throw new ServiceException(reason);
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 新列补上方言类型，已有列保留上次成功信息；真实类型最终仍由执行后读取确认。
     */
    private void fillPhysicalTypes(ModelPublishDatabaseSession session, ModelMaterializedStructure structure) {
        for (ModelMaterializedTableDefinition table : structure.getTables()) {
            for (ModelMaterializedColumnDefinition column : table.getColumns()) {
                if (column.getPhysicalType() == null) {
                    column.setPhysicalType(ModelMaterializationDbUtils.physicalType(session, ModelMaterializationDbUtils.column(session, column)));
                }
            }
        }
    }

    /**
     * 一个动作拆成多条SQL，依赖动作的最后一步完成后才能开始本动作。
     * <p>
     * 一个动作可拆成结构和注释多步；条件定义保留供异常恢复、回滚使用，正常执行不逐条查询。
     */
    private void appendAction(ModelPublishContext context, ModelPublishDatabaseSession session, ModelPublishExecutionPlan plan,
                              ModelDdlAction action, Map<String, Integer> lastSteps) {
        // 把单个结构动作交给common方言转换；model不在这里拼各数据库的建表语法。
        List<String> statements = generate(session, action);
        if (statements == null || statements.isEmpty()) {
            throw new ServiceException("common没有返回可用SQL：" + action.getActionKey());
        }
        // 动作依赖转换成具体步骤号；依赖动作的最后一条SQL完成，当前动作才能开始。
        List<Integer> dependencies = new ArrayList<>();
        for (String key : action.getDependencyActionKeys()) {
            Integer step = lastSteps.get(key);
            if (step == null) {
                throw new ServiceException("SQL步骤依赖缺失：" + key);
            }
            dependencies.add(step);
        }
        boolean structuralFound = false;
        JSONObject defaultBefore = action.getBeforeDefinition();
        int defaultStatementNo = 0;
        if (action.getOperation() == ModelDdlOperation.ALTER_DEFAULT && statements.size() > 2) {
            throw new ServiceException("默认值动作只能包含单次修改，或移除旧默认约束后设置新默认约束");
        }
        for (String sql : statements) {
            if (sql == null || sql.trim().isEmpty()) {
                throw new ServiceException("common返回了空SQL，不能保存计划");
            }
            // JDBC接收单条SQL，移除方言附带的末尾分号；字符串里的分号仍保留。
            sql = sql.trim();
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1).trim();
            }
            // 区分结构SQL与注释SQL，分别保存恢复、补偿所需的计划内容。
            boolean comment = isComment(sql);
            // 长度调整不变更已有注释或默认值，避免重复新增扩展属性及重述无关约束。
            if (action.getOperation() == ModelDdlOperation.EXPAND_COLUMN && (comment || isConstraintRestatement(sql))) {
                continue;
            }
            if (!comment && structuralFound && action.getOperation() != ModelDdlOperation.ALTER_DEFAULT) {
                throw new ServiceException("单项动作返回了多条结构变更SQL，需先确认方言适配：" + action.getActionKey());
            }
            if (comment && !structuralFound) {
                throw new ServiceException("注释SQL不能排在对应结构SQL之前");
            }
            validateSql(action, sql, comment);
            // 每条SQL独立编号和保存状态，注释SQL也单独记录；不能只留一段拼接后的SQL字符串。
            ModelSqlExecutionStep step = new ModelSqlExecutionStep();
            step.setStepNo(plan.getSteps().size() + 1);
            step.setActionKey(action.getActionKey());
            step.setDependencyStepNos(new ArrayList<>(dependencies));
            step.setObject(copy(action.getObject(), ModelSchemaObjectRef.class));
            step.setOperation(comment ? ModelDdlOperation.COMMENT : action.getOperation());
            step.setForwardSql(sql.trim());
            condition(step.getPreconditions(), ModelVerificationType.TARGET_IDENTITY_MATCH, step.getObject(),
                    JSONObject.from(session.getTarget()));
            if (comment) {
                // 注释属于首次建表/新增列的附属步骤，业务中文修改不会走这里。
                JSONObject expectedComment = commentExpected(session, action, sql);
                ModelSchemaObjectRef commentObject = copy(step.getObject(), ModelSchemaObjectRef.class);
                // 建表动作也包含列注释，要检查本条SQL真正对应的列，不能只确认整张表存在。
                if ("COLUMN".equals(expectedComment.getString("commentType"))) {
                    commentObject.setObjectType(ModelSchemaObjectType.COLUMN);
                    commentObject.setColumnName(expectedComment.getString("columnName"));
                } else {
                    commentObject.setObjectType(ModelSchemaObjectType.TABLE);
                }
                // 结构SQL已在执行后核验；注释前只确认对象存在，避免反复读取全表字段、主键和索引。
                condition(step.getPreconditions(), ModelVerificationType.OBJECT_PRESENT, commentObject, new JSONObject());
                // 注释只根据SQL是否正常返回记录结果，不生成读取注释的后置条件。
                step.setIrreversibleReason("注释随本次新增对象保留，不单独反向修改");
            } else {
                structuralFound = true;
                JSONObject beforeStep = action.getOperation() == ModelDdlOperation.ALTER_DEFAULT ? defaultBefore : action.getBeforeDefinition();
                JSONObject afterStep = expectedAfter(action);
                if (action.getOperation() == ModelDdlOperation.ALTER_DEFAULT && ++defaultStatementNo < statements.size()) {
                    afterStep = copy(action.getAfterDefinition(), JSONObject.class);
                    afterStep.put("defaultValue", null);
                }
                condition(step.getPreconditions(), beforeStep == null
                                ? ModelVerificationType.OBJECT_ABSENT : ModelVerificationType.STRUCTURE_MATCH,
                        step.getObject(), beforeStep);
                condition(step.getPostconditions(), ModelVerificationType.STRUCTURE_MATCH, step.getObject(), afterStep);
                defaultBefore = afterStep;
                addDataConditions(step, action);
                compensation(context, session, action, step);
            }
            plan.getSteps().add(step);
            dependencies = new ArrayList<>(Collections.singletonList(step.getStepNo()));
        }
        if (!structuralFound) {
            throw new ServiceException("DDL动作缺少对应的结构SQL：" + action.getActionKey());
        }
        lastSteps.put(action.getActionKey(), plan.getSteps().size());
    }

    /**
     * 只调用common已经实现的单项接口，不调用会整表比较的通用更新。
     */
    private List<String> generate(ModelPublishDatabaseSession session, ModelDdlAction action) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("GENERATE_SQL", null, null);
        try {
            String table = action.getObject().getTableName();
            // 仅开放建表、加列、字符串扩长和建索引四类动作；停用和删除元数据不走物理删除路径。
            switch (action.getOperation()) {
                case CREATE_TABLE:
                    ModelMaterializedTableDefinition definition = action.getAfterDefinition().to(ModelMaterializedTableDefinition.class);
                    List<DbColumn> columns = new ArrayList<>();
                    for (ModelMaterializedColumnDefinition column : definition.getColumns()) {
                        columns.add(ModelMaterializationDbUtils.column(session, column));
                    }
                    return session.getQuery().generateCreateTableSQL(session.getProperty(), table, definition.getTableComment(), columns);
                case ADD_COLUMN:
                    DbColumn added = ModelMaterializationDbUtils.column(session, action.getAfterDefinition().to(ModelMaterializedColumnDefinition.class));
                    // 丢失的主键列先按普通列补回，主键约束由后续独立动作添加，避免方言重复创建。
                    added.setColKey(false);
                    return session.getDialect().addColumn(session.getProperty(), table, added);
                case EXPAND_COLUMN:
                    ModelMaterializedColumnDefinition before = action.getBeforeDefinition().to(ModelMaterializedColumnDefinition.class);
                    ModelMaterializedColumnDefinition after = action.getAfterDefinition().to(ModelMaterializedColumnDefinition.class);
                    if (!"STRING".equals(before.getLogicalType()) || !"STRING".equals(after.getLogicalType())
                            || before.getCharacterLength() == null || after.getCharacterLength() == null
                            || after.getCharacterLength() <= before.getCharacterLength() || Boolean.TRUE.equals(after.getPrimaryKeyFlag())) {
                        throw new ServiceException("扩长动作必须是普通字符串列扩大长度");
                    }
                    DbColumn expanded = ModelMaterializationDbUtils.column(session, after);
                    // SQL Server默认值是独立约束，原modify会重复ADD同名约束；扩长不重建它。
                    if (session.getDialect() instanceof SQLServer2008Dialect) {
                        expanded.setDataDefault(null);
                    }
                    return session.getDialect().modifyColumn(session.getProperty(), table, expanded);
                case ALTER_DEFAULT:
                    ModelMaterializedColumnDefinition changedDefault = action.getAfterDefinition().to(ModelMaterializedColumnDefinition.class);
                    if (changedDefault.getModelFieldId() == null) throw new ServiceException("系统字段默认值不能修改");
                    try (java.sql.Connection connection = session.getQuery().getConnection()) {
                        return session.getDialect().alterColumnDefault(session.getProperty(), table,
                                ModelMaterializationDbUtils.logicalColumn(changedDefault), connection);
                    } catch (java.sql.SQLException ex) {
                        throw new ServiceException("默认值变更SQL生成失败，请检查目标库连接和默认约束");
                    }
                case RELAX_COLUMN:
                    ModelMaterializedColumnDefinition required = action.getBeforeDefinition().to(ModelMaterializedColumnDefinition.class);
                    ModelMaterializedColumnDefinition optional = action.getAfterDefinition().to(ModelMaterializedColumnDefinition.class);
                    if (optional.getModelFieldId() == null || Boolean.TRUE.equals(optional.getPrimaryKeyFlag())
                            || !Boolean.FALSE.equals(required.getNullable()) || !Boolean.TRUE.equals(optional.getNullable())) {
                        throw new ServiceException("放宽必填动作必须是普通业务列从非空改为可空");
                    }
                    DbColumn relaxed = ModelMaterializationDbUtils.column(session, optional);
                    // 达梦modifyColumn仅省略NOT NULL，不会撤销旧约束，必须明确生成NULL。
                    if (session.getDialect() instanceof DM8Dialect) {
                        return Collections.singletonList("ALTER TABLE "
                                + session.getDialect().getTableName(session.getProperty(), table)
                                + " MODIFY " + session.getDialect().quoteIdentifier(relaxed.getColName())
                                + " " + session.getDialect().getColumnType(relaxed) + " NULL");
                    }
                    // Oracle需要旧列参与比较，才能在从非空改为可空时追加NULL。
                    if (session.getDialect() instanceof OracleDialect) {
                        return ((OracleDialect) session.getDialect()).modifyColumn(session.getProperty(), table,
                                relaxed, ModelMaterializationDbUtils.column(session, required));
                    }
                    if (session.getDialect() instanceof SQLServer2008Dialect) {
                        relaxed.setDataDefault(null);
                    }
                    List<String> relaxSql = session.getDialect().modifyColumn(session.getProperty(), table, relaxed);
                    // PostgreSQL类方言拆分类型、默认值、可空语句，这里只执行放宽可空的那一条。
                    if (relaxSql != null) {
                        List<String> nullableSql = relaxSql.stream().filter(sql -> sql != null
                                        && Pattern.compile("(?i)\\bDROP\\s+NOT\\s+NULL\\s*;?\\s*$").matcher(sql).find())
                                .collect(Collectors.toList());
                        if (!nullableSql.isEmpty()) return nullableSql;
                    }
                    return relaxSql;
                case CREATE_INDEX:
                    ModelMaterializedIndexDefinition index = action.getAfterDefinition().to(ModelMaterializedIndexDefinition.class);
                    if (index.getIndexType() == ModelIndexType.PRIMARY_KEY) {
                        // 只取common返回的新增主键语句，丢弃它用于更换主键的DROP语句，修复时不删除其他约束。
                        List<DbColumn> primaryColumns = index.getColumnNames().stream()
                                .map(name -> DbColumn.builder().colName(name).colKey(true).build()).collect(Collectors.toList());
                        List<String> primarySql = session.getDialect().updateColKey(session.getProperty(), table, primaryColumns);
                        if (primarySql == null) {
                            throw new ServiceException("common当前方言尚未提供补建缺失主键的SQL");
                        }
                        List<String> additions = primarySql.stream().filter(sql -> sql != null
                                && sql.trim().toUpperCase(Locale.ROOT).startsWith("ALTER TABLE ")
                                && sql.toUpperCase(Locale.ROOT).contains(" ADD ")
                                && sql.toUpperCase(Locale.ROOT).contains(" PRIMARY KEY")
                                && !Pattern.compile("(?i)\\bDROP\\b").matcher(sql).find()).collect(Collectors.toList());
                        if (additions.size() != 1) throw new ServiceException("common没有返回明确的新增主键SQL");
                        return additions;
                    }
                    // 整表主键由CREATE承载；其他索引沿用common独立建索引能力。
                    DbIndex dbIndex = DbIndex.builder().indexName(index.getIndexName()).primaryKey(false)
                            .unique(index.getIndexType() == ModelIndexType.UNIQUE).columnNames(index.getColumnNames()).build();
                    return Collections.singletonList(session.getQuery().generateCreateIndexSQL(session.getProperty(), table, dbIndex));
                default:
                    throw new ServiceException("本次发布不允许该DDL动作");
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 建表完成时只有CREATE内的主键存在，业务唯一和普通索引要等后续步骤再核验。
     * 不修改原动作和完整目标结构，仅缩小本条SQL应完成的范围。
     */
    private JSONObject expectedAfter(ModelDdlAction action) {
        if (action.getOperation() != ModelDdlOperation.CREATE_TABLE) {
            return action.getAfterDefinition();
        }
        ModelMaterializedTableDefinition table = action.getAfterDefinition().to(ModelMaterializedTableDefinition.class);
        // 当前步骤只核验它自己创建的内容；后续唯一索引尚未执行，不能提前要求它已经存在。
        table.getIndexes().removeIf(index -> index.getIndexType() != ModelIndexType.PRIMARY_KEY);
        return JSONObject.from(table);
    }

    /**
     * 保存新增必填和唯一字段的原条件，供异常恢复和回滚核对；正常执行前的数据校验统一在TARGET完成。
     */
    private void addDataConditions(ModelSqlExecutionStep step, ModelDdlAction action) {
        // 保留原条件定义，但执行器不再在每条SQL发送前重复查询。
        if (action.getOperation() == ModelDdlOperation.ADD_COLUMN) {
            ModelMaterializedColumnDefinition column = action.getAfterDefinition().to(ModelMaterializedColumnDefinition.class);
            if (Boolean.FALSE.equals(column.getNullable()) && column.getDefaultValue() == null) {
                condition(step.getPreconditions(), ModelVerificationType.NO_DATA, step.getObject(), new JSONObject());
            }
        } else if (action.getOperation() == ModelDdlOperation.CREATE_INDEX
                && action.getAfterDefinition().to(ModelMaterializedIndexDefinition.class).getIndexType() == ModelIndexType.UNIQUE) {
            condition(step.getPreconditions(), ModelVerificationType.NO_DUPLICATE_VALUES, step.getObject(), expectedAfter(action));
        }
    }

    /**
     * 只给本次新表和新索引准备受条件约束的补偿；旧表加列与扩长没有DROP或缩短补偿。
     * 这里只保存候选语句，执行前必须核对本次创建证据、未正式生效、空表和实际结构。
     */
    private void compensation(ModelPublishContext context, ModelPublishDatabaseSession session, ModelDdlAction action,
                              ModelSqlExecutionStep step) {
        // 这里是失败时的候选撤销方案，不是模型停用/删除流程；真正执行前还要验证归属、空表等安全条件。
        if (action.getOperation() == ModelDdlOperation.CREATE_TABLE) {
            step.setCompensationSql(session.getDialect().generateDropTableSQL(session.getProperty(),
                    session.getDialect().getTableName(session.getProperty(), action.getObject().getTableName())));
        } else if (action.getOperation() == ModelDdlOperation.CREATE_INDEX
                && action.getAfterDefinition().to(ModelMaterializedIndexDefinition.class).getIndexType() == ModelIndexType.PRIMARY_KEY) {
            step.setIrreversibleReason("已有表补回主键后保留，不通过删除主键回撤");
            return;
        } else if (action.getOperation() == ModelDdlOperation.CREATE_INDEX) {
            step.setCompensationSql(session.getQuery().generateDropIndexSQL(session.getProperty(),
                    action.getObject().getTableName(), action.getObject().getIndexName()));
        } else {
            step.setIrreversibleReason("已有表新增列、扩大长度、放宽必填或修改默认值后保留真实效果，按原实例核验恢复");
            return;
        }
        if (step.getCompensationSql() == null || step.getCompensationSql().trim().isEmpty()) {
            throw new ServiceException("本次新增对象没有可用补偿SQL");
        }
        condition(step.getCompensationPreconditions(), ModelVerificationType.TARGET_IDENTITY_MATCH, step.getObject(), JSONObject.from(session.getTarget()));
        condition(step.getCompensationPreconditions(), ModelVerificationType.CREATED_BY_INSTANCE, step.getObject(),
                JSONObject.of("instanceId", context.getInstanceId(), "stepNo", step.getStepNo(), "notCommitted", true));
        condition(step.getCompensationPreconditions(), ModelVerificationType.STRUCTURE_MATCH, step.getObject(), expectedAfter(action));
        condition(step.getCompensationPreconditions(), ModelVerificationType.NO_DATA, step.getObject(), new JSONObject());
        condition(step.getCompensationPostconditions(), ModelVerificationType.OBJECT_ABSENT, step.getObject(), new JSONObject());
    }

    /**
     * 核验参数使用独立JSON，后续改步骤状态不会污染动作定义。
     */
    private void condition(List<ModelVerificationCondition> conditions, ModelVerificationType type,
                           ModelSchemaObjectRef object, JSONObject expected) {
        ModelVerificationCondition condition = new ModelVerificationCondition();
        condition.setConditionKey(type.name() + ":" + (conditions.size() + 1));
        condition.setType(type);
        condition.setObject(copy(object, ModelSchemaObjectRef.class));
        condition.setExpected(expected == null ? new JSONObject() : copy(expected, JSONObject.class));
        conditions.add(condition);
    }

    /**
     * 识别建表/加列附带的注释语句；不把它们误记成第二次建表或加列。
     */
    private boolean isComment(String sql) {
        return sql.trim().toUpperCase(Locale.ROOT).startsWith("COMMENT ON ")
                || sql.trim().matches("(?is)^EXEC\\s+(?:sys\\.)?sp_(?:add|update)extendedproperty\\b.*");
    }

    /**
     * 原modifyColumn附带的默认值和可空重述无需重复发送，扩长只保留类型语句。
     */
    private boolean isConstraintRestatement(String sql) {
        return Pattern.compile("(?is)\\bALTER\\s+COLUMN\\s+[^\\s]+\\s+(?:SET\\s+NOT\\s+NULL|DROP\\s+NOT\\s+NULL|SET\\s+DEFAULT|DROP\\s+DEFAULT)\\b")
                .matcher(sql).find();
    }

    /**
     * 只允许单条获准操作，防止旧方言混入删表、删列、重建主键或多语句脚本。
     */
    private void validateSql(ModelDdlAction action, String sql, boolean comment) {
        String structure = sql.replaceAll("'(?:''|[^'])*'", "''").trim();
        if (action.getOperation() == ModelDdlOperation.RELAX_COLUMN) {
            structure = structure.replaceFirst("(?i)\\bDROP\\s+NOT\\s+NULL\\s*$", "");
        }
        if (action.getOperation() == ModelDdlOperation.ALTER_DEFAULT) {
            structure = ModelMaterializationDbUtils.approvedDefaultSql(structure);
        }
        if (structure.contains(";") || Pattern.compile("(?is)\\b(?:DROP|TRUNCATE|RENAME)\\b").matcher(structure).find()) {
            throw new ServiceException("生成SQL包含未获准操作，不能保存执行计划");
        }
        if (comment) {
            return;
        }
        boolean restorePrimary = action.getOperation() == ModelDdlOperation.CREATE_INDEX
                && action.getAfterDefinition().to(ModelMaterializedIndexDefinition.class).getIndexType() == ModelIndexType.PRIMARY_KEY;
        String prefix = restorePrimary ? "ALTER TABLE "
                : action.getOperation() == ModelDdlOperation.CREATE_INDEX ? "CREATE " : "ALTER TABLE ";
        boolean valid = action.getOperation() == ModelDdlOperation.CREATE_TABLE
                ? ModelMaterializationDbUtils.isCreateTableSql(structure)
                : structure.toUpperCase(Locale.ROOT).startsWith(prefix);
        if (!valid) {
            throw new ServiceException("生成SQL与批准动作不一致");
        }
        if (action.getOperation() != ModelDdlOperation.CREATE_TABLE && !restorePrimary
                && Pattern.compile("(?is)\\bPRIMARY\\s+KEY\\b").matcher(structure).find()) {
            throw new ServiceException("已有表不能通过字段修改新增或重建主键");
        }
    }

    /**
     * JSON复制用于把上下文、动作和计划分开保存，不能共用可变对象。
     */
    private <T> T copy(Object value, Class<T> type) {
        return JSON.parseObject(JSON.toJSONString(value), type);
    }

    /**
     * 从common返回的注释语句中确定本条作用的对象，仅保存期望注释，不让核验器重放SQL。
     */
    private JSONObject commentExpected(ModelPublishDatabaseSession session, ModelDdlAction action, String sql) {
        String tableName = action.getObject().getTableName();
        String fullName = session.getDialect().getTableName(session.getProperty(), tableName);
        ModelMaterializedTableDefinition table = action.getOperation() == ModelDdlOperation.CREATE_TABLE
                ? action.getAfterDefinition().to(ModelMaterializedTableDefinition.class) : null;
        boolean sqlServer = sql.trim().matches("(?is)^EXEC\\s+(?:sys\\.)?sp_(?:add|update)extendedproperty\\b.*");
        if (table != null && (sql.startsWith("COMMENT ON TABLE " + fullName + " IS ")
                || (sqlServer && tableName.equals(commentParameter(sql, "level1name"))
                && commentParameter(sql, "level2name") == null))) {
            return JSONObject.of("commentType", "TABLE", "comment", table.getTableComment());
        }
        List<ModelMaterializedColumnDefinition> columns = table == null
                ? Collections.singletonList(action.getAfterDefinition().to(ModelMaterializedColumnDefinition.class)) : table.getColumns();
        for (ModelMaterializedColumnDefinition column : columns) {
            String name = column.getColumnName();
            // 部分旧方言生成未加引号的列名，两种写法都按已批准的固定列名匹配。
            if (sql.startsWith("COMMENT ON COLUMN " + fullName + "." + session.getDialect().quoteIdentifier(name) + " IS ")
                    || sql.startsWith("COMMENT ON COLUMN " + fullName + "." + name + " IS ")
                    || (sqlServer && tableName.equals(commentParameter(sql, "level1name"))
                    && name.equals(commentParameter(sql, "level2name")))) {
                return JSONObject.of("commentType", "COLUMN", "columnName", name, "comment", column.getColumnComment());
            }
        }
        throw new ServiceException("无法确定common注释SQL对应的表或字段，不能保存核验计划");
    }

    /**
     * SQL Server注释参数是SQL字面量；这里只识别common生成的对象参数，不把内容作为代码执行。
     */
    private String commentParameter(String sql, String parameter) {
        java.util.regex.Matcher matcher = Pattern.compile("(?is)@([A-Za-z0-9_]+)\\s*=\\s*N?'((?:''|[^'])*)'|'(?:''|[^'])*'").matcher(sql);
        while (matcher.find()) {
            // 跳过完整字符串字面量，注释内容里即使出现@level2name也不能误认成对象参数。
            if (parameter.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2).replace("''", "'");
            }
        }
        return null;
    }

    /**
     * 执行平台保存的计划。这里只做首次执行，不接恢复、重试或补偿。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ModelPublishExecutionPlan executePlan(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("EXECUTE", context);
        try {
            if (context == null || context.getModelId() == null || context.getInstanceId() == null) {
                throw new ServiceException("执行SQL缺少发布实例和主模型信息");
            }

            ModelInstanceDO record = new ModelInstanceDO();
            record.setId(context.getInstanceId());
            record.setModelId(context.getModelId());
            record.setModelVersionId(context.getModelVersionId());
            record.setDatasourceId(context.getDatasourceId());
            record.setBaseInstanceId(context.getBaseInstanceId());
            // 不信任context里可能过期或被改动的计划，执行SQL只能来自平台已保存的版本。
            // 通过发布服务的独立事务回读原计划、登记执行开始；平台保存失败时不能先发业务库SQL。
            ModelPublishExecutionPlan plan = modelPublishService.beginExecution(record);
            context.setExecutionPlan(plan);
            context.setVerifiedStructure(null);
            context.setTarget(plan.getTarget());
            context.setStatus(ModelPublishStatus.EXECUTING);
            context.setCurrentStage(ModelPublishStage.EXECUTE);
            return executeSteps(context, record, plan, false);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 恢复使用原实例、原配置和原SQL，完成目标核验、重试及结果保存。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ModelPublishContext recoverPlan(Long modelId, Long instanceId) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("RECOVER_PLAN", modelId, instanceId);
        try {
            if (modelId == null || instanceId == null) {
                throw new ServiceException("恢复发布缺少主模型ID或实例ID");
            }
            ModelPublishContext context = modelPublishService.beginRecovery(modelId, instanceId);
            if (Boolean.TRUE.equals(context.getExecutionPlan().getDeferredProgress())) {
                return recoverDeferredPlan(context);
            }
            // 原计划曾进入整批DDL事务时，改走事务结果核验；不能换新连接逐条重发原事务里的SQL。
            if (context.getExecutionPlan().getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE) {
                return recoverDdlTransaction(context);
            }
            ModelInstanceDO record = new ModelInstanceDO();
            record.setId(context.getInstanceId());
            record.setModelId(context.getModelId());
            record.setModelVersionId(context.getModelVersionId());
            record.setDatasourceId(context.getDatasourceId());
            record.setBaseInstanceId(context.getBaseInstanceId());
            executeSteps(context, record, context.getExecutionPlan(), true);
            return context;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 只在重新发布发现未完成的新计划时核对真实结构；正常发布不增加这轮查询。
     * 结构已经符合要求就跳过，缺失对象继续执行，存在但定义不符的对象不盲目覆盖。
     */
    private ModelPublishContext recoverDeferredPlan(ModelPublishContext context) {
        ModelPublishExecutionPlan plan = context.getExecutionPlan();
        ModelInstanceDO record = new ModelInstanceDO();
        record.setId(context.getInstanceId());
        record.setModelId(context.getModelId());
        record.setModelVersionId(context.getModelVersionId());
        record.setDatasourceId(context.getDatasourceId());
        record.setBaseInstanceId(context.getBaseInstanceId());
        if (plan.getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE) {
            if ((plan.getDdlTransactionStatus() == ModelDdlTransactionStatus.ACTIVE
                    || plan.getDdlTransactionStatus() == ModelDdlTransactionStatus.ROLLING_BACK
                    || plan.getDdlTransactionStatus() == ModelDdlTransactionStatus.COMMITTING) && rollbackMatches(context)) {
                // 进程停止后，原事务已经由数据库撤销；核实无残留才能按原计划重新开始事务。
                for (ModelSqlExecutionStep step : plan.getSteps()) {
                    step.setStatus(ModelSqlStepStatus.PENDING);
                    step.setRecoveredEffect(Boolean.FALSE);
                    step.setFailureReason(null);
                    step.setLastAttemptCompleted(step.getAttemptCount() > 0);
                    step.setLastAttemptRetryable(step.getAttemptCount() > 0);
                }
                plan.setDdlTransactionStatus(ModelDdlTransactionStatus.NONE);
                plan.setRecoveryReconciled(Boolean.TRUE);
                plan.setRollbackVerified(Boolean.TRUE);
                persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.EXECUTING,
                        "已核实原DDL事务回滚完成，使用原计划重新执行");
                executeSteps(context, record, plan, false);
                return context;
            }
            // 提交阶段已经有统一保存的完成记录，沿用原事务结果核验，不能在新连接假装回滚旧事务。
            return recoverDdlTransaction(context);
        }
        try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
            ModelVerificationCondition identity = new ModelVerificationCondition();
            identity.setConditionKey("RECOVERY_TARGET_IDENTITY");
            identity.setType(ModelVerificationType.TARGET_IDENTITY_MATCH);
            identity.setExpected(JSONObject.from(plan.getTarget()));
            if (!modelPublishService.verifyConditions(context, session, Collections.singletonList(identity))) {
                throw new ServiceException("原发布目标已变化，不能使用其他数据库继续恢复");
            }
            Map<String, Boolean> tableExists = new HashMap<>();
            for (ModelSqlExecutionStep step : plan.getSteps()) {
                String tableName = step.getObject().getTableName();
                Boolean exists = tableExists.get(tableName);
                if (exists == null) {
                    int count = session.getQuery().generateCheckTableExistsSQL(session.getProperty(), tableName);
                    if (count < 0 || count > 1) throw new ServiceException("恢复时无法确认目标表是否存在：" + tableName);
                    exists = count == 1;
                    tableExists.put(tableName, exists);
                }
                // 注释不读取内容作比对；丢失的内存结果无法还原时，重新写入原计划中的注释。
                if (step.getOperation() == ModelDdlOperation.COMMENT) {
                    if (step.getStatus() != ModelSqlStepStatus.SUCCEEDED || !exists) resetDeferredStep(step);
                    continue;
                }
                boolean present = exists && !ModelMaterializationDbUtils.objectAbsent(session, step.getObject());
                if (present && (modelPublishService.verifyConditions(context, session, step.getPostconditions())
                        || defaultActionFinished(context, session, plan, step))) {
                    // 记录“恢复确认效果存在”，不伪造原进程已经丢失的SQL次数和时间。
                    step.setStatus(ModelSqlStepStatus.SUCCEEDED);
                    step.setRecoveredEffect(Boolean.TRUE);
                    step.setFailureReason(null);
                    recordDeferredStep(plan, step, "中断恢复：已核实预期结构存在，跳过本条SQL", "RECOVERY");
                } else if (!present || ((step.getOperation() == ModelDdlOperation.EXPAND_COLUMN
                        || step.getOperation() == ModelDdlOperation.RELAX_COLUMN
                        || step.getOperation() == ModelDdlOperation.ALTER_DEFAULT)
                        && modelPublishService.verifyConditions(context, session, copyConditions(step.getPreconditions())))) {
                    if (!exists && ModelMaterializationDbUtils.namedObjectExists(session, tableName)) {
                        throw new ServiceException("恢复时发现同名非表对象，不能继续建表：" + tableName);
                    }
                    resetDeferredStep(step);
                    recordDeferredStep(plan, step, "中断恢复：预期效果尚未生效，将按原计划继续执行", "RECOVERY");
                } else {
                    throw new ServiceException("恢复时发现对象已存在但结构与原计划不符：" + tableName + "/步骤" + step.getStepNo());
                }
            }
            plan.setRecoveryReconciled(Boolean.TRUE);
            // 恢复检查结果先统一落库。随后中断仍能再次读取原实例，正常发布不经过这里。
            persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.EXECUTING,
                    "原计划实际结构已核实，已存在对象跳过，缺失对象和未确认注释继续执行");
        } catch (SQLException | RuntimeException ex) {
            context.setStatus(ModelPublishStatus.UNKNOWN);
            throw new ServiceException("中断恢复未完成：" + ex.getMessage());
        }
        executeSteps(context, record, plan, false);
        return context;
    }

    /**
     * 只重置本次需要继续的步骤，保留已经记录的历史尝试次数。
     */
    private void resetDeferredStep(ModelSqlExecutionStep step) {
        step.setStatus(ModelSqlStepStatus.PENDING);
        step.setRecoveredEffect(Boolean.FALSE);
        step.setFailureReason(null);
        // 原尝试的返回标记不改写；真正重发时再增加次数并记录新的返回结果。
    }

    /**
     * 普通执行的事件仅记内存，最终由平台短事务统一追加到MODEL_INSTANCE_LOG。
     */
    private void recordDeferredStep(ModelPublishExecutionPlan plan, ModelSqlExecutionStep step, String message) {
        recordDeferredStep(plan, step, message, "FORWARD");
    }

    /**
     * 记录本次实际发生的时间和证据；恢复核实与真正发送SQL使用不同方向标记。
     */
    private void recordDeferredStep(ModelPublishExecutionPlan plan, ModelSqlExecutionStep step, String message, String direction) {
        Date now = new Date();
        JSONObject evidence = JSONObject.of("stepNo", step.getStepNo(), "operation", step.getOperation(),
                "direction", direction, "attempt", step.getAttemptCount(), "status", step.getStatus());
        evidence.put("lastAttemptCompleted", step.getLastAttemptCompleted());
        evidence.put("lastAttemptRetryable", step.getLastAttemptRetryable());
        evidence.put("recoveredEffect", step.getRecoveredEffect());
        evidence.put("reason", step.getFailureReason());
        JSONObject event = JSONObject.of("stage", "RECOVERY".equals(direction) ? ModelPublishStage.VERIFY : ModelPublishStage.EXECUTE,
                "status", ModelPublishStatus.EXECUTING, "startedTime", step.getCheckStartedTime() == null ? now : step.getCheckStartedTime(),
                "finishedTime", now, "content", JSONObject.of("message", message, "steps", Collections.singletonList(evidence)));
        plan.getExecutionEvents().add(event);
    }

    /**
     * SQL Server的新增注释不能重复添加；恢复时用同一份参数判断属性是否存在，再新增或更新。
     * 只处理恢复场景，不查询注释内容，不改写原计划SQL及其他数据库的执行语句。
     */
    private String recoveryForwardSql(ModelPublishDatabaseSession session, ModelSqlExecutionStep step) {
        String sql = step.getForwardSql();
        if (step.getOperation() != ModelDdlOperation.COMMENT || !(session.getDialect() instanceof SQLServer2008Dialect)
                || !sql.toLowerCase(Locale.ROOT).contains("sp_addextendedproperty")) return sql;
        String name = commentParameter(sql, "name");
        String schema = commentParameter(sql, "level0name");
        String table = commentParameter(sql, "level1name");
        String column = commentParameter(sql, "level2name");
        if (name == null || schema == null || table == null)
            throw new ServiceException("恢复注释缺少属性名或表定位信息");
        String location = "N'" + name.replace("'", "''") + "', N'SCHEMA', N'" + schema.replace("'", "''")
                + "', N'TABLE', N'" + table.replace("'", "''") + "', "
                + (column == null ? "NULL, NULL" : "N'COLUMN', N'" + column.replace("'", "''") + "'");
        String update = sql.replaceFirst("(?i)sp_addextendedproperty", "sp_updateextendedproperty");
        return "IF EXISTS (SELECT 1 FROM fn_listextendedproperty(" + location + ")) BEGIN " + update
                + "; END ELSE BEGIN " + sql + "; END";
    }

    /**
     * 正式保存复用本次整批核验结果；跨请求补保存才重新采集，不重发已完成的DDL。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ModelPublishContext commitPlan(ModelPublishContext execution) {
        Long modelId = execution == null ? null : execution.getModelId();
        Long instanceId = execution == null ? null : execution.getInstanceId();
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("COMMIT", modelId, instanceId);
        try {
            if (modelId == null || instanceId == null) {
                throw new ServiceException("保存缺少主模型ID或实例ID");
            }
            ModelPublishContext context = modelPublishService.readPendingCommit(modelId, instanceId);
            if (context.getStatus() == ModelPublishStatus.SUCCEEDED) {
                return context;
            }
            try {
                // 同一次发布复用整批核验结果，不在正式保存前再查一遍；跨请求补保存没有该结果时才重新采集。
                ModelMaterializedStructure actual = execution.getVerifiedStructure();
                if (actual == null) {
                    try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
                        actual = ModelMaterializationDbUtils.readActualStructure(session, context.getExecutionPlan().getTargetStructure());
                    }
                }
                // 通过代理开启平台独立事务，把已核验结构和模型状态一起提交，不把业务库DDL包进平台事务。
                ModelPublishTimingUtils.ModelPublishTiming commitTiming = ModelPublishTimingUtils.start("COMMIT_PLATFORM_TX", context);
                try {
                    modelPublishService.completeSuccess(context, actual);
                } catch (RuntimeException | Error ex) {
                    commitTiming.failed(ex);
                    throw ex;
                } finally {
                    commitTiming.close();
                }
                context.setVerifiedStructure(actual);
                context.setStatus(ModelPublishStatus.SUCCEEDED);
                context.setCurrentStage(ModelPublishStage.COMMIT);
                return context;
            } catch (Exception ex) {
                // 提交返回异常不一定代表事务没成功，先重新查原实例，避免把已成功结果改成失败。
                try {
                    ModelPublishContext stored = modelPublishService.readPendingCommit(modelId, instanceId);
                    if (stored.getStatus() == ModelPublishStatus.SUCCEEDED) {
                        return stored;
                    }
                    modelPublishService.saveCommitFailure(modelId, instanceId);
                } catch (RuntimeException recordFailure) {
                    ex.addSuppressed(recordFailure);
                }
                throw new ServiceException("正式物化结果保存未完成，请核对原实例后补保存");
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 补偿按SQL逆序逐条评估；某个对象未处理完时仍继续评估前面的对象，保留各自结果。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ModelPublishContext compensatePlan(Long modelId, Long instanceId) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("COMPENSATE", modelId, instanceId);
        try {
            if (modelId == null || instanceId == null) {
                throw new ServiceException("缺少主模型ID或实例ID");
            }
            ModelPublishContext context = modelPublishService.beginCompensation(modelId, instanceId);
            if (context.getStatus() == ModelPublishStatus.FAILED) {
                return context;
            }
            ModelInstanceDO record = new ModelInstanceDO();
            record.setId(instanceId);
            record.setModelId(modelId);
            record.setModelVersionId(context.getModelVersionId());
            record.setDatasourceId(context.getDatasourceId());
            record.setBaseInstanceId(context.getBaseInstanceId());
            ModelPublishExecutionPlan plan = context.getExecutionPlan();
            try {
                // 按相反顺序撤销：先处理依赖对象，再处理它依赖的新表；不是把每条正向SQL机械地反写。
                for (int index = plan.getSteps().size() - 1; index >= 0; index--) {
                    ModelSqlExecutionStep step = plan.getSteps().get(index);
                    if (step.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED) {
                        // 可能在表已删、附属步骤尚未记完时中断；恢复只补这些记录，不重复DROP表。
                        if (step.getOperation() == ModelDdlOperation.CREATE_TABLE) {
                            completeRemovedTableSteps(context, record, step);
                        }
                        continue;
                    }
                    if (step.getAttemptCount() == 0 && !Boolean.TRUE.equals(step.getRecoveredEffect())) {
                        step.setCompensationFailureReason("正向SQL没有发送意图，无需补偿");
                        saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), "记录未发送步骤");
                    } else {
                        // 每个对象分别判断是否可撤销；一条未处理完，也继续处理前面已成功创建的对象。
                        compensateStep(context, record, step);
                    }
                    if (step.getOperation() == ModelDdlOperation.CREATE_TABLE
                            && step.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED) {
                        completeRemovedTableSteps(context, record, step);
                    }
                }
                List<ModelPublishRemainingEffect> effects = readRemainingEffects(context);
                // 先判断结果是否仍未知，再判断有没有明确保留的效果；只有确认没有残留才登记终结失败。
                boolean unknown = unfinishedTimetempt(plan) || effects.stream()
                        .anyMatch(effect -> effect.getVerificationStatus() == ModelVerificationStatus.UNKNOWN);
                boolean retained = plan.getSteps().stream().anyMatch(step -> step.getCompensationStatus() == ModelCompensationStatus.RETAINED
                        || step.getCompensationStatus() == ModelCompensationStatus.FAILED);
                ModelPublishStatus status = unknown ? ModelPublishStatus.UNKNOWN
                        : !effects.isEmpty() || retained ? ModelPublishStatus.PARTIAL_FAILED : ModelPublishStatus.FAILED;
                saveCompensation(context, record, status, effects, status == ModelPublishStatus.FAILED
                        ? "补偿核验完成，本次发布失败且已确认没有未处理物理效果"
                        : "本次发布失败，已保存不能清理或尚不能确认的实际效果，继续阻止新发布");
                return context;
            } catch (RuntimeException ex) {
                try {
                    ModelPublishRemainingEffect effect = new ModelPublishRemainingEffect();
                    effect.setVerificationStatus(ModelVerificationStatus.UNKNOWN);
                    effect.setReason("补偿执行或记录保存中断，须重新读取原实例与实际效果，不能直接重发逆向SQL");
                    saveCompensation(context, record, ModelPublishStatus.UNKNOWN, Collections.singletonList(effect), effect.getReason());
                } catch (RuntimeException recordFailure) {
                    if (ex != recordFailure) {
                        ex.addSuppressed(recordFailure);
                    }
                }
                throw new ServiceException("物化补偿未正常结束，请先核对原实例及实际效果");
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 任一正向或逆向尝试缺少结束证据，都不能继续删除其他对象。
     */
    private boolean unfinishedTimetempt(ModelPublishExecutionPlan plan) {
        return plan.getSteps().stream().anyMatch(step -> (step.getAttemptCount() > 0 && !Boolean.TRUE.equals(step.getLastAttemptCompleted())
                && !Boolean.TRUE.equals(step.getRecoveredEffect()))
                || (step.getCompensationAttemptCount() > 0 && !Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted())));
    }

    /**
     * 先确认对象是否还存在，再核对归属、数据及结构；返回false表示本轮不再发送后续补偿SQL。
     */
    private boolean compensateStep(ModelPublishContext context, ModelInstanceDO record, ModelSqlExecutionStep step) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startStep("COMPENSATE_STEP", context, step);
        try {
            try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
                if (!step.getCompensationPostconditions().isEmpty()
                        && modelPublishService.verifyConditions(context, session, step.getCompensationPostconditions())) {
                    step.setCompensationStatus(ModelCompensationStatus.SUCCEEDED);
                    step.setCompensationFailureReason("真实对象已不存在，不重复发送补偿SQL");
                    saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), step.getCompensationFailureReason());
                    return true;
                }
                if (step.getCompensationSql() == null || step.getCompensationSql().trim().isEmpty()) {
                    // 明确未生效的失败步骤仍要真实核对原前置，不能因为没有逆向SQL就声称已经清理。
                    List<ModelVerificationCondition> before = copyConditions(step.getPreconditions());
                    if (step.getStatus() != ModelSqlStepStatus.SUCCEEDED && modelPublishService.verifyConditions(context, session, before)) {
                        step.setCompensationStatus(ModelCompensationStatus.NOT_REQUIRED);
                        step.setCompensationFailureReason("上次尝试已结束，原前置仍成立，没有需要撤销的本次效果");
                        saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), step.getCompensationFailureReason());
                    } else {
                        retainCompensation(context, record, step, step.getIrreversibleReason() == null
                                ? "本步骤没有安全逆向SQL，保留实际效果" : step.getIrreversibleReason());
                    }
                    return true;
                }
                // 本次新表里的索引跟随整表评估；表需要保留时不先破坏它的索引结构。
                boolean newTableIndex = step.getOperation() == ModelDdlOperation.CREATE_INDEX && context.getExecutionPlan().getSteps().stream()
                        .anyMatch(other -> other.getOperation() == ModelDdlOperation.CREATE_TABLE
                                && (other.getAttemptCount() > 0 || Boolean.TRUE.equals(other.getRecoveredEffect()))
                                && Objects.equals(other.getObject().getTableName(), step.getObject().getTableName()));
                if (newTableIndex) {
                    retainCompensation(context, record, step, "本次新表内的索引跟随整表评估，表保留时不单独删除其索引");
                    return true;
                }
                if (step.getCompensationAttemptCount() > 0 && (!Boolean.TRUE.equals(step.getCompensationLastAttemptRetryable())
                        || step.getCompensationAttemptCount() >= 1 + context.getExecutionPlan().getCompensationMaxRetries())) {
                    retainCompensation(context, record, step, "补偿错误不允许重试或已达到保存计划的重试上限，保留效果");
                    return true;
                }
                // 先检查计划要求的安全条件没有缺项，再逐项查真实归属、结构和表数据，全部通过才允许撤销。
                assertCompensationConditions(context, step);
                if (!modelPublishService.verifyConditions(context, session, step.getCompensationPreconditions())) {
                    String reason = step.getCompensationPreconditions().stream()
                            .filter(condition -> condition.getStatus() != ModelVerificationStatus.SATISFIED)
                            .map(condition -> condition.getType().name() + "：" + condition.getFailureReason()).findFirst().orElse("安全条件未通过");
                    retainCompensation(context, record, step, "不满足安全补偿条件，保留对象；" + reason);
                    return true;
                }
                String sql = ModelMaterializationDbUtils.compensationSql(session, step);
                step.setCompensationStatus(ModelCompensationStatus.RUNNING);
                saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), "补偿安全条件通过，记录本条开始，尚未发送");
                step.setCompensationAttemptCount(step.getCompensationAttemptCount() + 1);
                step.setCompensationLastAttemptCompleted(false);
                step.setCompensationLastAttemptRetryable(false);
                saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), "补偿发送意图已保存，准备发送单条逆向SQL");

                boolean returned = false;
                try {
                    // 逆向SQL也先保存发送意图，再实际执行；结束后查对象是否消失，而不是执行返回就直接算已清理。
                    ModelPublishTimingUtils.ModelPublishTiming sqlTiming = ModelPublishTimingUtils.startStep("SQL_COMPENSATE", context, step);
                    try {
                        executeStatement(session, sql);
                    } catch (SQLException | RuntimeException | Error ex) {
                        sqlTiming.failed(ex);
                        throw ex;
                    } finally {
                        sqlTiming.close();
                    }
                    returned = true;
                    step.setCompensationLastAttemptCompleted(true);
                } catch (SQLException | RuntimeException ex) {
                    if (ex instanceof SQLException) {
                        step.setCompensationLastAttemptCompleted(definiteRejection((SQLException) ex));
                        step.setCompensationLastAttemptRetryable(Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted())
                                && retryableState(((SQLException) ex).getSQLState()));
                    }
                }
                boolean absent = modelPublishService.verifyConditions(context, session, step.getCompensationPostconditions());
                step.setCompensationStatus(absent ? ModelCompensationStatus.SUCCEEDED : ModelCompensationStatus.FAILED);
                step.setCompensationFailureReason(absent ? "补偿后已核验对象不存在"
                        : "补偿后对象仍存在或读取未知，本轮停止发送，须先重新核验再判断重试");
                saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), step.getCompensationFailureReason());
                return returned && absent;
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 新表已经核验不存在，附属索引和注释也随表消失；逐条保存证明，不额外发送删除SQL。
     */
    private void completeRemovedTableSteps(ModelPublishContext context, ModelInstanceDO record, ModelSqlExecutionStep tableStep) {
        for (int index = context.getExecutionPlan().getSteps().size() - 1; index >= 0; index--) {
            ModelSqlExecutionStep step = context.getExecutionPlan().getSteps().get(index);
            if ((step.getAttemptCount() == 0 && !Boolean.TRUE.equals(step.getRecoveredEffect())) || step.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED
                    || !(step.getOperation() == ModelDdlOperation.CREATE_INDEX || step.getOperation() == ModelDdlOperation.COMMENT)
                    || !Objects.equals(step.getObject().getTableName(), tableStep.getObject().getTableName())) {
                continue;
            }
            // 同一张表已不存在，索引不存在由这个已保存的表后置证明，不伪造一次DROP INDEX尝试。
            for (ModelVerificationCondition condition : step.getCompensationPostconditions()) {
                if (condition.getType() == ModelVerificationType.OBJECT_ABSENT) {
                    condition.setStatus(ModelVerificationStatus.SATISFIED);
                    condition.setFailureReason(null);
                }
            }
            step.setCompensationStatus(ModelCompensationStatus.SUCCEEDED);
            step.setCompensationFailureReason("所属本次新表已核验不存在，附属索引或注释随表清理，未单独发送逆向SQL");
            saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), step.getCompensationFailureReason());
        }
    }

    /**
     * 补偿必须包含全部安全条件，归属条件也必须指向当前实例及当前步骤，不能用另一条成功步骤代替。
     */
    private void assertCompensationConditions(ModelPublishContext context, ModelSqlExecutionStep step) {
        for (ModelVerificationType required : Arrays.asList(ModelVerificationType.TARGET_IDENTITY_MATCH, ModelVerificationType.CREATED_BY_INSTANCE,
                ModelVerificationType.STRUCTURE_MATCH, ModelVerificationType.NO_DATA)) {
            if (step.getCompensationPreconditions().stream().noneMatch(condition -> condition.getType() == required)) {
                throw new ServiceException("保存的补偿计划缺少必要安全条件，停止清理");
            }
        }
        for (ModelVerificationCondition condition : step.getCompensationPreconditions()) {
            if (!JSON.toJSONString(condition.getObject()).equals(JSON.toJSONString(step.getObject()))
                    || (condition.getType() == ModelVerificationType.CREATED_BY_INSTANCE
                    && (!Objects.equals(condition.getExpected().getLong("instanceId"), context.getInstanceId())
                    || !Objects.equals(condition.getExpected().getInteger("stepNo"), step.getStepNo())))) {
                throw new ServiceException("补偿条件的对象、实例或步骤归属不一致");
            }
        }
        if (step.getCompensationPostconditions().size() != 1
                || step.getCompensationPostconditions().get(0).getType() != ModelVerificationType.OBJECT_ABSENT
                || !JSON.toJSONString(step.getObject()).equals(JSON.toJSONString(step.getCompensationPostconditions().get(0).getObject()))) {
            throw new ServiceException("补偿后置条件必须核验本对象确实不存在");
        }
    }

    /**
     * 记录不能安全撤销的原因；不修改原来的正向结果，也不编造逆向SQL。
     */
    private void retainCompensation(ModelPublishContext context, ModelInstanceDO record, ModelSqlExecutionStep step, String reason) {
        step.setCompensationStatus(ModelCompensationStatus.RETAINED);
        step.setCompensationFailureReason(reason);
        saveCompensation(context, record, ModelPublishStatus.COMPENSATING, Collections.emptyList(), reason);
    }

    /**
     * 用副本只读核验历史前置，保持原正向执行证据不被本次探测覆盖。
     */
    private List<ModelVerificationCondition> copyConditions(List<ModelVerificationCondition> conditions) {
        return JSON.parseArray(JSON.toJSONString(conditions), ModelVerificationCondition.class);
    }

    /**
     * 保存成功后才更新内存状态；全部SQL、残留记录和日志都使用原实例。
     */
    private void saveCompensation(ModelPublishContext context, ModelInstanceDO record, ModelPublishStatus status,
                                  List<ModelPublishRemainingEffect> effects, String reason) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("SAVE_COMPENSATE_TX", context);
        try {
            modelPublishService.saveCompensationProgress(record, context.getExecutionPlan(), status, effects, reason);
            context.setStatus(status);
            context.setCurrentStage(ModelPublishStage.COMPENSATE);
            context.setRemainingEffects(effects);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 最后重新读取真实列、索引及存在情况；只保存结构，不查询业务字段内容，不用步骤状态假装对象已删除。
     */
    private List<ModelPublishRemainingEffect> readRemainingEffects(ModelPublishContext context) {
        List<ModelPublishRemainingEffect> effects = new ArrayList<>();
        try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
            for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
                if (step.getAttemptCount() == 0 && !Boolean.TRUE.equals(step.getRecoveredEffect())) {
                    continue;
                }
                ModelPublishRemainingEffect effect = new ModelPublishRemainingEffect();
                effect.setStepNo(step.getStepNo());
                effect.setObject(step.getObject());
                try {
                    boolean tableExists = ModelMaterializationDbUtils.namedObjectExists(session, step.getObject().getTableName());
                    boolean exists = tableExists && !ModelMaterializationDbUtils.objectAbsent(session, step.getObject());
                    // 对象暂时查不到不等于旧SQL已经结束；旧尝试未结束时仍保留UNKNOWN，防止迟到SQL再次生效。
                    boolean uncertain = (!Boolean.TRUE.equals(step.getLastAttemptCompleted()) && !Boolean.TRUE.equals(step.getRecoveredEffect()))
                            || (step.getCompensationAttemptCount() > 0 && !Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted()));
                    if (!exists && !uncertain) {
                        continue;
                    }
                    if (step.getCompensationStatus() == ModelCompensationStatus.NOT_REQUIRED && !uncertain
                            && step.getStatus() != ModelSqlStepStatus.SUCCEEDED) {
                        continue;
                    }
                    JSONObject observed = JSONObject.of("objectExists", exists);
                    if (exists) {
                        observed.put("columns", ModelMaterializationDbUtils.readColumns(session, step.getObject().getTableName()));
                        List<DbIndex> actualIndexes = session.getQuery().getTableIndexes(session.getProperty(), step.getObject().getTableName());
                        if (actualIndexes == null) {
                            throw new ServiceException("实际索引信息读取不完整");
                        }
                        observed.put("indexes", actualIndexes);
                    }
                    effect.setObservedDefinition(observed);
                    effect.setVerificationStatus(uncertain ? ModelVerificationStatus.UNKNOWN : ModelVerificationStatus.SATISFIED);
                    effect.setReason(uncertain ? "旧语句是否结束尚未知，不能确认最终效果" : step.getCompensationFailureReason());
                } catch (Exception ex) {
                    effect.setVerificationStatus(ModelVerificationStatus.UNKNOWN);
                    effect.setReason("实际结构读取失败，不能声称已经清理");
                }
                effects.add(effect);
            }
            if (context.getBaseStructure() != null && effects.isEmpty()) {
                for (ModelMaterializedTableDefinition table : context.getBaseStructure().getTables()) {
                    boolean matches;
                    try {
                        matches = ModelMaterializationDbUtils.structureProblems(session, table).isEmpty();
                    } catch (SQLException | RuntimeException ex) {
                        matches = false;
                    }
                    if (!matches) {
                        ModelPublishRemainingEffect effect = new ModelPublishRemainingEffect();
                        effect.setVerificationStatus(ModelVerificationStatus.UNKNOWN);
                        effect.setReason("旧成功结构核验不一致，不能标记已经恢复原状：" + table.getTableName());
                        effects.add(effect);
                    }
                }
            }
            if (context.getExecutionPlan().getSteps().isEmpty()) {
                ModelPublishRemainingEffect effect = new ModelPublishRemainingEffect();
                effect.setVerificationStatus(ModelVerificationStatus.UNKNOWN);
                effect.setReason("零SQL实例的目标异常仍需核对，不能通过补偿直接清除未决状态");
                effects.add(effect);
            }
        }
        return effects;
    }

    /**
     * 平台短事务保存业务库事务阶段；只同步阶段，不覆盖内存中逐条SQL的原执行证据。
     */
    private void recordTransaction(ModelPublishContext context, ModelDdlTransactionStatus phase, ModelPublishStatus status, String message) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("SAVE_DDL_TRANSACTION_TX", context);
        try {
            ModelPublishContext saved = modelPublishService.recordDdlTransaction(context.getModelId(), context.getInstanceId(), phase, status, message);
            context.getExecutionPlan().setDdlTransactionStatus(saved.getExecutionPlan().getDdlTransactionStatus());
            context.setStatus(saved.getStatus());
            context.setCurrentStage(saved.getCurrentStage());
            if (status == ModelPublishStatus.FAILED || status == ModelPublishStatus.UNKNOWN) {
                context.setExecutionPlan(saved.getExecutionPlan());
                context.setVerifiedStructure(null);
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 原连接rollback返回后用新连接核对：旧结构恢复、新表/新列/新索引不存在，保留正向日志不重写。
     */
    private boolean rollbackMatches(ModelPublishContext context) {
        try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
            Set<String> newTables = new HashSet<>();
            for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
                if (step.getOperation() == ModelDdlOperation.CREATE_TABLE)
                    newTables.add(step.getObject().getTableName());
            }
            Set<String> checkedDefaultActions = new HashSet<>();
            for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
                if (step.getOperation() == ModelDdlOperation.COMMENT) continue;
                if (step.getOperation() == ModelDdlOperation.ALTER_DEFAULT
                        && !checkedDefaultActions.add(step.getActionKey())) continue;
                if (step.getOperation() != ModelDdlOperation.CREATE_TABLE && newTables.contains(step.getObject().getTableName()))
                    continue;
                if (!modelPublishService.verifyConditions(context, session, copyConditions(step.getPreconditions())))
                    return false;
            }
            ModelMaterializedStructure existing = context.getExecutionPlan().getExistingStructure() == null
                    ? context.getBaseStructure() : context.getExecutionPlan().getExistingStructure();
            if (existing != null) for (ModelMaterializedTableDefinition table : existing.getTables()) {
                if (!ModelMaterializationDbUtils.structureProblems(session, table).isEmpty()) return false;
            }
            return true;
        } catch (SQLException | RuntimeException ex) {
            return false;
        }
    }

    /**
     * 两步默认约束变更完成后，不能再以第一步的临时“无默认值”状态判定恢复失败。
     */
    private boolean defaultActionFinished(ModelPublishContext context, ModelPublishDatabaseSession session,
                                          ModelPublishExecutionPlan plan, ModelSqlExecutionStep step) {
        if (step.getOperation() != ModelDdlOperation.ALTER_DEFAULT) return false;
        ModelSqlExecutionStep last = step;
        for (ModelSqlExecutionStep candidate : plan.getSteps()) {
            if (candidate.getOperation() == ModelDdlOperation.ALTER_DEFAULT
                    && Objects.equals(candidate.getActionKey(), step.getActionKey())
                    && candidate.getStepNo() > last.getStepNo()) last = candidate;
        }
        return last != step && modelPublishService.verifyConditions(context, session, last.getPostconditions());
    }

    /**
     * 事务中断后只查结果，不在新连接上重放原批DDL，也不把新连接rollback当作原事务已回滚。
     */
    private ModelPublishContext recoverDdlTransaction(ModelPublishContext context) {
        ModelDdlTransactionStatus phase = context.getExecutionPlan().getDdlTransactionStatus();
        if ((phase == ModelDdlTransactionStatus.COMMITTING || phase == ModelDdlTransactionStatus.COMMITTED)
                && context.getExecutionPlan().getSteps().stream().allMatch(step -> step.getStatus() == ModelSqlStepStatus.SUCCEEDED
                && (Boolean.TRUE.equals(step.getLastAttemptCompleted()) || Boolean.TRUE.equals(step.getRecoveredEffect())))) {
            try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
                context.setVerifiedStructure(ModelMaterializationDbUtils.readActualStructure(session, context.getExecutionPlan().getTargetStructure()));
                recordTransaction(context, ModelDdlTransactionStatus.COMMITTED, ModelPublishStatus.EXECUTING,
                        "原提交阶段中断，已从新连接核验整批实际结构，不重发DDL");
                ModelInstanceDO record = new ModelInstanceDO();
                record.setId(context.getInstanceId());
                record.setModelId(context.getModelId());
                record.setModelVersionId(context.getModelVersionId());
                record.setDatasourceId(context.getDatasourceId());
                record.setBaseInstanceId(context.getBaseInstanceId());
                persistExecution(context, record, context.getExecutionPlan(), ModelPublishStage.VERIFY, ModelPublishStatus.COMMIT_PENDING,
                        "原业务库事务实际提交效果已核验，等待平台正式保存");
                return context;
            } catch (SQLException | RuntimeException ex) {
                // 缺少整批提交的可见证据就保留未知，不转到自动提交执行或删除补偿。
            }
        }
        recordTransaction(context, null, ModelPublishStatus.UNKNOWN, "原业务库事务结果尚不能确认，仅保留核验结果，不重发DDL或执行逆向删除");
        return context;
    }

    /**
     * 首次执行和恢复共用发送、核验及停止流程；恢复时先处理旧证据，不从头重发SQL。
     */
    private ModelPublishExecutionPlan executeSteps(ModelPublishContext context, ModelInstanceDO record,
                                                   ModelPublishExecutionPlan plan, boolean recovering) {
        // 已发送的旧非事务计划仍按原模式恢复，不能把前面已提交的DDL假装并入新事务。
        if (plan.getSteps().isEmpty() || (!Boolean.TRUE.equals(plan.getRollbackVerified())
                && plan.getSteps().stream().anyMatch(step -> step.getAttemptCount() > 0))) {
            return executeSteps(context, record, plan, recovering, null);
        }
        plan.setRollbackVerified(Boolean.FALSE);
        try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
            // 读取JDBC事务能力；支持DDL事务才复用单连接整批执行，否则沿用逐条执行及安全补偿。
            if (!session.beginDdlTransaction()) return executeSteps(context, record, plan, recovering, null);
            try {
                recordTransaction(context, ModelDdlTransactionStatus.ACTIVE, ModelPublishStatus.EXECUTING,
                        "本批DDL使用同一业务库事务，事务内核验通过尚不代表已提交");
            } catch (RuntimeException ex) {
                // 平台没有记下事务开始就不能发送第一条SQL，物理连接关闭时释放空事务。
                stopExecution(context, record, plan, ModelPublishStage.EXECUTE, "业务库事务开始记录失败，本批未发送SQL");
                throw ex;
            }
            try {
                executeSteps(context, record, plan, recovering, session);
                if (context.getStatus() == ModelPublishStatus.COMMIT_PENDING) return plan;
            } catch (RuntimeException ex) {
                // 下方按本地提交是否已经开始区分回滚和提交未知，不重复发送DDL。
            }
            // 提交一旦发起，异常不代表提交失败；先保留UNKNOWN，后续从新连接核验，不再尝试逆向删除。
            if (session.isCommitStarted()) {
                recordTransaction(context, null, ModelPublishStatus.UNKNOWN, "业务库提交或提交后的平台记录未完成，需核验原事务结果，不发送逆向DDL");
                return context.getExecutionPlan();
            }
            try {
                recordTransaction(context, ModelDdlTransactionStatus.ROLLING_BACK, ModelPublishStatus.EXECUTING,
                        "本批DDL未提交，开始回滚原业务库连接");
                // 只有尚未开始提交，才在原连接回滚整批DDL；随后还要核对旧结构和新增对象是否恢复。
                session.rollbackDdlTransaction();
                if (!rollbackMatches(context)) throw new ServiceException("回滚后的原结构或新增对象核验未通过");
                recordTransaction(context, ModelDdlTransactionStatus.ROLLED_BACK, ModelPublishStatus.FAILED,
                        "业务库回滚正常完成，原结构及本次新增对象已核验，本次发布失败，草稿保留");
            } catch (SQLException | RuntimeException ex) {
                recordTransaction(context, null, ModelPublishStatus.UNKNOWN,
                        "业务库回滚或回滚后的核验未完成，保留原事务阶段，不执行逆向删除");
            }
            return context.getExecutionPlan();
        } catch (SQLException ex) {
            stopExecution(context, record, plan, ModelPublishStage.EXECUTE, "目标库事务能力读取或事务开启失败，本批未发送SQL");
            throw new ServiceException("目标库事务能力读取或事务开启失败，本批未发送SQL");
        } catch (RuntimeException ex) {
            if (context.getStatus() == ModelPublishStatus.EXECUTING
                    && plan.getDdlTransactionStatus() == ModelDdlTransactionStatus.NONE
                    && plan.getSteps().stream().noneMatch(step -> step.getAttemptCount() > 0)) {
                stopExecution(context, record, plan, ModelPublishStage.EXECUTE, "执行前读取目标能力失败，本批未发送SQL");
            }
            throw ex;
        }
    }

    /**
     * 整批事务与逐条日志分开处理；transactionSession不为空时每条SQL借用同一连接。
     */
    private ModelPublishExecutionPlan executeSteps(ModelPublishContext context, ModelInstanceDO record,
                                                   ModelPublishExecutionPlan plan, boolean recovering,
                                                   ModelPublishDatabaseSession transactionSession) {
        boolean targetWorkCompleted = false;
        try {
            for (ModelSqlExecutionStep step : plan.getSteps()) {
                if (step.getStatus() == ModelSqlStepStatus.SUCCEEDED) {
                    // 已成功的步骤可能已被后续加列、扩长改变，不能用早期完整表定义要求数据库回到旧结构。
                    if (recovering && !Boolean.TRUE.equals(step.getLastAttemptCompleted())) {
                        persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.UNKNOWN,
                                "已保存的物理效果虽已确认，但旧SQL是否结束仍未知；停止发送和成功提交，需核对旧目标会话");
                        return plan;
                    }
                    continue;
                }
                // 前面的依赖SQL必须已经执行成功，当前SQL才能继续；恢复时另行处理旧发送记录。
                assertDependencies(plan, step);
                if (recovering && !recoverStep(context, record, plan, step)) {
                    if (context.getStatus() != ModelPublishStatus.EXECUTING) {
                        return plan;
                    }
                    continue;
                }
                if (Boolean.TRUE.equals(plan.getDeferredProgress()) && step.getAttemptCount() >= 1 + plan.getForwardMaxRetries()) {
                    step.setStatus(ModelSqlStepStatus.FAILED);
                    step.setFailureReason("本条SQL已达到原计划的重试上限，停止后续执行");
                    recordDeferredStep(plan, step, step.getFailureReason());
                    stopExecution(context, record, plan, ModelPublishStage.EXECUTE, step.getFailureReason());
                    return plan;
                }
                // 先记本条开始时间；新计划只暂存内存，旧计划仍随发送意图保存，不做逐条数据库核验。
                context.setCurrentStage(ModelPublishStage.EXECUTE);
                step.setCheckStartedTime(new Date());
                step.setStatus(ModelSqlStepStatus.RUNNING);
                // 普通模式逐条重新读取可信目标；整批事务模式借用原会话，保证DDL和核验始终使用同一连接。
                try (ModelPublishDatabaseSession session = transactionSession == null
                        ? modelPublishService.openTarget(context) : transactionSession.borrow()) {
                    // CONFIG、CHANGE、TARGET已在计划生成前统一完成；这里不再逐条查询目标库做前置检查。
                    ModelMaterializationDbUtils.assertForwardSql(step);
                    step.setAttemptCount(step.getAttemptCount() + 1);
                    step.setLastAttemptCompleted(false);
                    step.setLastAttemptRetryable(false);
                    // 新计划已提前保存完整执行范围；本条开始和结果暂存内存，结束时统一落库。
                    if (Boolean.TRUE.equals(plan.getDeferredProgress())) {
                        recordDeferredStep(plan, step, "开始执行第" + step.getStepNo() + "条SQL，结果将在整批结束时保存");
                    } else {
                        persistExecution(context, record, plan, ModelPublishStage.EXECUTE, ModelPublishStatus.EXECUTING,
                                "第" + step.getStepNo() + "条SQL的发送意图已保存，准备发送");
                    }
                    // 开始事件已写入内存或旧计划日志；结果事件另记真实结束时间。
                    step.setCheckStartedTime(null);

                    try {
                        ModelPublishTimingUtils.ModelPublishTiming sqlTiming = ModelPublishTimingUtils.startStep("SQL_FORWARD", context, step);
                        try {
                            executeStatement(session, Boolean.TRUE.equals(plan.getRecoveryReconciled())
                                    ? recoveryForwardSql(session, step) : step.getForwardSql());
                        } catch (SQLException | RuntimeException | Error ex) {
                            sqlTiming.failed(ex);
                            throw ex;
                        } finally {
                            sqlTiming.close();
                        }
                        // 本条成功表示JDBC正常返回，完整表、列和索引统一在整批结束后核验。
                        step.setLastAttemptCompleted(true);
                        step.setStatus(ModelSqlStepStatus.SUCCEEDED);
                        step.setFailureReason(null);
                    } catch (SQLException | RuntimeException ex) {
                        // 任一SQL失败立即停止，不再查本条效果后继续发送；下方交给整批回滚或逆向DDL处理。
                        if (ex instanceof SQLException) {
                            SQLException failure = (SQLException) ex;
                            step.setLastAttemptCompleted(definiteRejection(failure));
                            step.setLastAttemptRetryable(Boolean.TRUE.equals(step.getLastAttemptCompleted()) && retryableState(failure.getSQLState()));
                        }
                        step.setStatus(ModelSqlStepStatus.UNKNOWN);
                        step.setFailureReason("第" + step.getStepNo() + "条SQL执行失败，停止后续SQL并进入本批回滚处理");
                        if (Boolean.TRUE.equals(plan.getDeferredProgress()))
                            recordDeferredStep(plan, step, step.getFailureReason());
                        stopExecution(context, record, plan, ModelPublishStage.EXECUTE, step.getFailureReason());
                        return plan;
                    }
                    if (Boolean.TRUE.equals(plan.getDeferredProgress())) {
                        recordDeferredStep(plan, step, "第" + step.getStepNo() + "条SQL执行成功");
                    } else {
                        persistExecution(context, record, plan, ModelPublishStage.EXECUTE, ModelPublishStatus.EXECUTING,
                                "第" + step.getStepNo() + "条SQL执行成功，整批完成后统一核验结构");
                    }
                }
            }
            try (ModelPublishDatabaseSession session = transactionSession == null
                    ? modelPublishService.openTarget(context) : transactionSession.borrow()) {
                // 全部SQL正常返回后统一采集一次完整结构；零SQL发布也保留这一检查。
                context.setCurrentStage(ModelPublishStage.VERIFY);
                ModelPublishTimingUtils.ModelPublishTiming finalVerifyTiming = ModelPublishTimingUtils.start("VERIFY_FINAL", context);
                try {
                    // 身份核验仍走原来的条件检查；结构由下方一次采集后统一比较，不再先全量读取一轮。
                    ModelVerificationCondition identity = new ModelVerificationCondition();
                    identity.setConditionKey("FINAL_TARGET_IDENTITY");
                    identity.setType(ModelVerificationType.TARGET_IDENTITY_MATCH);
                    identity.setExpected(JSONObject.from(plan.getTarget()));
                    if (!modelPublishService.verifyConditions(context, session, Collections.singletonList(identity))) {
                        ModelPublishStatus status = identity.getStatus() == ModelVerificationStatus.UNKNOWN
                                ? ModelPublishStatus.UNKNOWN : ModelPublishStatus.PARTIAL_FAILED;
                        persistExecution(context, record, plan, ModelPublishStage.VERIFY, status, "完整目标身份核验失败，请先核对原实例与数据源");
                        return plan;
                    }
                    List<String> problems = new ArrayList<>();
                    ModelMaterializedStructure actual;
                    try {
                        actual = ModelMaterializationDbUtils.readActualStructure(session, plan.getTargetStructure(), problems);
                    } catch (SQLException | RuntimeException ex) {
                        // 读取失败表示查不清楚，保留UNKNOWN；不能把连接或权限异常解释成结构不一致。
                        session.invalidateReadConnection();
                        persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.UNKNOWN,
                                "完整目标结构采集失败，保留原实例等待核验");
                        return plan;
                    }
                    if (!problems.isEmpty()) {
                        // 包含零SQL发布：明确发现外部结构变化时仍记部分失败，不能当作已经恢复原状。
                        persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.PARTIAL_FAILED,
                                "完整目标结构核验失败，需先核对：" + problems.get(0));
                        return plan;
                    }
                    context.setVerifiedStructure(actual);
                    // 全部SQL及整批结构核验通过后再提交业务库；平台记录另用独立短事务保存。
                } catch (RuntimeException | Error ex) {
                    finalVerifyTiming.failed(ex);
                    throw ex;
                } finally {
                    finalVerifyTiming.close();
                }
                if (transactionSession != null) {
                    if (Boolean.TRUE.equals(plan.getDeferredProgress())) {
                        persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.EXECUTING,
                                "全部SQL执行及核验完成，统一保存逐条结果后准备提交业务库事务");
                    }
                    recordTransaction(context, ModelDdlTransactionStatus.COMMITTING, ModelPublishStatus.EXECUTING,
                            "整批DDL及事务内结构核验已完成，提交意图已保存，准备提交业务库事务");
                    try {
                        session.commitDdlTransaction();
                    } catch (SQLException ex) {
                        throw new ServiceException("业务库提交未明确返回，保留原提交意图");
                    }
                    recordTransaction(context, ModelDdlTransactionStatus.COMMITTED, ModelPublishStatus.EXECUTING,
                            "业务库事务已明确提交，下一步保存平台正式结构");
                }
                persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.COMMIT_PENDING,
                        "全部SQL及完整目标结构已核验，等待保存正式结构；尚未发布成功");
                targetWorkCompleted = true;
                return plan;
            }
        } catch (RuntimeException ex) {
            // 不能让统一异常处理把已经执行的SQL记成普通准备失败。保存失败后原RUNNING证据仍阻止重发。
            if (!targetWorkCompleted && context.getStatus() == ModelPublishStatus.EXECUTING) {
                context.setStatus(ModelPublishStatus.UNKNOWN);
                String reason = "执行或记录保存中断，必须先核对已保存步骤与目标实际效果，不能直接重发SQL";
                try {
                    markUnsentSteps(plan);
                    for (ModelSqlExecutionStep step : plan.getSteps()) {
                        if (step.getStatus() == ModelSqlStepStatus.RUNNING) {
                            step.setStatus(ModelSqlStepStatus.UNKNOWN);
                            step.setFailureReason(reason);
                        }
                    }
                    modelPublishService.saveExecutionProgress(record, plan, context.getCurrentStage(), ModelPublishStatus.UNKNOWN, reason);
                    plan.getExecutionEvents().clear();
                } catch (RuntimeException recordFailure) {
                    // 同一异常对象被再次抛出时不能把自己作为附加异常，否则会掩盖原来的失败。
                    if (recordFailure != ex) {
                        ex.addSuppressed(recordFailure);
                    }
                }
            }
            context.setVerifiedStructure(null);
            throw new ServiceException("物化执行未正常结束，请先核对本次实例及目标实际效果");
        }
    }

    /**
     * 对没完成的步骤先查实际效果。返回true才允许进入发送流程；false表示已经补记成功或已经停止。
     * <p>
     * 返回true代表本条允许进入发送流程；返回false需要结合实例状态判断是已经补记成功继续下一条，还是整个流程已经停止。
     */
    private boolean recoverStep(ModelPublishContext context, ModelInstanceDO record, ModelPublishExecutionPlan plan,
                                ModelSqlExecutionStep step) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startStep("RECOVER_STEP", context, step);
        try {
            context.setCurrentStage(ModelPublishStage.VERIFY);
            if (step.getAttemptCount() == 0) {
                if (step.getStatus() != ModelSqlStepStatus.PENDING) {
                    step.setStatus(ModelSqlStepStatus.PENDING);
                    step.setFailureReason(null);
                    modelPublishService.saveRecoveryProgress(record, plan, "本条没有发送记录，按原计划恢复为待执行");
                }
                return true;
            }
            try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
                // 有发送意图就先查后置效果：已生效只补记录，未生效也必须确认旧尝试结束后才能判断重试。
                if (step.getAttemptCount() > 0) {
                    // 注释不再回读，也不能把未知的旧发送直接当作成功；继续沿用结束和可重试证据判断。
                    if (step.getOperation() != ModelDdlOperation.COMMENT
                            && modelPublishService.verifyConditions(context, session, step.getPostconditions())) {
                        step.setStatus(ModelSqlStepStatus.SUCCEEDED);
                        step.setFailureReason(null);
                        modelPublishService.saveRecoveryProgress(record, plan, "恢复核验确认第" + step.getStepNo() + "条SQL已生效，只补记结果，不重复发送");
                        if (!Boolean.TRUE.equals(step.getLastAttemptCompleted())) {
                            persistExecution(context, record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.UNKNOWN,
                                    "实际效果已补记，但旧SQL是否结束仍未知；停止后续发送，需核对旧目标会话");
                        }
                        return false;
                    }
                    if ((step.getOperation() != ModelDdlOperation.COMMENT && hasUnknown(step.getPostconditions()))
                            || !Boolean.TRUE.equals(step.getLastAttemptCompleted())) {
                        // 旧计划没有结束证据时也按未知处理；单次查不到表或列不能证明旧SQL已经停止。
                        step.setStatus(ModelSqlStepStatus.UNKNOWN);
                        step.setFailureReason("无法确认上次SQL已结束且未生效，停止重试，保留原发送证据");
                        modelPublishService.saveRecoveryProgress(record, plan, step.getFailureReason());
                        stopExecution(context, record, plan, ModelPublishStage.VERIFY, step.getFailureReason());
                        return false;
                    }
                    if (!Boolean.TRUE.equals(step.getLastAttemptRetryable())) {
                        step.setStatus(ModelSqlStepStatus.FAILED);
                        step.setFailureReason("数据库明确拒绝本条SQL，错误不属于可重试范围，停止后续执行，等待补偿处理");
                        modelPublishService.saveRecoveryProgress(record, plan, step.getFailureReason());
                        stopExecution(context, record, plan, ModelPublishStage.VERIFY, step.getFailureReason());
                        return false;
                    }
                }
                // 恢复只检查上一次已发送步骤的结果；尚未发送的步骤仍按原计划直接执行，不追加前置核验。
                // 总发送额度等于首次1次加计划允许的重试次数；恢复点击不会重置已用次数。
                if (step.getAttemptCount() >= 1 + plan.getForwardMaxRetries()) {
                    step.setStatus(ModelSqlStepStatus.FAILED);
                    step.setFailureReason("本条SQL已达到保存计划的重试上限，停止后续执行，等待补偿处理");
                    modelPublishService.saveRecoveryProgress(record, plan, step.getFailureReason());
                    stopExecution(context, record, plan, ModelPublishStage.VERIFY, step.getFailureReason());
                    return false;
                }
                step.setStatus(ModelSqlStepStatus.PENDING);
                step.setFailureReason(null);
                modelPublishService.saveRecoveryProgress(record, plan, step.getAttemptCount() == 0
                        ? "本条尚未发送，按原计划继续执行"
                        : "确认上次SQL已结束且未生效，按原计划额度进行一次重试");
                return true;
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 只识别标准明确拒绝和回滚状态；网络、超时和未知驱动异常都不能证明上次已经结束。
     * 不猜测各厂商错误码，无法确定时保留未知，实际效果核验也不能替代旧SQL结束证据。
     */
    private boolean definiteRejection(SQLException failure) {
        String state = failure.getSQLState();
        if (!rejectionState(state)) {
            return false;
        }
        Set<Throwable> checked = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        Set<SQLException> chained = Collections.newSetFromMap(new IdentityHashMap<SQLException, Boolean>());
        for (SQLException error = failure; error != null && chained.add(error); error = error.getNextException()) {
            String code = error.getSQLState();
            if (error instanceof java.sql.SQLTimeoutException || error instanceof java.sql.SQLRecoverableException
                    || !rejectionState(code)) {
                return false;
            }
            // 驱动可能把断线放在后续SQLException的cause里，不能只看最外层可重试状态就放行。
            for (Throwable cause = error; cause != null && checked.add(cause); cause = cause.getCause()) {
                if (cause instanceof java.io.IOException || cause instanceof java.sql.SQLTimeoutException
                        || cause instanceof java.sql.SQLRecoverableException
                        || (cause instanceof SQLException && !rejectionState(((SQLException) cause).getSQLState()))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 临时错误只接标准明确的回滚、死锁和锁不可用状态；40003表示完成情况未知，特意不纳入。
     */
    private boolean retryableState(String state) {
        return Arrays.asList("40001", "40P01", "55P03").contains(state);
    }

    /**
     * 标准明确拒绝既包括永久错误，也包括可重试的临时错误；两者都能说明这次已结束。
     */
    private boolean rejectionState(String state) {
        return retryableState(state) || (state != null && state.length() >= 2
                && Arrays.asList("22", "23", "42", "0A").contains(state.substring(0, 2)));
    }

    /**
     * 使用本次业务库连接执行单条DDL，不包进平台事务；执行异常后停止后续SQL并进入回滚处理。
     */
    protected void executeStatement(ModelPublishDatabaseSession session, String sql) throws SQLException {
        // 普通模式用自动提交连接；整批事务模式借用同一连接，关闭借用句柄不会提前提交或结束整批事务。
        try (Connection connection = session.getQuery().getConnection()) {
            if (!connection.getAutoCommit() && !session.transactional()) {
                throw new SQLException("非自动提交连接必须由本次物化事务会话创建");
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        } catch (SQLException | RuntimeException ex) {
            // 普通模式异常后不继续借用这条连接；整批事务模式仍由原流程在原连接回滚或核验。
            session.invalidateReadConnection();
            throw ex;
        }
    }

    /**
     * 必须等所有依赖SQL执行成功，本条才能开始；只接受前面的有效步骤编号。
     */
    private void assertDependencies(ModelPublishExecutionPlan plan, ModelSqlExecutionStep step) {
        for (Integer number : step.getDependencyStepNos()) {
            if (number == null || number < 1 || number >= step.getStepNo()
                    || plan.getSteps().get(number - 1).getStatus() != ModelSqlStepStatus.SUCCEEDED) {
                throw new ServiceException("依赖SQL尚未执行成功，不能开始下一条");
            }
        }
    }

    /**
     * 保存成功以后才改变上下文的实例状态，保存失败不能在内存里冒充已提交。
     */
    private void persistExecution(ModelPublishContext context, ModelInstanceDO record, ModelPublishExecutionPlan plan,
                                  ModelPublishStage stage, ModelPublishStatus status, String message) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("SAVE_PROGRESS_TX", context);
        try {
            context.setCurrentStage(stage);
            // 先通过平台独立事务保存，再同步内存状态；保存失败时不允许假装本条证据已落库。
            modelPublishService.saveExecutionProgress(record, plan, stage, status, message);
            plan.getExecutionEvents().clear();
            context.setStatus(status);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * SQL执行或整批核验失败时保存结果，后续未发送步骤统一标记为未执行。
     */
    private void stopExecution(ModelPublishContext context, ModelInstanceDO record, ModelPublishExecutionPlan plan,
                               ModelPublishStage stage, String reason) {
        // 停止时明确标记后续未发送步骤，并保存整次状态；原已成功或未知步骤的证据继续保留。
        markUnsentSteps(plan);
        boolean unknown = plan.getSteps().stream().anyMatch(step -> step.getStatus() == ModelSqlStepStatus.UNKNOWN);
        boolean effects = plan.getSteps().stream().anyMatch(step -> step.getAttemptCount() > 0 || Boolean.TRUE.equals(step.getRecoveredEffect()));
        ModelPublishStatus status = unknown ? ModelPublishStatus.UNKNOWN
                : effects ? ModelPublishStatus.PARTIAL_FAILED : ModelPublishStatus.FAILED;
        persistExecution(context, record, plan, stage, status, reason);
    }

    /**
     * 只把从未开始的步骤标为未执行，不覆盖已保存的执行中或未知证据。
     */
    private void markUnsentSteps(ModelPublishExecutionPlan plan) {
        for (ModelSqlExecutionStep step : plan.getSteps()) {
            if (step.getStatus() == ModelSqlStepStatus.PENDING) {
                step.setStatus(ModelSqlStepStatus.NOT_EXECUTED);
            }
        }
    }

    /**
     * 无法可靠读取目标条件时，需要恢复核验，不能按已确认失败处理。
     */
    private boolean hasUnknown(List<ModelVerificationCondition> conditions) {
        return conditions.stream().anyMatch(condition -> condition.getStatus() == ModelVerificationStatus.UNKNOWN);
    }

    // DDL动作选择：按批准的变化整理建表、加列、扩长和必要索引。

    /**
     * 构建完整结构、保存新旧比较结果，并校验本次变更边界。
     * 本方法不生成动作或SQL，等TARGET确认实际缺表、缺列情况后再统一选择动作。
     *
     * @param context 处于准备阶段、包含本次配置和旧成功结构的发布数据
     */
    private void prepareStructureAndChanges(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("PREPARE_STRUCTURE_CHANGE", context);
        try {
            if (context == null || context.getStatus() != ModelPublishStatus.PREPARING) {
                throw new ServiceException("只有准备阶段可以重新构建发布结构，执行中或恢复中的实例不能重新生成计划");
            }
            // 一旦重新构建，旧SQL计划不再代表当前配置；后续必须重新转换、TARGET校验并生成计划。
            context.setExecutionPlan(null);
            // 结构准备第1步：CONFIG校验通过后，构建完整来源/权威主子表。
            schemaService.build(context);
            if (context.getInstanceId() != null) {
                modelPublishService.saveStage(context, ModelPublishStage.BUILD, "来源、权威及主子表完整结构已构建");
            }
            // 结构准备第2步：与上次成功结构比较；比较结果先保存，后续CHANGE失败也能查到差异。
            schemaService.compare(context);
            if (context.getInstanceId() != null) {
                ModelInstanceDO record = new ModelInstanceDO();
                record.setId(context.getInstanceId());
                record.setModelId(context.getModelId());
                record.setBaseInstanceId(context.getBaseInstanceId());
                modelPublishService.saveComparison(record, context.getDiff());
            }
            // 结构准备第3步：批准变更边界；动作列表在TARGET之后按实际结构生成一次。
            modelPublishService.validate(ModelValidationStage.CHANGE, context);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 按指定的已有结构整理建表、加列、扩长和索引；TARGET后使用实际结构补齐丢失对象。
     */
    private List<ModelDdlAction> selectActions(ModelPublishContext context, ModelMaterializedStructure existing) {
        Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> before = tables(existing);
        Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> published = tables(context.getDiff().getBeforeStructure());
        List<ModelMaterializedTableDefinition> after = new ArrayList<>(context.getDiff().getAfterStructure().getTables());
        // 先主模型、再子模型；同层按模型ID和固定角色排序，配置列表换序不会改变动作顺序。
        after.sort(Comparator.comparingInt((ModelMaterializedTableDefinition table) -> table.getParentId() == 0 ? 0 : 1)
                .thenComparing(ModelMaterializedTableDefinition::getModelId)
                .thenComparingInt(table -> table.getTableRole() == ModelTableRole.SOURCE ? 0 : 1));
        Map<String, ModelDdlAction> actions = new LinkedHashMap<>();
        // 第一轮只处理新表，新主表排在新子表前；新表的业务列随建表一次生成。
        for (ModelMaterializedTableDefinition table : after) {
            ModelMaterializedTableDefinition old = before.get(identity(table));
            if (old == null) {
                addTable(actions, table);
            }
        }
        // 第二轮只处理已有表的新增列和扩长，不重新生成整表，也不会重复给新表加列。
        for (ModelMaterializedTableDefinition table : after) {
            ModelMaterializedTableDefinition old = before.get(identity(table));
            if (old != null) {
                addColumnActions(actions, old, table, published.get(identity(table)));
            }
        }
        // 第三轮补必要索引，并关联建表、加列等前置动作，确保索引引用的列已经存在。
        for (ModelMaterializedTableDefinition table : after) {
            addIndexActions(actions, before.get(identity(table)), table);
        }
        List<ModelDdlAction> result = new ArrayList<>(actions.values());
        for (int index = 0; index < result.size(); index++) {
            ModelDdlAction action = result.get(index);
            action.setActionNo(index + 1);
            for (String dependency : action.getDependencyActionKeys()) {
                ModelDdlAction parent = actions.get(dependency);
                if (parent == null || parent.getActionNo() == null || parent.getActionNo() >= action.getActionNo()) {
                    throw new ServiceException("DDL动作依赖缺失或顺序错误，不能生成SQL计划");
                }
            }
        }
        return result;
    }

    /**
     * 新表一次包含全部业务列和系统列，主键由CREATE承载，不另发加列或主键索引动作。
     *
     * @param actions 已整理的动作
     * @param table   新物理表的完整定义
     */
    private void addTable(Map<String, ModelDdlAction> actions, ModelMaterializedTableDefinition table) {
        // 未发布的停用模型已由Builder排除；历史停用表若已丢失，也按保留规则补回物理结构。
        ModelDdlAction action = action(table, ModelDdlOperation.CREATE_TABLE, "TABLE", null, table);
        if (table.getParentId() != 0) {
            String parentKey = actionKey(table.getParentId(), table.getTableRole(), ModelDdlOperation.CREATE_TABLE, "TABLE");
            // 父表也是本次新建时必须先建父表；父表早已存在时，由后续TARGET核对其真实结构。
            if (actions.containsKey(parentKey)) {
                action.getDependencyActionKeys().add(parentKey);
            }
        }
        put(actions, action);
    }

    /**
     * 旧表选择新增列、缺失列补回、STRING扩长及放宽必填；停用标记本身不生成DDL。
     *
     * @param actions   已整理的动作
     * @param old       上次成功表
     * @param table     本次完整表
     * @param published 上次成功结构，用于区分缺失字段修复和真正的新字段
     */
    private void addColumnActions(Map<String, ModelDdlAction> actions, ModelMaterializedTableDefinition old,
                                  ModelMaterializedTableDefinition table, ModelMaterializedTableDefinition published) {
        Map<String, ModelMaterializedColumnDefinition> previous = columns(old);
        Map<String, ModelMaterializedColumnDefinition> history = published == null ? Collections.emptyMap() : columns(published);
        for (ModelMaterializedColumnDefinition column : table.getColumns()) {
            String key = columnKey(column);
            ModelMaterializedColumnDefinition before = previous.get(key);
            if (before == null) {
                // 旧来源表允许补齐status；不会因此放开其他系统字段的新增。
                boolean sourceStatus = table.getTableRole() == ModelTableRole.SOURCE && "STATUS".equals(column.getSystemFieldKey());
                // 原来已发布过的业务列、系统列、停用列丢失时都按原定义补回；真正新加的字段仍沿用原限制。
                if (!history.containsKey(key) && ((column.getModelFieldId() == null && !sourceStatus)
                        || Boolean.FALSE.equals(column.getEnabledFlag()))) {
                    throw new ServiceException("旧表只允许新增正常使用的业务列或来源status字段");
                }
                put(actions, action(table, ModelDdlOperation.ADD_COLUMN, key, null, column));
            } else {
                boolean relax = column.getModelFieldId() != null && !Boolean.TRUE.equals(column.getPrimaryKeyFlag())
                        && Boolean.FALSE.equals(before.getNullable()) && Boolean.TRUE.equals(column.getNullable());
                if ("STRING".equals(column.getLogicalType()) && before.getCharacterLength() != null
                        && column.getCharacterLength() != null && column.getCharacterLength() > before.getCharacterLength()) {
                    ModelMaterializedColumnDefinition expanded = copy(column, ModelMaterializedColumnDefinition.class);
                    // 同次扩长并放宽必填时分两步执行，每一步使用对应的前后结构核验。
                    if (relax) expanded.setNullable(before.getNullable());
                    expanded.setDefaultValue(before.getDefaultValue());
                    put(actions, action(table, ModelDdlOperation.EXPAND_COLUMN, key, before, expanded));
                    before = expanded;
                }
                if (relax) {
                    ModelMaterializedColumnDefinition relaxed = copy(column, ModelMaterializedColumnDefinition.class);
                    relaxed.setDefaultValue(before.getDefaultValue());
                    ModelDdlAction action = action(table, ModelDdlOperation.RELAX_COLUMN, key, before, relaxed);
                    addDependency(actions, action, table, ModelDdlOperation.EXPAND_COLUMN, key);
                    put(actions, action);
                    before = relaxed;
                }
                if (!Objects.equals(ModelMaterializationDbUtils.defaultValue(before.getDefaultValue()),
                        ModelMaterializationDbUtils.defaultValue(column.getDefaultValue()))) {
                    ModelDdlAction action = action(table, ModelDdlOperation.ALTER_DEFAULT, key, before, column);
                    addDependency(actions, action, table, ModelDdlOperation.EXPAND_COLUMN, key);
                    addDependency(actions, action, table, ModelDdlOperation.RELAX_COLUMN, key);
                    put(actions, action);
                }
            }
        }
    }

    /**
     * 新表创建必要索引；旧表创建批准的新索引，并补回缺失列关联的索引和主键。
     *
     * @param actions 已整理的动作
     * @param old     旧表，首次新建时为空
     * @param table   本次完整表
     */
    private void addIndexActions(Map<String, ModelDdlAction> actions, ModelMaterializedTableDefinition old,
                                 ModelMaterializedTableDefinition table) {
        Map<String, ModelMaterializedIndexDefinition> previous = new LinkedHashMap<>();
        if (old != null) {
            for (ModelMaterializedIndexDefinition index : old.getIndexes()) {
                previous.put(index.getStableKey(), index);
            }
        }
        List<ModelMaterializedIndexDefinition> indexes = new ArrayList<>(table.getIndexes());
        indexes.sort(Comparator.comparing(ModelMaterializedIndexDefinition::getStableKey));
        for (ModelMaterializedIndexDefinition index : indexes) {
            if (previous.containsKey(index.getStableKey()) || (index.getIndexType() == ModelIndexType.PRIMARY_KEY && old == null)) {
                continue;
            }
            ModelDdlAction action = action(table, ModelDdlOperation.CREATE_INDEX, index.getStableKey(), null, index);
            addDependency(actions, action, table, ModelDdlOperation.CREATE_TABLE, "TABLE");
            for (String column : index.getColumnKeys()) {
                addDependency(actions, action, table, ModelDdlOperation.ADD_COLUMN, column);
                addDependency(actions, action, table, ModelDdlOperation.EXPAND_COLUMN, column);
                addDependency(actions, action, table, ModelDdlOperation.RELAX_COLUMN, column);
                addDependency(actions, action, table, ModelDdlOperation.ALTER_DEFAULT, column);
            }
            put(actions, action);
        }
    }

    /**
     * 当前动作依赖的建表或加列确实存在时才加入依赖，不虚构已经物化对象的动作。
     *
     * @param actions   已整理的动作
     * @param action    当前索引动作
     * @param table     所属物理表
     * @param operation 被依赖的操作
     * @param key       被依赖的固定对象标识
     */
    private void addDependency(Map<String, ModelDdlAction> actions, ModelDdlAction action,
                               ModelMaterializedTableDefinition table, ModelDdlOperation operation, String key) {
        String dependency = actionKey(table.getModelId(), table.getTableRole(), operation, key);
        if (actions.containsKey(dependency)) {
            action.getDependencyActionKeys().add(dependency);
        }
    }

    /**
     * 构造独立动作副本；后续转换common参数时不允许修改旧成功结构或原始配置。
     *
     * @param table     所属表
     * @param operation 操作类型
     * @param key       不随物理名称变化的对象标识
     * @param before    原完整定义，新增为空
     * @param after     新完整定义
     * @return 待转换成SQL的动作
     */
    private ModelDdlAction action(ModelMaterializedTableDefinition table, ModelDdlOperation operation, String key,
                                  Object before, Object after) {
        ModelDdlAction result = new ModelDdlAction();
        result.setOperation(operation);
        result.setActionKey(actionKey(table.getModelId(), table.getTableRole(), operation, key));
        result.setBeforeDefinition(before == null ? null : JSON.parseObject(JSON.toJSONString(before)));
        result.setAfterDefinition(JSON.parseObject(JSON.toJSONString(after)));
        ModelSchemaObjectRef ref = new ModelSchemaObjectRef();
        ref.setModelId(table.getModelId());
        ref.setParentId(table.getParentId());
        ref.setTableRole(table.getTableRole());
        ref.setTableName(table.getTableName());
        ref.setObjectKey(key);
        ref.setObjectType(operation == ModelDdlOperation.CREATE_TABLE ? ModelSchemaObjectType.TABLE
                : operation == ModelDdlOperation.CREATE_INDEX ? ModelSchemaObjectType.INDEX : ModelSchemaObjectType.COLUMN);
        ref.setColumnName(ref.getObjectType() == ModelSchemaObjectType.COLUMN ? result.getAfterDefinition().getString("columnName") : null);
        ref.setIndexName(ref.getObjectType() == ModelSchemaObjectType.INDEX ? result.getAfterDefinition().getString("indexName") : null);
        result.setObject(ref);
        return result;
    }

    /**
     * 动作标识只使用固定身份，不用表名或字段名判断是否重复。
     *
     * @param modelId   模型ID
     * @param role      来源或权威
     * @param operation 操作类型
     * @param key       对象固定标识
     * @return 本次清单内的唯一动作标识
     */
    private String actionKey(Long modelId, ModelTableRole role, ModelDdlOperation operation, String key) {
        return modelId + ":" + role + ":" + operation + ":" + key;
    }

    /**
     * 对重复动作明确报错，避免同一张表或同一列被重复生成SQL。
     *
     * @param actions 已整理的动作
     * @param action  新动作
     */
    private void put(Map<String, ModelDdlAction> actions, ModelDdlAction action) {
        if (actions.put(action.getActionKey(), action) != null) {
            throw new ServiceException("同一个DDL动作重复，不能重复生成SQL：" + action.getActionKey());
        }
    }

    /**
     * 按模型和角色定位旧表，不按名称匹配。
     *
     * @param structure 完整旧结构
     * @return 固定身份对应的表
     */
    private Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> tables(ModelMaterializedStructure structure) {
        Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> result = new LinkedHashMap<>();
        for (ModelMaterializedTableDefinition table : structure.getTables()) {
            result.put(identity(table), table);
        }
        return result;
    }

    /**
     * 产生表的固定身份。
     *
     * @param table 来源或权威表
     * @return 模型ID与角色组成的标识
     */
    private ModelMaterializedTableIdentity identity(ModelMaterializedTableDefinition table) {
        return ModelMaterializedTableIdentity.of(table.getModelId(), table.getTableRole());
    }

    /**
     * 把旧表列按业务字段ID或系统固定标识放入Map。
     *
     * @param table 旧表
     * @return 全部旧列，包含停用列
     */
    private Map<String, ModelMaterializedColumnDefinition> columns(ModelMaterializedTableDefinition table) {
        Map<String, ModelMaterializedColumnDefinition> result = new LinkedHashMap<>();
        for (ModelMaterializedColumnDefinition column : table.getColumns()) {
            result.put(columnKey(column), column);
        }
        return result;
    }

    /**
     * 产生业务列或系统列的固定标识，不随字段编码修改而改变。
     *
     * @param column 物化列定义
     * @return FIELD:字段ID或SYSTEM:字典标识
     */
    private String columnKey(ModelMaterializedColumnDefinition column) {
        return column.getSystemFieldKey() == null ? "FIELD:" + column.getModelFieldId() : "SYSTEM:" + column.getSystemFieldKey();
    }
}
