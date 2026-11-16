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

package tech.qiantong.qdata.mdm.dist.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogSummaryRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceCallLogDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceCallLogMapper;
import tech.qiantong.qdata.mdm.dist.util.DistServiceCallLogSummaryCalculator;
import tech.qiantong.qdata.mdm.dist.service.service.IDistServiceCallLogService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 数据服务调用日志查询与写入，调用次数由已有日志计算。
 *
 * @author chentangwen
 */
@Service
public class DistServiceCallLogServiceImpl implements IDistServiceCallLogService {
    @Resource
    private DistServiceCallLogMapper mapper;

    @Override
    public PageResult<DistServiceCallLogDO> getPage(DistServiceCallLogPageReqVO request) {
        return mapper.selectPage(request);
    }

    @Override
    public DistServiceCallLogDO getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public DistServiceCallLogSummaryRespVO getSummary(Long serviceId) {
        // 保留历史记录，使最近调用摘要不受近七天统计窗口限制。
        List<DistServiceCallLogDO> calls = mapper.selectList(new LambdaQueryWrapper<DistServiceCallLogDO>()
                .eq(DistServiceCallLogDO::getServiceId, serviceId)
                .orderByDesc(DistServiceCallLogDO::getCallTime));
        return DistServiceCallLogSummaryCalculator.calculate(calls, new Date());
    }

    @Override
    public Long record(DistServiceCallLogDO callLog) {
        if (callLog.getCallTime() == null) {
            callLog.setCallTime(new Date());
        }
        if (callLog.getValidFlag() == null) {
            callLog.setValidFlag("1");
        }
        if (callLog.getDelFlag() == null) {
            callLog.setDelFlag("0");
        }
        if (callLog.getAuthorizationNo() == null && callLog.getAuthorizationId() != null) {
            // 缺少业务单号时，以授权记录 ID 生成展示编号，不执行授权校验。
            callLog.setAuthorizationNo(String.format("SA-%08d", callLog.getAuthorizationId()));
        }
        mapper.insert(callLog);
        return callLog.getId();
    }
}
