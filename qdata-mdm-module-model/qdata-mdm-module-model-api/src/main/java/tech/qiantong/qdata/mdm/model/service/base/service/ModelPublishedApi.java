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

package tech.qiantong.qdata.mdm.model.service.base.service;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.base.dto.*;

/**
 * 供其他模块查询已发布模型的公共接口，调用方只需依赖model-api。
 * 返回最近一次成功发布的内容，不返回当前编辑草稿。
 */
public interface ModelPublishedApi {

    /**
     * 分页查询当前可用的已发布主模型，筛选条件按成功发布内容判断。
     *
     * @param request 模型筛选及分页参数，页码必须大于0，pageSize为-1时查询全部，否则必须大于0；固定按主模型ID升序
     * @return 基础信息和成功版本，不含字段、子模型或物化信息
     */
    PageResult<ModelPublishedRespDTO> getPublishedModelPage(ModelPublishedPageReqDTO request);

    /**
     * 根据主模型ID查询最近一次成功发布的基础信息。
     *
     * @param modelId 主模型ID
     * @return 成功发布的基础信息和版本；模型已停用、删除或没有成功记录时抛出业务异常
     */
    ModelPublishedRespDTO getPublishedModel(Long modelId);

    /** 按当前成功实例定位主模型ID，不读取快照或物化信息。 */
    Long getPublishedModelId(Long instanceId);

    /** 仅核对当前可用的成功实例和版本，不读取快照、执行计划或域名称。 */
    boolean isPublishedInstance(Long modelId, Long instanceId, Long versionId);

    /**
     * 查询同一次成功发布中的字段、直接子模型及字段引用的参考数据、编码方案。
     *
     * @param modelId 主模型ID
     * @return 成功发布的正常使用配置及实例/版本标识，不含物化信息或规则配置
     */
    ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId);

    /** 在同一次结构查询中按需附带权威主表及列绑定，不加载完整执行计划。 */
    ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId, boolean includeAuthorityTable);

    /**
     * 查询最近成功实例保存的目标数据库和来源、权威主子表完整物化信息。
     *
     * @param modelId 主模型ID
     * @return 成功模型及完整表、字段、索引记录，含停用标记，不含连接凭证
     */
    ModelPublishedMaterializationRespDTO getPublishedModelMaterialization(Long modelId);

    /**
     * 查询成功发布时保存的本主模型关联规则，不查询当前规则配置。
     *
     * @param modelId      主模型ID
     * @param ruleCategory MATCH匹配、SURVIVOR生存、QUALITY质量
     * @return 对应成功版本及指定类别正常启用的规则
     */
    ModelPublishedRulesRespDTO getPublishedModelRules(Long modelId, String ruleCategory);
}
