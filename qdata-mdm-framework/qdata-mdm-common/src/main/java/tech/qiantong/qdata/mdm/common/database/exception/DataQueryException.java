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

package tech.qiantong.qdata.mdm.common.database.exception;

import tech.qiantong.qdata.mdm.common.utils.MessageUtils;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;

/**
 * Data query exception.
 * Supports i18n internationalization: prioritizes retrieving localized text from resource files.
 *
 * @author qdata
 */
public class DataQueryException extends RuntimeException {

    /**
     * i18n message key
     */
    private String i18nCode;

    /**
     * Message formatting arguments
     */
    private Object[] args;

    /**
     * Plain text constructor
     */
    public DataQueryException(String message) {
        super(message);
    }

    /**
     * Construct with i18n message key + fallback message
     *
     * @param i18nCode       message key (e.g., "db.error.connection")
     * @param defaultMessage fallback message
     * @param args           formatting arguments
     */
    public DataQueryException(String i18nCode, String defaultMessage, Object... args) {
        super(defaultMessage);
        this.i18nCode = i18nCode;
        this.args = args;
    }

    public DataQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        String defaultMsg = super.getMessage();
        if (!StringUtils.isEmpty(i18nCode)) {
            return MessageUtils.messageWithFallback(i18nCode, defaultMsg, args);
        }
        return defaultMsg;
    }
}
