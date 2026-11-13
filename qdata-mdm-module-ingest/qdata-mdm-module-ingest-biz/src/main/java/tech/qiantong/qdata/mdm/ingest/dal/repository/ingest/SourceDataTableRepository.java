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

package tech.qiantong.qdata.mdm.ingest.dal.repository.ingest;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataPageReqVO;
import tech.qiantong.qdata.mdm.ingest.service.datasource.impl.IngestDatasourceServiceImpl;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.GOVERNANCE_PENDING;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.sql.Types;
import java.math.BigDecimal;

/**
 * 模型物化来源表的分页、详情和逻辑删除；表名来自发布快照，SQL列名按目标表实际元数据核验。
 */
@Repository
public class SourceDataTableRepository {
    @Resource
    private IngestDatasourceServiceImpl datasourceService;

    /** 按来源列表相同口径统计已发布来源表的最新未删除记录。 */
    public long countLatestRecords(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table) {
        return execute(target, query -> {
            DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(target.getDatabaseType()));
            String physicalTable = tableName(target, table, dialect);
            Map<String, DbColumn> columns = readColumns(query, physicalTable);
            String sql = "SELECT COUNT(*) FROM " + physicalTable + " current_row WHERE "
                    + column(columns, "del_flag", dialect) + " = ? AND "
                    + SourceDataVersionSql.latestVersionPredicate(physicalTable,
                            column(columns, "id", dialect), column(columns, "source_system_id", dialect),
                            column(columns, "source_record_id", dialect), column(columns, "collection_time", dialect));
            return query.query(sql, new Object[]{flag(columns, false)}, rows -> rows.next() ? rows.getLong(1) : 0L);
        });
    }

    /** 按实体行版本读取融合来源；指定编号时保留历史版本，不折叠为最新行。 */
    public List<Map<String, Object>> getFusionRecords(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, java.util.Collection<Long> ids, boolean lock) {
        if (ids != null && ids.isEmpty()) return new ArrayList<>();
        return fusionExecute(target, table, session -> {
            DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(target.getDatabaseType()));
            String name = tableName(target, table, dialect);
            Map<String, DbColumn> columns = readColumns(session.query, name);
            String sql = "SELECT current_row.* FROM " + name + " current_row WHERE "
                    + column(columns, "del_flag", dialect) + " = ?";
            List<Object> args = new ArrayList<>();
            args.add(flag(columns, false).getValue());
            if (ids != null) {
                sql += " AND " + column(columns, "id", dialect) + " IN ("
                        + String.join(",", java.util.Collections.nCopies(ids.size(), "?")) + ")";
                args.addAll(ids);
            } else {
                sql += " AND " + SourceDataVersionSql.latestVersionPredicate(name,
                        column(columns, "id", dialect), column(columns, "source_system_id", dialect),
                        column(columns, "source_record_id", dialect), column(columns, "collection_time", dialect));
            }
            sql += " ORDER BY " + column(columns, "id", dialect);
            if (lock) sql += " FOR UPDATE";
            try (java.sql.PreparedStatement statement = session.connection.prepareStatement(sql)) {
                statement.setQueryTimeout(120);
                for (int i = 0; i < args.size(); i++) statement.setObject(i + 1, args.get(i));
                List<Map<String, Object>> rows = new ArrayList<>();
                try (java.sql.ResultSet result = statement.executeQuery()) {
                    ColumnMapRowMapper mapper = new ColumnMapRowMapper();
                    while (result.next()) rows.add(mapper.mapRow(result, rows.size()));
                }
                return rows;
            } catch (java.sql.SQLException ex) {
                throw failure("融合来源实体表读取失败", ex);
            }
        });
    }

    /** 状态条件与更新在同一条 SQL 内执行，释放时不覆盖已经完成治理的来源。 */
    public void transitionFusionStatus(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, java.util.Collection<Long> ids,
            String expected, String status) {
        if (ids.isEmpty()) return;
        fusionExecute(target, table, session -> {
            DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(target.getDatabaseType()));
            String name = tableName(target, table, dialect);
            Map<String, DbColumn> columns = readColumns(session.query, name);
            String sql = "UPDATE " + name + " SET " + column(columns, "status", dialect) + " = ? WHERE "
                    + column(columns, "del_flag", dialect) + " = ? AND " + column(columns, "status", dialect)
                    + " = ? AND " + column(columns, "id", dialect) + " IN ("
                    + String.join(",", java.util.Collections.nCopies(ids.size(), "?")) + ")";
            try (java.sql.PreparedStatement statement = session.connection.prepareStatement(sql)) {
                statement.setQueryTimeout(120);
                statement.setString(1, status);
                statement.setObject(2, flag(columns, false).getValue());
                statement.setString(3, expected);
                int index = 4;
                for (Long id : ids) statement.setLong(index++, id);
                int changed = statement.executeUpdate();
                if (GOVERNANCE_PENDING.equals(expected) && changed != ids.size()) {
                    throw new ServiceException("来源已删除或治理状态已变化，请刷新后重试");
                }
                return null;
            } catch (java.sql.SQLException ex) {
                throw failure("融合来源实体表状态更新失败", ex);
            }
        });
    }

    /** 事务内按数据源和表复用连接并注册提交回调；事务外成功后立即提交并释放。 */
    private <T> T fusionExecute(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, Function<SourceDataConnection, T> action) {
        String key = getClass().getName() + ":" + target.getDatasourceId() + ":"
                + target.getSchemaName() + ":" + table.getTableName();
        boolean transactional = org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive();
        SourceDataConnection session = (SourceDataConnection) org.springframework.transaction.support
                .TransactionSynchronizationManager.getResource(key);
        if (session == null) {
            DbQuery query = datasourceService.buildDbQuery(target.getDatasourceId());
            try {
                session = new SourceDataConnection(query, key);
            } catch (Exception ex) {
                query.close();
                throw failure("来源实体表连接失败", ex);
            }
            if (transactional) {
                org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(key, session);
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(session);
            }
        }
        try {
            T result = action.apply(session);
            if (!transactional) session.afterCommit();
            return result;
        } finally {
            if (!transactional) session.close();
        }
    }

    /**
     * 默认查询每个来源实体的最新版本；历史模式查询所有未删除版本。
     */
    public PageResult<Map<String, Object>> getPage(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, SourceDataPageReqVO request) {
        return execute(target, query -> {
            DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(target.getDatabaseType()));
            Map<String, DbColumn> columns = readColumns(query, tableName(target, table, dialect));
            String physicalTable = tableName(target, table, dialect);
            StringBuilder sql = new StringBuilder("SELECT current_row.* FROM ").append(physicalTable)
                    .append(" current_row WHERE ").append(column(columns, "del_flag", dialect)).append(" = ?");
            List<Object> args = new ArrayList<>();
            args.add(flag(columns, false));
            if (!Boolean.TRUE.equals(request.getIncludeHistory())) {
                sql.append(" AND ").append(SourceDataVersionSql.latestVersionPredicate(physicalTable,
                        column(columns, "id", dialect), column(columns, "source_system_id", dialect),
                        column(columns, "source_record_id", dialect), column(columns, "collection_time", dialect)));
            }
            if (request.getSourceSystemId() != null) {
                sql.append(" AND ").append(column(columns, "source_system_id", dialect)).append(" = ?");
                args.add(request.getSourceSystemId());
            }
            String recordKey = hasText(request.getSourceRecordKey()) ? request.getSourceRecordKey() : request.getKeyword();
            if (hasText(recordKey)) {
                sql.append(" AND ").append(column(columns, "source_record_id", dialect)).append(" LIKE ? ESCAPE '!'");
                args.add(dialect.containsPattern(recordKey.trim()));
            }
            if (hasText(request.getGovernanceStatus())) {
                sql.append(" AND ").append(column(columns, "status", dialect)).append(" = ?");
                args.add(request.getGovernanceStatus());
            }
            if (hasText(request.getBusinessFieldValue())) {
                List<String> predicates = new ArrayList<>();

                for (ModelMaterializedColumnRespDTO field : table.getColumns()) {
                    if (!Boolean.TRUE.equals(field.getSystemFlag()) && Boolean.TRUE.equals(field.getEnabledFlag())) {
                        String name = column(columns, field.getColumnName(), dialect);
                        predicates.add(dialect.textSearchExpression(name, physicalColumn(columns, field.getColumnName()).getJdbcType())
                                + " LIKE ? ESCAPE '!'");
                        args.add(dialect.containsPattern(request.getBusinessFieldValue().trim()));
                    }
                }
                sql.append(" AND (").append(predicates.isEmpty() ? "1 = 0" : String.join(" OR ", predicates)).append(")");
            }
             // 先统计后排序分页，避免不支持子查询 ORDER BY 的数据库报错。
            Long total = query.query(dialect.count(sql.toString()), args.toArray(), rows -> rows.next() ? rows.getLong(1) : 0L);
            String order = "sourceRecordKey".equals(request.getOrderByColumn()) ? "source_record_id"
                    : "id".equals(request.getOrderByColumn()) ? "id" : "collection_time";
            boolean ascending = "ascending".equalsIgnoreCase(request.getIsAsc()) || "asc".equalsIgnoreCase(request.getIsAsc());
            sql.append(" ORDER BY ").append(column(columns, order, dialect)).append(ascending ? " ASC" : " DESC");
             // 主排序值相同时以物理主键确定顺序，保证静态数据的分页顺序稳定。
            if (!"id".equals(order)) sql.append(", ").append(column(columns, "id", dialect)).append(" DESC");
            int pageNum = request.getPageNum() == null ? 1 : Math.max(1, request.getPageNum());
            int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
            if (pageSize <= 0) throw new ServiceException("每页记录数必须大于0");
            String pageSql = dialect.buildPaginationSql(sql.toString(), (long) (pageNum - 1) * pageSize, pageSize);
             // 使用 JDBC 原始类型，避免通用展示映射将 DECIMAL 主键转为 double 导致精度丢失。
            return new PageResult<>(query.queryList(pageSql, args.toArray(), new ColumnMapRowMapper()), total);
        });
    }

    /**
     * 按当前模型来源表的物理主键查询未删除记录，不访问旧的模拟来源表。
     */
    public Map<String, Object> getRecord(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, Long id) {
        return execute(target, query -> {
            DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(target.getDatabaseType()));
            Map<String, DbColumn> columns = readColumns(query, tableName(target, table, dialect));
            List<Map<String, Object>> rows = query.queryList("SELECT * FROM " + tableName(target, table, dialect)
                    + " WHERE " + column(columns, "id", dialect) + " = ? AND "
                    + column(columns, "del_flag", dialect) + " = ?", new Object[]{id, flag(columns, false)}, new ColumnMapRowMapper());
            if (rows.isEmpty()) throw new ServiceException("来源数据不存在或已删除");
            return rows.get(0);
        });
    }

    /**
     * 同一条 UPDATE 校验未删除且待融合后设置删除标识，避免先查状态再删除的竞态。
     */
    public void deleteRecord(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, Long id) {
        execute(target, query -> {
            DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(target.getDatabaseType()));
            Map<String, DbColumn> columns = readColumns(query, tableName(target, table, dialect));
            String sql = "UPDATE " + tableName(target, table, dialect) + " SET " + column(columns, "del_flag", dialect)
                    + " = ? WHERE " + column(columns, "id", dialect) + " = ? AND " + column(columns, "del_flag", dialect) + " = ?"
                    + " AND " + column(columns, "status", dialect) + " = ?";
            if (query.update(sql, new Object[]{flag(columns, true), id, flag(columns, false), GOVERNANCE_PENDING}) != 1) {
                throw new ServiceException("来源数据不存在、已删除或非待融合状态");
            }
            return null;
        });
    }

    /**
     * 每次请求创建并检测目标查询器，结束后释放资源；连接失败与查询失败分别返回业务提示。
     */
    private <T> T execute(ModelPublishedMaterializationRespDTO target, Function<DbQuery, T> action) {
        DbQuery query = null;
        try {
            query = datasourceService.buildDbQuery(target.getDatasourceId());
            if (!query.valid()) throw new ServiceException("连接失败");
        } catch (Exception ex) {
            if (query != null) query.close();
            throw failure("连接失败", ex);
        }
        try {
            return action.apply(query);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("来源表查询失败，请检查模型生成的表及字段", ex);
        } finally {
            query.close();
        }
    }

    /**
     * 只使用发布信息中的 schema 和表名，分段校验及引用，避免误用连接默认模式。
     */
    private String tableName(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, DbDialect dialect) {
        String name = identifier(table.getTableName(), dialect);
        return hasText(target.getSchemaName()) ? identifier(target.getSchemaName(), dialect) + "." + name : name;
    }

    /**
     * 零行查询读取实际列名与 JDBC 类型，采集字段不依赖发布快照是否包含它们。
     */
    private Map<String, DbColumn> readColumns(DbQuery query, String table) {
        try {
            Map<String, DbColumn> columns = new LinkedHashMap<>();
            for (DbColumn column : query.getColumnsByQuerySql("SELECT * FROM " + table + " WHERE 1=0")) {
                columns.put(column.getColName().toLowerCase(Locale.ROOT), column);
            }
            return columns;
        } catch (Exception ex) {
            throw failure("来源表字段读取失败，请检查模型生成的表及权限", ex);
        }
    }

    /**
     * 忽略大小写定位实际列；缺失时明确报错，不用快照字段替代。
     */
    private DbColumn physicalColumn(Map<String, DbColumn> columns, String name) {
        DbColumn column = columns.get(name.toLowerCase(Locale.ROOT));
        if (column == null) throw new ServiceException("实际来源表缺少字段：" + name);
        return column;
    }

    /**
     * 保留驱动返回的真实列名大小写，并交给公共方言引用。
     */
    private String column(Map<String, DbColumn> columns, String name, DbDialect dialect) {
        return identifier(physicalColumn(columns, name).getColName(), dialect);
    }

    /**
     * 只接受单段标识符；业务筛选值通过参数绑定，不经此方法拼接。
     */
    private String identifier(String name, DbDialect dialect) {
        if (name == null || !name.matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) {
            throw new ServiceException("模型来源表或字段名称无效");
        }
        return dialect.quoteIdentifier(name);
    }

    /**
     * 按 del_flag 的实际 JDBC 类型绑定正常或删除值，避免字符串、数值和布尔值之间隐式转换。
     */
    private SqlParameterValue flag(Map<String, DbColumn> columns, boolean deleted) {
        int type = physicalColumn(columns, "del_flag").getJdbcType();
        Object value;
        switch (type) {
            case Types.BOOLEAN:
            case Types.BIT:
                value = deleted;
                break;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                value = deleted ? 1 : 0;
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                value = deleted ? BigDecimal.ONE : BigDecimal.ZERO;
                break;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
                value = deleted ? "1" : "0";
                break;
            default:
                throw new ServiceException("来源表删除标识字段类型不支持：" + type);
        }
        return new SqlParameterValue(type, value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 保留原始异常供服务端排查，对页面返回不含连接参数和业务值的提示。
     */
    private ServiceException failure(String message, Exception cause) {
        ServiceException failure = new ServiceException(message);
        failure.initCause(cause);
        return failure;
    }
}
