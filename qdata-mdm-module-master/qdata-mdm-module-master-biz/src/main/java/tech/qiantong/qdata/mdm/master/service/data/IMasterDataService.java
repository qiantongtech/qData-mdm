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

import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSourceSystemRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataDetailRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterTreeNodeRespVO;

import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 动态主数据查询业务接口。
 */
public interface IMasterDataService {

    /**
     * 查询主数据或子模型分页数据。
     *
     * @param instanceId 模型物化实例 ID
     * @param req 查询参数
     * @return 当前模型版本的分页记录和统计信息
     */
    MasterDataRespVO getData(Long instanceId, MasterDataPageReqVO req);

    /**
     * 读取时间节点之后的全部匹配主数据，不分页，不补充管理页面统计、审批和创建人信息。
     *
     * @param instanceId 当前成功发布实例ID
     * @param modelId 主模型ID
     * @param modelVersionId 当前成功发布版本ID
     * @param updatedAfter 已由公共API校验的非空时间下界；更新时间为空时使用创建时间
     * @param status 已由公共API校验的生命周期状态，不传或空字符串时包含全部状态，不包含逻辑删除记录
     * @return 当前值的平铺记录，不含子模型明细
     */
    List<Map<String, Object>> getSyncData(Long instanceId, Long modelId, Long modelVersionId, Date updatedAfter, String status);

    /** 查询分类模型完整筛选结果，供树形表格按 ID/CATEGORY_ID 组树，不进行行分页。 */
    MasterDataRespVO getCategoryData(Long instanceId, MasterDataPageReqVO req);

    /** 只查询当前已发布模型关联的来源系统，不读取业务物理表。 */
    List<MasterSourceSystemRespVO> getSourceSystems(Long instanceId, Long modelId);

    /**
     * 查询正式主数据详情，并校验记录所属的物化实例。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param modelId 当前页面的主模型ID，直接定位并核对发布实例
     * @return 主数据详情及字段定义
     */
    MasterDataDetailRespVO getDetail(Long instanceId, Long masterDataId, Long modelId);

    /**
     * 查询业务记录并校验物化实例归属，不加载页面扩展信息。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @return 动态字段值，日期和 ID 格式与现有维护快照保持一致
     */
    Map<String, Object> getRecord(Long instanceId, Long masterDataId);

    /**
     * 查询分类模型树。
     *
     * @param instanceId 模型物化实例 ID
     * @return 查询结果集合
     */
    List<MasterTreeNodeRespVO> getTree(Long instanceId);

    /**
     * 查询分类节点及其后代主数据 ID。
     *
     * @param instanceId 模型物化实例 ID
     * @param treeId 分类节点 ID
     * @return 查询结果集合
     */
    Set<Long> getCategoryDescendantIds(Long instanceId, String treeId);
}
