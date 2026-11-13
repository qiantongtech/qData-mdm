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

package tech.qiantong.qdata.mdm.merge.service.detect;

import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectScopeRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectStartReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskRespVO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectContextBO;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 重复检测的数据范围与快照查询。
 *
 * @author chentangwen
 */
public interface IMergeDetectScopeService {

    /** 固定本次检测上下文；无有效模型、规则或待治理目标时抛出业务异常。 */
    MergeDetectContextBO prepareDetection(MergeDetectStartReqVO request);

    /** 查询范围预览；不创建运行实例或改变来源治理状态。 */
    MergeDetectScopeRespVO previewScope(MergeDetectStartReqVO request);

    /** 补充列表统计；同一页内复用同模型的数据查询。 */
    void fillTaskMetrics(List<MergeTaskRespVO> rows);

    /** 查询已发布的有效模型；未发布时抛出业务异常。 */
    ModelBaseDO requirePublishedModel(Long modelId);

    /** 批量查询来源系统信息。 */
    Map<Long, IngestSourceDO> loadSystems(List<MergeSourceRecordBO> sources);

    /** 查询当前模型内仍被有效候选组占用的来源记录编号。 */
    Set<Long> occupiedSourceIds(Long modelId, List<Long> sourceIds);
}
