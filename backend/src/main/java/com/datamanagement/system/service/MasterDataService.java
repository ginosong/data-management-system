package com.datamanagement.system.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.datamanagement.system.security.CurrentUser;
import com.datamanagement.system.security.CurrentUserService;
import com.datamanagement.system.domain.StatisticsUnit;
import com.datamanagement.system.domain.TechnicalCenter;
import com.datamanagement.system.repository.StatisticsUnitRepository;
import com.datamanagement.system.repository.TechnicalCenterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final StatisticsUnitRepository statisticsUnitRepository;
    private final TechnicalCenterRepository technicalCenterRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUnitsTree() {
        currentUserService.requirePermission("master-data:view");
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        Map<Long, List<TechnicalCenter>> centerMap = technicalCenterRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc().stream()
            .filter(center -> currentUser.canAccessCenter(center.getId()))
            .collect(Collectors.groupingBy(center -> center.getUnit().getId(), LinkedHashMap::new, Collectors.toList()));

        return statisticsUnitRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc().stream()
            .filter(unit -> currentUser.admin() || centerMap.containsKey(unit.getId()))
            .map(unit -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", unit.getId());
                item.put("code", unit.getCode());
                item.put("name", unit.getName());
                item.put("centers", centerMap.getOrDefault(unit.getId(), List.of()).stream().map(this::toCenterMap).toList());
                return item;
            })
            .toList();
    }

    @Transactional
    public Map<String, Object> saveUnit(Long unitId, String code, String name) {
        currentUserService.requirePermission("master-data:edit");
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("统计单位名称不能为空");
        }
        StatisticsUnit unit = unitId == null
            ? new StatisticsUnit()
            : statisticsUnitRepository.findById(unitId).orElseThrow(() -> new IllegalArgumentException("统计单位不存在"));
        unit.setCode(resolveCode("UNIT", code));
        unit.setName(name.trim());
        unit.setSortOrder(unit.getSortOrder() == null ? 99 : unit.getSortOrder());
        unit.setEnabled(Boolean.TRUE);
        return toUnitMap(statisticsUnitRepository.save(unit));
    }

    @Transactional
    public Map<String, Object> saveCenter(Long centerId, Long unitId, String code, String name) {
        currentUserService.requirePermission("master-data:edit");
        if (unitId == null) {
            throw new IllegalArgumentException("技术中心必须归属统计单位");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("技术中心名称不能为空");
        }
        StatisticsUnit unit = statisticsUnitRepository.findById(unitId)
            .orElseThrow(() -> new IllegalArgumentException("统计单位不存在"));
        TechnicalCenter center = centerId == null
            ? new TechnicalCenter()
            : technicalCenterRepository.findById(centerId).orElseThrow(() -> new IllegalArgumentException("技术中心不存在"));
        center.setUnit(unit);
        center.setCode(resolveCode("CENTER", code));
        center.setName(name.trim());
        center.setSortOrder(center.getSortOrder() == null ? 99 : center.getSortOrder());
        center.setEnabled(Boolean.TRUE);
        return toCenterMap(technicalCenterRepository.save(center));
    }

    @Transactional
    public void disableUnit(Long unitId) {
        currentUserService.requirePermission("master-data:edit");
        StatisticsUnit unit = statisticsUnitRepository.findById(unitId)
            .orElseThrow(() -> new IllegalArgumentException("统计单位不存在"));
        long enabledCenterCount = technicalCenterRepository.countByUnitIdAndEnabledTrue(unitId);
        if (enabledCenterCount > 0) {
            throw new IllegalArgumentException("请先停用该统计单位下的技术中心");
        }
        unit.setEnabled(Boolean.FALSE);
    }

    @Transactional
    public void disableCenter(Long centerId) {
        currentUserService.requirePermission("master-data:edit");
        TechnicalCenter center = technicalCenterRepository.findById(centerId)
            .orElseThrow(() -> new IllegalArgumentException("技术中心不存在"));
        center.setEnabled(Boolean.FALSE);
    }

    private Map<String, Object> toUnitMap(StatisticsUnit unit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", unit.getId());
        item.put("code", unit.getCode());
        item.put("name", unit.getName());
        return item;
    }

    private Map<String, Object> toCenterMap(TechnicalCenter center) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", center.getId());
        item.put("code", center.getCode());
        item.put("name", center.getName());
        item.put("unitId", center.getUnit().getId());
        item.put("unitName", center.getUnit().getName());
        return item;
    }

    private String resolveCode(String prefix, String code) {
        if (StringUtils.hasText(code)) {
            return code.trim().toUpperCase();
        }
        return prefix + '_' + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
