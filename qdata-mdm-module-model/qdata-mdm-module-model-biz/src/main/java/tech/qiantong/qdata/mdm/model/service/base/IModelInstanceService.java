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
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceHistoryRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceReqDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;

import java.util.List;

/**
 * 模型发布实例Service接口
 *
 * @author qdata
 * @date 2026-09-24
 */
public interface IModelInstanceService extends IService<ModelInstanceDO> {
    /**
     * 查询指定主模型的全部发布实例，供详情页展示和前端分页。
     */
    List<ModelInstanceHistoryRespDTO> getModelInstanceHistory(Long modelId);

    /**
     * 获得模型发布实例列表
     *
     * @return 模型发布实例列表
     */
    List<ModelInstanceDO> getModelInstanceList(ModelInstanceReqDTO reqDTO);
}
