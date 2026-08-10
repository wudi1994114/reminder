# Reminder 迁出微信云并整合后端设计

## 1. 目标

本次改造同时完成两件事：

1. 小程序、H5 和 Web 前端统一通过标准 HTTPS API 访问后端，不再使用 `wx.cloud.callContainer`、云托管网关身份头或微信云存储运行时能力。
2. 后端从四个 Maven 子模块、两个启动进程收敛为一个 Spring Boot 项目、一个可执行 JAR 和一个 Docker 容器。

最终部署复用现有 SaaS 服务器的 Jenkins、内部镜像仓库、Docker Compose、Nginx HTTPS、PostgreSQL、Redis、Nacos，以及 saas-admin 已有的审计存储服务；底层仍为 MinIO，但 Reminder 不直连 MinIO。Reminder 保持独立服务和数据边界。

## 2. 现状与主要问题

- uni-app 请求层优先调用微信云托管，普通 HTTP 只是降级路径。
- 微信登录同时存在云网关注入 OpenID 和标准 `wx.login` code 换取 OpenID 两套流程。
- 头像上传、删除和部分音频播放依赖微信云存储；后端现有普通上传接口最终仍写入微信云存储。
- 后端包含 `common`、`core`、`job`、`stream-consumer` 四个 Maven 子模块，`core` 与 `job` 分别启动。
- `core` 与 `job` 重复定义 Repository、Redis、JPA、时区、Nacos 和业务服务；直接把两个包同时扫描进一个进程会产生 Bean 冲突。
- `stream-consumer` 只有 POM 和空目录，但 `core` 仍向 Redis Stream 发布复杂提醒生成事件，链路没有完整消费者。

## 3. 方案比较

### 方案 A：单模块模块化单体（采用）

把全部后端源码收敛到 `reminder-backend/src`，只保留一个 POM 和一个启动类。按包保留 API、领域模型、调度、通知和基础设施边界。Quartz 与 HTTP API 在同一进程运行。

优点：完全满足“一个整体”；只有一个构建产物、容器、配置和发布入口；可以彻底消除重复 Bean。缺点：文件迁移量较大，需要认真合并重复实现。

### 方案 B：新增聚合启动模块

保留原有 Maven 子模块，新增一个依赖全部模块的启动模块，只部署一个 JAR。

优点：改动较小。缺点：源码仍是多个项目，重复实现依然存在，不符合本次目标，因此不采用。

### 方案 C：只把调度代码并入 core

保留 `common`，删除 `job` 和空的 consumer，形成两个 Maven 模块。

优点：风险低于完全扁平化。缺点：仍有多模块构建和版本边界，也不符合“后端成为一个整体”，因此不采用。

## 4. 目标架构

### 4.1 后端目录与运行单元

后端根目录本身成为唯一 Maven 项目：

```text
reminder-backend/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/java/
    │   ├── com/common/reminder/   # 实体、枚举和通用工具
    │   ├── com/core/reminder/     # HTTP API、认证和核心业务
    │   └── com/task/reminder/     # Quartz Job 和通知发送器
    ├── main/resources/
    └── test/java/
```

一个启动类扫描上述三个包，启用 JPA、Redis、Quartz、Spring Scheduling 和 AOP。保留包级边界以降低迁移风险，但不再保留子模块 POM 或独立启动类。

### 4.2 重复代码合并规则

- Repository 只保留核心模块版本；调度代码改为注入同一套 Repository。
- 核心提醒服务作为唯一提醒业务实现，补齐调度所需的查询、生成和安排方法。
- Redis、JPA、时区和 Nacos 各保留一套配置。
- Quartz 专属配置、Job 和通知 Sender 合并进唯一应用上下文。
- 删除空的 Stream Consumer 和发布器。复杂提醒创建、修改后在同一事务内直接生成未来三个月的简单提醒，不再绕 Redis Stream。
- Redis 继续用于缓存和待发送提醒，不承担缺失消费者的异步业务一致性职责。

### 4.3 前后端交互

- 请求入口统一为 `uni.request`/`uni.uploadFile` 或 Web Axios。
- API 基础地址由构建环境变量 `VITE_API_BASE_URL` 提供；开发默认 `http://127.0.0.1:8080/api`，生产默认 `https://reminder-api.wwmty.com/api`。
- 所有受保护请求继续使用 `Authorization: Bearer <JWT>`。
- 微信小程序登录统一为：`wx.login` 获取临时 code → HTTPS POST `/api/auth/wechat/login` → 后端调用微信 `jscode2session` → 返回 JWT。
- 删除 `/cloud-login`、`X-WX-OPENID`、`X-WX-SERVICE` 和所有 `wx.cloud` 初始化及调用。

### 4.4 文件存储

- 前端通过 multipart 上传文件到 Reminder 后端。
- Reminder 使用独立的 `reminder` APP_CLIENT 调用 saas-admin 的 `/auth/app-login` 与 `/sys/storage/upload`，请求携带 `X-Project-Code: reminder`，由 saas-admin 完成租户审计和 MinIO 写入。
- saas-admin 强制把对象归档到 `app/reminder/`；Reminder 会校验返回 key 的作用域，拒绝跨项目结果。
- MinIO 地址、访问凭据、Bucket 和 URL 解析继续只由 saas-admin 管理；Reminder 仓库及容器中不保存 MinIO 凭据。
- APP_CLIENT 的 AppID/Secret 存放在 Jenkins 凭据 `reminder-saas-storage-app` 中，部署时注入环境变量，不写入源码、Compose 或日志。
- 对外返回 saas-admin 解析出的稳定 HTTPS URL，不返回 `cloud://` FileID 或临时签名地址。
- 已有微信云文件在停用前导出，再通过受审计的存储接口上传并更新数据库 URL。新代码不保留微信云运行时回退。

## 5. 配置与部署

### 5.1 应用配置

统一配置包含 PostgreSQL、Redis、Quartz、JWT、微信小程序、邮件、saas-admin 存储客户端和 Nacos。敏感值只允许通过环境变量、Jenkins 凭据或 Nacos 注入。

数据库使用独立数据库和用户；Redis 使用独立 database 与 `reminder:` Key 前缀；存储对象使用 `app/reminder/` 独立前缀。这样复用同一台服务器的中间件，但不与 SaaS 业务数据混用。

### 5.2 发布形态

- 镜像名：`reminder-backend`。
- 容器名：`reminder-backend`。
- 容器内端口：`8080`；宿主机只绑定 `127.0.0.1` 的独立端口。
- Nginx 使用 `reminder-api.wwmty.com` 提供 HTTPS，并反向代理到 Reminder 容器。
- Jenkins 流程与 SaaS 后端一致：拉取代码、Maven 构建、Docker 构建、推送内部镜像仓库、调用部署脚本、等待健康检查。
- Reminder 使用独立 Compose 服务和独立部署入口；不修改或重启现有 SaaS 后端容器。

## 6. 错误处理与兼容

- HTTP 请求统一按 2xx 成功处理，401/403 保持登录态错误语义，网络错误交给现有 UI 提示。
- 文件上传校验类型与大小；saas-admin 登录、审计或 MinIO 写入失败时不更新用户资料，上传成功后才更新头像 URL。
- 微信 code 无效、微信接口超时或缺少 OpenID 时返回明确的 401/502 响应，不依赖网关注入身份。
- Quartz Job 记录单次失败并允许后续周期继续执行；同一提醒发送继续使用数据库/Redis 的防重复约束。
- 旧 JWT 和现有数据库表结构保持兼容，不在本次改造中升级 Spring Boot 主版本或重做业务模型。

## 7. 验证标准

### 7.1 后端

- Maven 只有一个项目，并只生成一个可执行 JAR。
- Spring 上下文可同时加载 API、Quartz Job 和 Sender，且没有重复 Bean。
- 标准微信 code 登录测试通过，云登录路由不存在。
- 创建和修改复杂提醒会直接生成对应简单提醒。
- saas-admin APP_CLIENT 登录、审计上传、401 重试、失败处理和 URL 解析测试通过。
- Docker 镜像可启动，健康检查可访问。

### 7.2 前端

- 源码中不存在 `wx.cloud`、`callContainer`、`X-WX-SERVICE`、`cloud://` 或云托管生产域名。
- 微信登录只调用标准 HTTP 登录接口。
- 头像和普通文件只通过后端 multipart 接口上传。
- H5 与微信小程序构建通过。

### 7.3 部署前验收

- 在微信公众平台配置生产 HTTPS request/upload/download 合法域名。
- DNS、TLS、Nginx、独立数据库、Redis 前缀、saas-admin APP_CLIENT/Jenkins 凭据和 Nacos 配置准备完成。
- 旧云文件导出并完成 URL 迁移抽样验证。
- 新容器、日志、健康接口和一个真实微信登录完成验证后，才停止旧云托管服务。

## 8. 实施顺序

1. 建立单模块后端骨架并合并公共、核心和调度代码。
2. 消除重复 Repository、服务和配置，改为进程内复杂提醒生成。
3. 将文件存储从微信云替换为 saas-admin 审计存储接口（底层 MinIO）。
4. 将 uni-app 请求、登录、上传和媒体读取改为纯 HTTP。
5. 更新 Docker、Jenkins 和部署样例。
6. 执行后端测试、前端构建、静态云依赖扫描和容器启动验证。

本设计实施期间只先创建独立的 saas-admin APP_CLIENT 和 Jenkins 凭据以建立审计身份；Reminder 服务部署和旧云数据切换仍在代码验证通过后单独执行。
