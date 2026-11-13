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

package tech.qiantong.qdata.mdm.master.service.data.service;

import tech.qiantong.qdata.mdm.master.data.dto.MasterDataSyncReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDataChangePageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDataChangePageRespDTO;

import java.util.List;
import java.util.Map;

/**
 * 供其他模块查询已发布模型的权威主数据，调用方只需依赖master-api。
 * 返回正式记录的当前值，模型和物化绑定以成功发布内容为准，不返回逻辑删除记录。
 */
public interface MasterDataApi {

    /**
     * 按时间节点读取更新后的全部匹配主数据，普通和分类模型均返回平铺记录，不分页。
     * 状态由调用方指定，不传时包含生效、冻结、失效和归档；接收方须按状态决定是否可用。
     * 同一条记录只返回当前值，不返回节点之后的每次历史变更，也不包含子模型明细。
     *
     * @param request 模型ID、成功发布版本ID、必填的同步时间下界及可选状态，更新时间严格大于该时间
     * @return 全部匹配的动态字段记录，无数据时返回空集合；模型不可用或版本不一致时抛出业务异常
     */
    List<Map<String, Object>> getMasterDataSyncList(MasterDataSyncReqDTO request);

    /**
     * 按固定快照和游标分页读取主数据变更，供分发任务执行增量推送。
     *
     * @param request 模型、发布版本、起始时间和游标
     * @return 当前页记录和下一页游标
     */
    MasterDataChangePageRespDTO getMasterDataChangePage(MasterDataChangePageReqDTO request);
}
