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
 * Business License: https://qdata.qiantong.tech/business/policy.html
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
 * 接入任务的 Quartz 定时触发入口，负责提交异步运行实例。
 * <p>
 * 任务状态校验和执行互斥由提交服务处理，实际执行结果由运行实例及运行日志记录。
 *
 * @author chentangwen
 */
@Slf4j
@DisallowConcurrentExecution
public class IngestTaskJob implements Job {

    /**
     * Quartz 作业数据中保存接入任务 ID 的键。
     */
    public static final String TASK_ID = "taskId";

    /**
     * 读取并校验任务 ID，以定时触发方式提交接入任务。
     * <p>
     * 方法正常返回表示提交已受理；提交失败时保留异常原因交由 Quartz 记录，
     * 不要求 Quartz 立即重新触发。
     *
     * @param context 包含接入任务 ID 的 Quartz 执行上下文
     * @throws JobExecutionException 任务 ID 缺失、无效或任务提交失败
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = null;
        try {
            Object taskIdValue = context.getMergedJobDataMap().get(TASK_ID);
            if (!(taskIdValue instanceof Long)) {
                throw new IllegalArgumentException("接入任务 ID 缺失或类型不正确");
            }
            taskId = (Long) taskIdValue;
            if (taskId <= 0) {
                throw new IllegalArgumentException("接入任务 ID 必须为正整数");
            }
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
