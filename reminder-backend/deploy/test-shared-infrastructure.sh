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

assert_no_password_literal() {
  local file="$1"
  local variable="$2"

  if grep -Eq "^[[:space:]]*${variable}([:]|=)[[:space:]]*(change-me|replace-me|changeme|password|secret|[[:alnum:]][[:alnum:]_-]{7,})[[:space:]]*$" "${file}"; then
    fail "${variable} must remain a runtime placeholder (${file})"
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

# Every tracked password template must remain secret-free and runtime-injected.
assert_no_password_literal "${ENV_TEMPLATE}" 'DB_PASSWORD'
assert_no_password_literal "${ENV_TEMPLATE}" 'REDIS_PASSWORD'
assert_no_password_literal "${COMPOSE_FILE}" 'DB_PASSWORD'
assert_no_password_literal "${COMPOSE_FILE}" 'REDIS_PASSWORD'

# Spring must apply the same schema to each Druid connection and to Hibernate/JPA.
assert_contains "${SPRING_CONFIG}" 'SET search_path TO ${DB_SCHEMA:public}' \
  'Druid initializes the PostgreSQL search path from DB_SCHEMA'
assert_contains "${SPRING_CONFIG}" 'default_schema: ${DB_SCHEMA:public}' \
  'Hibernate uses DB_SCHEMA as the default schema'

# The deployment manual must describe the shared database, schema, Redis DB, and secret handoff.
assert_regex "${DEPLOY_MANUAL}" 'saas-admin|saas-postgres' \
  'deployment manual names the shared PostgreSQL service'
assert_contains "${DEPLOY_MANUAL}" 'reminder' \
  'deployment manual documents the Reminder isolation boundary'
assert_contains "${DEPLOY_MANUAL}" 'DB 9' \
  'deployment manual documents Redis DB 9'
assert_regex "${DEPLOY_MANUAL}" 'Secret file|Secret file 类型|reminder-runtime-env' \
  'deployment manual documents runtime secret injection'

printf '%s\n' 'shared infrastructure contract passed'
