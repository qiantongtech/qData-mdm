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

package tech.qiantong.qdata.mdm.merge.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import tech.qiantong.qdata.mdm.common.utils.spring.SpringUtils;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectService;

/**
 * Quartz 重复检测任务入口，执行记录由重复检测实例维护。
 *
 * @author chentangwen
 */
@Slf4j
@DisallowConcurrentExecution
public class MergeDetectTaskJob implements Job {

    public static final String TASK_ID = "taskId";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long taskId = context.getMergedJobDataMap().getLong(TASK_ID);
        try {
            log.info("触发重复检测定时任务，taskId={}", taskId);
            Long instanceId = SpringUtils.getBean(IMergeDetectService.class).executeScheduled(taskId, 0L, "系统调度");
            log.info("重复检测定时任务完成，taskId={}, instanceId={}", taskId, instanceId);
        } catch (Exception ex) {
            throw new JobExecutionException("重复检测定时任务执行失败，taskId=" + taskId, ex, false);
        }
    }
}
