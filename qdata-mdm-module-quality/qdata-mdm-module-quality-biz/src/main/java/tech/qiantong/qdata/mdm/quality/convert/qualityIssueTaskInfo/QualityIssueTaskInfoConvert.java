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

package tech.qiantong.qdata.mdm.quality.convert.qualityIssueTaskInfo;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo.vo.QualityIssueTaskInfoRespVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskInfo.QualityIssueTaskInfoDO;

import java.util.List;

/**
 * 整改任务明细转换。
 */
@Mapper
public interface QualityIssueTaskInfoConvert {
    QualityIssueTaskInfoConvert INSTANCE = Mappers.getMapper(QualityIssueTaskInfoConvert.class);

    /**
     * 将单条整改明细转换为响应对象。
     * @param qualityIssueTaskInfoDO 实体对象
     * @return 整改明细响应
     */
     QualityIssueTaskInfoRespVO convertToRespVO(QualityIssueTaskInfoDO qualityIssueTaskInfoDO);

    /**
     * 将整改明细列表转换为响应列表。
     * @param qualityIssueTaskInfoDOList 实体对象列表
     * @return 整改明细响应列表
     */
     List<QualityIssueTaskInfoRespVO> convertToRespVOList(List<QualityIssueTaskInfoDO> qualityIssueTaskInfoDOList);
}
