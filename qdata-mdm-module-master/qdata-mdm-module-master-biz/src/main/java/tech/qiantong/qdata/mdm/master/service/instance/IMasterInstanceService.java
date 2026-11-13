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

package tech.qiantong.qdata.mdm.master.service.instance;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterInstanceRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.instance.vo.MasterInstancePageReqVO;


/**
 * 权威主数据服务接口。
 */
public interface IMasterInstanceService {

    /**
     * 获得权威主数据入口卡片分页列表。
     *
     * @param pageReqVO 分页请求
     * @return 卡片元数据
     */
    PageResult<MasterInstanceRespDTO> getInstancePage(
            MasterInstancePageReqVO pageReqVO);


}
