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

import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskPageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;

import java.util.Collection;
import java.util.List;

/**
 * 接入任务
 *
 * @author chentangwen
 * @date 2026-09-15
 */
public interface IIngestTaskService {

    /**
     * 获得接入任务分页列表。
     *
     * @param req 分页及筛选条件
     * @return 接入任务分页列表
     */
    PageResult<IngestTaskRespVO> page(IngestTaskPageReqVO req);

    /**
     * 获得接入任务详情及其类型配置。
     *
     * @param id 接入任务编号
     * @return 接入任务详情
     */
    IngestTaskRespVO detail(Long id);

    /**
     * 获得接入任务运行记录分页列表。
     *
     * @param id 接入任务编号
     * @param req 分页参数
     * @return 运行记录分页列表
     */
    PageResult<IngestTaskRunRespVO> runs(Long id, PageParam req);

    /**
     * 提交接入任务，后台执行，运行记录接口提供实时结果。
     *
     * @param id 接入任务编号
     * @param operator 操作人
     * @return 已受理的运行实例
     */
    IngestTaskRunRespVO execute(Long id, String operator);

    /**
     * 工作线程执行前读取并校验当前任务配置。
     */
    IngestTaskRespVO prepareExecution(Long id);

    /**
     * 创建接入任务。
     *
     * @param req 接入任务信息
     * @return 接入任务编号
     */
    Long create(IngestTaskSaveReqVO req);

    /**
     * 更新接入任务。
     *
     * @param req 接入任务信息
     * @return 更新行数
     */
    int update(IngestTaskSaveReqVO req);

    /**
     * 复制接入任务。
     *
     * @param id 被复制的任务编号
     * @param operator 操作人
     * @return 新任务编号
     */
    Long copy(Long id, String operator);

    /**
     * 修改接入任务启用状态。
     *
     * @param id 接入任务编号
     * @param status 目标状态
     * @param operator 操作人
     * @return 更新行数
     */
    int changeStatus(Long id, String status, String operator);

    /**
     * 独立启停定时调度，提交后同步现有 Quartz 作业。
     * @param id 接入任务编号
     * @param status 调度状态，1启用、0暂停
     * @param operator 操作人
     * @return 更新行数
     */
    int changeScheduleStatus(Long id, String status, String operator);

    /**
     * 批量删除接入任务。
     *
     * @param ids 接入任务编号集合
     * @return 删除行数
     */
    int remove(Collection<Long> ids);

    /**
     * 获得模型业务字段列表。
     *
     * @param modelId 模型编号
     * @return 模型字段列表
     */
    List<IngestModelFieldRespVO> modelFields(Long modelId);

    /**
     * 校验接入任务配置。
     *
     * @param req 接入任务信息
     */
    void validate(IngestTaskSaveReqVO req);
}
