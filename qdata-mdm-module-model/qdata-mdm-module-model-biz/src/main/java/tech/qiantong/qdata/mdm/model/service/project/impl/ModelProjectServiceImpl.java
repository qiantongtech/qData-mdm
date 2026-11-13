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

package tech.qiantong.qdata.mdm.model.service.project.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelBaseMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.layer.ModelLayerMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.project.ModelProjectMapper;
import tech.qiantong.qdata.mdm.model.service.project.IModelProjectService;
import tech.qiantong.qdata.mdm.rule.api.rule.dto.RuleModelRulesRespDTO;
import tech.qiantong.qdata.mdm.rule.api.service.rule.RuleModelApi;

import javax.annotation.Resource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主数据域Service业务层处理
 *
 * @author qdata
 * @date 2026-09-15
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelProjectServiceImpl extends ServiceImpl<ModelProjectMapper, ModelProjectDO> implements IModelProjectService {
    @Resource
    private ModelProjectMapper modelProjectMapper;

    @Resource
    private ModelBaseMapper modelBaseMapper;

    @Resource
    private ModelLayerMapper modelLayerMapper;

    @Resource
    private RuleModelApi ruleModelApi;

    @Override
    /** 批量判断主数据域是否被模型目录引用。 */ public boolean hasLayerReference(Collection<Long> ids) {
        return ids != null && !ids.isEmpty() && modelLayerMapper.selectCount(new QueryWrapper<ModelLayerDO>().in("project_id", ids)) > 0;
    }

    @Override
    /** 批量判断主数据域是否被主数据模型引用。 */ public boolean hasModelReference(Collection<Long> ids) {
        return ids != null && !ids.isEmpty() && modelBaseMapper.selectCount(new QueryWrapper<ModelBaseDO>().in("project_id", ids)) > 0;
    }

    @Override
    public PageResult<ModelProjectDO> getModelProjectPage(ModelProjectPageReqVO pageReqVO) {
        PageResult<ModelProjectDO> pageResult = modelProjectMapper.selectPage(pageReqVO);
        List<ModelProjectDO> rows = pageResult.getRows();
        if (rows == null || rows.isEmpty()) {
            return pageResult;
        }

        List<Long> projectIds = rows.stream().map(ModelProjectDO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, Long> modelCountMap = new HashMap<>();
        Map<Long, Long> layerCountMap = new HashMap<>();
        Map<Long, Map<String, Long>> ruleCountMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            List<ModelBaseDO> models = modelBaseMapper.selectList(new QueryWrapper<ModelBaseDO>().select("id", "project_id").in("project_id", projectIds));
            modelCountMap = models.stream().filter(model -> model.getProjectId() != null).collect(Collectors.groupingBy(ModelBaseDO::getProjectId, Collectors.counting()));
            ruleCountMap = countProjectRules(models);
            layerCountMap = modelLayerMapper.selectList(new QueryWrapper<ModelLayerDO>().select("project_id").in("project_id", projectIds)).stream().filter(layer -> layer.getProjectId() != null).collect(Collectors.groupingBy(ModelLayerDO::getProjectId, Collectors.counting()));
        }
        for (ModelProjectDO row : rows) {
            row.setModelCount(modelCountMap.getOrDefault(row.getId(), 0L).toString());
            row.setLayerCount(layerCountMap.getOrDefault(row.getId(), 0L).toString());
            Map<String, Long> ruleCounts = ruleCountMap.getOrDefault(row.getId(), Collections.emptyMap());
            row.setMatchRuleCount(ruleCounts.getOrDefault("match", 0L));
            row.setSurvivorRuleCount(ruleCounts.getOrDefault("survivor", 0L));
            row.setQualityRuleCount(ruleCounts.getOrDefault("quality", 0L));
            row.setRuleCount(row.getMatchRuleCount() + row.getSurvivorRuleCount() + row.getQualityRuleCount());
        }
        return pageResult;
    }

    /**
     * 批量读取当前页所有域的模型规则，按模型所属域汇总三类未删除规则。
     */
    private Map<Long, Map<String, Long>> countProjectRules(List<ModelBaseDO> models) {
        Map<Long, Map<String, Long>> counts = new HashMap<>();
        Map<Long, Long> modelProjects = models.stream().filter(model -> model.getId() != null && model.getProjectId() != null).collect(Collectors.toMap(ModelBaseDO::getId, ModelBaseDO::getProjectId, (first, ignored) -> first));
        if (modelProjects.isEmpty()) {
            return counts;
        }
        RuleModelRulesRespDTO rules = ruleModelApi.getModelRules(modelProjects.keySet());
        rules.getMatch().stream().filter(rule -> !Boolean.TRUE.equals(rule.getDelFlag())).forEach(rule -> addRuleCount(counts, modelProjects, rule.getModelId(), "match"));
        rules.getSurvivor().stream().filter(rule -> !Boolean.TRUE.equals(rule.getDelFlag())).forEach(rule -> addRuleCount(counts, modelProjects, rule.getModelId(), "survivor"));
        rules.getQuality().stream().filter(rule -> !Boolean.TRUE.equals(rule.getDelFlag())).forEach(rule -> addRuleCount(counts, modelProjects, rule.getModelId(), "quality"));
        return counts;
    }

    private void addRuleCount(Map<Long, Map<String, Long>> counts, Map<Long, Long> modelProjects, Long modelId, String ruleType) {
        Long projectId = modelProjects.get(modelId);
        if (projectId != null) {
            counts.computeIfAbsent(projectId, ignored -> new HashMap<>()).merge(ruleType, 1L, Long::sum);
        }
    }

    @Override
    public Long createModelProject(ModelProjectSaveReqVO createReqVO) {
        ModelProjectDO dictType = BeanUtils.toBean(createReqVO, ModelProjectDO.class);
        modelProjectMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelProject(ModelProjectSaveReqVO updateReqVO) {
        // 相关校验

        // 更新主数据域
        ModelProjectDO updateObj = BeanUtils.toBean(updateReqVO, ModelProjectDO.class);
        // 显式写入空值，支持清空负责人；状态修改仍使用独立的 updateById。
        return modelProjectMapper.update(updateObj, new UpdateWrapper<ModelProjectDO>().eq("id", updateReqVO.getId()).set(updateReqVO.getResponsibleUserId() == null, "responsible_user_id", null));
    }

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        QueryWrapper<ModelProjectDO> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        if (id != null) wrapper.ne("id", id);
        return modelProjectMapper.selectCount(wrapper) == 0;
    }

    @Override
    public int updateModelProjectStatus(Long id, Boolean status) {
        ModelProjectDO updateObj = new ModelProjectDO();
        updateObj.setId(id);
        updateObj.setValidFlag(status);
        return modelProjectMapper.updateById(updateObj);
    }

    @Override
    public int removeModelProject(Collection<Long> idList) {
        // 批量删除主数据域
        return modelProjectMapper.deleteByIds(idList);
    }

    @Override
    public ModelProjectDO getModelProjectById(Long id) {
        ModelProjectDO project = modelProjectMapper.selectById(id);
        if (project == null) {
            return null;
        }
        long modelCount = modelBaseMapper.selectCount(new QueryWrapper<ModelBaseDO>().eq("project_id", id));
        long layerCount = modelLayerMapper.selectCount(new QueryWrapper<ModelLayerDO>().eq("project_id", id));
        project.setModelCount(String.valueOf(modelCount));
        project.setLayerCount(String.valueOf(layerCount));
        return project;
    }

    @Override
    public List<ModelProjectDO> getModelProjectList() {
        return modelProjectMapper.selectList();
    }
}
