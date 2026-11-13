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

package tech.qiantong.qdata.mdm.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布目标的非敏感描述；恢复须用数据源ID重新解析可信连接并核验指纹。
 */
@Data
@NoArgsConstructor
public class ModelPublishTarget {

    /**
     * 平台数据源ID；不在此保存URL、用户名、密码或连接对象
     */
    private Long datasourceId;

    /**
     * 目标连接的身份摘要（指纹）；用来发现数据源ID没变、实际却换了数据库的情况
     */
    private String connectionFingerprint;

    /**
     * 供common方言选择的数据库类型标识
     */
    private String databaseType;

    /**
     * 实际目标数据库版本，供能力校验使用
     */
    private String databaseVersion;

    /**
     * 本次连接的数据库名快照，仅存在JSON，不增加数据库列
     */
    private String databaseName;

    /**
     * 目标Schema快照，空值表示连接默认Schema
     */
    private String schemaName;
}

