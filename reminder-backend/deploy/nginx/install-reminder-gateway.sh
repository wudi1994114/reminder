#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly DEFAULT_GATEWAY_CONFIG="/opt/saas-app/nginx/gateway.conf"
readonly GATEWAY_CONFIG="${REMINDER_GATEWAY_CONFIG:-${DEFAULT_GATEWAY_CONFIG}}"
readonly GATEWAY_CONTAINER="${REMINDER_GATEWAY_CONTAINER:-saas-gateway}"
readonly GATEWAY_SOURCE="${REMINDER_GATEWAY_SOURCE:?REMINDER_GATEWAY_SOURCE is required}"
readonly BACKUP_DIR="${REMINDER_GATEWAY_BACKUP_DIR:-/opt/saas-app/reminder/gateway-backups}"
readonly BLOCK_START='# BEGIN REMINDER API'
readonly BLOCK_END='# END REMINDER API'

fail() {
  printf 'reminder gateway install failed: %s\n' "$1" >&2
  exit 1
}

[[ -f "${GATEWAY_CONFIG}" && ! -L "${GATEWAY_CONFIG}" ]] || fail "gateway config must be a regular file: ${GATEWAY_CONFIG}"
[[ -f "${GATEWAY_SOURCE}" ]] || fail "gateway source does not exist: ${GATEWAY_SOURCE}"

source_start_count="$(grep -Fxc -- "${BLOCK_START}" "${GATEWAY_SOURCE}" || true)"
source_end_count="$(grep -Fxc -- "${BLOCK_END}" "${GATEWAY_SOURCE}" || true)"
[[ "${source_start_count}" == '1' && "${source_end_count}" == '1' ]] \
  || fail 'gateway source must contain exactly one complete managed block'

candidate="$(mktemp "${GATEWAY_CONFIG}.candidate.XXXXXX")"
backup=""
installed=0

cleanup() {
  rm -f "${candidate}"
}

restore_previous_gateway() {
  local exit_code=$?
  trap - ERR
  if [[ "${installed}" == '1' && -n "${backup}" ]]; then
    install -m 640 "${backup}" "${GATEWAY_CONFIG}"
    docker exec "${GATEWAY_CONTAINER}" nginx -t >/dev/null
    docker exec "${GATEWAY_CONTAINER}" nginx -s reload >/dev/null || true
  fi
  cleanup
  exit "${exit_code}"
}

trap cleanup EXIT
trap restore_previous_gateway ERR

awk -v start="${BLOCK_START}" -v end="${BLOCK_END}" '
  $0 == start {
    if (inside) {
      exit 11
    }
    inside = 1
    seen += 1
    next
  }
  $0 == end {
    if (!inside) {
      exit 12
    }
    inside = 0
    next
  }
  !inside { print }
  END {
    if (inside) {
      exit 13
    }
  }
' "${GATEWAY_CONFIG}" > "${candidate}" || fail 'existing gateway configuration has an incomplete managed block'

printf '\n' >> "${candidate}"
sed -n "/^${BLOCK_START}$/,/^${BLOCK_END}$/p" "${GATEWAY_SOURCE}" >> "${candidate}"

docker run --rm --network saas-app \
  -v "${candidate}:/etc/nginx/conf.d/default.conf:ro" \
  nginx:1.27-alpine nginx -t >/dev/null

install -d -m 700 "${BACKUP_DIR}"
backup="${BACKUP_DIR}/gateway.conf.$(date +%Y%m%d%H%M%S).before-reminder"
install -m 600 "${GATEWAY_CONFIG}" "${backup}"
install -m 640 "${candidate}" "${GATEWAY_CONFIG}"
installed=1

docker exec "${GATEWAY_CONTAINER}" nginx -t
docker exec "${GATEWAY_CONTAINER}" nginx -s reload

installed=0
printf '%s\n' 'Reminder gateway configuration installed'
