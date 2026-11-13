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
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 主数据模型字段定义Service接口
 *
 * @author qdata
 * @date 2026-09-20
 */
public interface IModelColumnService extends IService<ModelColumnDO> {

    /**
     * 校验主数据模型字段编码是否唯一
     */
    boolean checkCodeUnique(String code, Long id);

    /**
     * 获得主数据模型字段定义详情
     *
     * @param id 主数据模型字段定义编号
     * @return 主数据模型字段定义
     */
    ModelColumnDO getModelColumnById(Long id);

    List<ModelColumnDO> getModelColumnListByReqVO(ModelColumnPageReqVO pageReqVO);

    /**
     * 获得全部主数据模型字段定义列表
     *
     * @return 主数据模型字段定义列表
     */
    List<ModelColumnDO> getModelColumnList();
}
