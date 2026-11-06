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

package tech.qiantong.qdata.mdm.common.utils.object;

import cn.hutool.core.bean.BeanUtil;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.collection.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bean utility class
 *
 * 1. By default, {@link BeanUtil} is used as the implementation class. Although the performance of different bean tools is different, for most students’ projects, you don’t need to worry about this performance.
 * 2. For complex object conversion, you can search for reference AuthConvert implementation, which is implemented through mapstruct + default
 *
 * @author taro source code
 */
public class BeanUtils {

    public static <T> T toBean(Object source, Class<T> targetClass) {
        return BeanUtil.toBean(source, targetClass);
    }

    public static <T> T toBean(Object source, Class<T> targetClass, Consumer<T> peek) {
        T target = toBean(source, targetClass);
        if (target != null) {
            peek.accept(target);
        }
        return target;
    }

    public static <S, T> List<T> toBean(List<S> source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        return CollectionUtils.convertList(source, s -> toBean(s, targetType));
    }

    public static <S, T> List<T> toBean(List<S> source, Class<T> targetType, Consumer<T> peek) {
        List<T> list = toBean(source, targetType);
        if (list != null) {
            list.forEach(peek);
        }
        return list;
    }

    public static <S, T> PageResult<T> toBean(PageResult<S> source, Class<T> targetType) {
        return toBean(source, targetType, null);
    }

    public static <S, T> PageResult<T> toBean(PageResult<S> source, Class<T> targetType, Consumer<T> peek) {
        if (source == null) {
            return null;
        }
        List<T> list = toBean(source.getRows(), targetType);
        if (peek != null) {
            list.forEach(peek);
        }
        return new PageResult<>(list, source.getTotal());
    }

}
