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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 待提交结果列表项
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergePendingRespVO {
    private Long id;
    private Long modelId;
    private Long modelVersionId;
    private String modelVersionNo;
    private Long modelVersionStatus;
    private String taskName;
    private String remark;
    private Long objectGroupId;
    private Long mergeRecordId;
    private String processType;
    private String status;
    private String qualityStatus;
    private Integer resultVersionNo;
    private Date confirmTime;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
    private Date transferTime;
    private String groupNo;
    private Integer sourceCount;
    private String sourceSystems;
    private String businessName;
    private BigDecimal maxMatchScore;
    private Integer ruleCount;
    private List<String> ruleNames;
}
