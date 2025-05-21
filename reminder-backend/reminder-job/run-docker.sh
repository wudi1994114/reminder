#!/bin/bash
set -e

# --- Configuration ---
DOCKER_REGISTRY_PREFIX="wwmty.cn"
MODULE_NAME="reminder-job"
CONTAINER_NAME="${MODULE_NAME}-container"

IMAGE_TO_RUN="${MODULE_NAME}:latest"
if [ -n "${DOCKER_REGISTRY_PREFIX}" ]; then
  IMAGE_TO_RUN="${DOCKER_REGISTRY_PREFIX}/${MODULE_NAME}:latest"
fi

# Optional: If reminder-job exposes a port (e.g., for Actuator), uncomment and set these
# HOST_JOB_PORT=8081
# CONTAINER_JOB_PORT=8081 # Must match EXPOSE in Dockerfile if used

# --- Script Logic ---
echo "Attempting to run Docker container ${CONTAINER_NAME} from image ${IMAGE_TO_RUN}..."

if [ "$(docker ps -q -f name=^/${CONTAINER_NAME}$)" ]; then
    echo "Stopping existing container ${CONTAINER_NAME}..."
    docker stop "${CONTAINER_NAME}"
fi
if [ "$(docker ps -aq -f name=^/${CONTAINER_NAME}$)" ]; then
    echo "Removing existing container ${CONTAINER_NAME}..."
    docker rm "${CONTAINER_NAME}"
fi

echo "Running new container ${CONTAINER_NAME}..."

# Add port mapping if HOST_JOB_PORT and CONTAINER_JOB_PORT are set
PORT_MAPPING_ARGS=""
# if [ -n "${HOST_JOB_PORT}" ] && [ -n "${CONTAINER_JOB_PORT}" ]; then
#    PORT_MAPPING_ARGS="-p ${HOST_JOB_PORT}:${CONTAINER_JOB_PORT}"
#    echo "Mapping host port ${HOST_JOB_PORT} to container port ${CONTAINER_JOB_PORT}"
# fi

docker run -d --name "${CONTAINER_NAME}" ${PORT_MAPPING_ARGS} "${IMAGE_TO_RUN}"

echo "Container ${CONTAINER_NAME} should be starting."
echo "You can check logs with: docker logs ${CONTAINER_NAME}"
echo "To stop the container: docker stop ${CONTAINER_NAME}" 