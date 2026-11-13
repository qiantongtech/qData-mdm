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

package tech.qiantong.qdata.mdm.system.dal.dataobject.flowMyBusiness;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 审批记录 DO 对象 SYSTEM_FLOW_MY_BUSINESS
 *
 * @author qdata
 * @date 2025-04-25
 */
@Data
@TableName(value = "system_flow_my_business")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("SYSTEM_FLOW_MY_BUSINESS_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemFlowMyBusinessDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程定义key */
    private String processDefinitionKey;

    /** 流程定义id */
    private String processDefinitionId;

    /** 流程业务实例id */
    private String processInstanceId;

    /** 流程业务简要描述 */
    private String title;

    /** 业务表id */
    private Long dataId;

    /** 流程类型，0：服务资源，1：数据维护 */
    private String type;

    /** 业务类名，用来获取spring容器里的服务对象 */
    private String serviceImplName;

    /** 申请人 */
    private String proposer;

    /** 流程状态，0：审批中，1：审批通过，2：审批拒绝，3：审批撤回，4：审批异常 */
    private String actStatus;

    /** 当前节点在实例中的id */
    private String taskId;

    /** 当前节点名称 */
    private String taskName;

    /** 当前节点在流程定义中的id */
    private String taskNameId;

    /** 当前节点可以处理的用户名，String[]的json串 */
    private String todoUsers;

    /** 处理过的人，String[]的json串 */
    private String doneUsers;

    /** 当前节点优先级 */
    private String prority;

    /** 流程变量，是一个JsonObj类型 */
    private String flowValues;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

}
