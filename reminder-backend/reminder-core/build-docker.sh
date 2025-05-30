#!/bin/bash
# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
DOCKER_REGISTRY_PREFIX="wwmty.cn" # Set to your Docker registry prefix

MODULE_NAME="reminder-core"
COMMON_MODULE_PATH="../reminder-common" # Path to the common module

# Generate timestamp tag YYYYMMDDHHMM
TIMESTAMP_TAG=$(date +"%Y%m%d%H%M")

# Define full image names
IMAGE_NAME_BASE="${MODULE_NAME}"
if [ -n "${DOCKER_REGISTRY_PREFIX}" ]; then
  IMAGE_NAME_BASE="${DOCKER_REGISTRY_PREFIX}/${MODULE_NAME}"
fi

IMAGE_NAME_LATEST="${IMAGE_NAME_BASE}:latest"
IMAGE_NAME_TIMESTAMPED="${IMAGE_NAME_BASE}:${TIMESTAMP_TAG}"

# --- Git Pull Process ---
echo "Pulling latest code from GitHub main branch..."
cd "$(dirname "$0")"
# Navigate to project root (assuming we're in reminder-backend/reminder-core)
cd ../../
git fetch origin
git checkout main
git pull origin main
echo "Successfully pulled latest code from main branch."

# --- Build Process ---
cd "$(dirname "$0")"

echo "Building and installing ${COMMON_MODULE_PATH} module..."
(cd "${COMMON_MODULE_PATH}" && mvn clean install -DskipTests)

echo "Building ${MODULE_NAME} Spring Boot application with Maven..."
mvn clean package -DskipTests

echo "Building Docker image ${IMAGE_NAME_LATEST}..."
docker build -t "${IMAGE_NAME_LATEST}" .

echo "Tagging image ${IMAGE_NAME_LATEST} as ${IMAGE_NAME_TIMESTAMPED}..."
docker tag "${IMAGE_NAME_LATEST}" "${IMAGE_NAME_TIMESTAMPED}"

echo "Docker image tagging complete."

