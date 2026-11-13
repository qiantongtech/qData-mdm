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

package tech.qiantong.qdata.mdm.master.controller.admin.change.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 单条主数据已生效变更列表响应项；不包含业务快照大字段。
 */
@Data
public class MasterChangeRespVO {

    /** 字符串传输 BIGINT，避免 JavaScript 丢失精度。 */
    private String id;

    /** 变更时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    /** 变更类型。 */
    private String type;

    /** 变更来源。 */
    private String source;

    /** 操作人名称。 */
    private String operatorName;

    /** 变更前版本号。 */
    private String beforeVersionNo;

    /** 变更后版本号。 */
    private String afterVersionNo;

    /** 关联申请编号。 */
    private String requestNo;

    /** 变更原因。 */
    private String changeReason;
}
