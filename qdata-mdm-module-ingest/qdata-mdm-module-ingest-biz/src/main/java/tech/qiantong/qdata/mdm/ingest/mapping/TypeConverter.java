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

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;

/**
 * 数据接入字段的目标模型类型转换规则。
 *
 * <p>由 {@link TypeConverters} 根据目标模型字段的类型、长度和小数精度创建，
 * 日期输入格式由映射配置提供或使用默认值。
 * {@link FieldMappingPlan} 将本规则与来源取值、{@link FieldProcessor} 组合，
 * 在字段处理完成后执行目标类型转换，逐行执行时复用已准备的规则。</p>
 *
 * <p>本接口负责字段值的类型转换及相应的长度、精度校验，
 * 不负责非空、唯一性等其他模型约束，也不访问数据库。</p>
 *
 * @author chentangwen
 */
@FunctionalInterface
public interface TypeConverter {

    /**
     * 将字段值转换为目标模型字段对应的 Java 类型，并校验长度或数值精度。
     *
     * <p>输入为 {@code null} 时直接返回 {@code null}，不进行非空校验。
     * 非空值无法转换或超出允许范围时抛出异常，不静默截断字符串、舍入小数或返回原值。
     * 转换失败的异常消息不应包含原始字段值。</p>
     *
     * @param value 字段处理后的待转换值，可以为 {@code null}
     * @return 符合目标类型及相应长度、精度要求的值；输入为 {@code null} 时返回 {@code null}
     * @throws ServiceException 字段值类型不受支持、格式不合法、数值溢出，
     *                          或不符合目标字段长度、精度要求时抛出
     */
    Object convert(Object value);
}
