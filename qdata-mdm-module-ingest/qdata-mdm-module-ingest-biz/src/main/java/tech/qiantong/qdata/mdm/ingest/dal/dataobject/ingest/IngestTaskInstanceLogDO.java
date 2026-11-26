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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 接入任务运行日志实体，对应表 {@code INGEST_TASK_INSTANCE_LOG}。
 *
 * @author chentangwen
 */
@Data
@TableName("INGEST_TASK_INSTANCE_LOG")
public class IngestTaskInstanceLogDO {

    /**
     * 运行日志主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属运行实例 ID。
     */
    private Long taskInstanceId;

    /**
     * 运行实例内递增的事件序号，用于日志归档去重和增量读取。
     */
    private Long eventSeq;

    /**
     * 日志级别。
     */
    private String logLevel;

    /**
     * 日志所属执行阶段。
     */
    private String stage;

    /**
     * 日志内容。
     */
    private String message;

    /**
     * 日志事件发生时间。
     */
    private Date logTime;
}
