#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.reminder.yml"
readonly ENV_TEMPLATE="${SCRIPT_DIR}/.env.example"
readonly SPRING_CONFIG="${REPO_ROOT}/reminder-backend/src/main/resources/application.yaml"
readonly DEPLOY_MANUAL="${REPO_ROOT}/部署手册.md"

fail() {
  printf 'shared infrastructure contract failed: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file="$1"
  local expected="$2"
  local description="$3"

  grep -Fq -- "${expected}" "${file}" || fail "${description} (${file})"
}

assert_regex() {
  local file="$1"
  local pattern="$2"
  local description="$3"

  grep -Eq -- "${pattern}" "${file}" || fail "${description} (${file})"
}

assert_exact_line_count() {
  local file="$1"
  local pattern="$2"
  local expected_count="$3"
  local description="$4"
  local actual_count

  actual_count="$(grep -Ec -- "${pattern}" "${file}" || true)"
  if [[ "${actual_count}" -ne "${expected_count}" ]]; then
    fail "${description}: expected ${expected_count} complete matching line(s), found ${actual_count} (${file})"
  fi
}

[[ -f "${COMPOSE_FILE}" ]] || fail 'tracked Compose file is missing'
[[ -f "${ENV_TEMPLATE}" ]] || fail 'tracked environment template is missing'
[[ -f "${SPRING_CONFIG}" ]] || fail 'tracked Spring configuration is missing'
[[ -f "${DEPLOY_MANUAL}" ]] || fail 'deployment manual is missing'

# Shared PostgreSQL contract: fixed endpoint/database/user, runtime password, and schema boundary.
assert_contains "${ENV_TEMPLATE}" \
  'DB_URL=jdbc:postgresql://saas-postgres:5432/saas-admin?timezone=Asia/Shanghai' \
  'environment template uses the shared PostgreSQL URL'
assert_contains "${ENV_TEMPLATE}" 'DB_USERNAME=pguser' \
  'environment template uses the shared PostgreSQL user'
assert_contains "${ENV_TEMPLATE}" 'DB_SCHEMA=reminder' \
  'environment template declares the Reminder schema'
assert_contains "${COMPOSE_FILE}" 'DB_SCHEMA: ${DB_SCHEMA:-reminder}' \
  'Compose passes the Reminder schema'
assert_contains "${COMPOSE_FILE}" 'DB_URL: ${DB_URL:-jdbc:postgresql://saas-postgres:5432/saas-admin?timezone=Asia/Shanghai}' \
  'Compose defaults to the shared PostgreSQL URL'
assert_contains "${COMPOSE_FILE}" 'DB_USERNAME: ${DB_USERNAME:-pguser}' \
  'Compose defaults to the shared PostgreSQL user'
assert_regex "${COMPOSE_FILE}" 'DB_PASSWORD: \$\{DB_PASSWORD:\?[^}]+\}' \
  'PostgreSQL password is a required runtime substitution'

# Redis contract: shared endpoint, required runtime password, and logical DB isolation.
assert_contains "${ENV_TEMPLATE}" 'REDIS_HOST=saas-redis' \
  'environment template uses the shared Redis host'
assert_contains "${ENV_TEMPLATE}" 'REDIS_PORT=6379' \
  'environment template uses the shared Redis port'
assert_contains "${ENV_TEMPLATE}" 'REDIS_DATABASE=9' \
  'environment template isolates Reminder in Redis DB 9'
assert_contains "${COMPOSE_FILE}" 'REDIS_HOST: ${REDIS_HOST:-saas-redis}' \
  'Compose defaults to the shared Redis host'
assert_contains "${COMPOSE_FILE}" 'REDIS_PORT: ${REDIS_PORT:-6379}' \
  'Compose defaults to the shared Redis port'
assert_contains "${COMPOSE_FILE}" 'REDIS_DATABASE: ${REDIS_DATABASE:-9}' \
  'Compose uses Redis DB 9'
assert_regex "${COMPOSE_FILE}" 'REDIS_PASSWORD: \$\{REDIS_PASSWORD:\?[^}]+\}' \
  'Redis password is a required runtime substitution'

# Every consumed tracked config must use one complete, secret-free password contract line.
# The environment template accepts only explicit placeholder forms; comments and quoted
# values are rejected so a committed secret cannot satisfy this assertion accidentally.
assert_exact_line_count "${ENV_TEMPLATE}" \
  '^DB_PASSWORD=(change-me|replace-me|injected-from-jenkins-credential|<[^>]+>|\$\{[^}]+\})$' \
  1 \
  'environment template keeps PostgreSQL password as a runtime placeholder'
assert_exact_line_count "${ENV_TEMPLATE}" \
  '^REDIS_PASSWORD=(change-me|replace-me|injected-from-jenkins-credential|<[^>]+>|\$\{[^}]+\})$' \
  1 \
  'environment template keeps Redis password as a runtime placeholder'
assert_exact_line_count "${COMPOSE_FILE}" \
  '^[[:space:]]+DB_PASSWORD: \$\{DB_PASSWORD:\?[^}]+\}$' \
  1 \
  'Compose has exactly one required PostgreSQL password interpolation'
assert_exact_line_count "${COMPOSE_FILE}" \
  '^[[:space:]]+REDIS_PASSWORD: \$\{REDIS_PASSWORD:\?[^}]+\}$' \
  1 \
  'Compose has exactly one required Redis password interpolation'
assert_exact_line_count "${SPRING_CONFIG}" \
  '^[[:space:]]+password: \$\{DB_PASSWORD:\}$' \
  1 \
  'Spring has exactly one PostgreSQL password placeholder'
assert_exact_line_count "${SPRING_CONFIG}" \
  '^[[:space:]]+password: \$\{REDIS_PASSWORD:\}$' \
  1 \
  'Spring has exactly one Redis password placeholder'

# Spring must apply the same schema to each Druid connection and to Hibernate/JPA.
assert_contains "${SPRING_CONFIG}" 'SET search_path TO ${DB_SCHEMA:reminder}' \
  'Druid initializes the PostgreSQL search path from DB_SCHEMA'
assert_contains "${SPRING_CONFIG}" 'default_schema: ${DB_SCHEMA:reminder}' \
  'Hibernate uses DB_SCHEMA as the default schema'

# The deployment manual must describe the shared database, schema, Redis DB, and secret handoff.
assert_regex "${DEPLOY_MANUAL}" '[Pp]ostgreSQL.*(schema|模式).*reminder|reminder.*(schema|模式).*[Pp]ostgreSQL' \
  'deployment manual explicitly scopes PostgreSQL to the Reminder schema'
assert_regex "${DEPLOY_MANUAL}" '[Rr]edis.*DB 9|DB 9.*[Rr]edis' \
  'deployment manual explicitly scopes Redis to DB 9'
assert_regex "${DEPLOY_MANUAL}" 'Secret file|Secret file 类型|reminder-runtime-env' \
  'deployment manual documents runtime secret injection'

printf '%s\n' 'shared infrastructure contract passed'
