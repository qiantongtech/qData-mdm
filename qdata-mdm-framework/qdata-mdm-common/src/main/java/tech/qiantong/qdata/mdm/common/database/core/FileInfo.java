/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.common.database.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.utils.FileTypeUtil;
import tech.qiantong.qdata.mdm.common.utils.MessageUtils;

import java.util.Date;

/**
 * Data assets - unstructured data directories or folders
 *
 * @author Chaos
 * @date 2025-07-16
 */
@Data
public class FileInfo {

    private String name;
    private String path;
    private boolean isDirectory;
    private long size;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date lastModified;
    private String type;

    public void fillType(){
        type = isDirectory
                ? MessageUtils.messageWithFallback("file.type.directory", "Directory")
                : FileTypeUtil.getFileType(name);
    }

}
