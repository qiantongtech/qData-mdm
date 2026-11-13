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

package tech.qiantong.qdata.mdm.quality.api.qualityIssueTask.dto;

import lombok.Data;

import java.util.Date;

/**
 * 问题整改任务 DTO 对象 QUALITY_ISSUE_TASK
 *
 * @author qdata
 * @date 2026-09-23
 */
@Data
public class QualityIssueTaskRespDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 整改任务编号 */
    private String taskNo;

    /** 质量问题ID */
    private Long issueId;

    /** 主数据模型ID */
    private Long modelId;

    /** 主数据模型版本 */
    private String modelVersion;

    /** 数据记录ID */
    private Long masterRecordId;

    /** 整改负责人用户 ID */
    private Long assigneeId;

    /** 整改任务状态：1待处理、2处理中、3已完成、4已取消； */
    private String status;

    /** 整改说明 */
    private String requirement;

    /** 整改时间 */
    private Date rectificationTime;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    private Boolean delFlag;


}
