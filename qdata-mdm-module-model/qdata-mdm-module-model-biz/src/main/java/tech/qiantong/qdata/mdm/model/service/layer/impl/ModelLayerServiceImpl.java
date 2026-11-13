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

package tech.qiantong.qdata.mdm.model.service.layer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelBaseMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.layer.ModelLayerMapper;
import tech.qiantong.qdata.mdm.model.service.layer.IModelLayerService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型目录层级Service业务层处理
 *
 * @author qdata
 * @date 2026-09-15
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelLayerServiceImpl extends ServiceImpl<ModelLayerMapper, ModelLayerDO> implements IModelLayerService {
    private static final int MAX_LAYER_DEPTH = 5;

    @Resource
    private ModelLayerMapper modelLayerMapper;

    @Resource
    private ModelBaseMapper modelBaseMapper;

    @Override
    public PageResult<ModelLayerDO> getModelLayerPage(ModelLayerPageReqVO pageReqVO) {
        PageResult<ModelLayerDO> page = modelLayerMapper.selectPage(pageReqVO);
        List<ModelLayerDO> rows = page.getRows();
        if (rows == null || rows.isEmpty()) return page;
        fillModelCounts(rows);
        return page;
    }

    /** 统计始终读取完整域目录树，筛选负责人、名称或直接子级时也保留后代模型数量。 */
    private void fillModelCounts(List<ModelLayerDO> rows) {
        List<Long> projectIds = rows.stream().map(ModelLayerDO::getProjectId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            rows.forEach(row -> row.setModelCount("0"));
            return;
        }
        List<ModelLayerDO> allLayers = modelLayerMapper.selectList(new QueryWrapper<ModelLayerDO>()
                .select("id", "parent_id", "project_id").in("project_id", projectIds));
        List<Long> layerIds = allLayers.stream().map(ModelLayerDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (layerIds.isEmpty()) {
            rows.forEach(row -> row.setModelCount("0"));
            return;
        }
        Map<Long, Long> directCounts = modelBaseMapper.selectList(new QueryWrapper<ModelBaseDO>()
                        .select("layer_id").in("layer_id", layerIds))
                .stream().filter(model -> model.getLayerId() != null)
                .collect(Collectors.groupingBy(ModelBaseDO::getLayerId, Collectors.counting()));
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        allLayers.stream().filter(row -> row.getId() != null && row.getParentId() != null)
                .forEach(row -> childrenMap.computeIfAbsent(row.getParentId(), key -> new ArrayList<>()).add(row.getId()));
        Map<Long, Long> aggregateCounts = new HashMap<>();
        rows.forEach(row -> row.setModelCount(String.valueOf(countModels(
                row.getId(), childrenMap, directCounts, aggregateCounts, new HashSet<>()))));
    }

    @Override
    public Long createModelLayer(ModelLayerSaveReqVO createReqVO) {
        validateHierarchy(createReqVO, null);
        ModelLayerDO dictType = BeanUtils.toBean(createReqVO, ModelLayerDO.class);
        modelLayerMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelLayer(ModelLayerSaveReqVO updateReqVO) {
        ModelLayerDO existing = modelLayerMapper.selectById(updateReqVO.getId());
        if (existing == null) throw new ServiceException("模型目录不存在");
        if (updateReqVO.getProjectId() == null) updateReqVO.setProjectId(existing.getProjectId());
        if (updateReqVO.getParentId() == null) updateReqVO.setParentId(existing.getParentId());
        if (!Objects.equals(existing.getProjectId(), updateReqVO.getProjectId())) {
            throw new ServiceException("模型目录不能跨主数据域移动");
        }
        validateHierarchy(updateReqVO, existing.getId());

        // 更新模型目录层级
        ModelLayerDO updateObj = BeanUtils.toBean(updateReqVO, ModelLayerDO.class);
        return modelLayerMapper.update(updateObj, new UpdateWrapper<ModelLayerDO>()
                .eq("id", updateReqVO.getId())
                .set(updateReqVO.getResponsibleUserId() == null, "responsible_user_id", null));
    }

    /** 顶级节点不计层数；移动目录时连同其所有后代一起校验。 */
    private void validateHierarchy(ModelLayerSaveReqVO request, Long editingId) {
        if (request.getProjectId() == null) throw new ServiceException("所属主数据域不能为空");
        if (request.getParentId() == null) request.setParentId(0L);
        List<ModelLayerDO> layers = modelLayerMapper.selectList(new QueryWrapper<ModelLayerDO>()
                .select("id", "parent_id", "project_id").eq("project_id", request.getProjectId()));
        Map<Long, ModelLayerDO> byId = new HashMap<>();
        Map<Long, List<Long>> children = new HashMap<>();
        for (ModelLayerDO layer : layers) {
            byId.put(layer.getId(), layer);
            children.computeIfAbsent(layer.getParentId(), key -> new ArrayList<>()).add(layer.getId());
        }
        int parentDepth = 0;
        Long parentId = request.getParentId();
        Set<Long> visited = new HashSet<>();
        if (editingId != null) visited.add(editingId);
        while (parentId != null && parentId != 0L) {
            if (!visited.add(parentId)) throw new ServiceException("上级模型目录不能为自身或子目录，且不能存在循环关系");
            ModelLayerDO parent = byId.get(parentId);
            if (parent == null) throw new ServiceException("上级模型目录不存在或不属于当前主数据域");
            parentDepth++;
            if (parentDepth >= MAX_LAYER_DEPTH) throw new ServiceException("模型目录最多支持 5 级");
            parentId = parent.getParentId();
        }
        validateSubtreeDepth(editingId, parentDepth + 1, children, new HashSet<>());
    }

    private void validateSubtreeDepth(Long id, int depth, Map<Long, List<Long>> children, Set<Long> visited) {
        if (depth > MAX_LAYER_DEPTH) throw new ServiceException("模型目录最多支持 5 级，含子目录的层级不能超过 5 级");
        if (id == null) return;
        if (!visited.add(id)) throw new ServiceException("模型目录不能存在循环关系");
        for (Long childId : children.getOrDefault(id, Collections.emptyList())) {
            validateSubtreeDepth(childId, depth + 1, children, visited);
        }
    }

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        QueryWrapper<ModelLayerDO> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        if (id != null) wrapper.ne("id", id);
        return modelLayerMapper.selectCount(wrapper) == 0;
    }

    /**
     * 批量判断模型目录是否被主数据模型引用。
     */
    @Override
    public boolean hasModelReference(Collection<Long> ids) {
        return ids != null && !ids.isEmpty()
                && modelBaseMapper.selectCount(new QueryWrapper<ModelBaseDO>().in("layer_id", ids)) > 0;
    }

    @Override
    public int updateModelLayerStatus(Long id, Boolean status) {
        ModelLayerDO updateObj = new ModelLayerDO();
        updateObj.setId(id);
        updateObj.setValidFlag(status);
        return modelLayerMapper.updateById(updateObj);
    }

    @Override
    public int removeModelLayer(Long id) {
        // 单独删除模型目录层级
        return modelLayerMapper.deleteById(id);
    }

    @Override
    public ModelLayerDO getModelLayerById(Long id) {
        ModelLayerDO layer = modelLayerMapper.selectById(id);
        if (layer != null) fillModelCounts(Collections.singletonList(layer));
        return layer;
    }

    @Override
    public List<ModelLayerDO> getModelLayerList() {
        return modelLayerMapper.selectList();
    }

    /**
     * 批量判断模型目录是否存在子目录。
     */
    @Override
    public boolean hasChildByModelLayerId(Collection<Long> ids) {
        return ids != null && !ids.isEmpty()
                && modelLayerMapper.selectCount(new QueryWrapper<ModelLayerDO>().in("parent_id", ids)) > 0;
    }

    /**
     * 统计当前目录及其所有后代目录关联的模型数量。
     */
    private long countModels(Long layerId, Map<Long, List<Long>> childrenMap,
                             Map<Long, Long> directCounts, Map<Long, Long> aggregateCounts,
                             Set<Long> visiting) {
        if (layerId == null) return 0L;
        Long cached = aggregateCounts.get(layerId);
        if (cached != null) return cached;
        // 防止异常的循环父子关系导致无限递归。
        if (!visiting.add(layerId)) return 0L;

        long count = directCounts.getOrDefault(layerId, 0L);
        for (Long childId : childrenMap.getOrDefault(layerId, new ArrayList<>())) {
            count += countModels(childId, childrenMap, directCounts, aggregateCounts, visiting);
        }
        visiting.remove(layerId);
        aggregateCounts.put(layerId, count);
        return count;
    }
}
