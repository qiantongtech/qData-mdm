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

package tech.qiantong.qdata.mdm.master.service.change.maintenance;

import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterExecuteTaskDO;

/** 操作策略负责正式写入，任务调度及项目库结果回写由统一入口处理。 */
public interface MaintenanceStrategy {
    String changeType();

    MaintenanceResult execute(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model);

    default MaintenanceWritePlan plan(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            MasterChangeRequestItemDO item, MasterModelRespDTO model) {
        throw new tech.qiantong.qdata.mdm.common.exception.ServiceException("此操作不支持批量执行");
    }
}
