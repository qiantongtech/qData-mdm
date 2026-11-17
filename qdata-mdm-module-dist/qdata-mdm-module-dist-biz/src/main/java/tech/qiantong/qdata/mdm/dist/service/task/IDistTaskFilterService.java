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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.service.task;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskFilterSaveReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskFilterDO;
/**
 * 分发任务过滤条件Service接口
 *
 * @author qdata
 * @date 2026-10-13
 */
public interface IDistTaskFilterService extends IService<DistTaskFilterDO> {

    /** 在任务保存事务内整体替换筛选条件，列表顺序即 AND 条件的展示顺序。 */
    void replaceTaskFilters(Long taskId, List<DistTaskFilterSaveReqVO> filters);
}
