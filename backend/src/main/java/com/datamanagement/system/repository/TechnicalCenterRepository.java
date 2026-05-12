package com.datamanagement.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.TechnicalCenter;

public interface TechnicalCenterRepository extends JpaRepository<TechnicalCenter, Long> {

    long countByUnitIdAndEnabledTrue(Long unitId);

    long countByEnabledTrue();

    List<TechnicalCenter> findAllByEnabledTrueOrderBySortOrderAscIdAsc();
}
