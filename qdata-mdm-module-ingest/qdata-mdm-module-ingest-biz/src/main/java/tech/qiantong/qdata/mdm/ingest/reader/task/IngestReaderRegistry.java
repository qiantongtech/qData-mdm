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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按接入类型注册 Spring 注入的读取策略，供读取服务统一分派。
 *
 * @author chentangwen
 */
@Component
public class IngestReaderRegistry {
    private final Map<String, IngestReader> readers = new HashMap<>();

    /**
     * 启动时建立类型到读取器的映射，重复类型直接报错，避免策略被覆盖。
     *
     * @param strategies Spring 注入的全部读取策略
     */
    public IngestReaderRegistry(List<IngestReader> strategies) {
        for (IngestReader strategy : strategies) {
            if (readers.putIfAbsent(strategy.getType(), strategy) != null) {
                throw new IllegalStateException("重复的接入读取类型：" + strategy.getType());
            }
        }
    }

    /**
     * 根据任务接入类型取得读取器，未注册的类型按配置错误处理。
     *
     * @param type 接入类型
     * @return 对应的读取策略
     */
    public IngestReader get(String type) {
        IngestReader reader = readers.get(type);
        if (reader == null) {
            throw new ServiceException("接入类型无效");
        }
        return reader;
    }
}
