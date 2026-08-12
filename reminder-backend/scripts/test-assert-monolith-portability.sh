#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
isolated_path="$(mktemp -d)"

cleanup() {
  rm -rf "${isolated_path}"
}
trap cleanup EXIT

for command in bash dirname find grep wc tr; do
  ln -s "$(command -v "${command}")" "${isolated_path}/${command}"
done

PATH="${isolated_path}" bash "${script_dir}/assert-monolith.sh"
