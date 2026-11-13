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

package tech.qiantong.qdata.mdm.quality.service.qualityTaskInstance;

import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;

import java.util.function.Consumer;

/**
 * 权威主数据检测执行入口。实现需按任务模型、检测范围以及当前有效规则检测数据。
 * 返回 [检测记录数, 通过记录数, 问题记录数]，按记录去重统计。
 */
public interface QualityTaskExecutor {
    /** 按记录回传全部失败规则，调用方负责在同一事务中保存问题及执行关联。 */
    long[] execute(QualityTaskDO task, Consumer<QualityIssueDO> issueConsumer);
}
