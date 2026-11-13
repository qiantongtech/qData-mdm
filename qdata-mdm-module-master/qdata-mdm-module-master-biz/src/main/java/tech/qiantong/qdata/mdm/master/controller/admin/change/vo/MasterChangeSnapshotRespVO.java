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

package tech.qiantong.qdata.mdm.master.controller.admin.change.vo;

import lombok.Data;

import java.util.List;

/**
 * 变更前后业务快照原文；不使用当前模型定义重新解释。
 */
@Data
public class MasterChangeSnapshotRespVO {
    /** 字符串传输 BIGINT 变更 ID，避免 JavaScript 丢失精度。 */
    private String id;
    /** 变更前业务快照原文。 */
    private String beforeData;
    /** 变更后业务快照原文。 */
    private String afterData;
    /** 变更前快照是否因超过长度上限而截断。 */
    private Boolean beforeTruncated;
    /** 变更后快照是否因超过长度上限而截断。 */
    private Boolean afterTruncated;
    /** 是否成功按 JSON 对象计算字段差异。 */
    private Boolean diffAvailable;
    /** 字段级差异列表。 */
    private List<FieldDiff> diffs;

    /** 一个 JSON 字段路径的变更前后值。 */
    @Data
    public static class FieldDiff {
        /** JSON 字段路径。 */
        private String fieldPath;
        /** 变更前字段值。 */
        private String beforeValue;
        /** 变更后字段值。 */
        private String afterValue;
    }
}
