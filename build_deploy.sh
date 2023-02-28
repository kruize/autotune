#!/bin/bash

set -exv

IMAGE_NAME="quay.io/cloudservices/autotune"
IMAGE_TAG=$(git rev-parse --short=7 HEAD)
ADDITIONAL_TAGS="qa latest"
AUTOTUNE_DOCKERFILE="Dockerfile.autotune"

if [[ -z "$QUAY_USER" || -z "$QUAY_TOKEN" ]]; then
    echo "QUAY_USER and QUAY_TOKEN must be set"
    exit 1
fi

if [[ -z "$RH_REGISTRY_USER" || -z "$RH_REGISTRY_TOKEN" ]]; then
    echo "RH_REGISTRY_USER and RH_REGISTRY_TOKEN  must be set"
    exit 1
fi

DOCKER_CONF="$PWD/.docker"
mkdir -p "$DOCKER_CONF"
docker --config="$DOCKER_CONF" login -u="$QUAY_USER" -p="$QUAY_TOKEN" quay.io
docker --config="$DOCKER_CONF" login -u="$RH_REGISTRY_USER" -p="$RH_REGISTRY_TOKEN" registry.redhat.io
docker --config="$DOCKER_CONF" build -t "${IMAGE_NAME}:${IMAGE_TAG}" -f ${AUTOTUNE_DOCKERFILE} .
docker --config="$DOCKER_CONF" push "${IMAGE_NAME}:${IMAGE_TAG}"

for ADDITIONAL_TAG in $ADDITIONAL_TAGS; do
    docker --config="$DOCKER_CONF" tag "${IMAGE_NAME}:${IMAGE_TAG}" "${IMAGE_NAME}:${ADDITIONAL_TAG}"
    docker --config="$DOCKER_CONF" push "${IMAGE_NAME}:${ADDITIONAL_TAG}"
done
