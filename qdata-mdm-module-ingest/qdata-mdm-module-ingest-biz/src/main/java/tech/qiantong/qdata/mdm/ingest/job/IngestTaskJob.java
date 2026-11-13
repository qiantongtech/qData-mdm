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

package tech.qiantong.qdata.mdm.ingest.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import tech.qiantong.qdata.mdm.common.utils.spring.SpringUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskSubmitService;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.SCHEDULED;

/**
 * Quartz 只负责提交，运行结果由接入任务实例记录。
 *
 * @author chentangwen
 */
@Slf4j
@DisallowConcurrentExecution
public class IngestTaskJob implements Job {

    /** 调度注册与触发执行之间传递接入任务编号的键。 */
    public static final String TASK_ID = "taskId";

    /**
     * Quartz 触发时提交异步接入实例，返回只代表受理完成。
     * 异常继续交给 Quartz 统一记录，实际运行结果由接入实例及运行日志提供。
     *
     * @param context 包含接入任务 ID 的 Quartz 执行上下文
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long taskId = context.getMergedJobDataMap().getLong(TASK_ID);
        try {
            log.info("触发接入定时任务，taskId={}", taskId);
            // Job 由 Quartz 创建，提交服务从 Spring 容器取得。
            IngestTaskRunRespVO run = SpringUtils.getBean(IIngestTaskSubmitService.class)
                    .submit(taskId, SCHEDULED, "system");
            log.info("接入定时任务提交完成，taskId={}, runId={}", taskId, run.getId());
        } catch (Exception ex) {
            throw new JobExecutionException("接入定时任务提交失败，taskId=" + taskId, ex, false);
        }
    }
}
