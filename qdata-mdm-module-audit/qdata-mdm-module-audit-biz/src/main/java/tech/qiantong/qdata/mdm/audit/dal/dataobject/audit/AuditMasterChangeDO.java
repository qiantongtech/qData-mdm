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

/** 审计查询使用的主数据变更状态。 */
@Data
@TableName("MASTER_CHANGE")
public class AuditMasterChangeDO {
    @TableId private Long id;
    private Long modelId;
    private Long masterRecordId;
    private Long requestItemId;
    private Long modelVersionId;
    private Long afterVersionId;
    private String afterVersionNo;
    private Long beforeVersionId;
    private String beforeVersionNo;
    private String type;
    private String beforeStatus;
    private String beforeData;
    private String proposedData;
    private String afterData;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private String processResult;
    private String afterStatus;
    private Date changeTime;
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
