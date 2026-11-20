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

package tech.qiantong.qdata.mdm.dist.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskFieldMappingRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskFilterRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskSaveReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.target.DistTargetSystemDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskFieldMappingDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskFilterDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.target.DistTargetSystemMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskMapper;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskFieldMappingService;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskFilterService;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskService;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;

/**
 * 分发任务业务实现。
 * 负责任务配置校验与持久化，并组装页面所需的查询结果。
 *
 * @author qdata
 * @date 2026-10-13
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DistTaskServiceImpl extends ServiceImpl<DistTaskMapper, DistTaskDO> implements IDistTaskService {
    @Resource
    private DistTaskMapper distTaskMapper;

    @Resource
    private DistTargetSystemMapper distTargetSystemMapper;

    @Resource
    private IDistTaskFilterService distTaskFilterService;

    @Resource
    private IDistTaskFieldMappingService distTaskFieldMappingService;

    @Resource
    private ISysUserService sysUserService;

    @Resource
    private ObjectMapper objectMapper;

    private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    private static final Pattern RESPONSE_FIELD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final String MASKED_SECRET = "******";
    private static final DateTimeFormatter CLONE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 按条件查询任务原始记录。
     *
     * @param pageReqVO 查询和分页条件
     * @return 任务原始记录分页结果
     */
    @Override
    public PageResult<DistTaskDO> getDistTaskPage(DistTaskPageReqVO pageReqVO) {
        return distTaskMapper.selectPage(pageReqVO);
    }

    /**
     * 查询列表并补齐关联名称、创建人电话和最近执行信息。
     *
     * @param pageReqVO 查询和分页条件
     * @return 不含认证配置的分页结果
     */
    @Override
    public PageResult<DistTaskRespVO> getDistTaskPageDetail(DistTaskPageReqVO pageReqVO) {
        PageResult<DistTaskRespVO> page = BeanUtils.toBean(getDistTaskPage(pageReqVO), DistTaskRespVO.class);
        List<DistTaskRespVO> rows = page.getRows();
        if (rows.isEmpty()) {
            return page;
        }
        List<Long> ids = rows.stream().map(DistTaskRespVO::getId).collect(Collectors.toList());
        Map<Long, DistTaskRespVO> details = getDistTaskListDetails(ids).stream()
                .collect(Collectors.toMap(DistTaskRespVO::getId, Function.identity()));
        for (DistTaskRespVO row : rows) {
            row.setAuthConfig(null);
            row.setHeaderConfig(null);
            DistTaskRespVO detail = details.get(row.getId());
            if (detail == null) {
                continue;
            }
            row.setModelName(detail.getModelName());
            row.setProjectName(detail.getProjectName());
            row.setProjectIcon(detail.getProjectIcon());
            row.setLayerName(detail.getLayerName());
            row.setTargetSystemCode(detail.getTargetSystemCode());
            row.setTargetSystemName(detail.getTargetSystemName());
            row.setResponsibleUserName(detail.getResponsibleUserName());
            row.setResponsiblePhone(detail.getResponsiblePhone());
            row.setCreatorPhone(detail.getCreatorPhone());
            row.setLastRunTime(detail.getLastRunTime());
            row.setLastRunEndTime(detail.getLastRunEndTime());
            if (detail.getLastRunTime() != null && detail.getLastRunEndTime() != null
                    && !detail.getLastRunEndTime().before(detail.getLastRunTime())) {
                row.setLastRunDurationMillis(detail.getLastRunEndTime().getTime() - detail.getLastRunTime().getTime());
            }
            row.setLastRunStatus(detail.getLastRunStatus());
            row.setLastSuccessCount(detail.getLastSuccessCount());
            row.setLastFailureCount(detail.getLastFailureCount());
        }
        return page;
    }

    /**
     * 组装任务详情，并按请求加载筛选条件和字段映射。
     *
     * @param id 分发任务 ID
     * @param includeFilters 是否加载筛选条件
     * @param includeFieldMappings 是否加载字段映射
     * @return 已遮蔽认证配置的任务详情；不存在时返回 null
     */
    @Override
    public DistTaskRespVO getDistTaskDetail(Long id, boolean includeFilters, boolean includeFieldMappings) {
        DistTaskDO task = getDistTaskById(id);
        if (task == null) {
            return null;
        }
        DistTaskRespVO result = BeanUtils.toBean(task, DistTaskRespVO.class);
        result.setResponsibleUserId(task.getResponsibleUserId());
        List<DistTaskRespVO> details = getDistTaskListDetails(Collections.singletonList(id));
        if (!details.isEmpty()) {
            DistTaskRespVO detail = details.get(0);
            result.setModelName(detail.getModelName());
            result.setProjectName(detail.getProjectName());
            result.setLayerName(detail.getLayerName());
            result.setTargetSystemCode(detail.getTargetSystemCode());
            result.setTargetSystemName(detail.getTargetSystemName());
            result.setResponsibleUserName(detail.getResponsibleUserName());
            result.setResponsiblePhone(detail.getResponsiblePhone());
            result.setCreatorPhone(detail.getCreatorPhone());
            result.setLastRunTime(detail.getLastRunTime());
            result.setLastRunStatus(detail.getLastRunStatus());
            result.setLastSuccessCount(detail.getLastSuccessCount());
            result.setLastFailureCount(detail.getLastFailureCount());
        }
        String authHeaderName = getAuthHeaderName(task);
        result.setAuthHeaderName(authHeaderName);
        result.setHeaders(maskHeaderConfig(task.getHeaderConfig(), authHeaderName));
        result.setAuthConfig(null);
        result.setHeaderConfig(null);
        if (includeFilters) {
            result.setFilters(distTaskFilterService.list(new LambdaQueryWrapper<DistTaskFilterDO>()
                    .eq(DistTaskFilterDO::getTaskId, id)
                    .orderByAsc(DistTaskFilterDO::getSortOrder)).stream()
                    .map(filter -> BeanUtils.toBean(filter, DistTaskFilterRespVO.class))
                    .collect(Collectors.toList()));
        }
        if (includeFieldMappings) {
            result.setFieldMappings(distTaskFieldMappingService.list(new LambdaQueryWrapper<DistTaskFieldMappingDO>()
                    .eq(DistTaskFieldMappingDO::getTaskId, id)
                    .orderByAsc(DistTaskFieldMappingDO::getSortOrder)).stream()
                    .map(mapping -> BeanUtils.toBean(mapping, DistTaskFieldMappingRespVO.class))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * 批量查询任务的关联名称、人员和最近执行信息。
     *
     * @param ids 当前页任务 ID
     * @return 关联信息；ID 为空时返回空列表
     */
    private List<DistTaskRespVO> getDistTaskListDetails(List<Long> ids) {
        // 分页为空时不执行 IN 查询；列表只补充当前页关联数据。
        return ids.isEmpty() ? Collections.emptyList() : distTaskMapper.selectListDetails(ids);
    }

    /**
     * 解析已保存的 Header，自定义参数返回原值，认证参数保持遮蔽。
     *
     * @param headerConfig 已保存的自定义 Header 配置
     * @param authHeaderName 认证使用的 Header 名称
     * @return Header 列表
     */
    private List<Map<String, String>> maskHeaderConfig(String headerConfig, String authHeaderName) {
        List<Map<String, String>> masked = new ArrayList<>();
        if (authHeaderName != null && !authHeaderName.trim().isEmpty()) {
            masked.add(maskedHeader(authHeaderName, "AUTH"));
        }
        if (headerConfig == null || headerConfig.trim().isEmpty()) {
            return masked;
        }
        try {
            JsonNode headers = objectMapper.readTree(headerConfig);
            if (!headers.isArray()) {
                return masked;
            }
            for (JsonNode header : headers) {
                String key = header.path("key").asText("").trim();
                if (!key.isEmpty()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("key", key);
                    row.put("value", header.path("value").asText(""));
                    row.put("source", "CUSTOM");
                    masked.add(row);
                }
            }
        } catch (JsonProcessingException ignored) {
            // 旧配置解析失败时不把原始内容返回页面。
        }
        return masked;
    }

    /**
     * 创建只含名称、遮蔽值和来源的 Header 响应项。
     *
     * @param key Header 名称
     * @param source Header 来源
     * @return 已遮蔽的 Header 响应项
     */
    private Map<String, String> maskedHeader(String key, String source) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("value", MASKED_SECRET);
        row.put("source", source);
        return row;
    }

    /**
     * 从 API Key 配置中读取认证 Header 名称，不返回密钥。
     *
     * @param task 分发任务记录
     * @return 认证 Header 名称；不适用时返回 null
     */
    private String getAuthHeaderName(DistTaskDO task) {
        if (!"API_KEY".equals(task.getAuthType()) || task.getAuthConfig() == null) {
            return null;
        }
        try {
            return objectMapper.readTree(task.getAuthConfig()).path("headerName").asText(null);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    /**
     * 校验并创建任务，同时保存筛选条件、字段映射和重试配置。
     *
     * @param createReqVO 任务保存信息
     * @return 新任务 ID
     */
    @Override
    public Long createDistTask(DistTaskSaveReqVO createReqVO) {
        validateExecutionConfig(createReqVO, true);
        fillCreator(createReqVO);
        validateResponsibleUser(createReqVO.getResponsibleUserId());
        validateRetryConfig(createReqVO);
        validateFilterScope(createReqVO, true);
        validateMappingScope(createReqVO, true);
        validateApiConfig(createReqVO, true);
        DistTaskDO dictType = BeanUtils.toBean(createReqVO, DistTaskDO.class);
        // 任务负责人始终取本次提交的用户 ID，持久化到 DIST_TASK.RESP_USER_ID。
        dictType.setResponsibleUserId(createReqVO.getResponsibleUserId());
        dictType.setId(null);
        String code = createReqVO.getCode() == null ? "" : createReqVO.getCode().trim();
        if (code.isEmpty()) code = "DIST_" + UUID.randomUUID().toString().replace("-", "");
        validateTaskCode(code, null);
        dictType.setCode(code);
        dictType.setType("API");
        dictType.setValidFlag("1");
        dictType.setDelFlag("0");
        // 当前仅保存和展示状态，不在创建任务时注册调度。
        dictType.setStatus(createReqVO.getStatus() == null ? "DISABLED" : createReqVO.getStatus());
        distTaskMapper.insert(dictType);
        if ("CONDITION".equals(createReqVO.getDataScopeType())) {
            distTaskFilterService.replaceTaskFilters(dictType.getId(), createReqVO.getFilters());
        }
        distTaskFieldMappingService.replaceTaskFieldMappings(dictType.getId(), createReqVO.getFieldMappings());
        return dictType.getId();
    }

    /**
     * 核实创建人账号，并填入用于展示的创建人姓名。
     *
     * @param req 待创建的分发任务
     */
    private void fillCreator(DistTaskSaveReqVO req) {
        SysUser creator = sysUserService.selectUserById(req.getCreatorId());
        if (creator == null || !"0".equals(creator.getDelFlag())) {
            throw new ServiceException("所选创建人不存在");
        }
        req.setCreatorId(creator.getUserId());
        req.setCreateBy(creator.getNickName() == null || creator.getNickName().trim().isEmpty()
                ? creator.getUserName() : creator.getNickName());
    }

    /**
     * 克隆任务及其筛选条件、字段映射，重置状态和运行水位。
     *
     * @param sourceId 原任务 ID
     * @param operatorId 当前操作人 ID
     * @param operatorName 当前操作人姓名
     * @return 新任务 ID
     */
    @Override
    public Long cloneDistTask(Long sourceId, Long operatorId, String operatorName) {
        DistTaskDO source = distTaskMapper.selectById(sourceId);
        if (source == null) throw new ServiceException("分发任务不存在或已删除", HttpStatus.WARN);

        Date now = new Date();
        DistTaskDO clone = BeanUtils.toBean(source, DistTaskDO.class);
        if (!"SCHEDULED".equals(source.getTriggerType()) || !"SCHEDULED".equals(source.getRetryTriggerType())
                || !"1".equals(source.getFailRetryFlag())) {
            clone.setRetryTriggerType("MANUAL");
            clone.setFailRetryFlag("0");
            clone.setFailRetryTimes(null);
            clone.setFailRetryInterval(null);
            clone.setFailRetryOutType(null);
        }
        clone.setId(null);
        clone.setCode("DIST_" + UUID.randomUUID().toString().replace("-", ""));
        clone.setName(nextCloneName(source.getName()));
        clone.setStatus("DISABLED");
        clone.setLastSuccessTime(null);
        clone.setCreatorId(operatorId);
        clone.setCreateBy(operatorName);
        clone.setCreateTime(now);
        clone.setUpdatorId(null);
        clone.setUpdateBy(null);
        clone.setUpdateTime(null);
        clone.setValidFlag("1");
        clone.setDelFlag("0");
        if (distTaskMapper.insert(clone) != 1) throw new ServiceException("克隆分发任务失败");

        // 子项必须换新主键和任务 ID；执行实例及逐条日志不参与克隆。
        List<DistTaskFilterDO> filters = distTaskFilterService.list(new LambdaQueryWrapper<DistTaskFilterDO>()
                .eq(DistTaskFilterDO::getTaskId, sourceId).orderByAsc(DistTaskFilterDO::getSortOrder));
        for (DistTaskFilterDO filter : filters) {
            DistTaskFilterDO copy = BeanUtils.toBean(filter, DistTaskFilterDO.class);
            copy.setId(null);
            copy.setTaskId(clone.getId());
            copy.setCreatorId(operatorId);
            copy.setCreateBy(operatorName);
            copy.setCreateTime(now);
            copy.setUpdatorId(null);
            copy.setUpdateBy(null);
            copy.setUpdateTime(null);
            copy.setValidFlag("1");
            copy.setDelFlag("0");
            if (!distTaskFilterService.save(copy)) throw new ServiceException("克隆筛选条件失败");
        }
        List<DistTaskFieldMappingDO> mappings = distTaskFieldMappingService.list(new LambdaQueryWrapper<DistTaskFieldMappingDO>()
                .eq(DistTaskFieldMappingDO::getTaskId, sourceId).orderByAsc(DistTaskFieldMappingDO::getSortOrder));
        for (DistTaskFieldMappingDO mapping : mappings) {
            DistTaskFieldMappingDO copy = BeanUtils.toBean(mapping, DistTaskFieldMappingDO.class);
            copy.setId(null);
            copy.setTaskId(clone.getId());
            copy.setCreatorId(operatorId);
            copy.setCreateBy(operatorName);
            copy.setCreateTime(now);
            copy.setUpdatorId(null);
            copy.setUpdateBy(null);
            copy.setUpdateTime(null);
            copy.setValidFlag("1");
            copy.setDelFlag("0");
            if (!distTaskFieldMappingService.save(copy)) throw new ServiceException("克隆字段映射失败");
        }
        return clone.getId();
    }

    /**
     * 生成不重复的克隆任务名称。
     *
     * @param sourceName 原任务名称
     * @return 新任务名称
     */
    private String nextCloneName(String sourceName) {
        String base = sourceName == null || sourceName.trim().isEmpty() ? "分发任务" : sourceName.trim();
        // 与 Pro2 离线任务一致：原名称_copy_年月日时分秒毫秒；同毫秒重名时追加序号。
        String timestampSuffix = "_copy_" + LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(CLONE_TIME_FORMAT);
        for (int index = 0; index < 10000; index++) {
            String suffix = index == 0 ? timestampSuffix : timestampSuffix + "_" + index;
            String candidate = base.substring(0, Math.min(base.length(), 100 - suffix.length())) + suffix;
            if (distTaskMapper.selectCount(new LambdaQueryWrapper<DistTaskDO>().eq(DistTaskDO::getName, candidate)) == 0L) {
                return candidate;
            }
        }
        throw new ServiceException("克隆任务名称生成失败");
    }

    /**
     * 校验并更新任务配置；按需替换筛选条件和字段映射。
     *
     * @param updateReqVO 任务保存信息
     * @return 受影响的记录数
     */
    @Override
    public int updateDistTask(DistTaskSaveReqVO updateReqVO) {
        validateExecutionConfig(updateReqVO, false);
        // 向导完整修改必须带负责人；列表里的单独状态切换不修改负责人。
        validateResponsibleUser(updateReqVO.getResponsibleUserId());
        validateRetryConfig(updateReqVO);
        validateFilterScope(updateReqVO, false);
        validateMappingScope(updateReqVO, false);
        validateApiConfig(updateReqVO, false);
        DistTaskDO updateObj = BeanUtils.toBean(updateReqVO, DistTaskDO.class);
        // 修改页选择的负责人独立于目标系统负责人，不能依赖其他关联字段回填。
        updateObj.setResponsibleUserId(updateReqVO.getResponsibleUserId());
        if (updateReqVO.getCode() != null) {
            String code = updateReqVO.getCode().trim();
            if (code.isEmpty()) throw new ServiceException("请输入任务编码");
            validateTaskCode(code, updateReqVO.getId());
            updateObj.setCode(code);
        }
        // 在同一次更新中清空已关闭的配置。未提交配置的局部更新只写本次提供的字段。
        boolean clearRetry = "0".equals(updateObj.getFailRetryFlag());
        LambdaUpdateWrapper<DistTaskDO> update = new LambdaUpdateWrapper<DistTaskDO>()
                .eq(DistTaskDO::getId, updateObj.getId())
                .set("MANUAL".equals(updateObj.getTriggerType()), DistTaskDO::getCronExpression, null)
                .set("FULL".equals(updateObj.getDataMode()), DistTaskDO::getIncrementalStartTime, null)
                .set(clearRetry, DistTaskDO::getFailRetryTimes, null)
                .set(clearRetry, DistTaskDO::getFailRetryInterval, null)
                .set(clearRetry, DistTaskDO::getFailRetryOutType, null);
        int updated = distTaskMapper.update(updateObj, update);
        // 空列表表示清除旧条件；null 表示局部修改任务属性时保持旧条件。
        if (updated > 0 && (updateReqVO.getFilters() != null || "ALL".equals(updateReqVO.getDataScopeType()))) {
            distTaskFilterService.replaceTaskFilters(updateReqVO.getId(), updateReqVO.getFilters());
        }
        // 普通属性编辑不传映射时沿用原配置；向导提交完整列表时整体替换。
        if (updated > 0 && updateReqVO.getFieldMappings() != null) {
            distTaskFieldMappingService.replaceTaskFieldMappings(updateReqVO.getId(), updateReqVO.getFieldMappings());
        }
        return updated;
    }

    /**
     * 校验已选择的负责人账号。
     *
     * @param userId 负责人用户 ID
     */
    private void validateResponsibleUser(Long userId) {
        if (userId == null) {
            return;
        }
        if (distTargetSystemMapper.selectResponsibleUserById(userId) == null) {
            throw new ServiceException("负责人账号不存在或已删除");
        }
    }

    /**
     * 校验任务编码长度及唯一性。
     *
     * @param code 任务编码
     * @param currentTaskId 修改时的当前任务 ID；新增时为 null
     */
    private void validateTaskCode(String code, Long currentTaskId) {
        if (code.length() > 64) throw new ServiceException("任务编码不能超过64个字符");
        LambdaQueryWrapper<DistTaskDO> query = new LambdaQueryWrapper<DistTaskDO>()
                .eq(DistTaskDO::getCode, code);
        if (currentTaskId != null) query.ne(DistTaskDO::getId, currentTaskId);
        if (distTaskMapper.selectCount(query) > 0) throw new ServiceException("任务编码已存在，请更换编码", HttpStatus.WARN);
    }

    /**
     * 校验重试方式、次数、间隔和超限处理配置。
     *
     * @param req 任务保存信息
     */
    private void validateRetryConfig(DistTaskSaveReqVO req) {
        if (req.getRetryTriggerType() != null
                && !"MANUAL".equals(req.getRetryTriggerType())
                && !"SCHEDULED".equals(req.getRetryTriggerType())) {
            throw new ServiceException("请选择有效的重试执行方式");
        }
        if (req.getFailRetryFlag() != null
                && !"0".equals(req.getFailRetryFlag())
                && !"1".equals(req.getFailRetryFlag())) {
            throw new ServiceException("请选择是否自动重试");
        }
        boolean retryProvided = req.getRetryTriggerType() != null || req.getFailRetryFlag() != null
                || req.getFailRetryTimes() != null || req.getFailRetryInterval() != null || req.getFailRetryOutType() != null;
        if (!retryProvided && !"MANUAL".equals(req.getTriggerType())) return;

        String triggerType = req.getTriggerType();
        if (triggerType == null) {
            DistTaskDO existing = distTaskMapper.selectById(req.getId());
        if (existing == null) throw new ServiceException("分发任务不存在或已删除", HttpStatus.WARN);
            triggerType = existing.getTriggerType();
            if (req.getFailRetryFlag() == null) req.setFailRetryFlag(existing.getFailRetryFlag());
            if (req.getFailRetryTimes() == null) req.setFailRetryTimes(existing.getFailRetryTimes());
            if (req.getFailRetryInterval() == null) req.setFailRetryInterval(existing.getFailRetryInterval());
            if (req.getFailRetryOutType() == null) req.setFailRetryOutType(existing.getFailRetryOutType());
        }
        boolean enabled = "SCHEDULED".equals(triggerType) && "1".equals(req.getFailRetryFlag())
                && !"MANUAL".equals(req.getRetryTriggerType());
        req.setRetryTriggerType(enabled ? "SCHEDULED" : "MANUAL");
        req.setFailRetryFlag(enabled ? "1" : "0");
        if (!enabled) {
            req.setFailRetryTimes(null);
            req.setFailRetryInterval(null);
            req.setFailRetryOutType(null);
            return;
        }
        if (req.getFailRetryTimes() == null || req.getFailRetryTimes() < 1) {
            throw new ServiceException("失败重试次数必须大于0");
        }
        if (req.getFailRetryInterval() == null || req.getFailRetryInterval() < 1) {
            throw new ServiceException("重试间隔必须大于0分钟");
        }
        if (!"1".equals(req.getFailRetryOutType()) && !"2".equals(req.getFailRetryOutType())) {
            throw new ServiceException("请选择有效的超出次数处理方式");
        }
    }

    /**
     * 校验任务、目标系统、执行方式和数据模式；定时任务只保存 Cron 配置。
     *
     * @param req 任务保存信息
     * @param creating 是否为新增操作
     */
    private void validateExecutionConfig(DistTaskSaveReqVO req, boolean creating) {
        if (!creating && req.getId() == null) throw new ServiceException("分发任务ID不能为空");
        if (creating && (req.getName() == null || req.getName().trim().isEmpty()
                || req.getModelId() == null || req.getTargetSystemId() == null)) {
            throw new ServiceException("请填写任务名称、主数据模型和目标系统");
        }
        // 新任务或更换接收方只能选择启用的目标系统；已有关联在系统停用后仍保留配置。
        if (req.getTargetSystemId() != null) {
            DistTargetSystemDO targetSystem = distTargetSystemMapper.selectById(req.getTargetSystemId());
            if (targetSystem == null) throw new ServiceException("目标系统不存在或已删除，请重新选择",HttpStatus.WARN);
            if (!"1".equals(targetSystem.getStatus())) {
                DistTaskDO existingTask = creating ? null : distTaskMapper.selectById(req.getId());
                if (creating || existingTask == null
                        || !req.getTargetSystemId().equals(existingTask.getTargetSystemId())) {
                    throw new ServiceException("目标系统已禁用，不能用于新建或更换分发任务",HttpStatus.WARN);
                }
            }
        }
        if (creating) {
            if (Long.valueOf(-1L).equals(req.getModelId())) {
                // 固定供应商模型尚无正式版本接口，使用与模型相同的内部版本 ID 满足任务表约束。
                req.setModelVersionId(-1L);
            } else if (req.getModelVersionId() == null || req.getModelVersionId() <= 0) {
                throw new ServiceException("请选择已发布的主数据模型版本");
            }
        }
        if (req.getTriggerType() != null || creating) {
            if (!"MANUAL".equals(req.getTriggerType()) && !"SCHEDULED".equals(req.getTriggerType())) {
                throw new ServiceException("请选择有效的执行方式");
            }
            if ("SCHEDULED".equals(req.getTriggerType())
                    && (req.getCronExpression() == null || req.getCronExpression().trim().isEmpty())) {
                throw new ServiceException("请输入调度周期");
            }
            if ("SCHEDULED".equals(req.getTriggerType())) req.setCronExpression(req.getCronExpression().trim());
            else req.setCronExpression(null);
        }
        if (req.getDataMode() != null || creating) {
            if (!"FULL".equals(req.getDataMode()) && !"INCREMENTAL".equals(req.getDataMode())) {
                throw new ServiceException("请选择有效的分发类型");
            }
            if ("INCREMENTAL".equals(req.getDataMode()) && req.getIncrementalStartTime() == null) {
                throw new ServiceException("请选择首次增量开始时间");
            }
            if ("FULL".equals(req.getDataMode())) req.setIncrementalStartTime(null);
        }
        if (req.getStatus() != null && !"ENABLED".equals(req.getStatus()) && !"DISABLED".equals(req.getStatus())) {
            throw new ServiceException("请选择有效的任务状态");
        }
        if (creating && Long.valueOf(-1L).equals(req.getModelId())) {
            Set<Long> allowed = new HashSet<>(java.util.Arrays.asList(-11L, -12L, -13L));
            if (req.getFieldMappings() == null || req.getFieldMappings().stream().anyMatch(
                    row -> row == null || !allowed.contains(row.getModelColumnId()))
                    || req.getFieldMappings().stream().noneMatch(row -> row != null && Long.valueOf(-11L).equals(row.getModelColumnId()))) {
                throw new ServiceException("主数据模型字段配置不完整");
            }
            if (req.getFilters() != null && req.getFilters().stream().anyMatch(
                    row -> row == null || !allowed.contains(row.getModelColumnId()))) {
                throw new ServiceException("筛选条件字段不属于当前主数据模型");
            }
        }
    }

    /**
     * 校验字段映射是否完整，以及模型切换时是否重新配置映射。
     *
     * @param reqVO 任务保存信息
     * @param creating 是否为新增操作
     */
    private void validateMappingScope(DistTaskSaveReqVO reqVO, boolean creating) {
        if ((creating || reqVO.getFieldMappings() != null)
                && (reqVO.getFieldMappings() == null || reqVO.getFieldMappings().isEmpty())) {
            throw new ServiceException("请至少选择一个分发字段");
        }
        // 模型或发布版本变更时旧字段 ID 可能失效，必须同步提交新模型的完整映射。
        if (!creating && reqVO.getFieldMappings() == null && reqVO.getId() != null) {
            DistTaskDO existing = distTaskMapper.selectById(reqVO.getId());
            if (existing != null && ((reqVO.getModelId() != null && !Objects.equals(reqVO.getModelId(), existing.getModelId()))
                    || (reqVO.getModelVersionId() != null && !Objects.equals(reqVO.getModelVersionId(), existing.getModelVersionId())))) {
                throw new ServiceException("更换主数据模型时请重新配置分发字段映射");
            }
        }
    }

    /**
     * 校验数据范围与筛选条件是否匹配，兼容局部状态修改。
     *
     * @param reqVO 任务保存信息
     * @param creating 是否为新增操作
     */
    private void validateFilterScope(DistTaskSaveReqVO reqVO, boolean creating) {
        if ("ALL".equals(reqVO.getDataScopeType()) && reqVO.getFilters() != null && !reqVO.getFilters().isEmpty()) {
            throw new ServiceException("全部数据模式不能配置筛选条件");
        }
        if ("CONDITION".equals(reqVO.getDataScopeType())
                && (creating || reqVO.getFilters() != null)
                && (reqVO.getFilters() == null || reqVO.getFilters().isEmpty())) {
            throw new ServiceException("条件筛选至少需要一条条件");
        }
        if (!creating && "CONDITION".equals(reqVO.getDataScopeType()) && reqVO.getFilters() == null) {
            DistTaskDO existing = distTaskMapper.selectById(reqVO.getId());
            if (existing == null || !"CONDITION".equals(existing.getDataScopeType())) {
                throw new ServiceException("切换为条件筛选时请同时提交筛选条件");
            }
        }
    }

    /**
     * 校验接收 API、认证、Header 和业务成功条件；局部状态修改可不提交 API 配置。
     *
     * @param reqVO 任务保存信息
     * @param creating 是否为新增操作
     */
    private void validateApiConfig(DistTaskSaveReqVO reqVO, boolean creating) {
        boolean hasApiFields = reqVO.getApiUrl() != null || reqVO.getHttpMethod() != null
                || reqVO.getAuthType() != null || reqVO.getAuthConfig() != null
                || reqVO.getHeaderConfig() != null || reqVO.getRequestBodyMode() != null
                || reqVO.getSuccessCondition() != null;
        if (!creating && !hasApiFields) return;
        if (!creating && reqVO.getId() == null) throw new ServiceException("分发任务ID不能为空");
        // 详情接口不回显凭据；编辑请求未传配置时沿用数据库原值，不要求用户重新输入。
        DistTaskDO existing = creating ? null : distTaskMapper.selectById(reqVO.getId());
        if (!creating && existing == null) throw new ServiceException("分发任务不存在", HttpStatus.WARN);
        if (!creating) mergeMaskedApiConfig(reqVO, existing);
        String apiUrl = reqVO.getApiUrl();
        String httpMethod = reqVO.getHttpMethod();
        String requestBodyMode = reqVO.getRequestBodyMode();
        String authType = reqVO.getAuthType();
        String authConfig = reqVO.getAuthConfig();
        String headerConfig = reqVO.getHeaderConfig();
        String successCondition = reqVO.getSuccessCondition();
        if (!creating) {
            if (apiUrl == null) apiUrl = existing.getApiUrl();
            if (httpMethod == null) httpMethod = existing.getHttpMethod();
            if (requestBodyMode == null) requestBodyMode = existing.getRequestBodyMode();
            if (authType == null) authType = existing.getAuthType();
            if (authConfig == null) authConfig = existing.getAuthConfig();
            if (headerConfig == null) headerConfig = existing.getHeaderConfig();
            if (successCondition == null) successCondition = existing.getSuccessCondition();
        }
        if (!creating && reqVO.getAuthType() != null && !Objects.equals(reqVO.getAuthType(), existing.getAuthType())) {
            if ("NONE".equals(reqVO.getAuthType()) && reqVO.getAuthConfig() == null) {
                // MyBatis-Plus 不更新 null；显式空串用于清除旧认证配置。
                reqVO.setAuthConfig("");
                authConfig = "";
            } else if (!"NONE".equals(reqVO.getAuthType()) && reqVO.getAuthConfig() == null) {
                throw new ServiceException("切换认证方式时请重新填写认证配置");
            }
        }
        if (apiUrl == null || apiUrl.trim().isEmpty()) throw new ServiceException("请输入目标 API 地址");
        try {
            URI uri = new URI(apiUrl.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null) {
                throw new ServiceException("目标 API 地址必须是有效的 HTTP 或 HTTPS 地址");
            }
        } catch (URISyntaxException e) {
            throw new ServiceException("目标 API 地址格式不正确");
        }
        if (!"POST".equals(httpMethod)) throw new ServiceException("本期仅支持 POST 请求");
        if (!"FLAT_JSON".equals(requestBodyMode)) throw new ServiceException("本期仅支持平铺 JSON 请求体");
        if (!"NONE".equals(authType) && !"BEARER".equals(authType)
                && !"API_KEY".equals(authType)) throw new ServiceException("请选择有效的 API 认证方式");
        String authHeaderName = validateAuthConfig(authType, authConfig);
        validateHeaderConfig(headerConfig, authHeaderName);
        validateSuccessCondition(successCondition);
    }

    /**
     * 将未修改的遮蔽值还原为原凭据，避免把占位符保存到数据库。
     *
     * @param reqVO 编辑页提交的任务信息
     * @param existing 数据库中的原任务
     */
    private void mergeMaskedApiConfig(DistTaskSaveReqVO reqVO, DistTaskDO existing) {
        if (reqVO.getAuthConfig() != null && !reqVO.getAuthConfig().trim().isEmpty()) {
            JsonNode submitted = parseConfig(reqVO.getAuthConfig(), "认证配置");
            String authType = reqVO.getAuthType() == null ? existing.getAuthType() : reqVO.getAuthType();
            String secretField = "BEARER".equals(authType) ? "token" : "API_KEY".equals(authType) ? "key" : null;
            if (secretField != null && MASKED_SECRET.equals(submitted.path(secretField).asText())) {
                if (!Objects.equals(authType, existing.getAuthType()) || existing.getAuthConfig() == null
                        || !submitted.isObject()) throw new ServiceException("请重新填写认证信息");
                JsonNode previous = parseConfig(existing.getAuthConfig(), "原认证配置");
                String secret = previous.path(secretField).asText("");
                if (secret.isEmpty()) throw new ServiceException("请重新填写认证信息");
                ((ObjectNode) submitted).put(secretField, secret);
                String restored = writeConfig(submitted, "认证配置");
                if (restored.length() > 512) throw new ServiceException("认证配置不能超过512个字符");
                reqVO.setAuthConfig(restored);
            }
        }
        if (reqVO.getHeaderConfig() == null) return;
        JsonNode submittedHeaders = parseConfig(reqVO.getHeaderConfig(), "请求头配置");
        if (!submittedHeaders.isArray()) throw new ServiceException("请求头配置必须是数组");
        Map<String, String> previousValues = new LinkedHashMap<>();
        if (existing.getHeaderConfig() != null && !existing.getHeaderConfig().trim().isEmpty()) {
            JsonNode previousHeaders = parseConfig(existing.getHeaderConfig(), "原请求头配置");
            if (previousHeaders.isArray()) {
                for (JsonNode item : previousHeaders) {
                    previousValues.put(item.path("key").asText(), item.path("value").asText());
                }
            }
        }
        List<Map<String, String>> merged = new ArrayList<>();
        for (JsonNode item : submittedHeaders) {
            String key = item.path("key").asText();
            String value = item.path("value").asText();
            if (MASKED_SECRET.equals(value)) {
                String originalKey = item.path("originalKey").asText(key);
                value = previousValues.get(originalKey);
                if (value == null) throw new ServiceException("请重新填写 Header 值");
            }
            Map<String, String> header = new LinkedHashMap<>();
            header.put("key", key);
            header.put("value", value);
            merged.add(header);
        }
        String restored = writeConfig(merged, "请求头配置");
        if (restored.length() > 512) throw new ServiceException("请求头配置不能超过512个字符");
        reqVO.setHeaderConfig(restored);
    }

    /**
     * 将配置序列化为 JSON，统一转换序列化错误。
     *
     * @param value 待保存的配置
     * @param label 配置名称，用于错误提示
     * @return JSON 字符串
     */
    private String writeConfig(Object value, String label) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { throw new ServiceException(label + "保存失败"); }
    }

    /**
     * 按认证类型校验配置，并确定占用的认证 Header 名称。
     *
     * @param authType 认证类型
     * @param config 认证配置 JSON
     * @return 认证 Header 名称；无需认证时返回 null
     */
    private String validateAuthConfig(String authType, String config) {
        if ("NONE".equals(authType)) {
            if (config != null && !config.trim().isEmpty()) throw new ServiceException("无需认证时不能提交认证配置");
            return null;
        }
        if (config == null || config.trim().isEmpty()) throw new ServiceException("请填写认证配置");
        JsonNode auth = parseConfig(config, "认证配置");
        if (!auth.isObject()) throw new ServiceException("认证配置必须是 JSON 对象");
        if ("BEARER".equals(authType)) {
            JsonNode token = auth.get("token");
            if (!isHeaderValue(token)) throw new ServiceException("请输入有效的 Bearer Token");
            return "authorization";
        }
        JsonNode headerName = auth.get("headerName");
        JsonNode key = auth.get("key");
        if (headerName == null || !headerName.isTextual() || !HEADER_NAME.matcher(headerName.asText()).matches()
                || "content-type".equalsIgnoreCase(headerName.asText()) || !isHeaderValue(key)) {
            throw new ServiceException("请输入有效的 API Key 请求头名称和值");
        }
        return headerName.asText();
    }

    /**
     * 判断节点是否为可安全写入 HTTP Header 的非空文本。
     *
     * @param value 待校验的 JSON 节点
     * @return 是否为有效 Header 值
     */
    private boolean isHeaderValue(JsonNode value) {
        return value != null && value.isTextual() && !value.asText().trim().isEmpty()
                && !value.asText().contains("\r") && !value.asText().contains("\n");
    }

    /**
     * 校验自定义 Header 的格式、唯一性和与系统 Header 的冲突。
     *
     * @param config 自定义 Header 配置 JSON
     * @param authHeaderName 认证占用的 Header 名称
     */
    private void validateHeaderConfig(String config, String authHeaderName) {
        if (config == null || config.trim().isEmpty()) return;
        JsonNode headers = parseConfig(config, "请求头配置");
        if (!headers.isArray()) throw new ServiceException("请求头配置必须是数组");
        Set<String> names = new HashSet<>();
        if (authHeaderName != null) names.add(authHeaderName.toLowerCase(java.util.Locale.ROOT));
        for (JsonNode header : headers) {
            JsonNode key = header.get("key");
            JsonNode value = header.get("value");
            if (key == null || !key.isTextual() || !HEADER_NAME.matcher(key.asText()).matches()
                    || !isHeaderValue(value)) {
                throw new ServiceException("请求头名称或值不正确");
            }
            String name = key.asText().toLowerCase(java.util.Locale.ROOT);
            if ("content-type".equals(name) || "authorization".equals(name) || !names.add(name)) {
                throw new ServiceException("请求头名称重复或由系统维护");
            }
        }
    }

    /**
     * 校验目标 API 的业务成功条件，只允许比较响应顶层字段。
     *
     * @param config 成功条件配置 JSON
     */
    private void validateSuccessCondition(String config) {
        if (config == null || config.trim().isEmpty()) return;
        JsonNode condition = parseConfig(config, "成功判定配置");
        JsonNode field = condition.get("field");
        JsonNode operator = condition.get("operator");
        JsonNode value = condition.get("value");
        if (!condition.isObject() || field == null || !field.isTextual()
                || !RESPONSE_FIELD.matcher(field.asText()).matches()
                || operator == null || !("EQ".equals(operator.asText()) || "NE".equals(operator.asText()))
                || value == null || !value.isTextual() || value.asText().trim().isEmpty()) {
            throw new ServiceException("业务成功判定只能配置顶层响应字段、比较方式和成功值");
        }
    }

    /**
     * 解析配置 JSON，并转换为可读的业务错误。
     *
     * @param config 配置 JSON
     * @param label 配置名称，用于错误提示
     * @return 解析后的 JSON 节点
     */
    private JsonNode parseConfig(String config, String label) {
        try {
            return objectMapper.readTree(config);
        } catch (Exception e) {
            throw new ServiceException(label + "不是有效的 JSON");
        }
    }
    /**
     * 批量删除分发任务。
     *
     * @param idList 待删除任务的 ID
     * @return 受影响的记录数
     */
    @Override
    public int removeDistTask(Collection<Long> idList) {
        // 批量删除分发任务
        return distTaskMapper.deleteBatchIds(idList);
    }

    /**
     * 根据 ID 查询任务原始记录。
     *
     * @param id 分发任务 ID
     * @return 任务记录；不存在时返回 null
     */
    @Override
    public DistTaskDO getDistTaskById(Long id) {
        return distTaskMapper.selectById(id);
    }


}
