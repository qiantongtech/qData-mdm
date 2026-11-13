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

package tech.qiantong.qdata.mdm.master.service.data.reference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterClassificationOptionsReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterClassificationTreeRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterMaintenanceColumnRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataMetadataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterMaintenanceFormConfigRespVO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.service.data.navigation.MasterClassificationNavigationService;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 导入、导出和列表共用发布配置、分类规则以及编码与名称映射。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterReferenceService {
    private static final int MAX_OPTIONS = 10000;
    private final IMasterDataMetadataService metadataService;
    private final MasterClassificationNavigationService classifications;
    private final ModelPublishedApi publishedApi;

    /** 显示映射只附在列定义上，列表和导入预览直接共用，不包装或替换业务行。 */
    public void enrich(MasterDataMetadataRespVO metadata) {
        try {
            enrich(metadata.getColumns(), metadataService.getMaintenanceFormConfig(metadata.getInstanceId(),
                    metadata.getModelId(), metadata.getModelVersionId()).getColumns());
        } catch (ServiceException error) {
            metadata.getColumns().forEach(column -> column.setValueLabels(null));
            log.warn("引用字段显示配置不可用，modelId={}：{}", metadata.getModelId(), error.getMessage());
        }
    }

    /** 复用已加载的维护配置，为导入预览补充显示映射。 */
    public void enrich(MasterMaintenanceFormConfigRespVO config) {
        enrich(config.getColumns(), config.getColumns());
    }

    private void enrich(List<? extends MasterColumnRespVO> columns, List<MasterMaintenanceColumnRespVO> definitions) {
        Map<String, ReferenceValues> choices = load(definitions,
                columns.stream().map(MasterColumnRespVO::getProp).collect(Collectors.toSet()), true);
        columns.forEach(column -> {
            ReferenceValues values = choices.get(column.getProp());
            column.setValueLabels(values == null ? null : values.labels);
        });
    }

    public Map<String, ReferenceValues> load(MasterModelRespDTO model) {
        return load(model, null);
    }

    /** 为申请快照提供显示映射，单个引用字段不可用时保留其原始值。 */
    public Map<String, Map<String, String>> displayLabels(MasterModelRespDTO model) {
        Map<String, ReferenceValues> choices = load(metadataService.getMaintenanceFormConfig(
                model.getInstanceId(), model.getModelId(), model.getModelVersionId()).getColumns(), null, true);
        Map<String, Map<String, String>> labels = new LinkedHashMap<>();
        choices.forEach((code, values) -> labels.put(code, new LinkedHashMap<>(values.labels)));
        return labels;
    }

    /** 按字段编码加载导入导出映射；fields 为 null 时加载全部引用字段。 */
    public Map<String, ReferenceValues> load(MasterModelRespDTO model, Collection<String> fields) {
        return load(metadataService.getMaintenanceFormConfig(model.getInstanceId(),
                model.getModelId(), model.getModelVersionId()).getColumns(),
                fields == null ? null : new HashSet<>(fields), false);
    }

    /** 仅显示时允许单个字段失败并回退原值；导入导出遇到配置错误则直接报错。 */
    private Map<String, ReferenceValues> load(List<MasterMaintenanceColumnRespVO> columns,
            Set<String> fields, boolean displayOnly) {
        Map<String, ReferenceValues> result = new LinkedHashMap<>();
        for (MasterMaintenanceColumnRespVO column : columns) {
            if (fields != null && !fields.contains(column.getProp())) continue;
            if (!"CLASS".equalsIgnoreCase(column.getColumnCategory()) && column.getDictTypeId() == null) continue;
            try {
                result.put(column.getProp(), choices(column));
            } catch (ServiceException error) {
                if (!displayOnly) throw error;
                log.warn("引用字段不可用，field={}，回退原值：{}", column.getProp(), error.getMessage());
            }
        }
        return result;
    }

    private ReferenceValues choices(MasterMaintenanceColumnRespVO column) {
        Map<String, String> labels = new LinkedHashMap<>();
        Set<String> selectable = new HashSet<>();
        if ("CLASS".equalsIgnoreCase(column.getColumnCategory())) {
            ModelPublishedRespDTO target = publishedApi.getPublishedModel(column.getRelatedModelId());
            if (target == null) throw new ServiceException("分类目标模型未成功发布：" + column.getLabel());
            MasterClassificationOptionsReqVO request = new MasterClassificationOptionsReqVO();
            request.setModelId(target.getId());
            request.setModelVersionId(target.getVersionId());
            request.setRelatedCode(column.getRelatedCode());
            request.setDisplayCode(column.getDisplayCode());
            request.setNodeRule(column.getNodeRule());
            request.setDisplayMode("FULL_PATH");
            collect(classifications.options(target.getInstanceId(), request).getNodes(), labels, selectable);
        } else {
            for (MasterMaintenanceColumnRespVO.ReferenceOption option : column.getReferenceOptions()) {
                put(labels, option.getValue(), option.getLabel());
                selectable.add(option.getValue());
            }
        }
        if (labels.size() > MAX_OPTIONS) throw new ServiceException("下拉选项超过" + MAX_OPTIONS + "项：" + column.getLabel());
        return new ReferenceValues(labels, selectable);
    }

    /** 分类显示保留完整路径，只有可分配节点进入下拉和导入选项。 */
    private void collect(List<MasterClassificationTreeRespVO.Node> nodes, Map<String, String> labels,
            Set<String> selectable) {
        for (MasterClassificationTreeRespVO.Node node : nodes) {
            put(labels, node.getValue(), node.getPath());
            if (node.isAssignable()) selectable.add(node.getValue());
            collect(node.getChildren(), labels, selectable);
        }
    }

    private void put(Map<String, String> labels, String value, String label) {
        if (value == null || label == null || labels.putIfAbsent(value, label) != null)
            throw new ServiceException("参考数据或分类关联键重复，无法生成下拉选项");
    }

    /** 同一份映射用于导出显示、导入还原和生成下拉，重复名称以编码区分。 */
    public static class ReferenceValues {
        /** 存储值 → 显示名称，包含仅展示、不可选择的分类节点。 */
        private final Map<String, String> labels = new LinkedHashMap<>();
        /** 可选名称 → 存储值，保留发布配置中的选项顺序。 */
        private final Map<String, String> values = new LinkedHashMap<>();
        private final Set<String> selectableValues = new HashSet<>();

        public ReferenceValues(Map<String, String> names, Set<String> selectable) {
            Map<String, Integer> counts = new HashMap<>();
            names.values().forEach(name -> counts.merge(name, 1, Integer::sum));
            Set<String> used = new HashSet<>();
            names.forEach((value, name) -> {
                String label = counts.get(name) > 1 ? name + "（" + value + "）" : name;
                while (!used.add(label)) label += "（" + value + "）";
                labels.put(value, label);
                if (selectable.contains(value)) {
                    values.put(label, value);
                    selectableValues.add(value);
                }
            });
        }

        /** 未知编码保留原值，避免已有数据在列表或导出中丢失显示。 */
        public String display(Object value) {
            if (value == null) return "";
            String code = String.valueOf(value);
            return labels.getOrDefault(code, code);
        }

        public Map<String, String> options() {
            return values;
        }

        /** 优先使用文件绑定还原编码，并以当前可选值校验；未命中的原值交给后续字段校验。 */
        public String resolve(String input, Map<String, String> bindings) {
            if (input == null || input.isEmpty()) return input;
            if (bindings != null && bindings.containsKey(input)) {
                String code = bindings.get(input);
                if (!selectableValues.contains(code)) throw new ServiceException("选项已失效，请重新下载文件并选择有效值：" + input);
                return code;
            }
            String code = values.get(input);
            if (code != null) {
                if (!code.equals(input) && selectableValues.contains(input))
                    throw new ServiceException("名称与编码存在歧义，请使用新下载文件的下拉选项：" + input);
                return code;
            }
            return input;
        }
    }
}
