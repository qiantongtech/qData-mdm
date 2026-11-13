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

package tech.qiantong.qdata.mdm.model.convert.layer;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;

/**
 * 模型目录层级 Convert
 *
 * @author qdata
 * @date 2026-09-15
 */
@Mapper
public interface ModelLayerConvert {
    ModelLayerConvert INSTANCE = Mappers.getMapper(ModelLayerConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelLayerPageReqVO 请求参数
     * @return ModelLayerDO
     */
     ModelLayerDO convertToDO(ModelLayerPageReqVO modelLayerPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelLayerSaveReqVO 保存请求参数
     * @return ModelLayerDO
     */
     ModelLayerDO convertToDO(ModelLayerSaveReqVO modelLayerSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelLayerDO 实体对象
     * @return ModelLayerRespVO
     */
     ModelLayerRespVO convertToRespVO(ModelLayerDO modelLayerDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelLayerDOList 实体对象列表
     * @return List<ModelLayerRespVO>
     */
     List<ModelLayerRespVO> convertToRespVOList(List<ModelLayerDO> modelLayerDOList);
}
