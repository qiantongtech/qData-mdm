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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.source;

import lombok.Data;

/**
 * 来源系统责任人基础信息查询结果。
 *
 * @author chentangwen
 */
@Data
public class IngestSourceResponsibleUserRow {

    /**
     * 责任人用户 ID。
     */
    private Long userId;

    /**
     * 责任人显示名称，优先使用用户昵称。
     */
    private String name;

    /**
     * 责任人联系电话。
     */
    private String phone;
}
