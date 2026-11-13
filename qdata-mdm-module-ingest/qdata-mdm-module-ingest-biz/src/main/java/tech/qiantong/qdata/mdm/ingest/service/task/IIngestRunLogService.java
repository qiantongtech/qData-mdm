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

package tech.qiantong.qdata.mdm.ingest.service.task;

import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestRunLogRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;

/**
 * 过程日志先缓冲到 Redis，后台批量归档，查询合并实时及历史记录。
 *
 * @author chentangwen
 */
public interface IIngestRunLogService {

    /**
     * 原子暂存进度快照与日志；失败时抛出异常，由执行入口终止或恢复实例。
     *
     * @param instance 当前运行实例及累计进度
     * @param level 日志级别
     * @param stage 执行阶段
     * @param message 不含来源原始数据的日志内容
     */
    void publish(IngestTaskInstanceDO instance, String level, String stage, String message);

    /**
     * 获取实时进度；缓存读取失败时返回已入库快照，已落库终态不被覆盖。
     *
     * @param stored 数据库中的实例，可为空
     * @return 可用的最新实例快照
     */
    IngestTaskInstanceDO latest(IngestTaskInstanceDO stored);

    /**
     * 校验任务归属后，按序号增量合并实时日志与归档日志。
     *
     * @param taskId 接入任务 ID
     * @param runId 运行实例 ID
     * @param afterSeq 排他游标，零表示从头读取
     * @return 实时进度、日志列表、后续游标及可见最大序号
     */
    IngestRunLogRespVO read(Long taskId, Long runId, long afterSeq);

    /**
     * 批量归档待处理事件，数据库提交成功后才删除 Redis 中对应的事件。
     */
    void flush();
}
