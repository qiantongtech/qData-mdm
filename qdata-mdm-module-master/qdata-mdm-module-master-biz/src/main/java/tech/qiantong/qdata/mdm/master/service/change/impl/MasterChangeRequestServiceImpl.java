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

package tech.qiantong.qdata.mdm.master.service.change.impl;

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeSubmodelSaveReqVO;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelBaseRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelBaseApi;
import tech.qiantong.qdata.mdm.common.utils.SecurityUtils;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestDetailRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestItemRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestBatchSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeBatchValidateRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeImportParseRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeValidateRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterRecordRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSubmitRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestModelRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterTreeNodeRespVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeImportDO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeImportMapper;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestApprovalDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterMyRequestRowDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestItemQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestStatusCountDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterMyRequestQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterMyRequestStatusCountDTO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestMapper;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeRequestItemService;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeRequestService;
import tech.qiantong.qdata.mdm.master.service.change.IMasterExecuteTaskService;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceData;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MasterAutomaticEncodingGenerator;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MasterMaintenanceValidator;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataVersionService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterSubmodelService;
import tech.qiantong.qdata.mdm.master.util.MasterRecordUtils;
import tech.qiantong.qdata.mdm.master.util.MasterLifecycleRules;
import tech.qiantong.qdata.mdm.master.service.data.excel.MasterEditableWorkbook;
import tech.qiantong.qdata.mdm.master.service.data.reference.MasterReferenceService.ReferenceValues;
import tech.qiantong.qdata.mdm.master.service.data.reference.MasterReferenceService;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.util.MasterTargetSummaryFormatter;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

/**
 * MASTER_CHANGE_REQUEST 申请查询、校验、提交与审批业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterChangeRequestServiceImpl implements IMasterChangeRequestService {

    private final IMasterDataMetadataService metadataService;
    private final MasterReferenceService references;
    private final ModelPublishedApi modelPublishedApi;
    private final ModelBaseApi modelBaseApi;
    private final IMasterDataService dataService;
    private final IMasterDataVersionService dataVersionService;
    private final IMasterSubmodelService submodelService;
    private final MasterDataQueryRepository dataQueryRepository;
    private final MasterChangeRequestMapper changeRequestMapper;
    private final IMasterChangeRequestItemService changeRequestItemService;
    private final IMasterExecuteTaskService executeTaskService;
    private final MasterMaintenanceValidator maintenanceValidator;
    private final MasterAutomaticEncodingGenerator automaticEncodingGenerator;
    private final TransactionTemplate transactionTemplate;
    private final ISysUserService sysUserService;
    @javax.annotation.Resource
    private MasterChangeImportMapper importMapper;

    // 当前登录人的申请查询。
    /**
     * 分页查询当前登录人的已提交申请。
     *
     * @param reqVO 分页及筛选参数
     * @return 当前登录人的申请分页列表及统计信息
     */
    @Override
    public MasterMyRequestPageRespVO getMyRequestPage(MasterMyRequestPageReqVO reqVO) {
        Long applicantId = myCurrentUserId();
        int page = reqVO == null || reqVO.getPageNum() == null ? 1 : Math.max(1, reqVO.getPageNum());
        int size = reqVO == null || reqVO.getPageSize() == null ? 10
                : Math.max(1, Math.min(100, reqVO.getPageSize()));
        String search = reqVO == null || reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (search.length() > 100) throw new ServiceException("关键词不能超过100字");
        String type = reqVO == null || reqVO.getChangeType() == null ? ""
                : reqVO.getChangeType().trim().toUpperCase(Locale.ROOT);
        if (!type.isEmpty() && !Arrays.asList("CREATE", "UPDATE", "BATCH_UPDATE", "BATCH_IMPORT",
                "STATUS_CHANGE", "VERSION_RESTORE").contains(type)) {
            throw new ServiceException("申请类型无效");
        }
        String group = reqVO == null || reqVO.getStatusGroup() == null ? "ALL"
                : reqVO.getStatusGroup().trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("ALL", "PROCESSING", "ENDED").contains(group)) throw new ServiceException("状态分组无效");
        Long modelId = reqVO == null ? null : reqVO.getModelId();
        if (modelId != null && modelId <= 0) throw new ServiceException("模型ID无效");
        Long selectedApplicantId = reqVO == null ? null : reqVO.getApplicantId();
        if (selectedApplicantId != null && selectedApplicantId <= 0) throw new ServiceException("申请人ID无效");
        String status = reqVO == null || reqVO.getApprovalStatus() == null ? ""
                : reqVO.getApprovalStatus().trim();
        if (!status.isEmpty() && !Arrays.asList("0", "1", "2", "3", "4").contains(status)) {
            throw new ServiceException("审批状态无效");
        }
        String keywordLike = null;
        if (!search.isEmpty()) {
            keywordLike = "%" + search.toLowerCase(Locale.ROOT).replace("!", "!!")
                    .replace("%", "!%").replace("_", "!_") + "%";
        }

        MasterMyRequestQueryDTO query = new MasterMyRequestQueryDTO();
        query.setApplicantId(applicantId);
        query.setSelectedApplicantId(selectedApplicantId);
        query.setKeywordLike(keywordLike);
        Map<Long, ModelPublishedRespDTO> models = null;
        if (!search.isEmpty()) {
            models = myPublishedModels(changeRequestMapper.selectMyRequestModelIds(applicantId));
            final String keyword = search.toLowerCase(Locale.ROOT);
            List<Long> matchedIds = models.values().stream().filter(model ->
                    (model.getName() != null && model.getName().toLowerCase(Locale.ROOT).contains(keyword))
                    || (model.getCode() != null && model.getCode().toLowerCase(Locale.ROOT).contains(keyword)))
                    .map(ModelPublishedRespDTO::getId).collect(java.util.stream.Collectors.toList());
            List<List<Long>> groups = new ArrayList<>();
            for (int offset = 0; offset < matchedIds.size(); offset += 500) {
                groups.add(matchedIds.subList(offset, Math.min(offset + 500, matchedIds.size())));
            }
            query.setKeywordModelIdGroups(groups);
        }
        query.setChangeType(type);
        query.setModelId(modelId);
        query.setApprovalStatus(status);
        query.setStatusGroup(group);
        query.setRequestNoLike(mySearchLike(reqVO == null ? null : reqVO.getRequestNo(), "申请编号"));
        query.setTargetNameLike(mySearchLike(reqVO == null ? null : reqVO.getTargetName(), "目标主数据名称"));
        query.setApplicantNameLike(mySearchLike(reqVO == null ? null : reqVO.getApplicantName(), "申请人名称"));
        query.setProjectId(reqVO == null ? null : reqVO.getProjectId());
        query.setLayerIds(reqVO == null ? null : reqVO.getLayerIds());

        List<MasterMyRequestStatusCountDTO> counts =
                changeRequestMapper.selectMyRequestStatusCounts(query);
        MasterMyRequestPageRespVO result = new MasterMyRequestPageRespVO();
        long all = 0, processing = 0;
        for (MasterMyRequestStatusCountDTO item : counts) {
            long count = item.getRowCount() == null ? 0L : item.getRowCount();
            all += count;
            if ("0".equals(item.getApprovalStatus())) processing += count;
        }
        result.setAllCount(all);
        result.setProcessingCount(processing);
        result.setEndedCount(all - processing);
        IPage<MasterMyRequestRowDTO> dataPage = changeRequestMapper.selectMyRequestPage(
                new Page<>(page, size), query, reqVO);
        Map<Long, ModelBaseRespDTO> displayModels = new HashMap<>();
        for (ModelBaseRespDTO model : modelBaseApi.getModelBaseList(dataPage.getRecords().stream()
                .map(MasterMyRequestRowDTO::getModelId).collect(java.util.stream.Collectors.toList()))) {
            displayModels.put(model.getId(), model);
        }
        List<MasterMyRequestRespVO> rows = new ArrayList<>();
        SysUser applicant = sysUserService.selectUserById(applicantId);
        for (MasterMyRequestRowDTO row : dataPage.getRecords()) {
            MasterMyRequestRespVO item = new MasterMyRequestRespVO();
            item.setRequestId(toId(row.getRequestId()));
            item.setRequestNo(row.getRequestNo());
            item.setRequestName(row.getRequestName());
            item.setApplyReason(row.getApplyReason());
            item.setApplicantName(applicant == null ? null : applicant.getNickName());
            item.setApplicantPhone(applicant == null ? null : applicant.getPhonenumber());
            item.setCanWithdraw("0".equals(row.getApprovalStatus()) && "0".equals(row.getExecuteStatus()));
            item.setCanReapply(Arrays.asList("2", "3", "4").contains(row.getApprovalStatus())
                    && "0".equals(row.getExecuteStatus()));
            item.setModelId(toId(row.getModelId()));
            ModelBaseRespDTO model = displayModels.get(row.getModelId());
            item.setModel(model);
            item.setModelName(model == null ? null : model.getName());
            item.setModelCode(model == null ? null : model.getCode());
            item.setChangeType(row.getChangeType());
            item.setApprovalStatus(row.getApprovalStatus());
            item.setExecuteStatus(row.getExecuteStatus());
            item.setSubmitTime(row.getSubmitTime());
            item.setFinishTime(row.getFinishTime());
            item.setExecuteMessage(myNormalizeExecuteMessage(row.getApprovalStatus(), row.getExecuteMessage()));
            item.setFlowInstanceId(toId(row.getFlowInstanceId()));
            item.setItemCount(row.getItemCount());
            rows.add(item);
        }
        result.setTotal(dataPage.getTotal());
        result.setRows(rows);
        return result;
    }

    private String mySearchLike(String value, String label) {
        String search = submissionTrim(value);
        if (search.length() > 100) throw new ServiceException(label + "不能超过100字");
        return search.isEmpty() ? null : "%" + search.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    /**
     * 查询当前登录人申请涉及的模型选项。
     *
     * @return 查询结果集合
     */
    @Override
    public List<MasterMyRequestModelRespVO> getMyRequestModelOptions() {
        List<MasterMyRequestModelRespVO> result = new ArrayList<>();
        Map<Long, ModelPublishedRespDTO> models = myPublishedModels(
                changeRequestMapper.selectMyRequestModelIds(myCurrentUserId()));
        for (ModelPublishedRespDTO model : models.values()) {
            MasterMyRequestModelRespVO item = new MasterMyRequestModelRespVO();
            item.setId(toId(model.getId()));
            item.setName(model.getName());
            result.add(item);
        }
        result.sort(Comparator.comparing(MasterMyRequestModelRespVO::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    private Map<Long, ModelPublishedRespDTO> myPublishedModels(Collection<Long> modelIds) {
        Map<Long, ModelPublishedRespDTO> models = new LinkedHashMap<>();
        for (Long modelId : new LinkedHashSet<>(modelIds)) {
            if (modelId == null) continue;
            try {
                models.put(modelId, modelPublishedApi.getPublishedModel(modelId));
            } catch (ServiceException ex) {
                // 申请历史独立保留，停用、删除或缺少成功记录的模型不阻断申请查询。
                log.warn("申请关联模型信息不可用，modelId={}", modelId, ex);
            }
        }
        return models;
    }

    /**
     * 查询本人申请详情，严格校验申请人归属。
     *
     * @param requestId 变更申请 ID
     * @return 申请详情及有效申请明细
     */
    @Override
    public MasterChangeRequestDetailRespVO getMyRequestDetail(Long requestId) {
        if (requestId == null || requestId <= 0) throw new ServiceException("申请ID无效");
        Long currentUserId = myCurrentUserId();
        MasterChangeRequestApprovalDTO detailRow =
                changeRequestMapper.selectMyRequestDetail(requestId, currentUserId);
        if (detailRow == null) throw new ServiceException("申请不存在或无权查看");
        SysUser applicant = detailRow.getApplicantId() == null ? null
                : sysUserService.selectUserById(detailRow.getApplicantId());
        MasterChangeRequestDetailRespVO detail = approvalToDetail(detailRow,
                applicant == null ? null : applicant.getNickName());
        detail.setModelId(toId(detailRow.getModelId()));
        detail.setInstanceId(toId(detailRow.getInstanceId()));
        detail.setChangeSource(detailRow.getChangeSource());
        detail.setFields(Collections.emptyList());
        detail.setFieldValueLabels(Collections.emptyMap());
        detail.setApplicantPhone(applicant == null ? null : applicant.getPhonenumber());
        ModelPublishedRespDTO published = myPublishedModels(Collections.singletonList(detailRow.getModelId()))
                .get(detailRow.getModelId());
        if (published != null) detail.setModelName(published.getName());
        try {
            MasterModelRespDTO model = metadataService.getModel(detailRow.getInstanceId());
            if (java.util.Objects.equals(model.getModelId(), detailRow.getModelId())
                    && java.util.Objects.equals(model.getModelVersionId(), detailRow.getModelVersionId())) {
                if (approvalHasText(model.getModelName())) detail.setModelName(model.getModelName());
                detail.setFields(model.getFields());
                detail.setFieldValueLabels(references.displayLabels(model));
            }
        } catch (ServiceException ex) {
            // 模型停用或实例不可用时仍可查看申请历史，字段名称回退为快照编码。
            log.warn("申请历史字段配置不可用，requestId={}", requestId, ex);
        }
        detail.setExecuteMessage(myNormalizeExecuteMessage(
                detail.getApprovalStatus(), detail.getExecuteMessage()));
        List<MasterChangeRequestItemRespVO> items =
                changeRequestItemService.getApplicationItems(requestId);
        for (MasterChangeRequestItemRespVO item : items) {
            item.setExecuteMessage(myNormalizeExecuteMessage(
                    detail.getApprovalStatus(), item.getExecuteMessage()));
        }
        detail.setItems(items);
        if ("BATCH_IMPORT".equals(detail.getChangeType())) {
            MasterChangeImportDO imported = importMapper.selectByRequestId(requestId);
            if (imported != null) {
                MasterChangeRequestDetailRespVO.ImportInfo info = new MasterChangeRequestDetailRespVO.ImportInfo();
                info.setFileName(imported.getFileName());
                info.setImportMode(imported.getImportMode());
                info.setTemplateVersionNo(imported.getTemplateVersionNo());
                info.setTotalCount(imported.getTotalCount());
                // 本期只有整批校验通过才落库申请，不能使用执行成功数或新增/更新数代替校验通过数。
                info.setPassedCount(imported.getTotalCount());
                detail.setImportInfo(info);
            }
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawMyRequest(Long requestId) {
        if (requestId == null || requestId <= 0) throw new ServiceException("申请ID无效");
        Long applicantId = myCurrentUserId();
        Date now = new Date();
        LambdaUpdateWrapper<MasterChangeRequestDO> update = new LambdaUpdateWrapper<>();
        update.eq(MasterChangeRequestDO::getId, requestId)
                .eq(MasterChangeRequestDO::getApplicantId, applicantId)
                .eq(MasterChangeRequestDO::getStatus, "0")
                .eq(MasterChangeRequestDO::getExecuteStatus, "0")
                .eq(MasterChangeRequestDO::getValidFlag, true)
                .eq(MasterChangeRequestDO::getDelFlag, false)
                .set(MasterChangeRequestDO::getStatus, "3")
                .set(MasterChangeRequestDO::getFinishTime, now)
                .set(MasterChangeRequestDO::getTerminateReason, "申请人主动撤回")
                .set(MasterChangeRequestDO::getExecuteMessage, null)
                .set(MasterChangeRequestDO::getUpdateBy, String.valueOf(applicantId))
                .set(MasterChangeRequestDO::getUpdatorId, applicantId)
                .set(MasterChangeRequestDO::getUpdateTime, now);
        if (changeRequestMapper.update(null, update) != 1) {
            throw new ServiceException("申请不存在、无权撤回或状态已发生变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public MasterChangeRequestSubmitRespVO reapplyMyRequest(Long requestId) {
        if (requestId == null || requestId <= 0) throw new ServiceException("申请ID无效");
        Long applicantId = myCurrentUserId();
        MasterChangeRequestDO source = changeRequestMapper.selectById(requestId);
        if (source == null || !Boolean.TRUE.equals(source.getValidFlag()) || Boolean.TRUE.equals(source.getDelFlag())
                || !Arrays.asList("MANUAL", "MERGE").contains(source.getChangeSource())
                || !applicantId.equals(source.getApplicantId())) {
            throw new ServiceException("申请不存在或无权重新申请");
        }
        if (!Arrays.asList("2", "3", "4").contains(source.getStatus())
                || !"0".equals(source.getExecuteStatus())) {
            throw new ServiceException("只有未执行的已驳回、已撤回或已终止申请可以重新申请");
        }
        maintenanceValidator.validateApplicant(applicantId, source.getChangeType());
        MasterModelRespDTO model = metadataService.getModel(source.getInstanceId());
        if (!java.util.Objects.equals(model.getModelId(), source.getModelId())
                || !java.util.Objects.equals(model.getModelVersionId(), source.getModelVersionId())) {
            throw new ServiceException("主数据模型已变更，请从主数据工作区重新发起申请");
        }
        List<MasterChangeRequestItemDO> sourceItems = changeRequestItemService.getItemsForReapply(requestId);
        if (sourceItems.isEmpty()) throw new ServiceException("原申请没有可重新提交的明细");
        List<Long> recordIds = sourceItems.stream().map(MasterChangeRequestItemDO::getMasterRecordId)
                .filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        lockAndCheckPendingMaintenance(source.getModelId(), recordIds);
        Map<Long, String> currentVersions = submissionCurrentVersions(source.getModelId(), recordIds);
        for (MasterChangeRequestItemDO sourceItem : sourceItems) {
            if (sourceItem.getMasterRecordId() == null) continue;
            String currentVersion = currentVersions.get(sourceItem.getMasterRecordId());
            if (!java.util.Objects.equals(currentVersion, sourceItem.getCurrentVersionNo())) {
                throw new ServiceException("主数据已发生变化，请从主数据工作区重新发起申请");
            }
            Long pending = changeRequestItemService.getPendingCountForRecord(
                    source.getModelId(), sourceItem.getMasterRecordId());
            if (pending != null && pending > 0) {
                throw new ServiceException(pendingMaintenanceMessage(source.getModelId(), sourceItem.getMasterRecordId()));
            }
        }
        SysUser user = sysUserService.selectUserById(applicantId);
        String applicantName = user == null ? source.getApplicantName()
                : (user.getNickName() == null || user.getNickName().trim().isEmpty()
                    ? user.getUserName() : user.getNickName());
        String requestNo = submissionRequestNo();
        MasterChangeRequestDO request = new MasterChangeRequestDO();
        request.setRequestNo(requestNo);
        request.setModelId(source.getModelId());
        request.setModelVersionId(source.getModelVersionId());
        request.setInstanceId(source.getInstanceId());
        request.setExecutionId(UUID.randomUUID().toString().replace("-", ""));
        request.setSourceRequestId(source.getId());
        request.setChangeType(source.getChangeType());
        request.setChangeSource(source.getChangeSource());
        request.setRequestName(source.getRequestName());
        request.setApplicantId(applicantId);
        request.setApplicantName(submissionLimit(applicantName, 64));
        request.setApplyReason(source.getApplyReason());
        request.setStatus("1");
        request.setExecuteStatus("0");
        request.setItemCount(sourceItems.size());
        request.setSuccessItemCount(0);
        request.setFailItemCount(0);
        request.setSubmitTime(new Date());
        request.setValidFlag(true);
        request.setDelFlag(false);
        request.setCreateBy(submissionLimit(applicantName, 32));
        request.setCreatorId(applicantId);
        request.setUpdateBy(submissionLimit(applicantName, 32));
        request.setUpdatorId(applicantId);
        request.setRemark("由申请 " + source.getRequestNo() + " 重新发起，审核通过后等待执行");
        if (changeRequestMapper.insert(request) != 1 || request.getId() == null) {
            throw new ServiceException("重新建立申请失败");
        }
        List<MasterChangeRequestItemDO> newItems = new ArrayList<>(sourceItems.size());
        for (MasterChangeRequestItemDO sourceItem : sourceItems) {
            MasterChangeRequestItemDO item = new MasterChangeRequestItemDO();
            item.setRequestId(request.getId());
            item.setItemNo(sourceItem.getItemNo());
            item.setMasterRecordId(sourceItem.getMasterRecordId());
            item.setChangeType(sourceItem.getChangeType());
            item.setBeforeData(sourceItem.getBeforeData());
            item.setProposedData(sourceItem.getProposedData());
            item.setBeforeStatus(sourceItem.getBeforeStatus());
            item.setTargetStatus(sourceItem.getTargetStatus());
            item.setCurrentVersionId(sourceItem.getCurrentVersionId());
            item.setCurrentVersionNo(sourceItem.getCurrentVersionNo());
            item.setRemark("由申请 " + source.getRequestNo() + " 重新发起");
            changeRequestItemService.initializeItem(item, applicantId, applicantName);
            newItems.add(item);
        }
        if (!changeRequestItemService.insertItems(newItems)) throw new ServiceException("重新建立申请明细失败");
        executeTaskService.createPendingTask(request.getId());
        return submissionResponse(request.getId(), requestNo, request.getRequestName(), sourceItems.size());
    }

    private Long myCurrentUserId() {
        Long id = SecurityUtils.getUserId();
        if (id == null || id <= 0) throw new ServiceException("请先登录");
        return id;
    }

    private String myNormalizeExecuteMessage(String approvalStatus, String value) {
        return "0".equals(approvalStatus) ? null : value;
    }

    // 模型和记录维度的申请审批查询
    private static final Set<String> APPROVAL_STATUSES = new HashSet<>(Arrays.asList("0", "1", "2", "3", "4"));
    private static final Set<String> APPROVAL_CHANGE_TYPES = new HashSet<>(Arrays.asList(
            "CREATE", "UPDATE", "BATCH_UPDATE", "BATCH_IMPORT", "STATUS_CHANGE", "VERSION_RESTORE"));

    /** 仅返回当前记录实际关联的直接维护申请，批量申请用 EXISTS 去重。 */
    @Override
    public MasterRecordRequestPageRespVO getRecordApprovalPage(Long instanceId, Long masterDataId,
                                                                  MasterChangePageReqVO reqVO) {
        Long modelId = approvalCheckedRecordModelId(instanceId, masterDataId);
        int page = reqVO == null || reqVO.getPageNum() == null ? 1 : Math.max(1, reqVO.getPageNum());
        int size = reqVO == null || reqVO.getPageSize() == null ? 10
                : Math.max(1, Math.min(100, reqVO.getPageSize()));
        try {
            MasterRecordRequestPageRespVO response = new MasterRecordRequestPageRespVO();
            IPage<MasterChangeRequestApprovalDTO> dataPage = changeRequestMapper.selectRecordApprovalPage(
                    new Page<>(page, size), modelId, masterDataId, reqVO);
            response.setTotal(dataPage.getTotal());
            if (response.getTotal() == 0L) {
                response.setRows(Collections.emptyList());
                return response;
            }
            List<MasterChangeRequestRespVO> result = new ArrayList<>();
            Map<Long, SysUser> applicants = new HashMap<>();
            for (MasterChangeRequestApprovalDTO row : dataPage.getRecords()) {
                Long applicantId = row.getApplicantId();
                if (applicantId != null && !applicants.containsKey(applicantId)) {
                    applicants.put(applicantId, sysUserService.selectUserById(applicantId));
                }
                SysUser applicant = applicants.get(applicantId);
                result.add(approvalToRow(row, applicant == null ? null : applicant.getNickName()));
            }
            response.setRows(result);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询单条主数据审批记录失败，instanceId={}, masterDataId={}", instanceId, masterDataId, ex);
            throw new ServiceException("主数据审批记录查询失败");
        }
    }

    /** 返回申请头信息，但申请明细严格限定为当前主数据，避免批量申请泄露其他记录。 */
    @Override
    public MasterChangeRequestDetailRespVO getRecordApprovalDetail(Long instanceId, Long masterDataId, Long requestId) {
        if (requestId == null || requestId <= 0) throw new ServiceException("审批申请ID格式无效");
        Long modelId = approvalCheckedRecordModelId(instanceId, masterDataId);
        try {
            MasterChangeRequestApprovalDTO request = changeRequestMapper.selectRecordApprovalDetail(
                    modelId, masterDataId, requestId);
            if (request == null) throw new ServiceException("当前主数据无此维护审批申请");
            SysUser applicant = request.getApplicantId() == null ? null
                    : sysUserService.selectUserById(request.getApplicantId());
            MasterChangeRequestDetailRespVO response = approvalToDetail(request,
                    applicant == null ? null : applicant.getNickName());
            List<MasterChangeRequestItemQueryDTO> rows = changeRequestItemService.getApplicationItemsForRecord(
                    requestId, masterDataId);
            List<MasterChangeRequestItemRespVO> items = new ArrayList<>();
            for (MasterChangeRequestItemQueryDTO row : rows) items.add(approvalToItem(row));
            response.setItems(items);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询单条主数据审批详情失败，instanceId={}, masterDataId={}, requestId={}",
                    instanceId, masterDataId, requestId, ex);
            throw new ServiceException("主数据审批详情查询失败");
        }
    }

    private Long approvalCheckedRecordModelId(Long instanceId, Long masterDataId) {
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        MasterRecordUtils.requireRecord(dataQueryRepository, model, masterDataId);
        return model.getModelId();
    }

    /**
     * 分页查询当前模型的维护审批申请。
     *
     * @param instanceId 模型物化实例 ID
     * @param req 查询参数
     * @return 当前模型的审批申请分页列表及统计信息
     */
    @Override
    public MasterChangeRequestPageRespVO getApprovalPage(Long instanceId, MasterChangeRequestPageReqVO req) {
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        MasterChangeRequestQueryDTO query = new MasterChangeRequestQueryDTO();
        query.setModelId(model.getModelId());
        if (approvalHasText(req.getChangeType())) {
            String changeType = req.getChangeType().trim().toUpperCase(Locale.ROOT);
            if (!APPROVAL_CHANGE_TYPES.contains(changeType)) throw new ServiceException("无效的维护操作类型");
            query.setChangeType(changeType);
        }
        if (approvalHasText(req.getApprovalStatus())) {
            String status = req.getApprovalStatus().trim();
            if (!APPROVAL_STATUSES.contains(status)) throw new ServiceException("无效的审批状态");
            query.setApprovalStatus(status);
        }
        if (approvalHasText(req.getRequestName())) {
            query.setRequestNameLike("%" + approvalEscapeLike(
                    req.getRequestName().trim().toLowerCase(Locale.ROOT)) + "%");
        }
        if (approvalHasText(req.getMasterDataId())) {
            try {
                query.setMasterDataId(Long.valueOf(req.getMasterDataId().trim()));
            } catch (NumberFormatException ex) {
                throw new ServiceException("主数据ID格式无效");
            }
        }
        if (approvalHasText(req.getTreeId())) {
            query.setCategoryRecordIdBatches(approvalBatches(
                    dataService.getCategoryDescendantIds(instanceId, req.getTreeId())));
        }

        try {
            MasterChangeRequestPageRespVO response = approvalSummary(model.getModelId());
            int pageNum = req.getPageNum() == null ? 1 : Math.max(1, req.getPageNum());
            int pageSize = req.getPageSize() == null ? 10 : Math.max(1, Math.min(100, req.getPageSize()));
            IPage<MasterChangeRequestApprovalDTO> dataPage = changeRequestMapper.selectApprovalPage(
                    new Page<>(pageNum, pageSize), query, req);
            response.setTotal(dataPage.getTotal());
            if (response.getTotal() == 0L) {
                response.setRows(Collections.emptyList());
                return response;
            }
            List<MasterChangeRequestApprovalDTO> rows = dataPage.getRecords();
            Map<Long, SysUser> applicants = new HashMap<>();
            Map<Long, String> targets = pageTargetSummaries(rows);
            List<MasterChangeRequestRespVO> result = new ArrayList<>();
            for (MasterChangeRequestApprovalDTO row : rows) {
                Long applicantId = row.getApplicantId();
                if (applicantId != null && !applicants.containsKey(applicantId)) {
                    applicants.put(applicantId, sysUserService.selectUserById(applicantId));
                }
                SysUser applicant = applicants.get(applicantId);
                MasterChangeRequestRespVO item = approvalToRow(row, applicant == null ? null : applicant.getNickName());
                item.setTargetSummary(targets.getOrDefault(row.getId(), "—"));
                result.add(item);
            }
            response.setRows(result);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询权威主数据维护审批失败，instanceId={}", instanceId, ex);
            throw new ServiceException("维护审批查询失败，请检查申请数据");
        }
    }

    /** 分类分支记录 ID 每 500 个一批，避免单个 IN 参数过多。 */
    private List<List<Long>> approvalBatches(Set<Long> recordIds) {
        List<Long> ids = new ArrayList<>(recordIds);
        List<List<Long>> result = new ArrayList<>();
        if (ids.isEmpty()) {
            result.add(Collections.singletonList(Long.MIN_VALUE));
            return result;
        }
        for (int start = 0; start < ids.size(); start += 500) {
            int end = Math.min(start + 500, ids.size());
            result.add(new ArrayList<>(ids.subList(start, end)));
        }
        return result;
    }

    /** 只查询当前页申请的有效明细，避免逐行读取详情造成 N+1 查询。 */
    Map<Long, String> pageTargetSummaries(List<MasterChangeRequestApprovalDTO> requests) {
        if (requests.isEmpty()) return Collections.emptyMap();
        List<Long> ids = new ArrayList<>();
        for (MasterChangeRequestApprovalDTO request : requests) ids.add(request.getId());
        List<MasterChangeRequestItemQueryDTO> itemRows = changeRequestItemService.getItemsForRequests(ids);
        Map<Long, List<MasterChangeRequestItemQueryDTO>> grouped = new HashMap<>();
        for (MasterChangeRequestItemQueryDTO item : itemRows) {
            grouped.computeIfAbsent(item.getRequestId(), key -> new ArrayList<>()).add(item);
        }
        Map<Long, String> summaries = new HashMap<>();
        for (MasterChangeRequestApprovalDTO request : requests) {
            long approvalId = request.getId();
            summaries.put(approvalId, MasterTargetSummaryFormatter.format(
                    request.getChangeType(), grouped.get(approvalId)));
        }
        return summaries;
    }

    /**
     * 查询当前模型中的指定申请详情。
     *
     * @param instanceId 模型物化实例 ID
     * @param requestId 变更申请 ID
     * @return 申请详情及有效申请明细
     */
    @Override
    public MasterChangeRequestDetailRespVO getApprovalDetail(Long instanceId, Long requestId) {
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        try {
            MasterChangeRequestApprovalDTO request = changeRequestMapper.selectApprovalDetail(
                    model.getModelId(), requestId);
            if (request == null) throw new ServiceException("维护审批申请不存在");
            SysUser applicant = request.getApplicantId() == null ? null
                    : sysUserService.selectUserById(request.getApplicantId());
            MasterChangeRequestDetailRespVO response = approvalToDetail(request,
                    applicant == null ? null : applicant.getNickName());
            response.setItems(changeRequestItemService.getApplicationItems(requestId));
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询维护审批详情失败，instanceId={}, requestId={}", instanceId, requestId, ex);
            throw new ServiceException("维护审批详情查询失败");
        }
    }

    private MasterChangeRequestPageRespVO approvalSummary(Long modelId) {
        MasterChangeRequestPageRespVO response = new MasterChangeRequestPageRespVO();
        response.setPendingCount(0L);
        response.setPassedCount(0L);
        response.setRejectedCount(0L);
        response.setAllCount(0L);
        List<MasterChangeRequestStatusCountDTO> rows = changeRequestMapper.selectApprovalStatusCounts(modelId);
        for (MasterChangeRequestStatusCountDTO row : rows) {
            long count = row.getRowCount() == null ? 0L : row.getRowCount();
            response.setAllCount(response.getAllCount() + count);
            String status = row.getStatus();
            if ("0".equals(status)) response.setPendingCount(count);
            else if ("1".equals(status)) response.setPassedCount(count);
            else if ("2".equals(status)) response.setRejectedCount(count);
        }
        return response;
    }

    private MasterChangeRequestRespVO approvalToRow(MasterChangeRequestApprovalDTO row, String applicantName) {
        MasterChangeRequestRespVO item = new MasterChangeRequestRespVO();
        item.setRequestId(toId(row.getId()));
        item.setRequestNo(row.getRequestNo());
        item.setRequestName(row.getRequestName());
        item.setChangeType(row.getChangeType());
        item.setApplicantName(approvalHasText(applicantName) ? applicantName : null);
        item.setSubmitTime(row.getSubmitTime());
        item.setFinishTime(row.getFinishTime());
        item.setApprovalStatus(row.getStatus());
        item.setExecuteStatus(row.getExecuteStatus());
        item.setFlowInstanceId(toId(row.getFlowInstanceId()));
        item.setItemCount(row.getItemCount());
        return item;
    }

    private MasterChangeRequestDetailRespVO approvalToDetail(MasterChangeRequestApprovalDTO row, String applicantName) {
        MasterChangeRequestDetailRespVO item = new MasterChangeRequestDetailRespVO();
        item.setRequestId(toId(row.getId()));
        item.setRequestNo(row.getRequestNo());
        item.setRequestName(row.getRequestName());
        item.setChangeType(row.getChangeType());
        item.setApprovalStatus(row.getStatus());
        item.setExecuteStatus(row.getExecuteStatus());
        item.setApplicantName(approvalHasText(applicantName) ? applicantName : null);
        item.setApplyReason(row.getApplyReason());
        item.setFlowInstanceId(toId(row.getFlowInstanceId()));
        item.setItemCount(row.getItemCount());
        item.setSuccessItemCount(row.getSuccessItemCount());
        item.setFailItemCount(row.getFailItemCount());
        item.setSubmitTime(row.getSubmitTime());
        item.setFinishTime(row.getFinishTime());
        item.setTerminateReason(row.getTerminateReason());
        item.setExecuteMessage(row.getExecuteMessage());
        return item;
    }

    private MasterChangeRequestItemRespVO approvalToItem(MasterChangeRequestItemQueryDTO row) {
        MasterChangeRequestItemRespVO item = new MasterChangeRequestItemRespVO();
        item.setItemId(toId(row.getId()));
        item.setItemNo(row.getItemNo());
        item.setMasterRecordId(toId(row.getMasterRecordId()));
        item.setResultMasterRecordId(toId(row.getResultMasterRecordId()));
        item.setChangeType(row.getChangeType());
        item.setBeforeStatus(row.getBeforeStatus());
        item.setTargetStatus(row.getTargetStatus());
        item.setCurrentVersionNo(row.getCurrentVersionNo());
        item.setTargetVersionNo(row.getTargetVersionNo());
        item.setBeforeData(row.getBeforeData());
        item.setProposedData(row.getProposedData());
        item.setExecuteStatus(row.getExecuteStatus());
        item.setExecuteMessage(row.getExecuteMessage());
        return item;
    }

    private String toId(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean approvalHasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String approvalEscapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    // 单条维护申请预校验
    /**
     * 只读校验单条维护申请。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @return 校验结果及问题明细
     */
    @Override
    public MasterChangeValidateRespVO validateMaintenance(Long instanceId,
            MasterChangeRequestSaveReqVO request) {
        return validateMaintenance(instanceId, request, true);
    }

    private MasterChangeValidateRespVO validateMaintenance(Long instanceId,
            MasterChangeRequestSaveReqVO request, boolean allowUnassignedAutomatic) {
        if (request == null) throw new ServiceException("申请内容不能为空");
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        MasterChangeValidateRespVO response = new MasterChangeValidateRespVO();
        Map<String, String> errors = response.getErrors();
        String mode = request.getMode();
        boolean create = "CREATE".equals(mode);
        boolean update = "UPDATE".equals(mode);
        boolean lifecycle = MasterLifecycleRules.isAction(mode);
        if (!update && !create && !lifecycle) {
            errors.put("mode", "不支持的申请类型");
            response.setValid(false);
            return response;
        }
        if (lifecycle) {
            maintenanceValidateLifecycle(model, instanceId, request, errors);
            response.setValid(errors.isEmpty());
            return response;
        }
        Long recordId = null;
        Map<String, Object> original = null;
        if (update) {
            recordId = maintenanceParseId(request.getRecordId());
            if (recordId == null) {
                errors.put("recordId", "主数据ID格式无效");
            } else {
                original = dataService.getRecord(instanceId, recordId);
                if (!"1".equals(String.valueOf(original.get("STATUS")))) {
                    errors.put("recordId", "只有生效状态的主数据可修改");
                }
                Long pending = changeRequestItemService.getPendingCountForRecord(
                        model.getModelId(), recordId);
                if (pending != null && pending > 0) {
                    errors.put("recordId", pendingMaintenanceMessage(model.getModelId(), recordId));
                }
            }
        } else if (create && !maintenanceTrim(request.getRecordId()).isEmpty()) {
            errors.put("recordId", "新增申请不能指定来源主数据ID");
        }
        Map<String, String> values = request.getValues();
        if (values == null) values = new HashMap<>();
        if (values.size() > 200) throw new ServiceException("申请字段数量超限");
        if (update) {
            Set<String> automaticFields = maintenanceValidator.automaticFieldCodes(model);
            values.keySet().stream()
                    .filter(Objects::nonNull)
                    .filter(code -> automaticFields.contains(code.toUpperCase(Locale.ROOT)))
                    .forEach(code -> errors.put(code, "自动编码字段由模型编码方案维护，不允许修改"));
        }
        if (create) {
            if (!"NORMAL".equalsIgnoreCase(model.getModelType())
                    && !"CATEGORY".equalsIgnoreCase(model.getModelType())) {
                errors.put("mode", "当前模型类型不支持单条新增");
            }
            if (!errors.isEmpty()) {
                response.setValid(false);
                return response;
            }
            values = allowUnassignedAutomatic
                    ? maintenanceValidator.withDefaultsForCreate(model, values)
                    : maintenanceValidator.withDefaults(model, values);
            errors.putAll(maintenanceValidator.validate(model, values, null, allowUnassignedAutomatic));
            maintenanceValidateParent(model, instanceId, request.getParentId(), null, errors);
            submodelService.validateChanges(instanceId, model, mode, null, request.getSubmodelChanges(), errors);
            response.setValid(errors.isEmpty());
            return response;
        }
        if (recordId != null && original != null) {
            Map<String, String> effectiveValues = submissionBusinessSnapshot(model, original);
            effectiveValues.putAll(values);
            errors.putAll(maintenanceValidator.validate(model, effectiveValues, recordId));
        }
        maintenanceValidateParent(model, instanceId, request.getParentId(), update ? recordId : null, errors);
        submodelService.validateChanges(instanceId, model, mode, update ? recordId : null,
                request.getSubmodelChanges(), errors);
        response.setValid(errors.isEmpty());
        return response;
    }

    private void maintenanceValidateLifecycle(MasterModelRespDTO model, Long instanceId,
            MasterChangeRequestSaveReqVO request, Map<String, String> errors) {
        Long recordId = maintenanceParseId(request.getRecordId());
        if (recordId == null) {
            errors.put("recordId", "主数据ID格式无效");
        } else {
            Map<String, Object> row = dataService.getRecord(instanceId, recordId);
            if (MasterLifecycleRules.target(request.getMode(), String.valueOf(row.get("STATUS"))) == null) {
                errors.put("mode", "当前生命周期不允许此状态操作");
            }
            Long pending = changeRequestItemService.getPendingCountForRecord(
                    model.getModelId(), recordId);
            if (pending != null && pending > 0) errors.put("recordId", pendingMaintenanceMessage(model.getModelId(), recordId));
        }
        if (request.getValues() != null && !request.getValues().isEmpty()) {
            errors.put("values", "状态申请不能修改业务字段");
        }
        if (request.getSubmodelChanges() != null && !request.getSubmodelChanges().isEmpty()) {
            errors.put("submodelChanges", "状态申请不能修改子模型数据");
        }
        if (!maintenanceTrim(request.getParentId()).isEmpty()) errors.put("parentId", "状态申请不能修改父级节点");
        String reason = maintenanceTrim(request.getApplyReason());
        if (reason.isEmpty()) errors.put("applyReason", "请输入申请原因");
        else if (reason.codePointCount(0, reason.length()) > 500) {
            errors.put("applyReason", "申请原因不能超过500字");
        }
    }

    private void maintenanceValidateParent(MasterModelRespDTO model, Long instanceId, String parent,
            Long recordId, Map<String, String> errors) {
        if (!"CATEGORY".equals(model.getModelType())) {
            if (!maintenanceTrim(parent).isEmpty()) errors.put("parentId", "普通模型不能设置父级节点");
            return;
        }
        if (model.getColumnMappings() == null || !model.getColumnMappings().containsKey("CATEGORY_ID")) {
            errors.put("parentId", "分类模型父级字段绑定不完整，请检查模型发布配置");
            return;
        }
        if (maintenanceTrim(parent).isEmpty()) return;
        Long parentId = maintenanceParseId(parent);
        if (parentId == null) {
            errors.put("parentId", "父级节点ID格式无效");
            return;
        }
        List<MasterTreeNodeRespVO> tree = dataService.getTree(instanceId);
        Set<String> forbidden = new HashSet<>();
        if (recordId != null) maintenanceCollectDescendants(maintenanceFind(tree, String.valueOf(recordId)), forbidden);
        if (forbidden.contains(String.valueOf(parentId))) errors.put("parentId", "不能选择当前节点或其后代作为父级");
        else if (maintenanceFind(tree, String.valueOf(parentId)) == null) {
            errors.put("parentId", "父级节点不存在");
        } else {
            try {
                Map<String, Object> parentRow = dataService.getRecord(instanceId, parentId);
                if (!"1".equals(String.valueOf(parentRow.get("STATUS")))) {
                    errors.put("parentId", "父级节点必须处于生效状态");
                }
            } catch (ServiceException ex) {
                errors.put("parentId", "父级节点不存在");
            }
        }
    }

    private MasterTreeNodeRespVO maintenanceFind(List<MasterTreeNodeRespVO> nodes, String id) {
        for (MasterTreeNodeRespVO node : nodes) {
            if (id.equals(node.getId())) return node;
            MasterTreeNodeRespVO child = maintenanceFind(node.getChildren(), id);
            if (child != null) return child;
        }
        return null;
    }

    private void maintenanceCollectDescendants(MasterTreeNodeRespVO node, Set<String> ids) {
        if (node == null || !ids.add(node.getId())) return;
        for (MasterTreeNodeRespVO child : node.getChildren()) maintenanceCollectDescendants(child, ids);
    }

    private Long maintenanceParseId(String text) {
        try {
            long id = Long.parseLong(maintenanceTrim(text));
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String maintenanceTrim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    // 批量维护申请预校验
    private static final int MAX_ROWS = 500;
    private static final Pattern BATCH_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");
    private static final DateTimeFormatter BATCH_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    /**
     * 只读校验整批维护申请；提交时在写事务中再次校验。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @return 校验结果及问题明细
     */
    @Override
    public MasterChangeBatchValidateRespVO validateBatch(Long instanceId, MasterChangeRequestBatchSaveReqVO request) {
        return validateBatch(instanceId, request, myCurrentUserId());
    }

    private MasterChangeBatchValidateRespVO validateBatch(Long instanceId,
            MasterChangeRequestBatchSaveReqVO request, Long applicantId) {
        if (request == null) throw new ServiceException("批量申请内容不能为空");
        maintenanceValidator.validateApplicant(applicantId, request.getMode());
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        MasterChangeBatchValidateRespVO result = new MasterChangeBatchValidateRespVO();
        String reason = request.getApplyReason() == null ? "" : request.getApplyReason().trim();
        if (reason.isEmpty()) result.getErrors().put("applyReason", "请输入申请原因");
        else if (reason.codePointCount(0, reason.length()) > 500) {
            result.getErrors().put("applyReason", "申请原因不能超过500字");
        }

        String mode = request.getMode();
        if ("BATCH_UPDATE".equals(mode) || "BATCH_STATUS".equals(mode)) {
            batchValidateSelected(instanceId, model, request, reason, result);
        } else if ("BATCH_IMPORT".equals(mode)) {
            batchValidateImport(instanceId, model, request, reason, applicantId, result);
        } else {
            result.getErrors().put("mode", "不支持的批量申请类型");
        }
        result.setTotal(result.getRows().size());
        result.setPassed((int) result.getRows().stream().filter(row -> row.getErrors().isEmpty()).count());
        result.setValid(result.getErrors().isEmpty() && result.getTotal() > 0
                && result.getPassed() == result.getTotal());
        return result;
    }

    private void batchValidateSelected(Long instanceId, MasterModelRespDTO model,
            MasterChangeRequestBatchSaveReqVO request, String reason, MasterChangeBatchValidateRespVO result) {
        List<String> ids = request.getRecordIds();
        if (ids == null || ids.isEmpty()) {
            result.getErrors().put("recordIds", "请选择至少一条主数据");
            return;
        }
        if (ids.size() > MAX_ROWS) throw new ServiceException("单批最多校验500条主数据");
        boolean update = "BATCH_UPDATE".equals(request.getMode());
        Set<String> automaticFields = maintenanceValidator.automaticFieldCodes(model);
        if (update && (request.getValues() == null || request.getValues().isEmpty())) {
            result.getErrors().put("values", "请选择至少一个修改字段");
        }
        if (update && request.getValues() != null) {
            request.getValues().keySet().stream()
                    .filter(Objects::nonNull)
                    .filter(code -> automaticFields.contains(code.toUpperCase(Locale.ROOT)))
                    .forEach(code -> result.getErrors().put(code, "自动编码字段由模型编码方案维护，不允许修改"));
        }
        if (update && ids.size() > 1 && request.getValues() != null) {
            for (MasterFieldRespDTO field : model.getFields()) {
                String batchValue = request.getValues().get(field.getCode());
                if (Boolean.TRUE.equals(field.getUnique()) && batchValue != null && !batchValue.trim().isEmpty()) {
                    result.getErrors().put(field.getCode(), "不能将多条主数据的唯一字段统一改为相同值");
                }
            }
        }
        if (!update && !MasterLifecycleRules.isAction(request.getAction())) {
            result.getErrors().put("action", "批量状态操作无效");
        }
        if (!update && request.getValues() != null && !request.getValues().isEmpty()) {
            result.getErrors().put("values", "批量状态不能修改业务字段");
        }
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            MasterChangeBatchValidateRespVO.RowResult row = batchNewRow(i + 1, ids.get(i),
                    update ? "UPDATE" : request.getAction());
            result.getRows().add(row);
            Long recordId = batchPositiveId(ids.get(i));
            if (recordId == null) {
                row.getErrors().put("recordId", "主数据ID格式无效");
                continue;
            }
            if (!seen.add(recordId)) {
                row.getErrors().put("recordId", "同一批次不能重复选择主数据");
                continue;
            }
            if (!result.getErrors().isEmpty()) {
                row.getErrors().put("batch", "请先修正批次参数");
                continue;
            }
            try {
                Map<String, Object> original = update
                        ? dataService.getRecord(instanceId, recordId) : Collections.emptyMap();
                // 单条维护校验会合并当前快照；这里只传变更，避免将自动编码误判为修改。
                Map<String, String> values = update ? request.getValues() : null;
                row.getErrors().putAll(batchValidateOne(instanceId, update ? "UPDATE" : request.getAction(),
                        ids.get(i), null, values, reason));
                if (update && row.getErrors().isEmpty() && !batchHasActualChange(model, original, request.getValues())) {
                    row.getErrors().put("values", "所选字段修改后与当前数据一致");
                }
            } catch (ServiceException ex) {
                row.getErrors().put("recordId", ex.getMessage());
            }
        }
    }

    private void batchValidateImport(Long instanceId, MasterModelRespDTO model,
            MasterChangeRequestBatchSaveReqVO request, String reason, Long applicantId,
            MasterChangeBatchValidateRespVO result) {
        List<MasterChangeRequestBatchSaveReqVO.ImportRow> rows = request.getRows();
        if (rows == null || rows.isEmpty()) {
            result.getErrors().put("rows", "导入数据不能为空");
            return;
        }
        if (rows.size() > MAX_ROWS) throw new ServiceException("单批最多校验500行导入数据");
        try {
            maintenanceValidator.validateApplicant(applicantId, "BATCH_IMPORT");
        } catch (ServiceException ex) {
            result.getErrors().put("permission", ex.getMessage());
        }
        boolean upsert = "UPSERT".equals(request.getImportMode());
        boolean updateOnly = "UPDATE_ONLY".equals(request.getImportMode());
        Set<String> automaticFields = maintenanceValidator.automaticFieldCodes(model);
        if (!upsert && !updateOnly && !"CREATE_ONLY".equals(request.getImportMode())) {
            result.getErrors().put("importMode", "导入方式无效");
        }
        List<MasterFieldRespDTO> businessKeys = model.getFields().stream()
                .filter(field -> Boolean.TRUE.equals(field.getUnique())
                        && !"SYSTEM".equalsIgnoreCase(field.getCategory()) && batchSafeIdentifier(field.getCode())
                        && !automaticFields.contains(field.getCode().toUpperCase(Locale.ROOT)))
                .collect(java.util.stream.Collectors.toList());
        String matchField = request.getMatchField();
        boolean recordIdImport = rows.stream().anyMatch(row -> row != null && approvalHasText(row.getRecordId()));
        // 自动编码由提交阶段逐行生成；没有手工唯一字段时，仅新增无需匹配现有记录。
        boolean directCreate = "CREATE_ONLY".equals(request.getImportMode())
                && businessKeys.isEmpty() && !automaticFields.isEmpty() && !approvalHasText(matchField);
        MasterFieldRespDTO matchingField = businessKeys.stream()
                .filter(field -> field.getCode().equals(matchField)).findFirst().orElse(null);
        if (!recordIdImport && businessKeys.isEmpty() && !directCreate) {
            result.getErrors().put("matchField", "模型未配置唯一业务字段");
        } else if (!recordIdImport && !directCreate && (matchField == null || matchField.isEmpty())) {
            result.getErrors().put("matchField", "请选择匹配业务键");
        } else if (!recordIdImport && !directCreate && matchingField == null) {
            result.getErrors().put("matchField", "匹配业务键必须是当前模型配置的唯一业务字段");
        }
        Map<String, MasterChangeBatchValidateRespVO.RowResult> matchValues = new HashMap<>();
        Map<Integer, MasterChangeBatchValidateRespVO.RowResult> sourceRows = new HashMap<>();
        Map<Long, MasterChangeBatchValidateRespVO.RowResult> recordRows = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            MasterChangeRequestBatchSaveReqVO.ImportRow source = rows.get(i);
            MasterChangeBatchValidateRespVO.RowResult row = batchNewRow(source != null && source.getRowNo() != null
                    ? source.getRowNo() : i + 2, null,
                    updateOnly ? "SKIP" : "CREATE");
            result.getRows().add(row);
            MasterChangeBatchValidateRespVO.RowResult firstSource = sourceRows.putIfAbsent(row.getRowNo(), row);
            if (row.getRowNo() < 2) row.getErrors().put("row", "文件行号不能小于2");
            if (firstSource != null) {
                firstSource.getErrors().put("row", "文件行号重复，请重新上传文件");
                row.getErrors().put("row", "文件行号重复，请重新上传文件");
            }
            if (result.getErrors().containsKey("permission")) row.getErrors().put("permission", result.getErrors().get("permission"));
            if (source == null) {
                row.getErrors().put("values", "导入行不能为空");
                continue;
            }
            Map<String, String> values = source.getValues() == null ? Collections.emptyMap() : source.getValues();
            values.keySet().stream()
                    .filter(Objects::nonNull)
                    .filter(code -> automaticFields.contains(code.toUpperCase(Locale.ROOT)))
                    .forEach(code -> row.getErrors().put(code,
                            "自动编码字段由模型编码方案生成，导入文件不应包含该字段"));
            if (recordIdImport) {
                if (approvalHasText(source.getRecordId())) {
                    Long id = batchPositiveId(source.getRecordId());
                    if (id == null) row.getErrors().put("recordId", "主数据ID格式无效");
                    else {
                        MasterChangeBatchValidateRespVO.RowResult duplicate = recordRows.putIfAbsent(id, row);
                        if (duplicate != null) {
                            duplicate.getErrors().put("recordId", "导入文件中主数据ID重复");
                            row.getErrors().put("recordId", "导入文件中主数据ID重复");
                        }
                        try {
                            dataService.getRecord(instanceId, id);
                            row.setRecordId(id.toString());
                            row.setOperation("CREATE_ONLY".equals(request.getImportMode()) ? "SKIP" : "UPDATE");
                            if ("SKIP".equals(row.getOperation())) row.setMessage("匹配到现有数据，仅新增模式不处理");
                        } catch (ServiceException ex) {
                            row.getErrors().put("recordId", "主数据ID不属于当前模型的有效记录，请重新导出");
                        }
                    }
                } else if (updateOnly) {
                    row.setOperation("SKIP");
                    row.setMessage("未填写主数据ID，仅更新模式不处理");
                }
            } else if (!directCreate && !result.getErrors().containsKey("matchField")) {
                String key = values.get(matchField);
                if (key == null || key.trim().isEmpty()) {
                    row.getErrors().put(matchField, "匹配业务键不能为空");
                } else {
                    try {
                        Object typedKey = MaintenanceData.convert(key, matchingField.getDataType());
                        String comparableKey = typedKey instanceof java.math.BigDecimal
                                ? ((java.math.BigDecimal) typedKey).stripTrailingZeros().toPlainString() : String.valueOf(typedKey);
                        MasterChangeBatchValidateRespVO.RowResult firstMatch = matchValues.putIfAbsent(comparableKey, row);
                        if (firstMatch != null) {
                            firstMatch.getErrors().put(matchField, "导入文件中匹配业务键重复");
                            row.getErrors().put(matchField, "导入文件中匹配业务键重复");
                        }
                        Long existing = batchExistingRecordId(model, matchField, typedKey);
                        if (existing != null) {
                            row.setRecordId(existing.toString());
                            row.setOperation("CREATE_ONLY".equals(request.getImportMode()) ? "SKIP" : "UPDATE");
                            if ("CREATE_ONLY".equals(request.getImportMode())) {
                                row.setMessage("匹配到现有数据，仅新增模式不处理");
                            }
                        } else if (updateOnly) {
                            row.setOperation("SKIP");
                            row.setMessage("未匹配到现有数据，仅更新模式不处理");
                        }
                    } catch (ServiceException ex) {
                        row.getErrors().put(matchField, ex.getMessage());
                    }
                }
            }
            if (result.getErrors().containsKey("importMode") || result.getErrors().containsKey("matchField")) {
                row.getErrors().put("batch", "请先修正导入参数");
                continue;
            }
            if (row.getErrors().isEmpty() && !"SKIP".equals(row.getOperation())) {
                if ("CREATE".equals(row.getOperation())) {
                    values = maintenanceValidator.withDefaultsForCreate(model, values);
                }
                try {
                    row.getErrors().putAll(maintenanceValidator.dataPermissionErrors(applicantId, model,
                            row.getOperation(), values.keySet()));
                } catch (ServiceException ex) {
                    row.getErrors().put("permission", ex.getMessage());
                }
                try {
                    Map<String, Object> original = "UPDATE".equals(row.getOperation())
                            ? dataService.getRecord(instanceId, Long.valueOf(row.getRecordId())) : Collections.emptyMap();
                    String parentId = source.getParentId();
                    if ("UPDATE".equals(row.getOperation()) && parentId == null)
                        parentId = submissionValue(original.get("CATEGORY_ID"));
                    row.getErrors().putAll(batchValidateOne(instanceId, row.getOperation(), row.getRecordId(),
                            parentId, values, reason));
                    if (row.getErrors().isEmpty() && "UPDATE".equals(row.getOperation())
                            && !batchHasActualChange(model, original, values)
                            && (source.getParentId() == null || batchTrim(source.getParentId())
                                    .equals(batchTrim(submissionValue(original.get("CATEGORY_ID")))))) {
                        row.setOperation("SKIP");
                        row.setMessage("导入值与现有数据一致，不处理");
                    }
                } catch (ServiceException ex) {
                    row.getErrors().put("row", ex.getMessage());
                }
            }
        }
        if (result.getRows().stream().allMatch(row -> "SKIP".equals(row.getOperation()))) {
            result.getErrors().put("rows", "当前导入方式下没有可处理的数据行");
        }
    }

    private boolean batchHasActualChange(MasterModelRespDTO model, Map<String, Object> original,
            Map<String, String> changes) {
        if (changes == null || changes.isEmpty()) return false;
        for (Map.Entry<String, String> change : changes.entrySet()) {
            String dataType = model.getFields().stream()
                    .filter(field -> change.getKey().equals(field.getCode()))
                    .findFirst()
                    .map(MasterFieldRespDTO::getDataType)
                    .orElse(null);
            String before = batchValue(original.get(change.getKey().toUpperCase(Locale.ROOT)), dataType);
            String after = change.getValue();
            if (!MaintenanceData.sameValue(before, after, dataType)) return true;
        }
        return false;
    }

    private Long batchExistingRecordId(MasterModelRespDTO model, String field, Object batchValue) {
        String table = batchTableName(model);
        List<Long> matches = dataQueryRepository.selectIdsByColumn(model.getDatasourceId(), table, field, batchValue, model.getColumnMappings());
        if (matches.size() > 1) throw new ServiceException("匹配字段在正式数据中不唯一");
        if (matches.isEmpty()) return null;
        return matches.get(0);
    }

    private Map<String, String> batchValidateOne(Long instanceId, String mode, String recordId,
            String parentId, Map<String, String> values, String reason) {
        MasterChangeRequestSaveReqVO single = new MasterChangeRequestSaveReqVO();
        single.setMode(mode);
        single.setRecordId(recordId);
        single.setParentId(parentId);
        single.setValues(values);
        single.setApplyReason(reason);
        return validateMaintenance(instanceId, single).getErrors();
    }

    private MasterChangeBatchValidateRespVO.RowResult batchNewRow(int rowNo, String recordId, String operation) {
        MasterChangeBatchValidateRespVO.RowResult row = new MasterChangeBatchValidateRespVO.RowResult();
        row.setRowNo(rowNo);
        row.setRecordId(recordId);
        row.setOperation(operation);
        return row;
    }

    private Long batchPositiveId(String text) {
        try {
            long batchValue = Long.parseLong(text == null ? "" : text.trim());
            return batchValue > 0 ? batchValue : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String batchValue(Object input) {
        if (input == null) return null;
        if (input instanceof Timestamp) return ((Timestamp) input).toLocalDateTime().format(BATCH_DATE_TIME);
        if (input instanceof LocalDateTime) return ((LocalDateTime) input).format(BATCH_DATE_TIME);
        return String.valueOf(input);
    }

    private String batchValue(Object input, String dataType) {
        if (input == null) return null;
        String type = dataType == null ? "" : dataType.toUpperCase(Locale.ROOT);
        if (type.contains("DATE") && !type.contains("TIME")) {
            if (input instanceof Timestamp) return ((Timestamp) input).toLocalDateTime().toLocalDate().toString();
            if (input instanceof LocalDateTime) return ((LocalDateTime) input).toLocalDate().toString();
            if (input instanceof java.sql.Date) return ((java.sql.Date) input).toLocalDate().toString();
            if (input instanceof LocalDate) return input.toString();
            String text = String.valueOf(input).trim();
            if (text.matches("^\\d{4}-\\d{2}-\\d{2}(?:[ T].*)?$")) return text.substring(0, 10);
        }
        return batchValue(input);
    }

    private String batchTrim(String input) {
        return input == null ? "" : input.trim();
    }

    private String batchTableName(MasterModelRespDTO model) {
        String name = model.getPhysicalTableName();
        String schema = model.getSchemaName();
        if (name != null && name.contains(".")) {
            String[] parts = name.split("\\.", -1);
            if (parts.length != 2 || !batchSafeIdentifier(parts[0]) || !batchSafeIdentifier(parts[1])) {
                throw new ServiceException("模型物理表标识无效");
            }
            return name;
        }
        if (!batchSafeIdentifier(name) || (schema != null && !schema.trim().isEmpty() && !batchSafeIdentifier(schema))) {
            throw new ServiceException("模型物理表标识无效");
        }
        return schema == null || schema.trim().isEmpty() ? name : schema + "." + name;
    }

    private boolean batchSafeIdentifier(String batchValue) {
        return batchValue != null && BATCH_IDENTIFIER.matcher(batchValue).matches();
    }

    // 正式提交与申请持久化
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter SUBMISSION_REQUEST_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 先校验并固定申请内容，再在同一事务内保存申请、明细及执行任务。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @param applicantId 申请人 ID
     * @param applicantName 申请人姓名
     * @return 申请提交结果
     */
    @Override
    public MasterChangeRequestSubmitRespVO submitSingle(Long instanceId,
            MasterChangeRequestSaveReqVO request, Long applicantId, String applicantName) {
        return submitSingle(instanceId, request, applicantId, applicantName, "MANUAL");
    }

    @Override
    public MasterChangeRequestSubmitRespVO submitSingle(Long instanceId,
            MasterChangeRequestSaveReqVO request, Long applicantId, String applicantName, String changeSource) {
        submissionPositiveId(instanceId, "模型实例ID无效");
        submissionPositiveId(applicantId, "请先登录再提交申请");
        String source = changeSource == null ? "" : changeSource.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("MANUAL", "MERGE").contains(source)) throw new ServiceException("申请来源无效");
        if (request == null) throw new ServiceException("申请内容不能为空");
        maintenanceValidator.validateApplicant(applicantId, request.getMode());
        MasterChangeValidateRespVO validation =
                validateMaintenance(instanceId, request);
        if (!validation.isValid()) {
            throw new ServiceException(submissionValidationMessage(validation));
        }

        String mode = request.getMode();
        boolean create = "CREATE".equals(mode);
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        if (create) {
            Map<String, String> values = maintenanceValidator.withDefaultsForCreate(model, request.getValues());
            automaticEncodingGenerator.generate(model, values);
            request.setValues(values);
            validation = validateMaintenance(instanceId, request, false);
            if (!validation.isValid()) {
                throw new ServiceException(submissionValidationMessage(validation));
            }
        }
        boolean lifecycle = MasterLifecycleRules.isAction(mode);
        Long modelVersionId = model.getModelVersionId();
        Long recordId = submissionParseId(request.getRecordId());
        Map<String, Object> current = recordId == null
                ? Collections.emptyMap() : dataService.getRecord(instanceId, recordId);
        String currentVersionNo = recordId == null ? null
                : submissionCurrentVersions(model.getModelId(), Collections.singletonList(recordId)).get(recordId);
        Map<String, String> before = recordId == null
                ? new LinkedHashMap<>() : submissionBusinessSnapshot(model, current);
        Map<String, String> proposed = submissionProposedSingle(model, request, before);
        String status = create ? "0" : submissionValue(current.get("STATUS"));
        String targetStatus = create ? "1"
                : lifecycle ? MasterLifecycleRules.target(mode, status) : status;
        String submissionRequestNo = submissionRequestNo();
        String requestName = submissionLimit(submissionSafe(model.getModelName()) + " · " + submissionSingleLabel(request), 128);
        String beforeData = submissionSerialize(create ? Collections.emptyMap() : before);
        String proposedData = submissionSerializePayload(proposed, request.getSubmodelChanges(), create, model, recordId);
        String applyReason = request.getApplyReason();

        // 等待模型锁后必须能读取上一提交者刚提交的申请，不能复用旧快照。
        TransactionTemplate submissionTransaction = new TransactionTemplate(
                transactionTemplate.getTransactionManager(), transactionTemplate);
        submissionTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return submissionTransaction.execute(transactionStatus -> {
            lockAndCheckPendingMaintenance(model.getModelId(),
                    create ? Collections.emptyList() : Collections.singletonList(recordId));
            long requestId = submissionInsertRequest(instanceId, model, modelVersionId, mode,
                    applyReason, submissionRequestNo, requestName, applicantId, applicantName, 1, source);
            MasterChangeRequestItemDO item = new MasterChangeRequestItemDO();
            item.setRequestId(requestId);
            item.setItemNo(1);
            item.setMasterRecordId(create ? null : recordId);
            item.setChangeType(mode);
            item.setBeforeData(beforeData);
            item.setProposedData(proposedData);
            item.setBeforeStatus(status);
            item.setTargetStatus(targetStatus);
            item.setCurrentVersionNo(create ? null : currentVersionNo);
            item.setRemark("主数据维护申请");
            changeRequestItemService.initializeItem(item, applicantId, applicantName);
            if (changeRequestItemService.insertItem(item) != 1) {
                throw new ServiceException("建立申请明细失败");
            }
            executeTaskService.createPendingTask(requestId);
            return submissionResponse(requestId, submissionRequestNo, requestName, 1);
        });
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    /**
     * 同一事务内校验并建立批量申请及明细。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @param applicantId 申请人 ID
     * @param applicantName 申请人姓名
     * @return 申请提交结果
     */
    @Override
    public MasterChangeRequestSubmitRespVO submitBatch(Long instanceId,
            MasterChangeRequestBatchSaveReqVO request, Long applicantId, String applicantName) {
        submissionPositiveId(instanceId, "模型实例ID无效");
        submissionPositiveId(applicantId, "请先登录再提交申请");
        MasterChangeBatchValidateRespVO validation = validateBatch(instanceId, request, applicantId);
        if (!validation.isValid()) {
            throw new ServiceException(submissionValidationMessage(validation));
        }

        if ("BATCH_IMPORT".equals(request.getMode())
                && (request.getFileName() == null || request.getFileName().trim().isEmpty()
                    || request.getFileName().length() > 256)) {
            throw new ServiceException("上传文件名不能为空且不能超过256字");
        }

        MasterModelRespDTO model = metadataService.getModel(instanceId);
        Long modelVersionId = model.getModelVersionId();
        List<SubmissionItem> items = submissionBatchItems(instanceId, model, request, validation);
        if ("BATCH_UPDATE".equals(request.getMode()) || "BATCH_STATUS".equals(request.getMode())) {
            maintenanceValidator.validateApplicant(applicantId, request.getMode());
            maintenanceValidator.validateDataPermission(applicantId, model,
                    "BATCH_UPDATE".equals(request.getMode()) ? "BATCH_UPDATE" : request.getAction(),
                    "BATCH_UPDATE".equals(request.getMode()) ? request.getValues().keySet() : Collections.emptySet());
        }
        String submissionRequestNo = submissionRequestNo();
        String requestName = submissionLimit(submissionSafe(model.getModelName()) + " · " + submissionBatchLabel(request)
                + "（" + items.size() + "条）", 128);
        lockAndCheckPendingMaintenance(model.getModelId(), items.stream()
                .map(item -> item.recordId).filter(java.util.Objects::nonNull)
                .distinct().collect(java.util.stream.Collectors.toList()));
        long requestId = submissionInsertRequest(instanceId, model, modelVersionId, request.getMode(),
                request.getApplyReason(), submissionRequestNo, requestName, applicantId, applicantName, items.size(), "MANUAL");
        List<MasterChangeRequestItemDO> records = new ArrayList<>(items.size());
        List<Integer> importRowNumbers = validation.getRows().stream()
                .filter(row -> !"SKIP".equals(row.getOperation()))
                .map(MasterChangeBatchValidateRespVO.RowResult::getRowNo)
                .collect(java.util.stream.Collectors.toList());
        for (int i = 0; i < items.size(); i++) {
            SubmissionItem item = items.get(i);
            MasterChangeRequestItemDO record = new MasterChangeRequestItemDO();
            record.setRequestId(requestId);
            record.setItemNo(i + 1);
            if ("BATCH_IMPORT".equals(request.getMode())) {
                record.setItemNo(importRowNumbers.get(i));
            }
            record.setMasterRecordId(item.recordId);
            record.setChangeType(item.changeType);
            record.setBeforeData(item.beforeData);
            record.setProposedData(item.proposedData);
            record.setBeforeStatus(item.beforeStatus);
            record.setTargetStatus(item.targetStatus);
            record.setCurrentVersionNo(item.currentVersionNo);
            record.setRemark("批量维护申请明细");
            changeRequestItemService.initializeItem(record, applicantId, applicantName);
            records.add(record);
        }
        if (records.isEmpty() || !changeRequestItemService.insertItems(records)) {
            throw new ServiceException("建立批量申请明细失败");
        }
        if ("BATCH_IMPORT".equals(request.getMode())) {
            MasterChangeImportDO record = new MasterChangeImportDO();
            record.setRequestId(requestId);
            record.setModelId(model.getModelId());
            record.setModelVersionId(model.getModelVersionId());
            record.setInstanceId(instanceId);
            record.setTemplateVersionNo(model.getVersionNo());
            record.setFileName(request.getFileName());
            record.setTotalCount(validation.getTotal());
            record.setImportMode(request.getImportMode());
            record.setMatchField(request.getMatchField());
            record.setCreateCount((int) validation.getRows().stream().filter(row -> "CREATE".equals(row.getOperation())).count());
            record.setUpdateCount((int) validation.getRows().stream().filter(row -> "UPDATE".equals(row.getOperation())).count());
            record.setCreatorId(applicantId);
            record.setCreateBy(applicantName);
            record.setUpdatorId(applicantId);
            record.setUpdateBy(applicantName);
            record.setValidFlag(true);
            record.setDelFlag(false);
            if (importMapper.insert(record) != 1) throw new ServiceException("保存上传记录失败");
        }
        executeTaskService.createPendingTask(requestId);
        return submissionResponse(requestId, submissionRequestNo, requestName, items.size());
    }

    /**
     * 项目库模型行作为跨实例的提交互斥点，只在申请落库的短事务中持有。
     * 同模型的不同记录可以各有申请；同一记录只能有一张未完成申请。
     */
    private void lockAndCheckPendingMaintenance(Long modelId, List<Long> recordIds) {
        if (recordIds.isEmpty()) return;
        if (!java.util.Objects.equals(modelId, changeRequestMapper.lockModelForMaintenance(modelId))) {
            throw new ServiceException("主数据模型不存在，无法提交申请");
        }
        List<MasterChangeRequestItemQueryDTO> pending =
                changeRequestMapper.selectPendingRecordRequests(modelId, recordIds);
        if (pending != null && !pending.isEmpty()) {
            throw new ServiceException(pendingMaintenanceMessage(pending.get(0)));
        }
    }

    private String pendingMaintenanceMessage(Long modelId, Long recordId) {
        List<MasterChangeRequestItemQueryDTO> pending = changeRequestMapper.selectPendingRecordRequests(
                modelId, Collections.singletonList(recordId));
        return pending == null || pending.isEmpty()
                ? "主数据ID " + recordId + " 有尚未处理完成的申请，请刷新后重试"
                : pendingMaintenanceMessage(pending.get(0));
    }

    private String pendingMaintenanceMessage(MasterChangeRequestItemQueryDTO conflict) {
        return "主数据ID " + conflict.getRecordId() + " 已存在未完成申请 "
                + conflict.getRequestNo() + "，请处理完成后再申请；本次申请未提交";
    }

    private List<SubmissionItem> submissionBatchItems(Long instanceId, MasterModelRespDTO model,
            MasterChangeRequestBatchSaveReqVO request, MasterChangeBatchValidateRespVO validation) {
        List<SubmissionItem> items = new ArrayList<>();
        List<Long> recordIds = new ArrayList<>();
        if ("BATCH_IMPORT".equals(request.getMode())) {
            for (MasterChangeBatchValidateRespVO.RowResult checked : validation.getRows()) {
                Long recordId = submissionParseId(checked.getRecordId());
                if (recordId != null && !"SKIP".equals(checked.getOperation())) recordIds.add(recordId);
            }
        } else {
            for (String id : request.getRecordIds()) recordIds.add(submissionParseId(id));
        }
        Map<Long, String> currentVersions = submissionCurrentVersions(model.getModelId(), recordIds);
        if ("BATCH_IMPORT".equals(request.getMode())) {
            List<MasterChangeRequestBatchSaveReqVO.ImportRow> rows = request.getRows();
            for (int i = 0; i < rows.size(); i++) {
                MasterChangeBatchValidateRespVO.RowResult checked = validation.getRows().get(i);
                if ("SKIP".equals(checked.getOperation())) continue;
                Long recordId = submissionParseId(checked.getRecordId());
                Map<String, Object> current = recordId == null ? Collections.emptyMap()
                        : dataService.getRecord(instanceId, recordId);
                Map<String, String> before = recordId == null ? new LinkedHashMap<>()
                        : submissionBusinessSnapshot(model, current);
                Map<String, String> values = rows.get(i).getValues() == null
                        ? new LinkedHashMap<>() : new LinkedHashMap<>(rows.get(i).getValues());
                Map<String, String> proposed = "CREATE".equals(checked.getOperation())
                        ? maintenanceValidator.withDefaultsForCreate(model, values) : new LinkedHashMap<>(values);
                if ("CREATE".equals(checked.getOperation())) {
                    automaticEncodingGenerator.generate(model, proposed);
                    Map<String, String> generatedErrors = maintenanceValidator.validate(model, proposed, null, false);
                    if (!generatedErrors.isEmpty()) {
                        throw new ServiceException("第" + checked.getRowNo() + "行自动编码生成后校验失败："
                                + generatedErrors.values().iterator().next());
                    }
                }
                if (recordId == null || rows.get(i).getParentId() != null)
                    submissionAddParentValue(model, rows.get(i).getParentId(), proposed);
                String beforeStatus = submissionValue(current.get("STATUS"));
                if (recordId == null) beforeStatus = "0";
                items.add(new SubmissionItem(recordId, "UPDATE".equals(checked.getOperation())
                        ? "UPDATE" : "CREATE", submissionSerialize(before),
                        submissionSerializePayload(proposed, null, "CREATE".equals(checked.getOperation()), model, recordId),
                        beforeStatus, "UPDATE".equals(checked.getOperation())
                                ? beforeStatus : "1",
                        currentVersions.get(recordId)));
            }
            if (items.isEmpty()) throw new ServiceException("没有可处理的数据行，请检查导入方式和匹配键");
            return items;
        }

        for (String id : request.getRecordIds()) {
            Long recordId = submissionParseId(id);
            Map<String, Object> current = dataService.getRecord(instanceId, recordId);
            Map<String, String> before = submissionBusinessSnapshot(model, current);
            String status = submissionValue(current.get("STATUS"));
            if ("BATCH_STATUS".equals(request.getMode())) {
                String action = request.getAction();
                String target = MasterLifecycleRules.target(action, status);
                items.add(new SubmissionItem(recordId, action, submissionSerialize(before), submissionSerialize(before),
                        status, target, currentVersions.get(recordId)));
            } else {
                Map<String, String> proposed = new LinkedHashMap<>(request.getValues());
                items.add(new SubmissionItem(recordId, "UPDATE", submissionSerialize(before), submissionSerialize(proposed),
                        status, status, currentVersions.get(recordId)));
            }
        }
        return items;
    }

    /** 保留原详情查询的版本异常语义：重复当前版本报错，其他查询失败时版本号为空。 */
    private Map<Long, String> submissionCurrentVersions(Long modelId, List<Long> recordIds) {
        if (recordIds.isEmpty()) return Collections.emptyMap();
        try {
            return dataVersionService.getCurrentVersionNumbers(modelId, recordIds);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("维护申请当前版本信息暂不可用，modelId={}", modelId, ex);
            return Collections.emptyMap();
        }
    }

    private Map<String, String> submissionProposedSingle(MasterModelRespDTO model,
            MasterChangeRequestSaveReqVO request, Map<String, String> before) {
        String mode = request.getMode();
        boolean create = "CREATE".equals(mode);
        boolean lifecycle = MasterLifecycleRules.isAction(mode);
        if (lifecycle) return new LinkedHashMap<>(before);
        Map<String, String> proposed = create ? maintenanceValidator.withDefaults(model, request.getValues())
                : new LinkedHashMap<>();
        if (request.getValues() != null) proposed.putAll(request.getValues());
        submissionAddParentValue(model, request.getParentId(), proposed);
        return proposed;
    }

    private void submissionAddParentValue(MasterModelRespDTO model, String parentId, Map<String, String> values) {
        if ("CATEGORY".equals(model.getModelType()) && parentId != null) {
            values.put("CATEGORY_ID", submissionTrim(parentId));
        }
    }

    private Map<String, String> submissionMergedValues(Map<String, String> before, Map<String, String> changes) {
        Map<String, String> values = new LinkedHashMap<>(before);
        if (changes != null) values.putAll(changes);
        return values;
    }

    private long submissionInsertRequest(Long instanceId, MasterModelRespDTO model, Long modelVersionId, String mode,
            String reason, String submissionRequestNo, String requestName, Long applicantId,
            String applicantName, int itemCount, String changeSource) {
        MasterChangeRequestDO request = new MasterChangeRequestDO();
        request.setRequestNo(submissionRequestNo);
        request.setModelId(model.getModelId());
        request.setModelVersionId(modelVersionId);
        request.setInstanceId(instanceId);
        request.setExecutionId(UUID.randomUUID().toString().replace("-", ""));
        request.setChangeType(MasterLifecycleRules.isAction(mode) || "BATCH_STATUS".equals(mode)
                ? "STATUS_CHANGE"
                : "BATCH_IMPORT".equals(mode) ? "BATCH_IMPORT" : mode);
        request.setChangeSource(changeSource);
        request.setRequestName(requestName);
        request.setApplicantId(applicantId);
        request.setApplicantName(submissionLimit(applicantName, 64));
        request.setApplyReason(submissionTrim(reason));
        // 本期免人工审批：申请落库即视为审核通过，正式数据仍由执行器异步写入。
        request.setStatus("1");
        request.setExecuteStatus("0");
        request.setItemCount(itemCount);
        request.setSuccessItemCount(0);
        request.setFailItemCount(0);
        request.setSubmitTime(new Date());
        request.setValidFlag(true);
        request.setDelFlag(false);
        request.setCreateBy(submissionLimit(applicantName, 32));
        request.setCreatorId(applicantId);
        request.setUpdateBy(submissionLimit(applicantName, 32));
        request.setUpdatorId(applicantId);
        request.setRemark("审核通过，等待执行器写入正式主数据");
        if (changeRequestMapper.insert(request) != 1 || request.getId() == null) {
            throw new ServiceException("建立申请记录失败");
        }
        return request.getId();
    }

    private MasterChangeRequestSubmitRespVO submissionResponse(long requestId, String submissionRequestNo,
            String requestName, int itemCount) {
        MasterChangeRequestSubmitRespVO submissionResponse = new MasterChangeRequestSubmitRespVO();
        submissionResponse.setRequestId(String.valueOf(requestId));
        submissionResponse.setRequestNo(submissionRequestNo);
        submissionResponse.setRequestName(requestName);
        submissionResponse.setApprovalStatus("1");
        submissionResponse.setItemCount(itemCount);
        submissionResponse.setFlowStarted(false);
        return submissionResponse;
    }

    private Map<String, String> submissionBusinessSnapshot(MasterModelRespDTO model, Map<String, Object> row) {
        Map<String, String> values = new LinkedHashMap<>();
        for (MasterFieldRespDTO field : model.getFields()) {
            if ("SYSTEM".equalsIgnoreCase(field.getCategory()) || field.getCode() == null) continue;
            Object submissionValue = row.get(field.getCode());
            if (submissionValue == null) submissionValue = row.get(field.getCode().toUpperCase());
            values.put(field.getCode(), MaintenanceData.fieldText(submissionValue, field.getDataType()));
        }
        if ("CATEGORY".equals(model.getModelType()) && row.containsKey("CATEGORY_ID")) {
            values.put("CATEGORY_ID", submissionValue(row.get("CATEGORY_ID")));
        }
        return values;
    }

    private String submissionSerializePayload(Map<String, String> values,
            List<MasterChangeSubmodelSaveReqVO> submodelChanges, boolean create,
            MasterModelRespDTO model, Long recordId) {
        if (!create && CollectionUtils.isEmpty(submodelChanges)) return submissionSerialize(values);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("values", values);
        if (!CollectionUtils.isEmpty(submodelChanges)) {
            payload.put("submodelChanges", submodelService.freezeChanges(model, recordId, submodelChanges));
        }
        return submissionSerializeObject(payload);
    }

    private String submissionSerialize(Map<String, String> values) {
        return submissionSerializeObject(values == null ? Collections.emptyMap() : values);
    }

    private String submissionSerializeObject(Object submissionValue) {
        try {
            return JSON.writeValueAsString(submissionValue);
        } catch (JsonProcessingException ex) {
            throw new ServiceException("申请数据无法序列化");
        }
    }

    private String submissionValidationMessage(MasterChangeValidateRespVO validation) {
        if (!validation.getErrors().isEmpty()) {
            return "申请校验未通过：" + validation.getErrors().values().iterator().next();
        }
        return "申请校验未通过";
    }

    private String submissionValidationMessage(MasterChangeBatchValidateRespVO validation) {
        if (!validation.getErrors().isEmpty()) {
            return "批量申请校验未通过：" + validation.getErrors().values().iterator().next();
        }
        for (MasterChangeBatchValidateRespVO.RowResult row : validation.getRows()) {
            if (!row.getErrors().isEmpty()) {
                return "批量申请第" + row.getRowNo() + "条校验未通过："
                        + row.getErrors().values().iterator().next();
            }
        }
        return "批量申请校验未通过";
    }

    private String submissionRequestNo() {
        return "MCR-" + LocalDateTime.now().format(SUBMISSION_REQUEST_TIME) + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String submissionSingleLabel(MasterChangeRequestSaveReqVO request) {
        return MasterLifecycleRules.isAction(request.getMode()) ? "状态变更申请"
                : ("CREATE".equals(request.getMode()) ? "新增"
                : "UPDATE".equals(request.getMode()) ? "修改" : request.getMode()) + "主数据申请";
    }

    private String submissionBatchLabel(MasterChangeRequestBatchSaveReqVO request) {
        return "BATCH_STATUS".equals(request.getMode()) ? "批量状态申请"
                : "BATCH_IMPORT".equals(request.getMode()) ? "批量导入申请" : "批量修改申请";
    }

    private Long submissionParseId(String submissionValue) {
        try {
            long id = Long.parseLong(submissionTrim(submissionValue));
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void submissionPositiveId(Long submissionValue, String message) {
        if (submissionValue == null || submissionValue <= 0) throw new ServiceException(message);
    }

    private String submissionTrim(String submissionValue) {
        return submissionValue == null ? "" : submissionValue.trim();
    }

    private String submissionSafe(String submissionValue) {
        return submissionValue == null || submissionValue.trim().isEmpty() ? "主数据" : submissionValue.trim();
    }

    private String submissionLimit(String submissionValue, int max) {
        if (submissionValue == null) return null;
        return submissionValue.length() <= max ? submissionValue : submissionValue.substring(0, max);
    }

    private String submissionValue(Object input) {
        return input == null ? null : String.valueOf(input);
    }

    private static final class SubmissionItem {
        private final Long recordId;
        private final String changeType;
        private final String beforeData;
        private final String proposedData;
        private final String beforeStatus;
        private final String targetStatus;
        private final String currentVersionNo;

        private SubmissionItem(Long recordId, String changeType, String beforeData,
                String proposedData, String beforeStatus, String targetStatus, String currentVersionNo) {
            this.recordId = recordId;
            this.changeType = changeType;
            this.beforeData = beforeData;
            this.proposedData = proposedData;
            this.beforeStatus = beforeStatus;
            this.targetStatus = targetStatus;
            this.currentVersionNo = currentVersionNo;
        }
    }

    private static final int IMPORT_MAX_ROWS = 500;
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Pattern IMPORT_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");


    /**
     * 解析导入文件，不写入业务表。
     *
     * @param instanceId 模型物化实例 ID
     * @param file 待解析的 CSV 或 XLSX 文件
     * @return 解析后的表头、数据行及文件格式问题
     */
    @Override
    public MasterChangeImportParseRespVO parseImportFile(Long instanceId, MultipartFile file) {
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ServiceException("导入文件不能为空且不能超过5 MiB");
        }
        String name = file.getOriginalFilename();
        String suffix = name == null || !name.contains(".") ? ""
                : name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!"csv".equals(suffix) && !"xlsx".equals(suffix)) {
            throw new ServiceException("仅支持 CSV 或 XLSX 导入文件");
        }
        try (InputStream input = file.getInputStream()) {
            return "csv".equals(suffix) ? parseCsv(model, input) : parseXlsx(model, input);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("导入文件无法解析，请检查编码、表头和文件格式");
        }
    }

    /**
     * 按当前模型字段生成导入模板。
     *
     * @param instanceId 模型物化实例 ID
     * @param response 用于写入导出文件的 HTTP 响应
     */
    @Override
    public void writeImportTemplate(Long instanceId, HttpServletResponse response) throws IOException {
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        List<String> codes = templateHeaders(model);
        Map<String, ReferenceValues> choices = references.load(model, codes);
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("导入数据");
            Row header = sheet.createRow(0);
            CellStyle textStyle = book.createCellStyle();
            textStyle.setDataFormat(book.createDataFormat().getFormat("@"));
            for (int i = 0; i < codes.size(); i++) {
                header.createCell(i).setCellValue(codes.get(i));
                sheet.setDefaultColumnStyle(i, textStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            List<MasterColumnRespVO> templateColumns = new ArrayList<>();
            for (String code : codes) {
                MasterColumnRespVO column = new MasterColumnRespVO();
                column.setProp(code);
                templateColumns.add(column);
            }
            MasterEditableWorkbook.attach(book, sheet, model, templateColumns, choices, IMPORT_MAX_ROWS);
            Sheet guide = book.createSheet("字段填写说明");
            String[] guideHeaders = {"字段编码", "字段名称", "数据类型", "必填", "唯一", "长度", "精度", "默认值", "填写说明"};
            Row guideHeader = guide.createRow(0);
            for (int i = 0; i < guideHeaders.length; i++) {
                guideHeader.createCell(i).setCellValue(guideHeaders[i]);
                guide.setColumnWidth(i, i == 8 ? 48 * 256 : 18 * 256);
            }
            int guideRowNo = 1;
            for (MasterFieldRespDTO field : importableFields(model)) {
                Row row = guide.createRow(guideRowNo++);
                row.createCell(0).setCellValue(safeCellText(field.getCode()));
                row.createCell(1).setCellValue(safeCellText(field.getName()));
                row.createCell(2).setCellValue(safeCellText(field.getDataType()));
                row.createCell(3).setCellValue(Boolean.TRUE.equals(field.getRequired()) ? "是" : "否");
                row.createCell(4).setCellValue(Boolean.TRUE.equals(field.getUnique()) ? "是" : "否");
                row.createCell(5).setCellValue(field.getDataLength() == null ? "" : String.valueOf(field.getDataLength()));
                row.createCell(6).setCellValue(field.getDataScale() == null ? "" : String.valueOf(field.getDataScale()));
                row.createCell(7).setCellValue(safeCellText(field.getDefaultValue()));
                row.createCell(8).setCellValue(importFieldGuide(field)
                        + (choices.containsKey(field.getCode()) ? "；请从下拉列表选择名称，系统自动转换为编码/ID；请保留文件的隐藏绑定表" : ""));
            }
            if ("CATEGORY".equals(model.getModelType())) {
                Row row = guide.createRow(guideRowNo);
                row.createCell(0).setCellValue("CATEGORY_ID");
                row.createCell(1).setCellValue("上级类目ID");
                row.createCell(2).setCellValue("BIGINT");
                row.createCell(3).setCellValue("否");
                row.createCell(4).setCellValue("否");
                row.createCell(8).setCellValue("填写有效上级类目ID；留空表示根节点");
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=authority-import-template.xlsx");
            book.write(response.getOutputStream());
        }
    }

    private String safeCellText(String value) {
        return value == null ? "" : value;
    }

    private String importFieldGuide(MasterFieldRespDTO field) {
        List<String> rules = new ArrayList<>();
        if (Boolean.TRUE.equals(field.getUnique())) rules.add("唯一业务键，可选择用于匹配现有数据；选为匹配业务键时必填，非空值不可重复");
        String type = field.getDataType() == null ? "" : field.getDataType().toUpperCase(Locale.ROOT);
        if ("DATE".equals(type)) rules.add("格式：yyyy-MM-dd；支持年月日以 / 或 . 分隔，上传后自动转换");
        else if ("DATETIME".equals(type) || "TIMESTAMP".equals(type)) {
            rules.add("格式：yyyy-MM-dd HH:mm:ss；支持年月日以 / 或 . 分隔；只填日期时补为当天00:00:00，省略秒时补00秒");
        }
        else if ("BOOLEAN".equals(type)) rules.add("填写 true 或 false");
        else if (Arrays.asList("INTEGER", "INT", "BIGINT", "LONG").contains(type)) rules.add("填写整数；超过15位时必须使用文本单元格");
        else if (Arrays.asList("DECIMAL", "NUMBER", "DOUBLE", "FLOAT").contains(type)) rules.add("填写数字，不含千位分隔符");
        rules.add("表头字段为空表示清空；更新时未包含在表头的字段保持原值；禁止公式");
        if (field.getComment() != null && !field.getComment().isEmpty()) rules.add(field.getComment());
        return String.join("；", rules);
    }

    private MasterChangeImportParseRespVO parseCsv(MasterModelRespDTO model, InputStream input)
            throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT));
                CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).build())) {
            Iterator<CSVRecord> records = parser.iterator();
            if (!records.hasNext()) throw new ServiceException("导入文件缺少表头");
            CSVRecord header = records.next();
            List<String> names = new ArrayList<>();
            for (String value : header) names.add(value);
            validateHeaders(model, names);
            Map<String, ReferenceValues> choices = references.load(model, names);
            MasterChangeImportParseRespVO result = new MasterChangeImportParseRespVO();
            result.setHeaders(names);
            while (records.hasNext()) {
                CSVRecord record = records.next();
                if (record.size() == 1 && record.get(0).trim().isEmpty()) continue;
                if (record.size() != names.size()) {
                    throw new ServiceException("第" + record.getRecordNumber() + "行字段数与表头不一致");
                }
                List<String> values = new ArrayList<>();
                for (String value : record) values.add(value);
                appendRow(result, model, names, values, (int) record.getRecordNumber(), choices, null);
            }
            result.setTotal(result.getRows().size());
            return result;
        }
    }

    private MasterChangeImportParseRespVO parseXlsx(MasterModelRespDTO model, InputStream input)
            throws Exception {
        try (Workbook book = WorkbookFactory.create(input)) {
            if (!(book instanceof XSSFWorkbook)) throw new ServiceException("仅支持 XLSX 工作簿");
            Sheet sheet = book.getNumberOfSheets() == 0 ? null : book.getSheetAt(0);
            if (sheet == null || sheet.getRow(0) == null) throw new ServiceException("导入文件缺少表头");
            Row header = sheet.getRow(0);
            int width = header.getLastCellNum();
            if (width <= 0) throw new ServiceException("导入文件缺少表头");
            DataFormatter formatter = new DataFormatter();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < width; i++) names.add(cellText(header.getCell(i), formatter, 1));
            MasterEditableWorkbook.Binding binding = MasterEditableWorkbook.read(book, model);
            if (binding == null) validateHeaders(model, names);
            else {
                if (names.size() > MasterEditableWorkbook.MAX_COLUMNS) throw new ServiceException("导出表头数量无效");
                Set<String> seen = new HashSet<>();
                for (int i = 0; i < names.size(); i++) {
                    String code = binding.header(names.get(i));
                    if (!seen.add(code)) throw new ServiceException("导出表头重复：" + names.get(i));
                    names.set(i, code);
                }
            }
            Set<String> importable = new HashSet<>(templateHeaders(model));
            List<String> editableHeaders = names.stream().filter(importable::contains)
                    .collect(java.util.stream.Collectors.toList());
            if (editableHeaders.isEmpty()) throw new ServiceException("导入文件至少包含一个可维护业务字段");
            Map<String, ReferenceValues> choices = references.load(model, editableHeaders);
            MasterChangeImportParseRespVO result = new MasterChangeImportParseRespVO();
            result.setHeaders(editableHeaders);
            Map<String, String> fieldTypes = new HashMap<>();
            for (MasterFieldRespDTO field : model.getFields()) {
                fieldTypes.put(field.getCode(), field.getDataType());
            }
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                List<String> values = new ArrayList<>();
                for (int i = 0; i < names.size(); i++) {
                    values.add(cellText(row.getCell(i), formatter, index + 1, fieldTypes.get(names.get(i))));
                }
                for (int i = names.size(); i < row.getLastCellNum(); i++) {
                    if (!cellText(row.getCell(i), formatter, index + 1).trim().isEmpty()) {
                        throw new ServiceException("第" + (index + 1) + "行包含表头之外的数据列");
                    }
                }
                appendRow(result, model, names, values, index + 1, choices, binding);
            }
            result.setTotal(result.getRows().size());
            return result;
        }
    }

    private String cellText(Cell cell, DataFormatter formatter, int rowNo) {
        return cellText(cell, formatter, rowNo, null);
    }

    private String cellText(Cell cell, DataFormatter formatter, int rowNo, String dataType) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.FORMULA) {
            throw new ServiceException("第" + rowNo + "行包含公式，请改为文本值");
        }
        if (cell.getCellType() == CellType.NUMERIC && Math.abs(cell.getNumericCellValue()) >= 1e15) {
            throw new ServiceException("第" + rowNo + "行包含超过15位的数字，请将该列设置为文本以避免精度损失");
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            if ("DATE".equalsIgnoreCase(dataType)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            if ("DATETIME".equalsIgnoreCase(dataType) || "TIMESTAMP".equalsIgnoreCase(dataType)) {
                return cell.getLocalDateTimeCellValue().format(BATCH_DATE_TIME);
            }
        }
        return formatter.formatCellValue(cell);
    }

    private void appendRow(MasterChangeImportParseRespVO result, MasterModelRespDTO model,
            List<String> names, List<String> values, int rowNo, Map<String, ReferenceValues> choices,
            MasterEditableWorkbook.Binding binding) {
        if (values.stream().allMatch(value -> value == null || value.trim().isEmpty())) return;
        if (result.getRows().size() >= IMPORT_MAX_ROWS) throw new ServiceException("单批最多导入500行数据");
        MasterChangeRequestBatchSaveReqVO.ImportRow row = new MasterChangeRequestBatchSaveReqVO.ImportRow();
        row.setRowNo(rowNo);
        Map<String, String> fields = new LinkedHashMap<>();
        Set<String> editable = new HashSet<>(result.getHeaders());
        Map<String, String> fieldTypes = new HashMap<>();
        for (MasterFieldRespDTO field : model.getFields()) {
            fieldTypes.put(field.getCode(), field.getDataType());
        }
        for (int i = 0; i < names.size(); i++) {
            String code = names.get(i);
            if (binding != null && "ID".equals(code)) {
                row.setRecordId(values.get(i).trim());
            } else if ("CATEGORY_ID".equals(code) && "CATEGORY".equals(model.getModelType())) {
                row.setParentId(values.get(i).trim());
            } else if (editable.contains(code)) {
                String value = values.get(i);
                ReferenceValues fieldChoices = choices.get(code);
                try {
                    if (fieldChoices != null) value = fieldChoices.resolve(value, binding == null ? null : binding.options(code));
                } catch (ServiceException ex) {
                    throw new ServiceException("第" + rowNo + "行 " + code + "：" + ex.getMessage());
                }
                fields.put(code, MaintenanceData.normalizeImportDate(fieldTypes.get(code), value));
            }
        }
        row.setValues(fields);
        result.getRows().add(row);
    }

    private void validateHeaders(MasterModelRespDTO model, List<String> names) {
        if (names.isEmpty() || names.size() > 201) throw new ServiceException("导入表头数量无效");
        Set<String> allowed = new HashSet<>(templateHeaders(model));
        Map<String, List<String>> nameAliases = new LinkedHashMap<>();
        for (MasterFieldRespDTO field : importableFields(model)) {
            String fieldName = field.getName() == null ? "" : field.getName().trim();
            if (!fieldName.isEmpty()) {
                nameAliases.computeIfAbsent(fieldName, key -> new ArrayList<>()).add(field.getCode());
            }
        }
        Set<String> seen = new HashSet<>();
        boolean business = false;
        for (int i = 0; i < names.size(); i++) {
            String originalName = names.get(i).replace("\uFEFF", "").trim();
            String canonicalName = resolveHeader(model, allowed, nameAliases, originalName);
            names.set(i, canonicalName);
            if (!seen.add(canonicalName)) throw new ServiceException("导入表头重复：" + originalName);
            if (!"CATEGORY_ID".equals(canonicalName)) business = true;
        }
        if (!business) throw new ServiceException("导入文件至少包含一个业务字段");
    }

    private String resolveHeader(MasterModelRespDTO model, Set<String> allowed,
            Map<String, List<String>> nameAliases, String originalName) {
        if (allowed.contains(originalName)) return originalName;
        if ("CATEGORY".equals(model.getModelType())
                && ("上级类目ID".equalsIgnoreCase(originalName)
                || "上级分类ID".equalsIgnoreCase(originalName))) {
            return "CATEGORY_ID";
        }
        List<String> matches = nameAliases.get(originalName);
        if (matches == null || matches.isEmpty()) {
            throw new ServiceException("导入表头不是当前模型字段：" + originalName);
        }
        if (matches.size() > 1) {
            throw new ServiceException("导入表头对应多个模型字段：" + originalName + "，请改用字段编码");
        }
        return matches.get(0);
    }

    private List<String> templateHeaders(MasterModelRespDTO model) {
        List<String> codes = new ArrayList<>();
        for (MasterFieldRespDTO field : importableFields(model)) codes.add(field.getCode());
        if (codes.isEmpty() || codes.size() > 200) throw new ServiceException("当前模型没有可导入的业务字段");
        if ("CATEGORY".equals(model.getModelType())) codes.add("CATEGORY_ID");
        return codes;
    }

    private List<MasterFieldRespDTO> importableFields(MasterModelRespDTO model) {
        List<MasterFieldRespDTO> fields = new ArrayList<>(model.getFields());
        fields.sort(Comparator.comparing((MasterFieldRespDTO field) ->
                field.getSortOrder() == null ? 0 : field.getSortOrder())
                .thenComparing(field -> field.getId() == null ? Long.MAX_VALUE : field.getId()));
        List<MasterFieldRespDTO> importable = new ArrayList<>();
        Set<String> automaticFields = maintenanceValidator.automaticFieldCodes(model);
        for (MasterFieldRespDTO field : fields) {
            if (!"SYSTEM".equalsIgnoreCase(field.getCategory()) && field.getCode() != null
                    && IMPORT_IDENTIFIER.matcher(field.getCode()).matches() && !reserved(field.getCode())
                    && !automaticFields.contains(field.getCode().toUpperCase(Locale.ROOT))) {
                importable.add(field);
            }
        }
        return importable;
    }

    private boolean reserved(String code) {
        return "ID".equalsIgnoreCase(code) || "STATUS".equalsIgnoreCase(code)
                || "CATEGORY_ID".equalsIgnoreCase(code)
                || "PARENT_ID".equalsIgnoreCase(code) || "LEVEL".equalsIgnoreCase(code)
                || "SORT_ORDER".equalsIgnoreCase(code) || "DATA_SOURCE".equalsIgnoreCase(code)
                || "CREATED_TIME".equalsIgnoreCase(code) || "UPDATED_TIME".equalsIgnoreCase(code)
                || "CREATE_TIME".equalsIgnoreCase(code) || "UPDATE_TIME".equalsIgnoreCase(code);
    }

}
