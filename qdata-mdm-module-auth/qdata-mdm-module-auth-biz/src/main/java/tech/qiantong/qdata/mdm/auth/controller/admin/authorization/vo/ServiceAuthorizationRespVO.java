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

package tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo;

import lombok.Data;
import tech.qiantong.qdata.mdm.auth.api.authorization.dto.scope.DataScopeConfigDTO;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationSubjectType;
import tech.qiantong.qdata.mdm.auth.api.enums.DataScopeType;

import java.util.Date;
import java.util.List;

/** 服务授权列表和详情返回对象。 */
@Data
public class ServiceAuthorizationRespVO {
    private Long id;
    private AuthorizationSubjectType subjectType;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    /** 授权主体自身的备注说明。 */
    private String subjectDescription;
    private Long serviceId;
    private String serviceCode;
    private String serviceName;
    private String serviceVersion;
    private DataScopeType dataScopeType;
    private DataScopeConfigDTO dataScopeConfig;
    private List<FieldPermissionVO> fieldPermissions;
    private Integer viewFieldCount;
    private Date effectiveTime;
    private Date expireTime;
    private Boolean longTerm;
    /** EFFECTIVE：有效，INACTIVE：未生效。 */
    private String displayStatus;
    /** REVOKED、NOT_STARTED、EXPIRED、INVALID；有效时为空。 */
    private String inactiveReason;
    private String persistedStatus;
    private Date revokeTime;
    private String revokeBy;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String remark;

    /** 服务授权的字段权限只包含查看，不返回数据授权专用的修改标记。 */
    @Data
    public static class FieldPermissionVO {
        private Long fieldId;
        private String fieldCode;
        private String fieldName;
        private Boolean view;
    }
}
