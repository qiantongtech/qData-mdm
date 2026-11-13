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
import java.util.List;

/** 一个主模型引用字段中需要统计的参考数据项；表名和列名由 master 根据成功发布信息定位。 */
@Data
public class MasterDictUsageReqDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 已成功发布的普通或分类主模型ID，不是物化实例ID或子模型ID。 */
    private Long modelId;

    /** 该模型引用参考数据的稳定字段ID。 */
    private Long fieldId;

    /** 字段成功发布配置中的参考数据类型ID。 */
    private Long dictTypeId;

    /** 参考数据项的code，不是数据项ID或名称；不能为空或仅含空白，不裁剪字符串编码。 */
    private List<String> itemCodes;
}
