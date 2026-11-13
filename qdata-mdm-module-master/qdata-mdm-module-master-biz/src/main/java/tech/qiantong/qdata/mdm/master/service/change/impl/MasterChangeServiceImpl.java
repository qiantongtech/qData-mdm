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

package tech.qiantong.qdata.mdm.master.service.change.impl;

import java.util.Arrays;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeSnapshotRespVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeDO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRecordDTO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeMapper;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeService;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.util.MasterRecordUtils;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权威主数据已生效的业务快照变更，只读；状态变更另由状态记录承载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterChangeServiceImpl implements IMasterChangeService {
    private static final int MAX_SNAPSHOT_CHARS = 262144;
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final List<String> EFFECTIVE_TYPES = Arrays.asList("UPDATE", "RESTORE", "MERGE");

    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository dataQueryRepository;
    private final MasterChangeMapper changeMapper;

    /**
     * 查询单条主数据已生效的业务变更记录，不读取业务快照大字段。
     *
     * @param instanceId 可用模型实例 ID
     * @param masterDataId 权威主数据记录 ID
     * @param reqVO 分页及排序参数
     * @return 变更记录总数与当前页数据
     */
    @Override
    public PageResult<MasterChangeRespVO> getPage(Long instanceId, Long masterDataId,
                                                  MasterChangePageReqVO reqVO) {
        Long modelId = checkedModelId(instanceId, masterDataId);
        Integer pageNum = reqVO == null ? null : reqVO.getPageNum();
        Integer pageSize = reqVO == null ? null : reqVO.getPageSize();
        int page = pageNum == null ? 1 : Math.max(1, pageNum);
        int size = pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize));
        try {
            IPage<MasterChangeRecordDTO> dataPage = changeMapper.selectEffectiveChangePage(
                    new Page<>(page, size), modelId, masterDataId, reqVO);
            List<MasterChangeRespVO> result = new ArrayList<>();
            for (MasterChangeRecordDTO row : dataPage.getRecords()) {
                MasterChangeRespVO item = new MasterChangeRespVO();
                item.setId(row.getId() == null ? null : String.valueOf(row.getId()));
                item.setChangeTime(row.getChangeTime());
                item.setType(row.getType());
                item.setSource(row.getSource());
                item.setOperatorName(row.getOperatorName());
                item.setBeforeVersionNo(row.getBeforeVersionNo());
                item.setAfterVersionNo(row.getAfterVersionNo());
                item.setRequestNo(row.getRequestNo());
                item.setChangeReason(row.getChangeReason());
                result.add(item);
            }
            return new PageResult<>(result, dataPage.getTotal());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询主数据变更记录失败，instanceId={}, masterDataId={}", instanceId, masterDataId, ex);
            throw new ServiceException("主数据变更记录查询失败");
        }
    }

    /**
     * 查询一条已生效变更的前后业务快照，并在可行时计算 JSON 字段差异。
     *
     * @param instanceId 可用模型实例 ID
     * @param masterDataId 权威主数据记录 ID
     * @param changeId 变更记录 ID
     * @return 原始快照、截断标志与字段差异
     */
    @Override
    public MasterChangeSnapshotRespVO getSnapshot(Long instanceId, Long masterDataId, Long changeId) {
        if (changeId == null || changeId <= 0) throw new ServiceException("变更ID格式无效");
        Long modelId = checkedModelId(instanceId, masterDataId);
        try {
            List<MasterChangeDO> rows = changeMapper.selectList(new LambdaQueryWrapper<MasterChangeDO>()
                    .select(MasterChangeDO::getId, MasterChangeDO::getBeforeData, MasterChangeDO::getAfterData)
                    .eq(MasterChangeDO::getId, changeId)
                    .eq(MasterChangeDO::getModelId, modelId)
                    .eq(MasterChangeDO::getMasterRecordId, masterDataId)
                    .in(MasterChangeDO::getType, EFFECTIVE_TYPES)
                    .eq(MasterChangeDO::getProcessResult, "1")
                    .isNotNull(MasterChangeDO::getAfterVersionId)
                    .eq(MasterChangeDO::getValidFlag, "1"));
            if (rows.isEmpty()) throw new ServiceException("已生效的数据变更不存在");
            if (rows.size() > 1) throw new ServiceException("变更ID存在重复记录");
            MasterChangeDO row = rows.get(0);
            Snapshot before = snapshot(row.getBeforeData());
            Snapshot after = snapshot(row.getAfterData());
            MasterChangeSnapshotRespVO response = new MasterChangeSnapshotRespVO();
            response.setId(String.valueOf(row.getId()));
            response.setBeforeData(before.content);
            response.setAfterData(after.content);
            response.setBeforeTruncated(before.truncated);
            response.setAfterTruncated(after.truncated);
            response.setDiffs(Collections.emptyList());
            response.setDiffAvailable(false);
            if (!before.truncated && !after.truncated && before.content != null && after.content != null) {
                try {
                    JsonNode left = JSON.readTree(before.content);
                    JsonNode right = JSON.readTree(after.content);
                    if (left != null && right != null && left.isObject() && right.isObject()) {
                        List<MasterChangeSnapshotRespVO.FieldDiff> diffs = new ArrayList<>();
                        collectDiffs("", left, right, diffs);
                        response.setDiffs(diffs);
                        response.setDiffAvailable(true);
                    }
                } catch (Exception ignored) {
                    // 保留原始快照供查看，不能以解析失败的结果伪造字段差异。
                }
            }
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询主数据变更快照失败，instanceId={}, masterDataId={}, changeId={}",
                    instanceId, masterDataId, changeId, ex);
            throw new ServiceException("主数据变更快照查询失败");
        }
    }

    private Long checkedModelId(Long instanceId, Long masterDataId) {
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        MasterRecordUtils.requireRecord(dataQueryRepository, model, masterDataId);
        return model.getModelId();
    }

    private static Snapshot snapshot(Object raw) throws Exception {
        if (raw == null) return new Snapshot(null, false);
        if (raw instanceof Clob) {
            Clob clob = (Clob) raw;
            long length = clob.length();
            String content = length == 0 ? "" : clob.getSubString(1, (int) Math.min(length, MAX_SNAPSHOT_CHARS));
            return new Snapshot(content, length > MAX_SNAPSHOT_CHARS);
        }
        String text = String.valueOf(raw);
        boolean truncated = text.length() > MAX_SNAPSHOT_CHARS;
        return new Snapshot(truncated ? text.substring(0, MAX_SNAPSHOT_CHARS) : text, truncated);
    }

    private static void collectDiffs(String prefix, JsonNode before, JsonNode after,
                                     List<MasterChangeSnapshotRespVO.FieldDiff> diffs) {
        Set<String> keys = new LinkedHashSet<>();
        Iterator<String> leftKeys = before.fieldNames();
        Iterator<String> rightKeys = after.fieldNames();
        while (leftKeys.hasNext()) keys.add(leftKeys.next());
        while (rightKeys.hasNext()) keys.add(rightKeys.next());
        for (String key : keys) {
            JsonNode left = before.get(key);
            JsonNode right = after.get(key);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (left != null && right != null && left.isObject() && right.isObject()) {
                collectDiffs(path, left, right, diffs);
            } else if (left == null ? right != null : !left.equals(right)) {
                MasterChangeSnapshotRespVO.FieldDiff diff =
                        new MasterChangeSnapshotRespVO.FieldDiff();
                diff.setFieldPath(path);
                diff.setBeforeValue(jsonValue(left));
                diff.setAfterValue(jsonValue(right));
                diffs.add(diff);
            }
        }
    }

    private static String jsonValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return node.isTextual() ? node.textValue() : node.toString();
    }

    private static class Snapshot {
        private final String content;
        private final boolean truncated;
        private Snapshot(String content, boolean truncated) {
            this.content = content;
            this.truncated = truncated;
        }
    }
}
