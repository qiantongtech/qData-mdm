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

package tech.qiantong.qdata.mdm.rule.service.ruleQuality.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityMergeLogDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityMergePendingDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityTaskCountDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.QualityMergePendingMapper;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.QualityMergeLogMapper;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.QualityTaskCountMapper;

import com.aliyun.oss.ServiceException;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualitySaveReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.RuleQualityMapper;
import tech.qiantong.qdata.mdm.rule.service.ruleQuality.IRuleQualityService;
/**
 * 质量规则Service业务层处理
 *
 * @author qdata
 * @date 2026-09-18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RuleQualityServiceImpl  extends ServiceImpl<RuleQualityMapper,RuleQualityDO> implements IRuleQualityService {
    @Resource
    private RuleQualityMapper ruleQualityMapper;

    @Resource
    private ISysUserService sysUserService;

    private void validateResponsibleUser(Long userId) {
        if (userId != null && sysUserService.selectUserById(userId) == null) {
            throw new ServiceException("所选负责人不存在，请重新选择");
        }
    }

    @Resource
    private QualityTaskCountMapper qualityTaskCountMapper;

    @Resource
    private QualityMergeLogMapper qualityMergeLogMapper;

    @Resource
    private QualityMergePendingMapper qualityMergePendingMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public PageResult<RuleQualityDO> getRuleQualityPage(RuleQualityPageReqVO pageReqVO) {
        PageResult<RuleQualityDO> page = ruleQualityMapper.selectPage(pageReqVO);
        if (page.getRows().isEmpty()) return page;
        List<Long> displayRuleIds = page.getRows().stream().map(RuleQualityDO::getId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, RuleQualityDO> displayInfo = new HashMap<>();
        for (int start = 0; start < displayRuleIds.size(); start += 500) {
            List<Long> batch = displayRuleIds.subList(start, Math.min(start + 500, displayRuleIds.size()));
            for (RuleQualityDO info : ruleQualityMapper.selectDisplayInfo(batch)) {
                displayInfo.put(info.getId(), info);
            }
        }
        for (RuleQualityDO row : page.getRows()) {
            RuleQualityDO info = displayInfo.get(row.getId());
            if (info != null) {
                row.setModelName(info.getModelName());
                row.setModelCode(info.getModelCode());
                row.setModelType(info.getModelType());
                row.setModelIcon(info.getModelIcon());
                row.setModelDescription(info.getModelDescription());
                row.setProjectName(info.getProjectName());
                row.setLayerName(info.getLayerName());
                row.setResponsibleUserName(info.getResponsibleUserName());
                row.setResponsibleUserPhone(info.getResponsibleUserPhone());
            }
        }
        List<Long> modelIds = page.getRows().stream().map(RuleQualityDO::getModelId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (modelIds.isEmpty()) return page;
        Set<Long> ruleIds = page.getRows().stream().map(RuleQualityDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> taskCounts = new HashMap<>();
        Map<Long, Set<Long>> mergedRecords = new HashMap<>();
        boolean snapshotsValid = true;
        for (int start = 0; start < modelIds.size(); start += 500) {
            List<Long> batch = modelIds.subList(start, Math.min(start + 500, modelIds.size()));
            for (QualityTaskCountDO task : qualityTaskCountMapper.selectByModels(batch)) {
                taskCounts.merge(task.getModelId(), 1L, Long::sum);
            }
            for (QualityMergeLogDO logItem : qualityMergeLogMapper.selectSnapshotsByModels(batch)) {
                if (logItem.getRuleSnapshot() == null || logItem.getRuleSnapshot().trim().isEmpty()) {
                    snapshotsValid = false;
                    continue;
                }
                try {
                    for (Long ruleId : parseSnapshotRuleIds(logItem.getRuleSnapshot())) {
                        if (ruleIds.contains(ruleId) && logItem.getPendingMasterId() != null) {
                            mergedRecords.computeIfAbsent(ruleId, key -> new HashSet<>()).add(logItem.getPendingMasterId());
                        }
                    }
                } catch (Exception ex) {
                    snapshotsValid = false;
                    log.warn("质量规则关联统计：合并记录 {} 的规则快照无法解析", logItem.getPendingMasterId(), ex);
                }
            }
        }
        for (RuleQualityDO rule : page.getRows()) {
            if (rule.getModelId() == null) continue;
            rule.setQualityTaskCount(taskCounts.getOrDefault(rule.getModelId(), 0L));
            if (snapshotsValid) rule.setMergeSnapshotCount((long) mergedRecords.getOrDefault(rule.getId(), Collections.emptySet()).size());
        }
        return page;
    }

    private Set<Long> parseSnapshotRuleIds(String text) throws java.io.IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new java.io.IOException("质量规则快照为空");
        }
        JsonNode snapshot = objectMapper.readTree(text);
        if (snapshot == null || !snapshot.isArray()) {
            throw new java.io.IOException("质量规则快照格式不正确");
        }
        Set<Long> ids = new HashSet<>();
        for (JsonNode item : snapshot) {
            JsonNode id = item.path("id");
            if (id.isIntegralNumber()) ids.add(id.longValue());
            else if (id.isTextual() && id.asText().matches("\\d+")) ids.add(Long.valueOf(id.asText()));
        }
        return ids;
    }

    @Override
    public PageResult<QualityMergePendingDO> getMergeRecords(Long ruleId, PageParam page) {
        RuleQualityDO rule = ruleQualityMapper.selectById(ruleId);
        if (rule == null) throw new ServiceException("质量规则不存在");
        if (rule.getModelId() == null) return PageResult.empty();
        Set<Long> pendingIds = new HashSet<>();
        PageParam scan = new PageParam();
        scan.setPageSize(500);
        int pageNum = 1;
        while (true) {
            scan.setPageNum(pageNum++);
            PageResult<QualityMergeLogDO> logs = qualityMergeLogMapper.selectSnapshotPage(rule.getModelId(), scan);
            for (QualityMergeLogDO entry : logs.getRows()) {
                try {
                    if (parseSnapshotRuleIds(entry.getRuleSnapshot()).contains(ruleId) && entry.getPendingMasterId() != null) {
                        pendingIds.add(entry.getPendingMasterId());
                    }
                } catch (Exception ex) {
                    log.warn("质量规则关联明细：合并记录 {} 的规则快照无法解析", entry.getPendingMasterId(), ex);
                    throw new ServiceException("合并记录的质量规则快照无法解析，请检查数据");
                }
            }
            if (logs.getRows().size() < scan.getPageSize()) break;
        }
        return pendingIds.isEmpty() ? PageResult.empty() : qualityMergePendingMapper.selectAssociatedPage(pendingIds, page);
    }

    @Override
    public Long createRuleQuality(RuleQualitySaveReqVO createReqVO) {
        validateResponsibleUser(createReqVO.getResponsibleUserId());
        RuleQualityDO dictType = BeanUtils.toBean(createReqVO, RuleQualityDO.class);
        ruleQualityMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateRuleQuality(RuleQualitySaveReqVO updateReqVO) {
        // 相关校验

        // 更新质量规则
        RuleQualityDO updateObj = BeanUtils.toBean(updateReqVO, RuleQualityDO.class);
        validateResponsibleUser(updateReqVO.getResponsibleUserId());
        return ruleQualityMapper.update(updateObj, new LambdaUpdateWrapper<RuleQualityDO>()
                .eq(RuleQualityDO::getId, updateObj.getId())
                .set(updateReqVO.isResponsibleUserSpecified() && updateReqVO.getResponsibleUserId() == null,
                        RuleQualityDO::getResponsibleUserId, null));
    }
    @Override
    public int removeRuleQuality(Collection<Long> idList) {
        // 批量删除质量规则
        return ruleQualityMapper.deleteBatchIds(idList);
    }

    @Override
    public RuleQualityDO getRuleQualityById(Long id) {
        RuleQualityDO record = ruleQualityMapper.selectById(id);
        if (record != null) {
            List<RuleQualityDO> info = ruleQualityMapper.selectDisplayInfo(Collections.singletonList(id));
            if (!info.isEmpty()) {
                record.setModelName(info.get(0).getModelName());
                record.setModelCode(info.get(0).getModelCode());
                record.setModelType(info.get(0).getModelType());
                record.setModelIcon(info.get(0).getModelIcon());
                record.setModelDescription(info.get(0).getModelDescription());
                record.setProjectName(info.get(0).getProjectName());
                record.setLayerName(info.get(0).getLayerName());
                record.setResponsibleUserName(info.get(0).getResponsibleUserName());
                record.setResponsibleUserPhone(info.get(0).getResponsibleUserPhone());
            }
        }
        return record;
    }

    @Override
    public List<RuleQualityDO> getRuleQualityList() {
        return ruleQualityMapper.selectList();
    }

    @Override
    public Map<Long, RuleQualityDO> getRuleQualityMap() {
        List<RuleQualityDO> ruleQualityList = ruleQualityMapper.selectList();
        return ruleQualityList.stream()
                .collect(Collectors.toMap(
                        RuleQualityDO::getId,
                        ruleQualityDO -> ruleQualityDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入质量规则数据
         *
         * @param importExcelList 质量规则数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importRuleQuality(List<RuleQualityRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (RuleQualityRespVO respVO : importExcelList) {
                try {
                    RuleQualityDO ruleQualityDO = BeanUtils.toBean(respVO, RuleQualityDO.class);
                    Long ruleQualityId = respVO.getId();
                    if (isUpdateSupport) {
                        if (ruleQualityId != null) {
                            RuleQualityDO existingRuleQuality = ruleQualityMapper.selectById(ruleQualityId);
                            if (existingRuleQuality != null) {
                                ruleQualityMapper.updateById(ruleQualityDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + ruleQualityId + " 的质量规则记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + ruleQualityId + " 的质量规则记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<RuleQualityDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", ruleQualityId);
                        RuleQualityDO existingRuleQuality = ruleQualityMapper.selectOne(queryWrapper);
                        if (existingRuleQuality == null) {
                            ruleQualityMapper.insert(ruleQualityDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + ruleQualityId + " 的质量规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + ruleQualityId + " 的质量规则记录已存在。");
                        }
                    }
                } catch (Exception e) {
                    failureNum++;
                    String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                    failureMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            StringBuilder resultMsg = new StringBuilder();
            if (failureNum > 0) {
                resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
                resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
                throw new ServiceException(resultMsg.toString());
            } else {
                resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
            }
            return resultMsg.toString();
        }
}
