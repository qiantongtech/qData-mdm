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

package tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueTaskInfo;

import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskInfo.QualityIssueTaskInfoDO;

import java.util.Date;
import java.util.List;

/**
 * 整改任务明细查询和状态更新。
 */
public interface QualityIssueTaskInfoMapper extends BaseMapperX<QualityIssueTaskInfoDO> {
    /** 查询问题项此前下发的整改明细。 */
    List<QualityIssueTaskInfoDO> selectRelatedByIssueInfoId(@Param("issueInfoId") Long issueInfoId);

    /** 锁定问题项明细，避免重复点击覆盖状态。 */
    QualityIssueTaskInfoDO lockInfo(@Param("id") Long id);

    /** 按任务查询明细，供完成状态同步与详情展示使用。 */
    List<QualityIssueTaskInfoDO> selectByTaskId(@Param("taskId") Long taskId);

    /** 按原状态更新单条问题项。 */
    int updateInfoStatus(@Param("id") Long id, @Param("oldStatus") String oldStatus,
                         @Param("status") String status, @Param("requirement") String requirement,
                         @Param("rectificationTime") Date rectificationTime,
                         @Param("updaterId") Long updaterId, @Param("updateBy") String updateBy,
                         @Param("updateTime") Date updateTime);
}
