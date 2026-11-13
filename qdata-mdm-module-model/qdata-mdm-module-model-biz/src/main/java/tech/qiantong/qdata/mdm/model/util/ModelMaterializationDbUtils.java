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

package tech.qiantong.qdata.mdm.model.util;

import com.alibaba.fastjson2.JSON;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbIndex;
import tech.qiantong.qdata.mdm.common.database.dialect.*;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.domain.*;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelDdlOperation;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelIndexType;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSchemaObjectType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段参数衔接和只读数据探测；DDL语法仍全部由common负责。
 */
public final class ModelMaterializationDbUtils {

    /**
     * 工具类不需要创建实例。
     */
    private ModelMaterializationDbUtils() {
    }

    /**
     * 将模型七类字段参数交给common转换，不把逻辑类型直接塞给旧建表方法。
     */
    public static DbColumn column(ModelPublishDatabaseSession session, ModelMaterializedColumnDefinition definition) {
        // model保留逻辑类型与约束，这里转换为common需要的列参数，后续由目标方言决定物理语法。
        DbColumn input = logicalColumn(definition);
        DbColumn converted = session.getQuery().convertLogicalColumn(input);
        // 已成功发布的SQL Server旧列保持原物理类型，扩长不隐式迁移VARCHAR/TEXT到Unicode。
        if (session.getDialect() instanceof SQLServer2008Dialect && definition.getPhysicalType() != null) {
            String storedType = definition.getPhysicalType().trim().toUpperCase(Locale.ROOT);
            if ("STRING".equals(definition.getLogicalType()) && storedType.matches("VARCHAR(?:\\(.*\\))?")) {
                converted.setDataType("VARCHAR");
            } else if ("TEXT".equals(definition.getLogicalType()) && "TEXT".equals(storedType)) {
                converted.setDataType("TEXT");
            } else if ("DATETIME".equals(definition.getLogicalType()) && "DATETIME".equals(storedType)) {
                // 已有时间戳的精度迁移需显式执行，不应由无关的模型编辑操作触发。
                converted.setDataType("DATETIME");
                converted.setDataScale(null);
            }
        }
        return converted;
    }

    /**
     * 将模型定义交给common，静态值保持原文，避免字符串默认值被重复编码。
     */
    public static DbColumn logicalColumn(ModelMaterializedColumnDefinition definition) {
        return DbColumn.builder().colName(definition.getColumnName()).dataType(definition.getLogicalType())
                .dataLength(text(definition.getCharacterLength())).dataPrecision(text(definition.getNumericPrecision()))
                .dataScale(text(definition.getNumericScale())).nullable(definition.getNullable())
                .colKey(definition.getPrimaryKeyFlag()).identityFlag(definition.getIdentityFlag())
                .dataDefault(definition.getDefaultValue())
                .colComment(definition.getColumnComment()).colPosition(definition.getColumnPosition()).build();
    }

    /**
     * 读取真实结果列类型、长度及可空性，不用缓存推断物理结构。
     */
    public static List<DbColumn> readColumns(ModelPublishDatabaseSession session, String tableName) throws SQLException {
        return readColumns(session, tableName, null);
    }

    /**
     * 一次JDBC读取同时返回列属性及需要的自增标记，避免完整采集时再执行同一条空结果查询。
     */
    private static List<DbColumn> readColumns(ModelPublishDatabaseSession session, String tableName,
                                              Map<String, Boolean> identities) throws SQLException {
        return readColumns(session, tableName, identities, null);
    }

    /**
     * 完整采集DM字段时，复用本来就要读取的JDBC字段目录，同时取得默认值。
     * 其他数据库仍沿用common读取默认值；这里不拿模型配置代替数据库实际值。
     */
    private static List<DbColumn> readColumns(ModelPublishDatabaseSession session, String tableName,
                                              Map<String, Boolean> identities,
                                              Map<String, DbColumn> definitions) throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("READ_COLUMNS", null, null);
        try {
            requireIdentifier(tableName);
            // DDL会改变返回列，使用普通Statement，避免PostgreSQL等数据库复用已失效的预编译结果结构。
            String sql = "SELECT * FROM " + session.getDialect().getTableName(session.getProperty(), tableName) + " WHERE 1 = 0";
            try (Connection connection = session.getQuery().getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(sql)) {
                ResultSetMetaData metadata = rows.getMetaData();
                Map<String, Long> characterLengths = readCharacterLengths(session, connection, tableName, definitions);
                Map<String, Boolean> nativeIdentities = identities == null ? Collections.emptyMap()
                        : session.getDialect().readIdentityColumns(connection,
                        session.getDialect().getSchemaName(session.getProperty()), tableName);
                List<DbColumn> columns = new ArrayList<>();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    // 驱动没有明确返回是否可空时，不能把“未知”误当成NOT NULL。
                    if (metadata.isNullable(index) == ResultSetMetaData.columnNullableUnknown) {
                        throw new SQLException("驱动未明确返回列的可空约束");
                    }
                    if (identities != null) {
                        identities.put(metadata.getColumnName(index), metadata.isAutoIncrement(index)
                                || Boolean.TRUE.equals(nativeIdentities.get(metadata.getColumnName(index))));
                    }
                    columns.add(DbColumn.builder().colName(metadata.getColumnName(index)).dataType(metadata.getColumnTypeName(index))
                            .dataLength(String.valueOf(characterLength(session, metadata, index, characterLengths)))
                            .dataPrecision(String.valueOf(metadata.getPrecision(index)))
                            .dataScale(String.valueOf(metadata.getScale(index)))
                            .nullable(metadata.isNullable(index) == ResultSetMetaData.columnNullable)
                            .colPosition(identities == null ? null : index).build());
                }
                return columns;
            }
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 旧jTDS驱动不实现getSchema时，使用已经明确配置的Schema；其他数据库异常仍向外抛出。
     *
     * @param connection       当前数据库连接
     * @param configuredSchema 配置或方言已经确定的Schema
     * @return 实际Schema，旧驱动不支持时返回配置值
     * @throws SQLException 连接读取失败时抛出
     */
    public static String readSchema(Connection connection, String configuredSchema) throws SQLException {
        try {
            return connection.getSchema();
        } catch (SQLFeatureNotSupportedException | AbstractMethodError ex) {
            return configuredSchema;
        }
    }

    /**
     * DM结果列精度可能返回字节上限，声明长度改从JDBC字段目录读取，不按固定倍数猜测。
     *
     * @param session     当前目标会话
     * @param connection  本次读取连接，事务内复用当前连接
     * @param tableName   实际表名
     * @param definitions 完整采集时接收DM实际默认值；普通字段读取传null，不额外读取默认值
     * @return DM字段名与声明长度，其他数据库返回空集合
     * @throws SQLException 目录查询失败或字段长度不能确认时抛出
     */
    private static Map<String, Long> readCharacterLengths(ModelPublishDatabaseSession session,
                                                          Connection connection, String tableName,
                                                          Map<String, DbColumn> definitions) throws SQLException {
        Map<String, Long> result = new HashMap<>();
        if (!(session.getDialect() instanceof DM8Dialect)) {
            return result;
        }
        String[] scope = metadataScope(session, connection, tableName);
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(scope[0], scope[1], pattern(metadata, scope[2]), null)) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                long length = columns.getLong("COLUMN_SIZE");
                if (!columns.wasNull()) {
                    result.put(name, length);
                }
                if (definitions != null) {
                    // COLUMN_DEF是驱动报告的真实默认值；SQL NULL表示无默认值，不能用模型默认值补齐。
                    DbColumn definition = DbColumn.builder().colName(name).dataDefault(columns.getString("COLUMN_DEF")).build();
                    if (definitions.put(name, definition) != null) {
                        throw new SQLException("真实字段名称重复，不能可靠核验");
                    }
                }
            }
        }
        return result;
    }

    /**
     * 字符串长度使用DM目录中的声明值；数字精度和其他数据库沿用原来的读取方式。
     *
     * @param session  当前目标会话
     * @param metadata SELECT结果列元数据
     * @param position 字段序号，从1开始
     * @param lengths  DM目录已经读取的声明长度
     * @return 本字段长度或原有数值精度
     * @throws SQLException DM字符串声明长度缺失时抛出，不能按字节数继续发布
     */
    private static long characterLength(ModelPublishDatabaseSession session, ResultSetMetaData metadata,
                                        int position, Map<String, Long> lengths) throws SQLException {
        int jdbcType = metadata.getColumnType(position);
        if (session.getDialect() instanceof DM8Dialect
                && (jdbcType == java.sql.Types.CHAR || jdbcType == java.sql.Types.VARCHAR
                || jdbcType == java.sql.Types.NCHAR || jdbcType == java.sql.Types.NVARCHAR
                || jdbcType == java.sql.Types.LONGVARCHAR || jdbcType == java.sql.Types.LONGNVARCHAR)) {
            Long length = lengths.get(metadata.getColumnName(position));
            if (length == null) {
                throw new SQLException("DM字符串声明长度读取不完整");
            }
            return length;
        }
        return metadata.getPrecision(position);
    }

    /**
     * 保存成功结果前，重新核对全部表，再读取实际字段和模型管理的索引。
     * 模型ID、字段ID和启停状态来自本次配置，类型、约束、顺序和注释来自数据库。
     * 实际库额外的字段和索引不接管为模型对象，也不因它们存在而阻断发布。
     *
     * @param session  已核对连接身份的目标库会话
     * @param expected 原实例保存的完整目标结构
     * @return 可写入正式物化表和字段记录的独立结构
     * @throws SQLException 实际结构读取失败时抛出，不用计划值冒充读取结果
     */
    public static ModelMaterializedStructure readActualStructure(ModelPublishDatabaseSession session,
                                                                 ModelMaterializedStructure expected) throws SQLException {
        List<String> problems = new ArrayList<>();
        ModelMaterializedStructure actual = readActualStructure(session, expected, problems);
        if (!problems.isEmpty()) {
            throw new ServiceException("正式保存前实际结构核验未通过：" + problems.get(0));
        }
        return actual;
    }

    /**
     * 采集和核验同一阶段的完整结构。明确不一致写入problems并返回null；读取异常继续抛出。
     * 执行器据此区分PARTIAL_FAILED与UNKNOWN，不能把读取失败当成表、字段或索引不存在。
     */
    public static ModelMaterializedStructure readActualStructure(ModelPublishDatabaseSession session,
                                                                 ModelMaterializedStructure expected, List<String> problems) throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("READ_ACTUAL_STRUCTURE", null, null);
        try {
            // 复制的是模型身份与使用状态；数据库类型、列位置、默认值、索引名等随后逐项实采，不能直接把计划当结果。
            ModelMaterializedStructure actual = new ModelMaterializedStructure();
            for (ModelMaterializedTableDefinition table : expected.getTables()) {
                // 表存在性沿用原核验规则；明确不存在属于结构不一致，读取异常仍向上交给UNKNOWN处理。
                ModelSchemaObjectRef object = new ModelSchemaObjectRef();
                object.setObjectType(ModelSchemaObjectType.TABLE);
                object.setTableName(table.getTableName());
                if (objectAbsent(session, object)) {
                    issue(problems, table.getTableName(), "目标表不存在");
                    return null;
                }
                ModelTableMetadata tableMetadata = readTableMetadata(session, table.getTableName());
                checkColumns(session, table, problems, tableMetadata.columns, tableMetadata.definitions);
                checkIndexes(table, problems, tableMetadata.primaryKey, tableMetadata.indexes);
                if (!problems.isEmpty()) {
                    return null;
                }
                ModelMaterializedTableDefinition result = JSON.parseObject(JSON.toJSONString(table), ModelMaterializedTableDefinition.class);
                // 核验与正式结构构建共用这一份实际采集结果，不再次查询相同列、主键或索引。
                applyActualColumns(session, result, tableMetadata);
                applyActualIndexes(result, tableMetadata);
                // 注释按发布定义保存，SQL执行成功后不再单独读取表注释。
                actual.getTables().add(result);
            }
            return actual;
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 用本阶段采集结果填入实际类型、字符长度、精度、可空、自增和列顺序，不再读取数据库。
     * 不适用的长度和精度留空，避免把驱动为整数返回的位数误当作模型小数精度。
     */
    private static void applyActualColumns(ModelPublishDatabaseSession session,
                                           ModelMaterializedTableDefinition table, ModelTableMetadata metadata) {
        Map<String, ModelMaterializedColumnDefinition> columns = table.getColumns().stream()
                .collect(Collectors.toMap(ModelMaterializedColumnDefinition::getColumnName, value -> value));
        List<ModelMaterializedColumnDefinition> ordered = new ArrayList<>();
        for (DbColumn row : metadata.columns) {
            String name = row.getColName();
            ModelMaterializedColumnDefinition column = columns.get(name);
            if (column == null) {
                // 只保存本次和历史发布管理的列，跳过实际库中额外的字段，避免空指针或接管外部字段。
                continue;
            }
            DbColumn definition = metadata.definitions.get(name);
            column.setPhysicalType(actualColumnType(session, row, definition));
            column.setCharacterLength("STRING".equals(column.getLogicalType()) ? number(row.getDataLength()) : null);
            column.setLengthUnit("STRING".equals(column.getLogicalType()) ? "CHAR" : null);
            column.setNumericPrecision("DECIMAL".equals(column.getLogicalType()) ? Integer.valueOf(row.getDataPrecision()) : null);
            column.setNumericScale("DECIMAL".equals(column.getLogicalType()) ? Integer.valueOf(row.getDataScale()) : null);
            column.setNullable(session.getDialect().observedColumnNullable(row, definition));
            // 自增序列表达式是数据库生成机制，不是模型配置的默认值。
            column.setDefaultValue(Boolean.TRUE.equals(metadata.identities.get(name)) ? null
                    : defaultValue(definition.getDataDefault()));
            if (table.getIndexes().stream().anyMatch(index -> index.getIndexType() == ModelIndexType.PRIMARY_KEY)) {
                column.setPrimaryKeyFlag(metadata.primaryKey != null && metadata.primaryKey.getColumnNames().contains(name));
            }
            column.setIdentityFlag(metadata.identities.get(name));
            column.setColumnPosition(row.getColPosition());
            // 列注释保留本次发布定义，不为核对说明再查询数据库。
            ordered.add(column);
        }
        table.setColumns(ordered);
    }

    /**
     * 保留索引稳定标识和用途，实际名称、列顺序取本阶段采集结果；主键使用数据库真实约束名。
     */
    private static void applyActualIndexes(ModelMaterializedTableDefinition table, ModelTableMetadata metadata) {
        for (ModelMaterializedIndexDefinition index : table.getIndexes()) {
            DbIndex actual = index.getIndexType() == ModelIndexType.PRIMARY_KEY ? metadata.primaryKey : metadata.indexes.stream()
                                                                                                        .filter(value -> Objects.equals(index.getIndexName(), value.getIndexName())).findFirst().orElse(null);
            // 名称、列和唯一性已经用同一份数据核验过，此处只补数据库真实命名，不重新查主键或索引。
            index.setIndexName(actual.getIndexName());
            index.setColumnNames(new ArrayList<>(actual.getColumnNames()));
        }
    }

    /**
     * 判断旧表是否有数据，只读聚合结果，不返回任何业务数据。
     */
    public static long rowCount(ModelPublishDatabaseSession session, String tableName) throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("COUNT_ROWS", null, null);
        try {
            requireIdentifier(tableName);
            String sql = "SELECT COUNT(*) FROM " + session.getDialect().getTableName(session.getProperty(), tableName);
            try (Connection connection = session.getQuery().getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new ServiceException("目标表数据量读取结果无效");
                }
                return rows.getLong(1);
            }
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 数据库类型别名先交给方言统一，再去掉长度参数，长度和精度另行严格核对。
     */
    public static String type(ModelPublishDatabaseSession session, DbColumn column) {
        String type = session.getDialect().toStandardColumn(column).getDataType();
        return type == null ? "" : type.replaceAll("\\(.*", "").trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 物理类型由 common 结合当前连接解析，业务层不判断数据库兼容模式。
     */
    public static String physicalType(ModelPublishDatabaseSession session, DbColumn column) {
        try (Connection connection = session.getQuery().getConnection()) {
            return session.getDialect().resolveColumnType(column, connection);
        } catch (SQLException ex) {
            throw new ServiceException("无法解析当前目标库的物理字段类型");
        }
    }

    /**
     * 去掉数据库为静态默认值加的括号和单引号，再按数字或字符串比较。
     */
    public static String defaultValue(String value) {
        if (value == null || value.trim().isEmpty() || "NULL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String result = value.trim();
        while (result.startsWith("(") && result.endsWith(")")) {
            result = result.substring(1, result.length() - 1).trim();
        }
        if (result.length() > 2 && (result.startsWith("N'") || result.startsWith("n'"))) {
            result = result.substring(1);
        }
        if (result.startsWith("'") && result.endsWith("'")) {
            return result.substring(1, result.length() - 1).replace("''", "'");
        }
        try {
            return new BigDecimal(result).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return result;
        }
    }

    /**
     * 可空数字转为common原有的字符串参数。
     */
    private static String text(Number value) {
        return value == null ? null : value.toString();
    }

    /**
     * 检查一张表的全部物理列和索引，TARGET和执行后核验共用同一套比较方法。
     */
    public static List<String> structureProblems(ModelPublishDatabaseSession session,
                                                 ModelMaterializedTableDefinition table) throws SQLException {
        return structureProblems(session, table, null);
    }

    /**
     * TARGET可传入本阶段刚读取的字段列表，避免缺列判断和结构核对各查一次。
     * 列表为空引用时仍重新读取；执行后核验和恢复流程不复用TARGET执行前的数据。
     * 默认值、主键及索引继续读取实际数据库，比较规则与原方法一致。
     */
    public static List<String> structureProblems(ModelPublishDatabaseSession session,
                                                 ModelMaterializedTableDefinition table,
                                                 List<DbColumn> columns) throws SQLException {
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("CHECK_TABLE", null, null);
        try {
            List<String> problems = new ArrayList<>();
            if (columns == null) {
                checkColumns(session, table, problems);
            } else {
                Map<String, DbColumn> definitions = readColumnDefinitions(session, table.getTableName());
                checkColumns(session, table, problems, columns, definitions);
            }
            checkIndexes(session, table, problems);
            return problems;
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 加列、扩长只核对当前列，不把同表其他列误认为本次额外新增。
     */
    public static List<String> columnProblems(ModelPublishDatabaseSession session, String tableName,
                                              ModelMaterializedColumnDefinition column) throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("CHECK_COLUMN", null, null);
        try {
            ModelMaterializedTableDefinition table = new ModelMaterializedTableDefinition();
            table.setTableName(tableName);
            table.getColumns().add(column);
            List<String> problems = new ArrayList<>();
            checkColumns(session, table, problems);
            return problems;
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 索引要同时核对名称、列顺序和唯一性，普通唯一索引不能代替主键。
     */
    public static boolean indexMatches(ModelPublishDatabaseSession session, String tableName,
                                       ModelMaterializedIndexDefinition expected) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("CHECK_INDEX", null, null);
        try {
            if (expected.getColumnNames() == null || expected.getColumnNames().isEmpty()) {
                throw new ServiceException("索引核验缺少字段信息");
            }
            DbIndex actual = expected.getIndexType() == ModelIndexType.PRIMARY_KEY
                    ? session.getQuery().getTablePrimaryKey(session.getProperty(), tableName)
                    : indexes(session, tableName).stream().filter(index -> Objects.equals(index.getIndexName(), expected.getIndexName()))
                      .findFirst().orElse(null);
            return actual != null && expected.getColumnNames().equals(actual.getColumnNames())
                    && (expected.getIndexType() == ModelIndexType.PRIMARY_KEY
                    || ((expected.getIndexType() == ModelIndexType.UNIQUE) == Boolean.TRUE.equals(actual.getUnique())
                    && !Boolean.TRUE.equals(actual.getPrimaryKey())));
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 注释前只确认目标表或列存在，不读取整表结构、主键和索引。
     * 列检查执行不返回数据的查询；对象缺失或无权限时抛出异常，由调用方停止本条SQL。
     */
    public static boolean objectPresent(ModelPublishDatabaseSession session, ModelSchemaObjectRef object) throws SQLException {
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("CHECK_PRESENT", null, null);
        try {
            requireIdentifier(object.getTableName());
            switch (object.getObjectType()) {
                case TABLE:
                    int count = session.getQuery().generateCheckTableExistsSQL(session.getProperty(), object.getTableName());
                    if (count < 0 || count > 1) {
                        throw new ServiceException("目标表存在情况无法唯一确认");
                    }
                    return count == 1;
                case COLUMN:
                    requireIdentifier(object.getColumnName());
                    // 名称经过校验并交给方言加引号；恒假条件只定位这一列，不读取业务数据或全表列目录。
                    hasRows(session, "SELECT " + session.getDialect().quoteIdentifier(object.getColumnName())
                            + " FROM " + session.getDialect().getTableName(session.getProperty(), object.getTableName())
                            + " WHERE 1=0");
                    return true;
                default:
                    throw new ServiceException("注释前只支持核验表或列是否存在");
            }
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 对象不存在必须由真实查询确认；查询异常交给调用方记为未知。
     */
    public static boolean objectAbsent(ModelPublishDatabaseSession session, ModelSchemaObjectRef object) throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("CHECK_ABSENT", null, null);
        try {
            requireIdentifier(object.getTableName());
            switch (object.getObjectType()) {
                case TABLE:
                    int count = session.getQuery().generateCheckTableExistsSQL(session.getProperty(), object.getTableName());
                    if (count < 0 || count > 1) {
                        throw new ServiceException("目标表存在情况无法唯一确认");
                    }
                    return count == 0 && !namedObjectExists(session, object.getTableName());
                case COLUMN:
                    requireIdentifier(object.getColumnName());
                    return readColumns(session, object.getTableName()).stream()
                            .noneMatch(column -> object.getColumnName().equals(column.getColName()));
                case INDEX:
                    requireIdentifier(object.getIndexName());
                    return indexes(session, object.getTableName()).stream()
                            .noneMatch(index -> object.getIndexName().equals(index.getIndexName()));
                default:
                    throw new ServiceException("当前对象不是可核验的物理表、列或索引");
            }
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 唯一索引按数据库的NULL规则核对。当前业务只有单列唯一，不接收联合唯一。
     */
    public static boolean noDuplicateValues(ModelPublishDatabaseSession session, String tableName, List<String> columns) throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("CHECK_DUPLICATES", null, null);
        try {
            if (columns == null || columns.size() != 1) {
                throw new ServiceException("唯一核验必须指定一个业务字段");
            }
            requireIdentifier(tableName);
            requireIdentifier(columns.get(0));
            String column = session.getDialect().quoteIdentifier(columns.get(0));
            String table = session.getDialect().getTableName(session.getProperty(), tableName);
            // SQL Server的唯一索引最多保留一个NULL，其余方言的NULL不参与普通唯一重复判断。
            String where = session.getDialect() instanceof SQLServer2008Dialect ? "" : " WHERE " + column + " IS NOT NULL";
            return !hasRows(session, "SELECT " + column + " FROM " + table + where + " GROUP BY " + column + " HAVING COUNT(*) > 1");
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 必填核验只查是否存在空值，不读取或记录业务字段内容。
     */
    public static boolean noNullValues(ModelPublishDatabaseSession session, String tableName, String columnName) throws SQLException {
        requireIdentifier(tableName);
        requireIdentifier(columnName);
        return !hasRows(session, "SELECT 1 FROM " + session.getDialect().getTableName(session.getProperty(), tableName)
                + " WHERE " + session.getDialect().quoteIdentifier(columnName) + " IS NULL");
    }

    /**
     * 只取是否存在结果，并及时关闭本次借用的连接和结果集。
     */
    private static boolean hasRows(ModelPublishDatabaseSession session, String sql) throws SQLException {
        try (Connection connection = session.getQuery().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setMaxRows(1);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    /**
     * SQL中的名称来自已校验的模型，但执行核验仍检查一次，禁止把表达式当作对象名。
     */
    private static void requireIdentifier(String name) {
        if (!ModelPublishUtils.identifier(name)) {
            throw new ServiceException("物理对象名称无效，不能执行核验查询");
        }
    }

    /**
     * JDBC的下划线和百分号是通配符；精确查询时必须转义。
     */
    private static String pattern(DatabaseMetaData metadata, String name) throws SQLException {
        String escape = metadata.getSearchStringEscape();
        if (escape == null || escape.isEmpty()) {
            return name;
        }
        return name.replace(escape, escape + escape).replace("_", escape + "_").replace("%", escape + "%");
    }

    /**
     * 索引读取失败不能当作空列表处理。
     */
    private static List<DbIndex> indexes(ModelPublishDatabaseSession session, String tableName) {
        List<DbIndex> indexes = session.getQuery().getTableIndexes(session.getProperty(), tableName);
        if (indexes == null) {
            throw new ServiceException("真实索引读取结果无效");
        }
        return indexes;
    }

    /**
     * 统一读取和核对字段的类型、长度、必填及默认值。
     */
    private static void checkColumns(ModelPublishDatabaseSession session, ModelMaterializedTableDefinition table,
                                     List<String> issues) throws SQLException {
        List<DbColumn> rows = readColumns(session, table.getTableName());
        Map<String, DbColumn> definitions = readColumnDefinitions(session, table.getTableName());
        checkColumns(session, table, issues, rows, definitions);
    }

    /**
     * 使用本阶段已采集的列和默认值核对，比较规则与逐SQL核验一致，不再读取数据库。
     */
    private static void checkColumns(ModelPublishDatabaseSession session, ModelMaterializedTableDefinition table,
                                     List<String> issues, List<DbColumn> rows,
                                     Map<String, DbColumn> defaults) {
        Map<String, DbColumn> columns = new LinkedHashMap<>();
        for (DbColumn column : rows) {
            if (columns.put(column.getColName(), column) != null) {
                throw new ServiceException("真实字段名称重复，不能可靠核验");
            }
        }
        // 只确认模型需要的列，实际库额外存在的列不参与数量比较，也不阻断发布。
        // 逐列核对名称、类型、可空、长度/精度和默认值；停用列仍属于物理结构，也必须参与核对。
        for (ModelMaterializedColumnDefinition expected : table.getColumns()) {
            String path = table.getTableName() + "." + expected.getColumnName();
            DbColumn actual = columns.get(expected.getColumnName());
            DbColumn actualDefault = defaults.get(expected.getColumnName());
            if (actual == null || actualDefault == null) {
                issue(issues, path, "上次成功的物理字段缺失或约束读取不完整");
                continue;
            }
            actual.setDataType(actualColumnType(session, actual, actualDefault));
            DbColumn converted = ModelMaterializationDbUtils.column(session, expected);
            DbColumn expectedType = DbColumn.builder().dataType(expected.getPhysicalType() == null
                    ? physicalType(session, converted) : expected.getPhysicalType()).build();
            if (!session.getDialect().matchesColumnType(expectedType, actual)
                    || !Objects.equals(expected.getNullable(), session.getDialect().observedColumnNullable(actual, actualDefault))) {
                issue(issues, path, "真实字段类型或必填约束与上次成功结构不一致");
            }
            if ("STRING".equals(expected.getLogicalType()) && !Objects.equals(expected.getCharacterLength(), number(actual.getDataLength()))) {
                issue(issues, path, "真实字符串长度与上次成功结构不一致");
            }
            if ("DECIMAL".equals(expected.getLogicalType())
                    && (!Objects.equals(longValue(expected.getNumericPrecision()), number(actual.getDataPrecision()))
                    || !Objects.equals(longValue(expected.getNumericScale()), number(actual.getDataScale())))) {
                issue(issues, path, "真实小数精度与上次成功结构不一致");
            }
            if (!Boolean.TRUE.equals(expected.getIdentityFlag())
                    && !Objects.equals(ModelMaterializationDbUtils.defaultValue(converted.getDataDefault()),
                    ModelMaterializationDbUtils.defaultValue(actualDefault.getDataDefault()))) {
                issue(issues, path, "真实默认值与上次成功结构不一致");
            }
        }
    }

    /**
     * 查询结果可能把OceanBase TEXT返回为VARCHAR，物理声明类型沿用已读取的字段目录。
     */
    private static String actualColumnType(ModelPublishDatabaseSession session, DbColumn row, DbColumn definition) {
        if (session.getDialect() instanceof SQLServer2008Dialect) {
            if (definition == null || definition.getDataType() == null || definition.getDataType().trim().isEmpty()) {
                throw new ServiceException("真实字段目录类型缺失，不能确认实际结构");
            }
            String type = definition.getDataType();
            // 目录长度为-1表示MAX，保留该物理类型信息以区别有界NVARCHAR，正式保存后也不丢失。
            if ("NVARCHAR".equalsIgnoreCase(type) && "-1".equals(definition.getDataLength())) {
                return "NVARCHAR(MAX)";
            }
            return type;
        }
        return session.getDialect().observedColumnType(row);
    }

    /**
     * 只核对模型管理的主键和索引，实际库额外的索引不阻断发布。
     */
    private static void checkIndexes(ModelPublishDatabaseSession session, ModelMaterializedTableDefinition table,
                                     List<String> issues) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("CHECK_INDEXES", null, null);
        try {
            DbIndex primaryKey = session.getQuery().getTablePrimaryKey(session.getProperty(), table.getTableName());
            List<DbIndex> indexes = session.getQuery().getTableIndexes(session.getProperty(), table.getTableName());
            if (indexes == null) {
                throw new ServiceException("真实索引读取结果无效");
            }
            checkIndexes(table, issues, primaryKey, indexes);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 用同一次采集结果确认计划中的主键和索引都在，不检查实际库额外的对象。
     */
    private static void checkIndexes(ModelMaterializedTableDefinition table, List<String> issues,
                                     DbIndex primaryKey, List<DbIndex> indexes) {
        // 只核对计划管理的主键和索引；实际库额外的普通、唯一索引或主键不参与比较。
        for (ModelMaterializedIndexDefinition expected : table.getIndexes()) {
            if (expected.getIndexType() == ModelIndexType.PRIMARY_KEY) {
                if (primaryKey == null || !expected.getColumnNames().equals(primaryKey.getColumnNames())) {
                    issue(issues, table.getTableName(), "真实主键列与上次成功结构不一致");
                }
            } else {
                DbIndex actual = indexes.stream().filter(index -> Objects.equals(index.getIndexName(), expected.getIndexName()))
                        .findFirst().orElse(null);
                if (actual == null || !expected.getColumnNames().equals(actual.getColumnNames())
                        || (expected.getIndexType() == ModelIndexType.UNIQUE) != Boolean.TRUE.equals(actual.getUnique())) {
                    issue(issues, table.getTableName() + "." + expected.getIndexName(), "真实索引与上次成功结构不一致");
                }
            }
        }
    }

    /**
     * 表和视图等同名对象都算占用，查询范围沿用common方言。
     */
    public static boolean namedObjectExists(ModelPublishDatabaseSession session, String tableName) throws SQLException {
        try (Connection connection = session.getQuery().getConnection()) {
            String[] scope = metadataScope(session, connection, tableName);
            DatabaseMetaData metadata = connection.getMetaData();
            String pattern = pattern(metadata, scope[2]);
            try (ResultSet rows = metadata.getTables(scope[0], scope[1], pattern, null)) {
                while (rows.next()) {
                    if (scope[2].equals(rows.getString("TABLE_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * 从common限定名取得JDBC查询范围，保留原方言大小写规则。
     */
    public static String[] metadataScope(ModelPublishDatabaseSession session, Connection connection, String tableName) throws SQLException {
        String[] parts = session.getDialect().getTableName(session.getProperty(), tableName).split("\\.");
        for (int index = 0; index < parts.length; index++) {
            parts[index] = parts[index].replace("\"", "").replace("`", "").replace("[", "").replace("]", "");
        }
        String catalog = connection.getCatalog();
        String configuredSchema = session.getProperty().getSid();
        if (session.getDialect() instanceof SQLServer2008Dialect
                && (configuredSchema == null || configuredSchema.trim().isEmpty())) {
            configuredSchema = "dbo";
        }
        String schema = readSchema(connection, configuredSchema);
        if (parts.length == 3) {
            catalog = parts[0];
            schema = parts[1];
        } else if (parts.length == 2) {
            if (session.getDialect() instanceof MySqlDialect) {
                catalog = parts[0];
                schema = null;
            } else {
                schema = parts[0];
            }
        }
        return new String[]{catalog, schema, parts[parts.length - 1]};
    }

    /**
     * 将精度参数统一成可空Long，避免Java数字类型不同导致误判。
     */
    private static Long number(String value) {
        return value == null ? null : Long.valueOf(value);
    }

    /**
     * 将可空整数转换成同样的比较类型。
     */
    private static Long longValue(Integer value) {
        return value == null ? null : value.longValue();
    }

    /**
     * 只记录物理对象和差异说明，不带连接参数及业务数据。
     */
    private static void issue(List<String> issues, String path, String message) {
        issues.add(path + "：" + message);
    }

    /**
     * 补偿SQL必须对应common为本次新表或新索引生成的单条语句，禁止删列、删旧表、级联和手工脚本。
     */
    public static String compensationSql(ModelPublishDatabaseSession session, ModelSqlExecutionStep step) {
        // 执行前依据步骤类型重新生成单条逆向语句，并与保存计划核对；不能直接执行计划文本里任意的删除SQL。
        String generated;
        String prefix;
        if (step.getObject() != null && step.getOperation() == ModelDdlOperation.CREATE_INDEX
                && step.getObject().getObjectType() == ModelSchemaObjectType.INDEX) {
            generated = session.getQuery().generateDropIndexSQL(session.getProperty(),
                    step.getObject().getTableName(), step.getObject().getIndexName());
            prefix = "DROP INDEX ";
        } else if (step.getObject() != null && step.getOperation() == ModelDdlOperation.CREATE_TABLE
                && step.getObject().getObjectType() == ModelSchemaObjectType.TABLE) {
            generated = session.getDialect().generateDropTableSQL(session.getProperty(),
                    session.getDialect().getTableName(session.getProperty(), step.getObject().getTableName()));
            prefix = "DROP TABLE ";
        } else {
            throw new ServiceException("补偿仅限已核验属于本次发布的新表或新索引");
        }
        String saved = step.getCompensationSql();
        if (generated == null || saved == null || !singleStatement(generated).equals(singleStatement(saved))) {
            throw new ServiceException("补偿SQL与本次新增对象或common生成结果不一致");
        }
        String sql = singleStatement(saved);
        if (!sql.toUpperCase(Locale.ROOT).startsWith(prefix) || sql.contains(";") || sql.contains("--") || sql.contains("/*")
                || java.util.regex.Pattern.compile("(?i)\\b(?:CASCADE|FORCE|PURGE)\\b").matcher(sql).find()) {
            throw new ServiceException("补偿SQL包含级联、强制或多条语句，停止清理");
        }
        return sql;
    }

    /**
     * 仅移除方言生成的末尾分号，内部任何分号仍由单语句检查拦截。
     */
    private static String singleStatement(String sql) {
        String value = sql.trim();
        return value.endsWith(";") ? value.substring(0, value.length() - 1).trim() : value;
    }

    /**
     * 计划和执行共用common建表前缀规则，包含HANA列存表；单语句及破坏性操作由调用方另行检查。
     */
    public static boolean isCreateTableSql(String sql) {
        if (sql == null) {
            return false;
        }
        String normalized = sql.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("CREATE TABLE ") || normalized.startsWith("CREATE COLUMN TABLE ");
    }

    /**
     * 保存计划之后仍检查正向SQL边界；执行器只允许新增和扩长，不执行删除、截断或多语句脚本。
     */
    public static void assertForwardSql(ModelSqlExecutionStep step) {
        if (step == null || step.getOperation() == null || step.getForwardSql() == null || step.getForwardSql().trim().isEmpty()) {
            throw new ServiceException("正向SQL或操作类型为空");
        }
        // 执行入口再次核对SQL类型与批准动作一致，并拦截删除、截断和多语句；这层检查不代替三阶段校验。
        String sql = step.getForwardSql().replaceAll("'(?:''|[^'])*'", "''").trim().toUpperCase(Locale.ROOT);
        if (step.getOperation() == tech.qiantong.qdata.mdm.model.enums.materialization.ModelDdlOperation.RELAX_COLUMN) {
            sql = sql.replaceFirst("\\bDROP\\s+NOT\\s+NULL\\s*$", "");
        }
        if (step.getOperation() == tech.qiantong.qdata.mdm.model.enums.materialization.ModelDdlOperation.ALTER_DEFAULT) {
            sql = approvedDefaultSql(sql);
        }
        if (sql.contains(";") || sql.contains("--") || sql.contains("/*")
                || java.util.regex.Pattern.compile("\\b(?:DROP|TRUNCATE|RENAME)\\b").matcher(sql).find()) {
            throw new ServiceException("正向SQL包含未允许的操作或多语句内容");
        }
        boolean valid;
        switch (step.getOperation()) {
            case CREATE_TABLE:
                valid = isCreateTableSql(sql);
                break;
            case ADD_COLUMN:
            case EXPAND_COLUMN:
            case RELAX_COLUMN:
            case ALTER_DEFAULT:
                valid = sql.startsWith("ALTER TABLE ") && !sql.contains("PRIMARY KEY");
                break;
            case CREATE_INDEX:
                boolean restorePrimary = step.getPostconditions().stream().anyMatch(condition -> condition.getType()
                        == tech.qiantong.qdata.mdm.model.enums.materialization.ModelVerificationType.STRUCTURE_MATCH
                        && "PRIMARY_KEY".equals(condition.getExpected().getString("indexType")));
                valid = sql.startsWith("CREATE INDEX ") || sql.startsWith("CREATE UNIQUE INDEX ")
                        || (restorePrimary && sql.startsWith("ALTER TABLE ") && sql.contains(" ADD ") && sql.contains(" PRIMARY KEY"));
                break;
            case COMMENT:
                valid = sql.startsWith("COMMENT ON TABLE ") || sql.startsWith("COMMENT ON COLUMN ")
                        || sql.matches("(?s)^EXEC\\s+(?:SYS\\.)?SP_(?:ADD|UPDATE)EXTENDEDPROPERTY\\b.*");
                break;
            default:
                valid = false;
        }
        if (!valid) {
            throw new ServiceException("正向SQL与保存的批准操作不一致");
        }
    }

    /**
     * 默认值动作仅额外允许撤销DEFAULT或其独立约束，不放开删表、删列、主键等操作。
     */
    public static String approvedDefaultSql(String sql) {
        return sql.replaceFirst("(?i)\\bDROP\\s+DEFAULT\\s*$", "")
                .replaceFirst("(?i)\\bDROP\\s+CONSTRAINT\\s+(?:\\[[^\\]]+\\]|\"[^\"]+\"|[A-Z0-9_$]+)\\s*$", "");
    }

    /**
     * 一张表本阶段的采集结果，只在当前方法调用内使用，不跨SQL、事务或发布缓存。
     */
    private static class ModelTableMetadata {
        /**
         * JDBC报告的列类型、长度、精度、可空及位置。
         */
        private List<DbColumn> columns;
        /**
         * 本轮读取的实际默认值；其他数据库还通过common取得必要的目录类型。
         */
        private Map<String, DbColumn> definitions;
        /**
         * 数据库实际主键；没有主键时为空。
         */
        private DbIndex primaryKey;
        /**
         * 本次真实索引，可能包含模型管理范围之外的索引。
         */
        private List<DbIndex> indexes;
        /**
         * JDBC报告的自增标记，与列元数据一起读取。
         */
        private Map<String, Boolean> identities = new LinkedHashMap<>();
    }

    /**
     * 同一张表的字段目录、JDBC列、主键及索引各读一次，后续比较与记录共用这些结果。
     * DM默认值和字符长度复用同一字段目录，其他数据库保留common补充属性。
     */
    private static ModelTableMetadata readTableMetadata(ModelPublishDatabaseSession session, String tableName) throws SQLException {
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("READ_TABLE_METADATA", null, null);
        try {
            ModelTableMetadata metadata = new ModelTableMetadata();
            if (session.getDialect() instanceof DM8Dialect) {
                // DM原本就要查字段目录修正字符长度，同一结果顺带取默认值，省去common的重复字段查询。
                // 类型、精度、可空、自增和顺序仍来自本次SELECT结果元数据，不用目录字节长度推算。
                metadata.definitions = new LinkedHashMap<>();
                metadata.columns = readColumns(session, tableName, metadata.identities, metadata.definitions);
            } else {
                // 各库目录与驱动能力不同，其他数据库保留原来的默认值和类型读取方式。
                metadata.definitions = readColumnDefinitions(session, tableName);
                metadata.columns = readColumns(session, tableName, metadata.identities);
            }
            metadata.primaryKey = session.getQuery().getTablePrimaryKey(session.getProperty(), tableName);
            metadata.indexes = indexes(session, tableName);
            return metadata;
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 默认值及注释沿用common目录查询；查询失败或列名重复时停止，不能静默覆盖字段。
     */
    private static Map<String, DbColumn> readColumnDefinitions(ModelPublishDatabaseSession session, String tableName) {
        List<DbColumn> definitions = session.getQuery().getTableColumns(session.getProperty(), tableName);
        if (definitions == null) {
            throw new ServiceException("真实字段约束读取结果无效");
        }
        return definitions.stream().collect(Collectors.toMap(DbColumn::getColName, column -> column));
    }
}
