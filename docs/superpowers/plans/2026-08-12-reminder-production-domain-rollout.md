# Reminder Production Domain Rollout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the migrated Reminder backend on the existing SaaS host behind `https://reminder-api.wwmty.com`, so a production mini-program build can call it directly.

**Architecture:** Reminder remains a standalone Docker Compose project. Its Jenkins job builds an immutable image in the existing SaaS registry, deploys only `reminder-backend` into the existing `saas-app` and `saas-middleware` networks, and records a rollback state under the Jenkins-visible persistent SaaS volume. Nginx owns the new hostname and proxies to the Reminder container over `saas-app`; the mini-program uses this same HTTPS API origin from its active request helper.

**Tech Stack:** Spring Boot 3, Docker Compose, Jenkins Pipeline, Nginx, Let's Encrypt, Tencent Cloud DNSPod, PostgreSQL schema `reminder`, Redis DB `9`, uni-app/Vite.

## Global Constraints

- Use `reminder-api.wwmty.com` only; do not reuse the old CloudBase endpoint or `api.reminder.com`.
- Keep Reminder separate from the saas-admin Compose project and roll back only Reminder.
- Use the SaaS host registry namespace `127.0.0.1:3000/admin/reminder-backend`.
- Use the existing `saas-app` and `saas-middleware` networks, PostgreSQL database `saas-admin` with schema `reminder`, and Redis logical database `9`.
- Keep every password, JWT secret, WeChat secret, storage AppID, and storage secret in Jenkins credentials; never commit or print them.
- Create schema SQL only when the `reminder` schema has no Reminder tables; never use the bootstrap SQL as an upgrade script.
- Do not remove or change existing saas-admin, middleware, CloudBase, or user-uncommitted mini-program files during this rollout.

---

### Task 1: Align the independently deployable Reminder release contract with the live SaaS host

**Files:**
- Modify: `reminder-backend/Jenkinsfile`
- Modify: `reminder-backend/deploy/deploy-reminder.sh`
- Modify: `reminder-backend/deploy/.env.example`
- Modify: `reminder-backend/deploy/test-deploy-reminder.sh`
- Create: `reminder-backend/deploy/test-production-rollout-contract.sh`
- Modify: `部署手册.md`

**Interfaces:**
- Consumes: Jenkins workspace, Docker socket, host-persistent `/opt/saas-app/reminder/deploy-state`, `reminder-runtime-env`, and `reminder-saas-storage-app` credentials.
- Produces: `127.0.0.1:3000/admin/reminder-backend:<version>` and a rollback snapshot whose image-prefix validation accepts only that repository.

- [ ] **Step 1: Write the failing release-contract test**

Create a shell test that asserts these exact production values before changing runtime files:

```bash
assert_contains reminder-backend/Jenkinsfile "NEXUS_REGISTRY_URL   = '127.0.0.1:3000'"
assert_contains reminder-backend/Jenkinsfile "DOCKER_IMAGE_NAME    = 'admin/reminder-backend'"
assert_contains reminder-backend/Jenkinsfile "REMINDER_STATE_DIR   = '/opt/saas-app/reminder/deploy-state'"
assert_contains reminder-backend/deploy/deploy-reminder.sh \
  'EXPECTED_IMAGE_PREFIX="${REMINDER_IMAGE_PREFIX:-127.0.0.1:3000/admin/reminder-backend}:"'
assert_contains reminder-backend/deploy/.env.example \
  'REMINDER_IMAGE=127.0.0.1:3000/admin/reminder-backend:1.0.0'
```

- [ ] **Step 2: Run the contract test and verify it fails**

Run: `bash reminder-backend/deploy/test-production-rollout-contract.sh`

Expected: non-zero exit because the source still targets `172.17.0.3:5001/reminder-backend` and `/opt/reminder/deploy-state`.

- [ ] **Step 3: Make the minimal release-contract changes**

Change the image registry and image name in the Jenkins Pipeline, use the SaaS-mounted persistent state directory, and adjust the default image-prefix guard, example runtime file, and rollback fixture to the same fully qualified prefix. Keep `docker compose --project-name reminder`, the `saas-app` and `saas-middleware` network attachments, secret-file injection, and per-service rollback unchanged.

- [ ] **Step 4: Run the deployment regression suite**

Run:

```bash
bash reminder-backend/deploy/test-production-rollout-contract.sh
bash reminder-backend/deploy/test-deploy-reminder.sh
bash reminder-backend/deploy/test-shared-infrastructure.sh
```

Expected: all three succeed; the rollback test still rejects an unexpected image and records a replacement release state only after health succeeds.

- [ ] **Step 5: Update the operational manual and commit**

Document the host-persistent state location and active local registry without secrets. Commit only this task's tracked release and test files:

```bash
git add reminder-backend/Jenkinsfile reminder-backend/deploy/deploy-reminder.sh \
  reminder-backend/deploy/.env.example reminder-backend/deploy/test-deploy-reminder.sh \
  reminder-backend/deploy/test-production-rollout-contract.sh 部署手册.md
git commit -m "build: target reminder release at saas host"
```

### Task 2: Make every production mini-program request helper use the approved HTTPS origin

**Files:**
- Modify: `reminder-uni-app/src/config/env.js`
- Create: `reminder-uni-app/scripts/assert-production-api-origin.mjs`

**Interfaces:**
- Consumes: the production origin in `src/config/api.js`, `VITE_API_BASE_URL`, and the legacy `src/api/request.js` helper.
- Produces: identical `https://reminder-api.wwmty.com/api` defaults for the Vite-aware and legacy request paths.

- [ ] **Step 1: Write the failing production-origin assertion**

Create a Node script that reads both configuration files and fails unless each contains the exact production origin and neither contains `api.reminder.com`:

```js
import { readFileSync } from 'node:fs';
const expected = 'https://reminder-api.wwmty.com/api';
for (const file of ['src/config/api.js', 'src/config/env.js']) {
  const source = readFileSync(new URL(`../${file}`, import.meta.url), 'utf8');
  if (!source.includes(expected) || source.includes('api.reminder.com')) process.exit(1);
}
```

- [ ] **Step 2: Run the assertion and verify it fails**

Run: `node reminder-uni-app/scripts/assert-production-api-origin.mjs`

Expected: non-zero exit because the helper used by `src/api/request.js` still selects `https://api.reminder.com/api` in production.

- [ ] **Step 3: Fix the legacy production default only**

Replace the production API URL in `src/config/env.js` with `https://reminder-api.wwmty.com/api`. Preserve the development and test URLs and leave the user-uncommitted package and page changes untouched.

- [ ] **Step 4: Verify source and production build**

Run:

```bash
node reminder-uni-app/scripts/assert-production-api-origin.mjs
npm --prefix reminder-uni-app run check:no-cloudbase
npm --prefix reminder-uni-app run build:mp-weixin
```

Expected: API assertion and CloudBase check succeed; the production bundle completes without embedding `api.reminder.com`.

- [ ] **Step 5: Commit only the API-origin task**

```bash
git add reminder-uni-app/src/config/env.js \
  reminder-uni-app/scripts/assert-production-api-origin.mjs
git commit -m "fix: align mini program production api origin"
```

### Task 3: Create the audited Jenkins release job and initialize only the isolated server state

**Files:**
- No repository file change required after Task 1.
- Jenkins credentials: `reminder-runtime-env`, `reminder-saas-storage-app`, and the existing SaaS registry credential.
- Server state: `/opt/saas-app/reminder/deploy-state`.

**Interfaces:**
- Consumes: the committed branch, Jenkins job configuration, Runtime Secret file, and Docker containers `saas-postgres`, `saas-redis`, and `saas-admin-backend`.
- Produces: an independent `reminder-backend` Jenkins job, a 700-permission rollback directory, and populated `reminder` PostgreSQL/Quartz schemas.

- [ ] **Step 1: Verify Jenkins credentials by identifier only**

In Jenkins Credentials, confirm the existing storage credential and registry credential are present. Create or update `reminder-runtime-env` as a Secret file containing the non-secret keys plus the separately supplied PostgreSQL/Redis passwords, a fresh one-line Base64 JWT secret, the correct WeChat AppID/Secret, and `EMAIL_PROVIDER=none`. Do not copy the storage app identity into this file because the Pipeline appends it from the audited separate credential.

- [ ] **Step 2: Prepare the persistent Reminder state directory**

On the SaaS host, create the Jenkins-visible state location and restrict it before the first deployment:

```bash
install -d -m 700 /opt/saas-app/reminder/deploy-state
test ! -L /opt/saas-app/reminder/deploy-state
```

- [ ] **Step 3: Verify schema is safe to initialize**

Run this read-only query from the SaaS host and require an empty result before bootstrap:

```bash
docker exec saas-postgres psql -U pguser -d saas-admin -Atc \
  "SELECT tablename FROM pg_tables WHERE schemaname = 'reminder' ORDER BY tablename;"
```

Expected: no table name. If any name is returned, stop bootstrap and retain the existing data for a migration-specific review.

- [ ] **Step 4: Initialize business and Quartz tables exactly once**

Run only after Step 3 reports no tables:

```bash
docker exec -i saas-postgres psql -v ON_ERROR_STOP=1 -U pguser -d saas-admin \
  < reminder-backend/src/main/resources/schema.sql
docker exec -i saas-postgres psql -v ON_ERROR_STOP=1 -U pguser -d saas-admin \
  < reminder-backend/src/main/resources/quartz.sql
```

Expected: both commands succeed, tables exist only in schema `reminder`, and no other schema is changed.

- [ ] **Step 5: Create the Jenkins Pipeline from SCM**

Create a Pipeline named `reminder-backend`. Use the Reminder Git repository, the committed migration branch, and script path `reminder-backend/Jenkinsfile`. Leave concurrent builds disabled. Do not use a freestyle shell job or attach the service to the saas-admin job.

### Task 4: Provision the public DNS, TLS, and isolated Nginx virtual host

**Files:**
- Server Nginx managed block in `/opt/saas-app/nginx/gateway.conf`
- Certificate paths in the mounted gateway namespace: `/etc/letsencrypt/live/reminder-api.wwmty.com/fullchain.pem` and `privkey.pem`
- Source references: `reminder-backend/deploy/nginx/reminder-api-http.conf`, `reminder-api-https.conf`, and `install-reminder-gateway.sh`

**Interfaces:**
- Consumes: the existing Tencent Cloud DNS zone `wwmty.com`, SaaS host public address, `saas-gateway`, and the `saas-app` Docker network.
- Produces: a valid public HTTPS origin for only `reminder-api.wwmty.com`.

- [ ] **Step 1: Add the DNSPod record**

Create the `A` record `reminder-api.wwmty.com` to the same active SaaS host used by the existing `api`, `admin`, and `jenkins` records. Set the TTL to `600`; do not change any existing records.

- [ ] **Step 2: Wait for authoritative DNS**

Run:

```bash
dig +short reminder-api.wwmty.com A
```

Expected: exactly the SaaS host public address before requesting a certificate.

- [ ] **Step 3: Install the temporary HTTP challenge managed block and issue TLS**

Before enabling the HTTPS proxy, install the HTTP-only managed block through the guarded gateway installer, then issue the certificate through the existing containerized Certbot volume:

```bash
export REMINDER_GATEWAY_SOURCE="$PWD/reminder-backend/deploy/nginx/reminder-api-http.conf"
bash reminder-backend/deploy/nginx/install-reminder-gateway.sh
bash reminder-backend/deploy/nginx/issue-reminder-certificate.sh
```

Expected: the installer creates a recoverable backup, validates both the candidate and live `saas-gateway` configuration, and certificate files exist at the documented mounted paths.

- [ ] **Step 4: Replace the temporary managed block with the full TLS proxy and verify syntax**

```bash
export REMINDER_GATEWAY_SOURCE="$PWD/reminder-backend/deploy/nginx/reminder-api-https.conf"
bash reminder-backend/deploy/nginx/install-reminder-gateway.sh
curl -fsS https://reminder-api.wwmty.com/actuator/health
```

Expected: gateway configuration reloads successfully. The proxy resolves `reminder-backend:8080` on `saas-app`; health becomes green after Task 5 deploys the backend.

### Task 5: Deploy, inspect the live service, and make the WeChat allowlist release-ready

**Files:**
- Jenkins build record for `reminder-backend`.
- WeChat Mini Program Settings → Development Settings → Server Domains.

**Interfaces:**
- Consumes: Tasks 1-4, the Jenkins Secret file, storage credential, DNS/TLS, and mini-program AppID/Secret.
- Produces: a healthy API and server-domain configuration that allows production request/upload/download operations.

- [ ] **Step 1: Run the new Jenkins release**

Build the committed migration branch with an immutable version such as the short commit SHA. Require the test, package, image push, `docker compose pull`, health wait, and audited state promotion stages to all pass.

- [ ] **Step 2: Inspect service isolation after deploy**

Run:

```bash
docker inspect --format '{{.State.Health.Status}}' reminder-backend
docker inspect --format '{{range $k, $_ := .NetworkSettings.Networks}}{{println $k}}{{end}}' reminder-backend
curl -fsS https://reminder-api.wwmty.com/actuator/health
```

Expected: `healthy`, `saas-app`, `saas-middleware`, and a successful public health response.

- [ ] **Step 3: Configure WeChat legal domains**

In the Mini Program console, save `https://reminder-api.wwmty.com` for both request and uploadFile. Save the actual HTTPS public MinIO origin configured by saas-admin for downloadFile. Do not add an IP address, HTTP origin, path, or CloudBase hostname.

- [ ] **Step 4: Verify the compiled mini-program requests the new origin**

Build/upload with production mode, then inspect the generated artifact or DevTools network panel. Require all `/api/...` calls to use `https://reminder-api.wwmty.com/api` and require the client to report no legal-domain error.

### Task 6: Execute the user-facing acceptance and preserve rollback evidence

**Files:**
- Jenkins successful build record.
- `/opt/saas-app/reminder/deploy-state/current` on the SaaS host.
- saas-admin storage audit record.

**Interfaces:**
- Consumes: a real mini-program `wx.login` code, public HTTPS API, authenticated JWT, and selected avatar image.
- Produces: evidence that login, authenticated data access, upload, and object audit all work after a production mini-program upload.

- [ ] **Step 1: Perform real mini-program login**

From the production-configured mini-program, execute `wx.login`, call `POST /api/auth/wechat/login`, and retain the issued JWT in the normal app storage. Confirm the response is successful over `https://reminder-api.wwmty.com`.

- [ ] **Step 2: Verify an authenticated business request**

Use the issued JWT for one existing reminder read/create request. Confirm a normal API response and that no CloudBase client call occurs.

- [ ] **Step 3: Verify avatar upload and returned HTTPS URL**

Upload a small JPEG or PNG through the normal mini-program profile flow. Confirm the returned URL is HTTPS and not an expiring presigned URL.

- [ ] **Step 4: Verify the storage audit boundary**

In saas-admin, verify the audited storage record/object key begins with `app/reminder/`. Confirm Reminder has no MinIO access key in its runtime config.

- [ ] **Step 5: Verify rollback evidence and record outcome**

Run:

```bash
readlink /opt/saas-app/reminder/deploy-state/current
docker logs --tail 120 reminder-backend
```

Expected: `current` points into `releases/`, the release log has no secret values, and no saas-admin or middleware container was restarted.
