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

package tech.qiantong.qdata.mdm.merge.service.manage;

import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterFieldDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

import java.util.List;
import java.util.Map;

/**
 * 合并结果质量检测服务，检测与质量快照写入属于同一次管理操作。
 *
 * @author chentangwen
 */
public interface IMergeManageQualityService {

    /**
     * 执行模型约束及质量规则并保存快照，调用方须持有结果写锁和事务。
     *
     * @param pending 已锁定的当前结果
     * @param fields 本次操作已加载的结果字段
     * @param columns 本次操作已加载的模型字段
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void check(MergePendingMasterDO pending, List<MergePendingMasterFieldDO> fields,
            Map<Long, ModelColumnDO> columns, Long operatorId, String operatorName);
}
