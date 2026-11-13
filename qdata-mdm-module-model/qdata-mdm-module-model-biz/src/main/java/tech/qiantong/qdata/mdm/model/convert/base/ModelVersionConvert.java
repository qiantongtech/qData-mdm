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

package tech.qiantong.qdata.mdm.model.convert.base;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelVersionRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelVersionSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelVersionDO;

/**
 * 主数据模型版本 Convert
 *
 * @author qdata
 * @date 2026-09-18
 */
@Mapper
public interface ModelVersionConvert {
    ModelVersionConvert INSTANCE = Mappers.getMapper(ModelVersionConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelVersionPageReqVO 请求参数
     * @return ModelVersionDO
     */
     ModelVersionDO convertToDO(ModelVersionPageReqVO modelVersionPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelVersionSaveReqVO 保存请求参数
     * @return ModelVersionDO
     */
     ModelVersionDO convertToDO(ModelVersionSaveReqVO modelVersionSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelVersionDO 实体对象
     * @return ModelVersionRespVO
     */
     ModelVersionRespVO convertToRespVO(ModelVersionDO modelVersionDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelVersionDOList 实体对象列表
     * @return List<ModelVersionRespVO>
     */
     List<ModelVersionRespVO> convertToRespVOList(List<ModelVersionDO> modelVersionDOList);
}
