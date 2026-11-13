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

package tech.qiantong.qdata.mdm.master.service.change.service;

import tech.qiantong.qdata.mdm.master.change.dto.MasterChangeRequestSubmitReqDTO;
import tech.qiantong.qdata.mdm.master.change.dto.MasterChangeRequestSubmitRespDTO;

/** 其他模块只需依赖master-api，提交最终业务字段值即可创建主数据申请。 */
public interface MasterChangeRequestApi {

    /**
     * 复用单条新增、修改申请流程，创建已通过申请并交由执行器异步生效。
     *
     * @param request 物化实例、操作、申请人和最终字段值；来源默认MERGE
     * @return 已提交的申请信息；校验或保存失败抛出业务异常
     */
    MasterChangeRequestSubmitRespDTO submit(MasterChangeRequestSubmitReqDTO request);
}
