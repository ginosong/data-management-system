CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    permission_type VARCHAR(32) NOT NULL,
    route_path VARCHAR(128)
);

CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id),
    CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission_id)
);

CREATE TABLE statistics_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE technical_center (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_technical_center_unit FOREIGN KEY (unit_id) REFERENCES statistics_unit (id)
);

CREATE TABLE user_center_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    CONSTRAINT fk_user_center_permission_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_center_permission_center FOREIGN KEY (center_id) REFERENCES technical_center (id),
    CONSTRAINT uk_user_center_permission UNIQUE (user_id, center_id)
);

CREATE TABLE report_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL
);

CREATE TABLE report_template_field (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    excel_column VARCHAR(16) NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    sub_group_name VARCHAR(128),
    field_name VARCHAR(255) NOT NULL,
    field_key VARCHAR(128) NOT NULL,
    value_type VARCHAR(32) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_report_template_field_template FOREIGN KEY (template_id) REFERENCES report_template (id),
    CONSTRAINT uk_report_template_field UNIQUE (template_id, field_key)
);

CREATE TABLE monthly_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    report_month VARCHAR(7) NOT NULL,
    statistics_unit_id BIGINT NOT NULL,
    technical_center_id BIGINT NOT NULL,
    submit_status VARCHAR(32) NOT NULL,
    audit_status VARCHAR(32) NOT NULL,
    submitted_by BIGINT,
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_monthly_report_template FOREIGN KEY (template_id) REFERENCES report_template (id),
    CONSTRAINT fk_monthly_report_unit FOREIGN KEY (statistics_unit_id) REFERENCES statistics_unit (id),
    CONSTRAINT fk_monthly_report_center FOREIGN KEY (technical_center_id) REFERENCES technical_center (id),
    CONSTRAINT uk_monthly_report UNIQUE (template_id, report_month, technical_center_id)
);

CREATE TABLE monthly_report_field_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    field_key VARCHAR(128) NOT NULL,
    text_value VARCHAR(1000),
    numeric_value DECIMAL(18, 2),
    CONSTRAINT fk_monthly_report_field_value_report FOREIGN KEY (report_id) REFERENCES monthly_report (id),
    CONSTRAINT uk_monthly_report_field_value UNIQUE (report_id, field_key)
);

CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    operator_name VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT,
    detail VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_role (id, code, name, enabled) VALUES
    (1, 'ADMIN', '系统管理员', TRUE),
    (2, 'REPORT_EDITOR', '报表填报员', TRUE),
    (3, 'REPORT_VIEWER', '报表查看员', TRUE);

INSERT INTO sys_user (id, username, display_name, password_hash, enabled, is_admin, created_at, updated_at) VALUES
    (1, 'admin', '默认管理员', 'admin123', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'zhangsan', '张三', 'zhangsan123', TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
    (1, 1, 1),
    (2, 2, 2);

INSERT INTO sys_permission (id, code, name, permission_type, route_path) VALUES
    (1, 'dashboard:view', '查看工作台', 'PAGE', '/dashboard'),
    (2, 'reports:view', '查看月报列表', 'PAGE', '/reports'),
    (3, 'reports:edit', '录入和修改月报', 'ACTION', '/reports/edit'),
    (4, 'reports:export', '导出月报', 'ACTION', '/reports/export'),
    (5, 'master-data:view', '查看技术中心主数据', 'PAGE', '/master-data'),
    (6, 'master-data:edit', '维护技术中心主数据', 'ACTION', '/master-data/edit'),
    (7, 'system:user:view', '查看账号角色', 'PAGE', '/system/users'),
    (8, 'system:user:edit', '维护账号角色', 'ACTION', '/system/users/edit');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
    (1, 1, 1),
    (2, 1, 2),
    (3, 1, 3),
    (4, 1, 4),
    (5, 1, 5),
    (6, 1, 6),
    (7, 1, 7),
    (8, 1, 8),
    (9, 2, 1),
    (10, 2, 2),
    (11, 2, 3),
    (12, 2, 4),
    (13, 2, 5),
    (14, 3, 1),
    (15, 3, 2),
    (16, 3, 5),
    (17, 3, 7);

INSERT INTO statistics_unit (id, code, name, sort_order, enabled) VALUES
    (1, 'UNIT_TMRI', '转化医学研究院', 1, TRUE),
    (2, 'UNIT_ATC_TMRI', '分析测试中心/转化医学研究院', 2, TRUE),
    (3, 'UNIT_RUIJIN', '瑞金', 3, TRUE);

INSERT INTO technical_center (id, unit_id, code, name, sort_order, enabled) VALUES
    (1, 1, 'CENTER_001', '生物医学制造与检测（装备制造与认证）技术中心', 1, TRUE),
    (2, 1, 'CENTER_002', '生物信息（数字医学）技术中心', 2, TRUE),
    (3, 1, 'CENTER_003', '生物信息（临床统计与数据服务）技术中心', 3, TRUE),
    (4, 1, 'CENTER_004', '新药创制（Click合成）技术中心', 4, TRUE),
    (5, 1, 'CENTER_005', '新药创制（多肽合成）技术中心', 5, TRUE),
    (6, 1, 'CENTER_006', '新药创制（药物筛选）技术中心', 6, TRUE),
    (7, 1, 'CENTER_007', '临床组学（代谢功能基因组学）技术中心', 7, TRUE),
    (8, 1, 'CENTER_008', '临床组学（蛋白组、代谢组、空间组）技术中心', 8, TRUE),
    (9, 1, 'CENTER_009', '诊断试剂（规模化制备）技术中心', 9, TRUE),
    (10, 1, 'CENTER_010', '诊断试剂（核酸材料生物合成）技术中心', 10, TRUE),
    (11, 1, 'CENTER_011', '生物医学影像（生物透景）技术中心', 11, TRUE),
    (12, 1, 'CENTER_012', '模式动物技术中心', 12, TRUE),
    (13, 1, 'CENTER_013', '微生物资源技术中心', 13, TRUE),
    (14, 1, 'CENTER_014', '质粒保藏与开发技术中心', 14, TRUE),
    (15, 1, 'CENTER_015', '祥符实验室公共平台服务中心', 15, TRUE),
    (16, 1, 'CENTER_016', '涉人科技伦理服务中心', 16, TRUE),
    (17, 2, 'CENTER_017', '新药创制（安评与工艺）技术中心', 1, TRUE),
    (18, 2, 'CENTER_018', '生物医学影像（分子、细胞、组织、小动物活体多模态成像）技术中心', 2, TRUE),
    (19, 2, 'CENTER_019', '成像平台', 3, TRUE),
    (20, 2, 'CENTER_020', '质谱平台', 4, TRUE),
    (21, 3, 'CENTER_021', '质谱平台', 1, TRUE),
    (22, 3, 'CENTER_022', '测序平台', 2, TRUE),
    (23, 3, 'CENTER_023', '药筛平台', 3, TRUE),
    (24, 3, 'CENTER_024', '生信大数据平台', 4, TRUE),
    (25, 3, 'CENTER_025', '研究型病房', 5, TRUE);

INSERT INTO user_center_permission (id, user_id, center_id) VALUES
    (1, 2, 1),
    (2, 2, 2);

INSERT INTO report_template (id, code, name, description, status) VALUES
    (1, 'DMS_MONTHLY_TEMPLATE', '数据统计月报模板', '依据 Excel 模板生成的固定字段月报模板，支持按月填报、查询、统计和导出。', 'ACTIVE');

INSERT INTO report_template_field (id, template_id, excel_column, group_name, sub_group_name, field_name, field_key, value_type, required_flag, sort_order) VALUES
    (1, 1, 'C', '基本运行情况', NULL, '运行机时', 'run_hours', 'DECIMAL', FALSE, 1),
    (2, 1, 'D', '基本运行情况', NULL, '服务机时', 'service_hours', 'DECIMAL', FALSE, 2),
    (3, 1, 'E', '基本运行情况', NULL, '用户数量', 'user_count', 'DECIMAL', FALSE, 3),
    (4, 1, 'F', '基本运行情况', NULL, '企业用户数量', 'enterprise_user_count', 'DECIMAL', FALSE, 4),
    (5, 1, 'G', '基本运行情况', NULL, '企业用户机时数', 'enterprise_user_hours', 'DECIMAL', FALSE, 5),
    (6, 1, 'H', '对外开放情况', '对外开放机时', '总数', 'open_hours_total', 'DECIMAL', FALSE, 6),
    (7, 1, 'I', '对外开放情况', '对外开放机时', '国际用户机时数', 'open_hours_international', 'DECIMAL', FALSE, 7),
    (8, 1, 'J', '对外开放情况', '对外开放机时', '国内用户机时数', 'open_hours_domestic', 'DECIMAL', FALSE, 8),
    (9, 1, 'K', '对外开放情况', '对外开放机时', '上海用户机时', 'open_hours_shanghai', 'DECIMAL', FALSE, 9),
    (10, 1, 'L', '对外开放情况', '对外开放机时', '苏浙皖三省用户机时', 'open_hours_yrd', 'DECIMAL', FALSE, 10),
    (11, 1, 'M', '对外开放情况', '外部用户数量', '总数', 'external_user_total', 'DECIMAL', FALSE, 11),
    (12, 1, 'N', '对外开放情况', '外部用户数量', '国际用户数量', 'external_user_international', 'DECIMAL', FALSE, 12),
    (13, 1, 'O', '对外开放情况', '外部用户数量', '国内用户数量', 'external_user_domestic', 'DECIMAL', FALSE, 13),
    (14, 1, 'P', '对外开放情况', '外部用户数量', '苏浙皖三省用户数量', 'external_user_yrd', 'DECIMAL', FALSE, 14),
    (15, 1, 'Q', '对外开放情况', '外部用户数量', '上海用户数量', 'external_user_shanghai', 'DECIMAL', FALSE, 15),
    (16, 1, 'R', '服务企业成效', '服务企业数量/金额', '前期已服务且本月仍在服务的企业数/金额', 'active_enterprise_service_count_amount', 'TEXT', FALSE, 16),
    (17, 1, 'S', '服务企业成效', '服务企业数量/金额', '本月新增服务企业数/经费', 'new_enterprise_service_count_fee', 'TEXT', FALSE, 17),
    (18, 1, 'T', '服务企业成效', '服务上海企业数量/金额', '前期已服务且本月仍在服务的企业数/金额', 'active_shanghai_enterprise_service_count_amount', 'TEXT', FALSE, 18),
    (19, 1, 'U', '服务企业成效', '服务上海企业数量/金额', '本月新增服务企业数/经费', 'new_shanghai_enterprise_service_count_fee', 'TEXT', FALSE, 19),
    (20, 1, 'V', '服务企业成效', '服务长三角企业（不含上海）数量/金额', '前期已服务且本月仍在服务的企业数/金额', 'active_yrd_enterprise_service_count_amount', 'TEXT', FALSE, 20),
    (21, 1, 'W', '服务企业成效', '服务长三角企业（不含上海）数量/金额', '本月新增服务企业数/经费', 'new_yrd_enterprise_service_count_fee', 'TEXT', FALSE, 21),
    (22, 1, 'X', '服务科研', '承担课题（项目）数量/经费', '前期课题（项目）本月仍在承担数/经费', 'active_project_count_funding', 'TEXT', FALSE, 22),
    (23, 1, 'Y', '服务科研', '承担课题（项目）数量/经费', '本月新增课题（项目）数/经费', 'new_project_count_funding', 'TEXT', FALSE, 23),
    (24, 1, 'Z', '服务科研', '承担国家课题（项目）数量/经费', '前期课题（项目）本月仍在承担数/经费', 'active_national_project_count_funding', 'TEXT', FALSE, 24),
    (25, 1, 'AA', '服务科研', '承担国家课题（项目）数量/经费', '本月新增课题（项目）数/经费', 'new_national_project_count_funding', 'TEXT', FALSE, 25),
    (26, 1, 'AB', '服务科研', '承担省市课题（项目）数量/经费', '前期课题（项目）本月仍在承担数/经费', 'active_provincial_project_count_funding', 'TEXT', FALSE, 26),
    (27, 1, 'AC', '服务科研', '承担省市课题（项目）数量/经费', '本月新增课题（项目）数/经费', 'new_provincial_project_count_funding', 'TEXT', FALSE, 27),
    (28, 1, 'AD', '服务科研', '承担上海课题（项目）数量/经费', '前期课题（项目）本月仍在承担数/经费', 'active_shanghai_project_count_funding', 'TEXT', FALSE, 28),
    (29, 1, 'AE', '服务科研', '承担上海课题（项目）数量/经费', '本月新增课题（项目）数/经费', 'new_shanghai_project_count_funding', 'TEXT', FALSE, 29),
    (30, 1, 'AF', '服务科研', NULL, '专利数量', 'patent_count', 'DECIMAL', FALSE, 30),
    (31, 1, 'AG', '服务科研', NULL, '发明专利数量', 'invention_patent_count', 'DECIMAL', FALSE, 31),
    (32, 1, 'AH', '服务科研', NULL, 'PCT专利数量', 'pct_patent_count', 'DECIMAL', FALSE, 32),
    (33, 1, 'AI', '服务科研', NULL, '发表论文数量', 'paper_count', 'DECIMAL', FALSE, 33),
    (34, 1, 'AJ', '服务科研', NULL, 'CNS论文数量', 'cns_paper_count', 'DECIMAL', FALSE, 34),
    (35, 1, 'AK', '服务科研', NULL, '技术转让数量', 'tech_transfer_count', 'DECIMAL', FALSE, 35),
    (36, 1, 'AL', '服务科研', NULL, '技术许可数量', 'tech_license_count', 'DECIMAL', FALSE, 36),
    (37, 1, 'AM', '服务科研', NULL, '出版标准数量', 'standard_publication_count', 'DECIMAL', FALSE, 37),
    (38, 1, 'AN', '服务科普工作', NULL, '组织科普活动次数', 'science_activity_count', 'DECIMAL', FALSE, 38),
    (39, 1, 'AO', '服务科普工作', NULL, '参加人（次）数', 'science_participant_count', 'DECIMAL', FALSE, 39),
    (40, 1, 'AP', '技术培训课时数', NULL, '课时数', 'training_hours', 'DECIMAL', FALSE, 40),
    (41, 1, 'AQ', '开展企业技术培训课时数', NULL, '课时数', 'enterprise_training_hours', 'DECIMAL', FALSE, 41),
    (42, 1, 'AR', '安全培训课时数', NULL, '课时数', 'safety_training_hours', 'DECIMAL', FALSE, 42);

INSERT INTO monthly_report (id, template_id, report_month, statistics_unit_id, technical_center_id, submit_status, audit_status, submitted_by, submitted_at, created_at, updated_at) VALUES
    (1, 1, '2026-04', 1, 1, 'SUBMITTED', 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, '2026-04', 1, 2, 'SUBMITTED', 'APPROVED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 1, '2026-03', 1, 1, 'SUBMITTED', 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO monthly_report_field_value (id, report_id, field_key, text_value, numeric_value) VALUES
    (1, 1, 'run_hours', NULL, 168.00),
    (2, 1, 'service_hours', NULL, 120.00),
    (3, 1, 'user_count', NULL, 54.00),
    (4, 1, 'enterprise_user_count', NULL, 19.00),
    (5, 1, 'enterprise_user_hours', NULL, 68.00),
    (6, 1, 'open_hours_total', NULL, 92.00),
    (7, 1, 'open_hours_domestic', NULL, 88.00),
    (8, 1, 'external_user_total', NULL, 31.00),
    (9, 1, 'active_enterprise_service_count_amount', '6 / 45.8', NULL),
    (10, 1, 'new_enterprise_service_count_fee', '2 / 16.4', NULL),
    (11, 1, 'active_project_count_funding', '4 / 128.0', NULL),
    (12, 1, 'new_project_count_funding', '1 / 42.0', NULL),
    (13, 1, 'patent_count', NULL, 3.00),
    (14, 1, 'invention_patent_count', NULL, 2.00),
    (15, 1, 'paper_count', NULL, 6.00),
    (16, 1, 'science_activity_count', NULL, 2.00),
    (17, 1, 'science_participant_count', NULL, 160.00),
    (18, 1, 'training_hours', NULL, 24.00),
    (19, 1, 'enterprise_training_hours', NULL, 10.00),
    (20, 1, 'safety_training_hours', NULL, 8.00),
    (21, 2, 'run_hours', NULL, 142.00),
    (22, 2, 'service_hours', NULL, 103.00),
    (23, 2, 'user_count', NULL, 48.00),
    (24, 2, 'enterprise_user_count', NULL, 15.00),
    (25, 2, 'open_hours_total', NULL, 74.00),
    (26, 2, 'external_user_total', NULL, 26.00),
    (27, 2, 'active_enterprise_service_count_amount', '5 / 34.2', NULL),
    (28, 2, 'new_enterprise_service_count_fee', '1 / 8.0', NULL),
    (29, 2, 'active_project_count_funding', '3 / 95.0', NULL),
    (30, 2, 'new_project_count_funding', '1 / 20.0', NULL),
    (31, 2, 'patent_count', NULL, 1.00),
    (32, 2, 'paper_count', NULL, 3.00),
    (33, 2, 'science_activity_count', NULL, 1.00),
    (34, 2, 'science_participant_count', NULL, 90.00),
    (35, 2, 'training_hours', NULL, 16.00),
    (36, 2, 'enterprise_training_hours', NULL, 6.00),
    (37, 2, 'safety_training_hours', NULL, 4.00),
    (38, 3, 'run_hours', NULL, 156.00),
    (39, 3, 'service_hours', NULL, 110.00),
    (40, 3, 'user_count', NULL, 51.00),
    (41, 3, 'enterprise_user_count', NULL, 17.00),
    (42, 3, 'open_hours_total', NULL, 80.00),
    (43, 3, 'external_user_total', NULL, 28.00),
    (44, 3, 'active_enterprise_service_count_amount', '4 / 28.6', NULL),
    (45, 3, 'new_enterprise_service_count_fee', '2 / 9.5', NULL),
    (46, 3, 'active_project_count_funding', '3 / 102.0', NULL),
    (47, 3, 'new_project_count_funding', '1 / 24.0', NULL),
    (48, 3, 'patent_count', NULL, 2.00),
    (49, 3, 'paper_count', NULL, 4.00),
    (50, 3, 'science_activity_count', NULL, 1.00),
    (51, 3, 'science_participant_count', NULL, 120.00),
    (52, 3, 'training_hours', NULL, 18.00),
    (53, 3, 'enterprise_training_hours', NULL, 7.00),
    (54, 3, 'safety_training_hours', NULL, 5.00);

INSERT INTO operation_log (id, operator_id, operator_name, action, target_type, target_id, detail, created_at) VALUES
    (1, 1, '默认管理员', 'SEED_INIT', 'SYSTEM', 1, '初始化重大科技设施运行数据管理系统基础角色、主数据、模板与样例月报。', CURRENT_TIMESTAMP);
