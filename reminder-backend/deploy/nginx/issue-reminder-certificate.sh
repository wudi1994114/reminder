#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

readonly DOMAIN='reminder-api.wwmty.com'
readonly LETSENCRYPT_DIR="${REMINDER_LETSENCRYPT_DIR:-/opt/saas-app/certbot/letsencrypt}"
readonly CERTBOT_WEBROOT="${REMINDER_CERTBOT_WEBROOT:-/opt/saas-app/certbot/www}"
readonly CERTBOT_IMAGE="${REMINDER_CERTBOT_IMAGE:-certbot/certbot:latest}"

fail() {
  printf 'Reminder certificate issue failed: %s\n' "$1" >&2
  exit 1
}

[[ -d "${LETSENCRYPT_DIR}" ]] || fail "Let's Encrypt directory does not exist: ${LETSENCRYPT_DIR}"
[[ -d "${CERTBOT_WEBROOT}" ]] || fail "Certbot webroot does not exist: ${CERTBOT_WEBROOT}"

acme_email="${REMINDER_ACME_EMAIL:-}"
if [[ -z "${acme_email}" ]]; then
  registration_file="$(find "${LETSENCRYPT_DIR}/accounts" -type f -name regr.json -print -quit 2>/dev/null || true)"
  if [[ -n "${registration_file}" ]]; then
    acme_email="$(sed -nE 's/^[[:space:]]*"email"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p' "${registration_file}" | head -n 1)"
  fi
fi

[[ -n "${acme_email}" ]] || fail 'set REMINDER_ACME_EMAIL or retain an existing registered ACME account'

docker run --rm \
  -v "${LETSENCRYPT_DIR}:/etc/letsencrypt" \
  -v "${CERTBOT_WEBROOT}:/var/www/certbot" \
  "${CERTBOT_IMAGE}" certonly --webroot \
  -w /var/www/certbot \
  -d "${DOMAIN}" \
  --non-interactive \
  --agree-tos \
  --no-eff-email \
  --keep-until-expiring \
  --email "${acme_email}"

[[ -s "${LETSENCRYPT_DIR}/live/${DOMAIN}/fullchain.pem" ]] || fail 'fullchain.pem was not created'
[[ -s "${LETSENCRYPT_DIR}/live/${DOMAIN}/privkey.pem" ]] || fail 'privkey.pem was not created'

printf '%s\n' 'Reminder certificate is ready'
