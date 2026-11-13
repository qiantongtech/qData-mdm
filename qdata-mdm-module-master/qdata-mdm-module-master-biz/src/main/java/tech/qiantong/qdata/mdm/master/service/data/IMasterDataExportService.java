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

package tech.qiantong.qdata.mdm.master.service.data;

import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataPageReqVO;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

/**
 * 动态主数据导出业务接口。
 */
public interface IMasterDataExportService {

    /**
     * 按当前筛选条件导出主数据。
     *
     * @param response 用于写入导出文件的 HTTP 响应
     * @param instanceId 模型物化实例 ID
     * @param filter 导出筛选条件
     */
    void export(HttpServletResponse response, Long instanceId,
                       MasterDataPageReqVO filter) throws IOException;
}
