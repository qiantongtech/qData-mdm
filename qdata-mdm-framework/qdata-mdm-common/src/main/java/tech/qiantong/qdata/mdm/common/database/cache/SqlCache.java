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

package tech.qiantong.qdata.mdm.common.database.cache;

import tech.qiantong.qdata.mdm.common.database.utils.MD5Util;

import java.util.Arrays;

/**
 * SQL cache interface
 */
public interface SqlCache {

    /**
     * Compute cache key
     */
    default String buildSqlCacheKey(String sql, Object[] args) {
        return MD5Util.encrypt(sql + ":" + Arrays.toString(args));
    }

    /**
     * Put into cache
     * @param key   key
     * @param value value
     */
    void put(String key, Object value, long ttl);

    /**
     * Get from cache
     * @param key   key
     * @return
     */
    <T> T get(String key);

    /**
     * Delete from cache
     * @param key  key
     */
    void delete(String key);
}
