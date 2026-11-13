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
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceReqDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceRespDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;

/**
 * 模型发布实例 Convert
 *
 * @author qdata
 * @date 2026-09-24
 */
@Mapper
public interface ModelInstanceConvert {
    ModelInstanceConvert INSTANCE = Mappers.getMapper(ModelInstanceConvert.class);

    /**
     * ReqDTO 转换为 DO
     * @param modelInstanceReqDTO 请求参数
     * @return ModelInstanceDO
     */
     ModelInstanceDO convertToDO(ModelInstanceReqDTO modelInstanceReqDTO);

    /**
     * DO 转换为 RespDTO
     * @param modelInstanceDO 实体对象
     * @return ModelInstanceRespDTO
     */
     ModelInstanceRespDTO convertToRespDTO(ModelInstanceDO modelInstanceDO);

    /**
     * DOList 转换为 RespDTOList
     * @param modelInstanceDOList 实体对象列表
     * @return List<ModelInstanceRespDTO>
     */
     List<ModelInstanceRespDTO> convertToRespDTOList(List<ModelInstanceDO> modelInstanceDOList);
}
