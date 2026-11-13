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

package tech.qiantong.qdata.mdm.quality.service.qualityIssue;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueSaveReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssuePageReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;
/**
 * 问题数据Service接口
 *
 * @author qdata
 * @date 2026-09-23
 */
public interface IQualityIssueService extends IService<QualityIssueDO> {

    /**
     * 获得问题数据分页列表
     *
     * @param pageReqVO 分页请求
     * @return 问题数据分页列表
     */
    PageResult<QualityIssueDO> getQualityIssuePage(QualityIssuePageReqVO pageReqVO);

    /** 获得当前筛选条件下的问题状态数量。 */
    Map<String, Long> getQualityIssueStatusCount(QualityIssuePageReqVO pageReqVO);

    /** 保存检测问题及规则明细，复用同一主数据尚未关闭的问题。 */
    void saveDetectedIssue(QualityIssueDO detected, QualityTaskInstanceDO instance, Long userId, String userName);

    /**
     * 创建问题数据
     *
     * @param createReqVO 问题数据信息
     * @return 问题数据编号
     */
    Long createQualityIssue(QualityIssueSaveReqVO createReqVO);

    /**
     * 更新问题数据
     *
     * @param updateReqVO 问题数据信息
     */
    int updateQualityIssue(QualityIssueSaveReqVO updateReqVO);

    /**
     * 删除问题数据
     *
     * @param idList 问题数据编号
     */
    int removeQualityIssue(Collection<Long> idList);

    /**
     * 获得问题数据详情
     *
     * @param id 问题数据编号
     * @return 问题数据
     */
    QualityIssueDO getQualityIssueById(Long id);

    /**
     * 获得全部问题数据列表
     *
     * @return 问题数据列表
     */
    List<QualityIssueDO> getQualityIssueList();

    /**
     * 获得全部问题数据 Map
     *
     * @return 问题数据 Map
     */
    Map<Long, QualityIssueDO> getQualityIssueMap();


    /**
     * 导入问题数据数据
     *
     * @param importExcelList 问题数据数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityIssue(List<QualityIssueRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 通过实例id查询问题数据列表
     *
     * @param qualityTaskInstanceId 质量任务实例id
     * @return 问题数据列表
     */
    List<QualityIssueDO> getQualityIssueListByInstanceId(Long qualityTaskInstanceId);

}
