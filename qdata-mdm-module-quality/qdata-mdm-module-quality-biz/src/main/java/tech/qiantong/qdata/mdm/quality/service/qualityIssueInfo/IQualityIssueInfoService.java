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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueInfo;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 问题数据明细Service接口
 *
 * @author qdata
 * @date 2026-09-23
 */
public interface IQualityIssueInfoService extends IService<QualityIssueInfoDO> {

    /**
     * 获得问题数据明细分页列表
     *
     * @param pageReqVO 分页请求
     * @return 问题数据明细分页列表
     */
    PageResult<QualityIssueInfoDO> getQualityIssueInfoPage(QualityIssueInfoPageReqVO pageReqVO);

    /**
     * 创建问题数据明细
     *
     * @param createReqVO 问题数据明细信息
     * @return 问题数据明细编号
     */
    Long createQualityIssueInfo(QualityIssueInfoSaveReqVO createReqVO);

    /**
     * 更新问题数据明细
     *
     * @param updateReqVO 问题数据明细信息
     */
    int updateQualityIssueInfo(QualityIssueInfoSaveReqVO updateReqVO);

    /**
     * 删除问题数据明细
     *
     * @param idList 问题数据明细编号
     */
    int removeQualityIssueInfo(Collection<Long> idList);

    /**
     * 获得问题数据明细详情
     *
     * @param id 问题数据明细编号
     * @return 问题数据明细
     */
    QualityIssueInfoDO getQualityIssueInfoById(Long id);

    /**
     * 获得全部问题数据明细列表
     *
     * @return 问题数据明细列表
     */
    List<QualityIssueInfoDO> getQualityIssueInfoList();

    /**
     * 获得全部问题数据明细 Map
     *
     * @return 问题数据明细 Map
     */
    Map<Long, QualityIssueInfoDO> getQualityIssueInfoMap();


    /**
     * 导入问题数据明细数据
     *
     * @param importExcelList 问题数据明细数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityIssueInfo(List<QualityIssueInfoRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
