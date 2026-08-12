#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly INSTALLER="${SCRIPT_DIR}/nginx/install-reminder-gateway.sh"
readonly HTTP_CONFIG="${SCRIPT_DIR}/nginx/reminder-api-http.conf"
readonly HTTPS_CONFIG="${SCRIPT_DIR}/nginx/reminder-api-https.conf"
readonly TEST_ROOT="$(mktemp -d)"
readonly FAKE_BIN="${TEST_ROOT}/bin"
readonly GATEWAY_CONFIG="${TEST_ROOT}/gateway.conf"
readonly LIVE_BOUND_CONFIG="${TEST_ROOT}/gateway-bound.conf"
readonly BACKUP_DIR="${TEST_ROOT}/backups"
readonly DOCKER_LOG="${TEST_ROOT}/docker.log"

cleanup() {
  rm -rf "${TEST_ROOT}"
}
trap cleanup EXIT

fail() {
  printf 'reminder gateway installer test failed: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file="$1"
  local expected="$2"
  local description="$3"
  grep -Fq -- "$expected" "$file" || fail "$description"
}

mkdir -p "${FAKE_BIN}"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -Eeuo pipefail' \
  'printf "%s\\n" "$*" >> "${REMINDER_TEST_DOCKER_LOG:?}"' \
  'if [[ "$1" == "inspect" ]]; then' \
  '  printf "%s\\n" dev-platform saas-app' \
  'fi' \
  'exit 0' > "${FAKE_BIN}/docker"
chmod +x "${FAKE_BIN}/docker"

printf '%s\n' \
  'server {' \
  '    listen 80 default_server;' \
  '    return 444;' \
  '}' \
  '# BEGIN REMINDER API' \
  'obsolete reminder block' \
  '# END REMINDER API' > "${GATEWAY_CONFIG}"
ln "${GATEWAY_CONFIG}" "${LIVE_BOUND_CONFIG}"

PATH="${FAKE_BIN}:${PATH}" \
REMINDER_TEST_DOCKER_LOG="${DOCKER_LOG}" \
REMINDER_GATEWAY_CONFIG="${GATEWAY_CONFIG}" \
REMINDER_GATEWAY_SOURCE="${HTTPS_CONFIG}" \
REMINDER_GATEWAY_BACKUP_DIR="${BACKUP_DIR}" \
REMINDER_GATEWAY_CONTAINER='saas-gateway-test' \
bash "${INSTALLER}" >/dev/null

assert_contains "${GATEWAY_CONFIG}" 'listen 80 default_server;' 'installer preserves unrelated gateway configuration'
assert_contains "${GATEWAY_CONFIG}" 'server_name reminder-api.wwmty.com;' 'installer appends the new Reminder managed block'
assert_contains "${GATEWAY_CONFIG}" 'proxy_pass http://$reminder_upstream;' 'installer writes the container-network proxy target'
assert_contains "${LIVE_BOUND_CONFIG}" 'server_name reminder-api.wwmty.com;' 'installer updates the inode already bound by the live gateway container'
[[ "$(grep -Fxc '# BEGIN REMINDER API' "${GATEWAY_CONFIG}")" == '1' ]] \
  || fail 'installer leaves exactly one managed block start marker'
[[ "$(grep -Fxc '# END REMINDER API' "${GATEWAY_CONFIG}")" == '1' ]] \
  || fail 'installer leaves exactly one managed block end marker'

backup_file="$(find "${BACKUP_DIR}" -type f -name 'gateway.conf.*.before-reminder' -print -quit)"
[[ -n "${backup_file}" ]] || fail 'installer creates a recoverable gateway backup'
assert_contains "${backup_file}" 'obsolete reminder block' 'gateway backup retains the prior managed block'

assert_contains "${DOCKER_LOG}" 'create --name reminder-gateway-config-check-' 'installer creates an isolated candidate Nginx container'
assert_contains "${DOCKER_LOG}" '/opt/saas-app/certbot/letsencrypt:/etc/letsencrypt:ro' 'installer mounts the live certificate directory on the candidate'
assert_contains "${DOCKER_LOG}" '/opt/saas-app/certbot/www:/var/www/certbot:ro' 'installer mounts the live ACME webroot on the candidate'
assert_contains "${DOCKER_LOG}" 'network connect saas-app reminder-gateway-config-check-' 'installer mirrors every live gateway network on the candidate'
assert_contains "${DOCKER_LOG}" 'start -a reminder-gateway-config-check-' 'installer validates the candidate after network attachment'
assert_contains "${DOCKER_LOG}" 'exec saas-gateway-test nginx -t' 'installer validates the live gateway configuration'
assert_contains "${DOCKER_LOG}" 'exec saas-gateway-test nginx -s reload' 'installer reloads the live gateway only after validation'

printf '%s\n' 'reminder gateway installer test passed'
