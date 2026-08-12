#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fake_bin="$(mktemp -d)"

cleanup() {
  rm -rf "${fake_bin}"
}
trap cleanup EXIT

cat > "${fake_bin}/cmp" <<'FAKE_CMP'
#!/usr/bin/env bash
exit 127
FAKE_CMP
chmod 700 "${fake_bin}/cmp"

PATH="${fake_bin}:${PATH}" bash "${script_dir}/test-deploy-reminder.sh"
