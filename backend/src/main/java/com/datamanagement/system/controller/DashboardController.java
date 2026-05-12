package com.datamanagement.system.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datamanagement.system.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return dashboardService.getOverview();
    }

    @GetMapping("/statistics")
    public Map<String, Object> statistics(@RequestParam(required = false) String month) {
        return dashboardService.getStatistics(month);
    }
}
