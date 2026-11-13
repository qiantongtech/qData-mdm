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
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.base.dto.ModelBaseRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.IModelBaseService;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelBaseApi;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** 与模型定义列表复用相同的版本映射和归属位置装配。 */
@Service
public class ModelBaseApiImpl implements ModelBaseApi {

    @Resource
    private IModelBaseService modelBaseService;

    @Override
    public List<ModelBaseRespDTO> getModelBaseList(Collection<Long> modelIds) {
        return modelBaseService.getModelBaseListByIds(modelIds).stream()
                .map(model -> BeanUtils.toBean(model, ModelBaseRespDTO.class))
                .collect(Collectors.toList());
    }
}
