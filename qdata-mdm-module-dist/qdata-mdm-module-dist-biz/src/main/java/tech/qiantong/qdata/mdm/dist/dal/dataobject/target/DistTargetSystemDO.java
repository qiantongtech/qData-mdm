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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.dataobject.target;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 分发目标系统 DO 对象 DIST_TARGET_SYSTEM
 *
 * @author zhangyanyan
 * @date 2026-10-10
 */
@Data
@TableName(value = "DIST_TARGET_SYSTEM")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DIST_TARGET_SYSTEM_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DistTargetSystemDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标系统编码 */
    private String code;

    /** 目标系统名称 */
    private String name;

    /** 目标系统类型：ERP、SRM、CRM、OTHER */
    private String type;

    /** 目标系统联系人姓名 */
    private String contactName;

    /** 负责人平台用户 ID，与创建人 CREATOR_ID 分开保存。 */
    @TableField("RESP_USER_ID")
    private Long responsibleUserId;

    /** 目标系统联系人信息 */
    private String contactInfo;

    /** 状态：1、启用、2、停用 */
    private String status;

    /** 目标系统用途及业务说明 */
    private String description;

    /** 是否有效；0：无效，1：有效 */
    private String validFlag;

    /** 逻辑删除标志：0未删除，1已删除 */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;


}
