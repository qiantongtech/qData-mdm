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
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;

/**
 * 主数据模型基本信息 Convert
 *
 * @author qdata
 * @date 2026-09-18
 */
@Mapper
public interface ModelBaseConvert {
    ModelBaseConvert INSTANCE = Mappers.getMapper(ModelBaseConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelBasePageReqVO 请求参数
     * @return ModelBaseDO
     */
     ModelBaseDO convertToDO(ModelBasePageReqVO modelBasePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelBaseSaveReqVO 保存请求参数
     * @return ModelBaseDO
     */
     ModelBaseDO convertToDO(ModelBaseSaveReqVO modelBaseSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelBaseDO 实体对象
     * @return ModelBaseRespVO
     */
     ModelBaseRespVO convertToRespVO(ModelBaseDO modelBaseDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelBaseDOList 实体对象列表
     * @return List<ModelBaseRespVO>
     */
     List<ModelBaseRespVO> convertToRespVOList(List<ModelBaseDO> modelBaseDOList);
}
