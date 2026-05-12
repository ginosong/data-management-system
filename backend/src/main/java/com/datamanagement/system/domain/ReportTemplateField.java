package com.datamanagement.system.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "report_template_field")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    @Column(name = "excel_column", nullable = false, length = 16)
    private String excelColumn;

    @Column(name = "group_name", nullable = false, length = 128)
    private String groupName;

    @Column(name = "sub_group_name", length = 128)
    private String subGroupName;

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Column(name = "field_key", nullable = false, length = 128)
    private String fieldKey;

    @Column(name = "value_type", nullable = false, length = 32)
    private String valueType;

    @Column(name = "required_flag", nullable = false)
    private Boolean requiredFlag;

    @Column(name = "read_only_flag", nullable = false)
    private Boolean readOnlyFlag;

    @Column(name = "formula_expression", length = 255)
    private String formulaExpression;

    @Column(name = "helper_text", length = 255)
    private String helperText;

    @Column(name = "min_value", precision = 18, scale = 2)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 18, scale = 2)
    private BigDecimal maxValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
