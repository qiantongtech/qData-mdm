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
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationSubjectType;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.Date;
import java.util.List;

/** 数据授权分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataAuthorizationPageReqVO extends PageParam {
    private AuthorizationSubjectType subjectType;
    private Long subjectId;
    private Long modelId;
    /** 类目树选中域或目录下的模型范围。 */
    private List<Long> modelIds;
    /** EFFECTIVE：有效，INACTIVE：未生效。 */
    private String displayStatus;
    private Date createTimeStart;
    private Date createTimeEnd;
}
