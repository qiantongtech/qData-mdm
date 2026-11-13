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

package tech.qiantong.qdata.mdm.merge.service.detect;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectScopeRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectStartReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;

import java.util.List;

/**
 * 重复检测
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface IMergeDetectService extends IService<MergeDuplicateTaskDO> {

    /**
     * 分页查询重复检测任务。
     *
     * @param reqVO 分页及筛选条件
     * @return 重复检测任务分页列表
     */
    PageResult<MergeDuplicateTaskDO> pageTasks(MergeTaskPageReqVO reqVO);

    /**
     * 分页查询重复检测实例。
     *
     * @param reqVO 分页及筛选条件
     * @return 重复检测实例分页列表
     */
    PageResult<MergeDuplicateTaskInstanceDO> pageInstances(MergeTaskPageReqVO reqVO);

    /**
     * 分页查询重复检测生成的候选组。
     *
     * @param reqVO 分页及筛选条件
     * @return 候选组分页列表
     */
    PageResult<MergeCandidateGroupDO> pageGroups(MergeGroupPageReqVO reqVO);

    /**
     * 批量补充重复检测任务统计信息。
     *
     * @param rows 待补充的任务列表
     */
    void fillTaskMetrics(List<MergeTaskRespVO> rows);

    /**
     * 保存重复检测任务配置。
     *
     * @param reqVO 任务配置
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 新建任务编号
     */
    Long save(MergeDetectStartReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 更新重复检测任务配置。
     *
     * @param taskId 任务编号
     * @param reqVO 更新后的任务配置
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 更新行数
     */
    Integer updateTask(Long taskId, MergeDetectStartReqVO reqVO, Long operatorId, String operatorName);

    /** 修改任务启停状态，停用时同时暂停调度。 */
    Integer changeStatus(Long taskId, String status, Long operatorId, String operatorName);

    /** 独立启停已启用且配置为定时执行的任务调度。 */
    Integer changeScheduleStatus(Long taskId, String status, Long operatorId, String operatorName);

    /** 定时执行时在任务锁内再次检查任务和调度开关。 */
    Long executeScheduled(Long taskId, Long operatorId, String operatorName);

    /**
     * 删除单个重复检测任务。
     *
     * @param taskId 任务编号
     * @return 删除行数
     */
    Integer deleteTask(Long taskId);

    /**
     * 批量删除重复检测任务。
     *
     * @param taskIds 任务编号集合
     * @return 删除行数
     */
    Integer deleteTasks(List<Long> taskIds);

    /**
     * 创建并执行重复检测任务。
     *
     * @param reqVO 检测配置
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 运行实例编号
     */
    Long start(MergeDetectStartReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 执行已有重复检测任务。
     *
     * @param taskId 任务编号
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 运行实例编号
     */
    Long execute(Long taskId, Long operatorId, String operatorName);

    /**
     * 预览重复检测范围和可处理数据。
     *
     * @param reqVO 检测范围配置
     * @return 范围预览结果
     */
    MergeDetectScopeRespVO previewScope(MergeDetectStartReqVO reqVO);

    /**
     * 重新检测尚未进入结果处理的模型数据。
     *
     * @param modelId 模型编号
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 运行实例编号
     */
    Long redetect(Long modelId, Long operatorId, String operatorName);

    /**
     * 将候选组转入匹配审核或单条结果处理。
     *
     * @param groupIds 候选组编号集合
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 转移成功的候选组数量
     */
    Integer transfer(List<Long> groupIds, Long operatorId, String operatorName);
}
