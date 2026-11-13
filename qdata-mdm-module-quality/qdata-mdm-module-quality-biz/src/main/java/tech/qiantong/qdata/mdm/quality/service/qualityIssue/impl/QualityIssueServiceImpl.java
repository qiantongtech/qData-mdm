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

package tech.qiantong.qdata.mdm.quality.service.qualityIssue.impl;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssuePageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssue.QualityIssueMapper;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueInfo.QualityIssueInfoMapper;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTask.QualityTaskMapper;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTaskInstance.QualityTaskInstanceMapper;
import tech.qiantong.qdata.mdm.quality.service.qualityIssue.IQualityIssueService;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.RuleQualityMapper;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 问题数据Service业务层处理
 *
 * @author qdata
 * @date 2026-09-23
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityIssueServiceImpl extends ServiceImpl<QualityIssueMapper, QualityIssueDO> implements IQualityIssueService {
    private static final int LIST_BATCH_SIZE = 500;

    @Resource
    private QualityIssueMapper qualityIssueMapper;
    @Resource
    private QualityIssueInfoMapper qualityIssueInfoMapper;
    @Resource
    private RuleQualityMapper ruleQualityMapper;

    @Resource
    private ModelColumnMapper modelColumnMapper;
    @Resource
    private QualityTaskInstanceMapper qualityTaskInstanceMapper;
    @Resource
    private QualityTaskMapper qualityTaskMapper;

    @Override
    public PageResult<QualityIssueDO> getQualityIssuePage(QualityIssuePageReqVO pageReqVO) {
        PageResult<QualityIssueDO> pageResult = qualityIssueMapper.selectPage(pageReqVO);
        fillIssueRelations(pageResult.getRows());
        return pageResult;
    }

    @Override
    public Map<String, Long> getQualityIssueStatusCount(QualityIssuePageReqVO pageReqVO) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", qualityIssueMapper.selectIssueCount(pageReqVO, null));
        counts.put("1", qualityIssueMapper.selectIssueCount(pageReqVO, "1"));
        counts.put("2", qualityIssueMapper.selectIssueCount(pageReqVO, "2"));
        counts.put("3", qualityIssueMapper.selectIssueCount(pageReqVO, "3"));
        return counts;
    }

    /** 列表、详情和运行结果共用关联装配，按批次读取，避免逐条查询。 */
    private void fillIssueRelations(List<QualityIssueDO> issues) {
        if (issues == null || issues.isEmpty()) return;

        List<Long> issueIds = issues.stream().map(QualityIssueDO::getId).collect(Collectors.toList());
        List<QualityIssueInfoDO> details = new ArrayList<>();
        for (int start = 0; start < issueIds.size(); start += LIST_BATCH_SIZE) {
            List<Long> batch = issueIds.subList(start, Math.min(start + LIST_BATCH_SIZE, issueIds.size()));
            details.addAll(qualityIssueInfoMapper.selectList(new LambdaQueryWrapper<QualityIssueInfoDO>()
                    .in(QualityIssueInfoDO::getIssueId, batch)));
        }
        Map<Long, RuleQualityDO> rules = selectRelationMap(details.stream()
                .map(QualityIssueInfoDO::getQualityRuleId).collect(Collectors.toList()), ruleQualityMapper, RuleQualityDO::getId);
        Map<Long, ModelColumnDO> columns = selectRelationMap(details.stream()
                .map(QualityIssueInfoDO::getModelColumnId).collect(Collectors.toList()), modelColumnMapper, ModelColumnDO::getId);
        Map<Long, QualityTaskInstanceDO> instances = selectRelationMap(issues.stream()
                .map(QualityIssueDO::getQualityTaskInstanceId).collect(Collectors.toList()),
                qualityTaskInstanceMapper, QualityTaskInstanceDO::getId);
        Map<Long, QualityTaskDO> tasks = selectRelationMap(instances.values().stream()
                .map(QualityTaskInstanceDO::getQualityTaskId).collect(Collectors.toList()), qualityTaskMapper, QualityTaskDO::getId);

        for (QualityIssueInfoDO detail : details) {
            RuleQualityDO rule = rules.get(detail.getQualityRuleId());
            if (rule != null) {
                detail.setQualityRuleName(rule.getRuleName());
                detail.setQualityRuleType(rule.getRuleType());
            }
            ModelColumnDO column = columns.get(detail.getModelColumnId());
            if (column != null) detail.setModelColumnName(column.getName());
        }
        Map<Long, List<QualityIssueInfoDO>> detailsByIssue = details.stream()
                .collect(Collectors.groupingBy(QualityIssueInfoDO::getIssueId));
        for (QualityIssueDO issue : issues) {
            issue.setQualityIssueInfoList(detailsByIssue.getOrDefault(issue.getId(), Collections.emptyList()));
            QualityTaskInstanceDO instance = instances.get(issue.getQualityTaskInstanceId());
            if (instance == null) continue;
            issue.setModelVersion(instance.getModelVersion());
            QualityTaskDO task = tasks.get(instance.getQualityTaskId());
            if (task != null) issue.setQualityTaskName(task.getTaskName());
        }
    }

    private <T> Map<Long, T> selectRelationMap(Collection<Long> ids, BaseMapperX<T> mapper, Function<T, Long> idGetter) {
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, T> result = new LinkedHashMap<>();
        for (int start = 0; start < distinctIds.size(); start += LIST_BATCH_SIZE) {
            List<Long> batch = distinctIds.subList(start, Math.min(start + LIST_BATCH_SIZE, distinctIds.size()));
            mapper.selectBatchIds(batch).forEach(row -> result.put(idGetter.apply(row), row));
        }
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void saveDetectedIssue(QualityIssueDO detected, QualityTaskInstanceDO instance, Long userId, String userName) {
        if (detected == null || !Objects.equals(detected.getModelId(), instance.getModelId())
                || detected.getMasterRecordId() == null || detected.getQualityIssueInfoList() == null
                || detected.getQualityIssueInfoList().isEmpty()) {
            throw new IllegalStateException("质量检测引擎返回的问题明细不完整");
        }
        List<QualityIssueDO> existing = qualityIssueMapper.selectList(new LambdaQueryWrapper<QualityIssueDO>()
                .eq(QualityIssueDO::getModelId, detected.getModelId())
                .eq(QualityIssueDO::getMasterRecordId, detected.getMasterRecordId())
                .in(QualityIssueDO::getStatus, "1", "2").orderByDesc(QualityIssueDO::getId));
        Date found = new Date();
        QualityIssueDO issue = existing.isEmpty() ? new QualityIssueDO() : existing.get(0);
        boolean creating = issue.getId() == null;
        if (creating) {
            issue.setIssueCode("QI" + UUID.randomUUID().toString().replace("-", ""));
            issue.setModelId(detected.getModelId());
            issue.setMasterRecordId(detected.getMasterRecordId());
            issue.setStatus("1");
            issue.setRectificationStatus("未下发");
            issue.setFirstFoundTime(found);
            issue.setValidFlag(true);
            issue.setDelFlag(false);
        }
        issue.setLastFoundTime(found);
        issue.setQualityTaskInstanceId(instance.getId());
        setAudit(issue, creating, userId, userName, found);
        if (creating) qualityIssueMapper.insert(issue);
        else qualityIssueMapper.updateById(issue);

        List<QualityIssueInfoDO> details = qualityIssueInfoMapper.selectList(new LambdaQueryWrapper<QualityIssueInfoDO>()
                .eq(QualityIssueInfoDO::getIssueId, issue.getId()));
        for (QualityIssueInfoDO failure : detected.getQualityIssueInfoList()) {
            if (failure.getQualityRuleId() == null) throw new IllegalStateException("问题明细缺少质量规则");
            QualityIssueInfoDO detail = details.stream()
                    .filter(item -> Objects.equals(item.getQualityRuleId(), failure.getQualityRuleId())
                            && Objects.equals(item.getModelColumnId(), failure.getModelColumnId()))
                    .findFirst().orElseGet(QualityIssueInfoDO::new);
            boolean newDetail = detail.getId() == null;
            detail.setIssueId(issue.getId());
            detail.setQualityRuleId(failure.getQualityRuleId());
            detail.setModelColumnId(failure.getModelColumnId());
            detail.setLastCheckInstanceId(instance.getId());
            detail.setFailReason(failure.getFailReason());
            detail.setLastFoundTime(found);
            detail.setStatus("1");
            detail.setValidFlag(true);
            detail.setDelFlag(false);
            setAudit(detail, newDetail, userId, userName, found);
            if (newDetail) {
                qualityIssueInfoMapper.insert(detail);
                details.add(detail);
            } else qualityIssueInfoMapper.updateById(detail);
        }
    }

    private static void setAudit(BaseEntity entity, boolean creating, Long userId, String userName, Date time) {
        if (creating) {
            entity.setCreatorId(userId);
            entity.setCreateBy(userName);
            entity.setCreateTime(time);
        }
        entity.setUpdatorId(userId);
        entity.setUpdateBy(userName);
        entity.setUpdateTime(time);
    }

    @Override
    public Long createQualityIssue(QualityIssueSaveReqVO createReqVO) {
        QualityIssueDO qualityIssue = BeanUtils.toBean(createReqVO, QualityIssueDO.class);
        qualityIssueMapper.insert(qualityIssue);
        return qualityIssue.getId();
    }

    @Override
    public int updateQualityIssue(QualityIssueSaveReqVO updateReqVO) {
        QualityIssueDO updateObj = BeanUtils.toBean(updateReqVO, QualityIssueDO.class);
        return qualityIssueMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityIssue(Collection<Long> idList) {
        // 批量删除问题数据
        return qualityIssueMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityIssueDO getQualityIssueById(Long id) {
        QualityIssueDO issue = qualityIssueMapper.selectById(id);
        if (issue != null) fillIssueRelations(Collections.singletonList(issue));
        return issue;
    }

    @Override
    public List<QualityIssueDO> getQualityIssueList() {
        return qualityIssueMapper.selectList();
    }

    @Override
    public Map<Long, QualityIssueDO> getQualityIssueMap() {
        List<QualityIssueDO> qualityIssueList = qualityIssueMapper.selectList();
        return qualityIssueList.stream()
                .collect(Collectors.toMap(
                        QualityIssueDO::getId,
                        qualityIssueDO -> qualityIssueDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入问题数据数据
         *
         * @param importExcelList 问题数据数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityIssue(List<QualityIssueRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityIssueRespVO respVO : importExcelList) {
                try {
                    QualityIssueDO qualityIssueDO = BeanUtils.toBean(respVO, QualityIssueDO.class);
                    Long qualityIssueId = respVO.getId();
                    if (isUpdateSupport) {
                        if (qualityIssueId != null) {
                            QualityIssueDO existingQualityIssue = qualityIssueMapper.selectById(qualityIssueId);
                            if (existingQualityIssue != null) {
                                qualityIssueMapper.updateById(qualityIssueDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + qualityIssueId + " 的问题数据记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + qualityIssueId + " 的问题数据记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityIssueDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", qualityIssueId);
                        QualityIssueDO existingQualityIssue = qualityIssueMapper.selectOne(queryWrapper);
                        if (existingQualityIssue == null) {
                            qualityIssueMapper.insert(qualityIssueDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + qualityIssueId + " 的问题数据记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + qualityIssueId + " 的问题数据记录已存在。");
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

    @Override
    public List<QualityIssueDO> getQualityIssueListByInstanceId(Long qualityTaskInstanceId) {
        if (qualityTaskInstanceId == null) return Collections.emptyList();
        List<QualityIssueDO> issues = qualityIssueMapper.selectList(new LambdaQueryWrapper<QualityIssueDO>()
                .eq(QualityIssueDO::getQualityTaskInstanceId, qualityTaskInstanceId));
        fillIssueRelations(issues);
        return issues;
    }
}
