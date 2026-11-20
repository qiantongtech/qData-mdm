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
 * Business License: https://qdata.qiantong.tech/business/policy.html
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
 * 来源系统与数据库连接关联实体，对应表 {@code INGEST_SYSTEM_DATASOURCE_REL}。
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "INGEST_SYSTEM_DATASOURCE_REL")
public class IngestSourceDatasourceRelDO {

    /**
     * 有效状态标识。
     */
    public static final String VALID_FLAG_YES = "1";

    /**
     * 关联关系 ID。
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 来源系统 ID。
     */
    @TableField(value = "SOURCE_SYSTEM_ID")
    private Long sourceId;

    /**
     * 数据库连接 ID。
     */
    @TableField(value = "DATASOURCE_ID")
    private Long datasourceId;

    /**
     * 有效标志，1 表示有效。
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
