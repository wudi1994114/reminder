#!/usr/bin/env bash

set -Eeuo pipefail

readonly SERVICE_NAME="reminder-backend"
readonly CONTAINER_NAME="reminder-backend"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly COMPOSE_FILE="${REMINDER_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.reminder.yml}"
readonly ENV_FILE="${REMINDER_ENV_FILE:?REMINDER_ENV_FILE is required}"
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

export REMINDER_IMAGE
compose=(docker compose --project-name reminder --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")

if ! "${compose[@]}" config --services | grep -qx "${SERVICE_NAME}"; then
  echo "Compose file does not define the expected service: ${SERVICE_NAME}" >&2
  exit 1
fi
"${compose[@]}" config --quiet

previous_image="$(docker inspect --format '{{.Config.Image}}' "${CONTAINER_NAME}" 2>/dev/null || true)"
backup_dir="${REMINDER_BACKUP_DIR:-${SCRIPT_DIR}/.deploy-backups}"
mkdir -p "${backup_dir}"
cp "${COMPOSE_FILE}" "${backup_dir}/docker-compose.reminder.$(date +%Y%m%d%H%M%S).yml"

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
  echo "Reminder deployment failed; rolling back only ${SERVICE_NAME}." >&2
  if [[ -n "${previous_image}" ]]; then
    export REMINDER_IMAGE="${previous_image}"
    "${compose[@]}" up -d --no-deps --force-recreate "${SERVICE_NAME}"
    wait_for_health || true
  else
    "${compose[@]}" rm --stop --force "${SERVICE_NAME}" >/dev/null 2>&1 || true
  fi
  exit "${exit_code}"
}
trap rollback ERR

"${compose[@]}" pull "${SERVICE_NAME}"
"${compose[@]}" up -d --no-deps --force-recreate "${SERVICE_NAME}"
wait_for_health

trap - ERR
echo "Reminder deployment succeeded: ${REMINDER_IMAGE}"
