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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按接入类型注册 Spring 注入的读取策略，供读取服务统一分派。
 *
 * <p>构造时校验并建立类型索引，完成后仅供查询，不支持运行期间增删或替换策略。
 * 类型按原值精确匹配，不裁剪空格或转换大小写；新增类型通过实现并注册 {@link IngestReader} 扩展。</p>
 *
 * <p>注册表只保存读取器引用，不保存任务配置和读取进度。
 * 注册表可供多个任务并发查询，读取器自身仍须保证每次读取的状态相互独立。</p>
 *
 * @author chentangwen
 */
@Component
public class IngestReaderRegistry {

    /** 构造完成后不可修改的类型索引，不对外暴露。 */
    private final Map<String, IngestReader> readers;

    /**
     * 启动时校验并注册读取策略，配置错误直接阻止注册表创建。
     *
     * <p>空列表可创建不含策略的注册表，查询时按未注册类型处理。
     * 每个读取器只获取一次类型；类型须稳定、非空白且唯一，不限制为内置的三种类型。</p>
     *
     * @param strategies Spring 注入的全部读取策略，不可为 {@code null}，不允许包含空元素
     * @throws IllegalStateException 策略列表或元素为空、类型为空白或类型重复时抛出
     */
    public IngestReaderRegistry(List<IngestReader> strategies) {
        if (ObjectUtil.isNull(strategies)) {
            throw new IllegalStateException("接入读取策略列表不能为空");
        }
        Map<String, IngestReader> registered = new HashMap<>();
        for (IngestReader strategy : strategies) {
            if (ObjectUtil.isNull(strategy)) {
                throw new IllegalStateException("接入读取策略不能为空");
            }
            String type = strategy.getType();
            if (ObjectUtil.isNull(type) || type.trim().isEmpty()) {
                throw new IllegalStateException("接入读取类型不能为空，策略：" + strategy.getClass().getName());
            }
            // 同一类型只允许一个实现，报告双方类名以便定位启动配置冲突。
            IngestReader previous = registered.putIfAbsent(type, strategy);
            if (ObjectUtil.isNotNull(previous)) {
                throw new IllegalStateException("重复的接入读取类型：" + type + "，冲突策略："
                        + previous.getClass().getName() + "、" + strategy.getClass().getName());
            }
        }
        // 局部映射不再向外暴露，发布后仅查询，避免运行期间修改注册关系。
        this.readers = Collections.unmodifiableMap(registered);
    }

    /**
     * 按任务接入类型精确查找读取器，不创建实例或触发来源读取。
     *
     * @param type 与 {@link IngestReader#getType()} 一致的接入类型
     * @return 启动时注册的读取器实例，不为 {@code null}
     * @throws ServiceException 类型为空或未注册时抛出
     */
    public IngestReader get(String type) {
        IngestReader reader = readers.get(type);
        if (ObjectUtil.isNull(reader)) {
            throw new ServiceException("接入类型无效");
        }
        return reader;
    }
}
