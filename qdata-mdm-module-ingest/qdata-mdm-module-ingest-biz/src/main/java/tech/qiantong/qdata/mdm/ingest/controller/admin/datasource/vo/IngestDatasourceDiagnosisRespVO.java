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

package tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Schema(description = "数据源诊断 Response VO")
@Data
public class IngestDatasourceDiagnosisRespVO {

    private String diagnosisId;
    private String datasourceType;
    private String diagnosisMode;
    private Boolean fullySupported;
    private String overallStatus;
    private String primaryErrorCode;
    private String message;
    private String rawError;
    private Long durationMs;
    private Date checkedAt;
    private Integer normalCount = 0;
    private Integer warningCount = 0;
    private Integer errorCount = 0;
    private Integer totalCount = 0;
    private List<Group> groups = new ArrayList<>();

    @Data
    public static class Item {
        private String checkCode;
        private String checkName;
        private String checkContent;
        private String status;
        /** 是否真实执行了该项检查；false表示因前置检查失败而直接判定未通过。 */
        private Boolean executed;
        private String message;
        private String impact;
        private List<String> suggestions = new ArrayList<>();
        private Long durationMs;
    }

    @Data
    public static class Group {
        private String groupCode;
        private String groupName;
        private String description;
        private String status;
        private Integer normalCount = 0;
        private Integer warningCount = 0;
        private Integer errorCount = 0;
        private Integer totalCount = 0;
        private List<Item> items = new ArrayList<>();
    }

}
