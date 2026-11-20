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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.source;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 来源系统实体，对应表 {@code INGEST_SOURCE_SYSTEM}。
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "INGEST_SOURCE_SYSTEM")
public class IngestSourceDO {

    /**
     * 来源系统启用状态标识。
     */
    public static final String VALID_FLAG_YES = "1";
    public static final String VALID_FLAG_NO = "0";


    /**
     * 来源可信度默认值。
     */
    public static final int TRUST_SCORE_DEFAULT = 50;

    /**
     * 来源可信度最小值。
     */
    public static final int TRUST_SCORE_MIN = 0;

    /**
     * 来源可信度最大值。
     */
    public static final int TRUST_SCORE_MAX = 100;

    /**
     * 来源系统 ID。
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 来源系统编码。
     */
    @TableField(value = "CODE")
    private String code;

    /**
     * 来源系统名称。
     */
    @TableField(value = "NAME", keepGlobalFormat = true)
    private String name;

    /**
     * 来源系统图标路径。
     */
    @TableField(value = "ICON")
    private String icon;

    /**
     * 来源系统责任人用户 ID。
     */
    @TableField(value = "RESPONSIBLE_USER_ID")
    private Long responsibleUserId;

    /**
     * 来源系统业务说明。
     */
    @TableField(value = "DESCRIPTION")
    private String description;

    /**
     * 来源可信度，取值范围为 0～100，默认值为 50。
     */
    @TableField(value = "TRUST_SCORE")
    private Integer trustScore;

    /**
     * 来源系统状态：0 表示停用，1 表示启用。
     */
    @TableField(value = "VALID_FLAG")
    private String validFlag;

    /**
     * 创建人名称。
     */
    @TableField(value = "CREATE_BY")
    private String createBy;

    /**
     * 创建人用户 ID。
     */
    @TableField(value = "CREATOR_ID")
    private Long creatorId;

    /**
     * 创建时间。
     */
    @TableField(value = "CREATE_TIME")
    private Date createTime;

    /**
     * 更新人名称。
     */
    @TableField(value = "UPDATE_BY")
    private String updateBy;

    /**
     * 更新人用户 ID。
     */
    @TableField(value = "UPDATER_ID")
    private Long updaterId;

    /**
     * 最近更新时间。
     */
    @TableField(value = "UPDATE_TIME")
    private Date updateTime;

    /**
     * 逻辑删除标志：0 表示未删除，1 表示已删除。
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    /**
     * 备注信息。
     */
    @TableField(value = "REMARK")
    private String remark;
}
