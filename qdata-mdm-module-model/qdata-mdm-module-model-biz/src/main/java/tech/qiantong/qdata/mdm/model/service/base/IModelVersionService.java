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

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelVersionDO;

/**
 * 主数据模型版本Service接口
 *
 * @author qdata
 * @date 2026-09-18
 */
public interface IModelVersionService extends IService<ModelVersionDO> {


    /**
     * 获得主数据模型版本分页列表
     *
     * @param pageReqVO 分页请求
     * @return 主数据模型版本分页列表
     */
    PageResult<ModelVersionDO> getModelVersionPage(ModelVersionPageReqVO pageReqVO);

    /**
     * 获得主数据模型版本详情
     *
     * @param id 主数据模型版本编号
     * @return 主数据模型版本
     */
    ModelVersionDO getModelVersionById(Long id);
}
