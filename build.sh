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
AUTOTUNE_DOCKERFILE="Dockerfile.autotune"
AUTOTUNE_DOCKER_REPO="kruize/autotune_operator"
# Fetch autotune version from the pom.xml file.
AUTOTUNE_VERSION="$(grep -A 1 "autotune" "${ROOT_DIR}"/pom.xml | grep version | awk -F '>' '{ split($2, a, "<"); print a[1] }')"
AUTOTUNE_DOCKER_IMAGE=${AUTOTUNE_DOCKER_REPO}:${AUTOTUNE_VERSION}
DEV_MODE=0
BUILD_PARAMS="--pull --no-cache"

function usage() {
	echo "Usage: $0 [-d] [-v version_string] [-i autotune_docker_image]"
	echo " -d: build in dev friendly mode"
	echo " -i: build with specific autotune operator docker image name"
	echo " -v: build as specific autotune version"
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

function set_tags() {
	AUTOTUNE_REPO=$(echo ${AUTOTUNE_DOCKER_IMAGE} | awk -F":" '{ print $1 }')
	DOCKER_TAG=$(echo ${AUTOTUNE_DOCKER_IMAGE} | awk -F":" '{ print $2 }')
	if [ -z "${DOCKER_TAG}" ]; then
		DOCKER_TAG="latest"
	fi
}

# Iterate through the commandline options
while getopts di:o:v: gopts
do
	case ${gopts} in
	d)
		DEV_MODE=1
		;;
	i)
		AUTOTUNE_DOCKER_IMAGE="${OPTARG}"
		;;
	v)
		AUTOTUNE_VERSION="${OPTARG}"
		;;
	[?])
		usage
	esac
done

git pull
set_tags
# Build the docker image with the given version string
if [ ${DEV_MODE} -eq 0 ]; then
	cleanup
else
	unset BUILD_PARAMS
fi
echo ${BUILD_PARAMS}

BUILDTMPFILE=/tmp/docker_build_log.$$
BUILDER="docker"

${BUILDER} build ${BUILD_PARAMS} --format=docker --build-arg AUTOTUNE_VERSION=${DOCKER_TAG} -t ${AUTOTUNE_DOCKER_IMAGE} -f ${AUTOTUNE_DOCKERFILE} . 2>${BUILDTMPFILE}
build_error=$(grep 'Error:\|unknown flag:' ${BUILDTMPFILE})

if [ -n "${build_error}" ]; then
	echo '--format=docker not supported'
	${BUILDER} build ${BUILD_PARAMS} --build-arg AUTOTUNE_VERSION=${DOCKER_TAG} -t ${AUTOTUNE_DOCKER_IMAGE} -f ${AUTOTUNE_DOCKERFILE} .
	check_err "Docker build of ${AUTOTUNE_DOCKER_IMAGE} failed."
fi

${BUILDER} images | grep -e "TAG" -e "${AUTOTUNE_REPO}" | grep "${DOCKER_TAG}"
