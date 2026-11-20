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
 * 数据接入字段的单项处理规则。
 *
 * <p>由 {@link FieldProcessors} 根据映射配置创建，支持去空格、空值替换、固定值、
 * 值映射、日期处理及配置指定的类型转换等操作。
 * {@link FieldMappingPlan} 将本规则与来源取值、目标模型转换组合为完整的字段映射流程，
 * 最终模型类型及约束由后续 {@link TypeConverter} 校验。</p>
 *
 * @author chentangwen
 */
@FunctionalInterface
public interface FieldProcessor {

    /**
     * 按已配置的单项规则处理字段值。
     *
     * <p>输入和输出均可为 {@code null}，空值的保留、替换或拒绝由具体规则决定。
     * 返回值的类型可以与输入不同；固定值规则可忽略输入，直接返回预先计算的值。</p>
     *
     * @param value 待处理的字段值，可以为 {@code null}
     * @return 处理后的字段值，可以为 {@code null}，不保证与输入类型一致
     * @throws ServiceException 字段值不符合当前规则要求或规则中的类型转换失败时抛出
     */
    Object process(Object value);
}
