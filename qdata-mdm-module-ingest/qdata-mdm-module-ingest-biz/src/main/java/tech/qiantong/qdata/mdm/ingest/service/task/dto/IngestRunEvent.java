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

package tech.qiantong.qdata.mdm.ingest.service.task.dto;

import lombok.Data;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceLogDO;

/**
 * Redis 事件同时携带当时进度，批次重试不会回退到更旧的统计值。
 */
@Data
public class IngestRunEvent {
    /**
     * 事件发布时的运行状态和累计统计，与日志一同序列化以保持一致。
     */
    private IngestTaskInstanceDO snapshot;
    /**
     * 本次执行事件；Redis 独立序号在解码后补入 eventSeq。
     */
    private IngestTaskInstanceLogDO log;
}
