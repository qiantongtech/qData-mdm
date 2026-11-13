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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskInfo.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskInfo.QualityIssueTaskInfoDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueTaskInfo.QualityIssueTaskInfoMapper;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskInfo.IQualityIssueTaskInfoService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 整改任务明细服务实现。
 */
@Service
public class QualityIssueTaskInfoServiceImpl
        extends ServiceImpl<QualityIssueTaskInfoMapper, QualityIssueTaskInfoDO>
        implements IQualityIssueTaskInfoService {

    @Resource
    private QualityIssueTaskInfoMapper qualityIssueTaskInfoMapper;

    @Override
    public List<QualityIssueTaskInfoDO> listByIssueInfoId(Long issueInfoId) {
        return qualityIssueTaskInfoMapper.selectRelatedByIssueInfoId(issueInfoId);
    }

    @Override
    public QualityIssueTaskInfoDO lockInfo(Long id) {
        return qualityIssueTaskInfoMapper.lockInfo(id);
    }

    @Override
    public List<QualityIssueTaskInfoDO> listByTaskId(Long taskId) {
        return qualityIssueTaskInfoMapper.selectByTaskId(taskId);
    }

    @Override
    public int updateInfoStatus(Long id, String oldStatus, String status, String requirement,
                                Date rectificationTime, Long updaterId, String updateBy, Date updateTime) {
        return qualityIssueTaskInfoMapper.updateInfoStatus(id, oldStatus, status, requirement,
                rectificationTime, updaterId, updateBy, updateTime);
    }
}

