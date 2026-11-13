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

package tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.Date;

/**
 * 编码方案版本 Request VO 对象 MODEL_ENCODING_VERSION
 *
 * @author qdata
 * @date 2026-09-17
 */
@Schema(description = "编码方案版本 Request VO")
@Data
public class ModelEncodingVersionPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "方案ID", example = "")
    private Long encodingId;

    @Schema(description = "版本号", example = "")
    private String versionNo;

    @Schema(description = "状态", example = "")
    private Integer status;

    @Schema(description = "生效时间", example = "")
    private Date effectiveTime;




}
