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

package tech.qiantong.qdata.mdm.model.service.layer;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;

import java.util.Collection;
import java.util.List;

/**
 * 模型目录层级Service接口
 *
 * @author qdata
 * @date 2026-09-15
 */
public interface IModelLayerService extends IService<ModelLayerDO> {

    /**
     * 获得模型目录层级分页列表
     *
     * @param pageReqVO 分页请求
     * @return 模型目录层级分页列表
     */
    PageResult<ModelLayerDO> getModelLayerPage(ModelLayerPageReqVO pageReqVO);

    /**
     * 创建模型目录层级
     *
     * @param createReqVO 模型目录层级信息
     * @return 模型目录层级编号
     */
    Long createModelLayer(ModelLayerSaveReqVO createReqVO);

    /**
     * 更新模型目录层级
     *
     * @param updateReqVO 模型目录层级信息
     */
    int updateModelLayer(ModelLayerSaveReqVO updateReqVO);

    /**
     * 校验模型目录编码是否唯一
     */
    boolean checkCodeUnique(String code, Long id);

    /**
     * 批量校验模型目录是否被主数据模型引用
     */
    boolean hasModelReference(Collection<Long> ids);

    /**
     * 更新模型目录层级状态
     *
     * @param id     模型目录层级编号
     * @param status 状态
     */
    int updateModelLayerStatus(Long id, Boolean status);

    /**
     * 删除模型目录层级
     *
     * @param id 模型目录层级编号
     */
    int removeModelLayer(Long id);

    /**
     * 获得模型目录层级详情
     *
     * @param id 模型目录层级编号
     * @return 模型目录层级
     */
    ModelLayerDO getModelLayerById(Long id);

    /**
     * 获得全部模型目录层级列表
     *
     * @return 模型目录层级列表
     */
    List<ModelLayerDO> getModelLayerList();

    /**
     * 批量校验模型目录是否存在子目录
     */
    boolean hasChildByModelLayerId(Collection<Long> ids);


}
