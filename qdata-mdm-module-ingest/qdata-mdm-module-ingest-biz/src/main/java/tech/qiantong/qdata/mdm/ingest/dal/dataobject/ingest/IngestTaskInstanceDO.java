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
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 接入任务运行实例，记录人工及定时执行结果。
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
     * 本次运行批次号。
     */
    private String batchNo;
    /**
     * 本次实际触发方式：MANUAL 人工或 SCHEDULED 定时。
     */
    private String triggerType;
    /**
     * 运行状态：1 运行中（含排队），2 失败，4 成功。
     */
    private String status;
    /**
     * 受理时初始化，工作线程进入执行阶段时更新为执行开始时间。
     */
    private Date startTime;
    /**
     * 终态时间；为空表示尚未记录结束。
     */
    private Date endTime;
    /**
     * 累计处理总数，收尾时包含读取器跳过的空记录。
     */
    private Long totalCount;
    /**
     * 已提交到目标业务库的新增或更新记录数。
     */
    private Long successCount;
    /**
     * 进入行处理后失败的记录数；读取或配置失败不计作行失败。
     */
    private Long failureCount;
    /**
     * 读取器跳过的空记录数。
     */
    private Long skipCount;
    /**
     * 数据库增量执行的起点，首次为配置起始值，后续为上次成功位置。
     */
    private String cursorBefore;
    /**
     * 数据库增量执行后已保存的位置，失败或无新数据时保持原值。
     */
    private String cursorAfter;
    /**
     * 长度受限的运行错误摘要，完整事件通过运行日志查询。
     */
    private String errorMessage;
    /**
     * 逻辑删除标志：0 正常，1 删除。
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
