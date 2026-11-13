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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;
import lombok.Data;

@Data
@TableName(value = "INGEST_TASK_FIELD_MAPPING")
public class IngestTaskFieldMappingDO {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField(value = "TASK_ID")
    private Long taskId;

    @TableField(value = "SOURCE_FIELD_NAME")
    private String sourceFieldName;

    @TableField(value = "SOURCE_FIELD_TYPE")
    private String sourceFieldType;

    @TableField(value = "TARGET_FIELD_ID")
    private Long targetFieldId;

    @TableField(value = "UNIQUE_KEY_FLAG")
    private String uniqueKeyFlag;

    @TableField(value = "UNIQUE_KEY_ORDER")
    private Integer uniqueKeyOrder;

    @TableField(value = "TRANSFORM_TYPE")
    private String transformType;

    @TableField(value = "TRANSFORM_CONFIG")
    private String transformConfig;

    @TableField(value = "SORT_ORDER")
    private Integer sortOrder;

    @TableField(value = "CREATE_BY")
    private String createBy;

    @TableField(value = "CREATE_TIME")
    private Date createTime;

    @TableField(value = "UPDATE_BY")
    private String updateBy;

    @TableField(value = "UPDATE_TIME")
    private Date updateTime;

    @TableLogic(value = "0", delval = "1")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    @TableField(value = "REMARK")
    private String remark;
}
