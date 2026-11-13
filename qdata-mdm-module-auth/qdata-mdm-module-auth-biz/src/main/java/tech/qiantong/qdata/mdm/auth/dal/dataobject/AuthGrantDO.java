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

package tech.qiantong.qdata.mdm.auth.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/** 数据授权主记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("AUTH_GRANT")
public class AuthGrantDO extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String grantNo;
    private String grantName;
    private String subjectType;
    private Long subjectId;
    @TableField("INCLUDE_CHILD_ORG_FLAG")
    private String includeChildOrgFlag;
    private Long modelId;
    @TableField("DATA_SCOPE_TYPE")
    private String dataScopeType;
    @TableField("DATA_SCOPE_CONFIG")
    private String dataScopeConfig;
    @TableField("OPERATION_SCOPE")
    private String operationScope;
    @TableField("EFFECTIVE_TIME")
    private Date effectiveTime;
    @TableField("EXPIRE_TIME")
    private Date expireTime;
    private String status;
    private Date revokeTime;
    private String revokeBy;
    private String validFlag;
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
    private Long creatorId;
    private Long updaterId;
}
