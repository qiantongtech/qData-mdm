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

package tech.qiantong.qdata.mdm.merge.utils.detect;

import com.fasterxml.jackson.databind.JsonNode;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectStartReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectScopeBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 重复检测领域配置和快照转换
 *
 * @author chentangwen
 */
public final class MergeDetectConfigUtils {

    private MergeDetectConfigUtils() {
    }

    /** 去除空编号和重复编号，保留首次出现顺序。 */
    public static List<Long> distinctIds(List<Long> ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        Set<Long> seen = new HashSet<>();
        for (Long id : ids) {
            if (id != null && seen.add(id)) {
                result.add(id);
            }
        }
        return result;
    }

    /** 读取历史来源编号配置，非法或空 JSON 沿用空列表。 */
    public static List<Long> readSystemIds(String text) {
        List<Long> ids = new ArrayList<>();
        JsonNode node = MergeJson.read(text);
        if (node == null || !node.isArray()) {
            return ids;
        }
        for (JsonNode item : node) {
            if (item.isNumber()) {
                ids.add(item.asLong());
            }
        }
        return ids;
    }

    /** 将持久化任务范围还原为请求配置，不复制运行状态。 */
    public static MergeDetectStartReqVO toScopeRequest(MergeDuplicateTaskDO task) {
        MergeDetectStartReqVO reqVO = new MergeDetectStartReqVO();
        reqVO.setModelId(task.getModelId());
        reqVO.setScopeType(task.getScopeType());
        reqVO.setReceiveStartTime(task.getReceiveStartTime());
        reqVO.setReceiveEndTime(task.getReceiveEndTime());
        reqVO.setSourceSystemIds(readSystemIds(task.getSourceSystemIds()));
        return reqVO;
    }

    /** 将任务列表项的范围还原为配置，供统计查询使用。 */
    public static MergeDetectStartReqVO toScopeRequest(MergeTaskRespVO task) {
        MergeDetectStartReqVO request = new MergeDetectStartReqVO();
        request.setModelId(task.getModelId());
        request.setScopeType(task.getScopeType());
        request.setSourceSystemIds(readSystemIds(task.getSourceSystemIds()));
        request.setReceiveStartTime(task.getReceiveStartTime());
        request.setReceiveEndTime(task.getReceiveEndTime());
        return request;
    }

    /** 保存本次范围快照，仅自定义范围保存来源系统与日期。 */
    public static Map<String, Object> taskConfig(Long modelId, MergeDetectScopeBO scope) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("modelId", modelId);
        config.put("scopeType", scope.isCustom() ? MergeCodes.SCOPE_CUSTOM : MergeCodes.SCOPE_MODEL);
        if (scope.isCustom()) {
            config.put("sourceSystemIds", scope.getSystemIds());
            config.put("receiveStartTime", scope.getStart());
            config.put("receiveEndTime", scope.getEnd());
        }
        return config;
    }

    /** 保存参与检测的规则编号、名称、类型和原始配置。 */
    public static List<Map<String, Object>> ruleSnapshot(List<RuleMatchDO> rules) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RuleMatchDO rule : rules) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rule.getId());
            row.put("ruleName", rule.getRuleName());
            row.put("ruleType", rule.getRuleType());
            row.put("ruleConfig", rule.getRuleConfig());
            rows.add(row);
        }
        return rows;
    }

    /** 保存检测时固定的来源版本，序号仅用于快照展示。 */
    public static List<Map<String, Object>> sourceSnapshot(List<SourceSnapshot> snapshots) {
        List<Map<String, Object>> rows = new ArrayList<>();
        long sequence = 1L;
        for (SourceSnapshot snapshot : snapshots) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", sequence++);
            row.put("sourceRecordId", snapshot.getSourceRecordId());
            row.put("versionId", snapshot.getVersionId());
            row.put("target", snapshot.isTarget());
            row.put("systemCode", snapshot.getSystemCode());
            row.put("systemName", snapshot.getSystemName());
            row.put("stableRecordId", snapshot.getStableRecordId());
            row.put("receiveTime", snapshot.getReceiveTime());
            row.put("modelData", snapshot.getModelData());
            rows.add(row);
        }
        return rows;
    }
}
