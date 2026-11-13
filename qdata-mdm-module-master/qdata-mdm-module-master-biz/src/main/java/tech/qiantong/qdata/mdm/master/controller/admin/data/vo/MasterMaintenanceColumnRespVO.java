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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/** 成功发布的维护字段规则，供主表和子表共用。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MasterMaintenanceColumnRespVO extends MasterColumnRespVO {
    private String columnCategory;
    private String encodingMode;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dictTypeId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long relatedModelId;
    private String relatedCode;
    private String displayCode;
    private String nodeRule;
    private String displayMode;
    private List<ReferenceOption> referenceOptions = new ArrayList<>();

    @Data
    public static class ReferenceOption {
        private String value;
        private String label;
    }
}
