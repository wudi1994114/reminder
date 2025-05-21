#!/bin/bash
set -e

# --- Configuration ---
DOCKER_REGISTRY_PREFIX="wwmty.cn"
MODULE_NAME="reminder-frontend"
CONTAINER_NAME="${MODULE_NAME}-container"
NETWORK_NAME="reminder-network" # Define network name

IMAGE_TO_RUN="${MODULE_NAME}:latest"
if [ -n "${DOCKER_REGISTRY_PREFIX}" ]; then
  IMAGE_TO_RUN="${DOCKER_REGISTRY_PREFIX}/${MODULE_NAME}:latest"
fi

HOST_PORT=5173       # Host port you want to access the frontend on
CONTAINER_PORT=80    # Nginx in the container listens on port 80 (as per Dockerfile)

# --- Script Logic ---
# Create network if it doesn\'t exist (primarily for explicitness, core script should create it)
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
    echo "Warning: Docker network \'${NETWORK_NAME}\' not found. It should be created by the core service script."
    echo "Attempting to create Docker network: ${NETWORK_NAME}"
    docker network create "${NETWORK_NAME}"
else
    echo "Docker network \'${NETWORK_NAME}\' found."
fi

echo "Attempting to run Docker container ${CONTAINER_NAME} from image ${IMAGE_TO_RUN}..."

# ---- New: Stop and remove ANY existing containers based on the IMAGE_TO_RUN ----
echo "Performing cleanup of any existing containers using the image ${IMAGE_TO_RUN}..."
EXISTING_IMAGE_CONTAINERS_IDS=$(docker ps -aq --filter ancestor="${IMAGE_TO_RUN}")

if [ -n "$EXISTING_IMAGE_CONTAINERS_IDS" ]; then
    echo "Found existing containers based on image ${IMAGE_TO_RUN}. Attempting to stop and remove them."
    # Stop them first. Filter for only running ones to avoid errors with 'docker stop' on stopped containers.
    RUNNING_IMAGE_CONTAINERS_IDS=$(docker ps -q --filter ancestor="${IMAGE_TO_RUN}")
    if [ -n "$RUNNING_IMAGE_CONTAINERS_IDS" ]; then
        echo "Stopping running containers: $RUNNING_IMAGE_CONTAINERS_IDS"
        docker stop $RUNNING_IMAGE_CONTAINERS_IDS || true # Continue if some are already stopped or fail
    fi
    # Remove all (stopped and previously running)
    echo "Removing containers: $EXISTING_IMAGE_CONTAINERS_IDS"
    docker rm $EXISTING_IMAGE_CONTAINERS_IDS || true # Continue if some are already removed or fail
else
    echo "No existing containers found using the image ${IMAGE_TO_RUN}."
fi
# ---- End New ----

if [ "$(docker ps -q -f name=^/${CONTAINER_NAME}$)" ]; then
    echo "Stopping existing container ${CONTAINER_NAME}..."
    docker stop "${CONTAINER_NAME}"
fi
if [ "$(docker ps -aq -f name=^/${CONTAINER_NAME}$)" ]; then
    echo "Removing existing container ${CONTAINER_NAME}..."
    docker rm "${CONTAINER_NAME}"
fi

echo "Running new container ${CONTAINER_NAME}..."
echo "Mapping host port ${HOST_PORT} to container port ${CONTAINER_PORT}"

docker run -d --name "${CONTAINER_NAME}" --network "${NETWORK_NAME}" -p ${HOST_PORT}:${CONTAINER_PORT} "${IMAGE_TO_RUN}"

echo "Container ${CONTAINER_NAME} should be starting on network \'${NETWORK_NAME}\'."
echo "Frontend should be accessible at http://localhost:${HOST_PORT} (or your server's IP/domain)"
echo "You can check Nginx logs with: docker logs ${CONTAINER_NAME}"
echo "To stop the container: docker stop ${CONTAINER_NAME}" 