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

import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;

import java.util.List;

/**
 * 接入任务执行接口，负责来源记录处理、实体来源表写入和运行统计。
 *
 * @author chentangwen
 */
public interface IIngestTaskExecuteService {

    /**
     * 执行已受理的实例，逐行转换和写入来源数据，并发布阶段日志及进度。
     *
     * @param task 已通过执行校验的任务配置
     * @param fields 目标模型业务字段
     * @param operator 执行操作人
     * @param instance 统一提交入口创建并取得占用的实例
     * @return 执行结束时的实例结果，日志和统计可能仍在等待归档
     */
    IngestTaskRunRespVO execute(IngestTaskRespVO task, List<IngestModelFieldRespVO> fields, String operator,
                                IngestTaskInstanceDO instance);
}
