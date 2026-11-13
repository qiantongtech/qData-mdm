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

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import java.time.*;
import java.time.format.*;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;

/**
 * 日期处理统一使用业务时区，严格校验日历日期，避免依赖服务器默认时区。
 */
public final class FieldDates {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private FieldDates() { }

    public static DateTimeFormatter formatter(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new ServiceException("请配置日期格式");
        }
        try {
            // yyyy 表示纪元内年份，严格模式需补充公元纪元，不能用字符串替换破坏字面量。
            return new DateTimeFormatterBuilder().appendPattern(pattern)
                    .parseDefaulting(ChronoField.ERA, 1).toFormatter(Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("日期格式配置不正确");
        }
    }

    public static ZonedDateTime parse(Object value, DateTimeFormatter input) {
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate().atStartOfDay(ZONE);
        if (value instanceof Date) return ((Date) value).toInstant().atZone(ZONE);
        if (value instanceof ZonedDateTime) return ((ZonedDateTime) value).withZoneSameInstant(ZONE);
        if (value instanceof OffsetDateTime) return ((OffsetDateTime) value).atZoneSameInstant(ZONE);
        if (value instanceof Instant) return ((Instant) value).atZone(ZONE);
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).atZone(ZONE);
        if (value instanceof LocalDate) return ((LocalDate) value).atStartOfDay(ZONE);
        if (!(value instanceof String)) throw new ServiceException("无法转换为日期");
        try {
            TemporalAccessor parsed = input.parseBest((String) value, ZonedDateTime::from,
                    OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
            return parse(parsed, input);
        } catch (DateTimeException ex) {
            throw new ServiceException("日期值与格式不匹配或日期无效");
        }
    }
}
