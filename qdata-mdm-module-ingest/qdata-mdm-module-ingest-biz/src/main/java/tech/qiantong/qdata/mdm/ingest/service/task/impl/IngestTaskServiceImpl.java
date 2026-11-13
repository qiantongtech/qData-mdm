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

package tech.qiantong.qdata.mdm.ingest.service.task.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskApiConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskDbConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskFileConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskMappingVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskPageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDatasourceRelDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskApiConfigDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDbConfigDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskFieldMappingDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskFileConfigDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.datasource.IngestDatasourceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.ingest.IngestTaskInstanceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceDatasourceRelMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskApiConfigMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskDbConfigMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskFieldMappingMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskFileConfigMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskLockMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskMapper;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestApiConfigCodec;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestApiReader;
import tech.qiantong.qdata.mdm.ingest.mapping.FieldMappingPlan;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskScheduleService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskSubmitService;
import tech.qiantong.qdata.mdm.model.service.base.IModelBaseService;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import javax.annotation.Resource;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.API;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.DATABASE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.DISABLED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.ENABLED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.FILE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.FULL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.INCREMENTAL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.MANUAL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.NOT_DELETED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.SCHEDULED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TASK_CODE_PREFIX;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_FIXED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.UNIQUE_KEY_NO;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.UNIQUE_KEY_YES;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.VALID_FLAG_YES;

/**
 * 接入任务管理服务，负责查询、配置维护、校验及执行入口。
 *
 * @author chentangwen
 */
@Service
public class IngestTaskServiceImpl implements IIngestTaskService {

    private static final int TASK_NAME_MAX_LENGTH = 64;
    private static final Pattern TASK_NAME_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5A-Za-z0-9_-]+");

    @Resource
    private IngestTaskMapper taskMapper;

    @Resource
    private IngestTaskLockMapper taskLockMapper;

    @Resource
    private IngestTaskDbConfigMapper taskDbConfigMapper;

    @Resource
    private IngestTaskFileConfigMapper taskFileConfigMapper;

    @Resource
    private IngestTaskApiConfigMapper taskApiConfigMapper;

    @Resource
    private IngestTaskFieldMappingMapper taskFieldMappingMapper;

    @Resource
    private IngestSourceMapper sourceMapper;

    @Resource
    private IngestSourceDatasourceRelMapper sourceDatasourceRelMapper;

    @Resource
    private IngestDatasourceMapper datasourceMapper;

    @Resource
    private IModelBaseService modelBaseService;

    @Resource
    private ModelPublishedApi modelPublishedApi;

    @Resource
    private IngestTaskInstanceMapper instanceMapper;

    @Resource
    private IIngestTaskSubmitService taskSubmitService;

    @Resource
    private IIngestTaskScheduleService taskScheduleService;

    @Resource
    private tech.qiantong.qdata.mdm.ingest.service.task.IIngestRunLogService runLogService;

    /**
     * 优先读取文件路径字段，兼容仅传入文件令牌的配置。
     *
     * @param file 文件配置
     * @return 文件路径或令牌，未配置时返回 null
     */
    private static String getConfiguredFilePath(IngestTaskFileConfigVO file) {
        if (file == null) {
            return null;
        }
        if (!isBlank(file.getFilePath())) {
            return file.getFilePath();
        }
        return file.getFileToken();
    }

    /**
     * 按历史 trim 规则判断空值，避免改变配置字段的空白字符语义。
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 查询历史运行记录，合并 Redis 实时进度并计算当前耗时。
     *
     * @param id  接入任务编号
     * @param req 分页参数
     * @return 运行记录分页列表
     */
    @Override
    public PageResult<IngestTaskRunRespVO> runs(Long id, PageParam req) {
        if (id == null || taskMapper.selectById(id) == null) {
            throw new ServiceException("接入任务不存在");
        }
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        try {
            List<IngestTaskInstanceDO> instances = instanceMapper.selectByTaskId(id);
            long total = new PageInfo<>(instances).getTotal();
            List<IngestTaskRunRespVO> rows = instances.stream().map(instance -> {
                instance = runLogService.latest(instance);
                IngestTaskRunRespVO row = BeanUtils.toBean(instance, IngestTaskRunRespVO.class);
                if (instance.getStartTime() != null) {
                    row.setDurationMillis(Math.max(0L,
                            (instance.getEndTime() == null ? System.currentTimeMillis() : instance.getEndTime().getTime()) - instance.getStartTime().getTime()));
                }
                return row;
            }).collect(Collectors.toList());
            return new PageResult<>(rows, total);
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 人工执行提交后立即返回实例，页面通过运行记录持续读取进度。
     *
     * @param id       接入任务编号
     * @param operator 操作人
     * @return 已受理的实例快照，最终结果由运行记录提供
     */
    @Override
    public IngestTaskRunRespVO execute(Long id, String operator) {
        return taskSubmitService.submit(id, MANUAL, operator);
    }

    /**
     * 读取完整任务配置并执行实际运行前的校验，由提交服务在确认占用后调用。
     *
     * @param id 接入任务 ID
     * @return 通过执行校验的任务配置
     */
    @Override
    public IngestTaskRespVO prepareExecution(Long id) {
        IngestTaskRespVO taskConfig = detail(id);
        validate(taskConfig, true);
        return taskConfig;
    }

    /**
     * 分页查询接入任务，模型名称由关联查询返回，唯一字段按任务编号批量补充。
     *
     * @param req 分页及筛选条件
     * @return 接入任务分页列表
     */
    @Override
    public PageResult<IngestTaskRespVO> page(IngestTaskPageReqVO req) {
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        try {
            List<IngestTaskRespVO> rows = taskMapper.selectPageRows(req);
            long total = new PageInfo<>(rows).getTotal();
            fillUniqueFields(rows);
            rows.forEach(row -> {
                fillDataFetchDescription(row);
                if (row.getLastRunTime() != null) {
                    row.setLastRunDurationMillis(Math.max(0L,
                            (row.getLastRunEndTime() == null ? System.currentTimeMillis() : row.getLastRunEndTime().getTime())
                                    - row.getLastRunTime().getTime()));
                }
            });
            return new PageResult<>(rows, total);
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 按接入类型补充列表的数据获取描述。
     *
     * @param task 接入任务列表项
     */
    private void fillDataFetchDescription(IngestTaskRespVO task) {
        if (DATABASE.equals(task.getType()) && !isBlank(task.getFetchSourceObject())) {
            String datasourceName = isBlank(task.getDatasourceName()) ? "—" : task.getDatasourceName();
            String ingestModeName = INCREMENTAL.equals(task.getIngestMode()) ? "增量更新" : "追加";
            task.setDataFetch(String.join(" · ", datasourceName, task.getFetchSourceObject(), ingestModeName));
        } else if (API.equals(task.getType()) && !isBlank(task.getFetchApiUrl())) {
            String methodPrefix = isBlank(task.getFetchHttpMethod()) ? "" : task.getFetchHttpMethod() + " ";
            task.setDataFetch(methodPrefix + task.getFetchApiUrl());
        } else if (FILE.equals(task.getType()) && !isBlank(task.getFetchFileName())) {
            task.setDataFetch(task.getFetchFileName());
        }
    }

    /**
     * 批量补充任务配置的来源唯一标识字段。
     *
     * @param rows 接入任务列表
     */
    private void fillUniqueFields(List<IngestTaskRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> taskIds = rows.stream().map(IngestTaskRespVO::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return;
        }
        Map<Long, List<String>> uniqueFieldsByTask = taskMapper.selectMappingsByTaskIds(taskIds).stream()
                .filter(mapping -> UNIQUE_KEY_YES.equals(mapping.getUniqueKeyFlag()))
                .sorted(Comparator.comparing(IngestTaskFieldMappingDO::getTaskId)
                        .thenComparing(IngestTaskFieldMappingDO::getUniqueKeyOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(
                        IngestTaskFieldMappingDO::getTaskId,
                        LinkedHashMap::new,
                        Collectors.mapping(IngestTaskFieldMappingDO::getSourceFieldName, Collectors.toList())));
        for (IngestTaskRespVO row : rows) {
            row.setUniqueFields(uniqueFieldsByTask.getOrDefault(row.getId(), Collections.emptyList()));
        }
    }

    /**
     * 查询接入任务详情及数据库、文件、接口和字段映射配置。
     *
     * @param id 接入任务编号
     * @return 接入任务详情
     */
    @Override
    public IngestTaskRespVO detail(Long id) {
        IngestTaskRespVO result = taskMapper.selectDetail(id);
        if (result == null) {
            throw new ServiceException("接入任务不存在");
        }
        List<IngestTaskFieldMappingDO> mappings = taskMapper.selectMappings(id);
        result.setMappings(mappings.stream().map(this::toMappingVO).collect(Collectors.toList()));
        result.setUniqueFields(
                mappings.stream()
                        .filter(m -> UNIQUE_KEY_YES.equals(m.getUniqueKeyFlag()))
                        .sorted(Comparator.comparing(IngestTaskFieldMappingDO::getUniqueKeyOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(IngestTaskFieldMappingDO::getSourceFieldName)
                        .distinct()
                        .collect(Collectors.toList()));
        fillTypeConfigs(id, result);
        return result;
    }

    /**
     * 将持久化字段映射转换为配置回显对象。
     *
     * @param mapping 字段映射记录
     * @return 字段映射配置
     */
    private IngestTaskMappingVO toMappingVO(IngestTaskFieldMappingDO mapping) {
        IngestTaskMappingVO mappingVO = new IngestTaskMappingVO();
        mappingVO.setSourceFieldName(mapping.getSourceFieldName());
        mappingVO.setSourceFieldType(mapping.getSourceFieldType());
        mappingVO.setRemark(mapping.getRemark());
        mappingVO.setTargetFieldId(mapping.getTargetFieldId());
        mappingVO.setTransformType(mapping.getTransformType());
        mappingVO.setTransformConfig(JSON.parseObject(mapping.getTransformConfig()));
        return mappingVO;
    }

    /**
     * 补充数据库、文件和接口配置，兼容已有任务的各类型配置回显。
     *
     * @param id     接入任务编号
     * @param result 接入任务详情
     */
    private void fillTypeConfigs(Long id, IngestTaskRespVO result) {
        IngestTaskDbConfigDO db = taskMapper.selectDb(id);
        if (db != null) {
            result.setDbConfig(BeanUtils.toBean(db, IngestTaskDbConfigVO.class));
            result.setDbConfigId(db.getId());
            result.setCurrentIncrementValue(db.getCurrentIncrementValue());
        }
        IngestTaskFileConfigDO file = taskMapper.selectFile(id);
        if (file != null) {
            IngestTaskFileConfigVO fileConfig = BeanUtils.toBean(file, IngestTaskFileConfigVO.class);
            fileConfig.setFileToken(file.getFilePath());
            result.setFileConfig(fileConfig);
        }
        IngestTaskApiConfigDO api = taskMapper.selectApi(id);
        if (api != null) {
            result.setApiConfig(toApiConfig(api));
        }
    }

    /**
     * 将接口请求头和参数 JSON 还原为配置对象。
     *
     * @param api 接口配置记录
     * @return 接口配置
     */
    private IngestTaskApiConfigVO toApiConfig(IngestTaskApiConfigDO api) {
        IngestTaskApiConfigVO apiConfig = new IngestTaskApiConfigVO();
        apiConfig.setUrl(api.getUrl());
        apiConfig.setHttpMethod(api.getHttpMethod());
        JSONObject headers = JSON.parseObject(api.getHeaderConfig());
        if (headers != null) {
            Map<String, String> headerConfig = new LinkedHashMap<>();
            headers.forEach(
                    (key, value) -> headerConfig.put(key, value == null ? "" : value.toString()));
            apiConfig.setHeaderConfig(headerConfig);
        }
        IngestApiConfigCodec.decode(api.getParamConfig(), apiConfig);
        return apiConfig;
    }

    /**
     * 创建接入任务及其类型配置。
     *
     * @param req 接入任务信息
     * @return 接入任务编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(IngestTaskSaveReqVO req) {
        // 创建和更新共用同一套配置校验，确保任务落库前引用对象均有效。
        validate(req);
        IngestTaskDO task = BeanUtils.toBean(req, IngestTaskDO.class);
        if (!DATABASE.equals(req.getType())) {
            task.setIngestMode(null);
        }
        task.setCronExpression(
                SCHEDULED.equals(req.getTriggerType()) ? req.getCronExpression().trim() : null);
        task.setId(null);
        task.setCode("tmp" + System.nanoTime());
        task.setStatus(req.getStatus() == null ? DISABLED : req.getStatus());
        task.setSchedulerStatus(DISABLED);
        task.setValidFlag(VALID_FLAG_YES);
        task.setDelFlag(NOT_DELETED);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        taskMapper.insert(task);
        if (task.getId() == null) {
            throw new ServiceException("任务ID生成失败");
        }
        assignGeneratedTaskCode(task);
        // 主任务生成 ID 后再写入类型配置和字段映射。
        saveConfig(task.getId(), req, null);
        taskScheduleService.syncAfterCommit(task.getId());
        return task.getId();
    }

    /**
     * 更新接入任务基础信息和类型配置。
     *
     * @param req 接入任务信息
     * @return 更新行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(IngestTaskSaveReqVO req) {
        IngestTaskDO previous = getTaskForUpdate(req.getId());
        if (previous.getActiveRunId() != null) {
            throw new ServiceException("任务正在执行，请等待执行结束后修改配置");
        }
        validate(req);
        String currentIncrementValue = retainedIncrementValue(previous, req);
        IngestTaskDO task = BeanUtils.toBean(req, IngestTaskDO.class);
        if (!DATABASE.equals(req.getType())) {
            task.setIngestMode(null);
        }
        task.setCronExpression(
                SCHEDULED.equals(req.getTriggerType()) ? req.getCronExpression().trim() : null);
        task.setCode(previous.getCode());
        task.setStatus(req.getStatus() == null ? previous.getStatus() : req.getStatus());
        // 编辑不擅自启用调度；改成人工执行时清除调度开关。
        task.setSchedulerStatus(ENABLED.equals(task.getStatus()) && SCHEDULED.equals(req.getTriggerType())
                ? previous.getSchedulerStatus() : DISABLED);
        task.setValidFlag(previous.getValidFlag());
        task.setCreateBy(null);
        task.setCreatorId(null);
        task.setCreateTime(null);
        task.setDelFlag(null);
        task.setUpdateTime(new Date());
        int changed = taskMapper.update(task, Wrappers.<IngestTaskDO>lambdaUpdate()
                .eq(IngestTaskDO::getId, task.getId())
                .set(req.getResponsibleUserId() == null, IngestTaskDO::getResponsibleUserId, null));
        // 配置保存方法负责清理旧配置并按任务类型重建关联数据。
        saveConfig(task.getId(), req, currentIncrementValue);
        taskScheduleService.syncAfterCommit(task.getId());
        return changed;
    }

    /**
     * 复制已有接入任务并生成新的任务名称和编号。
     *
     * @param id       被复制的任务编号
     * @param operator 操作人
     * @return 新任务编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id, String operator) {
        IngestTaskRespVO original = detail(id);
        original.setId(null);
        original.setStatus(DISABLED);
        original.setCode(null);
        String name = original.getName();
        String copyName = name.replaceAll("[^\\u4e00-\\u9fa5A-Za-z0-9_-]", "_");
        original.setName(copyName.substring(0, Math.min(copyName.length(), TASK_NAME_MAX_LENGTH - 3)) + "_副本");
        original.setCreateBy(operator);
        original.setUpdateBy(operator);
        return create(original);
    }

    /**
     * 启用或停用接入任务，启用前重新执行完整配置校验。
     *
     * @param id       接入任务编号
     * @param status   目标状态
     * @param operator 操作人
     * @return 更新行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(Long id, String status, String operator) {
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new ServiceException("任务状态不正确");
        }
        IngestTaskDO task = getTaskForUpdate(id);
        IngestTaskRespVO config = detail(id);
        if (ENABLED.equals(status)) {
            validate(config);
        }
        task.setStatus(status);
        if (DISABLED.equals(status)) {
            task.setSchedulerStatus(DISABLED);
        }
        task.setUpdateBy(operator);
        task.setUpdateTime(new Date());
        int changed = taskMapper.updateById(task);
        taskScheduleService.syncAfterCommit(id);
        return changed;
    }

    /**
     * 独立保存调度开关，并在事务提交后注册、恢复或暂停 Quartz 作业。
     *
     * @param id       接入任务编号
     * @param status   调度状态
     * @param operator 操作人
     * @return 更新行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeScheduleStatus(Long id, String status, String operator) {
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new ServiceException("调度状态不正确");
        }
        IngestTaskDO task = getTaskForUpdate(id);
        if (ENABLED.equals(status)) {
            if (!ENABLED.equals(task.getStatus())) {
                throw new ServiceException("请先启用任务");
            }
            if (!SCHEDULED.equals(task.getTriggerType())) {
                throw new ServiceException("请先配置 Cron 调度周期");
            }
            validate(detail(id));
        }
        task.setSchedulerStatus(status);
        task.setUpdateBy(operator);
        task.setUpdateTime(new Date());
        int changed = taskMapper.updateById(task);
        taskScheduleService.syncAfterCommit(id);
        return changed;
    }

    /**
     * 逻辑删除接入任务并移除调度，保留配置、来源数据和运行历史。
     *
     * @param ids 接入任务编号集合
     * @return 删除行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int remove(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        for (Long id : new TreeSet<>(ids)) {
            getTaskForUpdate(id);
        }
        int changed = taskMapper.deleteBatchIds(ids);
        ids.forEach(taskScheduleService::syncAfterCommit);
        return changed;
    }

    /**
     * 查询模型的业务字段。
     *
     * @param id 模型编号
     * @return 模型字段列表
     */
    @Override
    public List<IngestModelFieldRespVO> modelFields(Long id) {
        return modelPublishedApi.getPublishedModelStructure(id).getFields().stream()
                .filter(column -> "BUSINESS".equals(column.getColumnCategory()))
                .map(this::toIngestField)
                .collect(Collectors.toList());
    }

    /** 将成功发布快照中的字段转换为接入映射所需的轻量结构。 */
    private IngestModelFieldRespVO toIngestField(ModelPublishedColumnRespDTO column) {
        IngestModelFieldRespVO field = new IngestModelFieldRespVO();
        field.setId(column.getId());
        field.setColumnName(column.getName());
        field.setColumnCode(column.getCode());
        field.setColumnCategory("BUSINESS");
        field.setDataType(column.getDataType());
        field.setDataLength(column.getDataLength() == null ? null : Math.toIntExact(column.getDataLength()));
        field.setDataScale(column.getDataScale() == null ? null : Math.toIntExact(column.getDataScale()));
        return field;
    }

    /**
     * 校验接入任务配置。
     *
     * @param req 接入任务信息
     */
    @Override
    public void validate(IngestTaskSaveReqVO req) {
        validate(req, false);
    }

    /**
     * 按公共信息、唯一字段、映射和接入类型依次校验配置。
     *
     * @param req          接入任务信息
     * @param forExecution 是否为执行前校验
     */
    private void validate(IngestTaskSaveReqVO req, boolean forExecution) {
        validateBasicConfig(req, forExecution);
        if ((req.getStatus() != null && !Arrays.asList(ENABLED, DISABLED).contains(req.getStatus()))
                || !Arrays.asList("STOP", "CONTINUE").contains(req.getErrorStrategy())
                || !Arrays.asList("IGNORE", "WARN", "FAIL").contains(req.getNullStrategy())
                || req.getRetryCount() == null || req.getRetryCount() < 0 || req.getRetryCount() > 10
                || req.getRetryIntervalSeconds() == null || req.getRetryIntervalSeconds() < 0
                || req.getRetryIntervalSeconds() > 3600
                || (req.getRetryCount() > 0 && req.getRetryIntervalSeconds() < 1)) {
            throw new ServiceException("运行策略配置不正确，重试次数为0至10；启用重试时，间隔为1至3600秒");
        }
        List<String> uniqueFields = validateUniqueFields(req, forExecution);
        validateMappings(req, uniqueFields);
        if (DATABASE.equals(req.getType())) {
            validateDb(req);
        } else if (FILE.equals(req.getType())) {
            validateFile(req);
        } else if (API.equals(req.getType())) {
            validateApi(req);
        }
    }

    /**
     * 校验任务基础信息、引用对象和触发配置。
     *
     * @param req 接入任务信息
     */
    private void validateBasicConfig(IngestTaskSaveReqVO req, boolean forExecution) {
        // 先校验公共字段和外部引用，再校验不同接入类型的专属配置。
        if (req == null || isBlank(req.getName()) || req.getName().length() > TASK_NAME_MAX_LENGTH) {
            throw new ServiceException("任务名称不能为空且不能超过64个字符");
        }
        if (!TASK_NAME_PATTERN.matcher(req.getName()).matches()) {
            throw new ServiceException("任务名称支持中文、英文字符、数字、下划线和短横线");
        }
        if (req.getSourceId() == null || sourceMapper.selectById(req.getSourceId()) == null) {
            throw new ServiceException("来源系统不存在");
        }
        if (req.getModelId() == null || modelBaseService.getModelBaseById(req.getModelId()) == null) {
            throw new ServiceException("主数据模型不存在");
        }
        if (req.getResponsibleUserId() != null && taskMapper.countUser(req.getResponsibleUserId()) == 0) {
            throw new ServiceException("责任人不存在");
        }
        if (!Arrays.asList(DATABASE, FILE, API).contains(req.getType())) {
            throw new ServiceException("接入类型不正确");
        }
        if (!Arrays.asList(MANUAL, SCHEDULED).contains(req.getTriggerType())) {
            throw new ServiceException("执行方式不正确");
        }
        if (SCHEDULED.equals(req.getTriggerType()) && isBlank(req.getCronExpression())) {
            throw new ServiceException("请输入 Cron 表达式");
        }
        if (SCHEDULED.equals(req.getTriggerType()) && !forExecution) {
            IngestTaskScheduleServiceImpl.validateCron(req.getCronExpression());
        }
    }

    /**
     * 校验来源唯一字段；保存时允许暂不配置，执行前必须配置。
     *
     * @param req          接入任务信息
     * @param forExecution 是否为执行前校验
     * @return 按配置顺序排列的唯一字段
     */
    private List<String> validateUniqueFields(IngestTaskSaveReqVO req, boolean forExecution) {
        List<String> uniqueFields = Optional.ofNullable(req.getUniqueFields()).orElse(Collections.emptyList());
        if (forExecution && uniqueFields.isEmpty()) {
            throw new ServiceException("请先配置来源记录唯一标识后再执行");
        }
        if (uniqueFields.stream().anyMatch(IngestTaskServiceImpl::isBlank)
                || new HashSet<>(uniqueFields).size() != uniqueFields.size()) {
            throw new ServiceException("唯一标识字段不能为空或重复");
        }
        return uniqueFields;
    }

    /**
     * 校验目标字段、来源字段和转换规则，并检查唯一字段映射。
     *
     * @param req          接入任务信息
     * @param uniqueFields 来源唯一字段
     */
    private void validateMappings(IngestTaskSaveReqVO req, List<String> uniqueFields) {
        if (req.getMappings() == null || req.getMappings().isEmpty()) {
            throw new ServiceException("请配置字段映射");
        }
        FieldMappingPlan.compile(req.getMappings(), modelFields(req.getModelId()), req.getType());
        Set<String> sourceFields = new HashSet<>();
        for (IngestTaskMappingVO mapping : req.getMappings()) {
            // 未选目标时不执行处理规则，仍保留该来源字段用于唯一标识和配置回显。
            boolean fixed = mapping.getTargetFieldId() != null && TRANSFORM_FIXED.equals(mapping.getTransformType());
            if (fixed && uniqueFields.contains(mapping.getSourceFieldName())) {
                throw new ServiceException("固定值不能作为来源记录唯一标识");
            }
            if (mapping.getSourceFieldType() != null && mapping.getSourceFieldType().length() > 32) {
                throw new ServiceException("来源字段类型不能超过32个字符");
            }
            if (mapping.getRemark() != null && mapping.getRemark().length() > 500) {
                throw new ServiceException("字段映射备注不能超过500个字符");
            }
            if (!fixed) sourceFields.add(mapping.getSourceFieldName());
        }
        if (!sourceFields.containsAll(uniqueFields)) {
            throw new ServiceException("唯一标识字段必须包含在来源字段映射中");
        }
    }

    /**
     * 校验文件类型、名称和存储令牌。
     *
     * @param req 接入任务信息
     */
    private void validateFile(IngestTaskSaveReqVO req) {
        IngestTaskFileConfigVO file = req.getFileConfig();
        String path = getConfiguredFilePath(file);
        if (file == null
                || !Arrays.asList("CSV", "XLS", "XLSX").contains(file.getFileType())
                || isBlank(path)
                || isBlank(file.getFileName())) {
            throw new ServiceException("请上传CSV、XLS或XLSX文件");
        }
    }

    /**
     * 校验接口地址、请求方法以及配置字段长度。
     *
     * @param req 接入任务信息
     */
    private void validateApi(IngestTaskSaveReqVO req) {
        IngestTaskApiConfigVO api = req.getApiConfig();
        IngestApiReader.validateConfig(api);
        if (api == null
                || isBlank(api.getUrl())
                || !Arrays.asList("GET", "POST").contains(api.getHttpMethod())) {
            throw new ServiceException("API地址或请求方法不正确");
        }
        if (api.getUrl().length() > 1000
                || JSON.toJSONString(api.getHeaderConfig()).length() > 3072
                || IngestApiConfigCodec.encode(api).length() > 3072) {
            throw new ServiceException("API地址不能超过1000个字符，请求头或参数配置不能超过3072个字符");
        }
        try {
            URI uri = URI.create(api.getUrl());
            if (!Arrays.asList("http", "https").contains(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new ServiceException("API地址必须是有效的HTTP或HTTPS地址");
        }
    }

    /**
     * 校验数据库连接与来源系统关联关系，以及追加或增量更新配置。
     *
     * @param req 接入任务信息
     */
    private void validateDb(IngestTaskSaveReqVO req) {
        IngestTaskDbConfigVO config = req.getDbConfig();
        if (config == null || config.getDatasourceId() == null || isBlank(config.getSourceObject())) {
            throw new ServiceException("请选择数据库连接和数据对象");
        }
        if (datasourceMapper.selectById(config.getDatasourceId()) == null) {
            throw new ServiceException("数据库连接不存在");
        }
        List<IngestSourceDatasourceRelDO> relations =
                sourceDatasourceRelMapper.selectBySourceIds(Collections.singletonList(req.getSourceId()));
        if (relations.stream().noneMatch(r -> config.getDatasourceId().equals(r.getDatasourceId()))) {
            throw new ServiceException("数据库连接未关联当前来源系统");
        }
        if (!Arrays.asList(FULL, INCREMENTAL).contains(req.getIngestMode())) {
            throw new ServiceException("请选择追加或增量更新");
        }
        if (INCREMENTAL.equals(req.getIngestMode())
                && (isBlank(config.getIncrementField()) || isBlank(config.getInitialIncrementValue()))) {
            throw new ServiceException("增量任务必须配置增量字段及首次起始值");
        }
    }

    /**
     * 查询并锁定任务，调用方必须在事务内执行以保持锁的有效期。
     *
     * @param id 接入任务编号
     * @return 已锁定的任务
     */
    private IngestTaskDO getTaskForUpdate(Long id) {
        if (id == null) {
            throw new ServiceException("任务ID不能为空");
        }
        IngestTaskDO task = taskLockMapper.lockTask(id);
        if (task == null) {
            throw new ServiceException("接入任务不存在");
        }
        return task;
    }

    /**
     * 按接入类型重建配置，并按输入顺序保存字段映射。
     *
     * @param id  接入任务编号
     * @param req 接入任务信息
     */
    private void saveConfig(Long id, IngestTaskSaveReqVO req, String currentIncrementValue) {
        clearConfig(id);
        if (DATABASE.equals(req.getType())) {
            saveDbConfig(id, req, currentIncrementValue);
        } else if (FILE.equals(req.getType())) {
            saveFileConfig(id, req.getFileConfig());
        } else {
            saveApiConfig(id, req.getApiConfig());
        }
        saveFieldMappings(id, req);
    }

    /**
     * 保存数据库配置，追加任务不保留增量字段和首次起始值。
     */
    private void saveDbConfig(Long id, IngestTaskSaveReqVO req, String currentIncrementValue) {
        IngestTaskDbConfigDO config =
                BeanUtils.toBean(req.getDbConfig(), IngestTaskDbConfigDO.class);
        config.setTaskId(id);
        config.setCurrentIncrementValue(currentIncrementValue);
        if (FULL.equals(req.getIngestMode())) {
            config.setIncrementField(null);
            config.setInitialIncrementValue(null);
            config.setCurrentIncrementValue(null);
        }
        config.setValidFlag(VALID_FLAG_YES);
        config.setDelFlag(NOT_DELETED);
        taskDbConfigMapper.insert(config);
    }

    /**
     * 普通编辑保留位置；切换来源、模型、增量字段或首次起点时重新采集。
     */
    private String retainedIncrementValue(IngestTaskDO previous, IngestTaskSaveReqVO req) {
        if (!DATABASE.equals(previous.getType()) || !DATABASE.equals(req.getType())
                || !INCREMENTAL.equals(previous.getIngestMode()) || !INCREMENTAL.equals(req.getIngestMode())
                || !Objects.equals(previous.getSourceId(), req.getSourceId())
                || !Objects.equals(previous.getModelId(), req.getModelId())) {
            return null;
        }
        IngestTaskDbConfigDO old = taskMapper.selectDb(previous.getId());
        IngestTaskDbConfigVO config = req.getDbConfig();
        if (old == null || !Objects.equals(old.getDatasourceId(), config.getDatasourceId())
                || !Objects.equals(old.getSourceSchema(), config.getSourceSchema())
                || !Objects.equals(old.getSourceObject(), config.getSourceObject())
                || !Objects.equals(old.getIncrementField(), config.getIncrementField())
                || !Objects.equals(old.getInitialIncrementValue(), config.getInitialIncrementValue())) {
            return null;
        }
        return old.getCurrentIncrementValue();
    }

    /**
     * 保存文件配置，兼容旧文件路径字段与上传令牌。
     */
    private void saveFileConfig(Long id, IngestTaskFileConfigVO fileConfig) {
        IngestTaskFileConfigDO config =
                BeanUtils.toBean(fileConfig, IngestTaskFileConfigDO.class);
        config.setTaskId(id);
        config.setFilePath(getConfiguredFilePath(fileConfig));
        config.setValidFlag(VALID_FLAG_YES);
        config.setDelFlag(NOT_DELETED);
        if (config.getUploadTime() == null) {
            config.setUploadTime(new Date());
        }
        taskFileConfigMapper.insert(config);
    }

    /**
     * 保存接口配置，请求头和参数按 JSON 持久化。
     */
    private void saveApiConfig(Long id, IngestTaskApiConfigVO apiConfig) {
        IngestTaskApiConfigDO config = new IngestTaskApiConfigDO();
        config.setTaskId(id);
        config.setUrl(apiConfig.getUrl());
        config.setHttpMethod(apiConfig.getHttpMethod());
        config.setValidFlag(VALID_FLAG_YES);
        config.setDelFlag(NOT_DELETED);
        config.setHeaderConfig(JSON.toJSONString(apiConfig.getHeaderConfig()));
        config.setParamConfig(IngestApiConfigCodec.encode(apiConfig));
        taskApiConfigMapper.insert(config);
    }

    /**
     * 按配置顺序保存映射，唯一字段顺序用于后续稳定标识计算。
     */
    private void saveFieldMappings(Long id, IngestTaskSaveReqVO req) {
        int order = 0;
        List<String> uniqueFields = Optional.ofNullable(req.getUniqueFields()).orElse(Collections.emptyList());
        for (IngestTaskMappingVO mapping : req.getMappings()) {
            IngestTaskFieldMappingDO config =
                    BeanUtils.toBean(mapping, IngestTaskFieldMappingDO.class);
            config.setTaskId(id);
            config.setDelFlag(NOT_DELETED);
            config.setSortOrder(order++);
            // 空值是有效的固定值、默认值和值映射目标，持久化时不能省略。
            config.setTransformConfig(JSON.toJSONString(mapping.getTransformConfig(),
                    com.alibaba.fastjson2.JSONWriter.Feature.WriteNulls));
            // 兼容现有页面带来源字段的固定值配置；未提供来源字段时按方案存空。
            if (TRANSFORM_FIXED.equals(mapping.getTransformType()) && isBlank(mapping.getSourceFieldName())) {
                config.setSourceFieldName(null);
            }
            int uniqueOrder = uniqueFields.indexOf(mapping.getSourceFieldName());
            config.setUniqueKeyFlag(uniqueOrder >= 0 ? UNIQUE_KEY_YES : UNIQUE_KEY_NO);
            config.setUniqueKeyOrder(uniqueOrder);
            taskFieldMappingMapper.insert(config);
        }
    }

    /**
     * 清理任务下所有类型配置和字段映射。
     *
     * @param id 接入任务编号
     */
    private void clearConfig(Long id) {
        taskMapper.deleteDb(id);
        taskMapper.deleteFile(id);
        taskMapper.deleteApi(id);
        taskMapper.deleteMappings(id);
    }

    /**
     * 根据主键生成任务编码并校验唯一性。
     *
     * @param task 已落库的接入任务
     */
    private void assignGeneratedTaskCode(IngestTaskDO task) {
        String code = TASK_CODE_PREFIX + task.getId();
        if (taskMapper.countCode(code, task.getId()) > 0) {
            throw new ServiceException("任务编码已存在");
        }
        task.setCode(code);
        if (taskMapper.updateTaskCode(task.getId(), code) != 1) {
            throw new ServiceException("任务编码生成失败");
        }
    }
}
