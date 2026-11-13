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

package tech.qiantong.qdata.mdm.quality.service.qualityTask.impl;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTask.QualityTaskMapper;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTaskInstance.QualityTaskInstanceMapper;
import tech.qiantong.qdata.mdm.quality.service.qualityTask.IQualityTaskService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 主数据质量任务Service业务层处理
 *
 * @author qdata
 * @date 2026-09-22
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityTaskServiceImpl  extends ServiceImpl<QualityTaskMapper,QualityTaskDO> implements IQualityTaskService {
    @Resource
    private QualityTaskMapper qualityTaskMapper;

    @Resource
    private QualityTaskInstanceMapper qualityTaskInstanceMapper;

    @Override
    public PageResult<QualityTaskDO> getQualityTaskPage(QualityTaskPageReqVO pageReqVO) {
        PageResult<QualityTaskDO> page = qualityTaskMapper.selectPage(pageReqVO);
        fillLastRunDurations(page.getRows());
        return page;
    }

    private void fillLastRunDurations(List<QualityTaskDO> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        List<Long> taskIds = tasks.stream().map(QualityTaskDO::getId).collect(Collectors.toList());
        Map<Long, QualityTaskInstanceDO> latestRuns = qualityTaskInstanceMapper.selectLatestByTaskIds(taskIds)
                .stream().collect(Collectors.toMap(QualityTaskInstanceDO::getQualityTaskId, run -> run));
        for (QualityTaskDO task : tasks) {
            QualityTaskInstanceDO run = latestRuns.get(task.getId());
            // 新一轮执行中不能沿用上一轮的耗时。
            if ("1".equals(task.getLastRunStatus()) || run == null || run.getStartTime() == null
                    || run.getEndTime() == null || task.getLastRunTime() == null
                    || !task.getLastRunTime().equals(run.getStartTime())) continue;
            long duration = run.getEndTime().getTime() - run.getStartTime().getTime();
            if (duration >= 0) task.setLastRunDurationMs(duration);
        }
    }

    @Override
    public Long createQualityTask(QualityTaskSaveReqVO createReqVO) {
        QualityTaskDO dictType = BeanUtils.toBean(createReqVO, QualityTaskDO.class);
        qualityTaskMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateQualityTask(QualityTaskSaveReqVO updateReqVO) {
        // 相关校验

        // 更新主数据质量任务
        QualityTaskDO updateObj = BeanUtils.toBean(updateReqVO, QualityTaskDO.class);
        return qualityTaskMapper.update(updateObj, new UpdateWrapper<QualityTaskDO>()
                .eq("id", updateReqVO.getId())
                .set(updateReqVO.isResponsibleUserIdSpecified() && updateReqVO.getResponsibleUserId() == null,
                        "responsible_user_id", null));
    }
    @Override
    public int removeQualityTask(Collection<Long> idList) {
        // 批量删除主数据质量任务
        return qualityTaskMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityTaskDO getQualityTaskById(Long id) {
        return qualityTaskMapper.selectById(id);
    }

    @Override
    public List<QualityTaskDO> getQualityTaskList() {
        return qualityTaskMapper.selectList();
    }

    @Override
    public Map<Long, QualityTaskDO> getQualityTaskMap() {
        List<QualityTaskDO> qualityTaskList = qualityTaskMapper.selectList();
        return qualityTaskList.stream()
                .collect(Collectors.toMap(
                        QualityTaskDO::getId,
                        qualityTaskDO -> qualityTaskDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入主数据质量任务数据
         *
         * @param importExcelList 主数据质量任务数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityTask(List<QualityTaskRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityTaskRespVO respVO : importExcelList) {
                try {
                    QualityTaskDO qualityTaskDO = BeanUtils.toBean(respVO, QualityTaskDO.class);
                    Long qualityTaskId = respVO.getId();
                    if (isUpdateSupport) {
                        if (qualityTaskId != null) {
                            QualityTaskDO existingQualityTask = qualityTaskMapper.selectById(qualityTaskId);
                            if (existingQualityTask != null) {
                                qualityTaskMapper.updateById(qualityTaskDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + qualityTaskId + " 的主数据质量任务记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + qualityTaskId + " 的主数据质量任务记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityTaskDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", qualityTaskId);
                        QualityTaskDO existingQualityTask = qualityTaskMapper.selectOne(queryWrapper);
                        if (existingQualityTask == null) {
                            qualityTaskMapper.insert(qualityTaskDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + qualityTaskId + " 的主数据质量任务记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + qualityTaskId + " 的主数据质量任务记录已存在。");
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
