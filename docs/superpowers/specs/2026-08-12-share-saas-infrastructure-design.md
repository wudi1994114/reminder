# Reminder 共享 SaaS 基础设施设计

## 目标

Reminder 后端部署到 saas-admin 所在服务器后，复用现有 PostgreSQL 与 Redis 容器，不再创建独立数据库或缓存服务。

## 数据库设计

- PostgreSQL 服务固定通过 `saas-app` Docker 网络访问 `saas-postgres:5432`。
- 数据库使用 `saas-admin`，连接 URL 为 `jdbc:postgresql://saas-postgres:5432/saas-admin?timezone=Asia/Shanghai`。
- 登录用户使用 `pguser`，密码仅由 Jenkins 的 `reminder-runtime-env` Secret file 注入。
- Reminder 业务表和 Quartz 表放在同一数据库的独立 `reminder` schema 中，避免与 saas-admin 的公共 schema 发生表名或迁移冲突。
- 应用连接建立后设置 `search_path=reminder`，JPA 同时使用 `reminder` 作为默认 schema。
- 首次初始化只允许在 `reminder` schema 中执行。现有建表 SQL 含有 `DROP TABLE`，部署流水线不得自动执行。

## Redis 设计

- Redis 服务固定通过 `saas-app` Docker 网络访问 `saas-redis:6379`。
- 密码仅由 Jenkins Secret file 注入，Compose 缺少密码时直接拒绝解析部署配置。
- Reminder 保留逻辑 DB 9，与 saas-admin 的业务缓存隔离；不执行全库清理命令。

## 配置与密钥边界

- Compose 和环境模板固化非敏感的主机、端口、数据库名、用户名、schema 与 Redis DB。
- PostgreSQL、Redis、JWT、微信和存储密码不得写入 Git、镜像或构建日志。
- 本地可保存一份被 Git 忽略且权限为 `600` 的共享基础设施密钥片段，用于更新 Jenkins Secret file；它不是可提交配置。
- 回滚继续按现有机制同时恢复镜像、运行环境和 Compose 快照。

## 验证

- 部署配置测试断言数据库 URL、用户、schema、Redis 地址和 DB 9，并断言两类密码为必填运行密钥。
- Maven 完整测试和打包必须通过。
- Docker Compose 配置必须能使用环境模板完成解析。
- Git 已跟踪内容不得出现用户提供的 PostgreSQL 或 Redis 明文密码。
