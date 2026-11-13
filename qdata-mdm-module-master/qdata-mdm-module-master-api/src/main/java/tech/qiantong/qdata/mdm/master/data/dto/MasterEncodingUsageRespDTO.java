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

/** 编码引用字段中已填写编码的正式记录数，不代表该版本历史生成编码的数量。 */
@Data
public class MasterEncodingUsageRespDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long modelId;
    /** 本次读取到的成功模型版本；配置读取失败时为空。 */
    private Long modelVersionId;
    private Long fieldId;
    private Long encodingVersionId;
    /** 排除删除记录、空值、空串及仅含空格的值；查询失败返回0。 */
    private Long usageCount;
}
