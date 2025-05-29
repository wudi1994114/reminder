#!/bin/bash
set -e

# --- Configuration ---
# !!! IMPORTANT: If you used a DOCKER_REGISTRY_PREFIX in build-docker.sh and pushed images,
# ensure this prefix matches. If images are local and untagged with a prefix, set to "".
DOCKER_REGISTRY_PREFIX="wwmty.cn"
MODULE_NAME="reminder-core"
CONTAINER_NAME="reminder-core" # Ensure container name is 'reminder-core' for Nginx proxy
NETWORK_NAME="reminder-network" # Define network name

IMAGE_TO_RUN="${MODULE_NAME}:latest"
if [ -n "${DOCKER_REGISTRY_PREFIX}" ]; then
  IMAGE_TO_RUN="${DOCKER_REGISTRY_PREFIX}/${MODULE_NAME}:latest"
fi

HOST_PORT=8080
CONTAINER_PORT=8080

# --- Script Logic ---
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

# Stop and remove existing container with the same name, if any
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

# Create network if it doesn't exist
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
    echo "Creating Docker network: ${NETWORK_NAME}"
    docker network create "${NETWORK_NAME}"
else
    echo "Docker network '${NETWORK_NAME}' already exists."
fi

# Run the Docker container in detached mode (-d)
# --name: Assigns a name to the container
# -p: Publishes container's port to the host
# --restart: Automatically restart the container unless explicitly stopped
docker run -d --name "${CONTAINER_NAME}" --network "${NETWORK_NAME}" --restart=always -p ${HOST_PORT}:${CONTAINER_PORT} "${IMAGE_TO_RUN}"

echo "Container ${CONTAINER_NAME} should be starting on network '${NETWORK_NAME}'."
echo "You can check logs with: docker logs ${CONTAINER_NAME}"
echo "To stop the container: docker stop ${CONTAINER_NAME}" 