package com.datamanagement.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.MonthlyReport;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {

    long countByAuditStatus(String auditStatus);

    Optional<MonthlyReport> findFirstByOrderByReportMonthDesc();

    Optional<MonthlyReport> findByTemplateIdAndReportMonthAndTechnicalCenterId(Long templateId, String reportMonth, Long technicalCenterId);

    List<MonthlyReport> findAllByOrderByReportMonthDescUpdatedAtDescIdDesc();

    List<MonthlyReport> findAllByReportMonthOrderByUpdatedAtDescIdDesc(String reportMonth);
}
