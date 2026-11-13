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

package tech.qiantong.qdata.mdm.merge.support;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 一次检测中固定下来的来源记录。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class SourceSnapshot {
    /** 来源记录编号。 */
    private Long sourceRecordId;
    /** 检测时固定的来源版本编号。 */
    private Long versionId;
    /** 来源系统编号。 */
    private Long systemId;
    /** 来源系统编码。 */
    private String systemCode;
    /** 来源系统名称。 */
    private String systemName;
    /** 来源系统可信度，未配置时为 0。 */
    private int trustScore;
    /** 固定版本的接收时间。 */
    private Date receiveTime;
    /** 是否为本次待治理目标；历史比较记录不独立生成结果。 */
    private boolean target;
    /** 来源稳定标识。 */
    private String stableRecordId;
    /** 模型名称字段的显示值。 */
    private String businessName;
    /** 检测时固定的模型数据 JSON。 */
    private String modelData;
    /** 按字段编码和字段 ID 索引的匹配值。 */
    private Map<String, String> values = new LinkedHashMap<String, String>();

    /**
     * 将来源记录转换为本次检测快照；无有效 JSON 对象时返回 null。
     * 字段值依次兼容字段编码、字段 ID、字段名称，并兼容来源键名大小写及常见分隔符差异；
     * 历史数据与待治理数据使用相同映射。字段值本身的标准化由匹配策略显式决定。
     *
     * @param source 来源记录
     * @param system 来源系统与可信度，允许为空
     * @param columns 模型字段
     * @param target 是否为待治理目标，历史比较记录为 false
     * @return 检测快照，无法参与检测时返回 null
     */
    public static SourceSnapshot from(MergeSourceRecordBO source,
                                      IngestSourceDO system, List<ModelColumnDO> columns, boolean target) {
        if (source == null || source.getModelData() == null
                || source.getModelData().trim().isEmpty()) {
            return null;
        }
        JsonNode root;
        try {
            root = MergeJson.readStrict(source.getModelData());
        } catch (ServiceException ex) {
            // 无法解析的来源记录不能构成有效快照，由检测入口累计跳过数量。
            return null;
        }
        if (!root.isObject()) {
            return null;
        }
        Map<String, String> raw = new LinkedHashMap<String, String>();
        // 为来源 JSON 建立规范化键索引，兼容字段编码大小写或分隔符不一致的情况。
        Map<String, String> normalizedRaw = new LinkedHashMap<String, String>();
        root.fields().forEachRemaining(entry -> {
            String value = entry.getValue().isNull() ? "" : entry.getValue().asText();
            raw.put(entry.getKey(), value);
            normalizedRaw.putIfAbsent(normalizeFieldKey(entry.getKey()), value);
        });
        Map<String, String> values = new LinkedHashMap<String, String>();
        String businessName = null;
        if (columns != null) {
            for (ModelColumnDO column : columns) {
                String value = raw.get(column.getCode());
                if (value == null) {
                    value = raw.get(String.valueOf(column.getId()));
                }
                if (value == null) {
                    value = raw.get(column.getName());
                }
                if (value == null) {
                    value = findNormalizedValue(normalizedRaw, column.getCode());
                }
                if (value == null) {
                    value = findNormalizedValue(normalizedRaw, String.valueOf(column.getId()));
                }
                if (value == null) {
                    value = findNormalizedValue(normalizedRaw, column.getName());
                }
                String text = value == null ? "" : value;
                values.put(column.getCode(), text);
                values.put(String.valueOf(column.getId()), text);
                if (Boolean.TRUE.equals(column.getNameFlag()) && !text.trim().isEmpty()) {
                    businessName = text;
                }
            }
        }
        SourceSnapshot snapshot = new SourceSnapshot();
        snapshot.setSourceRecordId(source.getId());
        snapshot.setVersionId(source.getId());
        snapshot.setSystemId(source.getSourceSystemId());
        snapshot.setStableRecordId(source.getSourceRecordKey());
        snapshot.setBusinessName(businessName);
        snapshot.setTarget(target);
        snapshot.setModelData(source.getModelData());
        snapshot.setReceiveTime(source.getReceiveTime());
        snapshot.setValues(values);
        if (system != null) {
            snapshot.setSystemCode(system.getCode());
            snapshot.setSystemName(system.getName());
            snapshot.setTrustScore(system.getTrustScore() == null ? 0 : system.getTrustScore().intValue());
        }
        return snapshot;
    }

    /**
     * 按规范化后的字段键名读取来源 JSON 值。
     *
     * @param normalizedRaw 已规范化键名的来源数据
     * @param key 模型字段编码、ID 或名称
     * @return 匹配的字段值，未匹配时返回 null
     */
    private static String findNormalizedValue(Map<String, String> normalizedRaw, String key) {
        String normalizedKey = normalizeFieldKey(key);
        return normalizedKey.isEmpty() ? null : normalizedRaw.get(normalizedKey);
    }

    /**
     * 统一来源字段键名，兼容不同系统的大小写、空白、下划线和连字符差异。
     *
     * @param key 原始字段键名
     * @return 规范化后的字段键名
     */
    private static String normalizeFieldKey(String key) {
        return key == null ? "" : key.replaceAll("[\\s_-]", "").toLowerCase(Locale.ROOT);
    }

}
