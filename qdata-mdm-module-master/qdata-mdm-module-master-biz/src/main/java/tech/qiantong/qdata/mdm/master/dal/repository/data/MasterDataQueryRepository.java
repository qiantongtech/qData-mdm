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

import org.springframework.stereotype.Repository;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.DateUtils;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataPageReqVO;
import tech.qiantong.qdata.mdm.master.dal.dto.data.MasterDataQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.data.MasterDataStatisticsDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.data.MasterDataTreeNodeDTO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.TableAccess;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.RequestContext;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.TableMutation;
import tech.qiantong.qdata.mdm.common.database.sql.SqlTemplate;
import tech.qiantong.qdata.mdm.common.database.sql.SqlTemplate.Aggregate;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.RecordMutation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 动态主表查询；目标连接、SELECT、计数及分页复用 common。 */
@Repository
public class MasterDataQueryRepository {
    /** Repository 只定义业务统计项，SQL 语法及参数生成交给 common。 */
    private static final List<Aggregate> STATISTICS_AGGREGATES = Collections.unmodifiableList(Arrays.asList(
            Aggregate.count("DATA_COUNT"),
            Aggregate.countWhen("STATUS", "1", "ACTIVE_COUNT"),
            Aggregate.countWhen("STATUS", "2", "FROZEN_COUNT"),
            Aggregate.countWhen("STATUS", "3", "INVALID_COUNT"),
            Aggregate.countWhen("STATUS", "4", "ARCHIVED_COUNT")));

    private final MasterDataConnectionProvider connections;

    public MasterDataQueryRepository(MasterDataConnectionProvider connections) { this.connections = connections; }

    public Set<String> findPhysicalColumns(Long datasourceId, String table) {
        return connections.forTable(datasourceId, table).columns();
    }

    public long count(TableAccess access, MasterDataQueryDTO condition) {
        SqlParts parts = where(access, condition);
        return access.count(access.select(Collections.singletonList("ID")) + parts.sql, parts.parameters);
    }

    public List<Map<String, Object>> selectPage(TableAccess access, MasterDataQueryDTO condition,
            Collection<String> selectColumns, MasterDataPageReqVO request,
            long offset, int pageSize) {
        SqlParts parts = where(access, condition);
        String ordering = orderBy(access, selectColumns, request, false);
        return access.page(access.select(selectColumns) + parts.sql + " ORDER BY " + ordering,
                parts.parameters, offset, pageSize);
    }

    /**
     * 一次读取时间节点之后的全部匹配记录；时间、可选状态使用绑定参数，列名来自成功发布映射。
     * 更新时间为空时回退创建时间，避免只有创建时间的新记录漏同步；按时间及ID稳定升序返回。
     */
    public List<Map<String, Object>> selectSyncList(Long datasourceId, String table, Map<String, String> mappings,
            Collection<String> selectColumns, Date updatedAfter, String status) {
        TableAccess access = connections.forTable(datasourceId, table).withColumns(mappings);
        String updated = access.contains("UPDATED_TIME") ? "UPDATED_TIME"
                : access.contains("UPDATE_TIME") ? "UPDATE_TIME" : null;
        if (updated == null) throw new ServiceException("主数据缺少更新时间字段，无法增量同步");
        String created = access.contains("CREATED_TIME") ? "CREATED_TIME"
                : access.contains("CREATE_TIME") ? "CREATE_TIME" : null;
        String time = created == null ? access.column(updated)
                : "COALESCE(" + access.column(updated) + ", " + access.column(created) + ")";
        SqlParts parts = normal(access);
        if (status != null && !status.isEmpty()) parts.eq(access.column("STATUS"), "status", status);
        parts.sql.append(" AND ").append(time).append(" > :updatedAfter");
        parts.parameters.put("updatedAfter", new java.sql.Timestamp(updatedAfter.getTime()));
        return access.query(access.select(selectColumns) + parts.sql + " ORDER BY " + time + " ASC, "
                + access.column("ID") + " ASC", parts.parameters);
    }

    /** 分类树表读取全部匹配记录，避免父子节点被行分页拆开。 */
    public List<Map<String, Object>> selectCategoryList(TableAccess access, MasterDataQueryDTO condition,
            Collection<String> selectColumns, MasterDataPageReqVO request) {
        SqlParts parts = where(access, condition);
        String ordering = orderBy(access, selectColumns, request, true);
        return access.query(access.select(selectColumns) + parts.sql + " ORDER BY " + ordering, parts.parameters);
    }

    private String orderBy(TableAccess access, Collection<String> selectColumns,
            MasterDataPageReqVO request, boolean category) {
        String order = request == null || request.getOrderByColumn() == null ? "" : request.getOrderByColumn().trim();
        String direction = request == null || request.getIsAsc() == null ? "" : request.getIsAsc().trim();
        if (!direction.isEmpty() && !"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new ServiceException("排序方向无效");
        }
        if (order.isEmpty()) {
            order = category && access.contains("SORT_ORDER")
                    && selectColumns.stream().anyMatch("SORT_ORDER"::equalsIgnoreCase) ? "SORT_ORDER" : "ID";
            direction = category ? "asc" : "desc";
        }
        final String requested = order;
        if (!selectColumns.stream().anyMatch(requested::equalsIgnoreCase)
                || !access.contains(order)) throw new ServiceException("排序字段无效");
        String ordering = access.column(order) + ("asc".equalsIgnoreCase(direction) ? " ASC" : " DESC");
        if (!"ID".equalsIgnoreCase(order)) ordering += ", " + access.column("ID") + (category ? " ASC" : " DESC");
        return ordering;
    }

    public List<Map<String, Object>> selectDetail(Long datasourceId, String table, Collection<String> columns, Long recordId,
            Map<String, String> mappings) {
        TableAccess access = connections.forTable(datasourceId, table).withColumns(mappings);
        return selectDetail(access, columns, recordId);
    }

    public List<Map<String, Object>> selectDetail(TableAccess access, Collection<String> columns, Long recordId) {
        SqlParts parts = normal(access);
        parts.eq(access.column("ID"), "recordId", recordId);
        return access.query(access.select(columns) + parts.sql, parts.parameters);
    }

    /** 请求内复用目标连接，结束时统一关闭。 */
    public RequestContext openRequest() {
        return connections.openRequest();
    }

    /** 统计失败静默返回0的请求上下文。 */
    public RequestContext openQuietRequest() {
        return connections.openQuietRequest();
    }

    public MasterDataStatisticsDTO selectStatistics(RequestContext context, Long datasourceId, String table,
            Map<String, String> publishedColumns) {
        if (publishedColumns == null) return null;
        for (String key : new String[]{"STATUS", "UPDATED_TIME", "DEL_FLAG"}) {
            String column = publishedColumns.get(key);
            if (column == null || column.trim().isEmpty()) return null;
        }
        TableAccess access = context.forTable(datasourceId, table, publishedColumns);
        return selectStatistics(access);
    }

    /** 卡片和列表共用聚合模板；总量、状态数量和最近更新时间同次查询。 */
    public MasterDataStatisticsDTO selectStatistics(TableAccess access) {
        List<Aggregate> aggregates = new ArrayList<>(STATISTICS_AGGREGATES);
        if (access.contains("UPDATED_TIME")) aggregates.add(Aggregate.max("UPDATED_TIME", "LATEST_TIME"));
        SqlTemplate template = SqlTemplate.aggregate(access, aggregates, access.contains("DEL_FLAG")
                ? Collections.singletonMap("DEL_FLAG", "0") : Collections.emptyMap());
        Map<String, Object> row = access.query(template.getSql(), template.getParameters().toArray()).get(0);
        MasterDataStatisticsDTO result = new MasterDataStatisticsDTO();
        result.setDataCount(statisticCount(row.get("DATA_COUNT")));
        result.setActiveCount(statisticCount(row.get("ACTIVE_COUNT")));
        result.setFrozenCount(statisticCount(row.get("FROZEN_COUNT")));
        result.setInvalidCount(statisticCount(row.get("INVALID_COUNT")));
        result.setArchivedCount(statisticCount(row.get("ARCHIVED_COUNT")));
        Object time = row.get("LATEST_TIME");
        result.setLatestDataUpdateTime(time instanceof Date ? (Date) time : DateUtils.parseDate(time));
        return result;
    }

    private long statisticCount(Object value) {
        return value == null ? 0L : new BigDecimal(value.toString()).longValueExact();
    }

    /**
     * 同一字段的数据项在一条SQL中统计，COUNT(CASE WHEN)保留数据库自身的等值比较语义。
     * 使用调用方编码作为结果键，避免整数/小数列或大小写不敏感排序规则导致分组结果回填错误。
     * 聚合及标识符引用复用common，值全部参数绑定；IN限制匹配范围，固定排除逻辑删除。
     */
    public Map<String, Long> countFieldValues(TableAccess access, String column, Map<String, Object> values) {
        if (values == null || values.isEmpty() || values.size() > 500)
            throw new ServiceException("字段统计的数据项数量必须在1到500之间");
        List<Aggregate> aggregates = new ArrayList<>();
        int index = 0;
        for (Object value : values.values()) {
            if (value == null) throw new ServiceException("字段统计值不能为空");
            aggregates.add(Aggregate.countWhen(column, value, "USAGE_" + index++));
        }
        // 缺少删除列必须报错，不能回退为包含已删除记录的统计。
        SqlTemplate template = SqlTemplate.aggregate(access, aggregates, Collections.singletonMap("DEL_FLAG", "0"));
        List<Object> parameters = new ArrayList<>(template.getParameters());
        for (Object value : values.values()) parameters.add(access.jdbcParameter(column, value));
        String sql = template.getSql() + " AND " + access.column(column) + " IN ("
                + String.join(", ", Collections.nCopies(values.size(), "?")) + ")";
        List<Map<String, Object>> rows = access.query(sql, parameters.toArray());
        if (rows.size() != 1) throw new ServiceException("字段使用统计未返回有效聚合结果");
        Map<String, Long> result = new LinkedHashMap<>();
        index = 0;
        for (String code : values.keySet()) {
            Object count = rows.get(0).get("USAGE_" + index++);
            if (count == null) throw new ServiceException("字段使用统计结果缺失");
            result.put(code, statisticCount(count));
        }
        return result;
    }

    /** 编码引用字段的已填写条数，空值、空串和仅含空格的值均不计入。 */
    public long countNonBlankFieldValues(TableAccess access, String column) {
        SqlTemplate template = SqlTemplate.aggregate(access, Collections.singletonList(Aggregate.countNonBlank(column, "USAGE_COUNT")),
                Collections.singletonMap("DEL_FLAG", "0"));
        List<Map<String, Object>> rows = access.query(template.getSql(), template.getParameters().toArray());
        if (rows.size() != 1 || rows.get(0).get("USAGE_COUNT") == null)
            throw new ServiceException("编码使用统计未返回有效聚合结果");
        return statisticCount(rows.get(0).get("USAGE_COUNT"));
    }

    public void requireRecord(Long datasourceId, String table, Long recordId, Map<String, String> mappings) {
        requireRecord(connections.forTable(datasourceId, table).withColumns(mappings), recordId);
    }

    public void requireRecord(TableAccess access, Long recordId) {
        long count = selectDetail(access, Collections.singletonList("ID"), recordId).size();
        if (count == 0) throw new ServiceException("主数据记录不存在");
        if (count > 1) throw new ServiceException("主数据记录ID存在重复数据");
    }

    /**
     * 更新权威主表单条记录；调用方必须先完成模型、字段及业务规则校验。
     */
    public int updateRecord(Long datasourceId, String table, Long recordId,
            Map<String, Object> values, Map<String, String> mappings, String expectedStatus) {
        if (values == null || values.isEmpty()) throw new ServiceException("没有可更新的业务字段");
        return connections.forTable(datasourceId, table).withColumns(mappings)
                .updateById(values, recordId, expectedStatus);
    }

    /** 正式数据与子表在同一目标库事务提交。 */
    public int updateRecordWithChildren(Long datasourceId, String table, Long recordId,
            Map<String, Object> values, Map<String, String> mappings, String expectedStatus,
            List<TableMutation> children) {
        return connections.forTable(datasourceId, table).withColumns(mappings)
                .updateByIdWithChildren(values, recordId, expectedStatus, children);
    }

    /** 正式数据新增与子表在同一目标库事务提交。 */
    public Long insertRecordWithChildren(Long datasourceId, String table, Long legacyRecordId,
            Map<String, Object> values, Map<String, String> mappings, List<TableMutation> children) {
        return connections.forTable(datasourceId, table).withColumns(mappings)
                .insertWithChildren(values, legacyRecordId, children);
    }

    public Map<Long, Long> writeRecordsAtomically(Long datasourceId, String table, Map<String, String> mappings,
            List<RecordMutation> records) {
        return connections.forTable(datasourceId, table).withColumns(mappings).writeRecordsAtomically(records);
    }

    public long countByColumn(Long datasourceId, String table, String column, Object value, Long excludeRecordId,
            Map<String, String> mappings) {
        TableAccess access = connections.forTable(datasourceId, table).withColumns(mappings);
        SqlParts parts = new SqlParts();
        parts.eq(access.column(column), "value", access.jdbcParameter(column, value));
        if (excludeRecordId != null) {
            parts.sql.append(" AND ").append(access.column("ID")).append(" <> :excludedId");
            parts.parameters.put("excludedId", excludeRecordId);
        }
        return access.count(access.select(Collections.singletonList("ID")) + parts.sql, parts.parameters);
    }

    /** 关联字段只能引用未删除且生效的正式记录。 */
    public long countActiveByColumn(Long datasourceId, String table, String column, Object value,
            Map<String, String> mappings) {
        TableAccess access = connections.forTable(datasourceId, table).withColumns(mappings);
        SqlParts parts = normal(access);
        parts.eq(access.column("STATUS"), "status", "1");
        parts.eq(access.column(column), "value", access.jdbcParameter(column, value));
        return access.count(access.select(Collections.singletonList("ID")) + parts.sql, parts.parameters);
    }

    public List<Long> selectIdsByColumn(Long datasourceId, String table, String column, Object value, Map<String, String> mappings) {
        TableAccess access = connections.forTable(datasourceId, table).withColumns(mappings);
        SqlParts parts = normal(access);
        parts.eq(access.column(column), "value", access.jdbcParameter(column, value));
        return access.page(access.select(Collections.singletonList("ID")) + parts.sql
                + " ORDER BY " + access.column("ID"), parts.parameters, 0, 2).stream()
                .map(row -> new BigDecimal(row.get("ID").toString()).longValueExact()).collect(Collectors.toList());
    }

    public List<MasterDataTreeNodeDTO> selectTreeNodes(Long datasourceId, String table, String nameColumn, Map<String, String> mappings) {
        TableAccess access = connections.forTable(datasourceId, table).withColumns(mappings);
        return selectTreeNodes(access, nameColumn);
    }

    public List<MasterDataTreeNodeDTO> selectTreeNodes(TableAccess access, String nameColumn) {
        SqlParts parts = normal(access);
        String order = access.contains("SORT_ORDER") ? access.column("SORT_ORDER") + ", " : "";
        String sql = access.selectExpression(access.column("ID") + " AS ID, " + access.column("CATEGORY_ID") + " AS CATEGORY_ID"
                + ", " + access.column(nameColumn) + " AS NODE_LABEL") + parts.sql
                + " ORDER BY " + order + access.column("ID");
        return access.query(sql, parts.parameters).stream().map(row -> {
            MasterDataTreeNodeDTO node = new MasterDataTreeNodeDTO();
            node.setMasterDataId(row.get("ID") == null ? null : row.get("ID").toString());
            node.setCategoryId(row.get("CATEGORY_ID") == null ? null : row.get("CATEGORY_ID").toString());
            node.setLabel(row.get("NODE_LABEL") == null ? null : row.get("NODE_LABEL").toString());
            if (node.getMasterDataId() == null || node.getMasterDataId().isEmpty()) throw new ServiceException("分类节点缺少主数据ID");
            if (node.getLabel() == null || node.getLabel().isEmpty()) throw new ServiceException("分类节点缺少名称");
            return node;
        }).collect(Collectors.toList());
    }

    /** 有界读取完整分类数据；多读一条用于显式发现超限，禁止静默截断。 */
    public List<Map<String, Object>> selectClassificationNodes(Long datasourceId, String table,
            Map<String, String> mappings, Collection<String> columns, int limit) {
        try (RequestContext context = connections.openRequest()) {
            TableAccess access = context.forTable(datasourceId, table, mappings);
            SqlParts parts = normal(access);
            String order = access.contains("SORT_ORDER") ? access.column("SORT_ORDER") + ", " : "";
            return access.page(access.select(columns) + parts.sql + " ORDER BY " + order + access.column("ID"),
                    parts.parameters, 0, limit + 1);
        }
    }

    private SqlParts where(TableAccess access, MasterDataQueryDTO condition) {
        if (condition == null) throw new ServiceException("主数据查询条件不能为空");
        SqlParts parts = normal(access);
        if (condition.getStatus() != null && !condition.getStatus().trim().isEmpty()) {
            parts.eq(access.column("STATUS"), "status", condition.getStatus().trim());
        }
        if (condition.getName() != null && !condition.getName().trim().isEmpty()) {
            parts.sql.append(" AND LOWER(").append(access.column(condition.getNameColumn())).append(") LIKE :name ESCAPE '!'");
            String text = condition.getName().trim().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            parts.parameters.put("name", "%" + text + "%");
        }
        parts.in(access.column("ID"), "recordIds", condition.getRecordIds());
        parts.in(access.column("ID"), "categoryIds", condition.getCategoryIds());
        if (condition.getClassificationColumn() != null) {
            if (condition.getClassificationValues() == null) throw new ServiceException("分类筛选值缺失");
            if (condition.getClassificationValues().size() > 500) throw new ServiceException("分类筛选范围超过500个节点，请选择更小范围");
            parts.in(access.column(condition.getClassificationColumn()), "classificationValues", condition.getClassificationValues());
        }
        return parts;
    }

    private SqlParts normal(TableAccess access) {
        SqlParts parts = new SqlParts();
        if (access.contains("DEL_FLAG")) parts.eq(access.column("DEL_FLAG"), "deleted", "0");
        return parts;
    }

    private static class SqlParts {
        final StringBuilder sql = new StringBuilder(" WHERE 1=1");
        final Map<String, Object> parameters = new LinkedHashMap<>();
        void eq(String column, String parameter, Object value) {
            sql.append(" AND ").append(column).append(value == null ? " IS NULL" : " = :" + parameter);
            if (value != null) parameters.put(parameter, value);
        }
        void in(String column, String parameter, Collection<?> values) {
            if (values == null) return;
            if (values.isEmpty()) { sql.append(" AND 1=0"); return; }
            List<?> ids = new ArrayList<>(values);
            sql.append(" AND (");
            for (int offset = 0; offset < ids.size(); offset += 500) {
                if (offset > 0) sql.append(" OR ");
                String key = parameter + offset;
                sql.append(column).append(" IN (:").append(key).append(")");
                parameters.put(key, ids.subList(offset, Math.min(offset + 500, ids.size())));
            }
            sql.append(")");
        }
    }
}
