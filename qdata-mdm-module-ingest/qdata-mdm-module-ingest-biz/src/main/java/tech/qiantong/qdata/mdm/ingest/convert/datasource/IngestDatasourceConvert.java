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

package tech.qiantong.qdata.mdm.ingest.convert.datasource;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource.IngestDatasourceDO;

import java.util.List;

/**
 * 数据源 Convert
 *
 * @author lhs
 * @date 2025-01-21
 */
@Mapper
public interface IngestDatasourceConvert {
    IngestDatasourceConvert INSTANCE = Mappers.getMapper(IngestDatasourceConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param daDatasourcePageReqVO 请求参数
     * @return DaDatasourceDO
     */
     IngestDatasourceDO convertToDO(IngestDatasourcePageReqVO daDatasourcePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param daDatasourceSaveReqVO 保存请求参数
     * @return DaDatasourceDO
     */
     IngestDatasourceDO convertToDO(IngestDatasourceSaveReqVO daDatasourceSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param daDatasourceDO 实体对象
     * @return DaDatasourceRespVO
     */
     IngestDatasourceRespVO convertToRespVO(IngestDatasourceDO daDatasourceDO);

    /**
     * DOList 转换为 RespVOList
     * @param daDatasourceDOList 实体对象列表
     * @return List<DaDatasourceRespVO>
     */
     List<IngestDatasourceRespVO> convertToRespVOList(List<IngestDatasourceDO> daDatasourceDOList);
}
