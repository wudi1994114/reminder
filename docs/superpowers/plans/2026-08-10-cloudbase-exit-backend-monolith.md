# CloudBase Exit and Backend Monolith Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task with review checkpoints.

**Goal:** Remove every runtime dependency on WeChat Cloud Hosting/Cloud Storage, consolidate the backend into one Spring Boot JAR/container, and provide deployment assets compatible with the existing SaaS server pipeline.

**Architecture:** `reminder-backend` becomes the only Maven module and owns all common models, HTTP APIs, Quartz jobs, senders, and infrastructure configuration. The uni-app talks to the backend only through standard HTTP and multipart upload; standard `wx.login` code exchange remains the only WeChat authentication flow. MinIO replaces WeChat Cloud Storage, while PostgreSQL, Redis, Nacos, Nginx, Jenkins, and the internal registry are reused with Reminder-specific isolation.

**Tech Stack:** Java 17, Spring Boot 2.7.18, Spring MVC/Security/JPA/Redis/Quartz, PostgreSQL, MinIO Java SDK, Maven, Vue 3/uni-app, pnpm, Docker, Jenkins, Nginx.

---

### Task 1: Capture a reproducible baseline and add migration guardrails

**Files:**
- Create: `reminder-uni-app/scripts/assert-no-cloudbase.mjs`
- Modify: `reminder-uni-app/package.json`

- [ ] Record `git status --short` and the exact pre-existing uni-app dirty paths so later staging can exclude user work.
- [ ] Run the current backend tests from `reminder-backend` and save the actual baseline result in the execution notes; do not hide pre-existing failures.
- [ ] Run `pnpm build:mp-weixin` and `pnpm build:h5` from `reminder-uni-app` to establish the frontend baseline.
- [ ] Add a Node script that recursively scans `src` and fails on runtime tokens `wx.cloud`, `callContainer`, `X-WX-`, `cloud://`, `/cloud-login`, and the removed cloud config import.
- [ ] Add `"check:no-cloudbase": "node scripts/assert-no-cloudbase.mjs"` to `package.json`.
- [ ] Run `pnpm check:no-cloudbase` and verify it fails against the current implementation, proving the guard works.
- [ ] Commit only the guardrail files: `test: add cloudbase removal guard`.

### Task 2: Flatten the Maven reactor into one Spring Boot project

**Files:**
- Modify: `reminder-backend/pom.xml`
- Move: `reminder-backend/reminder-common/src/main/java/com/common/reminder/**` → `reminder-backend/src/main/java/com/common/reminder/**`
- Move: `reminder-backend/reminder-core/src/main/java/com/core/reminder/**` → `reminder-backend/src/main/java/com/core/reminder/**`
- Move: selected `reminder-backend/reminder-job/src/main/java/com/task/reminder/**` → `reminder-backend/src/main/java/com/task/reminder/**`
- Move: core resources/tests and job Quartz resources/template/tests into `reminder-backend/src/main/resources` and `reminder-backend/src/test/java`
- Delete: child module POMs and obsolete module directories after their retained content is moved

- [ ] Replace root packaging `pom` and `<modules>` with a normal JAR project and the union of runtime/test dependencies from common, core, and job, plus `spring-boot-starter-actuator` and MinIO.
- [ ] Preserve dependency versions already proven by the child modules; keep one Spring Boot Maven plugin with `com.core.reminder.ReminderApplication` as main class.
- [ ] Move common and core sources/resources/tests with `git mv` so history remains traceable.
- [ ] Move only Quartz configuration, jobs, senders, `UserPreferenceJobService`, `quartz.sql`, and the email template from the job module.
- [ ] Do not move duplicate job Repository, Redis/JPA/Nacos/time-zone configuration, `ReminderEventServiceImpl`, `UserCacheService`, or `RedisUtils`; task code will be changed to use core equivalents.
- [ ] Delete the empty stream-consumer module and all obsolete child POM/Docker/run scripts after validating their exact contents.
- [ ] Rename the retained startup class to `ReminderApplication`, scan `com.common.reminder`, `com.core.reminder`, and `com.task.reminder`, and enable scheduling/JPA repositories/entity scanning once.
- [ ] Run `mvn -DskipTests compile`; expected intermediate result is limited to task-package imports that still point at deleted duplicates.

### Task 3: Merge Quartz jobs into the core application context

**Files:**
- Modify: `reminder-backend/src/main/java/com/task/reminder/job/PrepareReminderJob.java`
- Modify: `reminder-backend/src/main/java/com/task/reminder/job/SendReminderJob.java`
- Modify: `reminder-backend/src/main/java/com/task/reminder/job/MonthlyComplexReminderJob.java`
- Modify: `reminder-backend/src/main/java/com/task/reminder/service/UserPreferenceJobService.java`
- Modify: `reminder-backend/src/main/java/com/task/reminder/config/QuartzConfig.java`
- Modify: `reminder-backend/src/main/java/com/task/reminder/config/QuartzInitializer.java`
- Modify: `reminder-backend/src/main/java/com/task/reminder/sender/**`
- Test: `reminder-backend/src/test/java/com/core/reminder/ReminderApplicationContextTest.java`

- [ ] Add a context test that asserts the HTTP controller, each Quartz job, and each sender factory are created in one Spring context; run it first and capture the failing state.
- [ ] Replace all `com.task.reminder.repository`, task `RedisUtils`, task cache service, and task reminder service imports with the retained core implementations.
- [ ] Where method contracts differ, add the smallest scheduling-oriented method to the core service/repository rather than keeping duplicate business services.
- [ ] Make Quartz use the same application `DataSource`; retain JDBC job persistence and remove duplicate task datasource/JPA setup.
- [ ] Ensure every job records one execution failure without stopping later schedules and keeps the existing anti-duplicate logic.
- [ ] Merge core and job `application.yaml` settings so one process has PostgreSQL, Redis, Quartz, mail, JWT, Nacos, and scheduling configuration.
- [ ] Run `mvn -Dtest=ReminderApplicationContextTest test`, then `mvn test`.
- [ ] Commit: `refactor: consolidate backend into one application`.

### Task 4: Remove the broken Redis Stream boundary

**Files:**
- Modify: `reminder-backend/src/main/java/com/core/reminder/controller/ReminderEventController.java`
- Modify: `reminder-backend/src/main/java/com/core/reminder/service/ReminderEventServiceImpl.java`
- Delete: `reminder-backend/src/main/java/com/core/reminder/utils/StreamEventPublisher.java`
- Modify: `reminder-backend/src/main/resources/application.yaml`
- Test: `reminder-backend/src/test/java/com/core/reminder/controller/ReminderEventControllerTest.java`

- [ ] Add controller/service tests proving complex reminder create and update call direct simple-reminder generation and that a generation failure rolls the transaction back.
- [ ] Run the focused tests and confirm they fail while the controller still publishes a stream event.
- [ ] Replace `StreamEventPublisher` injection/calls with one transactional service operation that saves the complex reminder and generates the configured future window in-process.
- [ ] Remove the publisher class and `reminder.stream.*` configuration.
- [ ] Run the focused tests and `mvn test`.
- [ ] Commit: `refactor: generate reminder instances in process`.

### Task 5: Replace WeChat Cloud Storage with MinIO

**Files:**
- Create: `reminder-backend/src/main/java/com/core/reminder/config/MinioProperties.java`
- Create: `reminder-backend/src/main/java/com/core/reminder/config/MinioConfig.java`
- Modify: `reminder-backend/src/main/java/com/core/reminder/service/StorageService.java`
- Modify: `reminder-backend/src/main/java/com/core/reminder/controller/FileUploadController.java`
- Modify: `reminder-backend/src/main/resources/application.yaml`
- Test: `reminder-backend/src/test/java/com/core/reminder/service/StorageServiceTest.java`

- [ ] Add tests around a mocked `MinioClient`: accepted image upload creates the bucket if needed, writes an object under a safe generated key, and returns `${PUBLIC_BASE_URL}/${bucket}/${object}`; invalid type/size and SDK failures return no persisted profile URL.
- [ ] Run the test and verify failure against the current TCB implementation.
- [ ] Bind endpoint, access key, secret key, bucket (`reminder`), public base URL, max size, and allowed content types from `MINIO_*` environment variables.
- [ ] Implement bucket existence/creation and `putObject` through the MinIO SDK; normalize names and never return credentials or temporary URLs.
- [ ] Update the controller response to return a stable `url` and optional `objectName`, with clear 4xx/5xx errors.
- [ ] Remove TCB upload API/token coupling from storage code while retaining unrelated Tencent TTS/STS features.
- [ ] Run `mvn -Dtest=StorageServiceTest test` and `mvn test`.
- [ ] Commit: `feat: store uploaded files in minio`.

### Task 6: Keep only standard WeChat code login

**Files:**
- Modify: `reminder-backend/src/main/java/com/core/reminder/controller/WechatAuthController.java`
- Modify: `reminder-backend/src/main/java/com/core/reminder/service/WechatAuthService.java`
- Modify: `reminder-backend/src/main/java/com/core/reminder/config/SecurityConfig.java`
- Test: `reminder-backend/src/test/java/com/core/reminder/controller/WechatAuthControllerTest.java`

- [ ] Add MockMvc tests proving `POST /api/auth/wechat/login` accepts a code and delegates to `jscode2session`, while `POST /api/auth/wechat/cloud-login` returns 404.
- [ ] Run the focused test and verify the cloud route assertion fails first.
- [ ] Delete the cloud-login controller method, gateway-header handling, and service method that trusts an injected OpenID.
- [ ] Keep existing account-binding, JWT, and profile behavior behind the code-login path.
- [ ] Remove CloudBase-specific avatar URL classification; treat stored avatar values as ordinary HTTPS URLs.
- [ ] Run the focused tests and `mvn test`.
- [ ] Commit: `refactor: use standard wechat code login`.

### Task 7: Convert uni-app networking and authentication to HTTP only

**Files:**
- Modify: `reminder-uni-app/src/api/http.js`
- Modify: `reminder-uni-app/src/api/index.js`
- Modify: `reminder-uni-app/src/services/api.js`
- Modify: `reminder-uni-app/src/api/wechat.js`
- Modify: `reminder-uni-app/src/config/api.js`
- Delete: `reminder-uni-app/src/config/cloud.js`
- Delete: `reminder-uni-app/src/services/api.js.backup`
- Modify: `reminder-uni-app/src/main.js`
- Create: `reminder-uni-app/.env.development`
- Create: `reminder-uni-app/.env.production.example`

- [ ] Reduce the request abstraction to `uni.request`, normalize relative API paths against `VITE_API_BASE_URL`, attach JWT, and preserve existing 2xx/401/403 behavior.
- [ ] Remove `callContainer` exports/imports and all cloud config/runtime initialization.
- [ ] Make WeChat login always call `wx.login` then `POST /auth/wechat/login` through the shared HTTP client.
- [ ] Set development API base to `http://127.0.0.1:8080/api`; document production as `https://reminder-api.wwmty.com/api` without committing credentials.
- [ ] Delete the obsolete backup source so static scans cannot regress from copied CloudBase code.
- [ ] Run `pnpm check:no-cloudbase`; remaining expected failures must now be confined to upload/media files.

### Task 8: Convert uploads and media URLs to backend/HTTPS only

**Files:**
- Modify: `reminder-uni-app/src/api/upload.js`
- Modify: `reminder-uni-app/src/components/UserInfoEditor.vue`
- Modify: `reminder-uni-app/src/pages/mine/mine.vue`
- Modify: `reminder-uni-app/src/pages/running-duck/strength-training.vue`
- Modify: `reminder-backend/src/main/java/com/common/reminder/model/Exercise.java`

- [ ] Make upload use only `uni.uploadFile` against `/files/upload`, add the JWT header, and consume the backend `url` response.
- [ ] Remove cloud deletion and temporary URL conversion; avatar components display the returned HTTPS URL directly.
- [ ] Replace the hardcoded `cloud://` training audio with an HTTPS-configurable value and make missing audio non-fatal.
- [ ] Update model comments/contracts to describe HTTPS URLs only.
- [ ] Carefully patch only cloud-related hunks in user-modified Vue files and inspect `git diff` to prove unrelated edits remain intact.
- [ ] Run `pnpm check:no-cloudbase`, `pnpm build:mp-weixin`, and `pnpm build:h5`.
- [ ] Commit only migration hunks using explicit paths/hunk review: `refactor: route uni-app through standard http`.

### Task 9: Add server-compatible build and deployment assets

**Files:**
- Modify: `reminder-backend/Dockerfile`
- Create: `reminder-backend/.dockerignore`
- Create: `reminder-backend/Jenkinsfile`
- Create: `reminder-backend/deploy/docker-compose.reminder.yml`
- Create: `reminder-backend/deploy/.env.example`
- Create: `reminder-backend/deploy/deploy-reminder.sh`
- Create: `reminder-backend/deploy/nginx/reminder-api.conf`
- Modify: `部署手册.md`

- [ ] Build one layered Java 17 runtime image from the root JAR and add a non-root user plus actuator health check.
- [ ] Add Jenkins stages matching the confirmed SaaS path: checkout, Maven test/package, Docker build, push to the internal Gitea registry, invoke the Reminder-only deploy script, and verify health.
- [ ] Add one Compose service named `reminder-backend`, bind its host port to `127.0.0.1`, attach to the existing gateway/middleware networks, and inject all secrets from an external env file.
- [ ] Add a deploy script that validates the explicit Reminder service/image, backs up only the Reminder compose file, updates only that service, waits for `/actuator/health`, and rolls back that service on failure.
- [ ] Add an Nginx HTTPS server block for `reminder-api.wwmty.com` with request/upload limits and proxy headers; do not alter SaaS routes.
- [ ] Document prerequisite DNS/TLS, independent PostgreSQL database/user, Redis DB/prefix, Nacos data ID, MinIO bucket, WeChat legal domains, and the final cutover/rollback order.
- [ ] Validate shell syntax with `bash -n`, render Compose with placeholder values using `docker compose config`, and build the Docker image locally.
- [ ] Commit: `build: add reminder server deployment pipeline`.

### Task 10: Run end-to-end local acceptance and report deployment boundary

**Files:**
- Modify only if verification exposes a scoped defect.

- [ ] Run `mvn clean test package` and prove exactly one executable backend JAR is produced.
- [ ] Run focused backend tests for application context, direct reminder generation, MinIO upload, and standard WeChat login.
- [ ] Run `pnpm check:no-cloudbase`, `pnpm build:mp-weixin`, and `pnpm build:h5`.
- [ ] Run repository-wide `rg` for CloudBase runtime tokens and classify any remaining documentation/history references separately from executable code.
- [ ] Build and start the Docker/Compose service with test configuration where local middleware is available; otherwise prove image build, config rendering, and health-check wiring and state the unavailable external dependency.
- [ ] Inspect `git status`, ensure user-owned pre-existing changes are still present, and ensure each migration commit contains only intended files/hunks.
- [ ] Report three states separately: code completed, locally verified, and production/server cutover not yet performed.
