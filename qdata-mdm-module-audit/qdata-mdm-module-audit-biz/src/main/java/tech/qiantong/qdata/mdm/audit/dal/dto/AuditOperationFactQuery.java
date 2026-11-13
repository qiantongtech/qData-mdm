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

package tech.qiantong.qdata.mdm.audit.dal.dto;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.Data;

/** 操作事件查询条件；Mapper 只接收已解析的条件，不解释业务编码。 */
@Data
public class AuditOperationFactQuery {
    private String modelName;
    private String modelCode;
    private List<Long> modelIds = Collections.emptyList();
    private List<String> types = Collections.emptyList();
    private String operatorName;
    private Long recordId;
    private Date beginTime;
    private Date endTime;
    private String orderBy;
    private boolean ascending;
}
