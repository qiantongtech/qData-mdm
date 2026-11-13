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

package tech.qiantong.qdata.mdm.ingest.api.datasource;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.api.datasource.dto.IngestDatasourceRespDTO;
import tech.qiantong.qdata.mdm.ingest.service.datasource.IIngestDatasourceService;

@Service
@RequiredArgsConstructor
public class IngestDatasourceApiImpl implements IngestDatasourceApi {
    private final IIngestDatasourceService datasourceService;

    @Override
    public IngestDatasourceRespDTO getDatasource(Long datasourceId) {
        return BeanUtils.toBean(datasourceService.getDaDatasourceById(datasourceId), IngestDatasourceRespDTO.class);
    }
}
