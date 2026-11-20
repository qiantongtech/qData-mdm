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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskMappingVO;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.API;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_FIXED;

/**
 * 数据接入的字段映射执行计划。
 *
 * <p>执行前校验配置并组合来源取值、字段处理和模型类型转换规则，逐行执行时复用已编译规则。
 * 仅向结果写入已选择的目标字段，来源唯一标识始终读取原值。本类不访问数据库。</p>
 *
 * @author chentangwen
 */
public final class FieldMappingPlan {

    /**
     * 按映射配置顺序保存的目标字段及其完整转换规则。
     */
    private final Map<String, Function<Map<String, Object>, Object>> fieldMappings;

    /**
     * 来源字段的原值读取规则，包含未选择目标字段的来源字段。
     */
    private final Map<String, Function<Map<String, Object>, Object>> identityReaders;

    /**
     * 来源字段首次关联的目标字段，仅用于补充唯一标识读取失败时的错误上下文。
     */
    private final Map<String, String> identityTargets;

    private FieldMappingPlan(Map<String, Function<Map<String, Object>, Object>> fieldMappings,
                             Map<String, Function<Map<String, Object>, Object>> identityReaders,
                             Map<String, String> identityTargets) {
        // 复制并冻结规则容器，保留映射顺序，避免计划创建后再被修改。
        this.fieldMappings = Collections.unmodifiableMap(new LinkedHashMap<>(fieldMappings));
        this.identityReaders = Collections.unmodifiableMap(new HashMap<>(identityReaders));
        this.identityTargets = Collections.unmodifiableMap(new HashMap<>(identityTargets));
    }

    /**
     * 校验字段映射并创建可复用的执行计划。
     *
     * <p>未选择目标字段的配置仅保留来源取值器，不编译或执行转换规则。
     * 固定值在编译时完成处理和模型类型转换，不读取来源字段。
     * 同一来源字段可映射到多个目标字段，每个目标字段只能映射一次。</p>
     *
     * @param mappings 来源字段映射配置，不能为空且至少选择一个目标字段
     * @param modelFields 当前模型的业务字段，字段 ID 和编码必须完整且分别唯一
     * @param sourceType 接入来源类型，API 来源额外支持记录内的字段路径
     * @return 按配置顺序执行目标字段转换的映射计划
     * @throws ServiceException 映射配置、模型字段或转换参数不合法时抛出
     */
    public static FieldMappingPlan compile(List<IngestTaskMappingVO> mappings,
                                          List<IngestModelFieldRespVO> modelFields, String sourceType) {
        if (mappings == null || mappings.isEmpty()) {
            throw new ServiceException("请配置字段映射", HttpStatus.WARN);
        }
        // 先校验模型元数据并建立索引，后续只允许引用当前模型中的有效字段。
        Map<Long, IngestModelFieldRespVO> fields = indexModelFields(modelFields);
        Set<Long> usedTargets = new HashSet<>();
        Map<String, Function<Map<String, Object>, Object>> fieldMappings = new LinkedHashMap<>();
        Map<String, Function<Map<String, Object>, Object>> identityReaders = new HashMap<>();
        Map<String, String> identityTargets = new HashMap<>();
        boolean api = API.equals(sourceType);
        for (IngestTaskMappingVO mapping : mappings) {
            if (mapping == null) {
                throw new ServiceException("字段映射配置不能为空", HttpStatus.WARN);
            }
            String source = mapping.getSourceFieldName();
            if (mapping.getTargetFieldId() == null) {
                // 未选目标时跳过转换规则，仅保留来源原值读取能力，供唯一标识使用。
                if (source != null && !source.trim().isEmpty()) {
                    identityReaders.computeIfAbsent(source, key -> compileSourceReader(key, null, api));
                }
                continue;
            }
            // 同一来源可以映射多个目标，但每个目标只能赋值一次，避免结果被覆盖。
            IngestModelFieldRespVO field = fields.get(mapping.getTargetFieldId());
            if (field == null) {
                throw failure("MAPPING_CONFIG", source, null, "选择的目标字段不属于当前模型或已失效");
            }
            String target = field.getColumnCode();
            if (!usedTargets.add(field.getId())) {
                throw failure("MAPPING_CONFIG", source, target, "目标字段被重复映射");
            }
            // 固定值不依赖来源；普通映射复用来源读取器，转换规则仍按各自目标单独编译。
            boolean fixed = TRANSFORM_FIXED.equals(mapping.getTransformType());
            Function<Map<String, Object>, Object> accessor = fixed ? null
                    : identityReaders.computeIfAbsent(source, key -> compileSourceReader(key, target, api));
            fieldMappings.put(target, compileFieldMapping(mapping, field, accessor));
            if (!fixed) {
                // 多个目标共用来源时，保留首次关联的目标，供唯一标识异常定位使用。
                identityTargets.putIfAbsent(source, target);
            }
        }
        if (fieldMappings.isEmpty()) {
            throw new ServiceException("请至少选择一个目标模型字段", HttpStatus.WARN);
        }
        return new FieldMappingPlan(fieldMappings, identityReaders, identityTargets);
    }

    /**
     * 对一条来源记录执行全部目标字段映射。
     *
     * <p>全部字段成功后才返回结果；任一字段失败即抛出异常，不返回部分转换结果，
     * 也不修改传入的来源记录。结果仅包含已配置映射的目标字段。</p>
     *
     * @param source 非空来源记录，字段名或字段路径与映射配置一致
     * @return 按映射配置顺序排列、以模型字段编码为键的转换结果
     * @throws ServiceException 来源记录为空、来源字段缺失或字段转换失败时抛出
     */
    public Map<String, Object> apply(Map<String, Object> source) {
        if (source == null) {
            throw new ServiceException("来源记录不能为空");
        }
        // 逐项写入本地结果，全部转换成功后才返回，避免调用方拿到部分转换结果。
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Function<Map<String, Object>, Object>> entry : fieldMappings.entrySet()) {
            result.put(entry.getKey(), entry.getValue().apply(source));
        }
        return result;
    }

    /**
     * 读取来源唯一标识字段的原始值，不执行字段处理或模型类型转换。
     *
     * @param source 已配置且可读取原值的来源字段名或字段路径
     * @param row 非空来源记录
     * @return 保留原始类型和内容的来源原值；不能为 null，字符串不能全为空白
     * @throws ServiceException 字段未配置、来源记录为空、字段缺失或标识值为空时抛出
     */
    public Object readIdentity(String source, Map<String, Object> row) {
        String target = identityTargets.get(source);
        Function<Map<String, Object>, Object> reader = identityReaders.get(source);
        if (reader == null) {
            throw failure("SOURCE_ID_CONFIG", source, target, "唯一标识必须配置可读取的来源字段");
        }
        // 身份字段直接读取原值，防止去空格、值替换或类型转换改变来源记录身份。
        Object value = readSourceValue(source, target, row, reader);
        // trim 仅用于判空；返回时保留原始内容，包括有效标识中的空格和前导零。
        if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
            throw failure("SOURCE_ID_EMPTY", source, target, "来源记录唯一标识不能为空");
        }
        return value;
    }

    /**
     * 校验模型元数据并按字段 ID 建立索引，防止无效字段或重复编码造成结果覆盖。
     */
    private static Map<Long, IngestModelFieldRespVO> indexModelFields(List<IngestModelFieldRespVO> modelFields) {
        if (modelFields == null || modelFields.isEmpty()) {
            throw new ServiceException("当前模型没有可用的业务字段");
        }
        Map<Long, IngestModelFieldRespVO> fields = new HashMap<>();
        Set<String> columnCodes = new HashSet<>();
        for (IngestModelFieldRespVO field : modelFields) {
            if (field == null || field.getId() == null
                    || field.getColumnCode() == null || field.getColumnCode().trim().isEmpty()) {
                throw new ServiceException("模型字段配置不完整，字段 ID 和编码不能为空");
            }
            // ID 用于定位模型字段，编码用于结果键，两者都必须唯一。
            if (fields.putIfAbsent(field.getId(), field) != null) {
                throw new ServiceException("模型字段 ID 重复：" + field.getId());
            }
            if (!columnCodes.add(field.getColumnCode())) {
                throw new ServiceException("模型字段编码重复：" + field.getColumnCode());
            }
        }
        return fields;
    }

    /**
     * 编译来源取值规则，为已映射字段和仅用于唯一标识的字段提供一致的配置错误上下文。
     */
    private static Function<Map<String, Object>, Object> compileSourceReader(String source, String target, boolean api) {
        try {
            return SourceFieldAccessor.compile(source, api);
        } catch (ServiceException ex) {
            throw failure("MAPPING_CONFIG", source, target, ex.getMessage());
        } catch (RuntimeException ex) {
            throw failure("MAPPING_CONFIG", source, target, "来源字段配置不正确");
        }
    }

    /**
     * 组合单个目标字段的处理和类型转换规则，固定值在此阶段完成计算。
     */
    private static Function<Map<String, Object>, Object> compileFieldMapping(IngestTaskMappingVO mapping,
            IngestModelFieldRespVO field, Function<Map<String, Object>, Object> accessor) {
        String source = mapping.getSourceFieldName();
        String target = field.getColumnCode();
        try {
            Map<String, Object> config = mapping.getTransformConfig() == null
                    ? Collections.emptyMap() : mapping.getTransformConfig();
            // 字段处理完成后仍需执行模型转换，确保最终值符合目标字段的类型、长度和精度约束。
            FieldProcessor processor = FieldProcessors.compile(mapping.getTransformType(), config, field);
            TypeConverter converter = TypeConverters.compile(field, config);
            if (TRANSFORM_FIXED.equals(mapping.getTransformType())) {
                // 固定值提前计算并校验，配置错误在执行前报出，逐行处理时直接复用结果。
                Object fixedValue = converter.convert(processor.process(null));
                return row -> fixedValue;
            }
            return row -> {
                // 来源读取放在转换异常捕获范围之外，区分字段缺失与值转换失败。
                Object raw = readSourceValue(source, target, row, accessor);
                try {
                    return converter.convert(processor.process(raw));
                } catch (ServiceException ex) {
                    throw failure("FIELD_CONVERSION", source, target, ex.getMessage());
                } catch (RuntimeException ex) {
                    throw failure("FIELD_CONVERSION", source, target, "字段处理失败");
                }
            };
        } catch (ServiceException ex) {
            // 此处仅包装规则编译阶段的错误；逐行执行的异常由返回的转换函数处理。
            throw failure("MAPPING_CONFIG", source, target, ex.getMessage());
        } catch (RuntimeException ex) {
            throw failure("MAPPING_CONFIG", source, target, "处理参数格式不正确");
        }
    }

    /**
     * 读取来源原值，统一映射执行和唯一标识读取时的字段缺失提示。
     */
    private static Object readSourceValue(String source, String target, Map<String, Object> row,
                                          Function<Map<String, Object>, Object> reader) {
        if (row == null) {
            throw failure("SOURCE_FIELD_MISSING", source, target, "来源记录不能为空");
        }
        try {
            return reader.apply(row);
        } catch (ServiceException ex) {
            throw failure("SOURCE_FIELD_MISSING", source, target, ex.getMessage());
        } catch (RuntimeException ex) {
            throw failure("SOURCE_FIELD_MISSING", source, target, "来源字段读取失败");
        }
    }

    /**
     * 构造包含错误分类和字段上下文的业务异常，不拼接来源记录值或缺失的目标字段。
     */
    private static ServiceException failure(String code, String source, String target, String reason) {
        StringBuilder message = new StringBuilder("[").append(code).append("] ");
        boolean hasSource = source != null && !source.trim().isEmpty();
        boolean hasTarget = target != null && !target.trim().isEmpty();
        // 固定值可能没有来源字段，未映射的唯一标识可能没有目标字段，仅展示实际存在的上下文。
        if (hasSource) {
            message.append("来源字段「").append(source).append("」");
        }
        if (hasTarget) {
            if (hasSource) {
                message.append("→");
            }
            message.append("目标字段「").append(target).append("」");
        }
        if (hasSource || hasTarget) {
            message.append("：");
        }
        return new ServiceException(message.append(reason).toString(), "MAPPING_CONFIG".equals(code) ? HttpStatus.WARN : HttpStatus.ERROR);
    }
}
