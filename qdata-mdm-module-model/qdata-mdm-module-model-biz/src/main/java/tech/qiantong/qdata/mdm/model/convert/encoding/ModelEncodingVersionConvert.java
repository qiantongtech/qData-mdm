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

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingVersionDO;

/**
 * 编码方案版本 Convert
 *
 * @author qdata
 * @date 2026-09-17
 */
@Mapper
public interface ModelEncodingVersionConvert {
    ModelEncodingVersionConvert INSTANCE = Mappers.getMapper(ModelEncodingVersionConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelEncodingVersionPageReqVO 请求参数
     * @return ModelEncodingVersionDO
     */
     ModelEncodingVersionDO convertToDO(ModelEncodingVersionPageReqVO modelEncodingVersionPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelEncodingVersionSaveReqVO 保存请求参数
     * @return ModelEncodingVersionDO
     */
     ModelEncodingVersionDO convertToDO(ModelEncodingVersionSaveReqVO modelEncodingVersionSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelEncodingVersionDO 实体对象
     * @return ModelEncodingVersionRespVO
     */
     ModelEncodingVersionRespVO convertToRespVO(ModelEncodingVersionDO modelEncodingVersionDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelEncodingVersionDOList 实体对象列表
     * @return List<ModelEncodingVersionRespVO>
     */
     List<ModelEncodingVersionRespVO> convertToRespVOList(List<ModelEncodingVersionDO> modelEncodingVersionDOList);
}
