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

package tech.qiantong.qdata.mdm.master.change.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/** 跨模块单条主数据申请，不包含融合处理或质检参数。 */
@Data
public class MasterChangeRequestSubmitReqDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当前成功发布的模型物化实例ID。 */
    private Long instanceId;
    /** CREATE新增或UPDATE修改。 */
    private String mode;
    /** 修改时必填，新增时为空。 */
    private Long recordId;
    /** 分类模型目标父节点ID，空表示根节点。 */
    private Long parentId;
    /** 申请归属人ID，名称由master读取。 */
    private Long applicantId;
    /** MERGE或MANUAL，未传或空白时默认MERGE。 */
    private String changeSource;
    private String applyReason;
    /** 模型业务字段编码及最终取值，参考数据传数据项编码。 */
    private Map<String, String> values;
}
