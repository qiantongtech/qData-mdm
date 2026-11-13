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

package tech.qiantong.qdata.mdm.master.service.data;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionSnapshotRespVO;

import java.util.List;
import java.util.Map;

/**
 * MASTER_DATA_VERSION 当前版本、历史列表及快照查询。
 */
public interface IMasterDataVersionService {

    /**
     * 查询单条主数据当前有效、未删除的版本 ID。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @return 当前版本 ID；无匹配时为 null
     * @throws ServiceException 存在多个有效当前版本时
     */
    Long getCurrentVersionId(Long modelId, Long masterRecordId);

    /**
     * 批量补充当前有效版本的展示编号；对记录 ID 去重，并按每批最多 500 个查询。
     *
     * @param modelId 主数据模型 ID
     * @param recordIds 主数据记录 ID 列表
     * @return 记录 ID 到版本展示编号的映射；无匹配时不包含该记录
     * @throws ServiceException 同一记录存在多个当前版本时
     */
    Map<Long, String> getCurrentVersionNumbers(Long modelId, List<Long> recordIds);

    /**
     * 分页查询历史版本并校验记录归属。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 分页及排序参数，排序字段仅允许业务白名单字段
     * @return 主数据历史版本分页列表
     */
    PageResult<MasterDataVersionRespVO> getPage(Long instanceId, Long masterDataId,
                                                MasterDataVersionPageReqVO reqVO);

    /**
     * 查询指定历史版本快照并校验记录归属。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param versionId 主数据版本 ID
     * @return 指定版本的数据快照及截断标志
     */
    MasterDataVersionSnapshotRespVO getSnapshot(Long instanceId, Long masterDataId, Long versionId);
}
