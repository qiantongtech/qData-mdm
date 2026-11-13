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

package tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 重复检测候选组
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeGroupRespVO {
    private Long id;
    private Long taskInstanceId;
    private String modelVersionNo;
    private Long modelVersionStatus;
    private String taskName;
    private String scopeType;
    private String remark;
    private String groupNo;
    private Long modelId;
    private String type;
    private Date transferTime;
    private String status;
    private BigDecimal maxMatchScore;
    private Long parentGroupId;
    private String validFlag;
    private Date createTime;
    private Integer memberCount;
    private String sourceSystems;
    private Integer ruleCount;
    private List<String> ruleNames;
    private String primaryObject;
    private String hitFields;
}
