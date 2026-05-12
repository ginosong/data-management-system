package com.datamanagement.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datamanagement.system.service.MasterDataService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
@Validated
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/units")
    public List<Map<String, Object>> units() {
        return masterDataService.getUnitsTree();
    }

    @PostMapping("/units")
    public Map<String, Object> createUnit(@Valid @RequestBody UnitRequest request) {
        return masterDataService.saveUnit(null, request.code(), request.name());
    }

    @PutMapping("/units/{unitId}")
    public Map<String, Object> updateUnit(@PathVariable Long unitId, @Valid @RequestBody UnitRequest request) {
        return masterDataService.saveUnit(unitId, request.code(), request.name());
    }

    @DeleteMapping("/units/{unitId}")
    public void deleteUnit(@PathVariable Long unitId) {
        masterDataService.disableUnit(unitId);
    }

    @PostMapping("/centers")
    public Map<String, Object> createCenter(@Valid @RequestBody CenterRequest request) {
        return masterDataService.saveCenter(null, request.unitId(), request.code(), request.name());
    }

    @PutMapping("/centers/{centerId}")
    public Map<String, Object> updateCenter(@PathVariable Long centerId, @Valid @RequestBody CenterRequest request) {
        return masterDataService.saveCenter(centerId, request.unitId(), request.code(), request.name());
    }

    @DeleteMapping("/centers/{centerId}")
    public void deleteCenter(@PathVariable Long centerId) {
        masterDataService.disableCenter(centerId);
    }

    public record UnitRequest(String code, @NotBlank(message = "不能为空") String name) {
    }

    public record CenterRequest(@NotNull(message = "不能为空") Long unitId,
                                String code,
                                @NotBlank(message = "不能为空") String name) {
    }
}
