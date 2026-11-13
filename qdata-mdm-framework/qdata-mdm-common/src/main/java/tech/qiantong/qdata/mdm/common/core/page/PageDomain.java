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

package tech.qiantong.qdata.mdm.common.core.page;

import tech.qiantong.qdata.mdm.common.utils.StringUtils;

/**
 * 分页数据
 *
 * @author qdata
 */
public class PageDomain
{
    /** 当前记录起始索引 */
    private Integer pageNum;

    /** 每页显示记录数 */
    private Integer pageSize;

    /** 排序列 */
    private String orderByColumn;

    /** 排序的方向desc或者asc */
    private String isAsc = "asc";

    /** 分页参数合理化 */
    private Boolean reasonable = true;

    public String getOrderBy()
    {
        if (StringUtils.isEmpty(orderByColumn))
        {
            return "";
        }
        return StringUtils.toUnderScoreCase(orderByColumn) + " " + isAsc;
    }

    /**
     * 获取包含 NULL 的排序字段
     *
     * @return 排序字段
     */
    public String getOrderByContainNull()
    {
        if (StringUtils.isEmpty(orderByColumn)) {
            return "";
        }

        // 将字段名转为下划线风格（如 userName -> user_name）
        String column = StringUtils.toUnderScoreCase(orderByColumn).trim();
        if (StringUtils.isEmpty(column)) {
            return "";
        }

        // 统一排序方向（默认升序）
        String direction = StringUtils.isEmpty(isAsc) ? "ASC" : isAsc.trim().toUpperCase();
        // Validate input before constructing the expression passed directly to PageHelper.
        if (!column.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)?")
                || !("ASC".equals(direction) || "DESC".equals(direction))) {
            throw new IllegalArgumentException("Invalid sort column or direction");
        }
        // 构建 CASE 表达式，控制 NULL 的排序位置
        String nullOrderCase;
        if ("ASC".equals(direction)) {
            nullOrderCase = String.format("CASE WHEN %s IS NULL THEN 0 ELSE 1 END, ", column);
        } else {
            nullOrderCase = String.format("CASE WHEN %s IS NULL THEN 1 ELSE 0 END, ", column);
        }

        return nullOrderCase + column + " " + direction;
    }

    public Integer getPageNum()
    {
        return pageNum;
    }

    public void setPageNum(Integer pageNum)
    {
        this.pageNum = pageNum;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(Integer pageSize)
    {
        this.pageSize = pageSize;
    }

    public String getOrderByColumn()
    {
        return orderByColumn;
    }

    public void setOrderByColumn(String orderByColumn)
    {
        this.orderByColumn = orderByColumn;
    }

    public String getIsAsc()
    {
        return isAsc;
    }

    public void setIsAsc(String isAsc)
    {
        if (StringUtils.isNotEmpty(isAsc))
        {
            // 兼容前端排序类型
            if ("ascending".equals(isAsc))
            {
                isAsc = "asc";
            }
            else if ("descending".equals(isAsc))
            {
                isAsc = "desc";
            }
            this.isAsc = isAsc;
        }
    }

    public Boolean getReasonable()
    {
        if (StringUtils.isNull(reasonable))
        {
            return Boolean.TRUE;
        }
        return reasonable;
    }

    public void setReasonable(Boolean reasonable)
    {
        this.reasonable = reasonable;
    }
}
