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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;
import lombok.Data;

/**
 * 接入任务 API 来源配置实体，对应表 {@code INGEST_TASK_API_CONFIG}。
 *
 * @author chentangwen
 */
@Data
@TableName(value = "INGEST_TASK_API_CONFIG")
public class IngestTaskApiConfigDO {

    /**
     * API 来源配置 ID。
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 所属接入任务 ID。
     */
    @TableField(value = "TASK_ID")
    private Long taskId;

    /**
     * 来源 API 请求地址。
     */
    @TableField(value = "URL")
    private String url;

    /**
     * HTTP 请求方法，支持 GET 和 POST。
     */
    @TableField(value = "HTTP_METHOD")
    private String httpMethod;

    /**
     * 请求头配置，使用 JSON 字符串保存。
     */
    @TableField(value = "HEADER_CONFIG")
    private String headerConfig;

    /**
     * 请求参数及响应读取规则的 JSON 配置。
     */
    @TableField(value = "PARAM_CONFIG")
    private String paramConfig;

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
