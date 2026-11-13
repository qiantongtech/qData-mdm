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

package tech.qiantong.qdata.mdm.audit.dal.dataobject.audit;

import com.baomidou.mybatisplus.annotation.TableLogic;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/** 审计查询使用的变更申请明细状态。 */
@Data
@TableName("MASTER_CHANGE_REQUEST_ITEM")
public class AuditChangeRequestItemDO {
    @TableId private Long id;
    private Long requestId;
    private Integer itemNo;
    private Long masterRecordId;
    private Long resultMasterRecordId;
    private String executeStatus;
    private String changeType;
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
    private String targetStatus;
    private String beforeStatus;
    private String beforeData;
    private String executeMessage;
    private Date executeTime;
    private Date updateTime;
}
