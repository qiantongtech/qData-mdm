/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.system.convert.flowMyBusiness;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessPageReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessSaveReqVO;
import tech.qiantong.qdata.mdm.system.dal.dataobject.flowMyBusiness.SystemFlowMyBusinessDO;

/**
 * 审批记录 Convert
 *
 * @author qdata
 * @date 2025-04-25
 */
@Mapper
public interface SystemFlowMyBusinessConvert {
    SystemFlowMyBusinessConvert INSTANCE = Mappers.getMapper(SystemFlowMyBusinessConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param systemFlowMyBusinessPageReqVO 请求参数
     * @return SystemFlowMyBusinessDO
     */
     SystemFlowMyBusinessDO convertToDO(SystemFlowMyBusinessPageReqVO systemFlowMyBusinessPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param systemFlowMyBusinessSaveReqVO 保存请求参数
     * @return SystemFlowMyBusinessDO
     */
     SystemFlowMyBusinessDO convertToDO(SystemFlowMyBusinessSaveReqVO systemFlowMyBusinessSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param systemFlowMyBusinessDO 实体对象
     * @return SystemFlowMyBusinessRespVO
     */
     SystemFlowMyBusinessRespVO convertToRespVO(SystemFlowMyBusinessDO systemFlowMyBusinessDO);

    /**
     * DOList 转换为 RespVOList
     * @param systemFlowMyBusinessDOList 实体对象列表
     * @return List<SystemFlowMyBusinessRespVO>
     */
     List<SystemFlowMyBusinessRespVO> convertToRespVOList(List<SystemFlowMyBusinessDO> systemFlowMyBusinessDOList);
}
