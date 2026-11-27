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

package tech.qiantong.qdata.mdm.merge.service.detect;

import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectRunLogRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;

/**
 * 重复检测实时过程日志，Redis 暂存，执行结束时归档到原实例。
 *
 * @author chentangwen
 */
public interface IMergeDetectRunLogService {
    /**
     * 原子暂存阶段事件及进度；日志失败不得阻止实例收尾。
     */
    void publish(MergeDuplicateTaskInstanceDO instance, String level, String stage, String message);

    /**
     * 将完整日志和结束事件写入传入实例，由调用方与终态一起保存。
     */
    void archive(MergeDuplicateTaskInstanceDO instance, String level, String stage, String message);

    /**
     * 终态与日志已提交后清理实时缓存，失败时由缓存过期兜底。
     */
    void clear(Long instanceId);

    /**
     * 校验任务与实例归属，增量读取过程事件及最新进度。
     */
    MergeDetectRunLogRespVO read(Long taskId, Long instanceId, long afterSeq);
}
