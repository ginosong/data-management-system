package com.datamanagement.system.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.datamanagement.system.domain.MonthlyReport;
import com.datamanagement.system.domain.ReportTemplateField;
import com.datamanagement.system.repository.MonthlyReportRepository;
import com.datamanagement.system.security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportExcelService {

    private static final String DATA_SHEET_NAME = "月报数据";
    private static final String GUIDE_SHEET_NAME = "填报说明";

    private final ReportTemplateService reportTemplateService;
    private final MonthlyReportService monthlyReportService;
    private final MonthlyReportRepository monthlyReportRepository;
    private final CurrentUserService currentUserService;

    public byte[] exportTemplate(String reportMonth, Long statisticsUnitId, Long technicalCenterId) {
        currentUserService.requirePermission("reports:export");
        return buildWorkbook(reportMonth, statisticsUnitId, technicalCenterId, "SUBMITTED", Map.of());
    }

    public byte[] exportReport(Long reportId) {
        currentUserService.requirePermission("reports:export");
        Map<String, Object> detail = monthlyReportService.getReportDetail(reportId);
        @SuppressWarnings("unchecked")
        Map<String, String> values = (Map<String, String>) detail.get("values");
        return buildWorkbook(
            String.valueOf(detail.get("reportMonth")),
            (Long) detail.get("statisticsUnitId"),
            (Long) detail.get("technicalCenterId"),
            String.valueOf(detail.get("submitStatus")),
            values == null ? Map.of() : values
        );
    }

    public Map<String, Object> importReport(MultipartFile file,
                                            String reportMonth,
                                            Long statisticsUnitId,
                                            Long technicalCenterId,
                                            String submitStatus,
                                            boolean overwriteExisting) {
        currentUserService.requirePermission("reports:edit");
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先选择要导入的 Excel 文件");
        }
        Map<String, String> values = parseWorkbook(file);
        Long reportId = null;
        if (overwriteExisting) {
            reportId = monthlyReportRepository.findByTemplateIdAndReportMonthAndTechnicalCenterId(
                    reportTemplateService.getDefaultTemplateEntity().getId(),
                    reportMonth.trim(),
                    technicalCenterId)
                .map(MonthlyReport::getId)
                .orElse(null);
        }
        return monthlyReportService.saveReport(reportId, reportMonth, statisticsUnitId, technicalCenterId, submitStatus, values);
    }

    private byte[] buildWorkbook(String reportMonth,
                                 Long statisticsUnitId,
                                 Long technicalCenterId,
                                 String submitStatus,
                                 Map<String, String> values) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            writeGuideSheet(workbook.createSheet(GUIDE_SHEET_NAME), reportMonth, statisticsUnitId, technicalCenterId, submitStatus);
            writeDataSheet(workbook.createSheet(DATA_SHEET_NAME), headerStyle, values);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("生成 Excel 文件失败: " + exception.getMessage());
        }
    }

    private void writeGuideSheet(Sheet sheet,
                                 String reportMonth,
                                 Long statisticsUnitId,
                                 Long technicalCenterId,
                                 String submitStatus) {
        writeGuideRow(sheet, 0, "重大科技设施运行数据管理系统月报模板");
        writeGuideRow(sheet, 2, "导入说明：请在“月报数据”工作表的“填报值”列录入或粘贴数据，不要修改“字段编码”。");
        writeGuideRow(sheet, 3, "导出说明：系统会自动回填公式字段和只读字段，导入时会再次校验。\n");
        writeGuideRow(sheet, 5, "填报月份", reportMonth);
        writeGuideRow(sheet, 6, "统计单位ID", statisticsUnitId == null ? "" : String.valueOf(statisticsUnitId));
        writeGuideRow(sheet, 7, "技术中心ID", technicalCenterId == null ? "" : String.valueOf(technicalCenterId));
        writeGuideRow(sheet, 8, "提交状态", submitStatus == null ? "SUBMITTED" : submitStatus);
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeDataSheet(Sheet sheet, CellStyle headerStyle, Map<String, String> values) {
        Row headerRow = sheet.createRow(0);
        List<String> headers = List.of("字段编码", "Excel列", "字段分组", "子分组", "字段名称", "值类型", "是否必填", "是否只读", "填报说明", "填报值");
        for (int index = 0; index < headers.size(); index++) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(headers.get(index));
            cell.setCellStyle(headerStyle);
        }

        List<ReportTemplateField> fields = reportTemplateService.getDefaultTemplateFields();
        for (int rowIndex = 0; rowIndex < fields.size(); rowIndex++) {
            ReportTemplateField field = fields.get(rowIndex);
            Row row = sheet.createRow(rowIndex + 1);
            row.createCell(0).setCellValue(field.getFieldKey());
            row.createCell(1).setCellValue(field.getExcelColumn());
            row.createCell(2).setCellValue(field.getGroupName());
            row.createCell(3).setCellValue(field.getSubGroupName() == null ? "" : field.getSubGroupName());
            row.createCell(4).setCellValue(field.getFieldName());
            row.createCell(5).setCellValue(field.getValueType());
            row.createCell(6).setCellValue(Boolean.TRUE.equals(field.getRequiredFlag()) ? "是" : "否");
            row.createCell(7).setCellValue(Boolean.TRUE.equals(field.getReadOnlyFlag()) ? "是" : "否");
            row.createCell(8).setCellValue(field.getHelperText() == null ? "" : field.getHelperText());
            row.createCell(9).setCellValue(values.getOrDefault(field.getFieldKey(), ""));
        }

        for (int index = 0; index < headers.size(); index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private Map<String, String> parseWorkbook(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet(DATA_SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 文件中未找到可读取的工作表");
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel 文件缺少表头");
            }

            Map<String, Integer> headerMap = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                headerMap.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
            }
            Integer fieldKeyColumn = headerMap.get("字段编码");
            Integer valueColumn = headerMap.get("填报值");
            if (fieldKeyColumn == null || valueColumn == null) {
                throw new IllegalArgumentException("Excel 模板格式不正确，缺少“字段编码”或“填报值”列");
            }

            Map<String, String> values = new LinkedHashMap<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String fieldKey = formatter.formatCellValue(row.getCell(fieldKeyColumn)).trim();
                if (!StringUtils.hasText(fieldKey)) {
                    continue;
                }
                String value = formatter.formatCellValue(row.getCell(valueColumn)).trim();
                if (StringUtils.hasText(value)) {
                    values.put(fieldKey, value);
                }
            }
            return values;
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取 Excel 文件失败: " + exception.getMessage());
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeGuideRow(Sheet sheet, int rowIndex, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(value);
    }

    private void writeGuideRow(Sheet sheet, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }
}