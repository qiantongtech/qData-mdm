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

package tech.qiantong.qdata.mdm.ingest.service.ingest;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataDetailRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataModelColumnRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataModelTreeRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataPageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataPageRespVO;

import java.util.List;

/**
 * 来源数据服务。
 */
public interface SourceDataService {

    PageResult<SourceDataPageRespVO> getPage(SourceDataPageReqVO pageReqVO);

    List<SourceDataModelTreeRespVO> getModelTree();

    /**
     * 查询指定已发布模型的有效业务字段定义。
     */
    List<SourceDataModelColumnRespVO> getModelColumns(Long modelId);

    SourceDataDetailRespVO getDetail(Long modelId, Long id);

    void deleteRecord(Long modelId, Long id);
}
