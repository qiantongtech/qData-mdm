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

package tech.qiantong.qdata.mdm.model.convert.dict;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypePageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictTypeDO;

/**
 * 参考数据 Convert
 *
 * @author qdata
 * @date 2026-09-16
 */
@Mapper
public interface ModelDictTypeConvert {
    ModelDictTypeConvert INSTANCE = Mappers.getMapper(ModelDictTypeConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelDictTypePageReqVO 请求参数
     * @return ModelDictTypeDO
     */
     ModelDictTypeDO convertToDO(ModelDictTypePageReqVO modelDictTypePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelDictTypeSaveReqVO 保存请求参数
     * @return ModelDictTypeDO
     */
     ModelDictTypeDO convertToDO(ModelDictTypeSaveReqVO modelDictTypeSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelDictTypeDO 实体对象
     * @return ModelDictTypeRespVO
     */
     ModelDictTypeRespVO convertToRespVO(ModelDictTypeDO modelDictTypeDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelDictTypeDOList 实体对象列表
     * @return List<ModelDictTypeRespVO>
     */
     List<ModelDictTypeRespVO> convertToRespVOList(List<ModelDictTypeDO> modelDictTypeDOList);
}
