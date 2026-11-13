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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 执行事件按实例及序号幂等落库，不保存来源原始数据。
 */
@Data
@TableName("INGEST_TASK_INSTANCE_LOG")
public class IngestTaskInstanceLogDO {
    /**
     * 数据库日志主键，与实例内 eventSeq 的用途不同。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属运行实例 ID。
     */
    private Long taskInstanceId;

    /**
     * 实例内递增序号，用于归档去重与增量读取。
     */
    private Long eventSeq;

    /**
     * 日志级别。
     */
    private String logLevel;

    /**
     * 执行阶段。
     */
    private String stage;

    /**
     * 日志正文，不保存来源原始数据。
     */
    private String message;

    /**
     * 事件发生时间。
     */
    private Date logTime;
}
