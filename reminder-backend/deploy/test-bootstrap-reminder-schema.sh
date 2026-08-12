#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly BOOTSTRAP_SCRIPT="${SCRIPT_DIR}/bootstrap-reminder-schema.sh"
readonly TEST_ROOT="$(mktemp -d)"
readonly FAKE_BIN="${TEST_ROOT}/bin"
readonly DOCKER_LOG="${TEST_ROOT}/docker.log"
readonly SCHEMA_SQL="${TEST_ROOT}/schema.sql"
readonly QUARTZ_SQL="${TEST_ROOT}/quartz.sql"

cleanup() {
  rm -rf "${TEST_ROOT}"
}
trap cleanup EXIT

fail() {
  printf 'reminder schema bootstrap test failed: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file="$1"
  local expected="$2"
  local description="$3"
  grep -Fq -- "$expected" "$file" || fail "$description"
}

[[ -f "${BOOTSTRAP_SCRIPT}" ]] || fail "missing ${BOOTSTRAP_SCRIPT}"
mkdir -p "${FAKE_BIN}"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -Eeuo pipefail' \
  'printf "%s\\n" "$*" >> "${REMINDER_TEST_DOCKER_LOG:?}"' \
  'if [[ "$*" == *"-Atqc"* ]]; then' \
  '  printf "%s\\n" "${REMINDER_TEST_TABLE_COUNT:?}"' \
  'fi' \
  'if [[ "$*" == *" -i "* ]]; then' \
  '  cat >/dev/null' \
  'fi' > "${FAKE_BIN}/docker"
chmod +x "${FAKE_BIN}/docker"

printf '%s\n' 'CREATE TABLE reminder_test ();' > "${SCHEMA_SQL}"
printf '%s\n' 'CREATE TABLE quartz_test ();' > "${QUARTZ_SQL}"

PATH="${FAKE_BIN}:${PATH}" \
REMINDER_TEST_DOCKER_LOG="${DOCKER_LOG}" \
REMINDER_TEST_TABLE_COUNT=0 \
REMINDER_SCHEMA_SQL="${SCHEMA_SQL}" \
REMINDER_QUARTZ_SQL="${QUARTZ_SQL}" \
bash "${BOOTSTRAP_SCRIPT}" >/dev/null

assert_contains "${DOCKER_LOG}" 'exec -i saas-postgres psql -v ON_ERROR_STOP=1 -U pguser -d saas-admin' \
  'bootstrap initializes an empty schema with psql fail-fast enabled'
[[ "$(grep -Fxc 'exec -i saas-postgres psql -v ON_ERROR_STOP=1 -U pguser -d saas-admin' "${DOCKER_LOG}")" == '2' ]] \
  || fail 'bootstrap executes both business and Quartz schema files exactly once'

: > "${DOCKER_LOG}"
PATH="${FAKE_BIN}:${PATH}" \
REMINDER_TEST_DOCKER_LOG="${DOCKER_LOG}" \
REMINDER_TEST_TABLE_COUNT=3 \
REMINDER_SCHEMA_SQL="${SCHEMA_SQL}" \
REMINDER_QUARTZ_SQL="${QUARTZ_SQL}" \
bash "${BOOTSTRAP_SCRIPT}" >/dev/null

if grep -Fq 'exec -i saas-postgres psql' "${DOCKER_LOG}"; then
  fail 'bootstrap must not overwrite a non-empty Reminder schema'
fi

printf '%s\n' 'reminder schema bootstrap test passed'
