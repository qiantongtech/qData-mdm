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

package tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** 变更历史详情；字段快照由各模型决定。 */
@Data
public class AuditChangeTraceRespVO {
    private String id;
    private Long recordId;
    private String operateId;
    private Long modelId;
    private Long masterRecordId;
    private Long modelVersionId;
    private String model;
    private String code;
    private String name;
    private String businessCode;
    private String type;
    private String category;
    private String result;
    private String time;
    private String summary;
    /** 本次变更记录保存的操作人，未记录时保持空值。 */
    private Long operatorId;
    private String operatorName;
    /** 根据操作人ID关联的当前联系电话。 */
    private String operatorPhone;
    private List<Map<String, Object>> fields;
    private Map<String, Object> lifecycle;
}
