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

package tech.qiantong.qdata.mdm.master.service.data.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.service.data.reference.MasterReferenceService.ReferenceValues;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Excel 的显示名称与存储值分离；隐藏表是文件绑定，不作为权限或有效性依据。 */
public final class MasterEditableWorkbook {
    public static final int MAX_COLUMNS = 256;
    private static final int MAX_OPTIONS = 100000;
    private static final int MIN_DROPDOWN_ROWS = 500;
    private static final String MARKER = "QDATA_MDM_EDITABLE_V1";
    private static final String COLUMNS = "_qdata_columns";
    private static final String OPTIONS = "_qdata_options";

    private MasterEditableWorkbook() {
    }

    /**
     * 写入隐藏的字段、选项绑定，并为引用列添加下拉。
     * 表头创建后、数据行写入前调用，避免流式工作簿刷出表头后无法读取或调整列名。
     */
    public static void attach(Workbook book, Sheet data, MasterModelRespDTO model,
            List<? extends MasterColumnRespVO> columns, Map<String, ReferenceValues> choices, int dataRows) {
        if (columns.isEmpty() || columns.size() > MAX_COLUMNS)
            throw new ServiceException("导出字段绑定数量无效，最多支持" + MAX_COLUMNS + "列");
        Row header = data.getRow(0);
        DataValidationHelper helper = data.getDataValidationHelper();
        Sheet binding = book.createSheet(COLUMNS);
        // 首行绑定模型、实例及发布版本，其余行保存字段编码与实际表头的对应关系。
        Row context = binding.createRow(0);
        String[] values = {MARKER, String.valueOf(model.getModelId()), String.valueOf(model.getInstanceId()),
                String.valueOf(model.getModelVersionId())};
        for (int i = 0; i < values.length; i++) context.createCell(i).setCellValue(values[i]);
        Sheet options = book.createSheet(OPTIONS);
        Row optionHeader = options.createRow(0);
        optionHeader.createCell(0).setCellValue("字段编码");
        optionHeader.createCell(1).setCellValue("显示名称");
        optionHeader.createCell(2).setCellValue("存储值");
        Set<String> titles = new HashSet<>();
        int optionRowNo = 1;
        for (int i = 0; i < columns.size(); i++) {
            MasterColumnRespVO column = columns.get(i);
            String title = text(header.getCell(i));
            if (!titles.add(title)) {
                title += "（" + column.getProp() + "）";
                if (!titles.add(title)) throw new ServiceException("导出字段名称重复，请检查模型配置");
                header.getCell(i).setCellValue(title);
            }
            Row row = binding.createRow(i + 1);
            row.createCell(0).setCellValue(column.getProp());
            row.createCell(1).setCellValue(title);
            ReferenceValues fieldChoices = choices.get(column.getProp());
            if (fieldChoices == null || fieldChoices.options().isEmpty()) continue;
            if (optionRowNo - 1 + fieldChoices.options().size() > MAX_OPTIONS)
                throw new ServiceException("下拉选项总数超过" + MAX_OPTIONS + "项，请缩小导出字段范围");
            int startRow = optionRowNo;
            for (Map.Entry<String, String> option : fieldChoices.options().entrySet()) {
                Row optionRow = options.createRow(optionRowNo++);
                optionRow.createCell(0).setCellValue(column.getProp());
                optionRow.createCell(1).setCellValue(option.getKey());
                optionRow.createCell(2).setCellValue(option.getValue());
            }
            // 用命名区域引用隐藏表，支持较长的下拉列表。
            Name range = book.createName();
            range.setNameName("_qdata_list_" + i);
            range.setRefersToFormula("'" + OPTIONS + "'!$B$" + (startRow + 1) + ":$B$" + optionRowNo);
            DataValidation validation = helper.createValidation(helper.createFormulaListConstraint(range.getNameName()),
                    new CellRangeAddressList(1, Math.max(MIN_DROPDOWN_ROWS, dataRows), i, i));
            validation.setEmptyCellAllowed(true);
            // POI 4.x 的 XSSF 实现会反转此参数；true 才写出 showDropDown=false，显示单元格下拉箭头。
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("请选择有效选项", "请从下拉列表选择名称，导入时会自动转换为对应编码。");
            data.addValidationData(validation);
        }
        book.setSheetHidden(book.getSheetIndex(binding), true);
        book.setSheetHidden(book.getSheetIndex(options), true);
    }

    /** 读取并检查文件绑定；无绑定表时返回 null，由调用方按普通模板解析。 */
    public static Binding read(Workbook book, MasterModelRespDTO model) {
        Sheet sheet = book.getSheet(COLUMNS);
        if (sheet == null) return null;
        Row context = sheet.getRow(0);
        if (context == null || !MARKER.equals(text(context.getCell(0)))
                || !String.valueOf(model.getModelId()).equals(text(context.getCell(1)))
                || !String.valueOf(model.getInstanceId()).equals(text(context.getCell(2)))
                || !String.valueOf(model.getModelVersionId()).equals(text(context.getCell(3))))
            throw new ServiceException("导出文件与当前模型发布版本不一致，请重新导出");
        if (sheet.getLastRowNum() < 1 || sheet.getLastRowNum() > MAX_COLUMNS)
            throw new ServiceException("导出字段绑定数量无效，最多支持" + MAX_COLUMNS + "列");
        Set<String> allowed = new HashSet<>();
        model.getFields().forEach(field -> allowed.add(field.getCode()));
        Binding binding = new Binding();
        Set<String> seen = new HashSet<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) throw new ServiceException("导出字段绑定不完整");
            String code = text(row.getCell(0));
            String label = text(row.getCell(1));
            if (!allowed.contains(code) || label.isEmpty() || !seen.add(code)
                    || binding.headers.putIfAbsent(label, code) != null)
                throw new ServiceException("导出字段绑定无效，请重新导出");
        }
        Sheet options = book.getSheet(OPTIONS);
        if (options == null || options.getRow(0) == null) throw new ServiceException("导出选项绑定缺失，请重新导出");
        if (options.getLastRowNum() > MAX_OPTIONS)
            throw new ServiceException("导出选项绑定数量无效");
        for (int rowNo = 1; rowNo <= options.getLastRowNum(); rowNo++) {
            Row row = options.getRow(rowNo);
            if (row == null) throw new ServiceException("导出选项绑定不完整");
            String code = text(row.getCell(0));
            if (!seen.contains(code))
                throw new ServiceException("导出选项绑定字段无效");
            Map<String, String> choices = binding.options.computeIfAbsent(code, key -> new LinkedHashMap<>());
            String label = text(row.getCell(1));
            String value = text(row.getCell(2));
            if (label.isEmpty() || value.isEmpty() || choices.putIfAbsent(label, value) != null)
                throw new ServiceException("导出选项名称绑定重复或为空");
        }
        return binding;
    }

    private static String text(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        if (cell.getCellType() != CellType.STRING) throw new ServiceException("导出绑定必须为文本，不能包含公式或数字");
        return cell.getStringCellValue();
    }

    /** 文件中的对应关系只用于解析；选项是否仍有效由引用值服务校验。 */
    public static class Binding {
        /** 实际表头 → 字段编码，允许重新导入时调整列顺序。 */
        private final Map<String, String> headers = new LinkedHashMap<>();
        /** 字段编码 →（导出时的显示名称 → 存储值）。 */
        private final Map<String, Map<String, String>> options = new LinkedHashMap<>();

        public String header(String label) {
            String code = headers.get(label);
            if (code == null) throw new ServiceException("导出列绑定缺失：" + label);
            return code;
        }

        public Map<String, String> options(String code) {
            return options.get(code);
        }
    }
}
