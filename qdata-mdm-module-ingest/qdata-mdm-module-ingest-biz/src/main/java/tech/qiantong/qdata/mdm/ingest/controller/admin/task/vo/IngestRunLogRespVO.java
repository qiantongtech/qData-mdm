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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import lombok.Data;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceLogDO;

import java.util.List;

/**
 * 接入任务运行日志增量查询响应对象。
 *
 * @author chentangwen
 */
@Data
public class IngestRunLogRespVO {

    /**
     * 当前运行进度，包含尚未归档的实时快照。
     */
    private IngestTaskRunRespVO run;

    /**
     * 按事件序号升序排列的本页日志。
     */
    private List<IngestTaskInstanceLogDO> logs;

    /**
     * 下次增量读取使用的游标。
     */
    private long nextSeq;

    /**
     * 当前可见日志的最大序号，用于固定下载范围。
     */
    private long lastSeq;

    /**
     * 当前游标后是否仍有待读取日志。
     */
    private boolean hasMore;
}
