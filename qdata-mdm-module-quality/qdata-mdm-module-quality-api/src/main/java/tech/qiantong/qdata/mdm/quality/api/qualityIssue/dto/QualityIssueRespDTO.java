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

package tech.qiantong.qdata.mdm.quality.api.qualityIssue.dto;

import lombok.Data;

import java.util.Date;

/**
 * 问题数据 DTO 对象 QUALITY_ISSUE
 *
 * @author qdata
 * @date 2026-09-23
 */
@Data
public class QualityIssueRespDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 编号 */
    private String issueCode;

    /** 主数据模型ID */
    private Long modelId;

    /** 主数据记录ID */
    private Long masterRecordId;

    /** 问题状态：1待处理、2整改中、3已关闭 */
    private String status;

    /** 首次发现时间 */
    private Date firstFoundTime;

    /** 最近一次发现时间 */
    private Date lastFoundTime;

    /** 人工关闭问题时间 */
    private Date closeTime;

    /** 人工关闭用户ID */
    private Long closerId;

    /** 整改情况 */
    private String rectificationStatus;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    private Boolean delFlag;


}
