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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelVersionDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelVersionMapper;
import tech.qiantong.qdata.mdm.model.service.base.IModelVersionService;

import javax.annotation.Resource;

/**
 * 主数据模型版本Service业务层处理
 *
 * @author qdata
 * @date 2026-09-18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelVersionServiceImpl extends ServiceImpl<ModelVersionMapper, ModelVersionDO> implements IModelVersionService {
    @Resource
    private ModelVersionMapper modelVersionMapper;


    @Override
    public PageResult<ModelVersionDO> getModelVersionPage(ModelVersionPageReqVO pageReqVO) {
        return modelVersionMapper.selectPage(pageReqVO);
    }

    @Override
    public ModelVersionDO getModelVersionById(Long id) {
        return modelVersionMapper.selectById(id);
    }
}
