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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskLog;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo.QualityIssueTaskLogPageReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskLog.QualityIssueTaskLogDO;

/**
 * 问题整改记录日志Service接口
 *
 * @author qdata
 * @date 2026-09-23
 */
public interface IQualityIssueTaskLogService extends IService<QualityIssueTaskLogDO> {

    /**
     * 获得问题整改记录日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 问题整改记录日志分页列表
     */
    PageResult<QualityIssueTaskLogDO> getQualityIssueTaskLogPage(QualityIssueTaskLogPageReqVO pageReqVO);

    /**
     * 获得问题整改记录日志详情
     *
     * @param id 问题整改记录日志编号
     * @return 问题整改记录日志
     */
    QualityIssueTaskLogDO getQualityIssueTaskLogById(Long id);

}
