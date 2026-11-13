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
