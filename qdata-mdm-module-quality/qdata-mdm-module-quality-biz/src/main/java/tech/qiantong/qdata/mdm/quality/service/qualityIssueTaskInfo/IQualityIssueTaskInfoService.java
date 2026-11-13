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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskInfo;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskInfo.QualityIssueTaskInfoDO;

import java.util.Date;
import java.util.List;

/**
 * 整改任务明细查询和状态更新。
 */
public interface IQualityIssueTaskInfoService extends IService<QualityIssueTaskInfoDO> {

    /** 查询问题项此前下发的整改明细。 */
    List<QualityIssueTaskInfoDO> listByIssueInfoId(Long issueInfoId);

    /** 锁定明细，避免并发状态变更。 */
    QualityIssueTaskInfoDO lockInfo(Long id);

    /** 查询任务下的全部明细。 */
    List<QualityIssueTaskInfoDO> listByTaskId(Long taskId);

    /** 仅在原状态匹配时更新单条明细。 */
    int updateInfoStatus(Long id, String oldStatus, String status, String requirement,
                         Date rectificationTime, Long updaterId, String updateBy, Date updateTime);
}

