package com.datamanagement.system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.datamanagement.system.security.CurrentUser;
import com.datamanagement.system.security.CurrentUserService;
import com.datamanagement.system.domain.MonthlyReport;
import com.datamanagement.system.domain.MonthlyReportFieldValue;
import com.datamanagement.system.repository.AppUserRepository;
import com.datamanagement.system.repository.MonthlyReportFieldValueRepository;
import com.datamanagement.system.repository.MonthlyReportRepository;
import com.datamanagement.system.repository.StatisticsUnitRepository;
import com.datamanagement.system.repository.TechnicalCenterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StatisticsUnitRepository statisticsUnitRepository;
    private final TechnicalCenterRepository technicalCenterRepository;
    private final AppUserRepository appUserRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final MonthlyReportFieldValueRepository fieldValueRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public Map<String, Object> getOverview() {
        currentUserService.requirePermission("dashboard:view");
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        String latestMonth = resolveMonth(null);
        long enabledCenters = technicalCenterRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc().stream()
            .filter(center -> currentUser.canAccessCenter(center.getId()))
            .count();
        long visibleReports = monthlyReportRepository.findAllByOrderByReportMonthDescUpdatedAtDescIdDesc().stream()
            .filter(report -> currentUser.canAccessCenter(report.getTechnicalCenter().getId()))
            .count();
        long approvedReports = monthlyReportRepository.findAllByOrderByReportMonthDescUpdatedAtDescIdDesc().stream()
            .filter(report -> "APPROVED".equals(report.getAuditStatus()))
            .filter(report -> currentUser.canAccessCenter(report.getTechnicalCenter().getId()))
            .count();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("systemName", "重大科技设施运行数据管理系统");
        payload.put("latestMonth", latestMonth);
        payload.put("auditPolicy", "已预留审核流程，当前新建或修改数据默认自动审核通过。\n");
        payload.put("defaultAdmin", Map.of("username", "admin", "password", "admin123"));
        payload.put("queryHighlights", List.of(
            "支持按月份、统计单位、技术中心、关键字组合查询",
            "月报模板字段来自 Excel 固定结构，支持统一导出",
            "数据权限按技术中心控制，张三仅可维护被授权中心"
        ));
        payload.put("cards", List.of(
            metric("统计单位", statisticsUnitRepository.countByEnabledTrue(), "个", "按 Excel 主数据初始化"),
            metric("技术中心", enabledCenters, "个", currentUser.admin() ? "支持基于中心的数据权限" : "仅统计当前账号可见中心"),
            metric("账号数", appUserRepository.countByEnabledTrue(), "个", "内置默认 admin 管理员"),
            metric("已填报月报", visibleReports, "份", currentUser.admin() ? "默认样例数据已导入" : "已按当前账号数据权限过滤"),
            metric("审核通过", approvedReports, "份", "当前流程默认审核通过")
        ));
        return payload;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics(String requestedMonth) {
        currentUserService.requirePermission("dashboard:view");
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        String month = resolveMonth(requestedMonth);
        List<MonthlyReport> reports = monthlyReportRepository.findAllByReportMonthOrderByUpdatedAtDescIdDesc(month).stream()
            .filter(report -> currentUser.canAccessCenter(report.getTechnicalCenter().getId()))
            .toList();
        Map<Long, Map<String, MonthlyReportFieldValue>> valueMap = buildValueMap(reports);
        long totalCenters = technicalCenterRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc().stream()
            .filter(center -> currentUser.canAccessCenter(center.getId()))
            .count();
        BigDecimal coverage = totalCenters == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(reports.size() * 100.0d / totalCenters).setScale(1, RoundingMode.HALF_UP);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("month", month);
        payload.put("cards", List.of(
            metric("本月填报中心", reports.size(), "个", "按技术中心去重"),
            metric("填报覆盖率", coverage, "%", "当前已启用技术中心口径"),
            metric("运行机时合计", sumByKey(valueMap.values(), "run_hours"), "小时", "来自月报字段运行机时"),
            metric("服务机时合计", sumByKey(valueMap.values(), "service_hours"), "小时", "来自月报字段服务机时"),
            metric("开放机时合计", sumByKey(valueMap.values(), "open_hours_total"), "小时", "来自对外开放情况"),
            metric("培训课时合计", sumTrainingHours(valueMap.values()), "小时", "技术培训 + 企业技术培训 + 安全培训")
        ));
        payload.put("unitBreakdown", buildUnitBreakdown(reports, valueMap));
        payload.put("centerHighlights", buildCenterHighlights(reports, valueMap));
        return payload;
    }

    private List<Map<String, Object>> buildUnitBreakdown(List<MonthlyReport> reports,
                                                         Map<Long, Map<String, MonthlyReportFieldValue>> valueMap) {
        Map<String, List<MonthlyReport>> grouped = reports.stream()
            .collect(Collectors.groupingBy(report -> report.getStatisticsUnit().getName(), LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Map.Entry<String, List<MonthlyReport>> entry : grouped.entrySet()) {
            BigDecimal runHours = BigDecimal.ZERO;
            BigDecimal serviceHours = BigDecimal.ZERO;
            BigDecimal openHours = BigDecimal.ZERO;
            BigDecimal trainingHours = BigDecimal.ZERO;
            for (MonthlyReport report : entry.getValue()) {
                Map<String, MonthlyReportFieldValue> reportValues = valueMap.getOrDefault(report.getId(), Map.of());
                runHours = runHours.add(valueOrZero(reportValues, "run_hours"));
                serviceHours = serviceHours.add(valueOrZero(reportValues, "service_hours"));
                openHours = openHours.add(valueOrZero(reportValues, "open_hours_total"));
                trainingHours = trainingHours.add(sumTrainingHours(List.of(reportValues)));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("unitName", entry.getKey());
            item.put("reportCount", entry.getValue().size());
            item.put("runHours", runHours);
            item.put("serviceHours", serviceHours);
            item.put("openHours", openHours);
            item.put("trainingHours", trainingHours);
            breakdown.add(item);
        }
        return breakdown;
    }

    private List<Map<String, Object>> buildCenterHighlights(List<MonthlyReport> reports,
                                                            Map<Long, Map<String, MonthlyReportFieldValue>> valueMap) {
        return reports.stream()
            .map(report -> {
                Map<String, MonthlyReportFieldValue> reportValues = valueMap.getOrDefault(report.getId(), Map.of());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("centerName", report.getTechnicalCenter().getName());
                item.put("unitName", report.getStatisticsUnit().getName());
                item.put("serviceHours", valueOrZero(reportValues, "service_hours"));
                item.put("runHours", valueOrZero(reportValues, "run_hours"));
                item.put("openHours", valueOrZero(reportValues, "open_hours_total"));
                return item;
            })
            .sorted((left, right) -> ((BigDecimal) right.get("serviceHours")).compareTo((BigDecimal) left.get("serviceHours")))
            .limit(6)
            .toList();
    }

    private Map<String, Object> metric(String label, Object value, String unit, String note) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("label", label);
        metric.put("value", value);
        metric.put("unit", unit);
        metric.put("note", note);
        return metric;
    }

    private String resolveMonth(String requestedMonth) {
        if (StringUtils.hasText(requestedMonth)) {
            return requestedMonth.trim();
        }
        return monthlyReportRepository.findFirstByOrderByReportMonthDesc()
            .map(MonthlyReport::getReportMonth)
            .orElse(YearMonth.now().toString());
    }

    private Map<Long, Map<String, MonthlyReportFieldValue>> buildValueMap(List<MonthlyReport> reports) {
        List<Long> reportIds = reports.stream().map(MonthlyReport::getId).filter(Objects::nonNull).toList();
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        return fieldValueRepository.findByReportIdIn(reportIds).stream()
            .collect(Collectors.groupingBy(value -> value.getReport().getId(), LinkedHashMap::new,
                Collectors.toMap(MonthlyReportFieldValue::getFieldKey, value -> value, (left, right) -> left, LinkedHashMap::new)));
    }

    private BigDecimal sumByKey(Collection<Map<String, MonthlyReportFieldValue>> reportValues, String key) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, MonthlyReportFieldValue> values : reportValues) {
            total = total.add(valueOrZero(values, key));
        }
        return total;
    }

    private BigDecimal sumTrainingHours(Collection<Map<String, MonthlyReportFieldValue>> reportValues) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, MonthlyReportFieldValue> values : reportValues) {
            total = total
                .add(valueOrZero(values, "training_hours"))
                .add(valueOrZero(values, "enterprise_training_hours"))
                .add(valueOrZero(values, "safety_training_hours"));
        }
        return total;
    }

    private BigDecimal valueOrZero(Map<String, MonthlyReportFieldValue> valueMap, String key) {
        MonthlyReportFieldValue value = valueMap.get(key);
        if (value == null || value.getNumericValue() == null) {
            return BigDecimal.ZERO;
        }
        return value.getNumericValue();
    }
}
