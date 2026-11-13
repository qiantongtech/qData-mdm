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

package tech.qiantong.qdata.mdm.ingest.service.task.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestFileUploadRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskFileConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskPreviewRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestFileReader;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestReadContext;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestReaderRegistry;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestRow;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskReadService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Resource;


/**
 * 接入任务读取服务，按接入类型分派读取器并组装预览结果。
 *
 * @author chentangwen
 */
@Service
public class IngestTaskReadServiceImpl implements IIngestTaskReadService {

    private static final int PREVIEW_ROW_LIMIT = 20;
    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

    @Resource
    private IngestReaderRegistry readerRegistry;

    @Resource
    private IngestFileReader fileReader;

    /**
     * 按接入类型同步读取来源记录，读取器返回字段列表并逐行触发回调。
     *
     * @param task 接入任务配置
     * @param limit 最大读取行数，小于或等于 0 时不限制
     * @param consumer 逐行处理回调
     * @return 来源字段列表
     */
    @Override
    public List<String> read(IngestTaskSaveReqVO task, int limit, Consumer<Map<String, Object>> consumer) {
        return readRows(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    @Override
    public List<String> readRows(IngestReadContext context, Consumer<IngestRow> consumer) {
        return readerRegistry.get(context.getTask().getType()).read(context, consumer);
    }

    /**
     * 预览来源字段和前 20 条数据，不写入运行记录或实体来源数据。
     *
     * @param task 接入任务配置
     * @return 字段列表及预览数据
     */
    @Override
    public IngestTaskPreviewRespVO preview(IngestTaskSaveReqVO task) {
        List<Map<String, Object>> rows = new ArrayList<>();
        IngestTaskPreviewRespVO result = new IngestTaskPreviewRespVO();
        result.setFields(read(task, PREVIEW_ROW_LIMIT, rows::add));
        result.setRows(rows);
        return result;
    }

    /**
     * 保存文件并校验表头；解析失败时删除本次上传文件。
     *
     * @param file CSV、XLS 或 XLSX 文件
     * @return 文件配置及来源字段
     */
    @Override
    public IngestFileUploadRespVO upload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ServiceException("文件不能为空且不能超过50MB");
        }
        String name = file.getOriginalFilename();
        String suffix =
                name == null || !name.contains(".")
                        ? ""
                        : name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!Arrays.asList("csv", "xls", "xlsx").contains(suffix)) {
            throw new ServiceException("仅支持CSV、XLS、XLSX文件");
        }
        String token = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        Path path = fileReader.resolveFile(token);
        try {
            Files.createDirectories(path.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, path);
            }
            IngestTaskFileConfigVO config = new IngestTaskFileConfigVO();
            config.setFileToken(token);
            config.setFileType(suffix.toUpperCase(Locale.ROOT));
            IngestFileUploadRespVO result = new IngestFileUploadRespVO();
            result.setFileToken(token);
            result.setFilePath(token);
            result.setFileName(name);
            result.setFileType(config.getFileType());
            result.setFileSize(Files.size(path));
            result.setUploadTime(new Date());
            // 只验证表头和首条数据，不保留上传阶段的预览内容。
            result.setFields(fileReader.read(config, 1, row -> {
            }));
            return result;
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException cleanup) {
                ex.addSuppressed(cleanup);
            }
            ServiceException failure = new ServiceException("文件上传或解析失败，请检查文件格式、编码和表头");
            failure.initCause(ex);
            throw failure;
        }
    }
}
