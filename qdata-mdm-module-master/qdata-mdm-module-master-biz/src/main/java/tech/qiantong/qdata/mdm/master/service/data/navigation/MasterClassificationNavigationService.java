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

package tech.qiantong.qdata.mdm.master.service.data.navigation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.*;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterClassificationTreeRespVO.Node;
import tech.qiantong.qdata.mdm.master.dal.dto.data.MasterDataQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceData;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/** 普通模型分类导航。配置只读成功发布结构，范围在目标库解析，筛选在源库执行。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterClassificationNavigationService {
    public static final int MAX_NODES = 2000;
    public static final int MAX_FILTER_VALUES = 500;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private final IMasterDataMetadataService metadataService;
    private final ModelPublishedApi publishedApi;
    private final MasterDataQueryRepository repository;
    private final MasterClassificationPermissionService permissions;

    /** 一次返回所有分类分组，同一目标模型的配置和原始节点在本次请求内复用。 */
    public List<MasterClassificationFieldRespVO> navigation(Long instanceId, Long modelId, Long versionId) {
        MasterModelRespDTO source = source(instanceId, modelId, versionId);
        List<MasterClassificationFieldRespVO> result = new ArrayList<>();
        Map<Long, MasterClassificationFieldRespVO> items = new HashMap<>();
        Map<Long, List<MasterFieldRespDTO>> targets = new LinkedHashMap<>();
        for (MasterFieldRespDTO field : source.getFields().stream()
                .filter(f -> "CLASS".equals(f.getCategory()))
                .sorted(Comparator.comparing((MasterFieldRespDTO f) -> f.getSortOrder() == null ? 0 : f.getSortOrder())
                        .thenComparing(MasterFieldRespDTO::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList())) {
            MasterClassificationFieldRespVO item = new MasterClassificationFieldRespVO();
            item.setId(String.valueOf(field.getId()));
            item.setCode(field.getCode());
            item.setLabel(field.getName() == null ? field.getCode() : field.getName());
            result.add(item);
            items.put(field.getId(), item);
            try {
                permissions.requireView(source, field);
                if (field.getRelatedModelId() == null) throw new ServiceException("分类字段缺少目标模型");
                targets.computeIfAbsent(field.getRelatedModelId(), id -> new ArrayList<>()).add(field);
            } catch (ServiceException error) {
                item.setMessage(error.getMessage());
            }
        }
        for (List<MasterFieldRespDTO> fields : targets.values()) fillNavigation(fields, items);
        return result;
    }

    private void fillNavigation(List<MasterFieldRespDTO> fields, Map<Long, MasterClassificationFieldRespVO> items) {
        Long targetModelId = fields.get(0).getRelatedModelId();
        try {
            MasterModelRespDTO target = metadataService.getPublishedDefinition(null, targetModelId, true);
            List<MasterFieldRespDTO> available = new ArrayList<>();
            Set<String> columns = new LinkedHashSet<>();
            for (MasterFieldRespDTO field : fields) {
                MasterClassificationFieldRespVO item = items.get(field.getId());
                try {
                    validateTarget(target, field);
                    item.setTargetModelId(String.valueOf(target.getModelId()));
                    item.setTargetModelName(target.getModelName());
                    item.setTargetInstanceId(String.valueOf(target.getInstanceId()));
                    item.setTargetVersionId(String.valueOf(target.getModelVersionId()));
                    available.add(field);
                    columns.addAll(treeColumns(field));
                } catch (ServiceException error) {
                    item.setMessage(error.getMessage());
                }
            }
            if (available.isEmpty()) return;
            List<Map<String, Object>> rows = loadRows(target, columns);
            for (MasterFieldRespDTO field : available) {
                MasterClassificationFieldRespVO item = items.get(field.getId());
                try {
                    MasterClassificationTreeRespVO tree = buildTree(target, field, rows);
                    item.setNodes(tree.getNodes());
                    item.setRevision(tree.getRevision());
                    item.setAvailable(true);
                } catch (ServiceException error) {
                    item.setMessage(error.getMessage());
                }
            }
            requireCurrent(target);
        } catch (RuntimeException error) {
            String message = error instanceof ServiceException ? error.getMessage() : "分类树读取失败，请稍后重试";
            if (!(error instanceof ServiceException)) log.warn("读取分类导航失败，targetModelId={}", targetModelId, error);
            for (MasterFieldRespDTO field : fields) {
                MasterClassificationFieldRespVO item = items.get(field.getId());
                item.setAvailable(false);
                item.setNodes(Collections.emptyList());
                item.setRevision(null);
                if (item.getMessage() == null) item.setMessage(message);
            }
        }
    }

    /** 维护控件直接访问目标分类模型，关联键/展示列仍必须属于该成功发布模型并具有查看权限。 */
    public MasterClassificationTreeRespVO options(Long instanceId, MasterClassificationOptionsReqVO request) {
        MasterModelRespDTO target = metadataService.getPublishedDefinition(instanceId, request.getModelId(), true);
        if (!Objects.equals(request.getModelVersionId(), target.getModelVersionId()))
            throw new ServiceException("分类目标版本已变化，请重新展开选择器");
        MasterFieldRespDTO field = new MasterFieldRespDTO();
        field.setRelatedCode(request.getRelatedCode());
        field.setDisplayCode(request.getDisplayCode());
        field.setNodeRule(request.getNodeRule());
        field.setDisplayMode(request.getDisplayMode());
        validateTarget(target, field);
        return loadTree(target, field);
    }

    /** 全部记录不传分类上下文；部分上下文或过期上下文一律报错，不能扩大为全量。 */
    public void applyFilter(MasterModelRespDTO source, MasterDataPageReqVO request, MasterDataQueryDTO query) {
        boolean any = request.getClassificationFieldId() != null || request.getClassificationNodeId() != null
                || request.getClassificationTargetVersionId() != null || request.getClassificationRevision() != null;
        if (!any) return;
        if (request.getModelId() == null || request.getModelVersionId() == null
                || request.getClassificationFieldId() == null || request.getClassificationNodeId() == null
                || request.getClassificationNodeId().isEmpty() || request.getClassificationTargetVersionId() == null
                || request.getClassificationRevision() == null)
            throw new ServiceException("分类筛选上下文不完整，请重新选择分类");
        if (!"NORMAL".equals(source.getModelType())) throw new ServiceException("分类导航只支持普通模型");
        MasterFieldRespDTO field = classificationField(source, request.getClassificationFieldId());
        permissions.requireView(source, field);
        MasterModelRespDTO target = target(field);
        if (!Objects.equals(request.getClassificationTargetVersionId(), target.getModelVersionId()))
            throw new ServiceException("分类目标版本已变化，请刷新分类树");
        MasterClassificationTreeRespVO tree = loadTree(target, field);
        if (!tree.getRevision().equals(request.getClassificationRevision()))
            throw new ServiceException("分类数据已变化，请刷新分类树后重新查询");
        Node selected = find(tree.getNodes(), request.getClassificationNodeId());
        if (selected == null) throw new ServiceException("分类节点不存在，请刷新分类树");
        MasterFieldRespDTO key = field(target, field.getRelatedCode());
        Set<Object> values = new LinkedHashSet<>();
        collectValues(selected, values, field, key);
        query.setClassificationColumn(field.getCode());
        query.setClassificationValues(values);
    }

    private MasterModelRespDTO source(Long instanceId, Long modelId, Long versionId) {
        if (instanceId == null || instanceId <= 0) throw new ServiceException("模型实例ID格式无效");
        MasterModelRespDTO source = metadataService.getPublishedDefinition(instanceId, modelId, false);
        if (versionId == null || !versionId.equals(source.getModelVersionId()))
            throw new ServiceException("分类导航与成功发布版本不一致，请重新进入模型");
        if (!"NORMAL".equals(source.getModelType())) throw new ServiceException("分类导航只支持普通模型");
        return source;
    }

    private MasterFieldRespDTO classificationField(MasterModelRespDTO model, Long id) {
        return model.getFields().stream().filter(f -> id != null && id.equals(f.getId()) && "CLASS".equals(f.getCategory()))
                .findFirst().orElseThrow(() -> new ServiceException("分类字段不属于当前已发布模型"));
    }

    private MasterModelRespDTO target(MasterFieldRespDTO field) {
        if (field.getRelatedModelId() == null) throw new ServiceException("分类字段缺少目标模型");
        MasterModelRespDTO target = metadataService.getPublishedDefinition(null, field.getRelatedModelId(), true);
        validateTarget(target, field);
        return target;
    }

    private void validateTarget(MasterModelRespDTO target, MasterFieldRespDTO config) {
        if (!"CATEGORY".equals(target.getModelType())) throw new ServiceException("分类字段目标不是分类模型");
        MasterFieldRespDTO key = field(target, config.getRelatedCode());
        MasterFieldRespDTO display = field(target, config.getDisplayCode());
        if (!"ID".equalsIgnoreCase(key.getCode()) && !Boolean.TRUE.equals(key.getUnique()))
            throw new ServiceException("分类关联键必须为ID或已发布的唯一字段");
        if (!stringType(key.getDataType()) && !numberType(key.getDataType()))
            throw new ServiceException("分类关联键仅支持字符串或数值类型");
        permissions.requireView(target, key, display);
    }

    private MasterFieldRespDTO field(MasterModelRespDTO model, String code) {
        if (code == null) throw new ServiceException("分类字段缺少关联键或展示字段");
        return model.getFields().stream().filter(f -> code.equalsIgnoreCase(f.getCode()))
                .findFirst().orElseThrow(() -> new ServiceException("分类配置字段未成功发布：" + code));
    }

    private MasterClassificationTreeRespVO loadTree(MasterModelRespDTO target, MasterFieldRespDTO config) {
        MasterClassificationTreeRespVO result = buildTree(target, config, loadRows(target, treeColumns(config)));
        requireCurrent(target);
        return result;
    }

    private Set<String> treeColumns(MasterFieldRespDTO config) {
        return new LinkedHashSet<>(Arrays.asList("ID", "CATEGORY_ID", "STATUS",
                config.getRelatedCode().toUpperCase(Locale.ROOT), config.getDisplayCode().toUpperCase(Locale.ROOT)));
    }

    private List<Map<String, Object>> loadRows(MasterModelRespDTO target, Collection<String> columns) {
        List<Map<String, Object>> rows = repository.selectClassificationNodes(target.getDatasourceId(),
                MaintenanceData.table(target), target.getColumnMappings(), columns, MAX_NODES);
        if (rows.size() > MAX_NODES) throw new ServiceException("分类树超过2000个节点，请调整分类规模");
        return rows;
    }

    private MasterClassificationTreeRespVO buildTree(MasterModelRespDTO target, MasterFieldRespDTO config,
            List<Map<String, Object>> rows) {
        MasterFieldRespDTO keyField = field(target, config.getRelatedCode());
        MasterFieldRespDTO displayField = field(target, config.getDisplayCode());
        Map<String, Node> nodes = new LinkedHashMap<>();
        Map<String, String> parents = new LinkedHashMap<>();
        Set<Object> keys = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String id = MaintenanceData.text(MaintenanceData.value(row, "ID"));
            if (id == null || !id.matches("[1-9]\\d{0,18}")) throw new ServiceException("分类节点ID无效");
            Object rawKey = MaintenanceData.value(row, keyField.getCode());
            String key = MaintenanceData.fieldText(rawKey, keyField.getDataType());
            if (key == null || key.trim().isEmpty()) throw new ServiceException("分类关联键存在空值");
            Object typedKey = typedKey(key, keyField.getDataType());
            if (!keys.add(typedKey)) throw new ServiceException("分类关联键存在重复数据");
            Node node = new Node();
            node.setId(id);
            node.setValue(key);
            node.setLabel(MaintenanceData.fieldText(MaintenanceData.value(row, displayField.getCode()), displayField.getDataType()));
            if (node.getLabel() == null || node.getLabel().trim().isEmpty()) throw new ServiceException("分类展示字段存在空值");
            node.setStatus(MaintenanceData.text(MaintenanceData.value(row, "STATUS")));
            if (nodes.put(id, node) != null) throw new ServiceException("分类节点ID重复");
            String parent = MaintenanceData.text(MaintenanceData.value(row, "CATEGORY_ID"));
            parents.put(id, parent == null || parent.isEmpty() || "0".equals(parent) ? null : parent);
        }
        MasterClassificationTreeRespVO result = new MasterClassificationTreeRespVO();
        result.setTargetModelId(String.valueOf(target.getModelId()));
        result.setTargetInstanceId(String.valueOf(target.getInstanceId()));
        result.setTargetVersionId(String.valueOf(target.getModelVersionId()));
        for (Node node : nodes.values()) {
            String parent = parents.get(node.getId());
            if (parent == null) result.getNodes().add(node);
            else {
                if (!nodes.containsKey(parent)) throw new ServiceException("分类数据存在无效的父节点");
                nodes.get(parent).getChildren().add(node);
            }
        }
        Set<String> visited = new HashSet<>();
        for (Node root : result.getNodes()) decorate(root, "", config, visited, 1);
        if (visited.size() != nodes.size()) throw new ServiceException("分类数据存在循环引用或自引用");
        byte[] payload = MaintenanceData.json(result).getBytes(StandardCharsets.UTF_8);
        if (payload.length > MAX_RESPONSE_BYTES) throw new ServiceException("分类树响应超过2MB，请缩小分类规模或展示字段长度");
        result.setRevision(digest(payload));
        return result;
    }

    private void requireCurrent(MasterModelRespDTO target) {
        // 读取期间重新发布不能混用旧字段与新目标表。
        if (!publishedApi.isPublishedInstance(target.getModelId(), target.getInstanceId(), target.getModelVersionId()))
            throw new ServiceException("分类目标版本已变化，请刷新分类树");
    }

    private void decorate(Node node, String parentPath, MasterFieldRespDTO config, Set<String> visited, int depth) {
        if (depth > MAX_DEPTH) throw new ServiceException("分类树层级超过64层");
        if (!visited.add(node.getId())) throw new ServiceException("分类数据存在循环引用");
        String name = node.getLabel();
        node.setPath(parentPath.isEmpty() ? name : parentPath + " / " + name);
        if ("FULL_PATH".equals(config.getDisplayMode())) node.setLabel(node.getPath());
        node.setAssignable("1".equals(node.getStatus())
                && (!"LEAF".equals(config.getNodeRule()) || node.getChildren().isEmpty()));
        for (Node child : node.getChildren()) decorate(child, node.getPath(), config, visited, depth + 1);
    }

    private Node find(List<Node> nodes, String id) {
        for (Node node : nodes) {
            if (node.getId().equals(id)) return node;
            Node child = find(node.getChildren(), id);
            if (child != null) return child;
        }
        return null;
    }

    private void collectValues(Node node, Set<Object> values, MasterFieldRespDTO source, MasterFieldRespDTO targetKey) {
        if (stringType(source.getDataType()) != stringType(targetKey.getDataType())
                || (!stringType(source.getDataType()) && !numberType(source.getDataType())))
            throw new ServiceException("普通模型分类字段与分类关联键类型不兼容");
        values.add(typedKey(node.getValue(), source.getDataType()));
        if (values.size() > MAX_FILTER_VALUES) throw new ServiceException("分类筛选范围超过500个节点，请选择更小范围");
        for (Node child : node.getChildren()) collectValues(child, values, source, targetKey);
    }

    private Object typedKey(String value, String type) {
        try {
            if (stringType(type)) return value;
            BigDecimal number = new BigDecimal(value);
            if (type.toUpperCase(Locale.ROOT).matches("BIGINT|INTEGER|SMALLINT|INT|TINYINT")) return number.longValueExact();
            return number.stripTrailingZeros();
        } catch (RuntimeException error) {
            throw new ServiceException("分类关联键不能转换为已发布字段类型：" + type);
        }
    }

    private boolean stringType(String type) {
        return type != null && type.toUpperCase(Locale.ROOT).matches("STRING|VARCHAR2?|N?CHAR|NVARCHAR2?|TEXT|CLOB");
    }

    private boolean numberType(String type) {
        return type != null && type.toUpperCase(Locale.ROOT).matches("BIGINT|INTEGER|SMALLINT|INT|TINYINT|DECIMAL|NUMBER|NUMERIC|FLOAT|DOUBLE|REAL");
    }

    private String digest(byte[] value) {
        try {
            StringBuilder result = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(value)) result.append(String.format("%02x", b & 0xff));
            return result.toString();
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
}
