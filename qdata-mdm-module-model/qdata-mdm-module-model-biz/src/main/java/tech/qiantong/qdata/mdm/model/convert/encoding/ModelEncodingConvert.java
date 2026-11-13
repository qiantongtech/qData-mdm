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
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingDO;

import java.util.List;

/**
 * 编码方案 Convert
 *
 * @author qdata
 * @date 2026-09-16
 */
@Mapper
public interface ModelEncodingConvert {
    ModelEncodingConvert INSTANCE = Mappers.getMapper(ModelEncodingConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelEncodingPageReqVO 请求参数
     * @return ModelEncodingDO
     */
     ModelEncodingDO convertToDO(ModelEncodingPageReqVO modelEncodingPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelEncodingSaveReqVO 保存请求参数
     * @return ModelEncodingDO
     */
     ModelEncodingDO convertToDO(ModelEncodingSaveReqVO modelEncodingSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelEncodingDO 实体对象
     * @return ModelEncodingRespVO
     */
     ModelEncodingRespVO convertToRespVO(ModelEncodingDO modelEncodingDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelEncodingDOList 实体对象列表
     * @return List<ModelEncodingRespVO>
     */
     List<ModelEncodingRespVO> convertToRespVOList(List<ModelEncodingDO> modelEncodingDOList);
}
