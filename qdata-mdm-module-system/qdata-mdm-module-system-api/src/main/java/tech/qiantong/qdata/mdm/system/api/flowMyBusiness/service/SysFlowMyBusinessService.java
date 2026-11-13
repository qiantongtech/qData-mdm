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

package tech.qiantong.qdata.mdm.system.api.flowMyBusiness.service;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.system.api.flowMyBusiness.dto.SystemFlowMyBusinessReqDTO;
import tech.qiantong.qdata.mdm.system.api.flowMyBusiness.dto.SystemFlowMyBusinessRespDTO;

import java.util.List;

public interface SysFlowMyBusinessService {

    /**
     * 创建审批记录
     *
     * @param flowMyBusinessReqDTO 审批记录信息
     * @return 审批记录编号
     */
    Long createSysFlowMyBusiness(SystemFlowMyBusinessReqDTO flowMyBusinessReqDTO);

    /**
     * 更新审批记录
     *
     * @param updateReqVO 审批记录信息
     */
    int updateSysFlowMyBusiness(SystemFlowMyBusinessReqDTO updateReqVO);

    /**
     * 分页查询服务申请记录
     *
     * @param updateReqVO 查询服务申请记录
     */
    PageResult<SystemFlowMyBusinessRespDTO> listPage(SystemFlowMyBusinessReqDTO updateReqVO);

    /**
     * 查询服务申请记录
     *
     * @param updateReqVO 查询服务申请记录条件
     */
    List<SystemFlowMyBusinessRespDTO> list(SystemFlowMyBusinessReqDTO updateReqVO);

    /**
     * 删除数据
     *
     * @param updateReqVO 删除条件
     */
    int removeFlowMyBusiness(SystemFlowMyBusinessReqDTO updateReqVO);
}
