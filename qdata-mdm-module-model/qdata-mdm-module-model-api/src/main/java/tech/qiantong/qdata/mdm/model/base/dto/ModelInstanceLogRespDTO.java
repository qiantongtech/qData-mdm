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
 * 模型发布全流程日志返回信息。
 */
@Data
public class ModelInstanceLogRespDTO {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 对应的唯一发布实例
     */
    private Long instanceId;

    /**
     * 全流程内容中最后一条事件的序号，追加事件时递增
     */
    private Long sequenceNo;

    /**
     * 最近阶段：READ_CONFIG读取配置；CONFIG配置校验；BUILD构建结构；DIFF比较结构；
     * CHANGE变更校验；TARGET目标库校验；PLAN生成SQL计划；EXECUTE执行SQL；
     * VERIFY核验结果；COMPENSATE安全回撤；COMMIT保存正式结果；CLOSE结束流程
     */
    private String stage;

    /**
     * 最近状态：RUNNING进行中；SUCCEEDED成功；FAILED失败；UNKNOWN待核验；
     * SKIPPED跳过；NOT_EXECUTED未执行；EXECUTING执行中；COMPENSATING回撤中；
     * PARTIAL_FAILED部分失败；COMMIT_PENDING待保存正式结果
     */
    private String status;

    /**
     * 对应SQL计划编号，非SQL阶段为空
     */
    private Integer stepNo;

    /**
     * FORWARD或COMPENSATION，非SQL阶段为空
     */
    private String executionDirection;

    /**
     * 该步骤该方向第几次尝试，从1开始，非SQL阶段为空
     */
    private Integer attemptNo;

    /**
     * 全流程事件列表JSON，完整保留各阶段、SQL尝试、核验、错误和回撤记录
     */
    private String logContent;

    /**
     * 整个发布实例的开始时间
     */
    private Date startedTime;

    /**
     * 整个发布实例的结束时间，未结束时为空
     */
    private Date finishedTime;

    /**
     * 系统有效标志，1有效、0无效
     */
    private Boolean validFlag;

    /**
     * 系统逻辑删除标志，0未删除、1已删除；不删除实际物理表或字段
     */
    private Boolean delFlag;

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
