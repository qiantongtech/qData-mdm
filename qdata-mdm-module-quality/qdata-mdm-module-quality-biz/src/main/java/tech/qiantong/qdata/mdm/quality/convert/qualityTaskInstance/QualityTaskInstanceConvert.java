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

package tech.qiantong.qdata.mdm.quality.convert.qualityTaskInstance;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;

import java.util.List;

/**
 * 主数据质量任务执行实例 Convert
 *
 * @author qdata
 * @date 2026-09-22
 */
@Mapper
public interface QualityTaskInstanceConvert {
    QualityTaskInstanceConvert INSTANCE = Mappers.getMapper(QualityTaskInstanceConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param qualityTaskInstancePageReqVO 请求参数
     * @return QualityTaskInstanceDO
     */
     QualityTaskInstanceDO convertToDO(QualityTaskInstancePageReqVO qualityTaskInstancePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param qualityTaskInstanceSaveReqVO 保存请求参数
     * @return QualityTaskInstanceDO
     */
     QualityTaskInstanceDO convertToDO(QualityTaskInstanceSaveReqVO qualityTaskInstanceSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param qualityTaskInstanceDO 实体对象
     * @return QualityTaskInstanceRespVO
     */
     QualityTaskInstanceRespVO convertToRespVO(QualityTaskInstanceDO qualityTaskInstanceDO);

    /**
     * DOList 转换为 RespVOList
     * @param qualityTaskInstanceDOList 实体对象列表
     * @return List<QualityTaskInstanceRespVO>
     */
     List<QualityTaskInstanceRespVO> convertToRespVOList(List<QualityTaskInstanceDO> qualityTaskInstanceDOList);
}
