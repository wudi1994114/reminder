#!/usr/bin/env bash

set -Eeuo pipefail

readonly TEST_ROOT="$(mktemp -d)"
readonly FAKE_BIN="${TEST_ROOT}/bin"
readonly FAKE_LOG="${TEST_ROOT}/docker.log"
readonly FAKE_PHASE="${TEST_ROOT}/docker.phase"
readonly DEPLOY_OUTPUT="${TEST_ROOT}/deploy.output"
readonly DEPLOY_STATE="${TEST_ROOT}/deploy-state"
readonly OLD_RELEASE="${DEPLOY_STATE}/releases/old"
readonly NEW_ENV="${TEST_ROOT}/new.env"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cleanup() {
  rm -rf "${TEST_ROOT}"
}
trap cleanup EXIT

mkdir -p "${FAKE_BIN}" "${OLD_RELEASE}"
printf 'DB_PASSWORD=old-database-password\n' > "${OLD_RELEASE}/runtime.env"
cp "${SCRIPT_DIR}/docker-compose.reminder.yml" "${OLD_RELEASE}/docker-compose.reminder.yml"
printf '%s\n' '127.0.0.1:3000/admin/reminder-backend:old' > "${OLD_RELEASE}/image"
ln -s 'releases/old' "${DEPLOY_STATE}/current"
printf 'DB_PASSWORD=new-invalid-password\n' > "${NEW_ENV}"
canonical_old_release="$(cd "${OLD_RELEASE}" && pwd -P)"

cat > "${FAKE_BIN}/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "$1" == "inspect" ]]; then
  if [[ "$3" == *'.Config.Image'* ]]; then
    printf '%s\n' '127.0.0.1:3000/admin/reminder-backend:old'
    exit 0
  fi

  phase="$(cat "${FAKE_DOCKER_PHASE}" 2>/dev/null || true)"
  if [[ "${phase}" == "new" ]]; then
    printf '%s\n' "${FAKE_DOCKER_NEW_HEALTH:-unhealthy}"
  elif [[ "${phase}" == "rollback" ]]; then
    printf '%s\n' 'healthy'
  fi
  exit 0
fi

if [[ "$1" == "exec" || "$1" == "logs" ]]; then
  exit 0
fi

if [[ "$1" != "compose" ]]; then
  printf 'Unexpected docker command: %s\n' "$*" >&2
  exit 2
fi

env_file=''
compose_file=''
shift
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project-name)
      shift 2
      ;;
    --env-file)
      env_file="$2"
      shift 2
      ;;
    -f)
      compose_file="$2"
      shift 2
      ;;
    config)
      if [[ "${2:-}" == "--services" ]]; then
        printf '%s\n' 'reminder-backend'
      fi
      exit 0
      ;;
    pull)
      exit 0
      ;;
    up)
      phase="$(cat "${FAKE_DOCKER_PHASE}" 2>/dev/null || true)"
      if [[ "${phase}" == "new" ]]; then
        next_phase='rollback'
      else
        next_phase='new'
      fi
      printf '%s\n' "${next_phase}" > "${FAKE_DOCKER_PHASE}"
      printf 'phase=%s env=%s compose=%s image=%s\n' \
        "${next_phase}" "${env_file}" "${compose_file}" "${REMINDER_IMAGE:-}" >> "${FAKE_DOCKER_LOG}"
      exit 0
      ;;
    rm)
      exit 0
      ;;
    *)
      shift
      ;;
  esac
done
FAKE_DOCKER
chmod 700 "${FAKE_BIN}/docker"

export PATH="${FAKE_BIN}:${PATH}"
export FAKE_DOCKER_LOG="${FAKE_LOG}"
export FAKE_DOCKER_PHASE="${FAKE_PHASE}"
export REMINDER_ENV_FILE="${NEW_ENV}"
export REMINDER_IMAGE='127.0.0.1:3000/admin/reminder-backend:new'
export REMINDER_STATE_DIR="${DEPLOY_STATE}"
export REMINDER_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.reminder.yml"

if bash "${SCRIPT_DIR}/deploy-reminder.sh" >"${DEPLOY_OUTPUT}" 2>&1; then
  echo 'Expected failed deployment to return a non-zero status' >&2
  exit 1
fi

if [[ ! -f "${FAKE_LOG}" ]]; then
  cat "${DEPLOY_OUTPUT}" >&2
  exit 1
fi

deploy_call_count="$(wc -l < "${FAKE_LOG}" | tr -d ' ')"
first_call="$(sed -n '1p' "${FAKE_LOG}")"
second_call="$(sed -n '2p' "${FAKE_LOG}")"

assert_contains() {
  local actual="$1"
  local expected="$2"
  if [[ "${actual}" != *"${expected}"* ]]; then
    printf 'Expected [%s] to contain [%s]\n' "${actual}" "${expected}" >&2
    exit 1
  fi
}

assert_files_equal() {
  local expected="$1"
  local actual="$2"
  if [[ "$(cat "${expected}")" != "$(cat "${actual}")" ]]; then
    printf 'Expected [%s] and [%s] to have identical content\n' "${expected}" "${actual}" >&2
    exit 1
  fi
}

if [[ "${deploy_call_count}" -ne 2 ]]; then
  printf 'Expected two deploy calls, got %s\n' "${deploy_call_count}" >&2
  exit 1
fi
assert_contains "${first_call}" "phase=new env=${NEW_ENV}"
assert_contains "${second_call}" "phase=rollback env=${canonical_old_release}/runtime.env"
assert_contains "${second_call}" "compose=${canonical_old_release}/docker-compose.reminder.yml"
assert_contains "${second_call}" 'image=127.0.0.1:3000/admin/reminder-backend:old'
if [[ "$(readlink "${DEPLOY_STATE}/current")" != 'releases/old' ]]; then
  echo 'Current deployment state changed after a failed rollout' >&2
  exit 1
fi

rm -f "${FAKE_PHASE}" "${FAKE_LOG}"
export FAKE_DOCKER_NEW_HEALTH=healthy
if ! bash "${SCRIPT_DIR}/deploy-reminder.sh" >"${DEPLOY_OUTPUT}" 2>&1; then
  cat "${DEPLOY_OUTPUT}" >&2
  exit 1
fi

current_target="$(readlink "${DEPLOY_STATE}/current")"
if [[ "${current_target}" == 'releases/old' ]]; then
  echo 'Successful deployment did not promote a new audited state' >&2
  exit 1
fi
current_release="$(cd "${DEPLOY_STATE}/${current_target}" && pwd -P)"
assert_files_equal "${NEW_ENV}" "${current_release}/runtime.env"
assert_files_equal "${SCRIPT_DIR}/docker-compose.reminder.yml" "${current_release}/docker-compose.reminder.yml"
if ! grep -qxF '127.0.0.1:3000/admin/reminder-backend:new' "${current_release}/image"; then
  echo 'Successful deployment state recorded the wrong image' >&2
  exit 1
fi

echo 'deployment rollback and audited state promotion passed'
