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

package tech.qiantong.qdata.mdm.model.encoding.dto;

import lombok.Data;

import java.util.Date;

/**
 * 编码方案版本 DTO 对象 MODEL_ENCODING_VERSION
 *
 * @author qdata
 * @date 2026-09-17
 */
@Data
public class ModelEncodingVersionReqDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 方案ID */
    private Long encodingId;

    /** 版本号 */
    private String versionNo;

    /** 状态（1草稿 2停用 3发布） */
    private String status;

    /** 生效时间 */
    private Date effectiveTime;

    /** 逻辑删除标志：0未删除，1已删除 */
    private Boolean delFlag;

    /** $column.columnComment */
    private Boolean validFlag;


}
