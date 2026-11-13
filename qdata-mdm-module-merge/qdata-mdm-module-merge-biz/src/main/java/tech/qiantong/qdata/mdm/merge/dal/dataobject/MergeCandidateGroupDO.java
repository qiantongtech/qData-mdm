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

import java.math.BigDecimal;
import java.util.Date;

/**
 * 重复检测候选组
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
@TableName(value = "MERGE_CANDIDATE_GROUP")
@EqualsAndHashCode(callSuper = true)
public class MergeCandidateGroupDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskInstanceId;
    private String groupNo;
    private Long modelId;
    private String type;
    private Date transferTime;
    private String status;
    private BigDecimal maxMatchScore;
    private Long parentGroupId;
    private String validFlag;

    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
