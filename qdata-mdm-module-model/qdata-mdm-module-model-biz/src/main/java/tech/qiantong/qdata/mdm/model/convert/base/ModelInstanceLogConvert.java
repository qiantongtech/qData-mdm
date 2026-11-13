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
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceLogReqDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceLogRespDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceLogDO;

import java.util.List;

/**
 * 模型发布阶段日志转换。
 */
@Mapper
public interface ModelInstanceLogConvert {
    /**
     * MapStruct生成的对象转换器，只负责复制字段，不查询或修改数据库。
     */
    ModelInstanceLogConvert INSTANCE = Mappers.getMapper(ModelInstanceLogConvert.class);

    /**
     * 将发布阶段日志请求转换为持久化对象；只转换数据，不执行数据库写入。
     *
     * @param reqDTO 发布阶段日志请求
     * @return 发布阶段日志持久化对象
     */
    ModelInstanceLogDO convertToDO(ModelInstanceLogReqDTO reqDTO);

    /**
     * 将发布阶段日志持久化对象转换为响应数据。
     *
     * @param dataObject 发布阶段日志持久化对象
     * @return 发布阶段日志响应数据
     */
    ModelInstanceLogRespDTO convertToRespDTO(ModelInstanceLogDO dataObject);

    /**
     * 按原列表顺序批量转换发布阶段日志响应数据。
     *
     * @param dataObjects 发布阶段日志持久化对象列表
     * @return 发布阶段日志响应列表
     */
    List<ModelInstanceLogRespDTO> convertToRespDTOList(List<ModelInstanceLogDO> dataObjects);
}
