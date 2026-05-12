package com.datamanagement.system;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BackendIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginAndCurrentUserWorkWithJwt() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.username").value("admin"))
            .andExpect(jsonPath("$.user.permissions[0]").exists());
    }

    @Test
    void saveReportComputesFormulaFieldsAndValidatesBusinessRules() throws Exception {
        String token = login("admin", "admin123");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportMonth", "2026-05");
        payload.put("statisticsUnitId", 1);
        payload.put("technicalCenterId", 1);
        payload.put("submitStatus", "SUBMITTED");
        payload.put("values", Map.of(
            "run_hours", "120",
            "service_hours", "80",
            "open_hours_international", "12",
            "open_hours_domestic", "18",
            "external_user_international", "2",
            "external_user_domestic", "5",
            "training_hours", "8",
            "enterprise_training_hours", "2",
            "safety_training_hours", "1"
        ));

        mockMvc.perform(post("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.values.open_hours_total").value("30"))
            .andExpect(jsonPath("$.values.external_user_total").value("7"));

        payload.put("reportMonth", "2026-06");
        payload.put("values", Map.of(
            "run_hours", "10",
            "service_hours", "20",
            "open_hours_international", "3",
            "open_hours_domestic", "4",
            "external_user_international", "1",
            "external_user_domestic", "2",
            "training_hours", "1",
            "enterprise_training_hours", "1",
            "safety_training_hours", "1"
        ));

        mockMvc.perform(post("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("服务机时不能大于运行机时")));
    }

    @Test
    void reportListRespectsCenterPermissions() throws Exception {
        String adminToken = login("admin", "admin123");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportMonth", "2026-07");
        payload.put("statisticsUnitId", 2);
        payload.put("technicalCenterId", 17);
        payload.put("submitStatus", "SUBMITTED");
        payload.put("values", Map.of(
            "run_hours", "90",
            "service_hours", "60",
            "open_hours_international", "15",
            "open_hours_domestic", "10",
            "external_user_international", "2",
            "external_user_domestic", "4",
            "training_hours", "3",
            "enterprise_training_hours", "2",
            "safety_training_hours", "1"
        ));

        mockMvc.perform(post("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload)))
            .andExpect(status().isOk());

        String editorToken = login("zhangsan", "zhangsan123");

        mockMvc.perform(get("/api/reports")
                .param("month", "2026-07")
                .header(HttpHeaders.AUTHORIZATION, bearer(editorToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void exportTemplateAndImportWorkbookWork() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/reports/export/template")
                .param("month", "2026-08")
                .param("unitId", "1")
                .param("centerId", "1")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "import.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            buildImportWorkbook()
        );

        mockMvc.perform(multipart("/api/reports/import")
                .file(file)
                .param("reportMonth", "2026-08")
                .param("statisticsUnitId", "1")
                .param("technicalCenterId", "1")
                .param("submitStatus", "SUBMITTED")
                .param("overwriteExisting", "false")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.values.open_hours_total").value("30"))
            .andExpect(jsonPath("$.values.external_user_total").value("7"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("accessToken").asText();
    }

    private byte[] buildImportWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("月报数据");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("字段编码");
            header.createCell(1).setCellValue("填报值");

            writeValueRow(sheet, 1, "run_hours", "120");
            writeValueRow(sheet, 2, "service_hours", "80");
            writeValueRow(sheet, 3, "open_hours_international", "12");
            writeValueRow(sheet, 4, "open_hours_domestic", "18");
            writeValueRow(sheet, 5, "external_user_international", "2");
            writeValueRow(sheet, 6, "external_user_domestic", "5");
            writeValueRow(sheet, 7, "training_hours", "8");
            writeValueRow(sheet, 8, "enterprise_training_hours", "2");
            writeValueRow(sheet, 9, "safety_training_hours", "1");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void writeValueRow(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String fieldKey, String value) {
        var row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(fieldKey);
        row.createCell(1).setCellValue(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}