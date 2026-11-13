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

package tech.qiantong.qdata.mdm.master.controller.admin.data.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 权威主数据列表筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MasterDataPageReqVO extends PageParam {
    /** 卡片携带的主模型ID；服务端核对成功发布实例归属。 */
    @javax.validation.constraints.Positive(message = "模型ID必须为正数")
    private Long modelId;
    /** 页面字段定义对应的成功发布版本。 */
    @javax.validation.constraints.Positive(message = "模型版本ID必须为正数")
    private Long modelVersionId;
    @Size(max = 100, message = "名称不能超过100字")
    private String name;
    @Pattern(regexp = "^$|[1-4]", message = "主数据状态无效")
    private String status;
    /** 主数据关联申请单的展示审批状态。 */
    @Pattern(regexp = "^$|[0-4]", message = "审批状态无效")
    private String approvalStatus;
    /** 来源系统 ID，通过有效来源记录映射筛选。 */
    @Size(max = 19, message = "来源系统ID长度不能超过19位")
    @Pattern(regexp = "^$|^[1-9]\\d*$", message = "来源系统ID格式无效")
    private String sourceSystem;
    /** 分类树节点的 ID。 */
    @Size(max = 19, message = "分类节点ID长度不能超过19位")
    @Pattern(regexp = "^$|^[1-9]\\d*$", message = "分类节点ID格式无效")
    private String treeId;

    /** 普通模型分类导航；四项必须同时传递，全部记录时均不传。 */
    @javax.validation.constraints.Positive
    private Long classificationFieldId;
    @Size(max = 19)
    @Pattern(regexp = "^[1-9]\\d*$")
    private String classificationNodeId;
    @javax.validation.constraints.Positive
    private Long classificationTargetVersionId;
    @Pattern(regexp = "^[a-f0-9]{64}$")
    private String classificationRevision;

    public MasterDataPageReqVO() {
        setPageSize(10);
    }
}
