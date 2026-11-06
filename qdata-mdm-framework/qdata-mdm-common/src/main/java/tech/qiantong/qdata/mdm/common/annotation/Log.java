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

import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.enums.OperatorType;

import java.lang.annotation.*;

/**
 * Custom operation logging annotations
 *
 * @author qdata
 *
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log
{
    /**
     * Module
     */
    public String title() default "";

    /**
     * Function
     */
    public BusinessType businessType() default BusinessType.OTHER;

    /**
     * Operator category
     */
    public OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * Whether to save requested parameters
     */
    public boolean isSaveRequestData() default true;

    /**
     * Whether to save response parameters
     */
    public boolean isSaveResponseData() default true;

    /**
     * Exclude specified request parameters
     */
    public String[] excludeParamNames() default {};
}
