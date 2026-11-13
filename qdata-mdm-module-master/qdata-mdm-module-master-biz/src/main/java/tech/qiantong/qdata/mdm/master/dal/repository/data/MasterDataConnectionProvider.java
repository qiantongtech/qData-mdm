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

package tech.qiantong.qdata.mdm.master.dal.repository.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import tech.qiantong.qdata.mdm.common.database.DataSourceFactory;
import tech.qiantong.qdata.mdm.common.database.datasource.SessionDataSourceFactoryBean;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.utils.DatabaseUtil;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.ingest.api.datasource.IngestDatasourceApi;
import tech.qiantong.qdata.mdm.ingest.api.datasource.dto.IngestDatasourceRespDTO;
import tech.qiantong.qdata.mdm.common.database.sql.SqlTemplate.Operation;
import tech.qiantong.qdata.mdm.common.database.sql.SqlTemplate;
import tech.qiantong.qdata.mdm.common.database.sql.SqlTemplate.Table;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.RecordMutation;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.TableMutation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 主数据目标连接入口；连接、字段读取、SQL 生成及执行均复用 common。
 */
@Component
@Slf4j
public class MasterDataConnectionProvider implements DisposableBean {
    private static final Pattern TABLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*(\\.[A-Za-z_][A-Za-z0-9_$]*)?");
    private final IngestDatasourceApi datasourceApi;
    private final DataSourceFactory factory;
    private final SessionDataSourceFactoryBean sessionFactory;
    private final ConcurrentMap<String, Target> targets = new ConcurrentHashMap<>();

    public MasterDataConnectionProvider(IngestDatasourceApi datasourceApi, DataSourceFactory factory,
                                        SessionDataSourceFactoryBean sessionFactory) {
        this.datasourceApi = datasourceApi;
        this.factory = factory;
        this.sessionFactory = sessionFactory;
    }

    /**
     * 主表、子表及关联校验均使用成功发布记录中的数据源。
     */
    public TableAccess forTable(Long datasourceId, String name) {
        if (datasourceId == null || datasourceId <= 0) throw new ServiceException("模型数据源ID无效");
        if (name == null || !TABLE.matcher(name).matches()) throw new ServiceException("模型物理表名无效");
        IngestDatasourceRespDTO config = datasourceApi.getDatasource(datasourceId);
        if (config == null || !Boolean.TRUE.equals(config.getValidFlag())) {
            throw new ServiceException("数据源 " + datasourceId + " 不存在或未启用");
        }
        String key = config.getDatasourceType() + "\n" + config.getIp() + "\n" + config.getPort() + "\n" + config.getDatasourceConfig();
        Target target = targets.computeIfAbsent(key, ignored -> createTarget(config, factory));
        return new TableAccess(target, name);
    }

    private Target createTarget(IngestDatasourceRespDTO config, DataSourceFactory selectedFactory) {
        try {
            DbQueryProperty property = config.getDbQueryProperty();
            DbDialect dialect = config.getDialect();
            if (dialect == null) throw new ServiceException("模型数据源的数据库类型不受支持");
            return new Target(selectedFactory.createDbQuery(property), dialect, property);
        } catch (Exception error) {
            ServiceException failure = new ServiceException("模型数据源初始化失败，请检查连接配置");
            failure.initCause(error);
            throw failure;
        }
    }

    @Override
    public void destroy() {
        for (Target target : targets.values()) target.query.close();
        targets.clear();
    }

    /**
     * 每次查询请求独立创建；上下文内串行复用连接，结束时统一关闭。
     */
    public RequestContext openRequest() {
        return new RequestContext();
    }

    /** 供约定失败返回0的统计接口使用，资源释放异常也不记录失败日志。 */
    public RequestContext openQuietRequest() {
        RequestContext context = new RequestContext();
        context.logCloseFailures = false;
        return context;
    }

    public class RequestContext implements AutoCloseable {
        private final Map<Long, Target> requestTargets = new LinkedHashMap<>();
        private final Map<Long, ServiceException> failures = new LinkedHashMap<>();
        private boolean closed;
        private boolean logCloseFailures = true;

        /**
         * 直接使用成功发布记录中的列名，不查询目标库字段元数据。
         */
        public TableAccess forTable(Long datasourceId, String name, Map<String, String> publishedColumns) {
            if (closed) throw new ServiceException("查询请求数据库上下文已关闭");
            if (publishedColumns == null || publishedColumns.isEmpty())
                throw new ServiceException("已发布字段映射缺失");
            if (datasourceId == null || datasourceId <= 0) throw new ServiceException("模型数据源ID无效");
            if (name == null || !TABLE.matcher(name).matches()) throw new ServiceException("模型物理表名无效");
            if (failures.containsKey(datasourceId)) throw failures.get(datasourceId);
            Target target = requestTargets.get(datasourceId);
            if (target == null) {
                try {
                    IngestDatasourceRespDTO config = datasourceApi.getDatasource(datasourceId);
                    if (config == null || !Boolean.TRUE.equals(config.getValidFlag()))
                        throw new ServiceException("数据源 " + datasourceId + " 不存在或未启用");
                    target = createTarget(config, sessionFactory);
                    requestTargets.put(datasourceId, target);
                } catch (ServiceException ex) {
                    // 同一请求内不重复等待同一个不可达库的建连超时。
                    failures.put(datasourceId, ex);
                    throw ex;
                }
            }
            // 表或字段错误不将整个数据源标记不可用。
            return new TableAccess(target, name, publishedColumns);
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            for (Map.Entry<Long, Target> entry : requestTargets.entrySet()) {
                try {
                    entry.getValue().query.close();
                } catch (RuntimeException ex) {
                    if (logCloseFailures) log.warn("释放查询请求目标连接失败，datasourceId={}", entry.getKey(), ex);
                }
            }
            requestTargets.clear();
            failures.clear();
        }
    }

    private static class Target {
        final DbQuery query;
        final DbDialect dialect;
        final DbQueryProperty property;

        Target(DbQuery query, DbDialect dialect, DbQueryProperty property) {
            this.query = query;
            this.dialect = dialect;
            this.property = property;
        }
    }

    public static final class TableAccess implements Table {
        private final Target target;
        private final String table;
        private final Map<String, String> columns = new LinkedHashMap<>();
        private final Map<String, String> physicalTypes = new LinkedHashMap<>();
        private final Map<String, String> mappings;

        private TableAccess(Target target, String qualifiedName) {
            this(target, qualifiedName, readPhysicalColumns(target, qualifiedName));
        }

        /** 详情、子模型等显式物理结构查询使用；不作为统计字段缺失时的回退。 */
        private static PhysicalColumns readPhysicalColumns(Target target, String qualifiedName) {
            String[] parts = qualifiedName.split("\\.");
            PhysicalColumns result = new PhysicalColumns();
            for (DbColumn column : target.query.getTableColumns(target.property, parts[parts.length - 1])) {
                String name = column.getColName().toUpperCase(Locale.ROOT);
                result.names.put(name, column.getColName());
                result.types.put(name, column.getDataType());
            }
            if (result.names.isEmpty()) throw new ServiceException("模型物理表不存在或没有可查询字段");
            return result;
        }

        private static final class PhysicalColumns {
            final Map<String, String> names = new LinkedHashMap<>();
            final Map<String, String> types = new LinkedHashMap<>();
        }

        private TableAccess(Target target, String qualifiedName, PhysicalColumns physical) {
            this(target, qualifiedName, physical.names);
            physicalTypes.putAll(physical.types);
        }

        private TableAccess(Target target, String qualifiedName, Map<String, String> publishedColumns) {
            this.target = target;
            String[] parts = qualifiedName.split("\\.");
            // 只保留发布表名；当前Schema、标识符引用和大小写统一由common方言处理。
            table = parts[parts.length - 1];
            Map<String, String> resolved = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : publishedColumns.entrySet()) {
                String alias = entry.getKey();
                String name = entry.getValue();
                if (StringUtils.isBlank(alias) || StringUtils.isBlank(name))
                    throw new ServiceException("已发布字段名不能为空");
                columns.put(name.toUpperCase(Locale.ROOT), name);
                resolved.put(alias.toUpperCase(Locale.ROOT), name);
            }
            this.mappings = java.util.Collections.unmodifiableMap(resolved);
        }

        private TableAccess(TableAccess source, Map<String, String> mappings) {
            this.target = source.target;
            this.table = source.table;
            this.columns.putAll(source.columns);
            this.physicalTypes.putAll(source.physicalTypes);
            Map<String, String> resolved = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                String alias = entry.getKey();
                String actual = entry.getValue();
                if (StringUtils.isBlank(alias) || StringUtils.isBlank(actual)
                        || !columns.containsKey(actual.toUpperCase(Locale.ROOT))
                        || resolved.putIfAbsent(alias.toUpperCase(Locale.ROOT), actual) != null) {
                    throw new ServiceException("模型字段映射与物理表不一致：" + alias);
                }
            }
            this.mappings = java.util.Collections.unmodifiableMap(resolved);
        }

        public TableAccess withColumns(Map<String, String> mappings) {
            if (mappings == null || mappings.isEmpty()) throw new ServiceException("模型字段绑定为空");
            return new TableAccess(this, mappings);
        }

        public Set<String> columns() {
            return java.util.Collections.unmodifiableSet(mappings == null ? columns.keySet() : mappings.keySet());
        }

        public boolean contains(String name) {
            return name != null && (mappings == null ? columns : mappings).containsKey(name.toUpperCase(Locale.ROOT));
        }

        public String column(String name) {
            String key = name == null ? null : name.toUpperCase(Locale.ROOT);
            String physical = mappings == null ? key : mappings.get(key);
            String actual = physical == null ? null : columns.get(physical.toUpperCase(Locale.ROOT));
            if (actual == null) throw new ServiceException("模型字段不在物理表中：" + name);
            return target.dialect.quoteIdentifier(actual);
        }

        private String physicalColumn(String name) {
            String key = name == null ? null : name.toUpperCase(Locale.ROOT);
            String physical = mappings == null ? key : mappings.get(key);
            String actual = physical == null ? null : columns.get(physical.toUpperCase(Locale.ROOT));
            if (actual == null) throw new ServiceException("模型字段不在物理表中：" + name);
            return actual;
        }

        /**
         * 返回经数据库方言处理的目标表名，供维护 SQL 模板使用。
         */
        public String qualifiedTable() {
            return target.dialect.getTableName(target.property, table);
        }

        @Override
        public String quoteIdentifier(String identifier) {
            return target.dialect.quoteIdentifier(identifier);
        }

        @Override
        public String trimExpression(String expression) {
            return target.dialect.trimExpression(expression);
        }

        /** 按字段映射定位真实列类型，Boolean 绑定值转换复用 common 工具方法。 */
        @Override
        public Object jdbcParameter(String field, Object value) {
            if (!(value instanceof Boolean)) return value;
            column(field);
            if (physicalTypes.isEmpty()) {
                physicalTypes.putAll(readPhysicalColumns(target, table).types);
            }
            String key = field.toUpperCase(Locale.ROOT);
            String physical = mappings == null ? key : mappings.get(key);
            String type = physicalTypes.get(physical.toUpperCase(Locale.ROOT));
            return DatabaseUtil.convertBooleanParameter(type, value);
        }

        /** 参数化查询保留原始数值和时间类型，供统计结果处理。 */
        public List<Map<String, Object>> query(String sql, Object[] parameters) {
            return target.query.queryList(sql, parameters, new ColumnMapRowMapper());
        }

        public String select(Collection<String> names) {
            return selectExpression(names.stream().map(name -> column(name) + " AS "
                    + target.dialect.quoteIdentifier(name.toUpperCase(Locale.ROOT))).collect(Collectors.joining(", ")));
        }

        /**
         * 表达式只由仓库生成，字段必须先通过 column() 的物理字段校验。
         */
        public String selectExpression(String expression) {
            return SqlTemplate.selectExpression(this, expression).getSql();
        }

        public List<Map<String, Object>> query(String sql, Map<String, Object> parameters) {
            // 保留Oracle NUMBER等精确数值和JDBC时间类型，不经过将BigDecimal转double的展示映射器。
            return target.query.queryListWithMapper(sql, parameters, new ColumnMapRowMapper());
        }

        public long count(String sql, Map<String, Object> parameters) {
            Object count = query(target.dialect.count(sql), parameters).get(0).values().iterator().next();
            return new BigDecimal(count.toString()).longValueExact();
        }

        public List<Map<String, Object>> page(String sql, Map<String, Object> parameters, long offset, int size) {
            if (offset < 0 || size < 1) throw new ServiceException("分页参数无效");
            return query(target.dialect.buildPaginationSql(sql, offset, size), parameters);
        }

        /**
         * 按稳定主数据 ID 更新业务字段；字段名经过 column() 校验，业务值使用 JDBC 参数绑定。
         * 仅允许追加状态条件，避免执行期间记录已被冻结仍被覆盖。
         */
        public int updateById(Map<String, Object> values, Long recordId, String expectedStatus) {
            if (values == null || values.isEmpty()) throw new ServiceException("没有可更新的业务字段");
            if (recordId == null || recordId <= 0) throw new ServiceException("主数据ID格式无效");
            Map<String, Object> conditions = new LinkedHashMap<>();
            conditions.put("ID", recordId);
            if (expectedStatus != null) conditions.put("STATUS", expectedStatus);
            SqlTemplate template = SqlTemplate.build(this,
                    Operation.update(values, conditions));
            try (Connection connection = target.query.getConnection()) {
                return executeMutation(connection, template);
            } catch (ServiceException ex) {
                throw ex;
            } catch (SQLException ex) {
                throw new ServiceException("主数据正式表更新失败：" + ex.getMessage());
            }
        }

        /**
         * 在目标数据源事务中更新正式数据及子表。
         */
        public int updateByIdWithChildren(Map<String, Object> values, Long recordId, String expectedStatus,
                                          List<TableMutation> children) {
            if (values == null || values.isEmpty()) throw new ServiceException("没有可更新的业务字段");
            Map<String, Object> conditions = new LinkedHashMap<>();
            conditions.put("ID", recordId);
            if (expectedStatus != null) conditions.put("STATUS", expectedStatus);
            if (contains("DEL_FLAG")) conditions.put("DEL_FLAG", false);
            return writeAtomic(Operation.update(values, conditions), recordId, children);
        }

        /**
         * 新增主记录及子表同事务提交；新申请读取自增主键，历史申请兼容显式主键。
         */
        public Long insertWithChildren(Map<String, Object> values, Long legacyRecordId, List<TableMutation> children) {
            if (values == null || values.isEmpty()) throw new ServiceException("没有可新增的业务字段");
            if (legacyRecordId != null && !java.util.Objects.equals(legacyRecordId, values.get("ID")))
                throw new ServiceException("新增主数据ID与历史申请预分配ID不一致");
            SqlTemplate template = SqlTemplate.build(this, Operation.insert(values));
            return inTransaction(connection -> {
                Long recordId = legacyRecordId;
                if (recordId == null) recordId = executeGeneratedInsert(connection, this, template);
                else if (executeMutation(connection, template) != 1) return null;
                for (TableMutation child : children) {
                    if (executeChildMutation(connection, child, recordId) != 1)
                        throw new ServiceException("子模型记录已变化或状态不允许操作，请刷新后重新提交");
                }
                return recordId;
            });
        }

        private Long executeGeneratedInsert(Connection connection, TableAccess access, SqlTemplate template) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    template.getSql(), new String[]{access.physicalColumn("ID")})) {
                List<Object> parameters = template.getParameters();
                for (int index = 0; index < parameters.size(); index++) statement.setObject(index + 1, parameters.get(index));
                if (statement.executeUpdate() != 1) throw new ServiceException("主数据正式表新增失败");
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new ServiceException("数据库未返回新增主数据ID");
                    Object key = keys.getObject(1);
                    if (key == null) throw new ServiceException("数据库返回的新增主数据ID为空");
                    return new BigDecimal(key.toString()).longValueExact();
                }
            }
        }

        private int writeAtomic(Operation operation, Long recordId, List<TableMutation> children) {
            if (recordId == null || recordId <= 0) throw new ServiceException("主数据ID格式无效");
            SqlTemplate template = SqlTemplate.build(this, operation);
            return inTransaction(connection -> {
                int updated = executeMutation(connection, template);
                if (updated != 1) return updated;
                for (TableMutation child : children) {
                    if (executeChildMutation(connection, child, recordId) != 1)
                        throw new ServiceException("子模型记录已变化或状态不允许操作，请刷新后重新提交");
                }
                return 1;
            });
        }

        /**
         * 整个申请的主子新增、更新在同一个目标数据源事务中提交。
         */
        public Map<Long, Long> writeRecordsAtomically(List<RecordMutation> records) {
            if (records == null || records.isEmpty() || records.size() > 500)
                throw new ServiceException("批量执行参数无效");
            List<SqlTemplate> mainTemplates = new java.util.ArrayList<>();
            Set<Long> recordIds = new java.util.HashSet<>();
            Set<Long> itemIds = new java.util.HashSet<>();
            for (RecordMutation record : records) {
                if ((!record.isInsert() && (record.getRecordId() == null || record.getRecordId() <= 0))
                        || (record.getRecordId() != null && !recordIds.add(record.getRecordId()))
                        || record.getRequestItemId() == null || record.getRequestItemId() <= 0 || !itemIds.add(record.getRequestItemId())
                        || record.getResultData() == null
                        || (!record.isInsert() && record.getExpectedStatus() == null))
                    throw new ServiceException("批量记录或申请明细标识无效、重复");
                if (record.isInsert()) {
                    if (record.getRecordId() != null && !java.util.Objects.equals(record.getRecordId(), record.getValues().get("ID")))
                        throw new ServiceException("新增主数据ID与申请预分配ID不一致");
                    mainTemplates.add(SqlTemplate.build(this, Operation.insert(record.getValues())));
                } else {
                    Map<String, Object> conditions = new LinkedHashMap<>();
                    conditions.put("ID", record.getRecordId());
                    conditions.put("STATUS", record.getExpectedStatus());
                    if (contains("DEL_FLAG")) conditions.put("DEL_FLAG", false);
                    mainTemplates.add(SqlTemplate.build(this, Operation.update(record.getValues(), conditions)));
                }
                for (TableMutation child : record.getChildren()) if (child.getTableName() == null
                        || !TABLE.matcher(child.getTableName()).matches()) throw new ServiceException("子模型物理表名无效");
            }
            return inTransaction(connection -> {
                Map<Long, Long> generatedIds = new LinkedHashMap<>();
                for (int index = 0; index < records.size(); index++) {
                    RecordMutation record = records.get(index);
                    try {
                        Long actualRecordId = record.getRecordId();
                        if (record.isInsert() && actualRecordId == null) {
                            actualRecordId = executeGeneratedInsert(connection, this, mainTemplates.get(index));
                        } else if (executeMutation(connection, mainTemplates.get(index)) != 1) {
                            throw new ServiceException("记录已删除或状态已变化");
                        }
                        for (TableMutation child : record.getChildren()) {
                            if (executeChildMutation(connection, child, actualRecordId) != 1)
                                throw new ServiceException("记录已删除、状态或主子归属已变化");
                        }
                        if (record.isInsert()) generatedIds.put(record.getRequestItemId(), actualRecordId);
                    } catch (Exception ex) {
                        throw new ServiceException("记录 " + record.getRecordId() + " 执行失败：" + ex.getMessage());
                    }
                }
                return generatedIds;
            });
        }

        private int executeChildMutation(Connection connection, TableMutation child, Long mainRecordId) throws SQLException {
            if (child.getTableName() == null || !TABLE.matcher(child.getTableName()).matches())
                throw new ServiceException("子模型物理表名无效");
            Operation operation = child.getOperation();
            if (child.isBindMainRecordId()) {
                Map<String, Object> values = new LinkedHashMap<>(operation.getValues());
                values.put("PARENT_ID", mainRecordId);
                operation = operation.isInsert() ? Operation.insert(values) : Operation.update(values, operation.getConditions());
            }
            TableAccess childAccess = new TableAccess(target, child.getTableName());
            SqlTemplate template = SqlTemplate.build(childAccess, operation);
            if (!child.isGeneratedId()) return executeMutation(connection, template);
            Long childId = executeGeneratedInsert(connection, childAccess, template);
            if (child.getResult() != null) child.getResult().put("recordId", String.valueOf(childId));
            return 1;
        }

        /**
         * 达梦、SQL Server 显式写入审批预分配ID时，在同一连接临时开启自增列写入。
         */
        private int executeMutation(Connection connection, SqlTemplate template) throws SQLException {
            String sql = template.getSql();
            boolean explicitIdentity = false;
            String insertTable = null;
            if (target.dialect.requiresIdentityInsert() && sql.startsWith("INSERT INTO ")) {
                // SQL全部来自SqlTemplate；限定表名已按方言引用，兼容主表及子表。
                insertTable = sql.substring("INSERT INTO ".length(), sql.indexOf('(')).trim();
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + insertTable + " WHERE 1=0");
                     ResultSet rows = statement.executeQuery()) {
                    ResultSetMetaData metadata = rows.getMetaData();
                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                        if ("id".equalsIgnoreCase(metadata.getColumnName(i))) {
                            explicitIdentity = metadata.isAutoIncrement(i);
                            break;
                        }
                    }
                }
            }
            if (!explicitIdentity) return target.query.update(connection, sql, template.getParameters().toArray());
            try (Statement statement = connection.createStatement()) {
                statement.execute(target.dialect.identityInsertSql(insertTable, true));
                Throwable failure = null;
                try {
                    return target.query.update(connection, sql, template.getParameters().toArray());
                } catch (RuntimeException | Error ex) {
                    failure = ex;
                    throw ex;
                } finally {
                    try {
                        statement.execute(target.dialect.identityInsertSql(insertTable, false));
                    } catch (SQLException ex) {
                        if (failure != null) failure.addSuppressed(ex);
                        else throw ex;
                    }
                }
            }
        }

        private <T> T inTransaction(TransactionWork<T> work) {
            boolean commitAttempted = false;
            try (Connection connection = target.query.getConnection()) {
                connection.setAutoCommit(false);
                T result;
                try {
                    result = work.execute(connection);
                } catch (Exception failure) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackFailure) {
                        failure.addSuppressed(rollbackFailure);
                        throw new MasterDataWriteOutcomeUnknownException("目标事务回滚结果待确认", failure);
                    }
                    throw new ServiceException("主数据及子表写入失败，整批回滚：" + failure.getMessage());
                }
                commitAttempted = true;
                connection.commit();
                return result;
            } catch (MasterDataWriteOutcomeUnknownException ex) {
                throw ex;
            } catch (Exception ex) {
                if (commitAttempted) throw new MasterDataWriteOutcomeUnknownException("目标事务提交结果待确认", ex);
                if (ex instanceof ServiceException) throw (ServiceException) ex;
                throw new ServiceException("目标数据源写入失败：" + ex.getMessage());
            }
        }
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T execute(Connection connection) throws Exception;
    }
}
