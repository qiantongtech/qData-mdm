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

package tech.qiantong.qdata.mdm.master.service.instance.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterInstanceRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.instance.vo.MasterInstancePageReqVO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestPendingCountDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.data.MasterDataStatisticsDTO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestMapper;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.RequestContext;
import tech.qiantong.qdata.mdm.master.service.instance.IMasterInstanceService;
import tech.qiantong.qdata.mdm.master.util.MasterPublishedModelUtils;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedPageReqDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 卡片模型信息直接来自 model-api，主数据统计与维护审批由 master 组装。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterInstanceServiceImpl implements IMasterInstanceService {
    private static final int ENTRY_PAGE_SIZE = 6;
    private final ModelPublishedApi modelPublishedApi;
    private final MasterDataQueryRepository dataQueryRepository;
    private final MasterChangeRequestMapper changeRequestMapper;

    @Override
    public PageResult<MasterInstanceRespDTO> getInstancePage(MasterInstancePageReqVO req) {
        ModelPublishedPageReqDTO request = new ModelPublishedPageReqDTO();
        request.setPageNum(req.getPageNum() == null ? 1 : Math.max(req.getPageNum(), 1));
        request.setPageSize(ENTRY_PAGE_SIZE);
        request.setProjectId(req.getProjectId());
        request.setLayerId(req.getLayerId());
        request.setId(req.getModelId());
        request.setName(req.getName());
        request.setType(req.getType());
        // 卡片在数据库分页前按模型 ID 倒序，保证新建模型优先展示。
        request.setOrderByColumn("id");
        request.setIsAsc("desc");
        PageResult<ModelPublishedRespDTO> page = modelPublishedApi.getPublishedModelPage(request);
        List<MasterInstanceRespDTO> cards;
        try (RequestContext context = dataQueryRepository.openRequest()) {
            cards = page.getRows().stream().map(model -> toCard(model, context)).collect(Collectors.toList());
        }
        fillPendingMaintenanceApprovals(cards);
        return new PageResult<>(cards, page.getTotal());
    }

    private MasterInstanceRespDTO toCard(ModelPublishedRespDTO model, RequestContext context) {
        MasterInstanceRespDTO card = new MasterInstanceRespDTO();
        card.setInstanceId(model.getInstanceId());
        card.setModelId(model.getId());
        card.setModelVersionId(model.getVersionId());
        card.setVersionNo(model.getVersionNo());
        card.setModelCode(model.getCode());
        card.setModelName(model.getName());
        card.setModelDescription(model.getDescription());
        card.setModelType(model.getType());
        card.setProjectId(model.getProjectId());
        card.setDomainName(model.getDomainName());
        card.setLayerId(model.getLayerId());
        fillDataStatistics(card, model, context);
        return card;
    }

    /** 只统计从权威主数据直接发起、已提交且仍在审批中的申请单。 */
    private void fillPendingMaintenanceApprovals(List<MasterInstanceRespDTO> cards) {
        if (cards.isEmpty()) return;
        List<Long> modelIds = cards.stream().map(MasterInstanceRespDTO::getModelId).collect(Collectors.toList());
        List<MasterChangeRequestPendingCountDTO> rows = changeRequestMapper.selectPendingCountsByModelIds(modelIds);
        Map<Long, Long> counts = new HashMap<>();
        for (MasterChangeRequestPendingCountDTO row : rows) {
            counts.put(row.getModelId(), row.getPendingCount());
        }
        for (MasterInstanceRespDTO card : cards) {
            card.setPendingMaintenanceApprovalCount(counts.getOrDefault(card.getModelId(), 0L));
        }
    }

    /** 只统计同一成功实例中启用的权威主表，来源表和子表不参与卡片总量。 */
    private void fillDataStatistics(MasterInstanceRespDTO card,
            ModelPublishedRespDTO model, RequestContext context) {
        if (model.getAuthorityTableName() == null) {
            card.setDataCountAvailable(false);
            return;
        }
        try {
            String tableName = MasterPublishedModelUtils.qualifiedTable(model.getAuthoritySchemaName(), model.getAuthorityTableName());
            MasterDataStatisticsDTO statistics = dataQueryRepository.selectStatistics(context,
                    model.getAuthorityDatasourceId(), tableName, model.getAuthorityStatisticsColumns());
            if (statistics == null) {
                card.setDataCountAvailable(false);
                return;
            }
            card.setDataCount(statistics.getDataCount());
            card.setActiveCount(statistics.getActiveCount());
            card.setFrozenCount(statistics.getFrozenCount());
            card.setInvalidCount(statistics.getInvalidCount());
            card.setArchivedCount(statistics.getArchivedCount());
            card.setLatestDataUpdateTime(statistics.getLatestDataUpdateTime());
            card.setDataCountAvailable(true);
        } catch (Exception ex) {
            log.warn("读取卡片权威数据统计失败，modelId={}, instanceId={}", card.getModelId(), card.getInstanceId(), ex);
            card.setDataCountAvailable(false);
        }
    }
}
