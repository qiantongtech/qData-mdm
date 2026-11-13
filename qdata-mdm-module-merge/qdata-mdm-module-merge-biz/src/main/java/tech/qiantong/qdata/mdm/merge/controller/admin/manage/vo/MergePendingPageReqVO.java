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
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
import java.util.List;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 待提交结果分页条件。状态为空时只查尚未进入审批的结果，业务状态可按分组值查询。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergePendingPageReqVO extends PageParam {
    private Long modelId;
    private List<Long> modelIds;
    private String status;
    private String processType;
    private String groupNo;
    private String businessName;
    private String taskName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date transferStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date transferEndTime;

    /** 旧的合并条件。传入对象组或业务名称时不再使用。 */
    private String keyword;
}
