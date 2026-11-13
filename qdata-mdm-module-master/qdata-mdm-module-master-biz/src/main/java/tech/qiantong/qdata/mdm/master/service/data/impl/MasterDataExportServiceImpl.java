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

package tech.qiantong.qdata.mdm.master.service.data.impl;

import org.apache.poi.ss.usermodel.Cell;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
            throw new ServiceException("导出期间模型版本发生变化，请重试");
        }
        long total = first.getTotal() == null ? 0L : first.getTotal();
        if (total > MAX_ROWS) throw new ServiceException("当前筛选结果超过 5000 条，请缩小范围后导出");
        if (first.getRows() == null || first.getRows().size() != Math.min(total, PAGE_SIZE)) {
            throw new ServiceException("导出期间数据发生变化，请重试");
        }
        // 与 qData-pro2 的导出一样，先按查询条件取完整结果，再写入 HttpServletResponse。
        // 当前模型的列来自元数据，不能使用仅支持固定 @Excel 注解字段的 ExcelUtil<T>。
        List<MasterColumnRespVO> columns = new ArrayList<>(definition.getColumns());
        // 主数据ID在首列，其后依次为业务字段和其他系统字段；组内保留元数据顺序。
        columns.sort(Comparator.comparingInt(column -> "ID".equalsIgnoreCase(column.getProp()) ? 0
                : Boolean.TRUE.equals(column.getBusinessField()) ? 1 : 2));
        MasterModelRespDTO model = metadataService.getModel(instanceId, definition.getModelId());
        if (!definition.getModelVersionId().equals(model.getModelVersionId()))
            throw new ServiceException("导出期间模型版本发生变化，请重试");
        Map<String, ReferenceValues> choices = references.load(model);
        List<Map<String, Object>> rows = new ArrayList<>(first.getRows());
        for (int page = 2; rows.size() < total; page++) {
            filter.setPageNum(page);
            MasterDataRespVO next = dataService.getData(instanceId, filter);
            long expected = Math.min(PAGE_SIZE, total - rows.size());
            if (!first.getModelVersionId().equals(next.getModelVersionId())
                    || next.getTotal() == null || next.getTotal() != total
                    || next.getRows() == null || next.getRows().size() != expected) {
                throw new ServiceException("导出期间数据发生变化，请重试");
            }
            rows.addAll(next.getRows());
        }

        // 共享字符串兼容导出文件在 Excel/WPS 中编辑、保存后再次导入。
        SXSSFWorkbook book = new SXSSFWorkbook(new XSSFWorkbook(), 500, true, true);
        try {
            Sheet sheet = book.createSheet("正式数据");
            CellStyle textStyle = book.createCellStyle();
            textStyle.setDataFormat(book.createDataFormat().getFormat("@"));
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                header.createCell(i).setCellValue(columns.get(i).getLabel());
                sheet.setColumnWidth(i, 20 * 256);
            }
            MasterEditableWorkbook.attach(book, sheet, model, columns, choices, rows.size());
            writeRows(sheet, 1, columns, rows, textStyle, choices);
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

    private int writeRows(Sheet sheet, int index, List<MasterColumnRespVO> columns,
                          List<Map<String, Object>> rows, CellStyle textStyle, Map<String, ReferenceValues> choices) {
        if (rows == null) return index;
        for (Map<String, Object> data : rows) {
            Row row = sheet.createRow(index++);
            for (int i = 0; i < columns.size(); i++) {
                MasterColumnRespVO column = columns.get(i);
                Object value = data.get(column.getProp());
                Cell cell = row.createCell(i);
                cell.setCellStyle(textStyle);
                ReferenceValues fieldChoices = choices.get(column.getProp());
                cell.setCellValue(fieldChoices == null ? display(value, column) : fieldChoices.display(value));
            }
        }
        return index;
    }

    String display(Object value, MasterColumnRespVO column) {
        if (value == null) return "";
        String text = String.valueOf(value);
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
