package com.datamanagement.system.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datamanagement.system.domain.MonthlyReportFieldValue;

public interface MonthlyReportFieldValueRepository extends JpaRepository<MonthlyReportFieldValue, Long> {

    @Modifying
    @Query("delete from MonthlyReportFieldValue fv where fv.report.id = :reportId")
    void deleteByReportId(@Param("reportId") Long reportId);

    List<MonthlyReportFieldValue> findByReportId(Long reportId);

    List<MonthlyReportFieldValue> findByReportIdIn(Collection<Long> reportIds);
}
