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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.dataobject.service;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 数据服务调用日志 DO 对象 DIST_SERVICE_CALL_LOG
 *
 * @author chentangwen
 * @date 2026-10-21
 */
@Data
@TableName(value = "DIST_SERVICE_CALL_LOG")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DistServiceCallLogDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据服务ID
     */
    private Long serviceId;

    /**
     * 数据服务名称快照
     */
    private String serviceName;

    /**
     * 服务授权记录ID
     */
    private Long authorizationId;

    /**
     * 授权单号快照
     */
    private String authorizationNo;

    /**
     * 授权主体类型
     */
    private String subjectType;

    /**
     * 授权主体ID
     */
    private Long subjectId;

    /**
     * 调用系统名称
     */
    private String callerSystem;

    /**
     * 调用方IP地址
     */
    private String callerIp;

    /**
     * 调用接口地址
     */
    private String requestPath;

    /**
     * HTTP请求方式
     */
    private String requestMethod;

    /**
     * 脱敏后的调用参数摘要
     */
    private String requestSummary;

    /**
     * HTTP响应状态码
     */
    private Long httpStatusCode;

    /**
     * 调用结果：SUCCESS成功、FAILURE失败
     */
    private String callResult;

    /**
     * 本次响应数据条数
     */
    private Long responseDataCount;

    /**
     * 脱敏后的响应内容摘要
     */
    private String responseSummary;

    /**
     * 异常信息
     */
    private String errorMessage;

    /**
     * 调用耗时，单位毫秒
     */
    private Long durationMs;

    /**
     * 调用时间
     */
    private Date callTime;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 是否有效：0无效，1有效
     */
    private String validFlag;

    /**
     * 删除标志：0未删除，1已删除
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;


}
