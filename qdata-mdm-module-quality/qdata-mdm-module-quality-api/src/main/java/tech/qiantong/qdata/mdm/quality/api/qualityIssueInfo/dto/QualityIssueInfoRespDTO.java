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

package tech.qiantong.qdata.mdm.quality.api.qualityIssueInfo.dto;

import lombok.Data;

import java.util.Date;

/**
 * 问题数据明细 DTO 对象 QUALITY_ISSUE_INFO
 *
 * @author qdata
 * @date 2026-09-23
 */
@Data
public class QualityIssueInfoRespDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 质量问题ID */
    private Long issueId;

    /** 质量规则ID */
    private Long qualityRuleId;

    /** 模型字段ID */
    private Long modelColumnId;

    /** 质量任务执行实例ID */
    private Long lastCheckInstanceId;

    /** 问题项状态：1未解决、2已解决 */
    private String status;

    /** 规则检测失败原因 */
    private String failReason;

    /** 最近一次检测失败时间 */
    private Date lastFoundTime;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    private Boolean delFlag;


}
