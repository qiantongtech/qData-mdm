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

import java.util.ArrayList;
import java.util.List;

/** 同一次成功发布对应的主、子模型维护表单配置。 */
@Data
public class MasterMaintenanceFormConfigRespVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long instanceId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelVersionId;
    private List<MasterMaintenanceColumnRespVO> columns = new ArrayList<>();
    private List<Submodel> submodels = new ArrayList<>();

    @Data
    public static class Submodel {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private List<MasterMaintenanceColumnRespVO> columns = new ArrayList<>();
    }
}
