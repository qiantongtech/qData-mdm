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

package tech.qiantong.qdata.mdm.model.service.base;

import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePublishRespVO;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseBatchReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseBatchPublishReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseBatchRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePublishReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelRelationGraphReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelRelationGraphRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;

import java.util.Collection;
import java.util.List;

/**
 * 主数据模型基本信息Service接口
 *
 * @author qdata
 * @date 2026-09-18
 */
public interface IModelBaseService extends IService<ModelBaseDO> {

    /** 按 ID 批量读取模型展示信息，包含当前版本、正式版本和归属位置。 */
    List<ModelBaseDO> getModelBaseListByIds(Collection<Long> modelIds);

    /**
     * 获得主数据模型基本信息分页列表
     *
     * @param pageReqVO 分页请求
     * @return 主数据模型基本信息分页列表
     */
    PageResult<ModelBaseDO> getModelBasePage(ModelBasePageReqVO pageReqVO);

    /**
     * 创建主数据模型基本信息
     *
     * @param createReqVO 主数据模型基本信息信息
     * @return 主数据模型基本信息编号
     */
    Long createModelBase(ModelBaseSaveReqVO createReqVO);

    /**
     * 更新主数据模型基本信息
     *
     * @param updateReqVO 主数据模型基本信息信息
     */
    int updateModelBase(ModelBaseSaveReqVO updateReqVO);

    /**
     * 校验主数据模型编码是否唯一
     */
    boolean checkCodeUnique(String code, Long id);

    /**
     * 发布模型当前版本，只有物化和正式保存全部成功才返回成功结果。
     *
     * @return 本次处理的实例ID、版本、执行及补偿状态
     */
    ModelBasePublishRespVO publishModelBase(Long modelId, ModelBasePublishReqVO publishReqVO, Long userId, String updateBy);

    /**
     * 批量发布模型，整批共用一个数据连接，逐项返回执行结果。
     */
    ModelBaseBatchRespVO batchPublishModelBase(ModelBaseBatchPublishReqVO request, Long userId, String userName);

    /**
     * 批量停用已发布的模型，复用单模型停用流程。
     */
    ModelBaseBatchRespVO batchDisableModelBase(ModelBaseBatchReqVO request, Long userId, String userName);

    /**
     * 批量删除草稿模型，复用单模型的级联逻辑删除。
     */
    ModelBaseBatchRespVO batchRemoveModelBase(ModelBaseBatchReqVO request);


    /**
     * 停用模型，将当前版本置为封存状态。
     */
    int disableModelBase(Long modelId, Long userId, String updateBy);

    /**
     * 删除主数据模型基本信息
     *
     * @param idList 主数据模型基本信息编号
     */
    int removeModelBase(Collection<Long> idList);

    /**
     * 获得主数据模型基本信息详情
     *
     * @param id 主数据模型基本信息编号
     * @return 主数据模型基本信息
     */
    ModelBaseDO getModelBaseById(Long id);

    /**
     * 查询模型关系图谱。
     * 传入 ID 时从指定模型开始查询；未传 ID 时按查询条件选择起始模型。
     *
     * @param reqVO 模型及关系类型查询条件
     * @return 已补齐节点展示信息和关系边的图谱
     */
    ModelRelationGraphRespVO getModelRelationGraph(ModelRelationGraphReqVO reqVO);

    /**
     * 获得全部主数据模型基本信息列表
     *
     * @return 主数据模型基本信息列表
     */
    List<ModelBaseDO> getModelBaseList();
}
