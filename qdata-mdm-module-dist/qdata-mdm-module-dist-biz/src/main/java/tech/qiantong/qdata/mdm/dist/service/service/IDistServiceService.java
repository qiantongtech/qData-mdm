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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.*;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceDO;
import java.util.Collection;

/**
 * 数据服务。
 *
 * @author chentangwen
 */
public interface IDistServiceService extends IService<DistServiceDO> {
    PageResult<DistServiceRespVO> getDistServicePage(DistServicePageReqVO pageReqVO);
    /**
     * 查询服务及按顺序排列的字段配置。
     */
    DistServiceRespVO getDistServiceById(Long id);
    /**
     * 新增服务及字段配置，返回服务ID。
     */
    Long createDistService(DistServiceSaveReqVO createReqVO);
    /**
     * 整体更新服务及字段配置。
     */
    int updateDistService(DistServiceSaveReqVO updateReqVO);
    /**
     * 独立修改启停状态及审计信息，不覆盖服务配置。
     */
    int updateDistServiceStatus(DistServiceStatusReqVO request);
    /**
     * 发布已启用的服务并记录发布时间及操作人。
     */
    int publishDistService(Long id, Long operatorId, String operatorName);
    /**
     * 同一事务内逻辑删除主表及全部字段配置。
     */
    int removeDistService(Collection<Long> ids);
}
