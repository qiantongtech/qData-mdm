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

package tech.qiantong.qdata.mdm.auth.api.dataauthorization.dto;

import lombok.Data;
import tech.qiantong.qdata.mdm.auth.api.authorization.dto.scope.DataScopeConfigDTO;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationOperation;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationSubjectType;
import tech.qiantong.qdata.mdm.auth.api.enums.DataScopeType;

import java.util.Date;
import java.util.List;

@Data
public class DataAuthorizationSaveReqDTO {
    private Long id;
    private AuthorizationSubjectType subjectType;
    private Long subjectId;
    private List<Long> subjectIds;
    private Long modelId;
    private String remark;
    private DataScopeType dataScopeType;
    private DataScopeConfigDTO dataScopeConfig;
    private Boolean includeChildOrg;
    private List<FieldPermissionDTO> fieldPermissions;
    private List<AuthorizationOperation> operations;
    private Date effectiveTime;
    private Date expireTime;
    private Boolean longTerm;

    @Data
    public static class FieldPermissionDTO {
        private Long fieldId;
        private String fieldCode;
        private Boolean view;
        private Boolean edit;
    }
}
