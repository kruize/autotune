#!/bin/bash
#
# Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
ROOT_DIR="${PWD}"
SCRIPTS_DIR="${ROOT_DIR}/scripts"

AUTOTUNE_DOCKER_REPO="kruize/autotune"
# Fetch autotune version from the pom.xml file.
AUTOTUNE_VERSION="$(grep -A 1 "autotune" "${ROOT_DIR}"/pom.xml | grep version | awk -F '>' '{ split($2, a, "<"); print a[1] }')"
AUTOTUNE_DOCKER_IMAGE=${AUTOTUNE_DOCKER_REPO}:${AUTOTUNE_VERSION}

function usage() {
	echo "Usage: $0 [-v version_string] [-t docker_image_name]"
	exit -1
}

# Check error code from last command, exit on error
function check_err() {
	err=$?
	if [ ${err} -ne 0 ]; then
		echo "$*"
		exit -1
	fi
}

# Remove any previous images of autotune
function cleanup() {
	echo -n "Cleanup any previous kruize images..."
	docker stop autotune >/dev/null 2>/dev/null
	sleep 5
	docker rmi $(docker images | grep autotune | awk '{ print $3 }') >/dev/null 2>/dev/null
	docker rmi $(docker images | grep autotune | awk '{ printf "%s:%s\n", $1, $2 }') >/dev/null 2>/dev/null
	echo "done"
}

# Iterate through the commandline options
while getopts t:v: gopts
do
	case ${gopts} in
	v)
		AUTOTUNE_VERSION="${OPTARG}"
		;;
	t)
		AUTOTUNE_DOCKER_IMAGE="${OPTARG}"
		;;
	[?])
		usage
	esac
done

git pull
cleanup

DOCKER_REPO=$(echo ${AUTOTUNE_DOCKER_IMAGE} | awk -F":" '{ print $1 }')
DOCKER_TAG=$(echo ${AUTOTUNE_DOCKER_IMAGE} | awk -F":" '{ print $2 }')
if [ -z "${DOCKER_TAG}" ]; then
	DOCKER_TAG="latest"
fi

# Build the docker image with the given version string
DOCKERFILE="Dockerfile"
docker build --pull --no-cache --build-arg AUTOTUNE_VERSION=${DOCKER_TAG} -t ${AUTOTUNE_DOCKER_IMAGE} -f ${DOCKERFILE} .
check_err "Docker build of ${AUTOTUNE_DOCKER_IMAGE} failed."

docker images | grep -e "TAG" -e "${DOCKER_REPO}" | grep "${DOCKER_TAG}"
