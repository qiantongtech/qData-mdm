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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import java.util.Date;
import lombok.Data;

/**
 * 接入任务文件来源配置对象。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskFileConfigVO {

    /**
     * 文件类型：CSV、XLS 或 XLSX。
     */
    private String fileType;

    /**
     * 来源文件名称。
     */
    private String fileName;

    /**
     * 来源文件路径或令牌；配置文件来源时优先使用该字段。
     */
    private String filePath;

    /**
     * 文件大小，单位：字节。
     */
    private Long fileSize;

    /**
     * 文件上传时间。
     */
    private Date uploadTime;

    /**
     * 上传文件令牌，filePath 未配置时使用该值定位文件。
     */
    private String fileToken;
}
