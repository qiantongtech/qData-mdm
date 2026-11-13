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

package tech.qiantong.qdata.mdm.master.service.change;

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeSnapshotRespVO;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;

/**
 * MASTER_CHANGE 已生效业务变更历史的只读接口。
 */
public interface IMasterChangeService {

    /**
     * 查询单条主数据已生效的业务变更记录，不读取业务快照大字段。
     *
     * @param instanceId 可用模型实例 ID
     * @param masterDataId 权威主数据记录 ID
     * @param reqVO 分页及排序参数
     * @return 变更记录总数与当前页数据
     */
    PageResult<MasterChangeRespVO> getPage(Long instanceId, Long masterDataId,
                                           MasterChangePageReqVO reqVO);

    /**
     * 查询一条已生效变更的前后业务快照，并在可行时计算 JSON 字段差异。
     *
     * @param instanceId 可用模型实例 ID
     * @param masterDataId 权威主数据记录 ID
     * @param changeId 变更记录 ID
     * @return 原始快照、截断标志与字段差异
     */
    MasterChangeSnapshotRespVO getSnapshot(Long instanceId, Long masterDataId, Long changeId);
}
