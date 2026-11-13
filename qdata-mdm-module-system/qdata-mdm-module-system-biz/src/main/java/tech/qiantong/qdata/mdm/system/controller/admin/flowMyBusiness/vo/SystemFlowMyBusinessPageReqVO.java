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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.List;

/**
 * 审批记录 Request VO 对象 SYSTEM_FLOW_MY_BUSINESS
 *
 * @author qdata
 * @date 2025-04-25
 */
@Schema(description = "审批记录 Request VO")
@Data
public class SystemFlowMyBusinessPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "流程定义key", example = "")
    private String processDefinitionKey;

    @Schema(description = "流程定义id", example = "")
    private String processDefinitionId;

    @Schema(description = "流程业务实例id", example = "")
    private String processInstanceId;

    @Schema(description = "流程业务简要描述", example = "")
    private String title;

    @Schema(description = "业务表id", example = "")
    private Long dataId;

    @Schema(description = "流程类型，0：服务资源，1：数据维护", example = "")
    private String type;

    @Schema(description = "业务类名，用来获取spring容器里的服务对象", example = "")
    private String serviceImplName;

    @Schema(description = "申请人", example = "")
    private String proposer;

    @Schema(description = "流程状态，0：审批中，1：审批中，2：审批通过，3：审批拒绝，4：审批撤回，5：审批异常", example = "")
    private String actStatus;

    @Schema(description = "当前节点在实例中的id", example = "")
    private String taskId;

    @Schema(description = "当前节点名称", example = "")
    private String taskName;

    @Schema(description = "当前节点在流程定义中的id", example = "")
    private String taskNameId;

    @Schema(description = "当前节点可以处理的用户名，String[]的json串", example = "")
    private String todoUsers;

    @Schema(description = "处理过的人，String[]的json串", example = "")
    private String doneUsers;

    @Schema(description = "当前节点优先级", example = "")
    private String prority;

    @Schema(description = "流程变量，是一个JsonObj类型", example = "")
    private String flowValues;

    private List<String> processInstanceIdList;

    private List<String> processInstanceBizCodeList;

}
