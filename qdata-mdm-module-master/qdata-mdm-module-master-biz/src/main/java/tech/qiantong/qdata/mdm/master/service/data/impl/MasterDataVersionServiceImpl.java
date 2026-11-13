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

package tech.qiantong.qdata.mdm.master.service.data.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionSnapshotRespVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.data.MasterDataVersionDO;
import tech.qiantong.qdata.mdm.master.dal.mapper.data.MasterDataVersionMapper;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataVersionService;
import tech.qiantong.qdata.mdm.master.util.MasterRecordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * MASTER_DATA_VERSION 当前版本、历史列表及快照查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataVersionServiceImpl implements IMasterDataVersionService {

    private static final int MAX_RECORD_IDS_PER_QUERY = 500;

    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository dataQueryRepository;

    private final MasterDataVersionMapper masterDataVersionMapper;

    /**
     * 查询单条主数据当前有效、未删除的版本 ID。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @return 当前版本 ID；无匹配时为 null
     * @throws ServiceException 存在多个有效当前版本时
     */
    @Override
    public Long getCurrentVersionId(Long modelId, Long masterRecordId) {
        List<Long> ids = masterDataVersionMapper.selectCurrentVersionIds(modelId, masterRecordId);
        if (ids.size() > 1) throw new ServiceException("当前主数据存在多个有效版本");
        return ids.isEmpty() ? null : ids.get(0);
    }

    /**
     * 批量补充当前有效版本的展示编号；对记录 ID 去重，并按每批最多 500 个查询。
     *
     * @param modelId 主数据模型 ID
     * @param recordIds 主数据记录 ID 列表
     * @return 记录 ID 到版本展示编号的映射；无匹配时不包含该记录
     * @throws ServiceException 同一记录存在多个当前版本时
     */
    @Override
    public Map<Long, String> getCurrentVersionNumbers(Long modelId, List<Long> recordIds) {
        Map<Long, String> result = new HashMap<>();
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(recordIds));
        for (int start = 0; start < uniqueIds.size(); start += MAX_RECORD_IDS_PER_QUERY) {
            List<Long> batch = uniqueIds.subList(start,
                    Math.min(start + MAX_RECORD_IDS_PER_QUERY, uniqueIds.size()));
            for (MasterDataVersionDO row : masterDataVersionMapper.selectCurrentVersionNumbers(modelId, batch)) {
                Long recordId = row.getMasterRecordId();
                if (recordId == null) continue;
                if (result.containsKey(recordId)) {
                    throw new ServiceException("主数据ID " + recordId + " 存在多个当前版本");
                }
                result.put(recordId, row.getVersionNo());
            }
        }
        return result;
    }

    private static final int MAX_SNAPSHOT_CHARS = 262144;

    /**
     * 分页查询历史版本并校验记录归属。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 分页及排序参数，排序字段仅允许业务白名单字段
     * @return 主数据历史版本分页列表
     */
    @Override
    public PageResult<MasterDataVersionRespVO> getPage(Long instanceId, Long masterDataId,
                                                       MasterDataVersionPageReqVO reqVO) {
        Long modelId = historyCheckedModelId(instanceId, masterDataId);
        Integer pageNum = reqVO.getPageNum();
        Integer pageSize = reqVO.getPageSize();
        int page = pageNum == null ? 1 : Math.max(1, pageNum);
        int size = pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize));
        try {
            IPage<MasterDataVersionDO> dataPage = masterDataVersionMapper.selectVersionPage(
                    new Page<>(page, size), modelId, masterDataId, reqVO);
            List<MasterDataVersionRespVO> result = new ArrayList<>();
            for (MasterDataVersionDO row : dataPage.getRecords()) {
                MasterDataVersionRespVO item = new MasterDataVersionRespVO();
                item.setId(String.valueOf(row.getId()));
                item.setVersionNo(row.getVersionNo());
                item.setGenerateTime(row.getGenerateTime());
                item.setGenerateType(row.getGenerateType());
                item.setChangeType(row.getChangeType());
                item.setOperatorName(row.getOperatorName());
                item.setRequestNo(row.getRequestNo());
                item.setCurrentFlag(row.getCurrentFlag());
                result.add(item);
            }
            return new PageResult<>(result, dataPage.getTotal());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询主数据版本记录失败，instanceId={}, masterDataId={}", instanceId, masterDataId, ex);
            throw new ServiceException("主数据版本记录查询失败");
        }
    }

    /**
     * 查询指定历史版本快照并校验记录归属。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param versionId 主数据版本 ID
     * @return 指定版本的数据快照及截断标志
     */
    @Override
    public MasterDataVersionSnapshotRespVO getSnapshot(Long instanceId, Long masterDataId, Long versionId) {
        if (versionId == null || versionId <= 0) throw new ServiceException("版本ID格式无效");
        Long modelId = historyCheckedModelId(instanceId, masterDataId);
        try {
            MasterDataVersionDO row = masterDataVersionMapper.selectSnapshot(modelId, masterDataId, versionId);
            if (row == null) throw new ServiceException("数据版本不存在");
            String full = row.getSnapshotData();
            boolean truncated = full != null && full.length() > MAX_SNAPSHOT_CHARS;
            String content = truncated ? full.substring(0, MAX_SNAPSHOT_CHARS) : full;
            MasterDataVersionSnapshotRespVO response = new MasterDataVersionSnapshotRespVO();
            response.setId(String.valueOf(row.getId()));
            response.setVersionNo(row.getVersionNo());
            response.setSnapshotData(content);
            response.setTruncated(truncated);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询主数据版本快照失败，instanceId={}, masterDataId={}, versionId={}",
                    instanceId, masterDataId, versionId, ex);
            throw new ServiceException("主数据版本快照查询失败");
        }
    }

    /** 校验实例可用且记录属于该实例，返回版本查询所需的模型 ID。 */
    private Long historyCheckedModelId(Long instanceId, Long masterDataId) {
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        MasterRecordUtils.requireRecord(dataQueryRepository, model, masterDataId);
        return model.getModelId();
    }

}
