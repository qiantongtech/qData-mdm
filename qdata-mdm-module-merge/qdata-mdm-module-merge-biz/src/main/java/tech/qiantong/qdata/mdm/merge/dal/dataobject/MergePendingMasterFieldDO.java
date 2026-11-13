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

package tech.qiantong.qdata.mdm.merge.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;


/**
 * 融合待提交结果字段
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
@TableName(value = "MERGE_PENDING_MASTER_FIELD")
@EqualsAndHashCode(callSuper = true)
public class MergePendingMasterFieldDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pendingId;
    private Long modelColumnId;
    private Long modelSubmodelColumnId;
    private String fieldPath;
    private String recommendedValue;
    private String finalValue;
    private String valueSourceType;
    private Long sourceVersionId;
    private Long survivorRuleId;
    private String conflictFlag;
    private String resolvedFlag;
    private String manualOverrideFlag;
    private String adjustReason;
    private String validFlag;

    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
