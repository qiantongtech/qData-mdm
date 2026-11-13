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

package tech.qiantong.qdata.mdm.quality.convert.qualityIssueInfo;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;

import java.util.List;

/**
 * 问题数据明细 Convert
 *
 * @author qdata
 * @date 2026-09-23
 */
@Mapper
public interface QualityIssueInfoConvert {
    QualityIssueInfoConvert INSTANCE = Mappers.getMapper(QualityIssueInfoConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param qualityIssueInfoPageReqVO 请求参数
     * @return QualityIssueInfoDO
     */
     QualityIssueInfoDO convertToDO(QualityIssueInfoPageReqVO qualityIssueInfoPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param qualityIssueInfoSaveReqVO 保存请求参数
     * @return QualityIssueInfoDO
     */
     QualityIssueInfoDO convertToDO(QualityIssueInfoSaveReqVO qualityIssueInfoSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param qualityIssueInfoDO 实体对象
     * @return QualityIssueInfoRespVO
     */
     QualityIssueInfoRespVO convertToRespVO(QualityIssueInfoDO qualityIssueInfoDO);

    /**
     * DOList 转换为 RespVOList
     * @param qualityIssueInfoDOList 实体对象列表
     * @return List<QualityIssueInfoRespVO>
     */
     List<QualityIssueInfoRespVO> convertToRespVOList(List<QualityIssueInfoDO> qualityIssueInfoDOList);
}
