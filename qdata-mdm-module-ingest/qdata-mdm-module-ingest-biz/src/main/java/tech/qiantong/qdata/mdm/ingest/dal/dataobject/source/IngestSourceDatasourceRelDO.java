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
 * 来源系统与数据连接关系 DO
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

    public static final String VALID_FLAG_YES = "1";

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField(value = "SOURCE_SYSTEM_ID")
    private Long sourceId;

    @TableField(value = "DATASOURCE_ID")
    private Long datasourceId;

    @TableField(value = "VALID_FLAG")
    private String validFlag;

    @TableField(value = "CREATE_BY")
    private String createBy;

    @TableField(value = "CREATOR_ID")
    private Long creatorId;

    @TableField(value = "CREATE_TIME")
    private Date createTime;

    @TableField(value = "UPDATE_BY")
    private String updateBy;

    @TableField(value = "UPDATER_ID")
    private Long updaterId;

    @TableField(value = "UPDATE_TIME")
    private Date updateTime;

    @TableLogic(value = "0", delval = "1")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    @TableField(value = "REMARK")
    private String remark;
}
