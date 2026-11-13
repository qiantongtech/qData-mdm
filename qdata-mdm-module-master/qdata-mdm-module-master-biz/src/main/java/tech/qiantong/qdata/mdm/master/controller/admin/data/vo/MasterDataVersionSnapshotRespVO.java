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

import lombok.Data;

/**
 * MASTER_DATA_VERSION 历史业务快照响应；保留原始内容，不以当前模型字段重新解释。
 */
@Data
public class MasterDataVersionSnapshotRespVO {
    /** 字符串传输 BIGINT 版本 ID，避免 JavaScript 丢失精度。 */
    private String id;
    private String versionNo;
    private String snapshotData;
    /** 原始快照的 String.length() 超过 262144 时为 true，snapshotData 已被截断。 */
    private Boolean truncated;
}
