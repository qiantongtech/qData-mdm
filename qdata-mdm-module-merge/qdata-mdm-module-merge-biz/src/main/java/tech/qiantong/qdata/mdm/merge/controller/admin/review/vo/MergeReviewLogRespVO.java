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

import java.util.Date;

/**
 * 匹配审核追溯记录。
 *
 * @author chentangwen
 * @date 2026-09-29
 */
@Data
public class MergeReviewLogRespVO {
    private Long id;
    private String actionType;
    private Long reviewerId;
    private String reviewerName;
    private Date reviewTime;
    private String reason;
    private String beforeData;
    private String afterData;
}
