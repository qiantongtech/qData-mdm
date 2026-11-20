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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.api.datasource;

import tech.qiantong.qdata.mdm.ingest.api.datasource.dto.IngestDatasourceRespDTO;

/**
 * 供其他后端模块读取数据源连接配置的公共接口。
 * <p>
 * 调用方通过本接口及返回 DTO 获取配置，具体查询由 ingest-biz 模块实现。
 *
 */
public interface IngestDatasourceApi {

    /**
     * 根据数据源 ID 查询连接配置信息，供调用方访问目标数据库时使用。
     * <p>
     * 本接口仅提供配置查询，不建立目标数据库连接；数据源是否启用由调用方校验。
     *
     * @param datasourceId 数据源 ID
     * @return 数据源连接配置信息
     */
    IngestDatasourceRespDTO getDatasource(Long datasourceId);
}
