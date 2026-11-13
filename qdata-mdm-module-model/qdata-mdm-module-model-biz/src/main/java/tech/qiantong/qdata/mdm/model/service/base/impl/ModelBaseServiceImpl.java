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

package tech.qiantong.qdata.mdm.model.service.base.impl;

import tech.qiantong.qdata.mdm.model.util.ModelPublishUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePublishRespVO;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.*;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelVersionDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelBaseMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelInstanceMapper;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedTableDefinition;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishContext;
import tech.qiantong.qdata.mdm.model.enums.ModelColumnCategoryEnum;
import tech.qiantong.qdata.mdm.model.enums.ModelRelationTypeEnum;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishStatus;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;
import tech.qiantong.qdata.mdm.model.service.base.IModelBaseService;
import tech.qiantong.qdata.mdm.model.service.base.IModelColumnService;
import tech.qiantong.qdata.mdm.model.service.base.IModelPublishService;
import tech.qiantong.qdata.mdm.model.service.base.IModelVersionService;
import tech.qiantong.qdata.mdm.model.service.layer.IModelLayerService;
import tech.qiantong.qdata.mdm.model.service.project.IModelProjectService;
import tech.qiantong.qdata.mdm.model.util.ModelVersionChangeDataUtils;
import tech.qiantong.qdata.mdm.model.util.VersionNoUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * 主数据模型基本信息Service业务层处理
 *
 * @author qdata
 * @date 2026-09-18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelBaseServiceImpl extends ServiceImpl<ModelBaseMapper, ModelBaseDO> implements IModelBaseService {
    private static final int LIST_BATCH_SIZE = 500;

    @Resource
    private ModelBaseMapper modelBaseMapper;

    /**
     * 批量操作经由当前服务的Spring代理调用单条方法，保留每个模型独立的事务。
     */
    @Resource
    @Lazy
    private IModelBaseService modelBaseService;


    @Resource
    private IModelVersionService iModelVersionService;

    @Resource
    private IModelLayerService iModelLayerService;

    @Resource
    private IModelColumnService iModelColumnService;

    /**
     * 恢复已逻辑删除的原字段记录，避免同名字段生成新的ID。
     */
    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Resource
    private IModelProjectService iModelProjectService;

    /**
     * 统一发起模型发布，并读取上次成功发布的配置和结构。
     */
    @Resource
    private IModelPublishService modelPublishService;

    /**
     * 读取上次成功发布记录，首次发布也有记录，不依赖版本是否存在上一版本。
     */
    @Resource
    private ModelInstanceMapper modelInstanceMapper;

    @Override
    public PageResult<ModelBaseDO> getModelBasePage(ModelBasePageReqVO pageReqVO) {
        pageReqVO.setScopeLayerIds(null);
        if (Boolean.TRUE.equals(pageReqVO.getIncludeDescendantLayers()) && pageReqVO.getLayerId() != null
                && !expandLayerScope(pageReqVO)) {
            return PageResult.empty();
        }
        // 主查询只加载模型和版本，字段、子模型、目录通过批量查询装配，避免 N+1。
        if (PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize())) {
            List<ModelBaseDO> models = modelBaseMapper.selectModelBaseFlatByXmlNoPage(pageReqVO);
            fillModelBaseListRelations(models);
            fillLayerPaths(models);
            return new PageResult<>(models, (long) models.size());
        }
        IPage<ModelBaseDO> page = modelBaseMapper.selectModelBaseFlatByXml(new Page(pageReqVO.getPageNum(), pageReqVO.getPageSize()), pageReqVO);
        List<ModelBaseDO> models = page.getRecords();
        fillModelBaseListRelations(models);
        fillLayerPaths(models);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public List<ModelBaseDO> getModelBaseListByIds(Collection<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) return Collections.emptyList();
        List<Long> ids = modelIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<ModelBaseDO> models = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += LIST_BATCH_SIZE) {
            models.addAll(modelBaseMapper.selectModelsWithVersionsByIds(
                    ids.subList(start, Math.min(start + LIST_BATCH_SIZE, ids.size()))));
        }
        fillLayerPaths(models);
        return models;
    }

    /**
     * 在同一主数据域内批量展开当前目录及所有后代，包含停用目录，排除已删除目录。
     */
    private boolean expandLayerScope(ModelBasePageReqVO request) {
        ModelLayerDO root = iModelLayerService.getById(request.getLayerId());
        if (root == null || root.getProjectId() == null || Boolean.TRUE.equals(root.getDelFlag())
                || (request.getProjectId() != null && !request.getProjectId().equals(root.getProjectId()))) {
            return false;
        }
        request.setProjectId(root.getProjectId());
        List<ModelLayerDO> layers = iModelLayerService.list(new QueryWrapper<ModelLayerDO>()
                .select("id", "parent_id", "project_id").eq("project_id", root.getProjectId()));
        Map<Long, List<Long>> children = new HashMap<>();
        layers.stream().filter(layer -> layer.getId() != null && layer.getParentId() != null
                        && root.getProjectId().equals(layer.getProjectId()) && !Boolean.TRUE.equals(layer.getDelFlag()))
                .forEach(layer -> children.computeIfAbsent(layer.getParentId(), ignored -> new ArrayList<>()).add(layer.getId()));
        Set<Long> ids = new LinkedHashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(root.getId());
        while (!pending.isEmpty()) {
            Long id = pending.removeFirst();
            if (ids.add(id)) pending.addAll(children.getOrDefault(id, Collections.emptyList()));
        }
        request.setScopeLayerIds(new ArrayList<>(ids));
        return true;
    }

    @Override
    public Long createModelBase(ModelBaseSaveReqVO createReqVO) {
        validateClassificationOptions(createReqVO);
        // 先创建模型主体；主模型需要版本，子模型通过 parentId 判断后不创建版本。
        ModelBaseDO dictType = BeanUtils.toBean(createReqVO, ModelBaseDO.class);
        dictType.setDelFlag(Boolean.FALSE);
        modelBaseMapper.insert(dictType);
        // parentId 为空表示主模型，只有主模型才拥有版本记录。
        if (createReqVO.getParentId() == null) {
            ModelVersionDO version = new ModelVersionDO();
            version.setModelId(dictType.getId());
            // 主模型首次创建使用统一版本初始值 V1.0。
            version.setVersionNo(VersionNoUtils.next(null));
            version.setDescription(createReqVO.getDescription());
            version.setStatus(0L);
            version.setCreatorId(createReqVO.getCreatorId());
            version.setCreateBy(createReqVO.getCreateBy());
            version.setCreateTime(createReqVO.getCreateTime());
            iModelVersionService.save(version);
            // 将主模型初始版本 ID 回写，作为主模型当前版本。
            dictType.setVersionId(version.getId());
            modelBaseMapper.updateById(dictType);
        }
        // 保存主模型字段，并递归创建子模型及子模型字段。
        saveModelFieldsAndChildren(dictType.getId(), createReqVO);
        return dictType.getId();
    }

    @Override
    public int updateModelBase(ModelBaseSaveReqVO updateReqVO) {
        validateClassificationOptions(updateReqVO);
        // 查询旧模型，用于生成字段级版本变更记录。
        ModelBaseDO oldModel = getModelBaseById(updateReqVO.getId());
        if (oldModel == null) throw new ServiceException("模型不存在");
        // 转换为完整 DO，参与所有模型业务字段的比对。
        ModelBaseDO updateModel = BeanUtils.toBean(updateReqVO, ModelBaseDO.class);
        // 删除状态只能通过逻辑删除流程维护，普通编辑不能从请求中篡改。
        updateModel.setDelFlag(oldModel.getDelFlag());
        // 成功指针只由物化提交维护，普通保存保留上次成功记录。
        updateModel.setPublishedInstanceId(oldModel.getPublishedInstanceId());
        Map<String, Object> beforeChanges = new LinkedHashMap<>();
        Map<String, Object> afterChanges = new LinkedHashMap<>();
        collectChanges(oldModel, updateModel, beforeChanges, afterChanges);
        // 明细表不属于 MODEL_BASE 主体字段，单独精确比较，避免前端列表对象触发误变更。
        boolean detailChanged = collectDetailChanges(oldModel.getId(), updateReqVO, beforeChanges, afterChanges);
        ModelVersionDO oldVersion = oldModel.getVersionId() == null ? null : iModelVersionService.getModelVersionById(oldModel.getVersionId());
        // 首次成功发布也要保护结构；只认上次成功物化的对象，不把修订中刚保存的对象算进去。
        ModelPublishContext publishedContext = readRevisionStructure(oldModel);
        validateRevision(updateReqVO, oldModel, publishedContext);
        // 草稿或修订中没有任何变化，直接结束，避免追加一条空操作记录。
        // 已发布版本不在这里结束，仍会进入下面的新建修订版本流程。
        if (beforeChanges.isEmpty() && !detailChanged && oldVersion != null && !Objects.equals(oldVersion.getStatus(), 1L))
            return 1;

        // 只有已发布版本需要新增修订版本；草稿和修订中均直接更新当前版本。
        if (oldVersion == null || !Objects.equals(oldVersion.getStatus(), 1L)) {
            if (oldVersion != null) {
                oldVersion.setDescription(updateModel.getDescription());
                // 在原记录上追加这次操作，不把前几次的修改过程覆盖掉。
                oldVersion.setChangeData(ModelVersionChangeDataUtils.append(oldVersion.getChangeData(),
                        beforeChanges, afterChanges, updateReqVO.getUpdatorId(),
                        updateReqVO.getUpdateBy(), updateReqVO.getUpdateTime()));
                oldVersion.setUpdatorId(updateReqVO.getUpdatorId());
                oldVersion.setUpdateBy(updateReqVO.getUpdateBy());
                oldVersion.setUpdateTime(updateReqVO.getUpdateTime());
                iModelVersionService.updateById(oldVersion);
            }

            // 保留当前版本关联、版本号和状态，防止请求参数误覆盖版本关联。
            updateModel.setVersionId(oldModel.getVersionId());
            int result = updateModelInfo(updateModel);
            saveRevisionChildren(updateReqVO, oldModel.getId());
            return result;
        }

        // 补全旧版本快照时保留旧历史；新版本的历史从本次修改重新开始累计。
        oldVersion.setChangeData(ModelVersionChangeDataUtils.mergeSnapshot(oldVersion.getChangeData(), beforeChanges));
        iModelVersionService.updateById(oldVersion);
        // 复制已发布版本，以当前版本 ID 作为修订基准。
        ModelVersionDO newVersion = BeanUtils.toBean(oldVersion, ModelVersionDO.class);
        newVersion.setId(null);
        newVersion.setBaseVersionId(oldVersion.getId());
        newVersion.setModelId(oldModel.getId());
        newVersion.setVersionNo(nextVersionName(oldVersion));
        newVersion.setDescription(updateModel.getDescription());
        // 传 null 表示从空历史开始，新版本不带上上一版本的操作记录。
        newVersion.setChangeData(ModelVersionChangeDataUtils.append(null, beforeChanges, afterChanges,
                updateReqVO.getUpdatorId(), updateReqVO.getUpdateBy(), updateReqVO.getUpdateTime()));
        newVersion.setStatus(2L);
        newVersion.setCreatorId(updateReqVO.getUpdatorId());
        newVersion.setCreateBy(updateReqVO.getUpdateBy());
        newVersion.setCreateTime(updateReqVO.getUpdateTime());
        iModelVersionService.save(newVersion);
        updateModel.setVersionId(newVersion.getId());
        int result = updateModelInfo(updateModel);
        saveRevisionChildren(updateReqVO, oldModel.getId());
        return result;
    }

    /**
     * 完整表单保存时允许清空负责人，不影响发布等流程的局部更新。
     */
    private int updateModelInfo(ModelBaseDO model) {
        return modelBaseMapper.update(model, new LambdaUpdateWrapper<ModelBaseDO>()
                .eq(ModelBaseDO::getId, model.getId())
                .set(model.getResponsibleUserId() == null, ModelBaseDO::getResponsibleUserId, null));
    }

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        QueryWrapper<ModelBaseDO> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        if (id != null) wrapper.ne("id", id);
        return modelBaseMapper.selectCount(wrapper) == 0;
    }

    /**
     * 现有发布接口只接收目标数据源ID；发布全过程不套平台长事务，过程记录由独立短事务保存。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ModelBasePublishRespVO publishModelBase(Long modelId, ModelBasePublishReqVO publishReqVO,
                                                   Long userId, String updateBy) {
        // 调用统一发布流程，拿到整体成功结果后再转换返回值；校验或执行失败会向上抛出异常。
        ModelPublishContext context = modelPublishService.publish(modelId, publishReqVO.getDatasourceId(), userId, updateBy);
        // 返回本次实例、版本和执行结果，页面可以据此定位发布记录。
        return ModelPublishUtils.toResponse(context, null);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ModelBaseBatchRespVO batchPublishModelBase(ModelBaseBatchPublishReqVO request, Long userId, String userName) {
        if (request == null || request.getDatasourceId() == null) throw new ServiceException("请选择数据连接");
        return executeBatchModels(batchModelIds(request.getIds()), Arrays.asList(0L, 2L), "仅草稿或修订中的模型可以发布", id -> {
            ModelBasePublishRespVO result = modelBaseService.publishModelBase(id, request, userId, userName);
            if (result == null || !"SUCCEEDED".equals(result.getStatus()))
                throw new ServiceException("模型尚未发布成功，请查看发布记录");
            return result;
        });
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ModelBaseBatchRespVO batchDisableModelBase(ModelBaseBatchReqVO request, Long userId, String userName) {
        return executeBatchModels(batchModelIds(request == null ? null : request.getIds()), Collections.singletonList(1L), "仅已发布的模型可以停用", id -> {
            if (modelBaseService.disableModelBase(id, userId, userName) <= 0)
                throw new ServiceException("模型停用失败");
            return null;
        });
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ModelBaseBatchRespVO batchRemoveModelBase(ModelBaseBatchReqVO request) {
        return executeBatchModels(batchModelIds(request == null ? null : request.getIds()), Collections.singletonList(0L), "仅草稿模型可以删除", id -> {
            if (modelBaseService.removeModelBase(Collections.singletonList(id)) <= 0)
                throw new ServiceException("模型删除失败");
            return null;
        });
    }

    private Collection<Long> batchModelIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.contains(null)) throw new ServiceException("请选择模型");
        return new LinkedHashSet<>(ids);
    }

    /**
     * 不包整批平台事务，避免一次物化长时间占用平台事务；单项失败后继续处理其余模型。
     */
    private ModelBaseBatchRespVO executeBatchModels(Collection<Long> ids, List<Long> statuses, String stateMessage,
                                                    Function<Long, ModelBasePublishRespVO> operation) {
        ModelBaseBatchRespVO response = new ModelBaseBatchRespVO();
        for (Long id : ids) {
            ModelBaseBatchRespVO.Item result = new ModelBaseBatchRespVO.Item();
            result.setModelId(id);
            try {
                // 每次执行前重新读当前版本，不能直接信任前端传来的状态。
                ModelBaseDO model = modelBaseService.getById(id);
                if (model == null || Boolean.TRUE.equals(model.getDelFlag()))
                    throw new ServiceException("模型不存在或已删除");
                result.setModelName(model.getName());
                ModelVersionDO version = model.getVersionId() == null ? null : iModelVersionService.getModelVersionById(model.getVersionId());
                if ((model.getParentId() != null && model.getParentId() != 0L) || version == null || !statuses.contains(version.getStatus()))
                    throw new ServiceException(stateMessage);
                result.setPublishResult(operation.apply(id));
                result.setSuccess(true);
                result.setMessage("操作成功");
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (RuntimeException error) {
                result.setMessage(error instanceof ServiceException ? error.getMessage() : "操作失败，请查看后台日志");
                response.setFailureCount(response.getFailureCount() + 1);
                log.warn("模型批量操作失败，modelId={}", id, error);
            }
            response.getResults().add(result);
        }
        return response;
    }

    @Override
    public int disableModelBase(Long modelId, Long userId, String updateBy) {
        // 将模型当前版本设置为封存状态，模型主体仍保留用于历史查询。
        ModelBaseDO model = modelBaseMapper.selectById(modelId);
        if (model == null || model.getVersionId() == null) throw new ServiceException("模型或当前版本不存在");
        ModelVersionDO version = iModelVersionService.getModelVersionById(model.getVersionId());
        if (version == null) throw new ServiceException("当前版本不存在");
        version.setStatus(3L);
        version.setUpdatorId(userId);
        version.setUpdateBy(updateBy);
        version.setUpdateTime(DateUtil.date());
        return iModelVersionService.updateById(version) ? 1 : 0;
    }

    @Override
    public int removeModelBase(Collection<Long> idList) {
        // 主模型删除时递归逻辑删除其子模型及字段，保留数据库记录供审计追溯。
        int removed = 0;
        for (Long id : idList) {
            if (id != null) removed += removeModelTree(id);
        }
        return removed;
    }

    @Override
    public ModelBaseDO getModelBaseById(Long id) {
        // 与分页查询使用同一套 XML 关联和结果映射，仅按 ID 查询单条模型。
        ModelBaseDO model = modelBaseMapper.selectModelBaseFlatById(id);
        if (model == null) return null;
        fillLayerPaths(Collections.singletonList(model));
        return model;
    }

    @Override
    public ModelRelationGraphRespVO getModelRelationGraph(ModelRelationGraphReqVO reqVO) {
        ModelRelationGraphReqVO query = reqVO == null ? new ModelRelationGraphReqVO() : reqVO;
        String relationType = StringUtils.trim(query.getRelationType());
        if (StringUtils.isNotEmpty(relationType)
                && !Arrays.asList(ModelRelationTypeEnum.CHILD.getValue(), ModelRelationTypeEnum.REFERENCE.getValue()).contains(relationType)) {
            throw new ServiceException("关系类型仅支持 主子关系 或 引用关系");
        }

        List<ModelBaseDO> roots;
        if (query.getId() != null) {
            ModelBaseDO root = getModelBaseById(query.getId());
            if (root == null) throw new ServiceException("模型不存在");
            roots = Collections.singletonList(root);
        } else {
            ModelBasePageReqVO pageReqVO = new ModelBasePageReqVO();
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            pageReqVO.setName(query.getName());
            pageReqVO.setCode(query.getCode());
            pageReqVO.setProjectId(query.getProjectId());
            pageReqVO.setLayerId(query.getLayerId());
            if (query.getLayerId() != null && !expandLayerScope(pageReqVO)) {
                return new ModelRelationGraphRespVO();
            }
            roots = modelBaseMapper.selectModelBaseFlatByXmlNoPage(pageReqVO);
            fillModelBaseListRelations(roots);
        }

        ModelRelationGraphRespVO graph = new ModelRelationGraphRespVO();
        Map<Long, ModelRelationGraphRespVO.Node> nodes = new LinkedHashMap<>();
        Map<String, ModelRelationGraphRespVO.Edge> edges = new LinkedHashMap<>();
        // 主数据域一次性加载并转成 Map，避免构建每个节点时重复查询 MODEL_PROJECT。
        Map<Long, String> projectNames = iModelProjectService.getModelProjectList().stream().filter(project -> project.getId() != null && StringUtils.isNotEmpty(project.getName()))
                .collect(Collectors.toMap(ModelProjectDO::getId, ModelProjectDO::getName,
                        (existing, replacement) -> existing));
        // 子模型不拥有独立版本和数据域，遍历时通过以下上下文继承所属主模型的信息。
        Map<Long, Long> inheritedProjectIds = new HashMap<>();
        Map<Long, ModelVersionDO> inheritedVersions = new HashMap<>();
        Map<Long, ModelBaseDO> rootModels = roots.stream()
                .collect(Collectors.toMap(ModelBaseDO::getId, model -> model, (existing, replacement) -> existing,
                        LinkedHashMap::new));
        Queue<Long> queue = new ArrayDeque<>();
        Set<Long> scheduled = new HashSet<>();
        roots.stream().map(ModelBaseDO::getId).filter(Objects::nonNull).forEach(rootId -> {
            queue.offer(rootId);
            scheduled.add(rootId);
        });

        // 广度优先遍历全部可达关系；scheduled 阻止循环引用导致死循环。
        while (!queue.isEmpty()) {
            Long modelId = queue.poll();
            ModelBaseDO model = rootModels.get(modelId);
            if (model == null) model = getModelBaseById(modelId);
            if (model == null) continue;

            // 子模型自身不保存 projectId/versionId，统一从主子关系上下文取得有效值。
            Long effectiveProjectId = model.getProjectId() != null
                    ? model.getProjectId() : inheritedProjectIds.get(modelId);
            ModelVersionDO effectiveVersion = model.getVersion() != null
                    ? model.getVersion() : inheritedVersions.get(modelId);
            nodes.put(modelId, buildGraphNode(model, rootModels.containsKey(modelId),
                    effectiveProjectId, effectiveVersion, projectNames));

            if (!ModelRelationTypeEnum.REFERENCE.getValue().equals(relationType)) {
                for (ModelBaseDO subModel : Optional.ofNullable(model.getSubModels()).orElse(Collections.emptyList())) {
                    if (subModel == null || subModel.getId() == null) continue;
                    // 在子节点入队前保存继承上下文；后续重新加载完整节点时仍能恢复域和版本。
                    inheritedProjectIds.put(subModel.getId(), effectiveProjectId);
                    inheritedVersions.put(subModel.getId(), effectiveVersion);
                    nodes.putIfAbsent(subModel.getId(), buildGraphNode(subModel, false,
                            effectiveProjectId, effectiveVersion, projectNames));
                    addGraphEdge(edges, modelId, subModel.getId(), ModelRelationTypeEnum.CHILD.getValue(), "1:N 主子", null);
                    if (scheduled.add(subModel.getId())) {
                        queue.offer(subModel.getId());
                    }
                }
            }

            if (!ModelRelationTypeEnum.CHILD.getValue().equals(relationType)) {
                for (ModelColumnDO field : Optional.ofNullable(model.getFields()).orElse(Collections.emptyList())) {
                    if (field == null || field.getRelatedModelId() == null
                            || !(ModelColumnCategoryEnum.RELATION.getValue().equals(field.getColumnCategory()) || ModelColumnCategoryEnum.CLASS.getValue().equals(field.getColumnCategory()))) {
                        continue;
                    }
                    ModelBaseDO relatedModel = getModelBaseById(field.getRelatedModelId());
                    if (relatedModel == null) continue;
                    Long relatedProjectId = relatedModel.getProjectId();
                    ModelVersionDO relatedVersion = relatedModel.getVersion();
                    nodes.putIfAbsent(relatedModel.getId(), buildGraphNode(relatedModel, false,
                            relatedProjectId, relatedVersion, projectNames));
                    addGraphEdge(edges, modelId, relatedModel.getId(), field.getColumnCategory(),
                            "引用：" + StringUtils.defaultIfEmpty(field.getName(), field.getCode()), field.getId());
                    if (scheduled.add(relatedModel.getId())) {
                        queue.offer(relatedModel.getId());
                    }
                }
            }
        }

        graph.setNodes(new ArrayList<>(nodes.values()));
        graph.setEdges(new ArrayList<>(edges.values()));
        return graph;
    }

    @Override
    public List<ModelBaseDO> getModelBaseList() {
        return modelBaseMapper.selectList();
    }

    /**
     * 一次查询当前结果中的全部字段和直接子模型，再按模型 ID 回填。
     */
    private void fillModelBaseListRelations(List<ModelBaseDO> models) {
        if (models == null || models.isEmpty()) return;

        List<Long> modelIds = models.stream()
                .map(ModelBaseDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (modelIds.isEmpty()) return;

        List<ModelColumnDO> fields = new ArrayList<>();
        List<ModelBaseDO> subModels = new ArrayList<>();
        for (int start = 0; start < modelIds.size(); start += LIST_BATCH_SIZE) {
            List<Long> batchIds = modelIds.subList(start, Math.min(start + LIST_BATCH_SIZE, modelIds.size()));
            fields.addAll(modelBaseMapper.selectFieldsByModelIds(batchIds));
            subModels.addAll(modelBaseMapper.selectSubModelsByParentIds(batchIds));
        }

        Map<Long, List<ModelColumnDO>> fieldsByModelId = fields.stream()
                .collect(Collectors.groupingBy(ModelColumnDO::getModelId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<ModelBaseDO>> subModelsByParentId = subModels.stream()
                .collect(Collectors.groupingBy(ModelBaseDO::getParentId, LinkedHashMap::new, Collectors.toList()));

        models.forEach(model -> {
            model.setFields(fieldsByModelId.getOrDefault(model.getId(), Collections.emptyList()));
            model.setSubModels(subModelsByParentId.getOrDefault(model.getId(), Collections.emptyList()));
        });
    }

    /**
     * 按当前结果涉及的数据域一次加载目录，并在内存中组装根目录到当前目录的链路。
     */
    private void fillLayerPaths(List<ModelBaseDO> models) {
        if (models == null || models.isEmpty()) return;

        Set<Long> projectIds = models.stream()
                .map(ModelBaseDO::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (projectIds.isEmpty()) {
            models.forEach(model -> model.setLayers(Collections.emptyList()));
            return;
        }

        Map<Long, ModelProjectDO> projectsById = iModelProjectService.listByIds(projectIds).stream()
                .collect(Collectors.toMap(ModelProjectDO::getId, project -> project, (left, right) -> left));
        models.forEach(model -> {
            ModelProjectDO project = projectsById.get(model.getProjectId());
            model.setProjectName(project == null ? null : project.getName());
            model.setProjectIcon(project == null ? null : project.getIcon());
        });

        Map<Long, ModelLayerDO> layerById = iModelLayerService.lambdaQuery()
                .in(ModelLayerDO::getProjectId, projectIds)
                .list()
                .stream()
                .filter(layer -> layer.getId() != null)
                .collect(Collectors.toMap(ModelLayerDO::getId, layer -> layer, (left, right) -> left));

        models.forEach(model -> model.setLayers(buildLayerPath(model, layerById)));
    }

    /**
     * 找出模型所在目录的完整路径，返回顺序是“顶层目录 → 下级目录 → 当前目录”。
     * 例如模型在“客户档案”下，会返回“客户主数据 / 客户档案”这两级目录。
     * 目录已由调用方一次查好，这里只从 layerById 中找，不再逐级查数据库。
     */
    private List<ModelLayerDO> buildLayerPath(ModelBaseDO model, Map<Long, ModelLayerDO> layerById) {
        LinkedList<ModelLayerDO> path = new LinkedList<>();
        // 记住已经走过的目录，防止错误的父子关系绕成圈，例如 A 的父目录是 B、B 的父目录又是 A。
        Set<Long> visited = new HashSet<>();
        Long layerId = model.getLayerId();
        // 目录 ID 为 null 或 0 就到头了；visited.add 返回 false 表示又走到同一目录，也立即停止。
        while (layerId != null && layerId != 0L && visited.add(layerId)) {
            ModelLayerDO layer = layerById.get(layerId);
            // 目录查不到就不能再往上找；模型有数据域时，也不能把其他数据域的目录拼到它的路径里。
            // 模型没有数据域时，这个判断只检查目录是否存在。
            if (layer == null || (model.getProjectId() != null
                    && !model.getProjectId().equals(layer.getProjectId()))) break;
            // 查找是从当前目录往父目录走，所以每次放到最前面，最终顺序才是从顶层到当前目录。
            path.addFirst(layer);
            // 这一层处理完，下一轮继续找它的父目录。
            layerId = layer.getParentId();
        }
        return path;
    }

    /**
     * 保存模型字段，并将主模型 ID 注入子模型 parentId 后递归创建。
     */
    private void saveModelFieldsAndChildren(Long modelId, ModelBaseSaveReqVO modelReqVO) {
        if (modelReqVO.getFields() != null) {
            for (ModelColumnSaveReqVO fieldReqVO : modelReqVO.getFields()) {
                ModelColumnDO field = BeanUtils.toBean(fieldReqVO, ModelColumnDO.class);
                field.setModelId(modelId);
                iModelColumnService.save(field);
            }
        }
        if (modelReqVO.getSubModels() != null) {
            for (ModelBaseSaveReqVO subModelReqVO : modelReqVO.getSubModels()) {
                // 子模型的 parentId 必须绑定刚刚生成的主模型 ID。
                subModelReqVO.setParentId(modelId);
                subModelReqVO.setCreatorId(modelReqVO.getCreatorId());
                subModelReqVO.setCreateBy(modelReqVO.getCreateBy());
                subModelReqVO.setCreateTime(modelReqVO.getCreateTime());
                createModelBase(subModelReqVO);
            }
        }
    }

    /**
     * 从主模型的成功指针读取上次物化结构。子模型沿用所属主模型的成功记录。
     */
    private ModelPublishContext readRevisionStructure(ModelBaseDO model) {
        ModelBaseDO root = model;
        if (model.getParentId() != null && model.getParentId() != 0L) {
            root = modelBaseMapper.selectById(model.getParentId());
            if (root == null) throw new ServiceException("所属主模型不存在");
        }
        // 草稿从未物化，没有需要保护的旧物理对象，仍按原流程正常保存。
        if (root.getPublishedInstanceId() == null) return null;
        ModelInstanceDO instance = modelInstanceMapper.selectById(root.getPublishedInstanceId());
        if (instance == null || !Objects.equals(root.getId(), instance.getModelId())
                || !ModelPublishStatus.SUCCEEDED.name().equals(instance.getStatus())) {
            throw new ServiceException("上次成功发布记录不存在或状态不正确，不能判断修订范围");
        }
        ModelPublishContext context = new ModelPublishContext();
        context.setModelId(root.getId());
        context.setBaseInstanceId(instance.getId());
        modelPublishService.readPublishedStructure(context, instance);
        return context;
    }

    /**
     * 只保护已经成功物化的模型和字段；新对象即使已经保存出ID，也允许继续编辑。
     */
    private void validateRevision(ModelBaseSaveReqVO req, ModelBaseDO old, ModelPublishContext context) {
        if (context == null) return;
        ModelMaterializedTableDefinition table = context.getBaseStructure().getTables().stream()
                // 两套表业务字段相同，使用权威表判断对象是否已经物化即可。
                .filter(item -> Objects.equals(item.getModelId(), old.getId())
                        && item.getTableRole() == ModelTableRole.AUTHORITY)
                .findFirst().orElse(null);
        if (table != null) {
            List<JSONObject> models = new ArrayList<>();
            models.add(context.getBaseSnapshot().getModel());
            models.addAll(ModelPublishUtils.objects(context.getBaseSnapshot().getModel(), "subModels"));
            JSONObject published = models.stream().filter(item -> Objects.equals(item.getLong("id"), old.getId()))
                    .findFirst().orElseThrow(() -> new ServiceException("成功配置缺少已物化模型，不能判断修订范围"));
            if (!Objects.equals(published.getString("code"), req.getCode())
                    || !Objects.equals(published.getString("type"), req.getType())) {
                throw new ServiceException("已发布模型不能修改模型编码或模型类型");
            }
            // 嵌套子模型请求可以不传parentId，此时沿用原归属；显式换父模型仍要拦截。
            Long parentId = req.getParentId() == null ? old.getParentId() : req.getParentId();
            if (published.getLongValue("parentId") != (parentId == null ? 0L : parentId)) {
                throw new ServiceException("已发布模型不能修改主子归属");
            }
            validateFields(req.getFields(), published, table);
        }
        if (req.getSubModels() != null) for (ModelBaseSaveReqVO sub : req.getSubModels()) {
            if (sub.getId() == null) continue;
            ModelBaseDO existing = modelBaseMapper.selectById(sub.getId());
            if (existing == null || !Objects.equals(existing.getParentId(), old.getId())) {
                throw new ServiceException("子模型不存在或不属于当前模型");
            }
            validateRevision(sub, existing, context);
        }
    }

    /**
     * 按成功发布配置校验已有字段，字符串只允许扩长，必填约束只允许放宽，其他结构属性保持不变。
     * 停用或逻辑删除不比较删除标记，也不生成DDL，继续使用原来的逻辑删除流程。
     */
    private void validateFields(List<ModelColumnSaveReqVO> fields, JSONObject model,
                                ModelMaterializedTableDefinition table) {
        if (fields == null) return;
        Set<Long> materializedIds = table.getColumns().stream()
                .map(column -> column.getModelFieldId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, JSONObject> publishedFields = new HashMap<>();
        for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
            // 系统字段同样受保护；业务字段只有真实物化过才限制修订。
            if (materializedIds.contains(field.getLong("id")) || "SYSTEM".equals(field.getString("columnCategory"))) {
                publishedFields.put(field.getLong("id"), field);
            }
        }
        for (ModelColumnSaveReqVO field : fields) {
            JSONObject published = publishedFields.get(field.getId());
            if (published == null) {
                // 删除后换一个ID重新添加同编码，也不能绕过已发布字段的结构限制。
                boolean reusedCode = table.getColumns().stream().anyMatch(column ->
                        Objects.equals(column.getColumnName(), field.getCode()));
                if (reusedCode) throw new ServiceException("字段" + field.getCode() + "已发布，请恢复原字段后再保存");
                continue;
            }
            JSONObject current = JSONObject.from(field);
            if ("SYSTEM".equals(published.getString("columnCategory")) && !sameRevisionProperty(published, current, "defaultValue")) {
                throw new ServiceException("不能修改系统字段的固定默认值");
            }
            for (String property : Arrays.asList("code", "columnCategory", "dataType", "dataScale")) {
                if (!sameRevisionProperty(published, current, property)) {
                    throw new ServiceException("已发布字段不能修改：" + property);
                }
            }
            // 已发布字段允许从必填改为非必填，但不能收紧为必填；null与false均表示非必填。
            if (!Boolean.TRUE.equals(ModelPublishUtils.flag(published, "requiredFlag"))
                    && Boolean.TRUE.equals(ModelPublishUtils.flag(current, "requiredFlag"))) {
                throw new ServiceException("已发布字段不能修改约束：requiredFlag");
            }
            for (String property : Arrays.asList("uniqueFlag", "primaryKeyFlag")) {
                // 未勾选和false按同一含义比较，与CHANGE保持一致，不把空值误认为修改约束。
                if (Boolean.TRUE.equals(ModelPublishUtils.flag(published, property))
                        != Boolean.TRUE.equals(ModelPublishUtils.flag(current, property))) {
                    throw new ServiceException("已发布字段不能修改约束：" + property);
                }
            }
            Long before = published.getLong("dataLength");
            Long after = field.getDataLength();
            if ("STRING".equals(published.getString("dataType"))) {
                // 未填写STRING长度按255处理；和上次成功长度比较，未发布的扩长允许反复调整。
                before = before == null ? 255L : before;
                after = after == null ? 255L : after;
                if (after < before) throw new ServiceException("已发布STRING字段不能缩小长度");
            } else if (!Objects.equals(before, after)) {
                throw new ServiceException("已发布字段不能修改数值精度或其他类型的长度");
            }
        }
    }

    /**
     * 用同一种文本形式比较结构属性，默认值未填写时的null和空字符串按同一含义处理。
     */
    private boolean sameRevisionProperty(JSONObject before, JSONObject after, String property) {
        String left = before.getString(property);
        String right = after.getString(property);
        if ("defaultValue".equals(property)) {
            left = left == null || left.isEmpty() ? null : left;
            right = right == null || right.isEmpty() ? null : right;
        }
        return Objects.equals(left, right);
    }

    /**
     * 持久化修订提交的字段和子模型，并按 ID 区分更新记录与新增记录。
     */
    private void saveRevisionChildren(ModelBaseSaveReqVO req, Long modelId) {
        if (req.getFields() != null) {
            // 修订提交的字段列表是当前完整快照；请求中已不存在的旧字段需要同步删除。
            // fields 为 null 时表示本次不处理字段，因此不能进入该删除逻辑。
            Set<Long> requestedFieldIds = req.getFields().stream()
                    .map(ModelColumnSaveReqVO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<ModelColumnDO> existingFields = iModelColumnService.getModelColumnListByReqVO(ModelColumnPageReqVO.builder().modelId(modelId).build());
            for (ModelColumnDO existing : existingFields) {
                if (!requestedFieldIds.contains(existing.getId())) {
                    iModelColumnService.removeById(existing.getId());
                }
            }
            Set<Long> activeFieldIds = existingFields.stream().map(ModelColumnDO::getId).collect(Collectors.toSet());
            for (ModelColumnSaveReqVO f : req.getFields()) {
                ModelColumnDO value = BeanUtils.toBean(f, ModelColumnDO.class);
                value.setModelId(modelId);
                // 字段 ID 为空表示新增，否则只更新对应的既有字段记录。
                if (f.getId() == null) iModelColumnService.save(value);
                else {
                    if (!activeFieldIds.contains(f.getId())) {
                        // 先恢复原行，再按正常更新流程保存配置；类上的事务确保后续失败时一起撤销。
                        if (modelColumnMapper.restoreDeletedField(f.getId(), modelId, f.getCode()) != 1) {
                            throw new ServiceException("待恢复字段不存在或不属于当前模型");
                        }
                        value.setDelFlag(Boolean.FALSE);
                    }
                    // 完整字段快照中的空参考数据表示解除关联，必须显式更新，避免忽略 null 后回显旧值。
                    iModelColumnService.update(value, new LambdaUpdateWrapper<ModelColumnDO>()
                            .eq(ModelColumnDO::getId, value.getId())
                            .eq(ModelColumnDO::getModelId, modelId)
                            .set(value.getDictTypeId() == null, ModelColumnDO::getDictTypeId, null));
                }
            }
        }
        if (req.getSubModels() != null) {
            // 子模型列表是当前完整快照：数据库中仍有效、但请求已不包含的记录需要逻辑删除。
            Set<Long> requestedSubModelIds = req.getSubModels().stream()
                    .map(ModelBaseSaveReqVO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<ModelBaseDO> existingSubModels = modelBaseMapper.selectList(
                    new QueryWrapper<ModelBaseDO>().eq("parent_id", modelId));
            for (ModelBaseDO existingSubModel : existingSubModels) {
                if (!requestedSubModelIds.contains(existingSubModel.getId())) {
                    removeModelTree(existingSubModel.getId());
                }
            }

            for (ModelBaseSaveReqVO sub : req.getSubModels()) {
                // 新子模型走完整创建流程，以便同时初始化版本和字段数据。
                if (sub.getId() == null) {
                    sub.setParentId(modelId);
                    createModelBase(sub);
                    continue;
                }
                ModelBaseDO existing = modelBaseMapper.selectById(sub.getId());
                // 更新前确认子模型存在且确实属于当前父模型，避免误绑定其他模型。
                if (existing == null || !Objects.equals(existing.getParentId(), modelId)) {
                    throw new ServiceException("子模型不存在或不属于当前模型");
                }
                ModelBaseDO value = BeanUtils.toBean(sub, ModelBaseDO.class);
                value.setId(sub.getId());
                value.setParentId(modelId);
                value.setVersionId(existing.getVersionId());
                value.setDelFlag(existing.getDelFlag());
                modelBaseMapper.updateById(value);
                saveRevisionChildren(sub, sub.getId());
            }
        }
    }

    /**
     * 递归逻辑删除模型及其字段。子模型不再展示时，其字段也必须同步失效，避免形成有效孤儿数据。
     */
    private int removeModelTree(Long modelId) {
        List<ModelBaseDO> children = modelBaseMapper.selectList(
                new QueryWrapper<ModelBaseDO>().eq("parent_id", modelId));
        for (ModelBaseDO child : children) {
            removeModelTree(child.getId());
        }
        iModelColumnService.remove(new QueryWrapper<ModelColumnDO>().eq("model_id", modelId));
        return modelBaseMapper.deleteById(modelId);
    }


    /**
     * 比较模型本身的业务属性，只把真正改动的属性放进 before 和 after。
     * 例如说明从“旧说明”改成“新说明”，两边只记 description 的旧值和新值。
     * 字段列表和子模型不在这里比较，由 collectDetailChanges 单独处理。
     */
    private void collectChanges(ModelBaseDO oldModel, ModelBaseDO newModel,
                                Map<String, Object> before, Map<String, Object> after) {
        // 按属性名逐个取值，避免为名称、说明等每个属性都手写一遍比较。
        // getDeclaredFields 只取 ModelBaseDO 自己声明的属性，不会取父类中的操作人、操作时间等属性。
        for (Field field : ModelBaseDO.class.getDeclaredFields()) {
            // 以下几类不比较：类的常量、系统维护的属性、查询时额外拼出来的目录路径和版本信息。
            // fields/subModels 是明细列表，留给专门的方法逐条比较，否则完整旧列表也会被当成变动内容。
            if (Modifier.isStatic(field.getModifiers()) || isSystemField(field.getName())
                    || "layers".equals(field.getName()) || "releaseVersion".equals(field.getName())
                    || "projectName".equals(field.getName()) || "projectIcon".equals(field.getName())
                    || "fields".equals(field.getName()) || "subModels".equals(field.getName())) continue;
            try {
                // 允许读取对象里的 private 属性，再分别取修改前、修改后的值。
                field.setAccessible(true);
                Object oldValue = field.get(oldModel);
                Object newValue = field.get(newModel);
                // 值相同就不记，包括两边都为 null；从有值改成 null 也要记，表示用户清空了这个属性。
                if (!java.util.Objects.equals(oldValue, newValue)) {
                    // 用同一个属性名保存两边的值，后续才能知道“哪个属性从什么改成了什么”。
                    before.put(field.getName(), oldValue);
                    after.put(field.getName(), newValue);
                }
            } catch (IllegalAccessException e) {
                // 属性读取失败就中止保存，不能跳过它之后继续生成不完整的变更记录。
                throw new ServiceException("模型字段比对失败");
            }
        }
    }

    /**
     * 判断这个属性是不是系统维护的内容；是的话，collectChanges 就跳过它。
     * 这里说的是模型对象的属性，不是 MASTER_DATA_ID 等“系统预置字段”的字段分类。
     */
    private boolean isSystemField(String fieldName) {
        // id 用来定位模型；versionId/version 用来维护版本；delFlag/validFlag 用来维护删除和有效状态。
        // 这些属性的变化不代表用户改了模型名称、说明或结构，所以不写进业务变更记录。
        return "id".equals(fieldName) || "versionId".equals(fieldName) || "version".equals(fieldName)
                || "delFlag".equals(fieldName) || "validFlag".equals(fieldName)
                || "publishedInstanceId".equals(fieldName);
    }

    // 只比较这些字段业务属性。ID、操作人、操作时间等不算用户修改了字段定义。
    private static final List<String> FIELD_CHANGE_PROPERTIES = Arrays.asList(
            "name", "code", "columnCategory", "dataType", "dataLength", "dataScale", "primaryKeyFlag",
            "requiredFlag", "uniqueFlag", "nameFlag", "defaultValue", "dictTypeId", "encodingVersionId",
            "encodingMode", "encodingRuleConfig", "description", "relatedModelId", "relatedCode",
            "displayCode", "nodeRule", "displayMode");

    /**
     * 一个字段挨个属性比较：只改说明，就只记录说明的旧值和新值。
     * before 放修改前的值，after 放修改后的值；其他没改的属性不放进去。
     */
    private void collectFieldChanges(ModelColumnDO old, ModelColumnSaveReqVO request,
                                     Map<String, Object> before, Map<String, Object> after) {
        // 先转成和旧字段一样的对象，保证取的是同一套属性，不比较前端临时参数。
        ModelColumnDO current = BeanUtils.toBean(request, ModelColumnDO.class);
        for (String property : FIELD_CHANGE_PROPERTIES) {
            try {
                // 按属性名取值，相当于逐个调用 getName、getDescription 等 getter。
                Field field = ModelColumnDO.class.getDeclaredField(property);
                field.setAccessible(true);
                Object previous = field.get(old);
                Object value = field.get(current);
                // 两边相同（包括都为 null）就跳过；由有值改成 null，也是一条真实变化。
                if (!Objects.equals(previous, value)) {
                    before.put(property, previous);
                    after.put(property, value);
                }
            } catch (ReflectiveOperationException e) {
                throw new ServiceException("模型字段属性比对失败");
            }
        }
    }

    /**
     * 分类字段选项仅接受稳定的大写英文值入库。
     */
    private void validateClassificationOptions(ModelBaseSaveReqVO request) {
        if (request == null) return;
        for (ModelColumnSaveReqVO field : Optional.ofNullable(request.getFields()).orElse(Collections.emptyList())) {
            if (!ModelColumnCategoryEnum.CLASS.getValue().equals(field.getColumnCategory())) {
                field.setNodeRule(null);
                field.setDisplayMode(null);
                continue;
            }
            String nodeRule = field.getNodeRule();
            String displayMode = field.getDisplayMode();
            if (!Arrays.asList("ANY", "LEAF").contains(nodeRule)) {
                throw new ServiceException("分类字段节点选择规则仅支持 ANY 或 LEAF");
            }
            if (!Arrays.asList("NAME", "FULL_PATH").contains(displayMode)) {
                throw new ServiceException("分类字段展示方式仅支持 NAME 或 FULL_PATH");
            }
        }
        for (ModelBaseSaveReqVO subModel : Optional.ofNullable(request.getSubModels()).orElse(Collections.emptyList())) {
            validateClassificationOptions(subModel);
        }
    }

    /**
     * 从完整请求列表里挑出真正改动的字段和子模型，不能把整张列表直接当成变更记录。
     * 新增记完整信息，更新只记变动属性，删除记原信息和删除标记，没变的完全不记。
     * 返回 true 表示至少有一处变化；子模型里面的字段和更下一级子模型也按同样规则处理。
     */
    private boolean collectDetailChanges(Long modelId, ModelBaseSaveReqVO request,
                                         Map<String, Object> before, Map<String, Object> after) {
        boolean changed = false;
        // null 表示这次没有提交字段，不处理；空列表表示提交了“一个字段都不要”的完整列表。
        if (request.getFields() != null) {
            // 先把旧字段按 ID 放好。处理一个已有字段就移走一个，最后剩下的就是被删除的字段。
            Map<Long, ModelColumnDO> remaining = iModelColumnService.getModelColumnListByReqVO(
                            ModelColumnPageReqVO.builder().modelId(modelId).build()).stream()
                    .collect(Collectors.toMap(ModelColumnDO::getId, field -> field, (left, right) -> left, LinkedHashMap::new));
            List<Object> beforeFields = new ArrayList<>();
            List<Object> afterFields = new ArrayList<>();
            for (ModelColumnSaveReqVO field : request.getFields()) {
                // 没有ID是新增；有ID先找正常字段，找不到时再按所属模型和编码查询已删除的原字段。
                ModelColumnDO old = field.getId() == null ? null : remaining.remove(field.getId());
                boolean restored = false;
                if (field.getId() != null && old == null) {
                    old = modelColumnMapper.selectDeletedField(field.getId(), modelId, field.getCode());
                    if (old == null) throw new ServiceException("模型字段不存在或不属于当前模型");
                    // 此处只读取和记录恢复意图；通过后续修订校验后，才在保存阶段恢复原记录。
                    restored = true;
                }
                if (old == null) {
                    // 新字段以前不存在，没有旧值可记；把它的全部信息放到 after 即可。
                    afterFields.add(BeanUtils.toBean(field, ModelColumnDO.class));
                    continue;
                }
                Map<String, Object> fieldBefore = new LinkedHashMap<>();
                Map<String, Object> fieldAfter = new LinkedHashMap<>();
                collectFieldChanges(old, field, fieldBefore, fieldAfter);
                if (restored) {
                    // 即使类型、名称等属性完全没变，恢复本身也要记为一次变化，不能被当成无修改跳过。
                    fieldBefore.put("_deleted", true);
                    fieldAfter.put("_deleted", false);
                }
                // 只有确实改了属性才记这个字段，否则系统字段和其他旧字段都会被误记进去。
                if (!fieldBefore.isEmpty()) {
                    // 补 ID 和编码只是为了认出“这是哪个字段”，不是说这两个属性一定发生了变化。
                    identify(fieldBefore, old.getId(), old.getCode());
                    identify(fieldAfter, field.getId(), field.getCode());
                    beforeFields.add(fieldBefore);
                    afterFields.add(fieldAfter);
                }
            }
            // 旧字段没出现在本次完整列表里，说明被删了：保留原信息，另写一个删除标记。
            for (ModelColumnDO removed : remaining.values()) {
                beforeFields.add(removed);
                afterFields.add(deletedDetail(removed.getId(), removed.getCode()));
            }
            // 新增可能只有 after；删除和更新有 before。任一边有内容才写字段变更。
            if (!beforeFields.isEmpty() || !afterFields.isEmpty()) {
                before.put("fields", beforeFields);
                after.put("fields", afterFields);
                changed = true;
            }
        }
        // 子模型列表同样遵守“null 不处理、空列表清空”的约定。
        if (request.getSubModels() != null) {
            // 按子模型 ID 找旧记录，处理完还剩下的子模型就是本次删除的。
            Map<Long, ModelBaseDO> remaining = modelBaseMapper.selectList(
                            new QueryWrapper<ModelBaseDO>().eq("parent_id", modelId)).stream()
                    .collect(Collectors.toMap(ModelBaseDO::getId, model -> model, (left, right) -> left, LinkedHashMap::new));
            List<Object> beforeSubs = new ArrayList<>();
            List<Object> afterSubs = new ArrayList<>();
            for (ModelBaseSaveReqVO sub : request.getSubModels()) {
                ModelBaseDO old = sub.getId() == null ? null : remaining.remove(sub.getId());
                if (sub.getId() != null && old == null) throw new ServiceException("子模型不存在或不属于当前模型");
                if (old == null) {
                    // 新子模型整个都是新增内容，连同它的字段和下级子模型一起记录。
                    afterSubs.add(sub);
                    continue;
                }
                Map<String, Object> subBefore = new LinkedHashMap<>();
                Map<String, Object> subAfter = new LinkedHashMap<>();
                // 先比较子模型自身的名称、说明等，再递归比较它里面的字段和子模型。
                collectChanges(old, BeanUtils.toBean(sub, ModelBaseDO.class), subBefore, subAfter);
                collectDetailChanges(sub.getId(), sub, subBefore, subAfter);
                // 自身属性没改，但里面新增了字段，也要记这条子模型变化。
                if (!subBefore.isEmpty() || !subAfter.isEmpty()) {
                    identify(subBefore, old.getId(), old.getCode());
                    identify(subAfter, sub.getId(), sub.getCode());
                    beforeSubs.add(subBefore);
                    afterSubs.add(subAfter);
                }
            }
            // 删子模型前把里面的结构补齐，历史里才能看出当时到底删掉了哪些内容。
            for (ModelBaseDO removed : remaining.values()) {
                fillDetailTree(removed);
                beforeSubs.add(removed);
                afterSubs.add(deletedDetail(removed.getId(), removed.getCode()));
            }
            if (!beforeSubs.isEmpty() || !afterSubs.isEmpty()) {
                before.put("subModels", beforeSubs);
                after.put("subModels", afterSubs);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 给属性变化加上 ID 和编码，后续合并时才知道它属于哪个字段或子模型。
     */
    private void identify(Map<String, Object> detail, Long id, String code) {
        detail.put("id", id);
        detail.put("code", code);
    }

    /**
     * 用“ID、编码、已删除”表示删除操作，不把删除误写成某个属性改为空。
     */
    private Map<String, Object> deletedDetail(Long id, String code) {
        Map<String, Object> detail = new LinkedHashMap<>();
        identify(detail, id, code);
        detail.put("_deleted", true);
        return detail;
    }

    /**
     * 删除子模型时，把它的字段及所有下级子模型查齐，保留下删除前的完整结构。
     */
    private void fillDetailTree(ModelBaseDO model) {
        model.setFields(iModelColumnService.getModelColumnListByReqVO(
                ModelColumnPageReqVO.builder().modelId(model.getId()).build()));
        List<ModelBaseDO> children = modelBaseMapper.selectList(new QueryWrapper<ModelBaseDO>().eq("parent_id", model.getId()));
        // 每一层都做同样的查询，直到没有下级子模型为止。
        children.forEach(this::fillDetailTree);
        model.setSubModels(children);
    }

    /**
     * 算出新修订版本的版本号，不在这里创建或保存版本记录。
     * 没有旧版本或旧版本号无法识别时，从 V1.0 开始；正常情况如 V1.0 → V1.1，V1.9 → V2.0。
     */
    private String nextVersionName(ModelVersionDO oldVersion) {
        // 旧版本为空就传 null，否则取它的版本号；false 表示按小版本递增，不使用主版本升级模式。
        // 具体解析和进位交给 VersionNoUtils，保证模型使用统一的版本号规则。
        return VersionNoUtils.next(oldVersion == null ? null : oldVersion.getVersionNo());
    }

    /**
     * 将模型实体转换为前端可直接展示的图节点。
     * effectiveProjectId/effectiveVersion 已处理子模型继承规则，前端无需再次猜测数据来源。
     */
    private ModelRelationGraphRespVO.Node buildGraphNode(ModelBaseDO model, boolean root,
                                                         Long effectiveProjectId,
                                                         ModelVersionDO effectiveVersion,
                                                         Map<Long, String> projectNames) {
        ModelRelationGraphRespVO.Node node = new ModelRelationGraphRespVO.Node();
        node.setId(model.getId());
        node.setName(model.getName());
        node.setCode(model.getCode());
        node.setProjectId(effectiveProjectId);
        node.setProjectName(projectNames.get(effectiveProjectId));
        node.setType(model.getType());
        node.setDescription(model.getDescription());
        node.setParentId(model.getParentId());
        node.setRoot(root);
        node.setSubModel(model.getParentId() != null && model.getParentId() != 0L);
        if (effectiveVersion != null) {
            node.setVersionNo(effectiveVersion.getVersionNo());
            node.setVersionStatus(effectiveVersion.getStatus());
        }
        return node;
    }

    /**
     * 按“关系类型 + 起点 + 终点 + 字段”生成稳定边 ID，同一字段关系只加入一次。
     */
    private void addGraphEdge(Map<String, ModelRelationGraphRespVO.Edge> edges, Long source, Long target,
                              String type, String label, Long fieldId) {
        String edgeId = type + ":" + source + ":" + target + ":" + (fieldId == null ? "model" : fieldId);
        if (edges.containsKey(edgeId)) return;
        ModelRelationGraphRespVO.Edge edge = new ModelRelationGraphRespVO.Edge();
        edge.setId(edgeId);
        edge.setSource(source);
        edge.setTarget(target);
        edge.setType(type);
        edge.setLabel(label);
        edge.setFieldId(fieldId);
        edges.put(edgeId, edge);
    }
}
