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

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogSummaryRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceCallLogDO;

/**
 * 数据服务调用记录的查询、统计和内部写入服务。
 *
 * @author chentangwen
 */
public interface IDistServiceCallLogService {
    /**
     * 按服务 ID 分页查询调用记录，按调用时间倒序返回。
     */
    PageResult<DistServiceCallLogDO> getPage(DistServiceCallLogPageReqVO request);

    /**
     * 查询单次调用记录；记录不存在或已逻辑删除时返回空。
     */
    DistServiceCallLogDO getById(Long id);

    /**
     * 统计近七天总数、成功数和失败数，并附历史最近一次调用摘要。
     */
    DistServiceCallLogSummaryRespVO getSummary(Long serviceId);

    /**
     * 补齐默认时间、状态及授权展示编号后写入记录，返回日志主键；摘要须由调用方脱敏。
     */
    Long record(DistServiceCallLogDO callLog);
}
