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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;
import lombok.Data;

/**
 * 接入任务字段映射实体，对应表 {@code INGEST_TASK_FIELD_MAPPING}。
 *
 * @author chentangwen
 */
@Data
@TableName(value = "INGEST_TASK_FIELD_MAPPING")
public class IngestTaskFieldMappingDO {

    /**
     * 字段映射 ID。
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 所属接入任务 ID。
     */
    @TableField(value = "TASK_ID")
    private Long taskId;

    /**
     * 来源字段名称。
     */
    @TableField(value = "SOURCE_FIELD_NAME")
    private String sourceFieldName;

    /**
     * 来源字段数据类型。
     */
    @TableField(value = "SOURCE_FIELD_TYPE")
    private String sourceFieldType;

    /**
     * 目标模型字段 ID；为空时该来源字段不写入模型。
     */
    @TableField(value = "TARGET_FIELD_ID")
    private Long targetFieldId;

    /**
     * 是否参与来源记录唯一标识：0 表示否，1 表示是。
     */
    @TableField(value = "UNIQUE_KEY_FLAG")
    private String uniqueKeyFlag;

    /**
     * 唯一标识字段的组合顺序，从 0 开始；不参与组合时为 -1。
     */
    @TableField(value = "UNIQUE_KEY_ORDER")
    private Integer uniqueKeyOrder;

    /**
     * 字段转换类型：0 不处理，1 类型转换，2 去空格，3 空值处理，4 固定值，5 值映射，6 日期处理。
     */
    @TableField(value = "TRANSFORM_TYPE")
    private String transformType;

    /**
     * 字段转换参数，使用 JSON 字符串保存。
     */
    @TableField(value = "TRANSFORM_CONFIG")
    private String transformConfig;

    /**
     * 字段映射处理顺序，从 0 开始。
     */
    @TableField(value = "SORT_ORDER")
    private Integer sortOrder;

    /**
     * 创建人名称。
     */
    @TableField(value = "CREATE_BY")
    private String createBy;

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
