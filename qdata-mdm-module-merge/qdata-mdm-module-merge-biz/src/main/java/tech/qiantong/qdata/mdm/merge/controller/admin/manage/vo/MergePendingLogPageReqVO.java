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

package tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo;

import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/** 合并结果操作记录分页查询参数，分页字段继承自统一分页对象。 */
@Data
public class MergePendingLogPageReqVO extends PageParam {

    /** 审批记录排序字段，仅支持编号与处理时间。 */
    private String orderByColumn;

    /** 表格排序方向：ascending 或 descending。 */
    private String isAsc;
}
