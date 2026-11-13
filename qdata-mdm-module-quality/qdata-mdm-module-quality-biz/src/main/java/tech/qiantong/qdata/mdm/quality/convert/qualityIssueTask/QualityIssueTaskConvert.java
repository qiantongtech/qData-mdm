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

package tech.qiantong.qdata.mdm.quality.convert.qualityIssueTask;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTask.QualityIssueTaskDO;

/**
 * 问题整改任务 Convert
 *
 * @author qdata
 * @date 2026-09-23
 */
@Mapper
public interface QualityIssueTaskConvert {
    QualityIssueTaskConvert INSTANCE = Mappers.getMapper(QualityIssueTaskConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param qualityIssueTaskPageReqVO 请求参数
     * @return QualityIssueTaskDO
     */
     QualityIssueTaskDO convertToDO(QualityIssueTaskPageReqVO qualityIssueTaskPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param qualityIssueTaskSaveReqVO 保存请求参数
     * @return QualityIssueTaskDO
     */
     QualityIssueTaskDO convertToDO(QualityIssueTaskSaveReqVO qualityIssueTaskSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param qualityIssueTaskDO 实体对象
     * @return QualityIssueTaskRespVO
     */
     QualityIssueTaskRespVO convertToRespVO(QualityIssueTaskDO qualityIssueTaskDO);

    /**
     * DOList 转换为 RespVOList
     * @param qualityIssueTaskDOList 实体对象列表
     * @return List<QualityIssueTaskRespVO>
     */
     List<QualityIssueTaskRespVO> convertToRespVOList(List<QualityIssueTaskDO> qualityIssueTaskDOList);
}
