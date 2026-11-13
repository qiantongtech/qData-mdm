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

package tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 审批记录 Response VO 对象 SYSTEM_FLOW_MY_BUSINESS
 *
 * @author qdata
 * @date 2025-04-25
 */
@Schema(description = "审批记录 Response VO")
@Data
public class SystemFlowMyBusinessRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "流程定义key")
    @Schema(description = "流程定义key", example = "")
    private String processDefinitionKey;

    @Excel(name = "流程定义id")
    @Schema(description = "流程定义id", example = "")
    private String processDefinitionId;

    @Excel(name = "流程业务实例id")
    @Schema(description = "流程业务实例id", example = "")
    private String processInstanceId;

    @Excel(name = "流程业务简要描述")
    @Schema(description = "流程业务简要描述", example = "")
    private String title;

    @Excel(name = "业务表id")
    @Schema(description = "业务表id", example = "")
    private Long dataId;

    @Excel(name = "流程类型，0：服务资源，1：数据维护")
    @Schema(description = "流程类型，0：服务资源，1：数据维护", example = "")
    private String type;

    @Excel(name = "业务类名，用来获取spring容器里的服务对象")
    @Schema(description = "业务类名，用来获取spring容器里的服务对象", example = "")
    private String serviceImplName;

    @Excel(name = "申请人")
    @Schema(description = "申请人", example = "")
    private String proposer;

    @Excel(name = "流程状态，0：审批中，1：审批中，2：审批通过，3：审批拒绝，4：审批撤回，5：审批异常")
    @Schema(description = "流程状态，0：审批中，1：审批中，2：审批通过，3：审批拒绝，4：审批撤回，5：审批异常", example = "")
    private String actStatus;

    @Excel(name = "当前节点在实例中的id")
    @Schema(description = "当前节点在实例中的id", example = "")
    private String taskId;

    @Excel(name = "当前节点名称")
    @Schema(description = "当前节点名称", example = "")
    private String taskName;

    @Excel(name = "当前节点在流程定义中的id")
    @Schema(description = "当前节点在流程定义中的id", example = "")
    private String taskNameId;

    @Excel(name = "当前节点可以处理的用户名，String[]的json串")
    @Schema(description = "当前节点可以处理的用户名，String[]的json串", example = "")
    private String todoUsers;

    @Excel(name = "处理过的人，String[]的json串")
    @Schema(description = "处理过的人，String[]的json串", example = "")
    private String doneUsers;

    @Excel(name = "当前节点优先级")
    @Schema(description = "当前节点优先级", example = "")
    private String prority;

    @Excel(name = "流程变量，是一个JsonObj类型")
    @Schema(description = "流程变量，是一个JsonObj类型", example = "")
    private String flowValues;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志")
    @Schema(description = "删除标志", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人id")
    @Schema(description = "创建人id", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人id")
    @Schema(description = "更新人id", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
