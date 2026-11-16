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

package tech.qiantong.qdata.mdm.dist.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceLogPageReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceLogDO;
/**
 * 分发任务执行日志Service接口
 *
 * @author qdata
 * @date 2026-10-13
 */
public interface IDistTaskInstanceLogService extends IService<DistTaskInstanceLogDO> {

    /**
     * 获得分发任务执行日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 分发任务执行日志分页列表
     */
    PageResult<DistTaskInstanceLogDO> getDistTaskInstanceLogPage(DistTaskInstanceLogPageReqVO pageReqVO);


}
