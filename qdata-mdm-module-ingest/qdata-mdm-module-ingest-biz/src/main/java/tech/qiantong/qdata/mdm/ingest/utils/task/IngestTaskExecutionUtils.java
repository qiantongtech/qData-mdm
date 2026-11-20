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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.utils.task;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.mapping.FieldDates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * 接入任务的来源标识及业务值规范化工具，不访问数据库。
 *
 * @author chentangwen
 */
public final class IngestTaskExecutionUtils {

    /**
     * 禁止实例化，仅提供静态转换方法。
     */
    private IngestTaskExecutionUtils() {
    }

    /**
     * 单字段使用规范化后的值，多字段按字段名排序后转义拼接。
     * 字符串保留大小写、空格和前导零，不截断超长标识。
     *
     * @param uniqueValues 来源唯一字段名及其原始值
     * @param maxLength 来源表标识列的字符长度，未报告长度时传 0
     * @return 来源记录标识
     * @throws ServiceException 唯一字段缺失、值无效或标识超过列长度
     */
    public static String stableRecordId(Map<String, Object> uniqueValues, int maxLength) {
        if (uniqueValues == null || uniqueValues.isEmpty()) {
            throw new ServiceException("请先配置来源记录唯一标识后再执行");
        }
        StringJoiner parts = new StringJoiner("|");
        for (Object value : new TreeMap<>(uniqueValues).values()) {
            String text = identityText(value);
            if (text.trim().isEmpty()) {
                throw new ServiceException("来源记录唯一标识字段值不能为空");
            }
            parts.add(uniqueValues.size() == 1 ? text : text.replace("\\", "\\\\").replace("|", "\\|"));
        }
        String recordId = parts.toString();
        if (maxLength > 0 && recordId.codePointCount(0, recordId.length()) > maxLength) {
            throw new ServiceException("来源记录唯一标识超过 source_record_id 的 " + maxLength + " 字符限制");
        }
        return recordId;
    }

    /**
     * 将映射后非空的日期值转为业务时区文本，供写入时按物理列类型绑定。
     *
     * @param values 本次映射生成的可修改字段值，不补充未映射字段
     * @param fields 目标模型业务字段
     * @throws ServiceException 日期值不受支持或无法转换
     */
    public static void normalizeModelDates(Map<String, Object> values, List<IngestModelFieldRespVO> fields) {
        for (IngestModelFieldRespVO field : fields) {
            String column = field.getColumnCode();
            Object value = values.get(column);
            if (value == null || field.getDataType() == null) {
                continue;
            }
            String type = field.getDataType().toUpperCase(Locale.ROOT);
            if ("DATE".equals(type)) {
                values.put(column, FieldDates.parse(value, null).toLocalDate().toString());
            } else if ("DATETIME".equals(type) || "TIMESTAMP".equals(type)) {
                values.put(column, FieldDates.parse(value, null).toLocalDateTime().toString());
            }
        }
    }

    /**
     * 将来源唯一字段转成稳定文本：字符串保留原样，纯日期保留日历日期，时间点转为 UTC。
     *
     * @param value 来源唯一字段的原始值
     * @return 用于拼接来源记录标识的字段文本
     * @throws ServiceException 值为空、类型不受支持或无法规范化
     */
    private static String identityText(Object value) {
        if (value == null) throw new ServiceException("来源记录唯一标识字段值不能为空");
        if (value instanceof Number) return normalizedNumber((Number) value).toPlainString();
        if (value instanceof Boolean || value instanceof String || value instanceof Character) return value.toString();
        if (value instanceof java.sql.Date || value instanceof LocalDate) {
            return FieldDates.parse(value, null).toLocalDate().toString();
        }
        if (value instanceof Date || value instanceof TemporalAccessor) {
            return FieldDates.parse(value, null).toInstant().toString();
        }
        throw new ServiceException("来源标识包含不支持的值类型");
    }

    /**
     * 去掉数值小数部分末尾的零，使数值相等的标识字段生成相同文本。
     *
     * @param value 来源唯一字段的数值
     * @return 去除尾随零的数值，零统一返回 {@link BigDecimal#ZERO}
     * @throws ServiceException 数值不能表示为有限小数
     */
    private static BigDecimal normalizedNumber(Number value) {
        try {
            BigDecimal number = new BigDecimal(value.toString()).stripTrailingZeros();
            return number.signum() == 0 ? BigDecimal.ZERO : number;
        } catch (NumberFormatException ex) {
            throw new ServiceException("来源数据包含非有限数值");
        }
    }
}
