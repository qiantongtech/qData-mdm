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

package tech.qiantong.qdata.mdm.master.service.data.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.DateUtils;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSourceSystemRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataDetailRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterTreeNodeRespVO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestItemQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.data.MasterDataQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.data.MasterDataTreeNodeDTO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestItemMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.source.MasterSourceRelMapper;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.RequestContext;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.TableAccess;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataService;
import tech.qiantong.qdata.mdm.master.service.data.navigation.MasterClassificationNavigationService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataVersionService;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于模型元数据和目标数据源的权威主数据查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataServiceImpl implements IMasterDataService {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");
    private static final Set<String> STATUSES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("1", "2", "3", "4")));
    private static final Set<String> APPROVAL_STATUSES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("0", "1", "2", "3", "4")));

    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository dataQueryRepository;
    private final IMasterDataVersionService dataVersionService;
    private final MasterChangeRequestMapper changeRequestMapper;
    private final MasterChangeRequestItemMapper changeRequestItemMapper;
    private final MasterSourceRelMapper sourceRelMapper;
    private final ISysUserService sysUserService;
    private final MasterClassificationNavigationService classificationNavigationService;

    /**
     * 查询主数据或子模型分页数据。
     *
     * @param instanceId 模型物化实例 ID
     * @param req 查询参数
     * @return 当前模型版本下的分页记录及统计信息
     */
    @Override
    public MasterDataRespVO getData(Long instanceId, MasterDataPageReqVO req) {
        return queryData(instanceId, req, false);
    }

    /** 按成功发布结构读取增量当前值，复用字段映射和类型转换，不经过管理列表的统计及扩展查询。 */
    @Override
    public List<Map<String, Object>> getSyncData(Long instanceId, Long modelId, Long modelVersionId, Date updatedAfter, String status) {
        MasterModelRespDTO model = metadataService.getModel(instanceId, modelId);
        if (modelVersionId == null || !modelVersionId.equals(model.getModelVersionId())) {
            throw new ServiceException("同步请求与成功发布版本不一致，请重新获取已发布模型");
        }
        String table = tableName(model);
        Set<String> physical = availableColumns(model, table);
        validateFields(model, physical);
        LinkedHashSet<String> selected = selectColumns(model.getFields());
        try {
            return dataQueryRepository.selectSyncList(model.getDatasourceId(), table, model.getColumnMappings(),
                    selected, updatedAfter, status).stream()
                    .map(raw -> normalizeRecordRow(raw, selected, model.getFields(), true))
                    .collect(Collectors.toList());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("读取主数据增量失败，instanceId={}", instanceId, ex);
            throw new ServiceException("主数据增量查询失败，请检查物理表");
        }
    }

    @Override
    public MasterDataRespVO getCategoryData(Long instanceId, MasterDataPageReqVO req) {
        return queryData(instanceId, req, true);
    }

    @Override
    public List<MasterSourceSystemRespVO> getSourceSystems(Long instanceId, Long modelId) {
        Long publishedModelId = metadataService.getPublishedModel(instanceId, modelId).getId();
        return sourceRelMapper.selectSourceSystems(publishedModelId).stream().map(source -> {
            MasterSourceSystemRespVO item = new MasterSourceSystemRespVO();
            item.setId(source.getId() == null ? null : String.valueOf(source.getId()));
            item.setName(source.getName());
            return item;
        }).collect(Collectors.toList());
    }

    private MasterDataRespVO queryData(Long instanceId, MasterDataPageReqVO req, boolean categoryList) {
        if (instanceId == null || instanceId <= 0) throw new ServiceException("模型实例ID格式无效");
        MasterModelRespDTO model = metadataService.getPublishedDefinition(instanceId, req.getModelId(), true);
        if (req.getModelVersionId() != null && !req.getModelVersionId().equals(model.getModelVersionId())) {
            throw new ServiceException("列表与成功发布版本不一致，请返回卡片列表后重新进入");
        }
        if (req.getOrderByColumn() != null && model.getFields().stream().anyMatch(field ->
                field.getCode().equalsIgnoreCase(req.getOrderByColumn().trim()) && "TEXT".equalsIgnoreCase(field.getDataType()))) {
            throw new ServiceException("长文本字段不支持排序");
        }
        if (categoryList && !"CATEGORY".equals(model.getModelType())) {
            throw new ServiceException("普通模型不支持分类树形列表");
        }
        String table = tableName(model);
        String nameCode = nameField(model);
        List<MasterFieldRespDTO> visible = visibleFields(model);

        MasterDataRespVO response = new MasterDataRespVO();
        response.setModelId(model.getModelId());
        response.setInstanceId(model.getInstanceId());
        response.setModelVersionId(model.getModelVersionId());

        MasterDataQueryDTO query = new MasterDataQueryDTO();
        query.setModelId(model.getModelId());
        query.setColumnMappings(model.getColumnMappings());
        query.setNameColumn(nameCode);
        if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
            String status = req.getStatus().trim();
            if (!STATUSES.contains(status)) throw new ServiceException("无效的主数据状态");
            query.setStatus(status);
        }
        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            query.setName(req.getName().trim());
        }
        classificationNavigationService.applyFilter(model, req, query);
        try {
            if (req.getSourceSystem() != null && !req.getSourceSystem().trim().isEmpty()) {
                Long sourceSystemId = masterId(req.getSourceSystem().trim());
                if (sourceSystemId <= 0) throw new ServiceException("来源系统ID格式无效");
                query.setRecordIds(sourceRelMapper.selectMasterRecordIdsBySourceSystem(
                        model.getModelId(), sourceSystemId));
            }
            if (req.getApprovalStatus() != null && !req.getApprovalStatus().trim().isEmpty()) {
                String approvalStatus = req.getApprovalStatus().trim();
                if (!APPROVAL_STATUSES.contains(approvalStatus)) throw new ServiceException("无效的审批状态");
                List<Long> approvalRecordIds = changeRequestItemMapper.selectLatestApprovalRecordIds(
                        model.getModelId(), approvalStatus);
                if (query.getRecordIds() == null) {
                    query.setRecordIds(approvalRecordIds);
                } else {
                    Set<Long> recordIds = new LinkedHashSet<>(query.getRecordIds());
                    recordIds.retainAll(approvalRecordIds);
                    query.setRecordIds(recordIds);
                }
            }
            LinkedHashSet<String> selectColumns = model.getFields().stream()
                    .map(MasterFieldRespDTO::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
            List<Map<String, Object>> rawRows;
            try (RequestContext context = dataQueryRepository.openRequest()) {
                TableAccess access = context.forTable(model.getDatasourceId(), table, model.getColumnMappings());
                fillSummary(response, model.getModelId(), access);
                if (req.getTreeId() != null && !req.getTreeId().trim().isEmpty()) {
                    if (!"CATEGORY".equals(model.getModelType())) throw new ServiceException("普通模型不支持分类节点筛选");
                    List<MasterDataTreeNodeDTO> nodes = dataQueryRepository.selectTreeNodes(access, nameCode);
                    Set<String> ids = descendantIds(nodes, req.getTreeId().trim());
                    if (ids.isEmpty()) throw new ServiceException("分类节点不存在");
                    query.setCategoryIds(ids.stream().map(this::masterId).collect(Collectors.toCollection(LinkedHashSet::new)));
                }
                if (categoryList) {
                    selectColumns.add("CATEGORY_ID");
                    rawRows = dataQueryRepository.selectCategoryList(access, query, selectColumns, req);
                    response.setTotal((long) rawRows.size());
                } else {
                    response.setTotal(dataQueryRepository.count(access, query));
                    if (response.getTotal() == 0L) {
                        response.setRows(Collections.emptyList());
                        return response;
                    }
                    int pageNum = req.getPageNum() == null ? 1 : Math.max(1, req.getPageNum());
                    int pageSize = req.getPageSize() == null ? 10 : Math.max(1, Math.min(100, req.getPageSize()));
                    long offset = (long) (pageNum - 1) * pageSize;
                    rawRows = dataQueryRepository.selectPage(access, query, selectColumns, req, offset, pageSize);
                }
            }
            List<Map<String, Object>> rows = rawRows.stream()
                    .map(raw -> normalizeRecordRow(raw, selectColumns, visible, false)).collect(Collectors.toList());
            if (categoryList) validateCategoryRows(rows, query);
            fillRowExtensions(model.getModelId(), rows);
            response.setRows(rows);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询权威主数据失败，instanceId={}", instanceId, ex);
            throw new ServiceException("主数据查询失败，请检查物理表");
        }
    }

    /** 筛选后允许未命中的父节点缺席，与模型目录树表的组树规则一致。 */
    private void validateCategoryRows(List<Map<String, Object>> rows, MasterDataQueryDTO query) {
        Map<String, String> parents = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            if (row.get("ID") == null) throw new ServiceException("分类数据缺少主数据ID");
            String id = String.valueOf(row.get("ID"));
            if (parents.containsKey(id)) throw new ServiceException("分类数据存在重复的主数据ID");
            // 当前分类模型约定：CATEGORY_ID指向上级记录ID，空值或0表示根节点。
            Object parent = row.get("CATEGORY_ID");
            parents.put(id, parent == null ? null : String.valueOf(parent));
        }
        boolean filtered = query.getName() != null || query.getStatus() != null
                || query.getRecordIds() != null || query.getCategoryIds() != null;
        Set<String> checked = new HashSet<>();
        for (Map.Entry<String, String> entry : parents.entrySet()) {
            String parent = entry.getValue();
            if (!filtered && parent != null && !parent.isEmpty() && !"0".equals(parent) && !parents.containsKey(parent)) {
                throw new ServiceException("分类数据存在无效的父节点");
            }
            Set<String> path = new HashSet<>();
            String current = entry.getKey();
            while (current != null && parents.containsKey(current) && !checked.contains(current)) {
                if (!path.add(current)) throw new ServiceException("分类数据存在循环引用");
                current = parents.get(current);
            }
            checked.addAll(path);
        }
    }

    /**
     * 查询正式主数据详情，并校验记录所属的物化实例。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param modelId 当前页面的主模型ID
     * @return 主数据详情及字段定义
     */
    @Override
    public MasterDataDetailRespVO getDetail(Long instanceId, Long masterDataId, Long modelId) {
        if (instanceId == null || instanceId <= 0) throw new ServiceException("模型实例ID格式无效");
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        MasterModelRespDTO model = metadataService.getPublishedDefinition(instanceId, modelId, true);
        String table = tableName(model);
        List<MasterColumnRespVO> columns = metadataService.getColumns(model);
        LinkedHashSet<String> selected = selectColumns(model.getFields());
        try {
            Map<String, Object> row;
            try (RequestContext context = dataQueryRepository.openRequest()) {
                TableAccess access = context.forTable(model.getDatasourceId(), table, model.getColumnMappings());
                row = readRecord(dataQueryRepository.selectDetail(access, selected, masterDataId), selected, model.getFields());
            }
            fillRowExtensions(model.getModelId(), Collections.singletonList(row));
            MasterDataDetailRespVO response = new MasterDataDetailRespVO();
            Object recordName = row.get(nameField(model));
            response.setRecordName(recordName == null ? null : String.valueOf(recordName));
            response.setBusinessColumns(columns.stream().filter(c -> Boolean.TRUE.equals(c.getBusinessField()))
                    .collect(Collectors.toList()));
            List<MasterColumnRespVO> system = columns.stream().filter(c -> !Boolean.TRUE.equals(c.getBusinessField()))
                    .collect(Collectors.toList());
            appendColumn(system, "MASTER_VERSION_NO", "当前数据版本", null);
            response.setSystemColumns(system);
            appendColumn(columns, "MASTER_APPROVAL_STATUS", "审批状态", "master_change_request_status");
            appendColumn(columns, "MASTER_VERSION_NO", "当前数据版本", null);
            appendColumn(columns, "MASTER_APPROVAL_REQUEST_NO", "关联申请编号", null);
            appendColumn(columns, "MASTER_APPROVAL_CHANGE_TYPE", "申请变更类型", null);
            response.setColumns(columns);
            response.setRow(row);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询单条权威主数据失败，instanceId={}, masterDataId={}", instanceId, masterDataId, ex);
            throw new ServiceException("主数据详情查询失败，请检查物理表");
        }
    }

    @Override
    public Map<String, Object> getRecord(Long instanceId, Long masterDataId) {
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        String table = tableName(model);
        Set<String> physical = availableColumns(model, table);
        validateFields(model, physical);
        LinkedHashSet<String> selected = selectColumns(model.getFields());
        try {
            return readRecord(dataQueryRepository.selectDetail(model.getDatasourceId(), table, selected, masterDataId,
                    model.getColumnMappings()), selected, model.getFields());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询主数据业务记录失败，instanceId={}, masterDataId={}", instanceId, masterDataId, ex);
            throw new ServiceException("主数据详情查询失败，请检查物理表");
        }
    }

    private Map<String, Object> readRecord(List<Map<String, Object>> raw, Set<String> selected,
            List<MasterFieldRespDTO> fields) {
        if (raw.isEmpty()) throw new ServiceException("主数据不存在");
        if (raw.size() > 1) throw new ServiceException("主数据ID存在重复记录");
        return normalizeRecordRow(raw.get(0), selected, fields, false);
    }

    private List<MasterFieldRespDTO> visibleFields(MasterModelRespDTO model) {
        return model.getFields().stream()
                .filter(f -> !"SYSTEM".equalsIgnoreCase(f.getCategory()))
                .sorted(Comparator.comparing((MasterFieldRespDTO f) -> f.getSortOrder() == null ? 0 : f.getSortOrder())
                        .thenComparing(f -> f.getId() == null ? Long.MAX_VALUE : f.getId()))
                .collect(Collectors.toList());
    }

    private void appendColumn(List<MasterColumnRespVO> columns, String code, String label, String dict) {
        if (columns.stream().anyMatch(c -> code.equalsIgnoreCase(c.getProp()))) return;
        MasterColumnRespVO extension = column(code, label, "STRING", dict);
        extension.setBusinessField(false);
        extension.setSortOrder(columns.stream().map(MasterColumnRespVO::getSortOrder)
                .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(0) + 1);
        columns.add(extension);
    }

    /** 发布绑定已经包含全部启用的业务列和系统列，ID保持首列。 */
    private LinkedHashSet<String> selectColumns(List<MasterFieldRespDTO> fields) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        selected.add("ID");
        for (MasterFieldRespDTO field : fields) selected.add(field.getCode());
        return selected;
    }

    /** 共用字段值转换；同步的系统时间保留毫秒，管理页面维持原来的展示格式。 */
    private Map<String, Object> normalizeRecordRow(Map<String, Object> raw, Set<String> selected,
                                            List<MasterFieldRespDTO> fields, boolean sync) {
        Map<String, String> fieldTypes = fields.stream()
                .filter(field -> field.getCode() != null)
                .collect(Collectors.toMap(field -> field.getCode().toUpperCase(Locale.ROOT),
                        field -> String.valueOf(field.getDataType()), (left, right) -> left));
        Map<String, Object> row = new LinkedHashMap<>();
        for (String code : selected) {
            Object value = raw.get(code.toUpperCase(Locale.ROOT));
            // 平台INTEGER可物化为BIGINT，整数与主键统一以字符串传输，避免前端精度丢失。
            String fieldType = fieldTypes.get(code.toUpperCase(Locale.ROOT));
            boolean bigintId = "ID".equalsIgnoreCase(code) || "PARENT_ID".equalsIgnoreCase(code)
                    || "CATEGORY_ID".equalsIgnoreCase(code);
            if (value instanceof Date || value instanceof LocalDateTime) {
                String dataType = fieldTypes.get(code.toUpperCase(Locale.ROOT));
                String format = "DATE".equalsIgnoreCase(dataType)
                        ? DateUtils.YYYY_MM_DD : DateUtils.YYYY_MM_DD_HH_MM_SS;
                if (sync && ("UPDATED_TIME".equalsIgnoreCase(code) || "UPDATE_TIME".equalsIgnoreCase(code)
                        || "CREATED_TIME".equalsIgnoreCase(code) || "CREATE_TIME".equalsIgnoreCase(code))) {
                    format = "yyyy-MM-dd HH:mm:ss.SSS";
                }
                value = value instanceof Date
                        ? DateUtils.parseDateToStr(format, (Date) value)
                        : LocalDateTimeUtil.format((LocalDateTime) value, format);
            } else if (value instanceof LocalDate) {
                value = LocalDateTimeUtil.format((LocalDate) value, DateUtils.YYYY_MM_DD);
            }
            if (value instanceof java.math.BigDecimal) value = ((java.math.BigDecimal) value).toPlainString();
            else if (value instanceof java.math.BigInteger) value = value.toString();
            else if (value instanceof Long && (bigintId || "BIGINT".equalsIgnoreCase(fieldType)
                    || "INTEGER".equalsIgnoreCase(fieldType))) value = value.toString();
            row.put(code, bigintId && value != null ? String.valueOf(value) : value);
        }
        return row;
    }

    /**
     * 按当前页补齐版本、审批及创建人信息；版本和审批分别批量读取。
     */
    private void fillRowExtensions(Long modelId, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return;
        List<Long> recordIds = rows.stream()
                .map(row -> masterId(String.valueOf(row.get("ID"))))
                .collect(Collectors.toList());
        Map<Long, String> versions = currentVersions(modelId, recordIds);
        Map<Long, ApprovalInfo> approvals = latestApprovals(modelId, recordIds);
        for (Map<String, Object> row : rows) {
            Long recordId = masterId(String.valueOf(row.get("ID")));
            row.put("MASTER_VERSION_NO", versions.get(recordId));
            ApprovalInfo approval = approvals.get(recordId);
            row.put("MASTER_APPROVAL_STATUS", approval == null ? null : approval.status);
            row.put("MASTER_EXECUTE_STATUS", approval == null ? null : approval.executeStatus);
            row.put("MASTER_APPROVAL_REQUEST_ID", approval == null ? null : String.valueOf(approval.requestId));
            row.put("MASTER_APPROVAL_REQUEST_NO", approval == null ? null : approval.requestNo);
            row.put("MASTER_APPROVAL_CHANGE_TYPE", approval == null ? null : approval.changeType);
        }
        fillCreatorExtensions(rows);
    }

    /** 批量补充版本号；重复当前版本异常保留，其他查询失败时列表仍展示但版本号为空。 */
    private Map<Long, String> currentVersions(Long modelId, List<Long> recordIds) {
        try {
            return dataVersionService.getCurrentVersionNumbers(modelId, recordIds);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            // 新项目允许先完成物化数据页面，版本表尚未准备好时列表仍可使用。
            log.warn("权威主数据版本信息暂不可用，modelId={}", modelId, ex);
        }
        return Collections.emptyMap();
    }

    private Map<Long, ApprovalInfo> latestApprovals(Long modelId, List<Long> recordIds) {
        Map<Long, ApprovalInfo> result = new HashMap<>();
        try {
            List<MasterChangeRequestItemQueryDTO> rows = changeRequestItemMapper.selectLatestApprovals(modelId, recordIds);
            for (MasterChangeRequestItemQueryDTO row : rows) {
                Long recordId = row.getRecordId();
                Long requestId = row.getRequestId();
                if (recordId == null || requestId == null) continue;
                String status = row.getStatus();
                ApprovalInfo current = result.get(recordId);
                if (current != null) {
                    if ("0".equals(status) && "0".equals(current.status)
                            && !requestId.equals(current.requestId)) {
                        throw new ServiceException("主数据ID " + recordId + " 存在多个审批中的申请");
                    }
                    continue;
                }
                ApprovalInfo info = new ApprovalInfo();
                info.requestId = requestId;
                info.requestNo = row.getRequestNo();
                info.status = status;
                info.executeStatus = row.getExecuteStatus();
                info.changeType = row.getChangeType();
                result.put(recordId, info);
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("权威主数据审批信息暂不可用，modelId={}", modelId, ex);
        }
        return result;
    }

    /** 补充列表创建人的姓名和电话，列表列本身仍只暴露一个创建人字段。 */
    private void fillCreatorExtensions(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> creatorIds = new LinkedHashSet<>();
        Set<String> creatorValues = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            String creatorId = firstValue(row, "CREATED_BY_ID", "CREATOR_ID");
            if (creatorId != null && !creatorId.trim().isEmpty()) {
                try {
                    creatorIds.add(Long.valueOf(creatorId.trim()));
                } catch (NumberFormatException ignored) {
                    creatorValues.add(creatorId.trim());
                }
            }
            String creator = firstValue(row, "CREATED_BY", "CREATE_BY");
            if (creator != null && !creator.trim().isEmpty()) creatorValues.add(creator.trim());
            row.put("CREATED_BY_NAME", creator);
            row.put("CREATED_BY_PHONE", null);
        }
        if (creatorIds.isEmpty() && creatorValues.isEmpty()) return;

        try {
            Map<String, SysUser> users = new HashMap<>();
            for (Long creatorId : creatorIds) {
                indexUser(users, sysUserService.selectUserById(creatorId));
            }
            for (String creatorValue : creatorValues) {
                SysUser known = users.get(creatorValue);
                if (known != null && creatorValue.equals(known.getUserName())) continue;
                SysUser user = sysUserService.selectUserByUserName(creatorValue);
                if (user == null) {
                    SysUser query = new SysUser();
                    query.setNickName(creatorValue);
                    user = sysUserService.selectUserList(query).stream()
                            .filter(candidate -> creatorValue.equals(candidate.getNickName()))
                            .findFirst()
                            .orElse(null);
                }
                indexUser(users, user);
            }
            for (Map<String, Object> row : rows) {
                String creator = firstValue(row, "CREATED_BY", "CREATE_BY");
                String creatorId = firstValue(row, "CREATED_BY_ID", "CREATOR_ID");
                SysUser user = creatorId == null ? null : users.get(creatorId);
                if (user == null && creator != null) user = users.get(creator);
                if (user == null) continue;
                String name = user.getNickName();
                if (name == null || name.trim().isEmpty()) name = user.getUserName();
                if (name != null && !name.trim().isEmpty()) row.put("CREATED_BY_NAME", name);
                row.put("CREATED_BY_PHONE", user.getPhonenumber());
            }
        } catch (Exception ex) {
            log.warn("主数据创建人信息暂不可用", ex);
        }
    }

    private void indexUser(Map<String, SysUser> users, SysUser user) {
        if (user == null) return;
        String userId = user.getUserId() == null ? null : String.valueOf(user.getUserId());
        String userName = user.getUserName();
        String nickName = user.getNickName();
        if (userId != null) users.put(userId, user);
        if (userName != null) users.put(userName, user);
        if (nickName != null) users.put(nickName, user);
    }

    private void fillSummary(MasterDataRespVO response, Long modelId, TableAccess access) {
        BeanUtils.copyProperties(dataQueryRepository.selectStatistics(access), response);
        try {
            Long pending = changeRequestMapper.selectPendingCountByModelId(modelId);
            response.setPendingMaintenanceApprovalCount(pending);
        } catch (Exception ex) {
            log.warn("维护审批统计不可用，modelId={}", modelId, ex);
        }
    }

    /**
     * 查询分类模型树。
     *
     * @param instanceId 模型物化实例 ID
     * @return 查询结果集合
     */
    @Override
    public List<MasterTreeNodeRespVO> getTree(Long instanceId) {
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        if (!"CATEGORY".equals(model.getModelType())) throw new ServiceException("普通模型没有分类树");
        String table = tableName(model);
        Set<String> columns = availableColumns(model, table);
        validateFields(model, columns);
        List<MasterDataTreeNodeDTO> nodes = loadNodes(model, table, nameField(model), columns);
        Map<String, MasterTreeNodeRespVO> byId = new LinkedHashMap<>();
        for (MasterDataTreeNodeDTO node : nodes) {
            MasterTreeNodeRespVO item = new MasterTreeNodeRespVO();
            item.setId(node.getMasterDataId());
            item.setLabel(node.getLabel());
            if (byId.put(node.getMasterDataId(), item) != null) throw new ServiceException("分类数据存在重复的主数据ID");
        }
        List<MasterTreeNodeRespVO> roots = new ArrayList<>();
        for (MasterDataTreeNodeDTO node : nodes) {
            MasterTreeNodeRespVO item = byId.get(node.getMasterDataId());
            String parentId = node.getCategoryId();
            if (parentId == null || parentId.isEmpty()
                    || ("0".equals(parentId) && !byId.containsKey("0"))) {
                roots.add(item);
            } else if (node.getMasterDataId().equals(parentId)) {
                throw new ServiceException("分类数据存在自引用节点");
            } else {
                MasterTreeNodeRespVO parent = byId.get(parentId);
                if (parent == null) throw new ServiceException("分类数据存在无效的父节点");
                parent.getChildren().add(item);
            }
        }
        if (!nodes.isEmpty() && roots.isEmpty()) throw new ServiceException("分类数据存在循环引用");
        Set<String> visited = new HashSet<>();
        for (MasterTreeNodeRespVO root : roots) visit(root, visited);
        if (visited.size() != nodes.size()) throw new ServiceException("分类数据存在循环引用");
        return roots;
    }

    /** 返回所选分类节点及其全部后代的主数据 ID，供分类列表和关联审批使用同一筛选口径。 */
    @Override
    public Set<Long> getCategoryDescendantIds(Long instanceId, String treeId) {
        if (treeId == null || treeId.trim().isEmpty()) return Collections.emptySet();
        List<MasterTreeNodeRespVO> tree = getTree(instanceId);
        MasterTreeNodeRespVO selected = findTreeNode(tree, treeId.trim());
        if (selected == null) throw new ServiceException("分类节点不存在");
        Set<Long> ids = new LinkedHashSet<>();
        collectTreeNodeIds(selected, ids);
        return ids;
    }

    private MasterTreeNodeRespVO findTreeNode(List<MasterTreeNodeRespVO> nodes, String id) {
        for (MasterTreeNodeRespVO node : nodes) {
            if (id.equals(node.getId())) return node;
            MasterTreeNodeRespVO found = findTreeNode(node.getChildren(), id);
            if (found != null) return found;
        }
        return null;
    }

    private void collectTreeNodeIds(MasterTreeNodeRespVO node, Set<Long> ids) {
        ids.add(masterId(node.getId()));
        for (MasterTreeNodeRespVO child : node.getChildren()) collectTreeNodeIds(child, ids);
    }

    private void visit(MasterTreeNodeRespVO node, Set<String> visited) {
        if (!visited.add(node.getId())) throw new ServiceException("分类数据存在循环引用");
        for (MasterTreeNodeRespVO child : node.getChildren()) visit(child, visited);
    }

    private List<MasterDataTreeNodeDTO> loadNodes(MasterModelRespDTO model, String table, String nameCode, Set<String> columns) {
        requireColumn(columns, "CATEGORY_ID");
        try {
            return dataQueryRepository.selectTreeNodes(model.getDatasourceId(), table, nameCode, model.getColumnMappings());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("读取分类树失败，table={}", table, ex);
            throw new ServiceException("分类树查询失败，请检查物理表");
        }
    }

    private Set<String> descendantIds(List<MasterDataTreeNodeDTO> nodes, String selectedId) {
        Map<String, List<String>> children = new HashMap<>();
        boolean found = false;
        for (MasterDataTreeNodeDTO node : nodes) {
            if (selectedId.equals(node.getMasterDataId())) found = true;
            children.computeIfAbsent(node.getCategoryId(), key -> new ArrayList<>()).add(node.getMasterDataId());
        }
        if (!found) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        List<String> pending = new ArrayList<>();
        pending.add(selectedId);
        while (!pending.isEmpty()) {
            String id = pending.remove(pending.size() - 1);
            if (!result.add(id)) throw new ServiceException("分类数据存在循环引用");
            pending.addAll(children.getOrDefault(id, Collections.emptyList()));
        }
        return result;
    }

    private void validateFields(MasterModelRespDTO model, Set<String> physical) {
        requireColumn(physical, "ID");
        requireColumn(physical, "STATUS");
        if (model.getFields() == null || model.getFields().isEmpty()) throw new ServiceException("模型字段配置为空");
        for (MasterFieldRespDTO field : model.getFields()) {
            String code = identifier(field.getCode());
            if (!"SYSTEM".equalsIgnoreCase(field.getCategory()) || Boolean.TRUE.equals(field.getNameField())) {
                requireColumn(physical, code);
            }
        }
        nameField(model);
    }

    private String nameField(MasterModelRespDTO model) {
        List<MasterFieldRespDTO> names = model.getFields().stream()
                .filter(f -> Boolean.TRUE.equals(f.getNameField())).collect(Collectors.toList());
        if (names.size() != 1) throw new ServiceException("模型必须配置且仅配置一个名称字段");
        return identifier(names.get(0).getCode());
    }

    private String tableName(MasterModelRespDTO model) {
        String table = model.getPhysicalTableName();
        if (table == null) throw new ServiceException("模型物理表名缺失");
        String[] parts = table.split("\\.", -1);
        if (parts.length == 2) return identifier(parts[0]) + "." + identifier(parts[1]);
        if (parts.length != 1) throw new ServiceException("模型物理表名无效");
        String schema = model.getSchemaName();
        return schema == null || schema.trim().isEmpty()
                ? identifier(table) : identifier(schema.trim()) + "." + identifier(table);
    }

    private Set<String> availableColumns(MasterModelRespDTO model, String table) {
        try {
            Set<String> actual = dataQueryRepository.findPhysicalColumns(model.getDatasourceId(), table);
            for (Map.Entry<String, String> entry : model.getColumnMappings().entrySet()) {
                if (!actual.contains(entry.getValue().toUpperCase(Locale.ROOT))) {
                    throw new ServiceException("发布字段与物理表不一致：" + entry.getKey());
                }
            }
            return model.getColumnMappings().keySet();
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("物理表不可用，table={}", table, ex);
            throw new ServiceException("模型物理表不存在，请检查物化结果");
        }
    }

    private void requireColumn(Set<String> physical, String code) {
        if (!physical.contains(code.toUpperCase(Locale.ROOT))) {
            throw new ServiceException("模型字段配置与物理表不一致：" + code);
        }
    }

    private String identifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) throw new ServiceException("模型物理字段编码无效");
        return value;
    }

    private Long masterId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ServiceException("主数据ID格式无效");
        }
    }

    private MasterColumnRespVO column(String code, String label, String dataType, String dict) {
        MasterColumnRespVO col = new MasterColumnRespVO();
        col.setProp(code);
        col.setLabel(label);
        col.setDataType(dataType);
        col.setDict(dict);
        return col;
    }

    private String value(Map<String, Object> row, String key) {
        Object raw = row.get(key.toUpperCase(Locale.ROOT));
        return raw == null ? null : String.valueOf(raw);
    }

    private String firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            String value = value(row, key);
            if (value != null && !value.trim().isEmpty()) return value;
        }
        return null;
    }

    private Long numberValue(Map<String, Object> row, String key) {
        Object raw = row.get(key.toUpperCase(Locale.ROOT));
        if (raw == null) return null;
        if (raw instanceof Number) return ((Number) raw).longValue();
        return masterId(String.valueOf(raw));
    }

    private static class ApprovalInfo {
        private Long requestId;
        private String requestNo;
        private String status;
        private String executeStatus;
        private String changeType;
    }

}
