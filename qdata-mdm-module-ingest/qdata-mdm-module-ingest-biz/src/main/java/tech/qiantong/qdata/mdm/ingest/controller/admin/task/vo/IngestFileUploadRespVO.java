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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import java.util.*;
import lombok.Data;

/**
 * 接入文件上传响应对象，包含文件令牌、文件信息及解析出的来源字段。
 *
 * @author chentangwen
 */
@Data
public class IngestFileUploadRespVO {

    /**
     * 上传文件令牌，用于后续预览及任务配置。
     */
    private String fileToken;

    /**
     * 上传文件访问标识。
     */
    private String filePath;

    /**
     * 上传文件的原始名称。
     */
    private String fileName;

    /**
     * 文件类型：CSV、XLS 或 XLSX。
     */
    private String fileType;

    /**
     * 文件大小，单位：字节。
     */
    private Long fileSize;

    /**
     * 文件上传时间。
     */
    private Date uploadTime;

    /**
     * 从上传文件解析出的来源字段名称列表。
     */
    private List<String> fields;
}
