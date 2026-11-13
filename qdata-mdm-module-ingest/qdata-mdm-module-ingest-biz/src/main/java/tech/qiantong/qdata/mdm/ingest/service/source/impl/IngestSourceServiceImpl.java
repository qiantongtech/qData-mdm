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

package tech.qiantong.qdata.mdm.ingest.service.source.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSimpleVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceModelRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource.IngestDatasourceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDatasourceRelDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceModelNameRow;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceResponsibleUserRow;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.datasource.IngestDatasourceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceDatasourceRelMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceMapper;
import tech.qiantong.qdata.mdm.ingest.service.source.IIngestSourceService;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.service.base.IModelBaseService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 来源系统
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Service
public class IngestSourceServiceImpl extends ServiceImpl<IngestSourceMapper, IngestSourceDO> implements IIngestSourceService {

    @Resource
    private IngestSourceMapper ingestSourceMapper;

    @Resource
    private IngestDatasourceMapper ingestDatasourceMapper;

    @Resource
    private IngestSourceDatasourceRelMapper ingestSourceDatasourceRelMapper;

    @Resource
    private IModelBaseService modelBaseService;

    /**
     * 获得来源系统分页列表，并补充任务、模型、责任人和数据源信息。
     *
     * @param pageReqVO 分页及筛选条件
     * @return 来源系统分页列表
     */
    @Override
    public PageResult<IngestSourceRespVO> getIngestSourcePage(IngestSourcePageReqVO pageReqVO) {
        // 责任人按姓名、账号或手机号筛选时，先解析为用户 ID，再交给来源系统分页条件查询。
        if (StringUtils.isNotEmpty(pageReqVO.getResponsiblePerson())) {
            pageReqVO.setResponsibleUserIds(ingestSourceMapper.selectResponsibleUserIdsByKeyword(
                    "%" + pageReqVO.getResponsiblePerson().trim() + "%"));
        }
        // 只有筛选“已引用/未引用”时才查询引用集合，避免普通列表额外访问接入任务表。
        List<Long> referencedSourceIds = null;
        if ("used".equals(pageReqVO.getRefFilter()) || "free".equals(pageReqVO.getRefFilter())) {
            referencedSourceIds = ingestSourceMapper.selectReferencedSourceIds();
        }
        PageResult<IngestSourceDO> page = ingestSourceMapper.selectPage(pageReqVO, referencedSourceIds);
        List<IngestSourceRespVO> rows = BeanUtils.toBean(page.getRows(), IngestSourceRespVO.class);
        List<Long> sourceIds = rows.stream().map(IngestSourceRespVO::getId).filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 任务数按批量结果统计，避免对分页中的每条来源系统逐条查询。
        Map<Long, Long> taskCountMap = Collections.emptyMap();
        if (!sourceIds.isEmpty()) {
            List<Long> taskSourceIds = ingestSourceMapper.selectTaskSourceIdsBySourceIds(sourceIds);
            if (taskSourceIds != null && !taskSourceIds.isEmpty()) {
                taskCountMap = taskSourceIds.stream().collect(Collectors.groupingBy(id -> id, Collectors.counting()));
            }
        }
        Map<Long, Long> finalTaskCountMap = taskCountMap;
        rows.forEach(row -> row.setTaskCount(finalTaskCountMap.getOrDefault(row.getId(), 0L)));
        fillResponsiblePersons(rows);
        fillCreatorPhones(rows);
        fillModels(rows);
        fillDatasource(rows);
        return new PageResult<>(rows, page.getTotal());
    }

    /**
     * 获得来源系统详情。
     *
     * @param id 来源系统编号
     * @return 来源系统详情
     */
    @Override
    public IngestSourceRespVO getIngestSource(Long id) {
        IngestSourceDO source = ingestSourceMapper.selectById(id);
        if (source == null) {
            throw new ServiceException("来源系统不存在");
        }
        IngestSourceRespVO respVO = BeanUtils.toBean(source, IngestSourceRespVO.class);
        fillResponsiblePersons(Collections.singletonList(respVO));
        fillCreatorPhones(Collections.singletonList(respVO));
        Long taskCount = ingestSourceMapper.countTaskBySourceId(id);
        respVO.setTaskCount(taskCount == null ? 0L : taskCount);
        fillDatasource(Collections.singletonList(respVO));
        List<Long> usedDatasourceIds = ingestSourceDatasourceRelMapper.selectUsedDatasourceIds(id);
        respVO.setUsedDatasourceIds(usedDatasourceIds == null ? Collections.emptyList() : usedDatasourceIds);
        List<IngestSourceTaskRespVO> taskList = ingestSourceMapper.selectTaskListBySourceId(id);
        if (taskList != null && !taskList.isEmpty()) {
            Map<Long, String> modelNames = modelBaseService.getModelBaseList().stream()
                    .collect(Collectors.toMap(ModelBaseDO::getId, ModelBaseDO::getName));
            taskList.forEach(task -> task.setModelName(modelNames.get(task.getModelId())));
        }
        respVO.setTaskList(taskList == null ? Collections.emptyList() : taskList);
        fillModels(Collections.singletonList(respVO));
        return respVO;
    }

    /**
     * 批量补充关联模型和按模型统计的任务数量。
     *
     * @param rows 来源系统列表
     */
    private void fillModels(List<IngestSourceRespVO> rows) {
        if (rows.isEmpty()) return;
        List<Long> ids = rows.stream().map(IngestSourceRespVO::getId).collect(Collectors.toList());
        Map<Long, List<IngestSourceModelNameRow>> models = ingestSourceMapper.selectModelNamesBySourceIds(ids).stream()
                .collect(Collectors.groupingBy(IngestSourceModelNameRow::getSourceId));
        rows.forEach(row -> {
            List<IngestSourceModelNameRow> list = models.getOrDefault(row.getId(), Collections.emptyList());
            row.setModelList(BeanUtils.toBean(list, IngestSourceModelRespVO.class));
            row.setModelNames(list.stream().map(IngestSourceModelNameRow::getModelName).collect(Collectors.toList()));
            row.setDuplicateTaskCount(list.stream().mapToLong(item -> item.getDuplicateTaskCount() == null ? 0 : item.getDuplicateTaskCount()).sum());
            row.setQualityTaskCount(list.stream().mapToLong(item -> item.getQualityTaskCount() == null ? 0 : item.getQualityTaskCount()).sum());
        });
    }

    /**
     * 批量补充责任人姓名和联系电话。
     */
    private void fillResponsiblePersons(List<IngestSourceRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream().map(IngestSourceRespVO::getResponsibleUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, IngestSourceResponsibleUserRow> usersById = ingestSourceMapper
                .selectResponsibleUsersByIds(userIds).stream()
                .collect(Collectors.toMap(IngestSourceResponsibleUserRow::getUserId,
                        Function.identity(), (first, second) -> first));
        rows.forEach(row -> {
            IngestSourceResponsibleUserRow user = usersById.get(row.getResponsibleUserId());
            if (user != null) {
                row.setResponsiblePersonName(user.getName());
                row.setResponsiblePersonPhone(user.getPhone());
            }
        });
    }

    /**
     * 批量补充创建人联系电话。
     *
     * @param rows 来源系统列表
     */
    private void fillCreatorPhones(List<IngestSourceRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream().map(IngestSourceRespVO::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, IngestSourceResponsibleUserRow> usersById = ingestSourceMapper
                .selectResponsibleUsersByIds(userIds).stream()
                .collect(Collectors.toMap(IngestSourceResponsibleUserRow::getUserId,
                        Function.identity(), (first, second) -> first));
        rows.forEach(row -> {
            IngestSourceResponsibleUserRow user = usersById.get(row.getCreatorId());
            if (user != null) {
                row.setCreatorPhone(user.getPhone());
            }
        });
    }

    /**
     * 创建来源系统并保存其数据源关联关系。
     *
     * @param createReqVO 来源系统信息
     * @return 来源系统编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createIngestSource(IngestSourceSaveReqVO createReqVO) {
        // 编码保存前统一去空格校验重复。
        String code = StringUtils.isEmpty(createReqVO.getCode()) ? createReqVO.getCode() : createReqVO.getCode().trim();
        if (StringUtils.isEmpty(code)) {
            throw new ServiceException("来源系统编码不能为空");
        }
        Long codeCount = ingestSourceMapper.countByCode(code);
        if (codeCount != null && codeCount > 0) {
            throw new ServiceException("来源系统编码已存在");
        }
        IngestSourceDO source = BeanUtils.toBean(createReqVO, IngestSourceDO.class);
        source.setId(null);
        source.setCode(code);
        source.setTrustScore(normalizeTrustScore(createReqVO.getTrustScore()));
        source.setUpdaterId(createReqVO.getUpdatorId());
        source.setValidFlag(createReqVO.getValidFlag() == null ? IngestSourceDO.VALID_FLAG_YES : createReqVO.getValidFlag());
        ingestSourceMapper.insert(source);
        saveDatasourceRels(source.getId(), createReqVO.getDatasourceIds(), createReqVO.getCreateBy());
        return source.getId();
    }

    /**
     * 更新来源系统及其数据源关联关系。
     *
     * @param updateReqVO 来源系统信息
     * @return 更新行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateIngestSource(IngestSourceSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null) {
            throw new ServiceException("来源系统ID不能为空");
        }
        IngestSourceDO dbSource = ingestSourceMapper.selectById(updateReqVO.getId());
        if (dbSource == null) {
            throw new ServiceException("来源系统不存在");
        }
        // 接入任务已经使用的连接不能被删除
        List<Long> usedDatasourceIds = ingestSourceDatasourceRelMapper.selectUsedDatasourceIds(updateReqVO.getId());
        if (CollUtil.isNotEmpty(usedDatasourceIds)) {
            Set<Long> selectedIds = new LinkedHashSet<>(normalizeDatasourceIds(updateReqVO.getDatasourceIds()));
            for (Long usedDatasourceId : usedDatasourceIds) {
                if (!selectedIds.contains(usedDatasourceId)) {
                    throw new ServiceException("存在接入任务正在使用的数据库连接，不能取消关联");
                }
            }
        }
        IngestSourceDO source = BeanUtils.toBean(updateReqVO, IngestSourceDO.class);
        source.setCode(dbSource.getCode());
        source.setTrustScore(normalizeTrustScore(updateReqVO.getTrustScore()));
        source.setUpdaterId(updateReqVO.getUpdatorId());
        source.setValidFlag(updateReqVO.getValidFlag());
        int rows = ingestSourceMapper.updateById(source);
        saveDatasourceRels(updateReqVO.getId(), updateReqVO.getDatasourceIds(), updateReqVO.getUpdateBy());
        return rows;
    }

    /**
     * 删除来源系统，删除前校验是否存在接入任务引用。
     *
     * @param idList 来源系统编号集合
     * @return 删除行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeIngestSource(Collection<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return 0;
        }
        // 删除前逐项检查任务引用；只要有一项不能删除，整个批量操作回滚。
        for (Long id : idList) {
            IngestSourceDO source = ingestSourceMapper.selectById(id);
            if (source == null) {
                throw new ServiceException("来源系统不存在");
            }
            Long taskCount = ingestSourceMapper.countTaskBySourceId(id);
            if (taskCount != null && taskCount > 0) {
                throw new ServiceException("该来源系统当前存在 " + taskCount + " 个接入任务引用，请先解除业务引用后再删除");
            }
        }
        // 先删关联表，再删主表
        ingestSourceDatasourceRelMapper.deleteBySourceIds(idList);
        return ingestSourceMapper.deleteBatchIds(idList);
    }

    /**
     * 校验并保存来源系统与数据源的关联关系。
     *
     * @param sourceId      来源系统编号
     * @param datasourceIds 数据源编号集合
     * @param operator      操作人
     */
    private void saveDatasourceRels(Long sourceId, List<Long> datasourceIds, String operator) {
        List<Long> normalizedIds = normalizeDatasourceIds(datasourceIds);
        if (!normalizedIds.isEmpty()) {
            List<IngestDatasourceDO> datasourceList = ingestDatasourceMapper.selectByIds(normalizedIds);
            if (datasourceList == null || datasourceList.size() != normalizedIds.size()) {
                throw new ServiceException("数据库连接不存在或已删除");
            }
        }
        ingestSourceDatasourceRelMapper.deleteBySourceId(sourceId);
        Date now = new Date();
        for (Long datasourceId : normalizedIds) {
            IngestSourceDatasourceRelDO rel = new IngestSourceDatasourceRelDO();
            rel.setSourceId(sourceId);
            rel.setDatasourceId(datasourceId);
            rel.setValidFlag(IngestSourceDatasourceRelDO.VALID_FLAG_YES);
            rel.setCreateBy(operator);
            rel.setCreateTime(now);
            rel.setDelFlag("0");
            ingestSourceDatasourceRelMapper.insert(rel);
        }
    }

    /**
     * 批量补充来源系统关联的数据源信息。
     *
     * @param rows 来源系统列表
     */
    private void fillDatasource(List<IngestSourceRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> sourceIds = rows.stream().map(IngestSourceRespVO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (sourceIds.isEmpty()) {
            rows.forEach(row -> {
                row.setDatasourceIds(Collections.emptyList());
                row.setDatasourceList(Collections.emptyList());
            });
            return;
        }
        List<IngestSourceDatasourceRelDO> relList = ingestSourceDatasourceRelMapper.selectBySourceIds(sourceIds);
        Map<Long, List<Long>> sourceDatasourceMap = relList.stream()
                .filter(rel -> rel.getDatasourceId() != null)
                .collect(Collectors.groupingBy(IngestSourceDatasourceRelDO::getSourceId,
                        Collectors.mapping(IngestSourceDatasourceRelDO::getDatasourceId, Collectors.toList())));
        Set<Long> datasourceIds = sourceDatasourceMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, IngestDatasourceSimpleVO> datasourceMap = Collections.emptyMap();
        if (!datasourceIds.isEmpty()) {
            datasourceMap = BeanUtils.toBean(ingestDatasourceMapper.selectByIds(datasourceIds), IngestDatasourceSimpleVO.class)
                    .stream()
                    .collect(Collectors.toMap(IngestDatasourceSimpleVO::getId, Function.identity(), (a, b) -> a));
        }
        Map<Long, IngestDatasourceSimpleVO> finalDatasourceMap = datasourceMap;
        rows.forEach(row -> {
            List<Long> ids = sourceDatasourceMap.getOrDefault(row.getId(), Collections.emptyList());
            List<IngestDatasourceSimpleVO> datasourceList = new ArrayList<>();
            for (Long datasourceId : ids) {
                IngestDatasourceSimpleVO vo = finalDatasourceMap.get(datasourceId);
                if (vo != null) {
                    datasourceList.add(vo);
                }
            }
            row.setDatasourceIds(datasourceList.stream().map(IngestDatasourceSimpleVO::getId).collect(Collectors.toList()));
            row.setDatasourceList(datasourceList);
        });
    }

    /**
     * 清理数据源编号中的空值并去重，同时保持原有顺序。
     *
     * @param datasourceIds 数据源编号集合
     * @return 清理后的数据源编号
     */
    private List<Long> normalizeDatasourceIds(List<Long> datasourceIds) {
        if (CollUtil.isEmpty(datasourceIds)) {
            return Collections.emptyList();
        }
        return datasourceIds.stream().filter(Objects::nonNull).collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    /**
     * 校验来源可信度，未传值时使用默认值。
     *
     * @param trustScore 来源可信度
     * @return 校验后的可信度
     */
    private Integer normalizeTrustScore(Integer trustScore) {
        if (trustScore == null) {
            return IngestSourceDO.TRUST_SCORE_DEFAULT;
        }
        if (trustScore < IngestSourceDO.TRUST_SCORE_MIN || trustScore > IngestSourceDO.TRUST_SCORE_MAX) {
            throw new ServiceException("来源可信度必须是 0～100 的整数");
        }
        return trustScore;
    }
}
