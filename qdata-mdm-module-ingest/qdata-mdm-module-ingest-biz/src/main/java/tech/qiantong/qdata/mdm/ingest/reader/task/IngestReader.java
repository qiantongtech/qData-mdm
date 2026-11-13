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

import java.util.List;
import java.util.function.Consumer;

/**
 * 数据库、文件和 API 的统一读取契约。
 * 读取器负责解析来源数据并同步回调，字段转换和入库由调用方处理。
 *
 * @author chentangwen
 */
public interface IngestReader {
    /**
     * 返回策略注册使用的接入类型：DATABASE、FILE 或 API。
     *
     * @return 接入类型
     */
    String getType();

    /**
     * 读取来源记录，达到上下文限制后停止，并在结束或异常时释放来源资源。
     * 回调同步执行，回调返回后才继续读取下一条记录。
     *
     * @param context 本次读取的任务配置、行数限制和统计信息
     * @param consumer 逐条消费来源值和定位信息的回调
     * @return 来源字段列表；API 为已回调记录的字段并集，按首次出现顺序排列
     */
    List<String> read(IngestReadContext context, Consumer<IngestRow> consumer);
}
