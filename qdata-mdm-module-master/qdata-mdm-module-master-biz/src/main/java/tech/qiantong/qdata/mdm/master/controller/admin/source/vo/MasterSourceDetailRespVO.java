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

package tech.qiantong.qdata.mdm.master.controller.admin.source.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 单条权威主数据的当前有效来源血缘。所有 BIGINT ID 均以字符串传输。
 */
@Data
public class MasterSourceDetailRespVO {
    private Long total;
    private List<SourceRecord> records;
    private List<FieldSource> fieldSources;

    /** 有效来源关系及关联采集版本的展示行；其中的 BIGINT ID 均为字符串。 */
    @Data
    public static class SourceRecord {
        private String relationId;
        private String sourceRecordId;
        private String sourceVersionId;
        private String sourceSystemName;
        private String sourceRecordKey;
        private String stableRecordId;
        private String relationType;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date effectiveTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date lastReceiveTime;
        private String modelData;
    }

    /** 当前主数据版本中一条有效的字段来源；来源 BIGINT ID 使用字符串。 */
    @Data
    public static class FieldSource {
        private String fieldPath;
        private String sourceFieldPath;
        private String sourceSystemName;
        private String sourceRecordId;
        private String sourceVersionId;
    }
}
