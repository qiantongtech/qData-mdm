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

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedTableReqDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelMaterializedTableDO;

import java.util.List;

/**
 * 成功物化表定义转换。
 */
@Mapper
public interface ModelMaterializedTableConvert {
    /**
     * MapStruct生成的对象转换器，只负责复制字段，不查询或修改数据库。
     */
    ModelMaterializedTableConvert INSTANCE = Mappers.getMapper(ModelMaterializedTableConvert.class);

    /**
     * 将成功物化表定义请求转换为持久化对象；只转换数据，不执行数据库写入。
     *
     * @param reqDTO 成功物化表定义请求
     * @return 成功物化表定义持久化对象
     */
    ModelMaterializedTableDO convertToDO(ModelMaterializedTableReqDTO reqDTO);

    /**
     * 将成功物化表定义持久化对象转换为响应数据。
     *
     * @param dataObject 成功物化表定义持久化对象
     * @return 成功物化表定义响应数据
     */
    ModelMaterializedTableRespDTO convertToRespDTO(ModelMaterializedTableDO dataObject);

    /**
     * 按原列表顺序批量转换成功物化表定义响应数据。
     *
     * @param dataObjects 成功物化表定义持久化对象列表
     * @return 成功物化表定义响应列表
     */
    List<ModelMaterializedTableRespDTO> convertToRespDTOList(List<ModelMaterializedTableDO> dataObjects);
}
