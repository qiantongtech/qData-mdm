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

package tech.qiantong.qdata.mdm.master.service.data.impl;

import org.apache.poi.ss.usermodel.Cell;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataMetadataRespVO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataExportService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.util.MasterStatusLabels;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.excel.MasterEditableWorkbook;
import tech.qiantong.qdata.mdm.master.service.data.reference.MasterReferenceService.ReferenceValues;
import tech.qiantong.qdata.mdm.master.service.data.reference.MasterReferenceService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.servlet.http.HttpServletResponse;

/**
 * 按当前筛选条件导出正式物化数据；所有值写为文本，避免 BIGINT 精度丢失。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDataExportServiceImpl implements IMasterDataExportService {
    private static final int PAGE_SIZE = 100;
    private static final int MAX_ROWS = 5000;
    private static final List<String> HIDDEN_LIST_COLUMNS =
            Arrays.asList("UPDATED_BY", "UPDATE_BY", "UPDATED_TIME", "UPDATE_TIME", "REMARK");
    private static final List<String> TRAILING_COLUMN_ORDER =
            Arrays.asList("STATUS", "CREATED_BY", "CREATE_BY", "CREATED_TIME", "CREATE_TIME");
    private final IMasterDataService dataService;
    private final IMasterDataMetadataService metadataService;
    private final MasterReferenceService references;

    /**
     * 按当前筛选条件导出主数据。
     *
     * @param response 用于写入导出文件的 HTTP 响应
     * @param instanceId 模型物化实例 ID
     * @param filter 导出筛选条件
     */
    @Override
    public void export(HttpServletResponse response, Long instanceId,
                       MasterDataPageReqVO filter) throws IOException  {
        filter.setPageNum(1);
        filter.setPageSize(PAGE_SIZE);
        MasterDataMetadataRespVO definition = metadataService.getMetadata(instanceId, filter.getModelId());
        MasterDataRespVO first = dataService.getData(instanceId, filter);
        if (!definition.getModelVersionId().equals(first.getModelVersionId())) {
            throw new ServiceException("导出期间模型版本发生变化，请重试", HttpStatus.WARN);
        }
        long total = first.getTotal() == null ? 0L : first.getTotal();
        if (total > MAX_ROWS) {
            throw new ServiceException("当前筛选结果超过 5000 条，请缩小范围后导出", HttpStatus.WARN);
        }
        if (first.getRows() == null || first.getRows().size() != Math.min(total, PAGE_SIZE)) {
            throw new ServiceException("导出期间数据发生变化，请重试", HttpStatus.WARN);
        }
        // 与 qData-pro2 的导出一样，先按查询条件取完整结果，再写入 HttpServletResponse。
        // 当前模型的列来自元数据，不能使用仅支持固定 @Excel 注解字段的 ExcelUtil<T>。
        List<MasterColumnRespVO> columns = listOrderColumns(definition.getColumns());
        MasterModelRespDTO model = metadataService.getModel(instanceId, definition.getModelId());
        if (!definition.getModelVersionId().equals(model.getModelVersionId()))
            throw new ServiceException("导出期间模型版本发生变化，请重试", HttpStatus.WARN);
        Map<String, ReferenceValues> choices = references.load(model);
        List<Map<String, Object>> rows = new ArrayList<>(first.getRows());
        for (int page = 2; rows.size() < total; page++) {
            filter.setPageNum(page);
            MasterDataRespVO next = dataService.getData(instanceId, filter);
            long expected = Math.min(PAGE_SIZE, total - rows.size());
            if (!first.getModelVersionId().equals(next.getModelVersionId())
                    || next.getTotal() == null || next.getTotal() != total
                    || next.getRows() == null || next.getRows().size() != expected) {
                throw new ServiceException("导出期间数据发生变化，请重试", HttpStatus.WARN);
            }
            rows.addAll(next.getRows());
        }

        // 共享字符串兼容导出文件在 Excel/WPS 中编辑、保存后再次导入。
        SXSSFWorkbook book = new SXSSFWorkbook(new XSSFWorkbook(), 500, true, true);
        try {
            Sheet sheet = book.createSheet("正式数据");
            CellStyle textStyle = book.createCellStyle();
            textStyle.setDataFormat(book.createDataFormat().getFormat("@"));
            DataFormat dataFormat = book.createDataFormat();
            CellStyle dateStyle = book.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));
            CellStyle dateTimeStyle = book.createCellStyle();
            dateTimeStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd hh:mm:ss"));
            CellStyle creatorStyle = book.createCellStyle();
            creatorStyle.cloneStyleFrom(textStyle);
            creatorStyle.setWrapText(true);
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                header.createCell(i).setCellValue(columns.get(i).getLabel());
                sheet.setColumnWidth(i, 20 * 256);
            }
            MasterEditableWorkbook.attach(book, sheet, model, columns, choices, rows.size());
            int nextRow = writeRows(sheet, 1, columns, rows, textStyle, dateStyle, dateTimeStyle,
                    creatorStyle, choices);
            MasterEditableWorkbook.setStreamingSheetRange(sheet, nextRow - 1, columns.size() - 1);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            book.write(response.getOutputStream());
        } catch (ServiceException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("导出权威主数据失败，instanceId={}", instanceId, ex);
            throw new ServiceException("权威主数据导出失败");
        } finally {
            book.dispose();
            try {
                book.close();
            } catch (IOException ex) {
                log.warn("关闭权威主数据导出工作簿失败，instanceId={}", instanceId, ex);
            }
        }
    }

    private List<MasterColumnRespVO> listOrderColumns(List<MasterColumnRespVO> definitionColumns) {
        List<MasterColumnRespVO> ordered = new ArrayList<>();
        Set<String> included = new HashSet<>();
        appendColumns(ordered, included, definitionColumns,
                column -> "ID".equalsIgnoreCase(column.getProp()));
        appendColumns(ordered, included, definitionColumns,
                column -> Boolean.TRUE.equals(column.getBusinessField())
                        && !"ID".equalsIgnoreCase(column.getProp()));
        appendColumns(ordered, included, definitionColumns,
                column -> !Boolean.TRUE.equals(column.getBusinessField())
                        && column.getProp() != null
                        && !"ID".equalsIgnoreCase(column.getProp())
                        && !TRAILING_COLUMN_ORDER.contains(column.getProp().toUpperCase(Locale.ROOT))
                        && !HIDDEN_LIST_COLUMNS.contains(column.getProp().toUpperCase(Locale.ROOT)));
        for (String prop : TRAILING_COLUMN_ORDER) {
            appendColumns(ordered, included, definitionColumns,
                    column -> prop.equalsIgnoreCase(column.getProp()));
        }
        appendColumns(ordered, included, definitionColumns, column -> true);
        return ordered;
    }

    private void appendColumns(List<MasterColumnRespVO> ordered, Set<String> included,
            List<MasterColumnRespVO> definitionColumns,
            Predicate<MasterColumnRespVO> matches) {
        for (MasterColumnRespVO column : definitionColumns) {
            String prop = column.getProp();
            if (prop != null && matches.test(column) && included.add(prop.toUpperCase(Locale.ROOT))) {
                ordered.add(column);
            }
        }
    }

    private int writeRows(Sheet sheet, int index, List<MasterColumnRespVO> columns,
                          List<Map<String, Object>> rows, CellStyle textStyle, CellStyle dateStyle,
                          CellStyle dateTimeStyle, CellStyle creatorStyle, Map<String, ReferenceValues> choices) {
        if (rows == null) return index;
        for (Map<String, Object> data : rows) {
            Row row = sheet.createRow(index++);
            for (int i = 0; i < columns.size(); i++) {
                MasterColumnRespVO column = columns.get(i);
                Cell cell = row.createCell(i);
                if (isUserColumn(column)) {
                    String userType = isCreatorColumn(column) ? "CREATED_BY" : "UPDATED_BY";
                    String name = text(data.get(userType + "_NAME"));
                    if (name.isEmpty()) name = text(data.get(column.getProp()));
                    String phone = text(data.get(userType + "_PHONE"));
                    cell.setCellStyle(creatorStyle);
                    cell.setCellValue(phone.isEmpty() ? name : name + "\n" + phone);
                    if (!phone.isEmpty()) row.setHeightInPoints(sheet.getDefaultRowHeightInPoints() * 2);
                    continue;
                }
                String dataType = column.getDataType() == null ? ""
                        : column.getDataType().trim().toUpperCase(Locale.ROOT).replaceAll("\\(.*\\)$", "");
                if ("DATE".equals(dataType) || "DATETIME".equals(dataType) || "TIMESTAMP".equals(dataType)) {
                    boolean dateOnly = "DATE".equals(dataType);
                    cell.setCellStyle(dateOnly ? dateStyle : dateTimeStyle);
                    Object value = data.get(column.getProp());
                    if (value != null && !String.valueOf(value).trim().isEmpty()) {
                        Double serial = excelDateValue(value, dateOnly);
                        if (serial != null) {
                            cell.setCellValue(serial);
                            continue;
                        }
                        cell.setCellStyle(textStyle);
                        cell.setCellValue(String.valueOf(value));
                    }
                    continue;
                }
                cell.setCellStyle(textStyle);
                Object value = data.get(column.getProp());
                ReferenceValues fieldChoices = choices.get(column.getProp());
                cell.setCellValue(fieldChoices == null
                        ? display(value, column) : display(fieldChoices.display(value), column));
            }
        }
        return index;
    }

    private Double excelDateValue(Object value, boolean dateOnly) {
        try {
            String text = String.valueOf(value).trim();
            if (dateOnly) {
                LocalDate date = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
                return DateUtil.getExcelDate(java.util.Date.from(
                        date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }
            LocalDateTime dateTime = LocalDateTime.parse(text.replace(' ', 'T'),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return DateUtil.getExcelDate(java.util.Date.from(
                    dateTime.atZone(ZoneId.systemDefault()).toInstant()));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean isCreatorColumn(MasterColumnRespVO column) {
        return "CREATED_BY".equalsIgnoreCase(column.getProp())
                || "CREATE_BY".equalsIgnoreCase(column.getProp())
                || "创建人".equals(column.getLabel());
    }

    private boolean isUserColumn(MasterColumnRespVO column) {
        return isCreatorColumn(column)
                || "UPDATED_BY".equalsIgnoreCase(column.getProp())
                || "UPDATE_BY".equalsIgnoreCase(column.getProp())
                || "更新人".equals(column.getLabel());
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    String display(Object value, MasterColumnRespVO column) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if ("DATA_SOURCE".equalsIgnoreCase(column == null ? null : column.getProp())) {
            switch (text) {
                case "MANUAL": return "平台录入";
                case "IMPORT": return "批量导入";
                case "MERGE": return "多源融合";
                default: return text;
            }
        }
        String dict = column == null ? null : column.getDict();
        if ("master_data_status".equals(dict)) {
            String label = MasterStatusLabels.lifecycle(text);
            return label == null ? text : label;
        }
        if ("master_change_request_status".equals(dict)) {
            switch (text) {
                case "0": return "审批中";
                case "1": return "已通过";
                case "2": return "已驳回";
                case "3": return "已撤回";
                case "4": return "已终止";
                default: return text;
            }
        }
        return text;
    }
}
