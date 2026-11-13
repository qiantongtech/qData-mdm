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

import java.util.Date;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 待提交结果详情
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergePendingDetailRespVO {
    private Long id;
    private Long modelId;
    private Long modelVersionId;
    private String modelVersionNo;
    private Long modelVersionStatus;
    private String taskName;
    private String remark;
    private String scopeType;
    private Integer sourceCount;
    private BigDecimal maxMatchScore;
    private Integer ruleCount;
    private Date transferTime;
    private Long objectGroupId;
    private Long mergeRecordId;
    private String processType;
    private String groupNo;
    private String groupType;
    private String status;
    private String qualityStatus;
    private Integer resultVersionNo;
    private String resultData;
    private Long qualityLogId;
    private String qualityMessage;
    private String qualityDetail;
    private String errorMessage;
    private String flowInstanceId;
    private Long requestId;
    private Long targetRecordId;
    private Long resultRecordId;
    private Long resultVersionId;
    /** 本次生效的正式版本编号，与合并结果修订号区分。 */
    private String effectiveVersionNo;
    /** 本次正式版本的业务字段快照，按合并字段路径返回。 */
    private Map<String, Object> effectiveData;
    /** 当前权威记录有效的来源映射数量。 */
    private Long sourceMappingCount;
    private Date confirmTime;
    /** 最近更新时间，供合并详情摘要展示。 */
    private Date updateTime;
    private List<MergePendingFieldRespVO> fields;
}
