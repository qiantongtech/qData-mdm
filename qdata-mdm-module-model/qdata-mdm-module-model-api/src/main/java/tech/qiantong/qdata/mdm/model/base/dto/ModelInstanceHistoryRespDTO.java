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

package tech.qiantong.qdata.mdm.model.base.dto;

import lombok.Data;

import java.util.Date;

/**
 * 模型发布历史的列表信息，不携带完整配置、执行计划或SQL大字段。
 */
@Data
public class ModelInstanceHistoryRespDTO {
    /**
     * 发布实例ID
     */
    private Long id;

    /**
     * 当前主模型ID
     */
    private Long modelId;

    /**
     * 本次发布的模型版本ID
     */
    private Long modelVersionId;

    /**
     * 本次发布的版本号
     */
    private String versionNo;

    /**
     * 本次发布使用的数据源ID
     */
    private Long datasourceId;

    /**
     * 发布实例的实际执行状态，包含成功、失败和执行中
     */
    private String status;

    /**
     * 当前或最后执行到的发布阶段
     */
    private String currentStage;

    /**
     * 开始发布的时间
     */
    private Date startedTime;

    /**
     * 本次发布结束时间，尚未结束时为空
     */
    private Date finishedTime;

    /**
     * 整体发布成功的时间，失败时为空
     */
    private Date publishedTime;

    /**
     * 本次发布的发起人
     */
    private String createBy;

    /**
     * 发布记录创建时间
     */
    private Date createTime;

    /**
     * 本次发布的变更类型
     */
    private String changeStatus;

    /**
     * 发布失败原因，成功时为空
     */
    private String failureReason;

    /**
     * 失败后的回撤或保留处理状态
     */
    private String compensationStatus;

    /**
     * 本次发布记录的备注
     */
    private String remark;
}
