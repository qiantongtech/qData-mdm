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

package tech.qiantong.qdata.mdm.dist.service.task;

import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceRespVO;

/**
 * 分发任务执行服务。
 * 负责人工触发一次分发并返回执行结果。
 */
public interface IDistTaskExecutionService {

    /**
     * 人工执行指定的分发任务一次。
     *
     * @param taskId 分发任务 ID
     * @param operatorId 操作人 ID
     * @param operator 操作人姓名
     * @return 本次执行结果
     */
    DistTaskInstanceRespVO execute(Long taskId, Long operatorId, String operator);
}
