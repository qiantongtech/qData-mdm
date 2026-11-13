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

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskMappingVO;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.*;

/**
 * 执行前编译取值、处理及模型转换规则，行处理期间不再解析配置。
 */
public final class FieldMappingPlan {
    private final List<Function<Map<String, Object>, Object>> readers;
    private final List<String> targets;
    private final Map<String, Function<Map<String, Object>, Object>> identityReaders;
    private final Map<String, String> identityTargets;

    private FieldMappingPlan(List<Function<Map<String, Object>, Object>> readers, List<String> targets,
                             Map<String, Function<Map<String, Object>, Object>> identityReaders,
                             Map<String, String> identityTargets) {
        this.readers = readers;
        this.targets = targets;
        this.identityReaders = identityReaders;
        this.identityTargets = identityTargets;
    }

    /**
     * 编译已选择目标字段的映射；未选择目标时仅保留来源取值器，供唯一标识读取原值。
     *
     * @param mappings 来源字段配置，目标字段可为空
     * @param modelFields 当前模型的业务字段
     * @param sourceType 接入来源类型
     * @return 只向模型写入已选目标字段的执行计划
     */
    public static FieldMappingPlan compile(List<IngestTaskMappingVO> mappings,
                                          List<IngestModelFieldRespVO> modelFields, String sourceType) {
        if (mappings == null || mappings.isEmpty()) throw new ServiceException("请配置字段映射");
        Map<Long, IngestModelFieldRespVO> fields = modelFields.stream()
                .collect(Collectors.toMap(IngestModelFieldRespVO::getId, field -> field));
        Set<Long> used = new HashSet<>();
        List<Function<Map<String, Object>, Object>> readers = new ArrayList<>();
        List<String> targets = new ArrayList<>();
        Map<String, Function<Map<String, Object>, Object>> identityReaders = new HashMap<>();
        Map<String, String> identityTargets = new HashMap<>();
        for (IngestTaskMappingVO mapping : mappings) {
            if (mapping == null) {
                throw new ServiceException("字段映射配置不能为空");
            }
            String source = mapping.getSourceFieldName();
            if (mapping.getTargetFieldId() == null) {
                // 不编译转换规则，也不读取普通未映射字段；唯一标识仍可按来源原值取值。
                if (source != null && !source.trim().isEmpty()) {
                    identityReaders.putIfAbsent(source, SourceFieldAccessor.compile(source, API.equals(sourceType)));
                }
                continue;
            }
            IngestModelFieldRespVO field = fields.get(mapping.getTargetFieldId());
            if (field == null || field.getColumnCode() == null) {
                throw new ServiceException("来源字段「" + source + "」选择的目标字段不属于当前模型或已失效");
            }
            if (!used.add(field.getId())) {
                throw new ServiceException("目标字段「" + field.getColumnCode() + "」被重复映射");
            }
            String target = field.getColumnCode();
            try {
                Map<String, Object> config = mapping.getTransformConfig() == null
                        ? Collections.emptyMap() : mapping.getTransformConfig();
                boolean fixed = TRANSFORM_FIXED.equals(mapping.getTransformType());
                Function<Map<String, Object>, Object> accessor = fixed ? row -> null
                        : SourceFieldAccessor.compile(source, API.equals(sourceType));
                if (!fixed) {
                    identityReaders.putIfAbsent(source, accessor);
                    identityTargets.putIfAbsent(source, target);
                }
                FieldProcessor processor = FieldProcessors.compile(mapping.getTransformType(), config, field);
                TypeConverter converter = TypeConverters.compile(field, config);
                // 固定值在编译阶段完成处理和转换，不依赖任何来源字段。
                Object fixedValue = fixed ? converter.convert(processor.process(null)) : null;
                readers.add(row -> {
                    if (fixed) return fixedValue;
                    Object raw;
                    try { raw = accessor.apply(row); }
                    catch (ServiceException ex) { throw failure("SOURCE_FIELD_MISSING", source, target, ex.getMessage()); }
                    try { return converter.convert(processor.process(raw)); }
                    catch (ServiceException ex) { throw failure("FIELD_CONVERSION", source, target, ex.getMessage()); }
                    catch (RuntimeException ex) { throw failure("FIELD_CONVERSION", source, target, "字段处理失败"); }
                });
                targets.add(target);
            } catch (ServiceException ex) {
                throw failure("MAPPING_CONFIG", source, target, ex.getMessage());
            } catch (RuntimeException ex) {
                throw failure("MAPPING_CONFIG", source, target, "处理参数格式不正确");
            }
        }
        if (targets.isEmpty()) {
            throw new ServiceException("请至少选择一个目标模型字段");
        }
        return new FieldMappingPlan(readers, targets, identityReaders, identityTargets);
    }

    /**
     * 全部字段成功后才返回整行，调用方不会收到半条转换结果。
     */
    public Map<String, Object> apply(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < readers.size(); i++) result.put(targets.get(i), readers.get(i).apply(source));
        return result;
    }

    /**
     * 来源身份始终取原值，复用编译后的取值器及错误上下文，不受轻量处理影响。
     */
    public Object readIdentity(String source, Map<String, Object> row) {
        String target = identityTargets.get(source);
        Function<Map<String, Object>, Object> reader = identityReaders.get(source);
        if (reader == null) throw failure("SOURCE_ID_CONFIG", source, target, "唯一标识必须配置可读取的来源字段");
        Object value;
        try {
            value = reader.apply(row);
        } catch (ServiceException ex) {
            throw failure("SOURCE_FIELD_MISSING", source, target, ex.getMessage());
        }
        if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
            throw failure("SOURCE_ID_EMPTY", source, target, "来源记录唯一标识不能为空");
        }
        return value;
    }

    private static ServiceException failure(String code, String source, String target, String reason) {
        return new ServiceException("[" + code + "] 来源字段「" + (source == null ? "固定值" : source)
                + "」→目标字段「" + target + "」：" + reason);
    }
}
