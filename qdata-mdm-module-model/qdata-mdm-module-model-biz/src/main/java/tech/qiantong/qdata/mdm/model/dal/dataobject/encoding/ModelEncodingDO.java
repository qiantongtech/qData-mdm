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

package tech.qiantong.qdata.mdm.model.dal.dataobject.encoding;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 编码方案 DO 对象 MODEL_ENCODING
 *
 * @author qdata
 * @date 2026-09-16
 */
@Data
@TableName(value = "MODEL_ENCODING")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MODEL_ENCODING_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelEncodingDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 方案编码 */
    private String code;

    /** 方案名称 */
    private String name;

    /** 当前版本ID */
    private Long currentVersionId;

    /** 描述 */
    private String description;

    /** 逻辑删除标志：0未删除，1已删除 */
    @TableLogic
    private Boolean delFlag;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 关联使用该编码方案版本的字段数量 */
    @TableField(exist = false)
    private String modelCount;

    /** 版本详情 */
    @TableField(exist = false)
    private ModelEncodingVersionDO version;
}
