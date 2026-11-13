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

package tech.qiantong.qdata.mdm.quality.convert.qualityIssue;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssuePageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;

import java.util.List;

/**
 * 问题数据 Convert
 *
 * @author qdata
 * @date 2026-09-23
 */
@Mapper
public interface QualityIssueConvert {
    QualityIssueConvert INSTANCE = Mappers.getMapper(QualityIssueConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param qualityIssuePageReqVO 请求参数
     * @return QualityIssueDO
     */
     QualityIssueDO convertToDO(QualityIssuePageReqVO qualityIssuePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param qualityIssueSaveReqVO 保存请求参数
     * @return QualityIssueDO
     */
     QualityIssueDO convertToDO(QualityIssueSaveReqVO qualityIssueSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param qualityIssueDO 实体对象
     * @return QualityIssueRespVO
     */
     QualityIssueRespVO convertToRespVO(QualityIssueDO qualityIssueDO);

    /**
     * DOList 转换为 RespVOList
     * @param qualityIssueDOList 实体对象列表
     * @return List<QualityIssueRespVO>
     */
     List<QualityIssueRespVO> convertToRespVOList(List<QualityIssueDO> qualityIssueDOList);
}
