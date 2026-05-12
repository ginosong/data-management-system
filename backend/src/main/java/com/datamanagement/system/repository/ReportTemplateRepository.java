package com.datamanagement.system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.ReportTemplate;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    Optional<ReportTemplate> findByCode(String code);
}
