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

package tech.qiantong.qdata.mdm.quality.service.qualityTask;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 主数据质量任务Service接口
 *
 * @author qdata
 * @date 2026-09-22
 */
public interface IQualityTaskService extends IService<QualityTaskDO> {

    /**
     * 获得主数据质量任务分页列表
     *
     * @param pageReqVO 分页请求
     * @return 主数据质量任务分页列表
     */
    PageResult<QualityTaskDO> getQualityTaskPage(QualityTaskPageReqVO pageReqVO);

    /**
     * 创建主数据质量任务
     *
     * @param createReqVO 主数据质量任务信息
     * @return 主数据质量任务编号
     */
    Long createQualityTask(QualityTaskSaveReqVO createReqVO);

    /**
     * 更新主数据质量任务
     *
     * @param updateReqVO 主数据质量任务信息
     */
    int updateQualityTask(QualityTaskSaveReqVO updateReqVO);

    /**
     * 删除主数据质量任务
     *
     * @param idList 主数据质量任务编号
     */
    int removeQualityTask(Collection<Long> idList);

    /**
     * 获得主数据质量任务详情
     *
     * @param id 主数据质量任务编号
     * @return 主数据质量任务
     */
    QualityTaskDO getQualityTaskById(Long id);

    /**
     * 获得全部主数据质量任务列表
     *
     * @return 主数据质量任务列表
     */
    List<QualityTaskDO> getQualityTaskList();

    /**
     * 获得全部主数据质量任务 Map
     *
     * @return 主数据质量任务 Map
     */
    Map<Long, QualityTaskDO> getQualityTaskMap();


    /**
     * 导入主数据质量任务数据
     *
     * @param importExcelList 主数据质量任务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityTask(List<QualityTaskRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
