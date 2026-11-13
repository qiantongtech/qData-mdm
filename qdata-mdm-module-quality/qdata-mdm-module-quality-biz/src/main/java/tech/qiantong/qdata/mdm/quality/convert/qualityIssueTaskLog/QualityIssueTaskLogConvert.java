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

package tech.qiantong.qdata.mdm.quality.convert.qualityIssueTaskLog;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo.QualityIssueTaskLogPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo.QualityIssueTaskLogRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo.QualityIssueTaskLogSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskLog.QualityIssueTaskLogDO;

import java.util.List;

/**
 * 问题整改记录日志 Convert
 *
 * @author qdata
 * @date 2026-09-23
 */
@Mapper
public interface QualityIssueTaskLogConvert {
    QualityIssueTaskLogConvert INSTANCE = Mappers.getMapper(QualityIssueTaskLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param qualityIssueTaskLogPageReqVO 请求参数
     * @return QualityIssueTaskLogDO
     */
     QualityIssueTaskLogDO convertToDO(QualityIssueTaskLogPageReqVO qualityIssueTaskLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param qualityIssueTaskLogSaveReqVO 保存请求参数
     * @return QualityIssueTaskLogDO
     */
     QualityIssueTaskLogDO convertToDO(QualityIssueTaskLogSaveReqVO qualityIssueTaskLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param qualityIssueTaskLogDO 实体对象
     * @return QualityIssueTaskLogRespVO
     */
     QualityIssueTaskLogRespVO convertToRespVO(QualityIssueTaskLogDO qualityIssueTaskLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param qualityIssueTaskLogDOList 实体对象列表
     * @return List<QualityIssueTaskLogRespVO>
     */
     List<QualityIssueTaskLogRespVO> convertToRespVOList(List<QualityIssueTaskLogDO> qualityIssueTaskLogDOList);
}
