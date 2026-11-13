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

package tech.qiantong.qdata.mdm.common.database.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据库中的实际索引或主键定义，不包含模型业务配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbIndex {

    /**
     * 数据库实际名称；驱动未提供主键名称时可以为空。
     */
    private String indexName;

    /**
     * 是否为主键，不能把普通唯一索引当作主键。
     */
    private Boolean primaryKey;

    /**
     * 是否唯一，主键也属于唯一。
     */
    private Boolean unique;

    /**
     * 按数据库中的先后顺序保存索引列。
     */
    private List<String> columnNames;
}
