package com.datamanagement.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.StatisticsUnit;

public interface StatisticsUnitRepository extends JpaRepository<StatisticsUnit, Long> {

    long countByEnabledTrue();

    List<StatisticsUnit> findAllByEnabledTrueOrderBySortOrderAscIdAsc();
}
