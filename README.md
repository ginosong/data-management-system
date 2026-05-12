# 重大科技设施运行数据管理系统

重大科技设施运行数据管理系统是一个独立的 PC 端后台项目，用于按月填报和管理固定 Excel 模板的财务/运营统计数据。当前版本已经完成独立工作区初始化、Spring Boot 后端骨架、React 管理台界面、模板字段初始化、中心级权限样例和月报 CRUD 基础接口。

## 当前技术栈

- 后端：Java 17、Spring Boot 3.5、Spring Data JPA、Flyway、MySQL/H2
- 前端：React 18、TypeScript、Vite 5
- 数据库：MySQL（开发默认 profile 使用 H2，方便本地快速启动）

## 已落地的核心能力

- 固定月报模板：依据 Excel 模板初始化统计字段、分组和样例数据
- 认证与会话：支持用户名密码登录、JWT 访问令牌、刷新令牌和退出登录
- 月报管理：支持列表查询、详情、创建、修改、删除、公式字段回填和业务规则校验
- 查询统计：支持月度概览、按统计单位汇总、技术中心亮点展示
- 主数据：支持查看统计单位和技术中心，支持新增和停用技术中心/统计单位
- 权限管理：支持账号、角色、中心级数据权限分配、重置密码和停用
- Excel 导入导出：支持模板导出、已填报月报导出和按模板回填导入
- 前端工作台：包含登录页、工作台、月报填报、月报列表、主数据、系统管理等权限化页面

## 目录结构

```text
.
├── backend
│   ├── src/main/java/com/datamanagement/system
│   ├── src/main/resources/db/migration
│   └── pom.xml
├── frontend
│   ├── src
│   ├── package.json
│   └── vite.config.ts
└── .github/copilot-instructions.md
```

## 默认账号

- 管理员：`admin` / `admin123`
- 示例填报员：`zhangsan` / `zhangsan123`

说明：示例账号已接入 JWT 登录、刷新和中心级数据权限控制，可直接用于本地联调。

## 后端启动说明

### 默认（MySQL）

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw spring-boot:run
```

默认连接本地 MySQL：`jdbc:mysql://localhost:3306/data_management_system`，账号 `root`，密码 `Iwish2024NB`。可通过环境变量 `DMS_DB_URL` / `DMS_DB_USERNAME` / `DMS_DB_PASSWORD` 覆盖。

### 本地无 MySQL（H2 内存库）

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

说明：H2 为内存库，服务重启后数据清空，仅供快速调试使用。
## 前端启动说明

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

Vite 已配置 `/api` 代理到 `http://localhost:8080`。

## 当前实现说明

- 登录后前端会自动拉取当前用户信息，并在访问令牌过期时尝试刷新会话
- 工作台、月报、主数据、系统管理菜单会按权限动态显示
- 月报模板字段已支持必填、只读、帮助提示、数值范围和公式汇总
- Excel 导入导出与页面填报共用同一套后端校验逻辑，避免线下模板与系统规则不一致
- 集成测试已覆盖认证、权限范围、公式校验和 Excel 导入导出关键链路

## 验证方式

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw test
```

```bash
cd frontend
npm run build
```

## 可继续推进的工作

1. 补充真实审批流和审核意见流转，区分草稿、已提交、退回、已审核等完整状态机。
2. 增加操作审计日志查询、密码策略和账号锁定等安全治理能力。
3. 接入 CI/CD、制品发布和部署监控，形成更稳定的交付链路。
