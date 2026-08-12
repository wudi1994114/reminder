#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly ISSUE_SCRIPT="${SCRIPT_DIR}/issue-reminder-certificate.sh"
readonly TEST_ROOT="$(mktemp -d)"
readonly FAKE_BIN="${TEST_ROOT}/bin"
readonly LETSENCRYPT_DIR="${TEST_ROOT}/letsencrypt"
readonly WEBROOT_DIR="${TEST_ROOT}/webroot"
readonly DOCKER_LOG="${TEST_ROOT}/docker.log"

cleanup() {
  rm -rf "${TEST_ROOT}"
}
trap cleanup EXIT

fail() {
  printf 'reminder certificate script test failed: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file="$1"
  local expected="$2"
  local description="$3"
  grep -Fq -- "$expected" "$file" || fail "$description"
}

[[ -f "${ISSUE_SCRIPT}" ]] || fail "missing ${ISSUE_SCRIPT}"
mkdir -p "${FAKE_BIN}" "${LETSENCRYPT_DIR}/live/reminder-api.wwmty.com" "${WEBROOT_DIR}"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -Eeuo pipefail' \
  'printf "%s\\n" "$*" >> "${REMINDER_TEST_DOCKER_LOG:?}"' \
  'printf x > "${REMINDER_TEST_CERT_DIR:?}/fullchain.pem"' \
  'printf x > "${REMINDER_TEST_CERT_DIR:?}/privkey.pem"' > "${FAKE_BIN}/docker"
chmod +x "${FAKE_BIN}/docker"

PATH="${FAKE_BIN}:${PATH}" \
REMINDER_TEST_DOCKER_LOG="${DOCKER_LOG}" \
REMINDER_TEST_CERT_DIR="${LETSENCRYPT_DIR}/live/reminder-api.wwmty.com" \
REMINDER_ACME_EMAIL='ops@example.test' \
REMINDER_LETSENCRYPT_DIR="${LETSENCRYPT_DIR}" \
REMINDER_CERTBOT_WEBROOT="${WEBROOT_DIR}" \
bash "${ISSUE_SCRIPT}" >/dev/null

assert_contains "${DOCKER_LOG}" 'certonly --webroot' 'certificate script uses the webroot challenge'
assert_contains "${DOCKER_LOG}" '-w /var/www/certbot' 'certificate script mounts the standard challenge webroot'
assert_contains "${DOCKER_LOG}" '-d reminder-api.wwmty.com' 'certificate script requests only the Reminder hostname'
assert_contains "${DOCKER_LOG}" '--keep-until-expiring' 'certificate script is safe to rerun'
[[ -s "${LETSENCRYPT_DIR}/live/reminder-api.wwmty.com/fullchain.pem" ]] || fail 'certificate script must verify fullchain output'
[[ -s "${LETSENCRYPT_DIR}/live/reminder-api.wwmty.com/privkey.pem" ]] || fail 'certificate script must verify private-key output'

printf '%s\n' 'reminder certificate script test passed'
