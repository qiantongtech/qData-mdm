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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.task.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 分发任务执行日志 Request VO 对象 DIST_TASK_INSTANCE_LOG
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务执行日志 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DistTaskInstanceLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "分发任务运行实例ID", example = "")
    private Long instanceId;

    @Schema(description = "权威主数据记录ID", example = "")
    private Long masterRecordId;

    @Schema(description = "主数据编码快照", example = "")
    private String masterCode;

    @Schema(description = "主数据名称快照", example = "")
    private String masterName;

    @Schema(description = "分发状态：SUCCESS成功、FAILURE失败", example = "")
    private String distributeStatus;

    @Schema(description = "本条数据分发时间", example = "")
    private Date distributeTime;

    @Schema(description = "分发请求数据JSON", example = "")
    private String requestData;

    @Schema(description = "目标系统响应编码", example = "")
    private String responseCode;

    @Schema(description = "目标系统响应信息", example = "")
    private String responseMessage;

    @Schema(description = "失败原因", example = "")
    private String failReason;

    @Schema(description = "重试次数", example = "")
    private Long retryCount;




}
