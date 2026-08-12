# Reminder Shared SaaS Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Configure Reminder to use saas-admin's PostgreSQL database and Redis service while isolating Reminder data in PostgreSQL schema `reminder` and Redis DB 9.

**Architecture:** Docker Compose supplies the shared service endpoints and requires both passwords at runtime. Spring config applies the dedicated PostgreSQL schema to Druid connections and Hibernate, while Redis remains isolated by logical database number. Jenkins continues to receive secrets through its existing runtime Secret file.

**Tech Stack:** Spring Boot 2.7, PostgreSQL 15 JDBC, Druid, Hibernate/JPA, Redis, Docker Compose, Jenkins

## Global Constraints

- PostgreSQL URL: `jdbc:postgresql://saas-postgres:5432/saas-admin?timezone=Asia/Shanghai`.
- PostgreSQL user: `pguser`; schema: `reminder`.
- Redis endpoint: `saas-redis:6379`; logical database: `9`.
- PostgreSQL and Redis passwords are runtime secrets and must never be committed.
- Existing uni-app working-tree changes are out of scope.

---

### Task 1: Add the deployment contract regression test

**Files:**
- Create: `reminder-backend/deploy/test-shared-infrastructure.sh`

**Interfaces:**
- Consumes: tracked Compose, environment template, Spring configuration, and deployment manual.
- Produces: a shell test that exits non-zero if Reminder stops targeting the shared services or loses schema/cache isolation.

- [ ] **Step 1: Write assertions for the required database and Redis contract**

The test must assert the exact non-secret connection values, required password interpolation, `DB_SCHEMA=reminder`, JPA/Druid schema configuration, Redis DB 9, and absence of password literals from tracked deployment files.

- [ ] **Step 2: Run the test before implementation**

Run: `bash reminder-backend/deploy/test-shared-infrastructure.sh`

Expected: FAIL because the environment template still targets database/user `reminder`, the Spring configuration has no schema selection, and the Redis password is optional.

### Task 2: Configure the shared services and isolation boundaries

**Files:**
- Modify: `reminder-backend/deploy/docker-compose.reminder.yml`
- Modify: `reminder-backend/deploy/.env.example`
- Modify: `reminder-backend/src/main/resources/application.yaml`
- Modify: `reminder-backend/src/main/resources/schema.sql`
- Modify: `reminder-backend/src/main/resources/quartz.sql`
- Modify: `部署手册.md`
- Modify: `reminder-backend/Jenkinsfile`
- Create, ignored: `reminder-backend/deploy/.env.shared-infrastructure`

**Interfaces:**
- Consumes: the existing `saas-app` external network and Jenkins `reminder-runtime-env` Secret file.
- Produces: runtime environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SCHEMA`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, and `REDIS_DATABASE`.

- [ ] **Step 1: Set Compose defaults and password gates**

Use the required PostgreSQL URL/user and Redis endpoint/DB. Keep both passwords as `${...:?...}` required runtime substitutions and pass `DB_SCHEMA=reminder` into the container.

- [ ] **Step 2: Apply the PostgreSQL schema in Spring**

For non-test profiles, initialize each Druid connection with `SET search_path TO ${DB_SCHEMA:reminder}` and set Hibernate `default_schema` to `${DB_SCHEMA:reminder}`.

- [ ] **Step 3: Update the environment template and deployment manual**

Document the shared database, dedicated schema initialization, Redis DB 9, and Jenkins secret injection. Make both business and Quartz initialization scripts select `reminder`; refuse to overwrite existing business or Quartz tables. Do not include either real password.

- [ ] **Step 4: Add the deployment test to Jenkins**

Run the shared-infrastructure test alongside the existing monolith and rollback deployment checks.

- [ ] **Step 5: Store the supplied secrets only in an ignored local handoff file**

Create `.env.shared-infrastructure` with mode `600`, verify `git check-ignore` succeeds, and never stage it.

### Task 3: Verify and commit

**Files:**
- Test: `reminder-backend/deploy/test-shared-infrastructure.sh`
- Test: all backend tests and deploy scripts

**Interfaces:**
- Consumes: the implementation from Task 2.
- Produces: a tested deployable JAR and a single scoped Git commit.

- [ ] **Step 1: Run focused deployment tests**

Run: `bash reminder-backend/deploy/test-shared-infrastructure.sh && bash reminder-backend/deploy/test-deploy-reminder.sh`

Expected: both scripts print success messages and exit 0.

- [ ] **Step 2: Run backend tests and package**

Run: `cd reminder-backend && mvn -q clean test package`

Expected: exit 0 with no failed or errored tests and a Spring Boot JAR under `target/`.

- [ ] **Step 3: Render the Compose model**

Run: `REMINDER_IMAGE=172.17.0.3:5001/reminder-backend:test docker compose --project-name reminder --env-file reminder-backend/deploy/.env.example -f reminder-backend/deploy/docker-compose.reminder.yml config --quiet`

Expected: exit 0.

- [ ] **Step 4: Check secret and scope safety**

Verify the two supplied passwords do not occur in `git grep`, `git diff --check` passes, and staged paths contain no uni-app files or ignored secret files.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-08-12-share-saas-infrastructure-design.md \
  docs/superpowers/plans/2026-08-12-share-saas-infrastructure.md \
  reminder-backend/deploy/test-shared-infrastructure.sh \
  reminder-backend/deploy/docker-compose.reminder.yml \
  reminder-backend/deploy/.env.example \
  reminder-backend/src/main/resources/application.yaml \
  reminder-backend/src/main/resources/schema.sql \
  reminder-backend/src/main/resources/quartz.sql \
  reminder-backend/Jenkinsfile 部署手册.md
git commit -m "build: share saas database and redis"
```
