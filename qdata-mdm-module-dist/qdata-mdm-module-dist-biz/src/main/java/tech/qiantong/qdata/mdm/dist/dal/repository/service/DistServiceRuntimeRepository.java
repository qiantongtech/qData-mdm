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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.repository.service;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Repository;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.DataSourceFactory;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceFieldDO;

import tech.qiantong.qdata.mdm.ingest.api.datasource.IngestDatasourceApi;
import tech.qiantong.qdata.mdm.ingest.api.datasource.dto.IngestDatasourceRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 执行已发布物化表查询，并将数据库列映射为服务响应字段。
 */
@Repository
public class DistServiceRuntimeRepository {


 @Resource
 private IngestDatasourceApi datasourceApi;
    @Resource
    private DataSourceFactory dataSourceFactory;

    /**
     * 校验数据源和物理列后执行查询；查询资源在方法结束时统一关闭。
     */
    public PageResult<Map<String, Object>> query(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, List<DistServiceFieldDO> outputs,
            Map<Long, String> physicalColumns, String whereClause, List<Object> values,
            int pageNum, int pageSize, boolean single) {

        DbQuery query = null;
        try {
            IngestDatasourceRespDTO datasource = datasourceApi.getDatasource(target.getDatasourceId());
            if (datasource == null || !Boolean.TRUE.equals(datasource.getValidFlag())
                    || datasource.getDbQueryProperty() == null) {
                throw new ServiceException("模型数据源不可用");
            }
            query = dataSourceFactory.createDbQuery(datasource.getDbQueryProperty());
            if (query == null || !query.valid()) {
                throw new ServiceException("模型数据源连接失败");
            }

            DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(target.getDatabaseType()));
            Map<String, String> actualColumns = readColumns(query, target, table, dialect);
            List<String> selectColumns = new ArrayList<>();
            List<String> actualOutputNames = new ArrayList<>();
            for (DistServiceFieldDO field : outputs) {
                String name = physicalColumns.get(field.getModelColumnId());
                String actual = actualColumns.get(name.toLowerCase(Locale.ROOT));
                if (actual == null) {
                    throw new ServiceException("发布物化表缺少已配置输出字段");
                }
                actualOutputNames.add(actual);
                selectColumns.add(dialect.quoteIdentifier(actual));
            }
            // 校验并引用的物理列名。
            String baseSql = "SELECT " + String.join(", ", selectColumns) + " FROM "
                    + tableName(target, table, dialect) + whereClause;
            String idColumn = actualColumns.get("id");
            if (idColumn != null) {
                baseSql += " ORDER BY " + dialect.quoteIdentifier(idColumn) + " ASC";
            }
            Long total = single ? null : query.query(dialect.count("SELECT 1 FROM "
                    + tableName(target, table, dialect) + whereClause), values.toArray(),
                    rows -> rows.next() ? rows.getLong(1) : 0L);
            String pageSql = dialect.buildPaginationSql(baseSql, single ? 0L : (long) (pageNum - 1) * pageSize,
                    single ? 1 : pageSize);
            List<Map<String, Object>> rawRows = query.queryList(pageSql, values.toArray(), new ColumnMapRowMapper());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> raw : rawRows) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 0; index < outputs.size(); index++) {
                    String actual = actualOutputNames.get(index);
                    Object value = null;
                    for (Map.Entry<String, Object> entry : raw.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(actual)) {
                            value = entry.getValue();
                            break;
                        }
                    }
                    row.put(outputs.get(index).getFieldAlias(), value);
                }
                rows.add(row);
            }
            return new PageResult<>(rows, single ? (long) rows.size() : total);
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServiceException("数据服务查询失败，请检查已发布物化表及数据源配置");
        } finally {
            if (query != null) {
                query.close();
            }
        }
    }

    /**
     * 通过零行查询读取数据库实际列名，避免依赖配置中的大小写形式。
     */
    private Map<String, String> readColumns(DbQuery query, ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, DbDialect dialect) {
        try {
            Map<String, String> columns = new LinkedHashMap<>();
            for (tech.qiantong.qdata.mdm.common.database.core.DbColumn column :
                    query.getColumnsByQuerySql("SELECT * FROM " + tableName(target, table, dialect) + " WHERE 1=0")) {
                columns.put(column.getColName().toLowerCase(Locale.ROOT), column.getColName());
            }
            return columns;
        } catch (Exception exception) {
            throw new ServiceException("发布物化表字段读取失败");
        }
    }

    /**
     * 按当前数据库方言引用模式名和物化表名。
     */
    private String tableName(ModelPublishedMaterializationRespDTO target,
            ModelPublishedMaterializedTableRespDTO table, DbDialect dialect) {
        String schema = target.getSchemaName();
        String name = identifier(table.getTableName(), dialect);
        return schema == null || schema.trim().isEmpty() ? name : identifier(schema, dialect) + "." + name;
    }

    /**
     * 标识符只允许常规列名字符，校验后再交给方言引用。
     */
    private String identifier(String name, DbDialect dialect) {
        if (name == null || !name.matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) {
            throw new ServiceException("模型物化表或字段名称无效");
        }
        return dialect.quoteIdentifier(name);
    }
}
