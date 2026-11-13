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
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

/**
 * 主数据模型字段定义 Convert
 *
 * @author qdata
 * @date 2026-09-20
 */
@Mapper
public interface ModelColumnConvert {
    ModelColumnConvert INSTANCE = Mappers.getMapper(ModelColumnConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param modelColumnPageReqVO 请求参数
     * @return ModelColumnDO
     */
     ModelColumnDO convertToDO(ModelColumnPageReqVO modelColumnPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param modelColumnSaveReqVO 保存请求参数
     * @return ModelColumnDO
     */
     ModelColumnDO convertToDO(ModelColumnSaveReqVO modelColumnSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param modelColumnDO 实体对象
     * @return ModelColumnRespVO
     */
     ModelColumnRespVO convertToRespVO(ModelColumnDO modelColumnDO);

    /**
     * DOList 转换为 RespVOList
     * @param modelColumnDOList 实体对象列表
     * @return List<ModelColumnRespVO>
     */
     List<ModelColumnRespVO> convertToRespVOList(List<ModelColumnDO> modelColumnDOList);
}
