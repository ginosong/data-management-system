package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Idempotent migration: adds template field rule columns if they do not exist.
 * Replaces the SQL version to work around MySQL 8.0 lack of ADD COLUMN IF NOT EXISTS.
 */
public class V3__template_field_rules extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        addColumnIfAbsent(conn, "read_only_flag",      "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfAbsent(conn, "formula_expression",  "VARCHAR(255)");
        addColumnIfAbsent(conn, "helper_text",         "VARCHAR(255)");
        addColumnIfAbsent(conn, "min_value",           "DECIMAL(18, 2)");
        addColumnIfAbsent(conn, "max_value",           "DECIMAL(18, 2)");

        runUpdate(conn, "UPDATE report_template_field SET min_value = 0, max_value = 999999.99 WHERE value_type = 'DECIMAL'");
        setFieldHelper(conn, "run_hours",                  true,  null, "请填写本月设备实际运行机时，且不得小于服务机时。");
        setFieldHelper(conn, "service_hours",              true,  null, "请填写本月对内外服务机时，且不得大于运行机时。");
        setFieldHelper(conn, "open_hours_total",           false, "open_hours_international + open_hours_domestic", "系统自动汇总国际用户机时与国内用户机时。");
        setReadOnly(conn,    "open_hours_total",           "open_hours_international + open_hours_domestic", "系统自动汇总国际用户机时与国内用户机时。");
        setFieldHelper(conn, "open_hours_international",   true,  null, "请填写本月国际用户实际开放机时。");
        setFieldHelper(conn, "open_hours_domestic",        true,  null, "请填写本月国内用户实际开放机时，系统会自动汇总总开放机时。");
        setReadOnly(conn,    "external_user_total",        "external_user_international + external_user_domestic", "系统自动汇总国际用户数量与国内用户数量。");
        setFieldHelper(conn, "external_user_international",true,  null, "请填写本月国际外部用户数量。");
        setFieldHelper(conn, "external_user_domestic",     true,  null, "请填写本月国内外部用户数量，系统会自动汇总总数。");
        setFieldHelper(conn, "training_hours",             true,  null, "请填写本月技术培训课时。");
        setFieldHelper(conn, "enterprise_training_hours",  true,  null, "请填写本月企业技术培训课时。");
        setFieldHelper(conn, "safety_training_hours",      true,  null, "请填写本月安全培训课时。");
    }

    private void addColumnIfAbsent(Connection conn, String column, String definition) throws Exception {
        String schema = conn.getCatalog();
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'report_template_field' AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    runUpdate(conn, "ALTER TABLE report_template_field ADD COLUMN " + column + " " + definition);
                }
            }
        }
    }

    private void setFieldHelper(Connection conn, String fieldKey, boolean required, String formula, String helperText) throws Exception {
        String sql = "UPDATE report_template_field SET required_flag = ?, formula_expression = ?, helper_text = ? WHERE field_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, required);
            ps.setString(2, formula);
            ps.setString(3, helperText);
            ps.setString(4, fieldKey);
            ps.executeUpdate();
        }
    }

    private void setReadOnly(Connection conn, String fieldKey, String formula, String helperText) throws Exception {
        String sql = "UPDATE report_template_field SET read_only_flag = TRUE, required_flag = FALSE, formula_expression = ?, helper_text = ? WHERE field_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, formula);
            ps.setString(2, helperText);
            ps.setString(3, fieldKey);
            ps.executeUpdate();
        }
    }

    private void runUpdate(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
