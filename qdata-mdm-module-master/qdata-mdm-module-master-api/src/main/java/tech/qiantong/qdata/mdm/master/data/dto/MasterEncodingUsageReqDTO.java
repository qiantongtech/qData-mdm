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

import lombok.Data;

import java.io.Serializable;

/** 指定主模型字段及其引用的编码方案版本。 */
@Data
public class MasterEncodingUsageReqDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 已成功发布的普通或分类主模型ID。 */
    private Long modelId;
    /** 引用编码方案的稳定字段ID。 */
    private Long fieldId;
    /** 编码方案版本ID，不是编码方案主体ID。 */
    private Long encodingVersionId;
}
