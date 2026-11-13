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

package tech.qiantong.qdata.mdm.model.convert.encoding;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingItemDO;

import java.util.List;

/**
 * 编码方案规则段 Convert
 *
 * @author qdata
 * @date 2026-09-17
 */
@Mapper
public interface ModelEncodingItemConvert {
    ModelEncodingItemConvert INSTANCE = Mappers.getMapper(ModelEncodingItemConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelEncodingItemPageReqVO 请求参数
     * @return ModelEncodingItemDO
     */
     ModelEncodingItemDO convertToDO(ModelEncodingItemPageReqVO modelEncodingItemPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelEncodingItemSaveReqVO 保存请求参数
     * @return ModelEncodingItemDO
     */
     ModelEncodingItemDO convertToDO(ModelEncodingItemSaveReqVO modelEncodingItemSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelEncodingItemDO 实体对象
     * @return ModelEncodingItemRespVO
     */
     ModelEncodingItemRespVO convertToRespVO(ModelEncodingItemDO modelEncodingItemDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelEncodingItemDOList 实体对象列表
     * @return List<ModelEncodingItemRespVO>
     */
     List<ModelEncodingItemRespVO> convertToRespVOList(List<ModelEncodingItemDO> modelEncodingItemDOList);
}
