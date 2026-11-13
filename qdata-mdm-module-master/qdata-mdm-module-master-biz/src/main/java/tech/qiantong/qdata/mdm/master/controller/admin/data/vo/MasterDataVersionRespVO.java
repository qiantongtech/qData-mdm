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

package tech.qiantong.qdata.mdm.master.controller.admin.data.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 单条主数据的正式版本列表响应；不包含 MASTER_DATA_VERSION.SNAPSHOT_DATA 大字段。
 */
@Data
public class MasterDataVersionRespVO {
    /** 字符串传输 BIGINT，避免 JavaScript 丢失精度。 */
    private String id;
    private String versionNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date generateTime;
    private String generateType;
    private String changeType;
    private String operatorName;
    private String requestNo;
    /** 当前版本标志：数据库字符值 0 否、1 是。 */
    private String currentFlag;
}
