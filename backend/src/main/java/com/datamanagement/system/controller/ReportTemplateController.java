package com.datamanagement.system.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datamanagement.system.service.ReportTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/report-templates")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final ReportTemplateService reportTemplateService;

    @GetMapping("/default")
    public Map<String, Object> defaultTemplate() {
        return reportTemplateService.getDefaultTemplate();
    }
}
