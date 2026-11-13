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

package tech.qiantong.qdata.mdm.quality.api.qualityTask.dto;

import lombok.Data;

import java.util.Date;

/**
 * 主数据质量任务 DTO 对象 QUALITY_TASK
 *
 * @author qdata
 * @date 2026-09-22
 */
@Data
public class QualityTaskReqDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 质量任务编码 */
    private String taskCode;

    /** 质量任务名称 */
    private String taskName;

    /** 负责人用户ID。 */
    private Long responsibleUserId;

    /** 主数据模型ID */
    private Long modelId;

    /** 主数据模型版本 */
    private String modelVersion;

    /** 数据范围类型：1全部权威主数据、2按条件筛选 */
    private String dataScopeType;

    /** 数据范围条件配置JSON */
    private String dataScopeConfig;

    /** 是否启用定时执行：0否、1是 */
    private String scheduleFlag;

    /** 定时执行配置JSON */
    private String scheduleConfig;

    /** 任务状态：1启用、2停用 */
    private String status;

    /** 最近一次实际启动执行的时间 */
    private Date lastRunTime;

    /** 最近一次运行状态：1执行中、2失败、3部分成功、4成功 */
    private String lastRunStatus;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    private Boolean delFlag;


}
