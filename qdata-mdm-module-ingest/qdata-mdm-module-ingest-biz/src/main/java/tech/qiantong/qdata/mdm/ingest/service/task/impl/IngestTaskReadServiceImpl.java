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

package tech.qiantong.qdata.mdm.ingest.service.task.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
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
 * 负责接入任务的来源读取、配置预览和文件上传校验。
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
     * 同步读取来源记录，逐条将原始字段值交给调用方，不传递来源位置及增量信息。
     *
     * @param task 接入任务配置
     * @param limit 最大回调条数，小于或等于 0 时不限制条数
     * @param consumer 逐条处理来源记录的回调，按来源字段名访问原始字段值
     * @return 来源字段名称列表
     */
    @Override
    public List<String> read(IngestTaskSaveReqVO task, int limit, Consumer<Map<String, Object>> consumer) {
        return readRows(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    /**
     * 按任务的接入类型选择读取器，同步向调用方传递完整来源记录。
     * 记录除原始字段值外，还保留来源位置和增量字段原值。
     *
     * @param context 本次读取的任务配置、条数限制和进度信息
     * @param consumer 逐条处理来源记录的回调
     * @return 来源字段名称列表；API 来源返回已回调记录中出现的字段并集
     * @throws ServiceException 接入类型未注册或来源读取失败
     */
    @Override
    public List<String> readRows(IngestReadContext context, Consumer<IngestRow> consumer) {
        return readerRegistry.get(context.getTask().getType()).read(context, consumer);
    }

    /**
     * 预览来源字段和最多 20 条来源记录，不创建运行实例或写入实体来源表。
     *
     * @param task 待预览的接入任务配置
     * @return 来源字段名称和预览数据
     */
    @Override
    public IngestTaskPreviewRespVO preview(IngestTaskSaveReqVO task) {
        List<Map<String, Object>> rows = new ArrayList<>();
        IngestTaskPreviewRespVO result = new IngestTaskPreviewRespVO();
        result.setFields(readRows(new IngestReadContext(task, PREVIEW_ROW_LIMIT, null, true),
                row -> rows.add(row.getValues())));
        result.setRows(rows);
        return result;
    }

    /**
     * 保存接入文件并试读表头及最多一条来源记录，不在上传阶段逐行校验整个文件。
     * 保存或试读失败时，尝试清理本次上传的文件。
     *
     * @param file 非空且不超过 50 MB 的 CSV、XLS 或 XLSX 文件
     * @return 文件令牌、文件信息及来源字段名称列表
     * @throws ServiceException 文件不符合上传要求，或保存、解析失败
     */
    @Override
    public IngestFileUploadRespVO upload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ServiceException("文件不能为空且不能超过50MB", HttpStatus.WARN);
        }
        String name = file.getOriginalFilename();
        String suffix =
                name == null || !name.contains(".")
                        ? ""
                        : name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!Arrays.asList("csv", "xls", "xlsx").contains(suffix)) {
            throw new ServiceException("仅支持CSV、XLS、XLSX文件", HttpStatus.WARN);
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
            // filePath 沿用原字段名，对外仅返回文件令牌，不暴露实际存储路径。
            result.setFilePath(token);
            result.setFileName(name);
            result.setFileType(config.getFileType());
            result.setFileSize(Files.size(path));
            result.setUploadTime(new Date());
            // 试读时只保留字段名称，不收集样例数据。
            result.setFields(fileReader.read(config, 1, row -> {
            }));
            return result;
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException cleanup) {
                // 附带清理异常，避免覆盖上传或解析失败的原始原因。
                ex.addSuppressed(cleanup);
            }
            ServiceException failure = new ServiceException("文件上传或解析失败，请检查文件格式、编码和表头");
            failure.initCause(ex);
            throw failure;
        }
    }
}
