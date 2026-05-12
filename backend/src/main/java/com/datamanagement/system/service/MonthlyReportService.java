package com.datamanagement.system.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.datamanagement.system.domain.MonthlyReport;
import com.datamanagement.system.domain.MonthlyReportFieldValue;
import com.datamanagement.system.domain.ReportTemplate;
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
public class MonthlyReportService {

    private final MonthlyReportRepository monthlyReportRepository;
    private final MonthlyReportFieldValueRepository fieldValueRepository;
    private final StatisticsUnitRepository statisticsUnitRepository;
    private final TechnicalCenterRepository technicalCenterRepository;
    private final ReportTemplateService reportTemplateService;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listReports(String reportMonth, Long unitId, Long centerId, String keyword) {
        currentUserService.requirePermission("reports:view");
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        List<MonthlyReport> reports = StringUtils.hasText(reportMonth)
            ? monthlyReportRepository.findAllByReportMonthOrderByUpdatedAtDescIdDesc(reportMonth.trim())
            : monthlyReportRepository.findAllByOrderByReportMonthDescUpdatedAtDescIdDesc();

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<MonthlyReport> filteredReports = reports.stream()
            .filter(report -> currentUser.canAccessCenter(report.getTechnicalCenter().getId()))
            .filter(report -> unitId == null || report.getStatisticsUnit().getId().equals(unitId))
            .filter(report -> centerId == null || report.getTechnicalCenter().getId().equals(centerId))
            .filter(report -> !StringUtils.hasText(normalizedKeyword)
                || report.getStatisticsUnit().getName().contains(normalizedKeyword)
                || report.getTechnicalCenter().getName().contains(normalizedKeyword)
                || report.getReportMonth().contains(normalizedKeyword))
            .toList();

        Map<Long, Map<String, MonthlyReportFieldValue>> valueMap = buildValueMap(filteredReports);
        return filteredReports.stream()
            .map(report -> toSummary(report, valueMap.getOrDefault(report.getId(), Map.of())))
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReportDetail(Long reportId) {
        currentUserService.requireAnyPermission("reports:view", "reports:export");
        MonthlyReport report = monthlyReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("月报不存在"));
        currentUserService.requireCenterAccess(report.getTechnicalCenter().getId());
        List<ReportTemplateField> fieldDefinitions = reportTemplateService.getDefaultTemplateFields();
        Map<String, String> values = normalizeValues(fieldValueRepository.findByReportId(reportId).stream()
            .collect(Collectors.toMap(MonthlyReportFieldValue::getFieldKey, this::displayValue, (left, right) -> left, LinkedHashMap::new)), fieldDefinitions);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", report.getId());
        detail.put("reportMonth", report.getReportMonth());
        detail.put("statisticsUnitId", report.getStatisticsUnit().getId());
        detail.put("statisticsUnitName", report.getStatisticsUnit().getName());
        detail.put("technicalCenterId", report.getTechnicalCenter().getId());
        detail.put("technicalCenterName", report.getTechnicalCenter().getName());
        detail.put("submitStatus", report.getSubmitStatus());
        detail.put("auditStatus", report.getAuditStatus());
        detail.put("submittedAt", report.getSubmittedAt() == null ? null : report.getSubmittedAt().toString());
        detail.put("updatedAt", report.getUpdatedAt() == null ? null : report.getUpdatedAt().toString());
        detail.put("values", values);
        return detail;
    }

    @Transactional
    public Map<String, Object> saveReport(Long reportId,
                                          String reportMonth,
                                          Long statisticsUnitId,
                                          Long technicalCenterId,
                                          String submitStatus,
                                          Map<String, String> values) {
        currentUserService.requirePermission("reports:edit");
        if (!StringUtils.hasText(reportMonth)) {
            throw new IllegalArgumentException("填报月份不能为空");
        }
        if (statisticsUnitId == null) {
            throw new IllegalArgumentException("统计单位不能为空");
        }
        if (technicalCenterId == null) {
            throw new IllegalArgumentException("技术中心不能为空");
        }

        ReportTemplate template = reportTemplateService.getDefaultTemplateEntity();
        List<ReportTemplateField> fieldDefinitions = reportTemplateService.getDefaultTemplateFields();
        Map<String, ReportTemplateField> fieldDefinitionMap = fieldDefinitions.stream()
            .collect(Collectors.toMap(ReportTemplateField::getFieldKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        Set<String> allowedKeys = fieldDefinitionMap.keySet();
        StatisticsUnit statisticsUnit = statisticsUnitRepository.findById(statisticsUnitId)
            .orElseThrow(() -> new IllegalArgumentException("统计单位不存在"));
        TechnicalCenter technicalCenter = technicalCenterRepository.findById(technicalCenterId)
            .orElseThrow(() -> new IllegalArgumentException("技术中心不存在"));
        currentUserService.requireCenterAccess(technicalCenter.getId());

        if (!technicalCenter.getUnit().getId().equals(statisticsUnit.getId())) {
            throw new IllegalArgumentException("技术中心不属于当前统计单位");
        }

        monthlyReportRepository.findByTemplateIdAndReportMonthAndTechnicalCenterId(template.getId(), reportMonth.trim(), technicalCenterId)
            .ifPresent(existing -> {
                if (reportId == null || !existing.getId().equals(reportId)) {
                    throw new IllegalArgumentException("同一技术中心在该月份下已经存在月报");
                }
            });

        MonthlyReport report = reportId == null
            ? MonthlyReport.builder().template(template).build()
            : monthlyReportRepository.findById(reportId).orElseThrow(() -> new IllegalArgumentException("月报不存在"));
        if (report.getId() != null) {
            currentUserService.requireCenterAccess(report.getTechnicalCenter().getId());
        }

        Map<String, String> normalizedValues = normalizeValues(values, fieldDefinitions).entrySet().stream()
            .filter(entry -> allowedKeys.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        validateValues(normalizedValues, fieldDefinitions);
        validateBusinessRules(normalizedValues);

        report.setTemplate(template);
        report.setReportMonth(reportMonth.trim());
        report.setStatisticsUnit(statisticsUnit);
        report.setTechnicalCenter(technicalCenter);
        report.setSubmitStatus(StringUtils.hasText(submitStatus) ? submitStatus.trim().toUpperCase(Locale.ROOT) : "SUBMITTED");
        report.setAuditStatus("APPROVED");
        report.setSubmittedBy(currentUserService.requireCurrentUser().id());
        report.setSubmittedAt(LocalDateTime.now());
        MonthlyReport savedReport = monthlyReportRepository.save(report);

        fieldValueRepository.deleteByReportId(savedReport.getId());
        List<MonthlyReportFieldValue> fieldValues = normalizedValues.entrySet().stream()
            .filter(entry -> StringUtils.hasText(entry.getValue()))
            .map(entry -> MonthlyReportFieldValue.builder()
                .report(savedReport)
                .fieldKey(entry.getKey())
                .textValue(entry.getValue().trim())
                .numericValue(parseDecimal(entry.getValue()))
                .build())
            .toList();
        fieldValueRepository.saveAll(fieldValues);

        writeOperationLog(reportId == null ? "CREATE" : "UPDATE", savedReport.getId(),
            "保存月份=" + savedReport.getReportMonth() + "，技术中心=" + savedReport.getTechnicalCenter().getName());
        return getReportDetail(savedReport.getId());
    }

    @Transactional
    public void deleteReport(Long reportId) {
        currentUserService.requirePermission("reports:edit");
        MonthlyReport report = monthlyReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("月报不存在"));
        currentUserService.requireCenterAccess(report.getTechnicalCenter().getId());
        fieldValueRepository.deleteByReportId(reportId);
        monthlyReportRepository.delete(report);
        writeOperationLog("DELETE", reportId, "删除月报：" + report.getReportMonth() + " / " + report.getTechnicalCenter().getName());
    }

    private Map<String, String> normalizeValues(Map<String, String> rawValues, List<ReportTemplateField> fieldDefinitions) {
        Map<String, ReportTemplateField> fieldMap = fieldDefinitions.stream()
            .collect(Collectors.toMap(ReportTemplateField::getFieldKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, String> normalizedValues = new LinkedHashMap<>();
        if (rawValues != null) {
            rawValues.forEach((key, value) -> {
                ReportTemplateField field = fieldMap.get(key);
                if (field != null && StringUtils.hasText(value)) {
                    normalizedValues.put(key, normalizeValue(field, value));
                }
            });
        }
        applyFormulaFields(normalizedValues, fieldDefinitions);
        return normalizedValues;
    }

    private String normalizeValue(ReportTemplateField field, String rawValue) {
        String trimmedValue = rawValue.trim();
        if (!StringUtils.hasText(trimmedValue)) {
            return "";
        }
        if (!"DECIMAL".equalsIgnoreCase(field.getValueType())) {
            return trimmedValue;
        }
        BigDecimal decimalValue = parseDecimal(trimmedValue);
        if (decimalValue == null) {
            throw new IllegalArgumentException(field.getFieldName() + " 必须填写数字");
        }
        return decimalValue.stripTrailingZeros().toPlainString();
    }

    private void applyFormulaFields(Map<String, String> values, List<ReportTemplateField> fieldDefinitions) {
        fieldDefinitions.stream()
            .filter(field -> StringUtils.hasText(field.getFormulaExpression()))
            .forEach(field -> values.put(field.getFieldKey(), evaluateFormula(field.getFormulaExpression(), values).stripTrailingZeros().toPlainString()));
    }

    private void validateValues(Map<String, String> values, List<ReportTemplateField> fieldDefinitions) {
        for (ReportTemplateField field : fieldDefinitions) {
            String value = values.get(field.getFieldKey());
            if (Boolean.TRUE.equals(field.getRequiredFlag()) && !StringUtils.hasText(value)) {
                throw new IllegalArgumentException(field.getFieldName() + " 不能为空");
            }
            if (!StringUtils.hasText(value) || !"DECIMAL".equalsIgnoreCase(field.getValueType())) {
                continue;
            }
            BigDecimal decimalValue = parseDecimal(value);
            if (decimalValue == null) {
                throw new IllegalArgumentException(field.getFieldName() + " 必须填写数字");
            }
            if (field.getMinValue() != null && decimalValue.compareTo(field.getMinValue()) < 0) {
                throw new IllegalArgumentException(field.getFieldName() + " 不能小于 " + field.getMinValue().stripTrailingZeros().toPlainString());
            }
            if (field.getMaxValue() != null && decimalValue.compareTo(field.getMaxValue()) > 0) {
                throw new IllegalArgumentException(field.getFieldName() + " 不能大于 " + field.getMaxValue().stripTrailingZeros().toPlainString());
            }
        }
    }

    private void validateBusinessRules(Map<String, String> values) {
        BigDecimal runHours = decimalOrZero(values, "run_hours");
        BigDecimal serviceHours = decimalOrZero(values, "service_hours");
        BigDecimal openHours = decimalOrZero(values, "open_hours_total");
        BigDecimal enterpriseUserHours = decimalOrZero(values, "enterprise_user_hours");

        if (serviceHours.compareTo(runHours) > 0) {
            throw new IllegalArgumentException("服务机时不能大于运行机时");
        }
        if (openHours.compareTo(serviceHours) > 0) {
            throw new IllegalArgumentException("对外开放机时总数不能大于服务机时");
        }
        if (enterpriseUserHours.compareTo(serviceHours) > 0) {
            throw new IllegalArgumentException("企业用户机时数不能大于服务机时");
        }
    }

    private BigDecimal evaluateFormula(String expression, Map<String, String> values) {
        String[] tokens = expression.trim().split("\\s+");
        if (tokens.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = decimalOrZero(values, tokens[0]);
        for (int index = 1; index < tokens.length; index += 2) {
            if (index + 1 >= tokens.length) {
                throw new IllegalArgumentException("公式配置不完整: " + expression);
            }
            BigDecimal operand = decimalOrZero(values, tokens[index + 1]);
            result = switch (tokens[index]) {
                case "+" -> result.add(operand);
                case "-" -> result.subtract(operand);
                default -> throw new IllegalArgumentException("公式存在不支持的运算符: " + expression);
            };
        }
        return result.max(BigDecimal.ZERO);
    }

    private BigDecimal decimalOrZero(Map<String, String> values, String key) {
        return parseDecimal(values.get(key)) == null ? BigDecimal.ZERO : parseDecimal(values.get(key));
    }

    private Map<Long, Map<String, MonthlyReportFieldValue>> buildValueMap(List<MonthlyReport> reports) {
        List<Long> reportIds = reports.stream().map(MonthlyReport::getId).filter(Objects::nonNull).toList();
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        return fieldValueRepository.findByReportIdIn(reportIds).stream()
            .collect(Collectors.groupingBy(fieldValue -> fieldValue.getReport().getId(), LinkedHashMap::new,
                Collectors.toMap(MonthlyReportFieldValue::getFieldKey, item -> item, (left, right) -> left, LinkedHashMap::new)));
    }

    private Map<String, Object> toSummary(MonthlyReport report, Map<String, MonthlyReportFieldValue> values) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", report.getId());
        item.put("reportMonth", report.getReportMonth());
        item.put("unitId", report.getStatisticsUnit().getId());
        item.put("unitName", report.getStatisticsUnit().getName());
        item.put("centerId", report.getTechnicalCenter().getId());
        item.put("centerName", report.getTechnicalCenter().getName());
        item.put("submitStatus", report.getSubmitStatus());
        item.put("auditStatus", report.getAuditStatus());
        item.put("updatedAt", report.getUpdatedAt() == null ? null : report.getUpdatedAt().toString());
        item.put("metrics", Map.of(
            "runHours", numericValue(values, "run_hours"),
            "serviceHours", numericValue(values, "service_hours"),
            "openHours", numericValue(values, "open_hours_total"),
            "trainingHours", numericValue(values, "training_hours")
                .add(numericValue(values, "enterprise_training_hours"))
                .add(numericValue(values, "safety_training_hours"))
        ));
        return item;
    }

    private BigDecimal numericValue(Map<String, MonthlyReportFieldValue> values, String key) {
        MonthlyReportFieldValue value = values.get(key);
        return value == null || value.getNumericValue() == null ? BigDecimal.ZERO : value.getNumericValue();
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

    private BigDecimal parseDecimal(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return new BigDecimal(rawValue.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeOperationLog(String action, Long targetId, String detail) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        jdbcTemplate.update(
            "INSERT INTO operation_log (operator_id, operator_name, action, target_type, target_id, detail, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            currentUser.id(),
            currentUser.displayName(),
            action,
            "MONTHLY_REPORT",
            targetId,
            detail,
            LocalDateTime.now()
        );
    }
}
