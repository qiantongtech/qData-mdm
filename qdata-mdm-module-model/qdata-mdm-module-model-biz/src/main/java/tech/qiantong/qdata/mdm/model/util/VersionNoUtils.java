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

package tech.qiantong.qdata.mdm.model.util;

import java.math.BigDecimal;

/** 统一生成和比较模型、编码方案使用的 V主版本.次版本号。 */
public final class VersionNoUtils {

    private VersionNoUtils() {
    }

    /** 返回初始版本或当前版本的下一个版本，统一使用 V1.0、V1.1 规则。 */
    public static String next(String currentVersion) {
        BigDecimal current = parse(currentVersion);
        if (current == null) return "V1.0";
        int major = current.intValue();
        int minor = current.subtract(BigDecimal.valueOf(major)).movePointRight(1).intValue();
        if (minor >= 9) {
            major++;
            minor = 0;
        } else {
            minor++;
        }
        return "V" + major + "." + minor;
    }

    /** 将版本号转换为可比较数值；无法识别的历史格式按空值处理。 */
    public static BigDecimal parse(String version) {
        if (version == null || version.trim().isEmpty()) return null;
        String value = version.trim();
        if (value.startsWith("V") || value.startsWith("v")) value = value.substring(1);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
