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

package tech.qiantong.qdata.mdm.mybatis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 主数据源配置文件
 * @author qdata
 */
@Slf4j
@Component
public class MasterDataSourceConfig {

    /** datasource.type 合法取值(mapper 的数据库方言 <choose> 分支按这些匹配;缺失或配错会导致生成的 SQL 残缺)。 */
    private static final List<String> KNOWN_TYPES = Arrays.asList("dm8", "mysql", "kingbase8", "gbase8s", "oracle", "postgresql");

    private static String databaseType;

    @Value("${datasource.type:}")
    public void setDatabaseType(String databaseType) {
        MasterDataSourceConfig.databaseType = databaseType;
    }

    public static String getDatabaseType() {
        return databaseType;
    }

    @PostConstruct
    public void validateDatabaseType() {
        if (databaseType == null || databaseType.trim().isEmpty()) {
            log.error("datasource.type 未配置!mapper 的数据库方言 <choose> 分支将全部不匹配 → 生成的 SQL 残缺/报错。请配置为 {} 之一", KNOWN_TYPES);
        } else if (!KNOWN_TYPES.contains(databaseType.trim().toLowerCase())) {
            log.warn("datasource.type={} 不在已知取值 {} 内,部分 mapper 方言分支可能不匹配", databaseType, KNOWN_TYPES);
        }
    }
}
