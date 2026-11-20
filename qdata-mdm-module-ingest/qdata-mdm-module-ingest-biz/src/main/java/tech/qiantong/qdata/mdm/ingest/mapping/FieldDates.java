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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

/**
 * 数据接入字段映射的日期解析工具。
 *
 * <p>字符串按指定格式严格校验日历日期，解析结果统一为上海时区。
 * 已包含时区或偏移量的值保持同一时刻转换；本地日期时间按上海时区解释，
 * 纯日期取该时区当日的起始时间。</p>
 *
 * @author chentangwen
 */
public final class FieldDates {

    /**
     * 数据接入使用的业务时区。
     */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private FieldDates() {
    }

    /**
     * 创建可复用的日期格式器，使用固定语言环境并启用严格解析。
     *
     * <p>支持 {@code yyyy} 和 {@code uuuu} 等 Java 日期格式表达式；
     * 格式器创建成功仅表示表达式合法，解析时仍需提供足以确定日期的内容。</p>
     *
     * @param pattern 日期格式表达式，不能为 {@code null} 或空白
     * @return 线程安全的日期格式器
     * @throws ServiceException 日期格式未配置或表达式不合法时抛出
     */
    public static DateTimeFormatter formatter(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new ServiceException("请配置日期格式");
        }
        try {
            // yyyy 表示纪元内年份，严格模式需补充公元纪元，不能用字符串替换破坏字面量。
            return new DateTimeFormatterBuilder()
                    .appendPattern(pattern)
                    .parseDefaulting(ChronoField.ERA, 1)
                    .toFormatter(Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("日期格式配置不正确");
        }
    }

    /**
     * 将字段值转换为业务时区的日期时间。
     *
     * <p>支持字符串、{@link Date}（含 JDBC 日期和时间戳，不含仅有时间的
     * {@link java.sql.Time}）、{@link Instant}、{@link ZonedDateTime}、
     * {@link OffsetDateTime}、{@link LocalDateTime} 和 {@link LocalDate}。
     * 空值由调用方处理，本方法不返回 {@code null}。</p>
     *
     * @param value 待转换的非空字段值
     * @param input 字符串的解析格式器；非字符串值不使用该参数，允许为 {@code null}
     * @return 统一为业务时区的日期时间
     * @throws ServiceException 值为空、类型不支持、字符串缺少格式器，或日期无效、超出范围时抛出
     */
    public static ZonedDateTime parse(Object value, DateTimeFormatter input) {
        try {
            if (value instanceof java.sql.Time) {
                throw new ServiceException("仅包含时间的值无法转换为日期");
            }
            // JDBC 日期不支持 toInstant()，必须在 java.util.Date 之前单独处理。
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate().atStartOfDay(ZONE);
            }
            if (value instanceof Date) {
                return ((Date) value).toInstant().atZone(ZONE);
            }
            if (value instanceof ZonedDateTime) {
                return ((ZonedDateTime) value).withZoneSameInstant(ZONE);
            }
            if (value instanceof OffsetDateTime) {
                return ((OffsetDateTime) value).atZoneSameInstant(ZONE);
            }
            if (value instanceof Instant) {
                return ((Instant) value).atZone(ZONE);
            }
            if (value instanceof LocalDateTime) {
                return ((LocalDateTime) value).atZone(ZONE);
            }
            if (value instanceof LocalDate) {
                return ((LocalDate) value).atStartOfDay(ZONE);
            }
            if (!(value instanceof String)) {
                throw new ServiceException("无法转换为日期");
            }
            if (input == null) {
                throw new ServiceException("请配置日期格式");
            }
            TemporalAccessor parsed = input.parseBest((String) value, ZonedDateTime::from,
                    OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
            return parse(parsed, input);
        } catch (DateTimeException ex) {
            throw new ServiceException("日期值与格式不匹配或日期无效");
        }
    }
}
