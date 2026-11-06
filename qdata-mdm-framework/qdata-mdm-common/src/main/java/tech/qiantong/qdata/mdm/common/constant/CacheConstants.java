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

package tech.qiantong.qdata.mdm.common.constant;

/**
 * Cache key constant
 *
 * @author qdata
 */
public class CacheConstants
{
    /**
     * Login user redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * Verification code redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * Parameter management cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * Dictionary management cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * Anti-resubmit redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * Current limiting redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * Number of incorrect login account passwords redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * Number of incorrect login account passwords redis key
     */
    public static final String RP_PWD_ERR_CNT_KEY = "rp_pwd_err_cnt:";

    /**
     * Data asset preview
     */
    public static final String ASSET_PREVIEW_KEY = "asset_preview:";
}
