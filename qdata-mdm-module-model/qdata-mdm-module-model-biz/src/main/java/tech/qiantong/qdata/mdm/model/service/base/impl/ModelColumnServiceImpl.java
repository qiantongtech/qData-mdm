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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.model.service.base.IModelColumnService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 主数据模型字段定义Service业务层处理
 *
 * @author qdata
 * @date 2026-09-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelColumnServiceImpl extends ServiceImpl<ModelColumnMapper, ModelColumnDO> implements IModelColumnService {
    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        QueryWrapper<ModelColumnDO> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        if (id != null) wrapper.ne("id", id);
        return modelColumnMapper.selectCount(wrapper) == 0;
    }

    @Override
    public ModelColumnDO getModelColumnById(Long id) {
        return modelColumnMapper.selectById(id);
    }

    @Override
    public List<ModelColumnDO> getModelColumnListByReqVO(ModelColumnPageReqVO pageReqVO) {
        MPJLambdaWrapper<ModelColumnDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq("model_id", pageReqVO.getModelId());
        return modelColumnMapper.selectList(wrapper);
    }

    @Override
    public List<ModelColumnDO> getModelColumnList() {
        return modelColumnMapper.selectList();
    }
}
