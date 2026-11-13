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

package tech.qiantong.qdata.mdm.master.service.change.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateReqDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingItemRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingVersionRespDTO;
import tech.qiantong.qdata.mdm.model.enums.ModelEncodingItemTypeEnum;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.model.service.encoding.ModelEncodingApi;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 新增主数据时，使用发布快照中的编码规则生成自动编码字段。 */
@Component
@RequiredArgsConstructor
public class MasterAutomaticEncodingGenerator {
    private final ModelPublishedApi modelPublishedApi;
    private final ModelEncodingApi modelEncodingApi;

    public void generate(MasterModelRespDTO model, Map<String, String> values) {
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(model.getModelId());
        if (structure == null || !Objects.equals(model.getModelId(), structure.getModelId())
                || !Objects.equals(model.getInstanceId(), structure.getInstanceId())
                || !Objects.equals(model.getModelVersionId(), structure.getVersionId())) {
            throw new ServiceException("自动编码配置与当前成功发布版本不一致，请刷新页面后重试");
        }

        for (ModelPublishedColumnRespDTO target : structure.getFields()) {
            if (!"AUTOMATIC".equalsIgnoreCase(target.getEncodingMode())) continue;
            if (target.getEncodingVersionId() == null) {
                throw new ServiceException(target.getName() + "没有绑定已发布的编码方案版本");
            }
            ModelEncodingVersionRespDTO version = structure.getEncodingVersions().stream()
                    .filter(item -> Objects.equals(item.getId(), target.getEncodingVersionId()))
                    .findFirst().orElse(null);
            if (version == null || !"3".equals(version.getStatus())) {
                throw new ServiceException(target.getName() + "绑定的编码方案版本不可用，请重新发布模型");
            }

            List<ModelEncodingItemRespDTO> items = structure.getEncodingItems().stream()
                    .filter(item -> Objects.equals(item.getEncodingVersionId(), version.getId()))
                    .sorted(Comparator.comparing(ModelEncodingItemRespDTO::getSortOrder,
                            Comparator.nullsLast(Long::compareTo)))
                    .collect(Collectors.toList());
            if (items.isEmpty()) throw new ServiceException(target.getName() + "绑定的编码方案没有规则段");

            ModelEncodingGenerateReqDTO request = new ModelEncodingGenerateReqDTO();
            request.setEncodingVersionId(version.getId());
            request.setModelId(model.getModelId());
            for (ModelEncodingItemRespDTO item : items) {
                ModelEncodingItemTypeEnum type = ModelEncodingItemTypeEnum.getByValue(item.getType());
                if (type == null) throw new ServiceException("自动编码规则类型无效：" + item.getType());
                if (type == ModelEncodingItemTypeEnum.BUSINESS_FIELD) {
                    addBusinessField(request, model, values, item);
                } else if (type == ModelEncodingItemTypeEnum.REFERENCE_MAPPING) {
                    addReferenceMapping(request, structure, values, target, item);
                }
            }

            ModelEncodingGenerateRespDTO generated = modelEncodingApi.generate(request);
            if (generated == null || generated.getCode() == null || generated.getCode().trim().isEmpty()) {
                throw new ServiceException(target.getName() + "的编码方案未生成有效编码");
            }
            MasterFieldRespDTO field = findField(model, target.getCode());
            values.put(field == null ? target.getCode() : field.getCode(), generated.getCode());
        }
    }

    private void addBusinessField(ModelEncodingGenerateReqDTO request, MasterModelRespDTO model,
            Map<String, String> values, ModelEncodingItemRespDTO item) {
        String configValue = item.getFixedValue();
        MasterFieldRespDTO field = findField(model, configValue);
        if (field == null) throw new ServiceException("编码规则引用了不存在的业务字段：" + configValue);
        addParameter(request.getBusinessFields(), configValue, fieldValue(values, field.getCode()),
                "编码参数字段 " + field.getName());
    }

    private void addReferenceMapping(ModelEncodingGenerateReqDTO request, ModelPublishedStructureRespDTO structure,
            Map<String, String> values, ModelPublishedColumnRespDTO target, ModelEncodingItemRespDTO item) {
        String configValue = item.getFixedValue();
        Long referenceTypeId;
        try {
            referenceTypeId = Long.valueOf(configValue);
        } catch (RuntimeException ex) {
            throw new ServiceException(target.getName() + "的参考数据映射配置无效");
        }
        List<ModelPublishedColumnRespDTO> bindings = structure.getFields().stream()
                .filter(field -> Objects.equals(field.getDictTypeId(), referenceTypeId))
                .collect(Collectors.toList());
        if (bindings.isEmpty()) {
            throw new ServiceException(target.getName() + "引用的参考数据类型没有绑定到模型字段：" + referenceTypeId);
        }

        String referenceValue = null;
        for (ModelPublishedColumnRespDTO binding : bindings) {
            String value = fieldValue(values, binding.getCode());
            if (value == null || value.trim().isEmpty()) continue;
            if (referenceValue != null && !referenceValue.equals(value)) {
                throw new ServiceException("参考数据类型 " + referenceTypeId + " 绑定了多个不同字段值，无法确定编码映射");
            }
            referenceValue = value;
        }
        addParameter(request.getReferenceMappings(), configValue, referenceValue,
                "参考数据映射 " + referenceTypeId);
    }

    private MasterFieldRespDTO findField(MasterModelRespDTO model, String code) {
        if (code == null) return null;
        return model.getFields().stream()
                .filter(field -> field.getCode() != null && field.getCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }

    private String fieldValue(Map<String, String> values, String code) {
        return values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(code))
                .map(Map.Entry::getValue).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private void addParameter(List<ModelEncodingGenerateReqDTO.FieldValue> parameters,
            String configValue, String value, String label) {
        if (configValue == null || configValue.trim().isEmpty()) {
            throw new ServiceException(label + "缺少规则配置");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new ServiceException(label + "不能为空");
        }
        ModelEncodingGenerateReqDTO.FieldValue parameter = new ModelEncodingGenerateReqDTO.FieldValue();
        parameter.setConfigValue(configValue);
        parameter.setValue(value);
        parameters.add(parameter);
    }
}
