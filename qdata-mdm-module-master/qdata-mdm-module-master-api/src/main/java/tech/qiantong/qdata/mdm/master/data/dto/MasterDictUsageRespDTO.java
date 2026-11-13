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

/** 一个参考数据项在指定模型字段中的正式数据使用条数。 */
@Data
public class MasterDictUsageRespDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long modelId;
    /** 本次读取到的成功模型版本；模型配置读取失败时为空。 */
    private Long modelVersionId;
    private Long fieldId;
    private Long dictTypeId;
    /** 保留调用方传入的原始编码。 */
    private String itemCode;
    /** 未删除正式记录的条数，包含生效、冻结、失效和归档状态；查询失败时返回0。 */
    private Long usageCount;
}
