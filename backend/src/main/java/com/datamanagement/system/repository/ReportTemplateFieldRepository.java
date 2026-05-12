package com.datamanagement.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.ReportTemplateField;

public interface ReportTemplateFieldRepository extends JpaRepository<ReportTemplateField, Long> {

    List<ReportTemplateField> findByTemplateIdOrderBySortOrderAscIdAsc(Long templateId);
}
