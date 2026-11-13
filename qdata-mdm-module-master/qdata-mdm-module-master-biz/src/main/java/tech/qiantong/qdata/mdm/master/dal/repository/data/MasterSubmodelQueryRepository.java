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
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSubmodelPageReqVO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.TableAccess;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 主子表使用相同目标连接及 common 查询能力。 */
@Repository
public class MasterSubmodelQueryRepository {
    private final MasterDataConnectionProvider connections;
    public MasterSubmodelQueryRepository(MasterDataConnectionProvider connections) { this.connections = connections; }
    public Set<String> findPhysicalColumns(Long datasourceId, String table) { return connections.forTable(datasourceId, table).columns(); }

    public List<Map<String, Object>> selectRecord(Long datasourceId, String table, Long parentId, Long recordId) {
        TableAccess access = connections.forTable(datasourceId, table);
        Map<String, Object> parameters = parameters(access);
        parameters.put("parentId", parentId);
        parameters.put("recordId", recordId);
        return access.query(access.select(access.columns()) + normal(access) + " AND " + access.column("PARENT_ID")
                + " = :parentId AND " + access.column("ID") + " = :recordId", parameters);
    }

    public long countByParent(TableAccess access, Long parentId) {
        return count(access, "PARENT_ID", parentId, null, false, true);
    }
    public long countByColumn(Long datasourceId, String table, String column, Object value, Long excludedId) {
        return count(connections.forTable(datasourceId, table), column, value, excludedId, false, false);
    }
    public long countActiveByColumn(Long datasourceId, String table, String column, Object value) {
        return count(connections.forTable(datasourceId, table), column, value, null, true, true);
    }
    private long count(TableAccess access, String column, Object value, Long excludedId, boolean active, boolean filterDeleted) {
        Map<String, Object> parameters = filterDeleted ? parameters(access) : new LinkedHashMap<>();
        String where = filterDeleted ? normal(access) : " WHERE 1=1";
        where += " AND " + access.column(column) + (value == null ? " IS NULL" : " = :value");
        if (value != null) parameters.put("value", access.jdbcParameter(column, value));
        if (excludedId != null) {
            where += " AND " + access.column("ID") + " <> :excludedId";
            parameters.put("excludedId", excludedId);
        }
        if (active) {
            where += " AND " + access.column("STATUS") + " = :status";
            parameters.put("status", "1");
        }
        return access.count(access.select(java.util.Collections.singletonList("ID")) + where, parameters);
    }

    public List<Map<String, Object>> selectPage(TableAccess access, Collection<String> columns, Long parentId,
            MasterSubmodelPageReqVO request, long offset, int size) {
        String sort = request == null ? null : request.getOrderByColumn();
        String direction = request == null ? null : request.getIsAsc();
        if (sort != null && !sort.trim().isEmpty() && !"ID".equalsIgnoreCase(sort)) throw new ServiceException("排序字段无效");
        if (direction != null && !direction.isEmpty() && !"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new ServiceException("排序方向无效");
        }
        Map<String, Object> parameters = parameters(access);
        parameters.put("parentId", parentId);
        String sql = access.select(columns) + normal(access) + " AND " + access.column("PARENT_ID")
                + " = :parentId ORDER BY " + access.column("ID")
                + ("asc".equalsIgnoreCase(direction) ? " ASC" : " DESC");
        return access.page(sql, parameters, offset, size);
    }

    private String normal(TableAccess access) {
        return " WHERE 1=1" + (access.contains("DEL_FLAG") ? " AND " + access.column("DEL_FLAG") + " = :deleted" : "");
    }
    private Map<String, Object> parameters(TableAccess access) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (access.contains("DEL_FLAG")) parameters.put("deleted", "0");
        return parameters;
    }
}
