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

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictDataDO;

import java.util.List;

/**
 * 参考数据项 Convert
 *
 * @author qdata
 * @date 2026-09-16
 */
@Mapper
public interface ModelDictDataConvert {
    ModelDictDataConvert INSTANCE = Mappers.getMapper(ModelDictDataConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelDictDataPageReqVO 请求参数
     * @return ModelDictDataDO
     */
     ModelDictDataDO convertToDO(ModelDictDataPageReqVO modelDictDataPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelDictDataSaveReqVO 保存请求参数
     * @return ModelDictDataDO
     */
     ModelDictDataDO convertToDO(ModelDictDataSaveReqVO modelDictDataSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelDictDataDO 实体对象
     * @return ModelDictDataRespVO
     */
     ModelDictDataRespVO convertToRespVO(ModelDictDataDO modelDictDataDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelDictDataDOList 实体对象列表
     * @return List<ModelDictDataRespVO>
     */
     List<ModelDictDataRespVO> convertToRespVOList(List<ModelDictDataDO> modelDictDataDOList);
}
