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

package tech.qiantong.qdata.mdm.ingest.service.task;

import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestFileUploadRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskPreviewRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestRow;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestReadContext;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 接入任务来源读取接口，统一上传、预览和执行时的数据读取入口。
 *
 * @author chentangwen
 */
public interface IIngestTaskReadService {

    /**
     * 按接入类型读取来源数据，并同步交给调用方处理。
     *
     * @param task 接入任务配置
     * @param limit 最大读取行数，小于或等于 0 时不限制
     * @param consumer 逐行处理回调，按来源字段名访问数据
     * @return 读取到的来源字段名，保留来源顺序
     */
    List<String> read(IngestTaskSaveReqVO task, int limit, Consumer<Map<String, Object>> consumer);

    /**
     * 按任务类型分派读取策略，将来源值和位置一起交给执行链路。
     *
     * @param context 本次读取配置、限制和统计信息
     * @param consumer 同步处理单条来源记录的回调
     * @return 来源字段列表；API 返回已回调记录的字段并集
     */
    List<String> readRows(IngestReadContext context, Consumer<IngestRow> consumer);

    /**
     * 读取来源字段和前 20 条数据用于配置预览。
     *
     * @param task 接入任务配置
     * @return 来源字段及预览数据
     */
    IngestTaskPreviewRespVO preview(IngestTaskSaveReqVO task);

    /**
     * 保存接入文件并校验表头，文件配置只返回存储令牌。
     *
     * @param file CSV、XLS 或 XLSX 文件
     * @return 文件配置及字段列表
     */
    IngestFileUploadRespVO upload(MultipartFile file);
}
