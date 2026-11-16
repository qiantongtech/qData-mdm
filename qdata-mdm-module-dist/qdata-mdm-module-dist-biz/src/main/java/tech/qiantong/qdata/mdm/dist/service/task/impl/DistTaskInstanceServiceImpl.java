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

package tech.qiantong.qdata.mdm.dist.service.task.impl;

import javax.annotation.Resource;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskInstanceMapper;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskInstanceService;
/**
 * 分发任务执行实例Service业务层处理
 *
 * @author qdata
 * @date 2026-10-13
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DistTaskInstanceServiceImpl  extends ServiceImpl<DistTaskInstanceMapper,DistTaskInstanceDO> implements IDistTaskInstanceService {
    @Resource
    private DistTaskInstanceMapper distTaskInstanceMapper;

    @Override
    public PageResult<DistTaskInstanceDO> getDistTaskInstancePage(DistTaskInstancePageReqVO pageReqVO) {
        return distTaskInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public DistTaskInstanceDO getDistTaskInstanceById(Long id) {
        return distTaskInstanceMapper.selectById(id);
    }

}
