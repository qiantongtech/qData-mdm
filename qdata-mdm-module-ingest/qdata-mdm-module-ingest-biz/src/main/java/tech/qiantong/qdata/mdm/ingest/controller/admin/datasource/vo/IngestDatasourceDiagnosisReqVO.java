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

package tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;

@Schema(description = "数据源诊断 Request VO")
@Data
public class IngestDatasourceDiagnosisReqVO {

    @Schema(description = "已保存数据源ID，与 connection 二选一")
    private Long datasourceId;

    @Schema(description = "未保存的连接配置，与 datasourceId 二选一")
    @Valid
    private IngestDatasourceSaveReqVO connection;

    @Schema(description = "指定模式/架构名称")
    private String schemaName;

    @Schema(description = "指定读取权限检查表")
    private String tableName;

    @Schema(description = "HDFS/FTP目录，默认根目录", defaultValue = "/")
    private String resourcePath = "/";
}
