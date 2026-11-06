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

package tech.qiantong.qdata.mdm.common.annotation;

import tech.qiantong.qdata.mdm.common.constant.CacheConstants;
import tech.qiantong.qdata.mdm.common.enums.LimitType;

import java.lang.annotation.*;

/**
 * Current limiting annotation
 *
 * @author qdata
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter
{
    /**
     * Current limiting key
     */
    public String key() default CacheConstants.RATE_LIMIT_KEY;

    /**
     * Current limiting time, unit seconds
     */
    public int time() default 60;

    /**
     * Number of current limits
     */
    public int count() default 100;

    /**
     * Current limiting type
     */
    public LimitType limitType() default LimitType.DEFAULT;
}
