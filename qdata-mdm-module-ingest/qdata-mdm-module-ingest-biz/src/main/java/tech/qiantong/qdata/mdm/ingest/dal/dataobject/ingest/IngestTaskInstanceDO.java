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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 接入任务运行实例实体，对应表 {@code INGEST_TASK_INSTANCE}。
 *
 * @author chentangwen
 */
@Data
@TableName("INGEST_TASK_INSTANCE")
public class IngestTaskInstanceDO {

    /**
     * 运行实例 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属接入任务 ID。
     */
    private Long taskId;

    /**
     * 本次运行的批次号。
     */
    private String batchNo;

    /**
     * 实际触发方式：MANUAL 表示人工执行，SCHEDULED 表示定时执行。
     */
    private String triggerType;

    /**
     * 运行状态：1 表示运行中（含排队），2 表示失败，4 表示成功。
     */
    private String status;

    /**
     * 运行开始时间，受理时初始化，进入执行阶段时更新。
     */
    private Date startTime;

    /**
     * 运行结束时间。
     */
    private Date endTime;

    /**
     * 累计处理记录总数，包含读取器跳过的空记录。
     */
    private Long totalCount;

    /**
     * 已成功提交至目标业务库的新增或更新记录数。
     */
    private Long successCount;

    /**
     * 行处理失败的记录数。
     */
    private Long failureCount;

    /**
     * 读取器跳过的空记录数。
     */
    private Long skipCount;

    /**
     * 本次数据库增量读取的起始位置。
     */
    private String cursorBefore;

    /**
     * 本次数据库增量读取后保存的位置。
     */
    private String cursorAfter;

    /**
     * 运行错误摘要。
     */
    private String errorMessage;

    /**
     * 逻辑删除标志：0 表示未删除，1 表示已删除。
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
