#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.reminder.yml"

fail() {
  printf 'runtime network topology failed: %s\n' "$1" >&2
  exit 1
}

rendered="$(env \
  REMINDER_IMAGE=127.0.0.1:3000/admin/reminder-backend:network-test \
  DB_PASSWORD=test REDIS_PASSWORD=test JWT_SECRET=test \
  WECHAT_APP_ID=test WECHAT_APP_SECRET=test \
  SAAS_STORAGE_APP_ID=test SAAS_STORAGE_SECRET_CODE=test \
  docker compose --project-name reminder-network-test -f "${COMPOSE_FILE}" config)"

assert_line() {
  local expected="$1"
  printf '%s\n' "${rendered}" | grep -Fxq -- "${expected}" || fail "missing rendered line ${expected}"
}

assert_line '      saas-app: null'
assert_line '      saas-middleware: null'
assert_line '  saas-app:'
assert_line '    name: saas-app'
assert_line '  saas-middleware:'
assert_line '    name: saas-middleware'

external_count="$(printf '%s\n' "${rendered}" | grep -Fxc '    external: true' || true)"
[[ "${external_count}" -eq 2 ]] || fail "expected two external network declarations, got ${external_count}"

printf '%s\n' 'runtime network topology passed'
