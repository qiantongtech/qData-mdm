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

package tech.qiantong.qdata.mdm.ingest.api.datasource.dto;

import lombok.Data;

import java.util.List;

@Data
public class DatasourceCreaTeTableListReqDTO {

    /**
     * 数据源类型
     */
    private String datasourceType;

    /**
     * 数据源配置(json字符串)
     */
    private String datasourceConfig;

    /**
     * IP
     */
    private String ip;

    /**
     * 端口号
     */
    private Long port;


    private List<DatasourceCreaTeTableReqDTO> dtoList;
}
