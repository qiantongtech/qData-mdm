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
 * 编码方案规则段 DO 对象 MODEL_ENCODING_ITEM
 *
 * @author qdata
 * @date 2026-09-17
 */
@Data
@TableName(value = "MODEL_ENCODING_ITEM")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MODEL_ENCODING_ITEM_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelEncodingItemDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 编码版本ID */
    private Long encodingVersionId;

    /** 规则段类型 */
    private String type;

    /** 排序号 */
    private Long sortOrder;

    /** 固定值/字段参数名称/日期时间输出格式 */
    private String fixedValue;

    /** 位数 */
    private Long sequenceLength;

    /** 流水号开始 */
    private Long sequenceStart;

    /** 补位字符 */
    private String padChar;

    /** 扩展配置JSON */
    private String config;

    /** 逻辑删除标志：0未删除，1已删除 */
    @TableLogic
    private Boolean delFlag;

    /** 重置方式 */
    private String resetMethod;

    /** 步长 */
    private Long stepSize;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;


}
