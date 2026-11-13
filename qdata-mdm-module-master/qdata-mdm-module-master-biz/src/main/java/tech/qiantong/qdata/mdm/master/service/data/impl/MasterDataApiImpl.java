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

package tech.qiantong.qdata.mdm.master.service.data.impl;

import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataRespVO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDataSyncReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDataChangePageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDataChangePageRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataService;
import tech.qiantong.qdata.mdm.master.service.data.service.MasterDataApi;
import tech.qiantong.qdata.mdm.master.util.MasterPublishedModelUtils;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 模块间主数据增量同步的实现，复用权威数据绑定和字段值转换，不加载管理页面扩展信息。
 */
@Service
public class MasterDataApiImpl implements MasterDataApi {
    /**
     * 查询当前成功发布的模型，定位实例并核对调用方保存的版本。
     */
    @Resource
    private ModelPublishedApi modelPublishedApi;

    /**
     * 复用权威主数据的增量读取、状态过滤及字段值转换能力。
     */
    @Resource
    private IMasterDataService masterDataService;

    /**
     * 读取时间节点之后的全部匹配主数据，不在API层拼接SQL或查询数据源配置。
     *
     * @param request 模型、成功发布版本及同步时间下界
     * @return 全部匹配的动态主数据记录
     */
    @Override
    public List<Map<String, Object>> getMasterDataSyncList(MasterDataSyncReqDTO request) {
        validateRequest(request);
        Date updatedAfter = new Date(request.getUpdatedAfter().getTime());
        ModelPublishedRespDTO published = modelPublishedApi.getPublishedModel(request.getModelId());
        MasterPublishedModelUtils.requirePublication(published, request.getModelId(), null, request.getModelVersionId());

        return masterDataService.getSyncData(published.getInstanceId(), request.getModelId(),
                request.getModelVersionId(), updatedAfter, request.getStatus());
    }

    /**
     * 增量取数契约占位。主数据模块实现快照游标查询前明确报错，避免分发侧误报成功 0 条。
     */
    @Override
    public MasterDataChangePageRespDTO getMasterDataChangePage(MasterDataChangePageReqDTO request) {
        throw new ServiceException("主数据增量查询接口尚未实现");
    }

    /**
     * 校验模块间调用的必填标识和时间，避免绕过HTTP参数校验后误读取全表。
     */
    private void validateRequest(MasterDataSyncReqDTO request) {
        if (request == null) throw new ServiceException("增量同步参数不能为空");
        if (request.getModelId() == null || request.getModelId() <= 0)
            throw new ServiceException("模型ID必须为正数");
        if (request.getModelVersionId() == null || request.getModelVersionId() <= 0)
            throw new ServiceException("模型发布版本ID必须为正数");
        if (request.getUpdatedAfter() == null) throw new ServiceException("同步时间节点不能为空");
        if (request.getStatus() != null && !request.getStatus().isEmpty()
                && !request.getStatus().matches("[1-4]")) throw new ServiceException("主数据状态无效");
    }
}
