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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.Map;

/**
 * 单条来源记录，定位信息独立于来源值，不参与字段映射。
 *
 * <p>由 {@link IngestReadContext#emit} 封装后同步传给读取回调，仅承载原始数据及附带信息。
 * 字段转换、唯一标识校验、来源记录 ID 计算和增量位置持久化由后续处理负责。</p>
 *
 * <p>原始数据保留来源 Map 引用，不复制字段值，调用方应按只读数据使用。</p>
 *
 * @author chentangwen
 */
@Getter
public class IngestRow {

    /**
     * 以来源字段名为键的原始数据，不为 null，允许无字段或字段值为 null。
     * API 嵌套对象及数组保留原始结构，本类不展开路径或转换字段值。
     */
    private final Map<String, Object> values;
    /**
     * 来源位置描述，例如文件物理行号、API 页内序号或数据库读取序号，允许为 null。
     * 仅供日志和错误提示使用，不作为来源记录唯一标识。
     */
    private final String location;
    /**
     * 当前记录的数据库增量字段原值，保留来源类型；非增量读取或字段缺失、值为空时为 null。
     * 该值不代表本条记录已入库，也不代表增量位置已经保存。
     */
    private final Object incrementValue;

    /**
     * 创建单条来源记录，校验容器引用后保留全部原始值，不复制或包装 Map。
     *
     * @param values 来源字段及原值，不可为 {@code null}
     * @param location 来源位置描述，允许为 {@code null}
     * @param incrementValue 当前记录的增量原值，允许为 {@code null}
     * @throws ServiceException 来源数据 Map 为空引用
     */
    public IngestRow(Map<String, Object> values, String location, Object incrementValue) {
        if (ObjectUtil.isNull(values)) {
            throw new ServiceException("来源记录不能为空");
        }
        this.values = values;
        this.location = location;
        this.incrementValue = incrementValue;
    }
}
