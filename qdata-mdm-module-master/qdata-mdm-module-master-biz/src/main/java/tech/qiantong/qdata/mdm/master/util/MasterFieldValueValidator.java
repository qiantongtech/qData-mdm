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

package tech.qiantong.qdata.mdm.master.util;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/** 主模型、子模型共用的字段值格式校验。 */
public final class MasterFieldValueValidator {
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private MasterFieldValueValidator() {
    }

    public static boolean valid(String dataType, String value, Long length, Long scaleValue) {
        String type = dataType == null ? "" : dataType.toUpperCase(Locale.ROOT);
        if (type.matches("^(BIGINT|INTEGER|SMALLINT|INT|TINYINT)(\\(.*\\))?$")) {
            return INTEGER.matcher(value).matches()
                    && (length == null || value.replace("-", "").length() <= length);
        }
        if (type.matches(".*(DECIMAL|NUMBER|NUMERIC|FLOAT|DOUBLE).*")) {
            if (!DECIMAL.matcher(value).matches()) return false;
            String[] parts = value.replace("-", "").split("\\.", -1);
            int integerDigits = parts[0].length();
            int fractionDigits = parts.length > 1 ? parts[1].length() : 0;
            long scale = scaleValue == null ? 0 : scaleValue;
            return length == null || (integerDigits + fractionDigits <= length
                    && integerDigits <= length - scale && fractionDigits <= scale);
        }
        if (type.contains("DATE") || type.contains("TIME")) {
            try {
                if (type.contains("TIME")) {
                    LocalDateTime.parse(value.length() == 16 ? value + ":00" : value, DATE_TIME);
                } else {
                    LocalDate.parse(value);
                }
                return true;
            } catch (DateTimeException ex) {
                return false;
            }
        }
        return length == null || value.codePointCount(0, value.length()) <= length;
    }
}
