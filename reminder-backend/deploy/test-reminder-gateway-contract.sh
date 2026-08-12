#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly NGINX_DIR="${SCRIPT_DIR}/nginx"
readonly HTTP_CONFIG="${NGINX_DIR}/reminder-api-http.conf"
readonly HTTPS_CONFIG="${NGINX_DIR}/reminder-api-https.conf"
readonly STANDALONE_HTTPS_CONFIG="${NGINX_DIR}/reminder-api.conf"
readonly INSTALLER="${NGINX_DIR}/install-reminder-gateway.sh"

fail() {
  printf 'reminder gateway contract failed: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file="$1"
  local expected="$2"
  local description="$3"
  grep -Fqx -- "$expected" "$file" || fail "$description"
}

for required_file in "$HTTP_CONFIG" "$HTTPS_CONFIG" "$STANDALONE_HTTPS_CONFIG" "$INSTALLER"; do
  [[ -f "$required_file" ]] || fail "missing ${required_file}"
done

assert_contains "$HTTP_CONFIG" '# BEGIN REMINDER API' 'HTTP configuration has a managed-block start marker'
assert_contains "$HTTP_CONFIG" '# END REMINDER API' 'HTTP configuration has a managed-block end marker'
assert_contains "$HTTP_CONFIG" '    server_name reminder-api.wwmty.com;' 'HTTP configuration serves the Reminder hostname'
assert_contains "$HTTP_CONFIG" '    location ^~ /.well-known/acme-challenge/ {' 'HTTP configuration exposes the ACME webroot'
assert_contains "$HTTP_CONFIG" '        root /var/www/certbot;' 'HTTP configuration uses the shared certbot webroot'

assert_contains "$HTTPS_CONFIG" '# BEGIN REMINDER API' 'HTTPS configuration has a managed-block start marker'
assert_contains "$HTTPS_CONFIG" '# END REMINDER API' 'HTTPS configuration has a managed-block end marker'
assert_contains "$HTTPS_CONFIG" '    resolver 127.0.0.11 ipv6=off valid=30s;' 'HTTPS configuration resolves Docker service names at request time'
assert_contains "$HTTPS_CONFIG" '        set $reminder_upstream reminder-backend:8080;' 'HTTPS configuration proxies to the Reminder container on saas-app'
assert_contains "$HTTPS_CONFIG" '        proxy_pass http://$reminder_upstream;' 'HTTPS configuration avoids a host-loopback proxy'
assert_contains "$HTTPS_CONFIG" '    ssl_certificate /etc/letsencrypt/live/reminder-api.wwmty.com/fullchain.pem;' 'HTTPS configuration references the Reminder certificate'
assert_contains "$HTTPS_CONFIG" '    ssl_certificate_key /etc/letsencrypt/live/reminder-api.wwmty.com/privkey.pem;' 'HTTPS configuration references the Reminder key'

if grep -Fq '127.0.0.1:18080' "$HTTPS_CONFIG"; then
  fail 'HTTPS configuration must not proxy from the gateway container to host loopback'
fi
if grep -Fq '127.0.0.1:18080' "$STANDALONE_HTTPS_CONFIG"; then
  fail 'standalone HTTPS configuration must not proxy from the gateway container to host loopback'
fi

assert_contains "$INSTALLER" 'readonly DEFAULT_GATEWAY_CONFIG="/opt/saas-app/nginx/gateway.conf"' 'installer targets the actual mounted gateway config'
grep -Fq -- 'docker inspect --format' "$INSTALLER" \
  || fail 'installer discovers every live gateway network before candidate validation'
grep -Fq -- 'docker create --name "${candidate_container}" --network "${gateway_networks[0]}"' "$INSTALLER" \
  || fail 'installer starts the candidate on the gateway primary network'
grep -Fq -- '/opt/saas-app/certbot/letsencrypt:/etc/letsencrypt:ro' "$INSTALLER" \
  || fail 'installer gives the candidate the same certificate mount as the gateway'
grep -Fq -- '/opt/saas-app/certbot/www:/var/www/certbot:ro' "$INSTALLER" \
  || fail 'installer gives the candidate the same ACME webroot mount as the gateway'
grep -Fq -- 'docker network connect "${network}" "${candidate_container}"' "$INSTALLER" \
  || fail 'installer connects the candidate to every remaining gateway network'
grep -Fq -- 'docker start -a "${candidate_container}"' "$INSTALLER" \
  || fail 'installer runs Nginx syntax validation after all gateway networks are attached'
grep -Fq -- 'dd if="${source}" of="${destination}" conv=fsync status=none' "$INSTALLER" \
  || fail 'installer overwrites the file inode already mounted by the live gateway container'
if grep -Fq -- 'install -m 640 "${candidate}" "${GATEWAY_CONFIG}"' "$INSTALLER"; then
  fail 'installer must not replace the mounted gateway configuration inode'
fi
assert_contains "$INSTALLER" 'docker exec "${GATEWAY_CONTAINER}" nginx -t' 'installer validates the live gateway before reload'
assert_contains "$INSTALLER" 'docker exec "${GATEWAY_CONTAINER}" nginx -s reload' 'installer reloads only the gateway container'

printf '%s\n' 'reminder gateway contract passed'
