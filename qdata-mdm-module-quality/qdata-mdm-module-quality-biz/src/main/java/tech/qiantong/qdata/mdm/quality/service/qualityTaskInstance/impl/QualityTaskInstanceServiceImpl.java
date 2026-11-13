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

package tech.qiantong.qdata.mdm.quality.service.qualityTaskInstance.impl;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;
import tech.qiantong.qdata.mdm.quality.service.qualityIssue.IQualityIssueService;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTask.QualityTaskMapper;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTaskInstance.QualityTaskInstanceMapper;
import tech.qiantong.qdata.mdm.quality.service.qualityTaskInstance.IQualityTaskInstanceService;
import tech.qiantong.qdata.mdm.quality.service.qualityTaskInstance.QualityTaskExecutor;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
/**
 * 主数据质量任务执行实例Service业务层处理
 *
 * @author qdata
 * @date 2026-09-22
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityTaskInstanceServiceImpl  extends ServiceImpl<QualityTaskInstanceMapper,QualityTaskInstanceDO> implements IQualityTaskInstanceService {
    @Resource
    private QualityTaskInstanceMapper qualityTaskInstanceMapper;
    @Resource
    private QualityTaskMapper qualityTaskMapper;
    @Resource
    private ObjectProvider<QualityTaskExecutor> executorProvider;
    @Resource
    private IQualityIssueService qualityIssueService;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    public QualityTaskInstanceDO executeOnce(Long taskId, Long userId, String userName) {
        QualityTaskDO task = qualityTaskMapper.selectById(taskId);
        if (task == null || !"1".equals(task.getStatus()) || Boolean.FALSE.equals(task.getValidFlag())) {
            throw new ServiceException("任务不存在或已停用，无法执行");
        }
        Date started = new Date();
        // 条件更新在事务内锁定任务，防止同一任务并发启动。
        int claimed = qualityTaskMapper.update(null, new LambdaUpdateWrapper<QualityTaskDO>()
                .eq(QualityTaskDO::getId, taskId).eq(QualityTaskDO::getStatus, "1")
                .and(query -> query.isNull(QualityTaskDO::getLastRunStatus).or().ne(QualityTaskDO::getLastRunStatus, "1"))
                .set(QualityTaskDO::getLastRunStatus, "1").set(QualityTaskDO::getLastRunTime, started));
        if (claimed != 1) {
            throw new ServiceException("任务已停用或正在执行，请稍后重试");
        }
        QualityTaskInstanceDO instance = new QualityTaskInstanceDO();
        instance.setQualityTaskId(taskId);
        instance.setTaskCode(task.getTaskCode());
        instance.setModelId(task.getModelId());
        instance.setModelVersion(task.getModelVersion());
        instance.setExecutionNo("QTI" + UUID.randomUUID().toString().replace("-", ""));
        instance.setTriggerType("1");
        instance.setStartTime(started);
        instance.setValidFlag(true);
        instance.setDelFlag(false);
        instance.setCreatorId(userId);
        instance.setCreateBy(userName);
        instance.setCreateTime(started);
        instance.setStatus("1");
        instance.setTotalCount(0L);
        instance.setPassCount(0L);
        instance.setFailCount(0L);
        qualityTaskInstanceMapper.insert(instance);
        try {
            QualityTaskExecutor executor = executorProvider.getIfAvailable();
            if (executor == null) {
                throw new IllegalStateException("质量检测引擎尚未接入，未检测任何数据。请配置权威主数据读取及质量规则执行服务后重试。");
            }
            // 保留失败的执行记录，但检测或入库失败时撤销本次全部问题变更。
            TransactionTemplate detection = new TransactionTemplate(transactionManager);
            detection.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
            long[] counts = detection.execute(status -> {
                long[] saved = {0L};
                long[] result = executor.execute(task, issue -> {
                    qualityIssueService.saveDetectedIssue(issue, instance, userId, userName);
                    saved[0]++;
                });
                if (result == null || result.length != 3 || result[0] < 0 || result[1] < 0
                        || result[2] < 0 || result[1] > result[0] || result[0] - result[1] != result[2]
                        || saved[0] != result[2]) {
                    throw new IllegalStateException("质量检测引擎返回的统计数据与问题明细不一致");
                }
                return result;
            });
            instance.setTotalCount(counts[0]);
            instance.setPassCount(counts[1]);
            instance.setFailCount(counts[2]);
            instance.setStatus("4");
        } catch (Exception exception) {
            log.error("质量任务执行失败，taskId={}", taskId, exception);
            instance.setStatus("2");
            String message = exception.getMessage() == null ? "质量任务执行失败，请查看服务日志" : exception.getMessage();
            instance.setErrorMessage(message.substring(0, Math.min(message.length(), 512)));
        }
        instance.setEndTime(new Date());
        qualityTaskInstanceMapper.updateById(instance);
        String result = executionResult(instance);
        qualityTaskMapper.update(null, new LambdaUpdateWrapper<QualityTaskDO>()
                .eq(QualityTaskDO::getId, taskId)
                .set(QualityTaskDO::getLastRunStatus, instance.getStatus())
                .set(QualityTaskDO::getLastResult, result.substring(0, Math.min(result.length(), 500))));
        return instance;
    }

    private static String executionResult(QualityTaskInstanceDO instance) {
        if ("2".equals(instance.getStatus())) return "执行失败：" + instance.getErrorMessage();
        if (instance.getFailCount() > 0) return "发现" + instance.getFailCount() + "个问题";
        return instance.getTotalCount() == 0 ? "未检测到数据" : "全部通过";
    }

    @Override
    public PageResult<QualityTaskInstanceDO> getQualityTaskInstancePage(QualityTaskInstancePageReqVO pageReqVO) {
        return qualityTaskInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createQualityTaskInstance(QualityTaskInstanceSaveReqVO createReqVO) {
        QualityTaskInstanceDO dictType = BeanUtils.toBean(createReqVO, QualityTaskInstanceDO.class);
        qualityTaskInstanceMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateQualityTaskInstance(QualityTaskInstanceSaveReqVO updateReqVO) {
        // 相关校验

        // 更新主数据质量任务执行实例
        QualityTaskInstanceDO updateObj = BeanUtils.toBean(updateReqVO, QualityTaskInstanceDO.class);
        return qualityTaskInstanceMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityTaskInstance(Collection<Long> idList) {
        // 批量删除主数据质量任务执行实例
        return qualityTaskInstanceMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityTaskInstanceDO getQualityTaskInstanceById(Long id) {
        return qualityTaskInstanceMapper.selectById(id);
    }

    @Override
    public List<QualityTaskInstanceDO> getQualityTaskInstanceList() {
        return qualityTaskInstanceMapper.selectList();
    }

    @Override
    public Map<Long, QualityTaskInstanceDO> getQualityTaskInstanceMap() {
        List<QualityTaskInstanceDO> qualityTaskInstanceList = qualityTaskInstanceMapper.selectList();
        return qualityTaskInstanceList.stream()
                .collect(Collectors.toMap(
                        QualityTaskInstanceDO::getId,
                        qualityTaskInstanceDO -> qualityTaskInstanceDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入主数据质量任务执行实例数据
         *
         * @param importExcelList 主数据质量任务执行实例数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityTaskInstance(List<QualityTaskInstanceRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityTaskInstanceRespVO respVO : importExcelList) {
                try {
                    QualityTaskInstanceDO qualityTaskInstanceDO = BeanUtils.toBean(respVO, QualityTaskInstanceDO.class);
                    Long qualityTaskInstanceId = respVO.getId();
                    if (isUpdateSupport) {
                        if (qualityTaskInstanceId != null) {
                            QualityTaskInstanceDO existingQualityTaskInstance = qualityTaskInstanceMapper.selectById(qualityTaskInstanceId);
                            if (existingQualityTaskInstance != null) {
                                qualityTaskInstanceMapper.updateById(qualityTaskInstanceDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + qualityTaskInstanceId + " 的主数据质量任务执行实例记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + qualityTaskInstanceId + " 的主数据质量任务执行实例记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityTaskInstanceDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", qualityTaskInstanceId);
                        QualityTaskInstanceDO existingQualityTaskInstance = qualityTaskInstanceMapper.selectOne(queryWrapper);
                        if (existingQualityTaskInstance == null) {
                            qualityTaskInstanceMapper.insert(qualityTaskInstanceDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + qualityTaskInstanceId + " 的主数据质量任务执行实例记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + qualityTaskInstanceId + " 的主数据质量任务执行实例记录已存在。");
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
