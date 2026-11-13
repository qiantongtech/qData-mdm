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

package tech.qiantong.qdata.mdm.model.service.project;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;

import java.util.Collection;
import java.util.List;

/**
 * 主数据域Service接口
 *
 * @author qdata
 * @date 2026-09-15
 */
public interface IModelProjectService extends IService<ModelProjectDO> {

    /**
     * 获得主数据域分页列表
     *
     * @param pageReqVO 分页请求
     * @return 主数据域分页列表
     */
    PageResult<ModelProjectDO> getModelProjectPage(ModelProjectPageReqVO pageReqVO);

    /**
     * 创建主数据域
     *
     * @param createReqVO 主数据域信息
     * @return 主数据域编号
     */
    Long createModelProject(ModelProjectSaveReqVO createReqVO);

    /**
     * 更新主数据域
     *
     * @param updateReqVO 主数据域信息
     */
    int updateModelProject(ModelProjectSaveReqVO updateReqVO);

    /**
     * 校验编码类型称是否唯一
     */
    boolean checkCodeUnique(String code, Long id);

    /**
     * 更新主数据域状态
     *
     * @param id     主数据域编号
     * @param status 状态
     */
    int updateModelProjectStatus(Long id, Boolean status);

    /**
     * 删除主数据域
     *
     * @param idList 主数据域编号
     */
    int removeModelProject(Collection<Long> idList);

    /**
     * 批量校验主数据域是否被模型目录引用
     */
    boolean hasLayerReference(Collection<Long> ids);

    /**
     * 批量校验主数据域是否被主数据模型引用
     */
    boolean hasModelReference(Collection<Long> ids);

    /**
     * 获得主数据域详情
     *
     * @param id 主数据域编号
     * @return 主数据域
     */
    ModelProjectDO getModelProjectById(Long id);

    /**
     * 获得全部主数据域列表
     *
     * @return 主数据域列表
     */
    List<ModelProjectDO> getModelProjectList();
}
