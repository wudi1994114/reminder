#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

readonly SERVICE_NAME="reminder-backend"
readonly CONTAINER_NAME="reminder-backend"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly COMPOSE_FILE="${REMINDER_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.reminder.yml}"
readonly ENV_FILE="${REMINDER_ENV_FILE:?REMINDER_ENV_FILE is required}"
readonly STATE_DIR="${REMINDER_STATE_DIR:?REMINDER_STATE_DIR is required}"
readonly RELEASES_DIR="${STATE_DIR}/releases"
readonly CURRENT_STATE_LINK="${STATE_DIR}/current"
readonly EXPECTED_IMAGE_PREFIX="${REMINDER_IMAGE_PREFIX:-172.17.0.3:5001/reminder-backend}:"

: "${REMINDER_IMAGE:?REMINDER_IMAGE is required}"

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "Compose file not found: ${COMPOSE_FILE}" >&2
  exit 1
fi
if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Runtime env file not found: ${ENV_FILE}" >&2
  exit 1
fi
if [[ "${REMINDER_IMAGE}" != "${EXPECTED_IMAGE_PREFIX}"* ]]; then
    echo "Refusing unexpected image: ${REMINDER_IMAGE}" >&2
    exit 1
fi
if [[ -L "${STATE_DIR}" ]]; then
    echo "Refusing symlink deployment state directory: ${STATE_DIR}" >&2
    exit 1
fi
if [[ -L "${RELEASES_DIR}" ]]; then
    echo "Refusing symlink releases directory: ${RELEASES_DIR}" >&2
    exit 1
fi
install -d -m 700 "${STATE_DIR}" "${RELEASES_DIR}"
readonly CANONICAL_RELEASES_DIR="$(cd "${RELEASES_DIR}" && pwd -P)"

export REMINDER_IMAGE
deploy_compose=(docker compose --project-name reminder --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")

if ! "${deploy_compose[@]}" config --services | grep -qx "${SERVICE_NAME}"; then
    echo "Compose file does not define the expected service: ${SERVICE_NAME}" >&2
    exit 1
fi
"${deploy_compose[@]}" config --quiet

previous_image="$(docker inspect --format '{{.Config.Image}}' "${CONTAINER_NAME}" 2>/dev/null || true)"
previous_state=""
previous_env_file=""
previous_compose_file=""

if [[ -n "${previous_image}" ]]; then
    if [[ "${previous_image}" != "${EXPECTED_IMAGE_PREFIX}"* ]]; then
        echo "Refusing to replace unexpected running image: ${previous_image}" >&2
        exit 1
    fi
    if [[ ! -L "${CURRENT_STATE_LINK}" ]]; then
        echo "Cannot safely deploy: previous runtime state is missing at ${CURRENT_STATE_LINK}" >&2
        exit 1
    fi

    state_target="$(readlink "${CURRENT_STATE_LINK}")"
    if [[ "${state_target}" == /* ]]; then
        state_path="${state_target}"
    else
        state_path="${STATE_DIR}/${state_target}"
    fi
    previous_state="$(cd "${state_path}" && pwd -P)"
    if [[ "${previous_state}" != "${CANONICAL_RELEASES_DIR}/"* ]]; then
        echo "Refusing deployment state outside releases directory: ${previous_state}" >&2
        exit 1
    fi

    previous_env_file="${previous_state}/runtime.env"
    previous_compose_file="${previous_state}/docker-compose.reminder.yml"
    previous_image_file="${previous_state}/image"
    if [[ ! -f "${previous_env_file}" || ! -f "${previous_compose_file}" || ! -f "${previous_image_file}" ]]; then
        echo "Cannot safely deploy: previous release state is incomplete" >&2
        exit 1
    fi
    IFS= read -r stored_previous_image < "${previous_image_file}"
    if [[ "${stored_previous_image}" != "${previous_image}" ]]; then
        echo "Cannot safely deploy: running image differs from audited deployment state" >&2
        exit 1
    fi
fi

wait_for_health() {
  local attempt status
  for attempt in $(seq 1 45); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${CONTAINER_NAME}" 2>/dev/null || true)"
    if [[ "${status}" == "healthy" ]]; then
      docker exec "${CONTAINER_NAME}" curl --fail --silent --show-error \
        http://127.0.0.1:8080/actuator/health >/dev/null
      return 0
    fi
    if [[ "${status}" == "unhealthy" || "${status}" == "exited" || "${status}" == "dead" ]]; then
      break
    fi
    sleep 2
  done

  docker logs --tail 120 "${CONTAINER_NAME}" 2>&1 || true
  return 1
}

rollback() {
    local exit_code=$?
    trap - ERR
    if [[ "${deployment_mutated}" != "1" ]]; then
        exit "${exit_code}"
    fi

    echo "Reminder deployment failed; rolling back only ${SERVICE_NAME}." >&2
    if [[ -n "${previous_image}" ]]; then
        export REMINDER_IMAGE="${previous_image}"
        rollback_compose=(docker compose --project-name reminder \
            --env-file "${previous_env_file}" -f "${previous_compose_file}")
        "${rollback_compose[@]}" config --quiet
        "${rollback_compose[@]}" up -d --no-deps --force-recreate "${SERVICE_NAME}"
        wait_for_health || true
    else
        "${deploy_compose[@]}" rm --stop --force "${SERVICE_NAME}" >/dev/null 2>&1 || true
    fi
    exit "${exit_code}"
}

record_successful_release() {
    local release_id release_dir link_target
    release_id="$(date +%Y%m%d%H%M%S)-$$"
    release_dir="${RELEASES_DIR}/${release_id}"
    link_target="releases/${release_id}"

    install -d -m 700 "${release_dir}"
    install -m 600 "${ENV_FILE}" "${release_dir}/runtime.env"
    install -m 600 "${COMPOSE_FILE}" "${release_dir}/docker-compose.reminder.yml"
    printf '%s\n' "${REMINDER_IMAGE}" > "${release_dir}/image"
    chmod 600 "${release_dir}/image"
    ln -sfn "${link_target}" "${CURRENT_STATE_LINK}"
}

deployment_mutated=0
trap rollback ERR

"${deploy_compose[@]}" pull "${SERVICE_NAME}"
deployment_mutated=1
"${deploy_compose[@]}" up -d --no-deps --force-recreate "${SERVICE_NAME}"
wait_for_health
record_successful_release

trap - ERR
echo "Reminder deployment succeeded: ${REMINDER_IMAGE}"
