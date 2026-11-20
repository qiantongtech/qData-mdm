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

import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.List;
import java.util.function.Consumer;

/**
 * 数据库、文件和 API 的统一读取契约。
 *
 * <p>由 {@link IngestReaderRegistry} 按接入类型选择实现，供来源预览和正式采集共用。
 * 读取器负责访问来源、解析记录和管理来源资源；字段转换、目标入库、事务及逐行错误策略由调用方负责。</p>
 *
 * <p>每次读取使用独立的 {@link IngestReadContext}，上下文不得在多个读取任务间并发共用。
 * 调用方应在读取开始前完成任务配置，读取期间不再修改配置。</p>
 *
 * @author chentangwen
 */
public interface IngestReader {

    /**
     * 返回策略注册和查找使用的接入类型。
     *
     * <p>类型应为稳定、非空白的值，并在所有已注册读取器中唯一。
     * 现有类型为 {@code DATABASE}、{@code FILE} 和 {@code API}，与任务配置的类型一致。</p>
     *
     * @return 当前读取器支持的接入类型，不为 {@code null}
     */
    String getType();

    /**
     * 同步读取来源记录，逐条回调，并返回本次读取获得的来源字段列表。
     *
     * <p>回调在当前读取线程中顺序执行，上一条回调返回后才回调下一条记录。
     * 按上下文已完成的回调数判断上限，小于或等于 0 表示不限量，跳过的空记录不占上限。
     * 达到上限只表示停止后续记录的回调，不保证来源剩余内容已经读取或校验。</p>
     *
     * <p>回调正常返回后，上下文才更新读取计数和候选增量位置，读取计数不等同于入库成功数。
     * 回调抛出的运行时异常原样向调用方传播并中止读取，已完成回调的业务操作不会由读取器自动回滚。
     * 读取器在正常结束或异常退出时释放本次打开的来源资源，并在读取过程中检查线程中断。</p>
     *
     * <p>字段列表的含义随来源类型而异：</p>
     * <ul>
     *     <li>数据库：来源对象的完整字段列表，不限于实际查询的数据列。</li>
     *     <li>文件：规范化后的表头，按文件列顺序排列。</li>
     *     <li>API：已正常完成回调记录的顶层字段并集，按首次出现顺序排列；
     *     未回调任何记录时为空列表，不代表接口的完整数据结构。</li>
     * </ul>
     *
     * @param context 本次读取的任务配置、回调条数上限和进度，不可为 {@code null}
     * @param consumer 逐条消费 {@link IngestRow} 的同步回调，不可为 {@code null}
     * @return 来源字段列表，不为 {@code null}；具体含义见上述来源类型约定
     * @throws ServiceException 参数或来源配置无效、来源访问或解析失败，以及检测到读取中断时抛出
     * @throws RuntimeException 回调处理失败时原样抛出，交由调用方执行失败策略
     */
    List<String> read(IngestReadContext context, Consumer<IngestRow> consumer);
}
