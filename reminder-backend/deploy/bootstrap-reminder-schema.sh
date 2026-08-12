#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCHEMA_SQL="${REMINDER_SCHEMA_SQL:-${SCRIPT_DIR}/../src/main/resources/schema.sql}"
readonly QUARTZ_SQL="${REMINDER_QUARTZ_SQL:-${SCRIPT_DIR}/../src/main/resources/quartz.sql}"
readonly POSTGRES_CONTAINER="${REMINDER_POSTGRES_CONTAINER:-saas-postgres}"
readonly POSTGRES_DATABASE="${REMINDER_POSTGRES_DATABASE:-saas-admin}"
readonly POSTGRES_USER="${REMINDER_POSTGRES_USER:-pguser}"

fail() {
  printf 'Reminder schema bootstrap failed: %s\n' "$1" >&2
  exit 1
}

[[ -f "${SCHEMA_SQL}" ]] || fail "business schema SQL does not exist: ${SCHEMA_SQL}"
[[ -f "${QUARTZ_SQL}" ]] || fail "Quartz schema SQL does not exist: ${QUARTZ_SQL}"

table_count="$(docker exec "${POSTGRES_CONTAINER}" psql \
  -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DATABASE}" -Atqc \
  "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'reminder' AND table_type = 'BASE TABLE';")"

[[ "${table_count}" =~ ^[0-9]+$ ]] || fail "could not determine Reminder table count: ${table_count}"

if [[ "${table_count}" -gt 0 ]]; then
  printf '%s\n' 'Reminder schema already contains tables; bootstrap SQL was not executed'
  exit 0
fi

docker exec -i "${POSTGRES_CONTAINER}" psql \
  -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DATABASE}" < "${SCHEMA_SQL}"
docker exec -i "${POSTGRES_CONTAINER}" psql \
  -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DATABASE}" < "${QUARTZ_SQL}"

printf '%s\n' 'Reminder business and Quartz schemas initialized'
