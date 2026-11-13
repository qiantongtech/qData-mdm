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

package tech.qiantong.qdata.mdm.merge.controller.admin.review.vo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
import java.util.List;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 匹配审核分页条件。状态为空或 * 时查询全部审核状态，completed 查询已完成。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeReviewPageReqVO extends PageParam {
    private Long modelId;
    private List<Long> modelIds;
    private Long taskInstanceId;
    private String status;
    private String groupNo;
    private String taskName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date transferStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date transferEndTime;
}
