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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueTask.impl;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.SecurityUtils;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskIssueItemRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskStatusCountRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo.vo.QualityIssueTaskInfoSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTask.QualityIssueTaskDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskInfo.QualityIssueTaskInfoDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskLog.QualityIssueTaskLogDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueTask.QualityIssueTaskMapper;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueTaskLog.QualityIssueTaskLogMapper;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTaskInstance.QualityTaskInstanceMapper;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTask.IQualityIssueTaskService;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskInfo.IQualityIssueTaskInfoService;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedPageReqDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.rule.api.ruleQuality.dto.RuleQualityRespDTO;
import tech.qiantong.qdata.mdm.rule.api.service.rule.RuleModelApi;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.INFO_STATUS_COMPLETED;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.INFO_STATUS_PENDING;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.INFO_STATUS_PROCESSING;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.NOTE_MAX_LENGTH;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.TASK_STATUS_CANCELLED;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.TASK_STATUS_COMPLETED;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.TASK_STATUS_IN_APPROVAL;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.TASK_STATUS_PENDING_APPROVAL;
import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.TASK_STATUS_RECTIFYING;

/**
 * 整改任务业务处理。一条主任务关联本次下发的全部问题项明细。
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityIssueTaskServiceImpl extends ServiceImpl<QualityIssueTaskMapper, QualityIssueTaskDO>
        implements IQualityIssueTaskService {
    private static final int DISPLAY_BATCH_SIZE = 500;
    private static final int RULE_MODEL_BATCH_SIZE = 500;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private QualityIssueTaskMapper qualityIssueTaskMapper;

    @Resource
    private IQualityIssueTaskInfoService qualityIssueTaskInfoService;

    @Resource
    private QualityIssueTaskLogMapper qualityIssueTaskLogMapper;

    @Resource
    private QualityTaskInstanceMapper qualityTaskInstanceMapper;

    @Resource
    private ISysUserService sysUserService;

    @Resource
    private ModelPublishedApi modelPublishedApi;

    @Resource
    private RuleModelApi ruleModelApi;

    @Override
    public PageResult<QualityIssueTaskDO> getQualityIssueTaskPage(QualityIssueTaskPageReqVO request) {
        boolean unpaged = PageParam.PAGE_SIZE_NONE.equals(request.getPageSize());
        // 导出等不分页场景直接查全部，避免 new Page<>(1, -1) 在 maxLimit 拦截器下被当作空分页。
        if (unpaged) {
            List<QualityIssueTaskDO> rows = qualityIssueTaskMapper.selectDisplayList(request);
            fillDisplayStats(rows);
            fillAssigneeNames(rows);
            fillModelNames(rows);
            return new PageResult<>(rows, (long) rows.size());
        }
        int page = request.getPageNum() == null ? 1 : Math.max(1, request.getPageNum());
        int size = request.getPageSize() == null ? 10 : Math.max(1, Math.min(100, request.getPageSize()));
        Page<QualityIssueTaskDO> paging = new Page<>(page, size);
        qualityIssueTaskMapper.selectDisplayPage(paging, request);
        fillDisplayStats(paging.getRecords());
        fillAssigneeNames(paging.getRecords());
        fillModelNames(paging.getRecords());
        return new PageResult<>(paging.getRecords(), paging.getTotal());
    }

    @Override
    public QualityIssueTaskStatusCountRespVO getQualityIssueTaskStatusCount(QualityIssueTaskPageReqVO request) {
        QualityIssueTaskStatusCountRespVO result = new QualityIssueTaskStatusCountRespVO();
        for (Map<String, Object> count : qualityIssueTaskMapper.selectStatusCounts(request)) {
            long total = ((Number) count.get("total")).longValue();
            result.setAllCount(result.getAllCount() + total);
            switch (String.valueOf(count.get("status"))) {
                case TASK_STATUS_PENDING_APPROVAL: result.setPendingCount(total); break;
                case TASK_STATUS_IN_APPROVAL: result.setProcessingCount(total); break;
                case TASK_STATUS_COMPLETED: result.setCompletedCount(total); break;
                case TASK_STATUS_CANCELLED: result.setCancelledCount(total); break;
                case TASK_STATUS_RECTIFYING: result.setRectifyingCount(total); break;
                default: break;
            }
        }
        return result;
    }

    @Override
    public QualityIssueTaskDO getQualityIssueTaskDetail(Long id) {
        getQualityIssueTaskById(id);
        QualityIssueTaskDO detail = qualityIssueTaskMapper.selectDisplay(id);
        if (detail != null) {
            fillDisplayStats(Collections.singletonList(detail));
            fillAssigneeNames(Collections.singletonList(detail));
            List<QualityIssueTaskIssueItemRespVO> issueItems = qualityIssueTaskMapper.selectTaskIssueItems(id);
            fillIssueUpdaterPhones(issueItems);
            detail.setIssueItems(issueItems);
            fillIssueMetadata(detail, issueItems);
        }
        return detail;
    }

    /** 按本次结果中的模型去重读取，空值也在本次请求内缓存。 */
    private void fillModelNames(List<QualityIssueTaskDO> rows) {
        Map<Long, ModelPublishedRespDTO> models = new HashMap<>();
        for (QualityIssueTaskDO row : rows) {
            Long modelId = row.getModelId();
            if (!models.containsKey(modelId)) {
                models.put(modelId, findPublishedModel(modelId));
            }
            fillModelName(row, models.get(modelId));
        }
    }

    private void fillModelName(QualityIssueTaskDO task, ModelPublishedRespDTO model) {
        if (model != null) {
            task.setModelName(model.getName());
            task.setModelCode(model.getCode());
        }
    }

    /** 复用同一模型发布结构和同一份规则结果补充详情。 */
    private void fillIssueMetadata(QualityIssueTaskDO task, List<QualityIssueTaskIssueItemRespVO> items) {
        Long modelId = task.getModelId();
        if (items.isEmpty()) {
            fillModelName(task, findPublishedModel(modelId));
            return;
        }
        ModelPublishedStructureRespDTO structure = findPublishedStructure(modelId);
        Map<Long, RuleQualityRespDTO> rules = loadRules(Collections.singletonList(modelId));
        Map<Long, ModelPublishedColumnRespDTO> fields = indexFields(structure);
        for (QualityIssueTaskIssueItemRespVO item : items) {
            ModelPublishedColumnRespDTO field = fields.get(item.getModelColumnId());
            if (field != null) {
                item.setModelColumnName(field.getName());
            }
            RuleQualityRespDTO rule = rules.get(item.getQualityRuleId());
            if (rule != null) {
                item.setQualityRuleName(rule.getRuleName());
                item.setRuleType(rule.getRuleType());
            }
        }
        if (structure != null) {
            fillModelName(task, structure.getModel());
            fillMasterInfo(modelId, items, structure.getFields());
        }
    }

    /** 模型未发布或已不可用时，保留历史问题，仅缺少模型展示信息。 */
    private ModelPublishedRespDTO findPublishedModel(Long modelId) {
        if (modelId == null || modelId <= 0) {
            return null;
        }
        ModelPublishedPageReqDTO request = new ModelPublishedPageReqDTO();
        request.setId(modelId);
        request.setPageNum(1);
        request.setPageSize(1);
        PageResult<ModelPublishedRespDTO> result = modelPublishedApi.getPublishedModelPage(request);
        return result.getRows().isEmpty() ? null : result.getRows().get(0);
    }

    /** 只为仍有发布版本的模型读取结构，未发布时跳过补充信息。 */
    private ModelPublishedStructureRespDTO findPublishedStructure(Long modelId) {
        return findPublishedModel(modelId) == null ? null : modelPublishedApi.getPublishedModelStructure(modelId);
    }

    private Map<Long, ModelPublishedColumnRespDTO> indexFields(ModelPublishedStructureRespDTO structure) {
        if (structure == null) {
            return Collections.emptyMap();
        }
        return Stream.concat(structure.getFields().stream(),
                        structure.getSubModels().stream().flatMap(model -> model.getFields().stream()))
                .collect(Collectors.toMap(ModelPublishedColumnRespDTO::getId, field -> field, (first, next) -> first));
    }

    /** 规则 API 包含已删除规则，展示时排除它们并保留已停用规则的历史标签。 */
    private Map<Long, RuleQualityRespDTO> loadRules(Collection<Long> modelIds) {
        List<Long> ids = modelIds.stream().filter(id -> id != null && id > 0)
                .distinct().collect(Collectors.toList());
        Map<Long, RuleQualityRespDTO> rules = new HashMap<>();
        for (int start = 0; start < ids.size(); start += RULE_MODEL_BATCH_SIZE) {
            int end = Math.min(start + RULE_MODEL_BATCH_SIZE, ids.size());
            for (RuleQualityRespDTO rule : ruleModelApi.getModelRules(ids.subList(start, end)).getQuality()) {
                if (Boolean.FALSE.equals(rule.getDelFlag())) {
                    rules.put(rule.getId(), rule);
                }
            }
        }
        return rules;
    }

    /** 仅统计当前页任务的问题项和最近处理时间。 */
    private void fillDisplayStats(List<QualityIssueTaskDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> taskIds = rows.stream().map(QualityIssueTaskDO::getId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, QualityIssueTaskDO> countsById = new HashMap<>();
        Map<Long, QualityIssueTaskDO> latestById = new HashMap<>();
        for (int start = 0; start < taskIds.size(); start += DISPLAY_BATCH_SIZE) {
            List<Long> batchIds = taskIds.subList(start, Math.min(start + DISPLAY_BATCH_SIZE, taskIds.size()));
            for (QualityIssueTaskDO counts : qualityIssueTaskMapper.selectDisplayCounts(batchIds)) {
                countsById.put(counts.getId(), counts);
            }
            for (QualityIssueTaskDO latest : qualityIssueTaskMapper.selectLastHandledTimes(batchIds)) {
                latestById.put(latest.getId(), latest);
            }
        }
        for (QualityIssueTaskDO row : rows) {
            QualityIssueTaskDO counts = countsById.get(row.getId());
            row.setMasterDataCount(counts == null ? 0L : counts.getMasterDataCount());
            row.setIssueDataCount(counts == null ? 0L : counts.getIssueDataCount());
            row.setCompletedIssueCount(counts == null ? 0L : counts.getCompletedIssueCount());
            QualityIssueTaskDO latest = latestById.get(row.getId());
            row.setLastHandledTime(latest == null || latest.getLastHandledTime() == null
                    ? row.getCreateTime() : latest.getLastHandledTime());
        }
    }

    /** 用户信息归系统模块管理；同一请求内重复或缺失的用户只读取一次。 */
    private void fillAssigneeNames(List<QualityIssueTaskDO> rows) {
        Map<Long, SysUser> users = new HashMap<>();
        for (QualityIssueTaskDO row : rows) {
            Long userId = row.getAssigneeId();
            if (userId != null && !users.containsKey(userId)) {
                SysUser user = sysUserService.selectUserById(userId);
                // 历史任务允许展示停用用户的姓名，但不展示已删除用户。
                users.put(userId, user != null && "0".equals(user.getDelFlag()) ? user : null);
            }
            SysUser user = users.get(userId);
            row.setAssigneeName(user == null ? null : user.getNickName());
            row.setAssigneePhone(user == null ? null : user.getPhonenumber());
        }
    }

    private void fillIssueUpdaterPhones(List<QualityIssueTaskIssueItemRespVO> items) {
        Map<Long, String> phones = new HashMap<>();
        for (QualityIssueTaskIssueItemRespVO item : items) {
            Long userId = item.getUpdaterId();
            if (userId != null && !phones.containsKey(userId)) {
                SysUser user = sysUserService.selectUserById(userId);
                phones.put(userId, user != null && "0".equals(user.getDelFlag()) ? user.getPhonenumber() : null);
            }
            item.setUpdaterPhone(phones.get(userId));
        }
    }

    /** 为整改任务的问题项解析主数据记录编码与名称。 */
    private void fillMasterInfo(Long modelId, List<QualityIssueTaskIssueItemRespVO> issueItems,
                                List<ModelPublishedColumnRespDTO> columns) {
        String[] fieldCodes = resolveMasterFieldCodes(columns);
        String codeField = fieldCodes[0];
        String nameField = fieldCodes[1];
        if (codeField == null && nameField == null) {
            return;
        }
        List<Long> recordIds = issueItems.stream().map(QualityIssueTaskIssueItemRespVO::getMasterRecordId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Map<String, String>> recordValues = loadSnapshotFieldValues(
                modelId, recordIds, Arrays.asList(codeField, nameField));
        for (QualityIssueTaskIssueItemRespVO item : issueItems) {
            Map<String, String> values = recordValues.get(item.getMasterRecordId());
            if (values != null) {
                item.setMasterCode(codeField == null ? null : values.get(codeField));
                item.setMasterName(nameField == null ? null : values.get(nameField));
            }
        }
    }

    /** 解析模型下「编码字段」「名称字段」对应的快照 JSON 字段 code。 */
    private String[] resolveMasterFieldCodes(List<ModelPublishedColumnRespDTO> columns) {
        String nameField = columns.stream()
                .filter(column -> Boolean.TRUE.equals(column.getNameFlag()))
                .map(ModelPublishedColumnRespDTO::getCode)
                .findFirst().orElse(null);
        // 编码字段：老模型为自动编码(AUTOMATIC)；新模型为手动编码，退回按字段 code 以 _CODE 结尾识别。
        String codeField = columns.stream()
                .filter(column -> "AUTOMATIC".equals(column.getEncodingMode()))
                .map(ModelPublishedColumnRespDTO::getCode)
                .findFirst()
                .orElseGet(() -> columns.stream()
                        .filter(column -> column.getCode() != null && column.getCode().toUpperCase().endsWith("_CODE"))
                        .map(ModelPublishedColumnRespDTO::getCode)
                        .findFirst().orElse(null));
        return new String[]{codeField, nameField};
    }

    /** 每次调用只查询同一主数据记录的快照一次。 */
    private Map<Long, Map<String, String>> loadSnapshotFieldValues(Long modelId, Collection<Long> recordIds,
                                                                    Collection<String> fieldCodes) {
        Map<Long, Map<String, String>> valuesByRecordId = new HashMap<>();
        for (Long recordId : recordIds) {
            if (recordId == null || valuesByRecordId.containsKey(recordId)) {
                continue;
            }
            Map<String, String> values = new HashMap<>();
            valuesByRecordId.put(recordId, values);
            String snapshot = qualityIssueTaskMapper.selectSnapshotData(modelId, recordId);
            if (snapshot == null || snapshot.trim().isEmpty()) {
                continue;
            }
            try {
                JsonNode node = OBJECT_MAPPER.readTree(snapshot);
                if (node != null && node.isObject()) {
                    for (String fieldCode : fieldCodes) {
                        if (fieldCode != null) {
                            values.put(fieldCode, getSnapshotField(node, fieldCode));
                        }
                    }
                }
            } catch (JsonProcessingException ex) {
                log.warn("解析主数据快照失败，modelId={}, masterRecordId={}", modelId, recordId, ex);
            }
        }
        return valuesByRecordId;
    }

    /** 大小写不敏感地从快照 JSON 中取字段值。 */
    private String getSnapshotField(JsonNode node, String fieldCode) {
        JsonNode value = node.get(fieldCode);
        if (value != null && !value.isNull()) {
            return value.asText();
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (fieldCode.equalsIgnoreCase(entry.getKey())) {
                JsonNode v = entry.getValue();
                return v == null || v.isNull() ? null : v.asText();
            }
        }
        return null;
    }

    @Override
    public QualityIssueTaskDO getQualityIssueTaskById(Long id) {
        QualityIssueTaskDO task = qualityIssueTaskMapper.selectById(id);
        if (task == null) {
            throw new ServiceException("整改任务不存在");
        }
        return task;
    }

    @Override
    public Long createQualityIssueTask(QualityIssueTaskSaveReqVO request) {
        String taskName = request.getTaskName();
        if (taskName == null || taskName.trim().isEmpty() || taskName.length() > 128) {
            throw new ServiceException("请填写128个字符以内的整改任务名称");
        }
        if (request.getAssigneeId() == null || request.getAssigneeId() <= 0) {
            throw new ServiceException("请选择整改负责人");
        }
        List<QualityIssueInfoDO> selected = resolveDispatchItems(request);
        Map<Long, QualityIssueDO> issues = new LinkedHashMap<>();
        // 所有入口先按父问题 ID 加锁，再锁问题项，避免批量下发锁顺序相反。
        for (Long issueId : selected.stream().map(QualityIssueInfoDO::getIssueId).distinct().sorted().collect(Collectors.toList())) {
            QualityIssueDO issue = qualityIssueTaskMapper.lockIssue(issueId);
            if (issue == null || "3".equals(issue.getStatus()) || Boolean.FALSE.equals(issue.getValidFlag())) {
                throw new ServiceException("所选问题不存在、已失效或已关闭，未下发任何任务");
            }
            if (issue.getModelId() == null || issue.getMasterRecordId() == null) {
                throw new ServiceException("问题关联的模型或主数据记录信息缺失，不能下发");
            }
            issues.put(issueId, issue);
        }
        Long modelId = issues.values().iterator().next().getModelId();
        if (issues.values().stream().anyMatch(issue -> !modelId.equals(issue.getModelId()))) {
            throw new ServiceException("同一整改任务请选择同一主数据模型的问题");
        }
        ModelPublishedStructureRespDTO structure = findPublishedStructure(modelId);
        if (structure == null) {
            throw new ServiceException("问题关联的模型不存在或未发布，不能下发");
        }
        SysUser assignee = sysUserService.selectUserById(request.getAssigneeId());
        if (assignee == null || !"0".equals(assignee.getDelFlag()) || !"0".equals(assignee.getStatus())) {
            throw new ServiceException("整改负责人不存在或已停用");
        }
        List<QualityIssueInfoDO> items = new ArrayList<>();
        for (QualityIssueInfoDO candidate : selected) {
            QualityIssueInfoDO item = qualityIssueTaskMapper.lockIssueItem(candidate.getId());
            if (item == null || !Objects.equals(item.getIssueId(), candidate.getIssueId())) {
                throw new ServiceException("所选问题项已变化，请刷新后重新下发");
            }
            if (!"1".equals(item.getStatus()) || Boolean.FALSE.equals(item.getValidFlag()) || item.getQualityRuleId() == null) {
                throw new ServiceException("所选问题项已解决、失效或缺少规则，未下发任何任务");
            }
            items.add(item);
        }
        for (QualityIssueInfoDO item : items) {
            validateRedispatch(issues.get(item.getIssueId()), item,
                    qualityIssueTaskInfoService.listByIssueInfoId(item.getId()));
        }
        QualityIssueTaskDO task = new QualityIssueTaskDO();
        task.setTaskNo("RT" + UUID.randomUUID().toString().replace("-", ""));
        task.setTaskName(taskName.trim());
        String taskDescription = request.getTaskDescription();
        task.setRequirement(taskDescription == null || taskDescription.trim().isEmpty() ? null : taskDescription.trim());
        task.setModelId(modelId);
        task.setAssigneeId(request.getAssigneeId());
        task.setPriority(request.getPriority());
        task.setRectificationTime(request.getRectificationTime());
        task.setStatus(TASK_STATUS_RECTIFYING);
        task.setValidFlag(true);
        task.setDelFlag(false);
        task.setCreatorId(request.getCreatorId());
        task.setCreateBy(request.getCreateBy());
        task.setCreateTime(request.getCreateTime());
        task.setUpdatorId(request.getCreatorId());
        task.setUpdateBy(request.getCreateBy());
        task.setUpdateTime(request.getCreateTime());
        if (qualityIssueTaskMapper.insert(task) != 1) {
            throw new ServiceException("整改任务下发失败");
        }
        List<Long> recordIds = issues.values().stream().map(QualityIssueDO::getMasterRecordId)
                .distinct().collect(Collectors.toList());
        List<Long> fieldIds = items.stream().map(QualityIssueInfoDO::getModelColumnId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        LogMetadata logMetadata = loadLogMetadata(modelId, structure, recordIds, fieldIds);
        for (QualityIssueInfoDO item : items) {
            QualityIssueDO issue = issues.get(item.getIssueId());
            QualityIssueTaskInfoDO info = new QualityIssueTaskInfoDO();
            info.setTaskId(task.getId());
            info.setIssueInfoId(item.getId());
            info.setIssueId(item.getIssueId());
            info.setQualityRuleId(item.getQualityRuleId());
            info.setMasterRecordId(issue.getMasterRecordId());
            info.setStatus(INFO_STATUS_PENDING);
            info.setValidFlag(true);
            info.setDelFlag(false);
            info.setCreatorId(request.getCreatorId());
            info.setCreateBy(request.getCreateBy());
            info.setCreateTime(request.getCreateTime());
            info.setUpdatorId(request.getCreatorId());
            info.setUpdateBy(request.getCreateBy());
            info.setUpdateTime(request.getCreateTime());
            if (!qualityIssueTaskInfoService.save(info)) {
                throw new ServiceException("整改问题项下发失败");
            }
            appendLog(task, info, "ASSIGN", "下发整改任务", request.getCreatorId(), request.getCreateBy(),
                    item.getModelColumnId(), logMetadata);
        }
        for (Long issueId : issues.keySet()) {
            if (qualityIssueTaskMapper.markIssueDispatched(issueId, request.getCreatorId(),
                    request.getCreateBy()) != 1) {
                throw new ServiceException("问题状态更新失败");
            }
        }
        return task.getId();
    }

    /** 新版按问题项主键下发；旧版单项参数必须唯一定位，不按规则猜测多个候选项。 */
    private List<QualityIssueInfoDO> resolveDispatchItems(QualityIssueTaskSaveReqVO request) {
        List<QualityIssueInfoDO> items;
        if (request.getIssueInfoIds() != null) {
            List<Long> ids = request.getIssueInfoIds();
            if (request.getIssueId() != null || request.getQualityRuleId() != null) {
                throw new ServiceException("问题项集合与旧版单项参数不能同时传入");
            }
            if (ids.isEmpty() || ids.size() > 500 || ids.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new ServiceException("一次请选择1至500个有效问题项");
            }
            ids = ids.stream().distinct().sorted().collect(Collectors.toList());
            items = qualityIssueTaskMapper.selectIssueItemsByIds(ids);
            if (items.size() != ids.size()) {
                throw new ServiceException("部分问题项不存在或已删除，请刷新后重新选择");
            }
        } else {
            if (request.getIssueId() == null || request.getIssueId() <= 0
                    || request.getQualityRuleId() == null || request.getQualityRuleId() <= 0) {
                throw new ServiceException("请选择具体问题项");
            }
            // 旧入口也先锁父问题，保持与新版相同的锁顺序。
            if (qualityIssueTaskMapper.lockIssue(request.getIssueId()) == null) {
                throw new ServiceException("问题不存在，不能下发");
            }
            items = qualityIssueTaskMapper.selectIssueItemsForUpdate(request.getIssueId(), request.getQualityRuleId());
            if (items.size() != 1) {
                throw new ServiceException("问题项关联缺失或不唯一，请核对问题数据");
            }
        }
        if (items.stream().anyMatch(item -> item.getId() == null || item.getIssueId() == null)) {
            throw new ServiceException("问题项缺少主键或所属问题，不能下发");
        }
        return items.stream().sorted(java.util.Comparator.comparing(QualityIssueInfoDO::getId)).collect(Collectors.toList());
    }

    /** 已整改的问题项必须有后续检测仍失败的证据，不能把整改完成当作问题解决。 */
    private void validateRedispatch(QualityIssueDO issue, QualityIssueInfoDO item,
            List<QualityIssueTaskInfoDO> previous) {
        if (previous.stream().anyMatch(info -> info.getQualityRuleId() == null
                || Arrays.asList(INFO_STATUS_PENDING, INFO_STATUS_PROCESSING).contains(info.getStatus()))) {
            throw new ServiceException("同一问题项存在未完成任务，或旧任务关联不明确，不能重复下发");
        }
        Date completed = previous.stream().filter(info -> INFO_STATUS_COMPLETED.equals(info.getStatus()))
                .map(QualityIssueTaskInfoDO::getRectificationTime).filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        if (previous.stream().anyMatch(info -> INFO_STATUS_COMPLETED.equals(info.getStatus()) && info.getRectificationTime() == null)) {
            throw new ServiceException("旧任务缺少完成时间，无法确认再次检测结果");
        }
        if (completed != null) {
            QualityTaskInstanceDO check = item.getLastCheckInstanceId() == null ? null
                    : qualityTaskInstanceMapper.selectById(item.getLastCheckInstanceId());
            if (check == null || !Objects.equals(check.getModelId(), issue.getModelId())
                    || !Arrays.asList("3", "4").contains(check.getStatus()) || check.getStartTime() == null
                    || !check.getStartTime().after(completed) || check.getEndTime() == null
                    || item.getLastFoundTime() == null || item.getLastFoundTime().before(check.getStartTime())
                    || item.getLastFoundTime().after(check.getEndTime()) || Boolean.FALSE.equals(check.getValidFlag())) {
                throw new ServiceException("尚无任务完成后再次检测仍失败的有效结果");
            }
        }
    }

    @Override
    public int deleteQualityIssueTasks(List<Long> ids, Long userId, String userName) {
        if (ids == null || ids.isEmpty() || ids.size() > 500 || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ServiceException("一次请选择1至500条有效整改任务");
        }

        List<Long> taskIds = ids.stream().distinct().sorted().collect(Collectors.toList());
        List<QualityIssueTaskInfoDO> items = new ArrayList<>();
        for (Long taskId : taskIds) {
            QualityIssueTaskDO task = qualityIssueTaskMapper.lockTask(taskId);
            if (task == null) {
                throw new ServiceException("部分整改任务不存在或已删除，请刷新后重试");
            }
            List<QualityIssueTaskInfoDO> taskItems = qualityIssueTaskInfoService.listByTaskId(taskId);
            if (!TASK_STATUS_RECTIFYING.equals(task.getStatus())
                    || taskItems.stream().anyMatch(item -> !INFO_STATUS_PENDING.equals(item.getStatus()))) {
                throw new ServiceException("仅可删除尚未开始整改的任务，本次未删除任何任务");
            }
            items.addAll(taskItems);
        }

        List<Long> issueIds = items.stream().map(QualityIssueTaskInfoDO::getIssueId)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        // 与下发共用父问题锁，避免恢复问题状态时覆盖新下发的任务。
        for (Long issueId : issueIds) {
            qualityIssueTaskMapper.lockIssue(issueId);
        }
        qualityIssueTaskInfoService.remove(new LambdaQueryWrapper<QualityIssueTaskInfoDO>()
                .in(QualityIssueTaskInfoDO::getTaskId, taskIds));
        qualityIssueTaskLogMapper.delete(new LambdaQueryWrapper<QualityIssueTaskLogDO>()
                .in(QualityIssueTaskLogDO::getQualityIssueTaskId, taskIds));
        int deleted = qualityIssueTaskMapper.delete(new LambdaQueryWrapper<QualityIssueTaskDO>()
                .in(QualityIssueTaskDO::getId, taskIds));
        if (deleted != taskIds.size()) {
            throw new ServiceException("整改任务删除失败，请刷新后重试");
        }
        for (Long issueId : issueIds) {
            qualityIssueTaskMapper.restoreUndispatchedIssue(issueId, userId, userName);
        }
        return deleted;
    }

    @Override
    public int updateQualityIssueTaskStatus(QualityIssueTaskInfoSaveReqVO request) {
        if (request.getId() == null || !Arrays.asList(INFO_STATUS_PROCESSING, INFO_STATUS_COMPLETED).contains(request.getStatus())) {
            throw new ServiceException("仅支持开始整改和完成整改");
        }
        // 统一先锁主任务，再锁明细，避免与提交审批同时修改问题项。
        QualityIssueTaskInfoDO candidate = qualityIssueTaskInfoService.getById(request.getId());
        if (candidate == null) {
            throw new ServiceException("整改问题项不存在");
        }
        QualityIssueTaskDO task = qualityIssueTaskMapper.lockTask(candidate.getTaskId());
        QualityIssueTaskInfoDO info = qualityIssueTaskInfoService.lockInfo(request.getId());
        if (task == null || info == null || !Objects.equals(info.getTaskId(), task.getId())
                || Boolean.FALSE.equals(task.getValidFlag()) || Boolean.FALSE.equals(info.getValidFlag())) {
            throw new ServiceException("整改任务或问题项不存在、已失效");
        }
        if (TASK_STATUS_COMPLETED.equals(task.getStatus()) || TASK_STATUS_CANCELLED.equals(task.getStatus())) {
            throw new ServiceException("整改任务已经结束");
        }
        if (!Objects.equals(task.getAssigneeId(), SecurityUtils.getUserId())) {
            throw new ServiceException("仅负责人可以处理本人任务");
        }
        String targetStatus = request.getStatus();
        String currentStatus = info.getStatus();
        // 开始整改：待整改 -> 整改中；修改结果：已整改 -> 整改中；完成整改：整改中 -> 已整改。
        boolean validTransition = INFO_STATUS_PROCESSING.equals(targetStatus)
                ? INFO_STATUS_PENDING.equals(currentStatus) || INFO_STATUS_COMPLETED.equals(currentStatus)
                : INFO_STATUS_PROCESSING.equals(currentStatus);
        if (!validTransition) {
            throw new ServiceException("问题项状态已变化，请刷新后重试");
        }
        if (request.getRequirement() != null && request.getRequirement().length() > NOTE_MAX_LENGTH) {
            throw new ServiceException("整改说明长度不能超过512个字符");
        }
        Date now = request.getUpdateTime() == null ? new Date() : request.getUpdateTime();
        int rows = qualityIssueTaskInfoService.updateInfoStatus(info.getId(), currentStatus,
                targetStatus, request.getRequirement(),
                INFO_STATUS_COMPLETED.equals(targetStatus) ? now : null,
                request.getUpdatorId(), request.getUpdateBy(), now);
        if (rows != 1) {
            throw new ServiceException("问题项状态已变化，请刷新后重试");
        }
        if (INFO_STATUS_COMPLETED.equals(targetStatus) && TASK_STATUS_RECTIFYING.equals(task.getStatus())) {
            List<QualityIssueTaskInfoDO> items = qualityIssueTaskInfoService.listByTaskId(task.getId());
            if (!items.isEmpty() && items.stream().allMatch(item -> INFO_STATUS_COMPLETED.equals(item.getStatus()))
                    && qualityIssueTaskMapper.markReadyForApproval(task.getId(), request.getUpdatorId(),
                    request.getUpdateBy(), now) != 1) {
                throw new ServiceException("整改任务状态已变化，请刷新后重试");
            }
        }
        // 已整改回退为整改中时，将待审批/审批中的任务回退为处理中。
        if (INFO_STATUS_PROCESSING.equals(targetStatus) && INFO_STATUS_COMPLETED.equals(currentStatus)
                && (TASK_STATUS_PENDING_APPROVAL.equals(task.getStatus())
                || TASK_STATUS_IN_APPROVAL.equals(task.getStatus()))) {
            if (qualityIssueTaskMapper.revertToRectifying(task.getId(), request.getUpdatorId(),
                    request.getUpdateBy(), now) != 1) {
                throw new ServiceException("整改任务状态已变化，请刷新后重试");
            }
        }
        // 已整改回退为整改中时记录「修改结果」，其余沿用开始/完成整改语义。
        String action;
        String description;
        if (INFO_STATUS_PROCESSING.equals(targetStatus) && INFO_STATUS_COMPLETED.equals(currentStatus)) {
            action = "MODIFY";
            description = "修改结果";
        } else {
            action = INFO_STATUS_PROCESSING.equals(targetStatus) ? "START" : "COMPLETE";
            description = INFO_STATUS_PROCESSING.equals(targetStatus) ? "开始整改" : "完成整改";
        }
        Long fieldId = qualityIssueTaskMapper.selectIssueFieldId(info.getIssueInfoId());
        LogMetadata logMetadata = loadLogMetadata(task.getModelId(),
                findPublishedStructure(task.getModelId()),
                Collections.singletonList(info.getMasterRecordId()), Collections.singletonList(fieldId));
        appendLog(task, info, action, description, request.getUpdatorId(), request.getUpdateBy(), fieldId, logMetadata);
        return 1;
    }

    @Override
    public int updateApprovalStatus(Long id, boolean submit, Long updaterId, String updateBy) {
        QualityIssueTaskDO task = qualityIssueTaskMapper.lockTask(id);
        if (task == null || Boolean.FALSE.equals(task.getValidFlag())) {
            throw new ServiceException("整改任务不存在或已失效");
        }
        String expectedStatus = submit ? TASK_STATUS_PENDING_APPROVAL : TASK_STATUS_IN_APPROVAL;
        String targetStatus = submit ? TASK_STATUS_IN_APPROVAL : TASK_STATUS_PENDING_APPROVAL;
        if (!expectedStatus.equals(task.getStatus())) {
            throw new ServiceException("整改任务状态已变化，请刷新后重试");
        }
        if (submit) {
            List<QualityIssueTaskInfoDO> items = qualityIssueTaskInfoService.listByTaskId(id);
            if (items.isEmpty() || items.stream().anyMatch(item -> !INFO_STATUS_COMPLETED.equals(item.getStatus()))) {
                throw new ServiceException("所有问题项完成整改后才能提交审批");
            }
        }
        int rows = qualityIssueTaskMapper.updateApprovalStatus(id, expectedStatus, targetStatus,
                updaterId, updateBy, new Date());
        if (rows != 1) {
            throw new ServiceException("整改任务状态已变化，请刷新后重试");
        }
        return rows;
    }

    /** 在一次状态操作或下发中复用模型标签和主数据字段值。 */
    private LogMetadata loadLogMetadata(Long modelId, ModelPublishedStructureRespDTO structure,
                                        Collection<Long> recordIds, Collection<Long> fieldIds) {
        Map<Long, ModelPublishedColumnRespDTO> fields = indexFields(structure);
        List<String> fieldCodes = fieldIds.stream().map(fields::get).filter(Objects::nonNull)
                .map(ModelPublishedColumnRespDTO::getCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Map<String, String>> values = fieldCodes.isEmpty() ? Collections.emptyMap()
                : loadSnapshotFieldValues(modelId, recordIds, fieldCodes);
        ModelPublishedRespDTO model = structure == null ? null : structure.getModel();
        return new LogMetadata(model == null ? null : model.getName(), fields, values);
    }

    private static final class LogMetadata {
        private final String modelName;
        private final Map<Long, ModelPublishedColumnRespDTO> fields;
        private final Map<Long, Map<String, String>> values;

        private LogMetadata(String modelName, Map<Long, ModelPublishedColumnRespDTO> fields,
                            Map<Long, Map<String, String>> values) {
            this.modelName = modelName;
            this.fields = fields;
            this.values = values;
        }
    }

    private void appendLog(QualityIssueTaskDO task, QualityIssueTaskInfoDO info,
                           String action, String description, Long actorId, String actorName,
                           Long fieldId, LogMetadata metadata) {
        Date now = new Date();
        QualityIssueTaskLogDO entry = new QualityIssueTaskLogDO();
        entry.setQualityIssueTaskId(task.getId());
        entry.setIssueId(info.getIssueId());
        entry.setTaskInfoId(info.getId());
        // 整改前值取自主数据快照，整改后值留空。
        ModelPublishedColumnRespDTO field = metadata.fields.get(fieldId);
        Map<String, String> values = metadata.values.get(info.getMasterRecordId());
        entry.setBeforeValue(field == null || values == null ? null : values.get(field.getCode()));
        entry.setAfterValue(null);
        entry.setRectificationField(field == null ? null : field.getName());
        entry.setMasterDataObject(metadata.modelName);

        entry.setTaskSendId(task.getCreatorId());
        entry.setTaskHandlerId(actorId);
        entry.setSendTime(task.getCreateTime());
        entry.setHandlerTime(now);
        entry.setCreateTime(now);
        entry.setCreatorId(actorId);
        entry.setCreateBy(actorName);
        entry.setDescription("问题项 " + info.getIssueInfoId() + "：" + description);
        entry.setRemark("rectification:v1:" + action);
        entry.setValidFlag(true);
        entry.setDelFlag(false);
        if (qualityIssueTaskLogMapper.insert(entry) != 1) {
            throw new ServiceException("处理记录保存失败");
        }
    }

    @Override
    public List<QualityIssueTaskDO> getQualityIssueTaskList() {
        QualityIssueTaskPageReqVO request = new QualityIssueTaskPageReqVO();
        request.setPageSize(PageParam.PAGE_SIZE_NONE);
        return getQualityIssueTaskPage(request).getRows();
    }

    @Override
    public Map<Long, QualityIssueTaskDO> getQualityIssueTaskMap() {
        return getQualityIssueTaskList().stream().collect(Collectors.toMap(
                QualityIssueTaskDO::getId, task -> task, (existing, replacement) -> existing));
    }
}
