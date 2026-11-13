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

package tech.qiantong.qdata.mdm.model.service.base.impl;

import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.base.dto.*;
import tech.qiantong.qdata.mdm.model.service.base.IModelPublishService;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import javax.annotation.Resource;

/**
 * 模块间公共查询的实现，与HTTP查询复用同一套成功记录读取逻辑。
 */
@Service
public class ModelPublishedApiImpl implements ModelPublishedApi {

    /**
     * 向其他模块提供成功发布模型的统一查询能力。
     */
    @Resource
    private IModelPublishService modelPublishService;

    /**
     * 查询成功模型列表，不在API层重复筛选或读取草稿。
     */
    @Override
    public PageResult<ModelPublishedRespDTO> getPublishedModelPage(ModelPublishedPageReqDTO request) {
        return modelPublishService.getPublishedModelPage(request);
    }

    /**
     * 查询成功基础信息，原查询服务的不可用和配置错误提示原样保留。
     */
    @Override
    public ModelPublishedRespDTO getPublishedModel(Long modelId) {
        return modelPublishService.getPublishedModel(modelId);
    }

    @Override
    public Long getPublishedModelId(Long instanceId) {
        return modelPublishService.getPublishedModelId(instanceId);
    }

    @Override
    public boolean isPublishedInstance(Long modelId, Long instanceId, Long versionId) {
        return modelPublishService.isPublishedInstance(modelId, instanceId, versionId);
    }

    /**
     * 与HTTP查询共用成功配置读取和停用过滤，不重新查当前字段或引用配置。
     */
    @Override
    public ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId) {
        return modelPublishService.getPublishedModelStructure(modelId);
    }

    @Override
    public ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId, boolean includeAuthorityTable) {
        return modelPublishService.getPublishedModelStructure(modelId, includeAuthorityTable);
    }

    /**
     * 复用成功物化记录查询，不在公共API连接业务库。
     */
    @Override
    public ModelPublishedMaterializationRespDTO getPublishedModelMaterialization(Long modelId) {
        return modelPublishService.getPublishedModelMaterialization(modelId);
    }

    /**
     * 规则仍从同一成功配置读取，保持HTTP和模块调用结果一致。
     */
    @Override
    public ModelPublishedRulesRespDTO getPublishedModelRules(Long modelId, String ruleCategory) {
        return modelPublishService.getPublishedModelRules(modelId, ruleCategory);
    }
}
