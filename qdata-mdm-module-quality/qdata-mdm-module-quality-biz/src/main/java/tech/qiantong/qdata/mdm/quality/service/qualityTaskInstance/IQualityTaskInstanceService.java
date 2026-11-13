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

package tech.qiantong.qdata.mdm.quality.service.qualityTaskInstance;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 主数据质量任务执行实例Service接口
 *
 * @author qdata
 * @date 2026-09-22
 */
public interface IQualityTaskInstanceService extends IService<QualityTaskInstanceDO> {
    /** 手动执行任务并保存执行实例（含失败结果）。 */
    QualityTaskInstanceDO executeOnce(Long taskId, Long userId, String userName);

    /**
     * 获得主数据质量任务执行实例分页列表
     *
     * @param pageReqVO 分页请求
     * @return 主数据质量任务执行实例分页列表
     */
    PageResult<QualityTaskInstanceDO> getQualityTaskInstancePage(QualityTaskInstancePageReqVO pageReqVO);

    /**
     * 创建主数据质量任务执行实例
     *
     * @param createReqVO 主数据质量任务执行实例信息
     * @return 主数据质量任务执行实例编号
     */
    Long createQualityTaskInstance(QualityTaskInstanceSaveReqVO createReqVO);

    /**
     * 更新主数据质量任务执行实例
     *
     * @param updateReqVO 主数据质量任务执行实例信息
     */
    int updateQualityTaskInstance(QualityTaskInstanceSaveReqVO updateReqVO);

    /**
     * 删除主数据质量任务执行实例
     *
     * @param idList 主数据质量任务执行实例编号
     */
    int removeQualityTaskInstance(Collection<Long> idList);

    /**
     * 获得主数据质量任务执行实例详情
     *
     * @param id 主数据质量任务执行实例编号
     * @return 主数据质量任务执行实例
     */
    QualityTaskInstanceDO getQualityTaskInstanceById(Long id);

    /**
     * 获得全部主数据质量任务执行实例列表
     *
     * @return 主数据质量任务执行实例列表
     */
    List<QualityTaskInstanceDO> getQualityTaskInstanceList();

    /**
     * 获得全部主数据质量任务执行实例 Map
     *
     * @return 主数据质量任务执行实例 Map
     */
    Map<Long, QualityTaskInstanceDO> getQualityTaskInstanceMap();


    /**
     * 导入主数据质量任务执行实例数据
     *
     * @param importExcelList 主数据质量任务执行实例数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityTaskInstance(List<QualityTaskInstanceRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
