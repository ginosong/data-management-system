package com.datamanagement.system.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.datamanagement.system.domain.MonthlyReport;
import com.datamanagement.system.domain.MonthlyReportFieldValue;
import com.datamanagement.system.domain.ReportTemplateField;
import com.datamanagement.system.domain.StatisticsUnit;
import com.datamanagement.system.domain.TechnicalCenter;
import com.datamanagement.system.repository.MonthlyReportFieldValueRepository;
import com.datamanagement.system.repository.MonthlyReportRepository;
import com.datamanagement.system.repository.StatisticsUnitRepository;
import com.datamanagement.system.repository.TechnicalCenterRepository;
import com.datamanagement.system.security.CurrentUser;
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
    private final MonthlyReportFieldValueRepository fieldValueRepository;
    private final StatisticsUnitRepository statisticsUnitRepository;
    private final TechnicalCenterRepository technicalCenterRepository;
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

    @Transactional(readOnly = true)
    public byte[] exportMonthlyEntryTable(String reportMonth) {
        currentUserService.requirePermission("reports:export");
        if (!StringUtils.hasText(reportMonth)) {
            throw new IllegalArgumentException("填报月份不能为空");
        }
        String targetMonth = reportMonth.trim();
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        List<ReportTemplateField> fields = reportTemplateService.getDefaultTemplateFields();
        List<StatisticsUnit> units = statisticsUnitRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc();
        Map<Long, List<TechnicalCenter>> centersByUnitId = technicalCenterRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc().stream()
            .filter(center -> currentUser.canAccessCenter(center.getId()))
            .collect(Collectors.groupingBy(center -> center.getUnit().getId(), LinkedHashMap::new, Collectors.toList()));

        List<MonthlyReport> reports = monthlyReportRepository.findAllByReportMonthOrderByUpdatedAtDescIdDesc(targetMonth).stream()
            .filter(report -> currentUser.canAccessCenter(report.getTechnicalCenter().getId()))
            .toList();
        Map<Long, MonthlyReport> reportsByCenterId = reports.stream()
            .collect(Collectors.toMap(report -> report.getTechnicalCenter().getId(), report -> report, (left, right) -> left, LinkedHashMap::new));
        Map<Long, Map<String, String>> valuesByReportId = buildDisplayValueMap(reports);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeMonthlyEntrySheet(workbook, targetMonth, fields, units, centersByUnitId, reportsByCenterId, valuesByReportId);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("生成 Excel 文件失败: " + exception.getMessage());
        }
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

    private void writeMonthlyEntrySheet(XSSFWorkbook workbook,
                                        String reportMonth,
                                        List<ReportTemplateField> fields,
                                        List<StatisticsUnit> units,
                                        Map<Long, List<TechnicalCenter>> centersByUnitId,
                                        Map<Long, MonthlyReport> reportsByCenterId,
                                        Map<Long, Map<String, String>> valuesByReportId) {
        Sheet sheet = workbook.createSheet(reportMonth + "月报填报");
        CellStyle groupHeaderStyle = createEntryHeaderStyle(workbook, IndexedColors.PALE_BLUE.getIndex(), false);
        CellStyle redHeaderStyle = createEntryHeaderStyle(workbook, IndexedColors.PALE_BLUE.getIndex(), true);
        CellStyle fieldHeaderStyle = createEntryHeaderStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex(), false);
        CellStyle redFieldHeaderStyle = createEntryHeaderStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex(), true);
        CellStyle unitCellStyle = createEntryCellStyle(workbook, true);
        CellStyle centerCellStyle = createEntryCellStyle(workbook, true);
        CellStyle valueCellStyle = createEntryCellStyle(workbook, false);
        CellStyle noteStyle = createEntryNoteStyle(workbook);

        Row groupRow = sheet.createRow(0);
        Row sectionRow = sheet.createRow(1);
        Row fieldRow = sheet.createRow(2);
        groupRow.setHeightInPoints(28);
        sectionRow.setHeightInPoints(28);
        fieldRow.setHeightInPoints(36);

        createStyledCell(groupRow, 0, "统计单位", groupHeaderStyle);
        createStyledCell(groupRow, 1, "技术平台/技术中心", groupHeaderStyle);
        mergeAndStyle(sheet, 0, 2, 0, 0, groupHeaderStyle);
        mergeAndStyle(sheet, 0, 2, 1, 1, groupHeaderStyle);

        List<EntryGroupSpan> groupSpans = buildEntryGroupSpans(fields);
        int columnIndex = 2;
        for (EntryGroupSpan group : groupSpans) {
            boolean redHeader = isRedEntryHeader(group.name());
            CellStyle topStyle = redHeader ? redHeaderStyle : groupHeaderStyle;
            createStyledCell(groupRow, columnIndex, group.name(), topStyle);
            if (group.colSpan() > 1) {
                mergeAndStyle(sheet, 0, group.hasSections() ? 0 : 1, columnIndex, columnIndex + group.colSpan() - 1, topStyle);
            } else if (!group.hasSections()) {
                mergeAndStyle(sheet, 0, 1, columnIndex, columnIndex, topStyle);
            }
            if (group.hasSections()) {
                int sectionColumn = columnIndex;
                for (EntrySectionSpan section : group.sections()) {
                    CellStyle sectionStyle = redHeader ? redHeaderStyle : groupHeaderStyle;
                    createStyledCell(sectionRow, sectionColumn, section.name(), sectionStyle);
                    if (section.colSpan() > 1) {
                        mergeAndStyle(sheet, 1, 1, sectionColumn, sectionColumn + section.colSpan() - 1, sectionStyle);
                    }
                    sectionColumn += section.colSpan();
                }
            }
            columnIndex += group.colSpan();
        }

        for (int index = 0; index < fields.size(); index++) {
            ReportTemplateField field = fields.get(index);
            CellStyle style = isRedEntryHeader(field.getGroupName()) ? redFieldHeaderStyle : fieldHeaderStyle;
            createStyledCell(fieldRow, index + 2, field.getFieldName(), style);
        }

        int rowIndex = 3;
        for (StatisticsUnit unit : units) {
            List<TechnicalCenter> centers = centersByUnitId.getOrDefault(unit.getId(), List.of());
            if (centers.isEmpty()) {
                continue;
            }
            int unitStartRow = rowIndex;
            for (TechnicalCenter center : centers) {
                Row row = sheet.createRow(rowIndex);
                row.setHeightInPoints(24);
                createStyledCell(row, 0, unit.getName(), unitCellStyle);
                createStyledCell(row, 1, center.getName(), centerCellStyle);
                MonthlyReport report = reportsByCenterId.get(center.getId());
                Map<String, String> values = report == null ? Map.of() : valuesByReportId.getOrDefault(report.getId(), Map.of());
                for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                    String value = values.getOrDefault(fields.get(fieldIndex).getFieldKey(), "");
                    createStyledCell(row, fieldIndex + 2, value, valueCellStyle);
                }
                rowIndex++;
            }
            if (rowIndex - unitStartRow > 1) {
                mergeAndStyle(sheet, unitStartRow, rowIndex - 1, 0, 0, unitCellStyle);
            }
        }

        Row noteRow = sheet.createRow(rowIndex);
        noteRow.setHeightInPoints(30);
        createStyledCell(noteRow, 0, "注：医院按照企业类别统计", noteStyle);
        mergeAndStyle(sheet, rowIndex, rowIndex, 0, fields.size() + 1, noteStyle);

        sheet.createFreezePane(2, 3);
        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 32 * 256);
        for (int index = 0; index < fields.size(); index++) {
            sheet.setColumnWidth(index + 2, 16 * 256);
        }
    }

    private List<EntryGroupSpan> buildEntryGroupSpans(List<ReportTemplateField> fields) {
        Map<String, LinkedHashMap<String, List<ReportTemplateField>>> grouped = new LinkedHashMap<>();
        for (ReportTemplateField field : fields) {
            String sectionName = StringUtils.hasText(field.getSubGroupName()) ? field.getSubGroupName() : field.getGroupName();
            grouped.computeIfAbsent(field.getGroupName(), ignored -> new LinkedHashMap<>())
                .computeIfAbsent(sectionName, ignored -> new ArrayList<>())
                .add(field);
        }
        List<EntryGroupSpan> spans = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, List<ReportTemplateField>>> groupEntry : grouped.entrySet()) {
            List<EntrySectionSpan> sections = groupEntry.getValue().entrySet().stream()
                .map(section -> new EntrySectionSpan(section.getKey(), section.getValue().size()))
                .toList();
            boolean hasSections = sections.size() > 1 || sections.stream().anyMatch(section -> !section.name().equals(groupEntry.getKey()));
            int colSpan = sections.stream().mapToInt(EntrySectionSpan::colSpan).sum();
            spans.add(new EntryGroupSpan(groupEntry.getKey(), colSpan, hasSections, sections));
        }
        return spans;
    }

    private Map<Long, Map<String, String>> buildDisplayValueMap(List<MonthlyReport> reports) {
        List<Long> reportIds = reports.stream().map(MonthlyReport::getId).toList();
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        return fieldValueRepository.findByReportIdIn(reportIds).stream()
            .collect(Collectors.groupingBy(fieldValue -> fieldValue.getReport().getId(), LinkedHashMap::new,
                Collectors.toMap(MonthlyReportFieldValue::getFieldKey, this::displayValue, (left, right) -> left, LinkedHashMap::new)));
    }

    private String displayValue(MonthlyReportFieldValue fieldValue) {
        if (StringUtils.hasText(fieldValue.getTextValue())) {
            return fieldValue.getTextValue();
        }
        if (fieldValue.getNumericValue() == null) {
            return "";
        }
        return fieldValue.getNumericValue().stripTrailingZeros().toPlainString();
    }

    private boolean isRedEntryHeader(String groupName) {
        return "基本运行情况".equals(groupName) || "对外开放情况".equals(groupName);
    }

    private void createStyledCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void mergeAndStyle(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol, CellStyle style) {
        if (firstRow != lastRow || firstCol != lastCol) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
        }
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex) == null ? sheet.createRow(rowIndex) : sheet.getRow(rowIndex);
            for (int columnIndex = firstCol; columnIndex <= lastCol; columnIndex++) {
                Cell cell = row.getCell(columnIndex) == null ? row.createCell(columnIndex) : row.getCell(columnIndex);
                cell.setCellStyle(style);
            }
        }
    }

    private CellStyle createEntryHeaderStyle(XSSFWorkbook workbook, short fillColor, boolean redText) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(redText ? IndexedColors.RED.getIndex() : IndexedColors.DARK_BLUE.getIndex());
        CellStyle style = createBorderedStyle(workbook);
        style.setFont(font);
        style.setFillForegroundColor(fillColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createEntryCellStyle(XSSFWorkbook workbook, boolean centered) {
        CellStyle style = createBorderedStyle(workbook);
        style.setAlignment(centered ? HorizontalAlignment.CENTER : HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createEntryNoteStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        CellStyle style = createBorderedStyle(workbook);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBorderedStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
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

    private record EntryGroupSpan(String name, int colSpan, boolean hasSections, List<EntrySectionSpan> sections) {
    }

    private record EntrySectionSpan(String name, int colSpan) {
    }
}
