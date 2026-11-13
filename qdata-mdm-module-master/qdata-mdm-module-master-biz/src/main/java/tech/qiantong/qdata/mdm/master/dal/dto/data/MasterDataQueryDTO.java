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

package tech.qiantong.qdata.mdm.master.dal.dto.data;

import lombok.Data;

import java.util.Collection;
import java.util.Map;

/**
 * 权威主数据动态物理表查询条件。
 *
 * <p>字段值均由业务层完成格式和业务规则校验，数据访问层只负责绑定参数和生成动态表查询。</p>
 */
@Data
public class MasterDataQueryDTO {
    /** 发布元数据提供的逻辑列到真实列名映射。 */
    private Map<String, String> columnMappings;

    private Long modelId;
    private String nameColumn;
    private String status;
    private String name;
    private Collection<Long> categoryIds;
    /** 普通模型的分类业务列及按其发布类型转换后的关联键。 */
    private String classificationColumn;
    private Collection<Object> classificationValues;
    /**
     * 固定业务表筛选后允许查询的主数据记录 ID；动态物化表 Repository 只消费该结果。
     */
    private Collection<Long> recordIds;
}
