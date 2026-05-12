package com.datamanagement.system.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.datamanagement.system.service.ReportExcelService;
import com.datamanagement.system.service.MonthlyReportService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;
    private final ReportExcelService reportExcelService;

    @GetMapping
    public List<Map<String, Object>> reports(@RequestParam(required = false) String month,
                                             @RequestParam(required = false) Long unitId,
                                             @RequestParam(required = false) Long centerId,
                                             @RequestParam(required = false) String keyword) {
        return monthlyReportService.listReports(month, unitId, centerId, keyword);
    }

    @GetMapping("/{reportId}")
    public Map<String, Object> reportDetail(@PathVariable Long reportId) {
        return monthlyReportService.getReportDetail(reportId);
    }

    @GetMapping("/export/template")
    public ResponseEntity<byte[]> exportTemplate(@RequestParam(required = false) String month,
                                                 @RequestParam(required = false) Long unitId,
                                                 @RequestParam(required = false) Long centerId) {
        String targetMonth = month == null ? "template" : month;
        return excelResponse(
            reportExcelService.exportTemplate(month, unitId, centerId),
            "monthly-report-template-" + targetMonth + ".xlsx"
        );
    }

    @GetMapping("/{reportId}/export")
    public ResponseEntity<byte[]> exportReport(@PathVariable Long reportId) {
        return excelResponse(reportExcelService.exportReport(reportId), "monthly-report-" + reportId + ".xlsx");
    }

    @PostMapping
    public Map<String, Object> createReport(@Valid @RequestBody SaveReportRequest request) {
        return monthlyReportService.saveReport(
            null,
            request.reportMonth(),
            request.statisticsUnitId(),
            request.technicalCenterId(),
            request.submitStatus(),
            request.values()
        );
    }

    @PutMapping("/{reportId}")
    public Map<String, Object> updateReport(@PathVariable Long reportId, @Valid @RequestBody SaveReportRequest request) {
        return monthlyReportService.saveReport(
            reportId,
            request.reportMonth(),
            request.statisticsUnitId(),
            request.technicalCenterId(),
            request.submitStatus(),
            request.values()
        );
    }

    @DeleteMapping("/{reportId}")
    public void deleteReport(@PathVariable Long reportId) {
        monthlyReportService.deleteReport(reportId);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> importReport(@RequestParam("file") MultipartFile file,
                                            @RequestParam @NotBlank(message = "不能为空") String reportMonth,
                                            @RequestParam @NotNull(message = "不能为空") Long statisticsUnitId,
                                            @RequestParam @NotNull(message = "不能为空") Long technicalCenterId,
                                            @RequestParam(required = false) String submitStatus,
                                            @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        return reportExcelService.importReport(file, reportMonth, statisticsUnitId, technicalCenterId, submitStatus, overwriteExisting);
    }

    public record SaveReportRequest(
        @NotBlank(message = "不能为空") String reportMonth,
        @NotNull(message = "不能为空") Long statisticsUnitId,
        @NotNull(message = "不能为空") Long technicalCenterId,
        String submitStatus,
        Map<String, String> values
    ) {
    }

    private ResponseEntity<byte[]> excelResponse(byte[] content, String fileName) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8))
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(content);
    }
}
