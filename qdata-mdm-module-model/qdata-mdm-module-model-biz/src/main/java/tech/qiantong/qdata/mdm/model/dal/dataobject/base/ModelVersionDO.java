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

package tech.qiantong.qdata.mdm.model.dal.dataobject.base;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 主数据模型版本 DO 对象 MODEL_VERSION
 *
 * @author qdata
 * @date 2026-09-18
 */
@Data
@TableName(value = "MODEL_VERSION")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MODEL_VERSION_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelVersionDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属主数据模型ID，关联MODEL_BASE.ID；同一模型可以对应多个正式版本 */
    private Long modelId;

    /** 上一正式版本ID，关联MODEL_VERSION.ID；首次发布时为空 */
    private Long baseVersionId;

    /** 版本号 */
    private String versionNo;

    /** 发布时的模型业务说明快照 */
    private String description;

    /** 累计字段最新值及 _history 中本版本每次修改的前后值、操作人和时间（JSON） */
    private String changeData;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志；0：未删除，1：已删除 */
    @TableLogic
    private Boolean delFlag;

    /** 模型状态；0：草稿，1：已发布，2：修订中，3：已封存，4：已删除 */
    private Long status;


}
