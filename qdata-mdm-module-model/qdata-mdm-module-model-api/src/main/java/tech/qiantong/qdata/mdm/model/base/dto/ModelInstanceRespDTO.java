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
 * 模型发布实例返回信息。
 */
@Data
public class ModelInstanceRespDTO {

    /**
     * 发布记录ID，一次根模型整体发布对应一条记录
     */
    private Long id;

    /**
     * 发布根模型ID
     */
    private Long modelId;

    /**
     * 本次工作版本ID；读取失败时可为空，执行DDL前必须完整
     */
    private Long modelVersionId;

    /**
     * 目标数据源ID；解析失败时可为空，执行DDL前必须完整
     */
    private Long datasourceId;

    /**
     * 本次发布执行状态，与模型版本业务状态分开
     */
    private String status;

    /**
     * 上一成功发布记录ID，首次为空
     */
    private Long baseInstanceId;

    /**
     * 当前阶段，页面显示发布进度
     */
    private String currentStage;

    /**
     * 本次发布开始时间
     */
    private Date startedTime;

    /**
     * 本次发布最终结束时间，未决任务可为空
     */
    private Date finishedTime;

    /**
     * 整体成功发布时间，失败为空
     */
    private Date publishedTime;

    /**
     * 实际执行的SQL汇总JSON，含顺序、方向与尝试；零DDL为空
     */
    private String executedSql;

    /**
     * FIRST_PUBLISH/STRUCTURE_CHANGE/METADATA_ONLY/DISABLE/MIXED/NO_CHANGE
     */
    private String changeStatus;

    /**
     * 变化明细JSON：对象、属性、旧值、新值、是否涉及DDL
     */
    private String changeContent;

    /**
     * 点击发布时保存的完整模型配置；成功后作为正式模型读取依据
     */
    private String modelSnapshot;

    /**
     * 目标完整结构及正向/补偿SQL计划JSON，含前后置核验条件和目标连接指纹
     */
    private String executionPlan;

    /**
     * 模型配置及执行计划JSON格式版本
     */
    private Integer structureFormatVersion;

    /**
     * 执行计划内完整目标结构标准化后的SHA-256摘要，构建后填写
     */
    private String structureHash;

    /**
     * 主要失败原因，不另设ERROR_CODE
     */
    private String failureReason;

    /**
     * 主要失败所在阶段
     */
    private String failedStage;

    /**
     * NOT_REQUIRED/PENDING/RUNNING/SUCCEEDED/FAILED/RETAINED
     */
    private String compensationStatus;

    /**
     * 失败或补偿后已确认的残留物理效果JSON，不写入成功结构
     */
    private String remainingEffects;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新人ID
     */
    private Long updaterId;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

}
