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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import java.util.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 接入任务启停或调度开关变更请求对象，包含任务 ID 和目标状态。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskStatusReqVO {

    /**
     * 待变更状态的接入任务 ID。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 目标开关状态：0 表示关闭，1 表示开启，具体含义由状态变更接口确定。
     */
    private String status;
}
