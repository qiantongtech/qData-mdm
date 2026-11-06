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

package tech.qiantong.qdata.mdm.common.database.dialect;

import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.FileInfo;

import java.util.List;

/**
 * Data Assets - Unstructured Dialect
 *
 * @author Chaos
 * @date 2025-07-16
 */
public interface FileDialect {

    List<FileInfo> getFiles(DbQueryProperty dbQueryProperty, String path);

    void uploadFile(DbQueryProperty dbQueryProperty, String path, MultipartFile file);

}
