#!/usr/bin/env bash

set -Eeuo pipefail

readonly TEST_ROOT="$(mktemp -d)"
readonly FAKE_BIN="${TEST_ROOT}/bin"
readonly ENV_FILE="${TEST_ROOT}/runtime.env"
readonly STATE_DIR="${TEST_ROOT}/deploy-state"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly EXPECTED_IMAGE='127.0.0.1:3000/admin/reminder-backend:contract-test'

cleanup() {
  rm -rf "${TEST_ROOT}"
}
trap cleanup EXIT

fail() {
  printf 'production rollout contract failed: %s\n' "$1" >&2
  exit 1
}

mkdir -p "${FAKE_BIN}"
printf 'DB_PASSWORD=test\nREDIS_PASSWORD=test\nJWT_SECRET=test\nWECHAT_APP_ID=test\nWECHAT_APP_SECRET=test\nSAAS_STORAGE_APP_ID=test\nSAAS_STORAGE_SECRET_CODE=test\n' > "${ENV_FILE}"

cat > "${FAKE_BIN}/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "$1" == "inspect" ]]; then
  if [[ "${3:-}" == '{{.Config.Image}}' ]]; then
    exit 1
  fi
  printf '%s\n' 'healthy'
  exit 0
fi

if [[ "$1" == "exec" || "$1" == "logs" ]]; then
  exit 0
fi

if [[ "$1" != "compose" ]]; then
  printf 'unexpected docker command: %s\n' "$*" >&2
  exit 2
fi

case " $* " in
  *' config --services '*) printf '%s\n' 'reminder-backend' ;;
  *' config --quiet '*) ;;
  *' pull '*) ;;
  *' up '*) ;;
  *) printf 'unexpected compose command: %s\n' "$*" >&2; exit 2 ;;
esac
FAKE_DOCKER
chmod 700 "${FAKE_BIN}/docker"

export PATH="${FAKE_BIN}:${PATH}"
export REMINDER_ENV_FILE="${ENV_FILE}"
export REMINDER_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.reminder.yml"
export REMINDER_STATE_DIR="${STATE_DIR}"
unset REMINDER_IMAGE_PREFIX

export REMINDER_IMAGE='unexpected.registry/reminder-backend:contract-test'
if bash "${SCRIPT_DIR}/deploy-reminder.sh" >/dev/null 2>&1; then
  fail 'default image-prefix guard accepted an unexpected image'
fi

export REMINDER_IMAGE="${EXPECTED_IMAGE}"
if ! bash "${SCRIPT_DIR}/deploy-reminder.sh" >/dev/null 2>&1; then
  fail 'default image-prefix guard rejected the live SaaS registry image'
fi

current_link="${STATE_DIR}/current"
[[ -L "${current_link}" ]] || fail 'successful release did not record audited state'
current_release="$(cd "${STATE_DIR}/$(readlink "${current_link}")" && pwd -P)"
grep -qxF "${EXPECTED_IMAGE}" "${current_release}/image" || fail 'audited state stored the wrong image'

printf '%s\n' 'production rollout contract passed'
