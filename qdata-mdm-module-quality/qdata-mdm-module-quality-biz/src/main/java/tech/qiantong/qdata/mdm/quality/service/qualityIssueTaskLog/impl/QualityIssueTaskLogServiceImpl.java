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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskLog.impl;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo.QualityIssueTaskLogPageReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskLog.QualityIssueTaskLogDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueTaskLog.QualityIssueTaskLogMapper;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTask.IQualityIssueTaskService;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskLog.IQualityIssueTaskLogService;
/**
 * 整改任务处理记录查询。记录由任务操作事务写入，不允许独立导入、修改或删除。
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityIssueTaskLogServiceImpl extends ServiceImpl<QualityIssueTaskLogMapper, QualityIssueTaskLogDO>
        implements IQualityIssueTaskLogService {
    @Resource
    private QualityIssueTaskLogMapper qualityIssueTaskLogMapper;
    @Resource
    private IQualityIssueTaskService qualityIssueTaskService;
    @Resource
    private ISysUserService sysUserService;

    @Override
    public PageResult<QualityIssueTaskLogDO> getQualityIssueTaskLogPage(QualityIssueTaskLogPageReqVO request) {
        if (request.getQualityIssueTaskId() == null) {
            throw new ServiceException("请指定整改任务后查看处理记录");
        }
        qualityIssueTaskService.getQualityIssueTaskById(request.getQualityIssueTaskId());
        int pageNum = request.getPageNum() == null ? 1 : Math.max(1, request.getPageNum());
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        Page<QualityIssueTaskLogDO> page = new Page<>(pageNum, pageSize);
        page.setSearchCount(!PageParam.PAGE_SIZE_NONE.equals(request.getPageSize()));
        qualityIssueTaskLogMapper.selectBatchPage(page, request.getQualityIssueTaskId());
        fillHandlerPhones(page.getRecords());
        return new PageResult<>(page.getRecords(), page.getSize() < 0 ? (long) page.getRecords().size() : page.getTotal());
    }

    @Override
    public QualityIssueTaskLogDO getQualityIssueTaskLogById(Long id) {
        QualityIssueTaskLogDO entry = qualityIssueTaskLogMapper.selectById(id);
        if (entry == null) {
            throw new ServiceException("处理记录不存在");
        }
        qualityIssueTaskService.getQualityIssueTaskById(entry.getQualityIssueTaskId());
        fillHandlerPhones(Collections.singletonList(entry));
        return entry;
    }

    private void fillHandlerPhones(List<QualityIssueTaskLogDO> rows) {
        Map<Long, SysUser> users = new HashMap<>();
        for (QualityIssueTaskLogDO row : rows) {
            // 与日志中的创建人姓名对应；历史记录缺少创建人 ID 时使用处理人 ID。
            Long userId = row.getCreatorId() == null ? row.getTaskHandlerId() : row.getCreatorId();
            if (userId != null && !users.containsKey(userId)) {
                SysUser user = sysUserService.selectUserById(userId);
                users.put(userId, user != null && "0".equals(user.getDelFlag()) ? user : null);
            }
            SysUser user = users.get(userId);
            row.setHandlerPhone(user == null ? null : user.getPhonenumber());
        }
    }
}
