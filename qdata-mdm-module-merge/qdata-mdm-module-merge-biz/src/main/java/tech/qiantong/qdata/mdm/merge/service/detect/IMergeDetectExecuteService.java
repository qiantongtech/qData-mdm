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

import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectContextBO;

/**
 * 重复检测执行入口。
 *
 * @author chentangwen
 */
public interface IMergeDetectExecuteService {

    /**
     * 保存运行实例并在事务提交后异步检测，调用方必须在事务内持有任务行锁。
     *
     * @param task 已锁定的有效任务
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 运行实例编号
     */
    Long execute(MergeDuplicateTaskDO task, Long operatorId, String operatorName);

    /** 使用已校验的固定数据创建实例，事务提交后异步执行。 */
    Long executePrepared(MergeDuplicateTaskDO task, MergeDetectContextBO context,
                         Long operatorId, String operatorName);
}
