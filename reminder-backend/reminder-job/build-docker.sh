#!/bin/bash
set -e

# --- Configuration ---
# !!! IMPORTANT: Replace YOUR_DOCKER_REGISTRY_PREFIX with your actual Docker registry prefix !!!
DOCKER_REGISTRY_PREFIX="wwmty.cn"

MODULE_NAME="reminder-job"
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

# --- Build Process ---
cd "$(dirname "$0")"

# 0. Build and install the common module first
echo "Building and installing ${COMMON_MODULE_PATH} module..."
(cd "${COMMON_MODULE_PATH}" && mvn clean install -DskipTests)

echo "Building ${MODULE_NAME} Spring Boot application with Maven..."
mvn clean package -DskipTests

echo "Building Docker image ${MODULE_NAME}:temp_build ..."
docker build -t "${IMAGE_NAME_LATEST}" .

echo "Tagging image ${IMAGE_NAME_LATEST} as ${IMAGE_NAME_TIMESTAMPED}"
docker tag ${IMAGE_NAME_LATEST} "${IMAGE_NAME_TIMESTAMPED}"

# Optional: docker rmi "${MODULE_NAME}:temp_build"

echo "Docker image tagging complete."
