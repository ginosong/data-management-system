package com.datamanagement.system.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.datamanagement.system.domain.ReportTemplate;
import com.datamanagement.system.domain.ReportTemplateField;
import com.datamanagement.system.repository.ReportTemplateFieldRepository;
import com.datamanagement.system.repository.ReportTemplateRepository;
import com.datamanagement.system.security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    public static final String DEFAULT_TEMPLATE_CODE = "DMS_MONTHLY_TEMPLATE";

    private final ReportTemplateRepository reportTemplateRepository;
    private final ReportTemplateFieldRepository reportTemplateFieldRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public Map<String, Object> getDefaultTemplate() {
        currentUserService.requireAnyPermission("reports:view", "reports:edit", "reports:export");
        ReportTemplate template = getDefaultTemplateEntity();
        List<ReportTemplateField> fields = getDefaultTemplateFields();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", template.getId());
        payload.put("code", template.getCode());
        payload.put("name", template.getName());
        payload.put("description", template.getDescription());
        payload.put("groups", buildGroups(fields));
        payload.put("fields", fields.stream().map(this::toFieldMap).toList());
        return payload;
    }

    @Transactional(readOnly = true)
    public ReportTemplate getDefaultTemplateEntity() {
        return reportTemplateRepository.findByCode(DEFAULT_TEMPLATE_CODE)
            .orElseThrow(() -> new IllegalArgumentException("默认月报模板不存在"));
    }

    @Transactional(readOnly = true)
    public List<ReportTemplateField> getDefaultTemplateFields() {
        return reportTemplateFieldRepository.findByTemplateIdOrderBySortOrderAscIdAsc(getDefaultTemplateEntity().getId());
    }

    private List<Map<String, Object>> buildGroups(List<ReportTemplateField> fields) {
        Map<String, Map<String, List<ReportTemplateField>>> grouped = new LinkedHashMap<>();
        for (ReportTemplateField field : fields) {
            String sectionName = StringUtils.hasText(field.getSubGroupName()) ? field.getSubGroupName() : "__SELF__";
            grouped.computeIfAbsent(field.getGroupName(), ignored -> new LinkedHashMap<>())
                .computeIfAbsent(sectionName, ignored -> new ArrayList<>())
                .add(field);
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<ReportTemplateField>>> groupEntry : grouped.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("name", groupEntry.getKey());
            group.put("sections", groupEntry.getValue().entrySet().stream().map(sectionEntry -> {
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("name", "__SELF__".equals(sectionEntry.getKey()) ? groupEntry.getKey() : sectionEntry.getKey());
                section.put("fields", sectionEntry.getValue().stream().map(this::toFieldMap).toList());
                return section;
            }).toList());
            groups.add(group);
        }
        return groups;
    }

    private Map<String, Object> toFieldMap(ReportTemplateField field) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", field.getId());
        item.put("key", field.getFieldKey());
        item.put("label", field.getFieldName());
        item.put("groupName", field.getGroupName());
        item.put("subGroupName", field.getSubGroupName());
        item.put("excelColumn", field.getExcelColumn());
        item.put("valueType", field.getValueType());
        item.put("required", field.getRequiredFlag());
        item.put("readOnly", field.getReadOnlyFlag());
        item.put("formulaExpression", field.getFormulaExpression());
        item.put("helperText", field.getHelperText());
        item.put("minValue", field.getMinValue());
        item.put("maxValue", field.getMaxValue());
        item.put("sortOrder", field.getSortOrder());
        return item;
    }
}
