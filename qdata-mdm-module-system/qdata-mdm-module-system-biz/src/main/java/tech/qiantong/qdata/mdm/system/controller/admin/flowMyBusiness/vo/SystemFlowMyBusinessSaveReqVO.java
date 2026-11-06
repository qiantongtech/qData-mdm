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

package tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 审批记录 创建/修改 Request VO SYSTEM_FLOW_MY_BUSINESS
 *
 * @author qdata
 * @date 2025-04-25
 */
@Schema(description = "审批记录 Response VO")
@Data
public class SystemFlowMyBusinessSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "流程定义key", example = "")
    @Size(max = 256, message = "流程定义key长度不能超过256个字符")
    private String processDefinitionKey;

    @Schema(description = "流程定义id", example = "")
    @Size(max = 256, message = "流程定义id长度不能超过256个字符")
    private String processDefinitionId;

    @Schema(description = "流程业务实例id", example = "")
    @Size(max = 256, message = "流程业务实例id长度不能超过256个字符")
    private String processInstanceId;

    @Schema(description = "流程业务简要描述", example = "")
    @Size(max = 256, message = "流程业务简要描述长度不能超过256个字符")
    private String title;

    @Schema(description = "业务表id", example = "")
    @Size(max = 256, message = "业务表id长度不能超过256个字符")
    private Long dataId;

    @Schema(description = "流程类型，0：服务资源，1：数据维护", example = "")
    @Size(max = 256, message = "流程类型，0：服务资源，1：数据维护长度不能超过256个字符")
    private String type;

    @Schema(description = "业务类名，用来获取spring容器里的服务对象", example = "")
    @Size(max = 256, message = "业务类名，用来获取spring容器里的服务对象长度不能超过256个字符")
    private String serviceImplName;

    @Schema(description = "申请人", example = "")
    @Size(max = 256, message = "申请人长度不能超过256个字符")
    private String proposer;

    @Schema(description = "流程状态，0：审批中，1：审批中，2：审批通过，3：审批拒绝，4：审批撤回，5：审批异常", example = "")
    @Size(max = 256, message = "流程状态长度不能超过256个字符")
    private String actStatus;

    @Schema(description = "当前节点在实例中的id", example = "")
    @Size(max = 256, message = "当前节点在实例中的id长度不能超过256个字符")
    private String taskId;

    @Schema(description = "当前节点名称", example = "")
    @Size(max = 256, message = "当前节点名称长度不能超过256个字符")
    private String taskName;

    @Schema(description = "当前节点在流程定义中的id", example = "")
    @Size(max = 256, message = "当前节点在流程定义中的id长度不能超过256个字符")
    private String taskNameId;

    @Schema(description = "当前节点可以处理的用户名，String[]的json串", example = "")
    @Size(max = 256, message = "当前节点可以处理的用户名，String[]的json串长度不能超过256个字符")
    private String todoUsers;

    @Schema(description = "处理过的人，String[]的json串", example = "")
    @Size(max = 256, message = "处理过的人，String[]的json串长度不能超过256个字符")
    private String doneUsers;

    @Schema(description = "当前节点优先级", example = "")
    @Size(max = 256, message = "当前节点优先级长度不能超过256个字符")
    private String prority;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;

}
