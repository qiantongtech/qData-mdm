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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueTask;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskStatusCountRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo.vo.QualityIssueTaskInfoSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTask.QualityIssueTaskDO;

import java.util.List;
import java.util.Map;
/**
 * 问题整改任务Service接口
 *
 * @author qdata
 * @date 2026-09-23
 */
public interface IQualityIssueTaskService extends IService<QualityIssueTaskDO> {

    /**
     * 按当前用户范围查询整改任务，列表和导出共用筛选逻辑。
     *
     * @param request 模型、视角、关键词及分页条件
     * @return 保留 rows/total 的任务展示结果
     */
    PageResult<QualityIssueTaskDO> getQualityIssueTaskPage(QualityIssueTaskPageReqVO request);

    /** 按列表相同的数据范围统计所有任务状态，忽略状态和分页条件。 */
    QualityIssueTaskStatusCountRespVO getQualityIssueTaskStatusCount(QualityIssueTaskPageReqVO request);

    /**
     * 查询单个任务展示信息，同时校验当前用户访问权。
     *
     * @param id 整改任务 ID
     * @return 整改任务及其关联问题项
     */
    QualityIssueTaskDO getQualityIssueTaskDetail(Long id);

    /**
     * 创建问题整改任务
     *
     * @param createReqVO 问题整改任务信息
     * @return 问题整改任务编号
     */
    Long createQualityIssueTask(QualityIssueTaskSaveReqVO createReqVO);

    /**
     * 开始处理、完成整改或取消任务，不允许修改关联问题和负责人。
     *
     * @param updateReqVO 任务 ID、目标状态及完成说明
     * @return 成功变更的任务数
     */
    int updateQualityIssueTaskStatus(QualityIssueTaskInfoSaveReqVO updateReqVO);

    /** 批量删除尚未开始整改的任务及其下发明细。 */
    int deleteQualityIssueTasks(List<Long> ids, Long userId, String userName);

    /** 提交或撤回审批，仅在待审批和审批中之间切换。 */
    int updateApprovalStatus(Long id, boolean submit, Long updaterId, String updateBy);

    /**
     * 获得问题整改任务详情
     *
     * @param id 问题整改任务编号
     * @return 问题整改任务
     */
    QualityIssueTaskDO getQualityIssueTaskById(Long id);

    /**
     * 获得当前用户可查看的问题整改任务列表
     *
     * @return 问题整改任务列表
     */
    List<QualityIssueTaskDO> getQualityIssueTaskList();

    /**
     * 获得当前用户可查看的问题整改任务 Map
     *
     * @return 问题整改任务 Map
     */
    Map<Long, QualityIssueTaskDO> getQualityIssueTaskMap();

}
