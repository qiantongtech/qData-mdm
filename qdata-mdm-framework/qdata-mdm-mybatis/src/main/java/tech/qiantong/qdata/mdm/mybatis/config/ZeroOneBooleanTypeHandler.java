/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.mybatis.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 把 Java Boolean 与数据库 varchar(1) 的 '0'/'1' 互转。
 * <p>
 * 背景：GBase 8s（Informix）JDBC 在 PreparedStatement 上对 Boolean 参数会绑定成字符串 "true"/"false"
 * （4/5 字符），写入 varchar(1) 的标志列（valid_flag/del_flag 等）会报"值的长度超过了列的长度"。
 * MySQL JDBC 则绑成 0/1，无此问题。
 * <p>
 * 解法：全局覆盖 Boolean 的 TypeHandler，写库时统一绑 '0'/'1' 字符串，读库时 '1'→true、'0'→false。
 * 对 MySQL 同样兼容（写入 '0'/'1' 与原行为一致）。
 */
@MappedTypes(Boolean.class)
@Component
public class ZeroOneBooleanTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        // 真 BOOLEAN/BIT 列按布尔绑定(避免写字符串到原生布尔列报错);其余(varchar(1) 标志位等)绑 '0'/'1' 字符串
        if (jdbcType == JdbcType.BOOLEAN || jdbcType == JdbcType.BIT) {
            ps.setBoolean(i, Boolean.TRUE.equals(parameter));
        } else {
            ps.setString(i, Boolean.TRUE.equals(parameter) ? "1" : "0");
        }
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private Boolean parse(String v) {
        if (v == null) {
            return null;
        }
        v = v.trim();
        if (v.isEmpty()) {
            return null;
        }
        // 兼容 '1'/'true'/'t'/'Y' → true
        return "1".equals(v) || "true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v);
    }
}
