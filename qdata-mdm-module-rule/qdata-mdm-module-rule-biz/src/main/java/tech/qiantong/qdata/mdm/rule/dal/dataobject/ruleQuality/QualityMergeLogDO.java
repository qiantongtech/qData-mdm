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

package tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 关联质量规则统计时使用的质量检测快照只读字段。 */
@Data
@TableName("MERGE_QUALITY_LOG")
public class QualityMergeLogDO {
    @TableId("ID") private Long id;
    @TableField("PENDING_MASTER_ID") private Long pendingMasterId;
    @TableField("RULE_SNAPSHOT") private String ruleSnapshot;
    @TableField("VALID_FLAG") private String validFlag;
    @TableField("DEL_FLAG") private String deleted;
}
