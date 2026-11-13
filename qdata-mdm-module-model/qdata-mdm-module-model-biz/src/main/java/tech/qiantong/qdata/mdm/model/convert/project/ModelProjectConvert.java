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

package tech.qiantong.qdata.mdm.model.convert.project;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;


/**
 * 主数据域 Convert
 *
 * @author qdata
 * @date 2026-09-15
 */
@Mapper
public interface ModelProjectConvert {
    ModelProjectConvert INSTANCE = Mappers.getMapper(ModelProjectConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelProjectPageReqVO 请求参数
     * @return ModelProjectDO
     */
     ModelProjectDO convertToDO(ModelProjectPageReqVO modelProjectPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelProjectSaveReqVO 保存请求参数
     * @return ModelProjectDO
     */
     ModelProjectDO convertToDO(ModelProjectSaveReqVO modelProjectSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelProjectDO 实体对象
     * @return ModelProjectRespVO
     */
     ModelProjectRespVO convertToRespVO(ModelProjectDO modelProjectDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelProjectDOList 实体对象列表
     * @return List<ModelProjectRespVO>
     */
     List<ModelProjectRespVO> convertToRespVOList(List<ModelProjectDO> modelProjectDOList);
}
