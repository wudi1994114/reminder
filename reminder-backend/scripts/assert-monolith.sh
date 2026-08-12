#!/usr/bin/env bash
set -euo pipefail

backend_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
pom_file="${backend_dir}/pom.xml"

if grep -Eq '<packaging>[[:space:]]*pom[[:space:]]*</packaging>|<modules>' "${pom_file}"; then
  echo "root pom is still an aggregator" >&2
  exit 1
fi

for module in reminder-common reminder-core reminder-job reminder-stream-consumer; do
  if [[ -e "${backend_dir}/${module}/pom.xml" ]]; then
    echo "legacy module remains: ${module}" >&2
    exit 1
  fi
done

if [[ ! -d "${backend_dir}/src/main/java/com/common/reminder" ]] ||
   [[ ! -d "${backend_dir}/src/main/java/com/core/reminder" ]] ||
   [[ ! -d "${backend_dir}/src/main/java/com/task/reminder" ]]; then
  echo "monolith source packages are incomplete" >&2
  exit 1
fi

application_count="$( (find "${backend_dir}/src/main/java" -type f -name '*.java' -exec grep -l '@SpringBootApplication' {} + || true) | wc -l | tr -d ' ')"
if [[ "${application_count}" != "1" ]]; then
  echo "expected exactly one Spring Boot application, found ${application_count}" >&2
  exit 1
fi

echo "backend is a single Maven/Spring Boot application"
