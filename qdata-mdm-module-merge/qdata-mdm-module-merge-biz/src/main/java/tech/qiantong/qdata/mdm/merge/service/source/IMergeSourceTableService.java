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

package tech.qiantong.qdata.mdm.merge.service.source;

import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import java.util.Collection;
import java.util.List;

/** 融合来源实体行读取与治理状态维护。 */
public interface IMergeSourceTableService {
    /**
     * 读取模型已发布来源实体表中的记录。
     * @param modelId 所属主数据模型编号
     * @param ids 为 null 时读取每个来源的最新版本；空集合返回空列表，其余读取指定实体行版本
     * @return 未删除的来源记录
     */
    List<MergeSourceRecordBO> load(Long modelId, Collection<Long> ids);
    /**
     * 在当前写事务使用的目标连接中锁定指定实体行。
     * @param modelId 所属主数据模型编号
     * @param id 来源实体行主键，即固定采集版本编号
     * @return 来源记录，不存在或已删除时返回 null
     */
    MergeSourceRecordBO lock(Long modelId, Long id);
    /**
     * 按预期治理状态批量变更实体行。
     * @param modelId 所属主数据模型编号
     * @param ids 非空集合对象；空集合不执行更新
     * @param expected 更新前必须满足的治理状态
     * @param status 目标治理状态
     */
    void transition(Long modelId, Collection<Long> ids, String expected, String status);
}
