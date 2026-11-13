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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceHistoryRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceReqDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelInstanceMapper;
import tech.qiantong.qdata.mdm.model.service.base.IModelInstanceService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 模型发布实例Service业务层处理
 *
 * @author qdata
 * @date 2026-09-24
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelInstanceServiceImpl extends ServiceImpl<ModelInstanceMapper, ModelInstanceDO> implements IModelInstanceService {
    /**
     * 查询平台中的发布实例，历史列表不连接物化业务数据库。
     */
    @Resource
    private ModelInstanceMapper modelInstanceMapper;

    /**
     * 按模型ID读取真实发布记录，不再根据版本的修订来源判断是否发布。
     */
    @Override
    public List<ModelInstanceHistoryRespDTO> getModelInstanceHistory(Long modelId) {
        return modelInstanceMapper.selectHistoryByModelId(modelId);
    }

    @Override
    public List<ModelInstanceDO> getModelInstanceList(ModelInstanceReqDTO reqDTO) {
        QueryWrapper<ModelInstanceDO> wrapper = new QueryWrapper<>();
        wrapper.eq(reqDTO.getId() != null, "ID", reqDTO.getId())
                .eq(reqDTO.getModelId() != null, "MODEL_ID", reqDTO.getModelId())
                .eq(reqDTO.getModelVersionId() != null, "MODEL_VERSION_ID", reqDTO.getModelVersionId())
                .eq(reqDTO.getDatasourceId() != null, "DATASOURCE_ID", reqDTO.getDatasourceId())
                .eq(reqDTO.getBaseInstanceId() != null, "BASE_INSTANCE_ID", reqDTO.getBaseInstanceId())
                .eq(reqDTO.getStatus() != null, "STATUS", reqDTO.getStatus())
                .orderByDesc("ID");
        return modelInstanceMapper.selectList(wrapper);
    }
}
