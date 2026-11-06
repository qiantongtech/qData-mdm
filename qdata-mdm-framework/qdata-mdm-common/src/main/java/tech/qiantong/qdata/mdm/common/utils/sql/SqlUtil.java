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

package tech.qiantong.qdata.mdm.common.utils.sql;

import tech.qiantong.qdata.mdm.common.exception.UtilException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.MessageUtils;

/**
 * sql operation tool class
 *
 * @author qdata
 */
public class SqlUtil
{
    /**
     * Define commonly used sql keywords
     */
    public static String SQL_REGEX = "and |extractvalue|updatexml|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |+|user()";

    /**
     * Only supports letters, numbers, underscores, spaces, commas, and decimal points (supports sorting by multiple fields)
     */
    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    /**
     * Limit the maximum length of orderBy
     */
    private static final int ORDER_BY_MAX_LENGTH = 500;

    /**
     * Check characters to prevent injection bypass
     */
    public static String escapeOrderBySql(String value)
    {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value))
        {
            throw new UtilException(MessageUtils.messageWithFallback(
                    "sys.error.sql.param.invalid", "The parameter is invalid; the query cannot be performed"));
        }
        if (StringUtils.length(value) > ORDER_BY_MAX_LENGTH)
        {
            throw new UtilException(MessageUtils.messageWithFallback(
                    "sys.error.sql.param.limit.exceeded",
                    "The parameter exceeds the maximum limit; the query cannot be performed"));
        }
        return value;
    }

    /**
     * Verify whether the order by syntax complies with the specification
     */
    public static boolean isValidOrderBySql(String value)
    {
        return value.matches(SQL_PATTERN);
    }

    /**
     * SQL keyword check
     */
    public static void filterKeyword(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords)
        {
            if (StringUtils.indexOfIgnoreCase(value, sqlKeyword) > -1)
            {
                throw new UtilException(MessageUtils.messageWithFallback(
                        "sys.error.sql.injection.risk", "The parameter poses an SQL injection risk"));
            }
        }
    }
}
