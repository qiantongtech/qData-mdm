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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.api.datasource;

import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.api.datasource.dto.IngestDatasourceRespDTO;
import tech.qiantong.qdata.mdm.ingest.service.datasource.IIngestDatasourceService;

import javax.annotation.Resource;

/**
 * 数据源模块间查询接口的实现，复用采集模块内部服务读取连接配置。
 * <p>
 * 接口及返回 DTO 定义在 ingest-api 模块，本类在 ingest-biz 模块中提供具体实现，
 * 其他模块通过 IngestDatasourceApi 调用，避免直接依赖内部服务和数据库实体。
 *
 */
@Service
public class IngestDatasourceApiImpl implements IngestDatasourceApi {

    @Resource
    private IIngestDatasourceService datasourceService;

    /**
     * 根据数据源 ID 查询连接配置，并将数据库实体转换为模块间传输对象。
     * <p>
     * 本方法仅查询并转换数据源配置，不建立目标数据库连接，也不校验数据源是否启用。
     *
     * @param datasourceId 数据源 ID
     * @return 数据源连接配置信息
     */
    @Override
    public IngestDatasourceRespDTO getDatasource(Long datasourceId) {
        return BeanUtils.toBean(datasourceService.getDaDatasourceById(datasourceId), IngestDatasourceRespDTO.class);
    }
}
