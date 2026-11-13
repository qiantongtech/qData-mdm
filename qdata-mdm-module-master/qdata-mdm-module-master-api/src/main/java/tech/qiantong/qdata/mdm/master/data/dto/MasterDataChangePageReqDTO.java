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

package tech.qiantong.qdata.mdm.master.data.dto;

import java.util.Date;
import lombok.Data;

/**
 * 分发模块读取主数据变更的分页条件。
 * 首次请求传起始时间，后续请求沿用同一条件并携带上页游标。
 */
@Data
public class MasterDataChangePageReqDTO {

    /** 已成功发布的主数据模型 ID。 */
    private Long modelId;

    /** 已成功发布的模型版本 ID。 */
    private Long modelVersionId;

    /** 只返回变更时间严格晚于该时间的记录。 */
    private Date sinceTime;

    /** 每页记录数，分发侧固定为 100。 */
    private Integer pageSize;

    /** 上页返回的游标；第一页不传。 */
    private String cursor;
}
