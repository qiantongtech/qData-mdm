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

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单条权威主数据关联的子模型定义及子记录分页数据。
 */
@Data
public class MasterSubmodelDataRespVO {
    private List<Submodel> submodels = new ArrayList<>();
    /** 字符串传输 BIGINT，避免 JavaScript 丢失精度。 */
    private String selectedSubmodelId;
    private List<MasterColumnRespVO> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    private Long total = 0L;

    @Data
    public static class Submodel {
        /** 字符串传输 BIGINT，避免 JavaScript 丢失精度。 */
        private String id;
        private String code;
        private String name;
        private String instanceId;
        private Long recordCount;
        /** 是否已经登记可用的子模型物化实例。 */
        private Boolean materialized;
        /** 物化表结构是否满足子数据查询契约。 */
        private Boolean available;
        /** 未物化或结构不可用时的明确原因。 */
        private String message;
    }
}
