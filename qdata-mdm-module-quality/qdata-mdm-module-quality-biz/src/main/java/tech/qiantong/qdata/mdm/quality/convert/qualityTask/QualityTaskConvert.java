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

package tech.qiantong.qdata.mdm.quality.convert.qualityTask;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;

/**
 * 主数据质量任务 Convert
 *
 * @author qdata
 * @date 2026-09-22
 */
@Mapper
public interface QualityTaskConvert {
    QualityTaskConvert INSTANCE = Mappers.getMapper(QualityTaskConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param qualityTaskPageReqVO 请求参数
     * @return QualityTaskDO
     */
     QualityTaskDO convertToDO(QualityTaskPageReqVO qualityTaskPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param qualityTaskSaveReqVO 保存请求参数
     * @return QualityTaskDO
     */
     QualityTaskDO convertToDO(QualityTaskSaveReqVO qualityTaskSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param qualityTaskDO 实体对象
     * @return QualityTaskRespVO
     */
     QualityTaskRespVO convertToRespVO(QualityTaskDO qualityTaskDO);

    /**
     * DOList 转换为 RespVOList
     * @param qualityTaskDOList 实体对象列表
     * @return List<QualityTaskRespVO>
     */
     List<QualityTaskRespVO> convertToRespVOList(List<QualityTaskDO> qualityTaskDOList);
}
