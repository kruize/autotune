#!/bin/bash
#
# Copyright (c) 2020, 2020 Red Hat Corporation and others.
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
DOCKER_MANIFEST="manifests/docker/autotune-docker.yaml"
DOCKER_TMP_JSON="manifests/docker/autotune-docker-tmp.json"
DOCKER_JSON="manifests/docker/autotune-docker.json"
PROMETHEUS_MANIFEST="manifests/docker/prometheus.yaml"
GRAFANA_MANIFESTS="manifests/docker/grafana/"

CADVISOR_DOCKER_IMAGE="google/cadvisor:latest"
PROMETHEUS_DOCKER_IMAGE="prom/prometheus:latest"
GRAFANA_DOCKER_IMAGE="grafana/grafana:latest"
if [ $(arch) == "ppc64le" ]; then
	PROMETHEUS_DOCKER_IMAGE="quay.io/powercloud/prometheus-linux-ppc64le:latest"
	GRAFANA_DOCKER_IMAGE="ppc64le/grafana:latest"
fi

CADVISOR_PORT="8080"
PROMETHEUS_PORT="9090"
GRAFANA_PORT="3000"

NETWORK="autotune-network"

################################  v Docker v ##################################

# Read the docker manifest and build a list of containers to be monitored
function get_all_monitored_containers() {
	all_monitored_containers=$(cat ${DOCKER_MANIFEST} | grep "name" | grep -v -E "^\s?#" | awk -F '"' '{ print $2 }')
	all_monitored_containers="${all_monitored_containers} $(cat ${DOCKER_MANIFEST} | grep "name" | grep -v -E "^\s?#" | grep -v '"' | awk -F ':' '{ print $2 }')"
	all_monitored_containers=$(echo ${all_monitored_containers} | sort | uniq)

	echo ${all_monitored_containers}
}

function create_dummy_json_file() {
	printf '{\n  "containers": [' > ${DOCKER_JSON}
	printf '     { "name": "autotune", "cpu_limit": "0", "mem_limit": "0" }' >> ${DOCKER_JSON}
	printf '  ]\n}\n' >> ${DOCKER_JSON}
}

function create_json_file() {
	printf '{\n  "containers": [' > ${DOCKER_TMP_JSON}
}

function close_json_file() {
	printf '\n  ]\n}' >> ${DOCKER_TMP_JSON}
	sed -ie "$(sed -n '/name/ =' ${DOCKER_TMP_JSON} | tail -n 1)"' s/,$//' ${DOCKER_TMP_JSON}
}

function create_json_entry() {
	printf "\n    { \"name\": \"$1\", \"cpu_limit\": \"$2\", \"mem_limit\": \"$3\" }," >> ${DOCKER_TMP_JSON}
}

function get_container_info() {
	create_json_file
	for ctnr in $(get_all_monitored_containers)
	do
		docker inspect ${ctnr} >/dev/null 2>/dev/null
		if [ $? -ne 0 ]; then
			echo " ${ctnr}: not found running. Ignoring..."
			continue;
		fi
		echo " ${ctnr}: found. Adding to list of containers to be monitored."
		# Get the container id from docker inspect
		cont_id=$(docker inspect ${ctnr} | grep '\"Id\":' | awk -F'"' '{ print $4 }')
		# Get quota and period
		cont_cpu_quota=$(docker inspect ${ctnr} | grep -E '"CpuQuota":' | awk '{ print $2 }' | awk -F',' '{ print $1 }')
		cont_cpu_period=$(docker inspect ${ctnr} | grep -E '"CpuPeriod":' | awk '{ print $2 }' | awk -F',' '{ print $1 }')
		cont_mem_limit=$(docker inspect ${ctnr} | grep -E '"Memory":' | awk '{ print $2 }' | awk -F',' '{ print $1 }')

		# Calculate the cpu_limit using the period and the quota
		# If the period is not set, assume a default period of 100000
		if [ ${cont_cpu_period} -eq 0 ]; then
			cont_cpu_limit=$(( ${cont_cpu_quota} / 100000 ))
		else
			cont_cpu_limit=$(( ${cont_cpu_quota} / ${cont_cpu_period} ))
		fi

		create_json_entry ${ctnr} ${cont_cpu_limit} ${cont_mem_limit}
	done
	close_json_file
	cp ${DOCKER_TMP_JSON} ${DOCKER_JSON}
}

#
function app_monitor_loop() {
	echo "########################     Starting App Monitor loop    #########################"
	echo "autotune recommendations available on the grafana dashboard at: http://localhost:3000"
	echo "Info: Press CTRL-C to exit"
	loop_count=${timeout}
	while true
	do
		get_container_info
		sleep 5
		loop_count=$(( ${loop_count} - 5 ))
		if [ ${timeout} -gt 0 ] && [ ${loop_count} -le 0 ]; then
			break;
		fi
	done
}

#
function docker_prereq() {
	echo
	echo "Info: Checking pre requisites for Docker..."

	docker pull ${CADVISOR_DOCKER_IMAGE} 2>/dev/null
	check_err "Error: Unable to pull prometheus docker image: ${CADVISOR_DOCKER_IMAGE}"

	docker pull ${PROMETHEUS_DOCKER_IMAGE} 2>/dev/null
	check_err "Error: Unable to pull prometheus docker image: ${PROMETHEUS_DOCKER_IMAGE}"

	docker pull ${GRAFANA_DOCKER_IMAGE} 2>/dev/null
	check_err "Error: Unable to pull grafana docker image: ${GRAFANA_DOCKER_IMAGE}"

	docker pull ${AUTOTUNE_DOCKER_IMAGE} 2>/dev/null
	if [ $? != 0 ]; then
		# Check if the image is available locally. Eg testing a image built locally
		DOCKER_REPO=$(echo ${AUTOTUNE_DOCKER_IMAGE} | awk -F":" '{ print $1 }')
		DOCKER_TAG=$(echo ${AUTOTUNE_DOCKER_IMAGE} | awk -F":" '{ print $2 }')
		if [ -z "${DOCKER_TAG}" ]; then
			DOCKER_TAG="latest"
		fi
		IMAGE_AVAIL=$(docker images | grep "${DOCKER_REPO}" | grep "${DOCKER_TAG}")
		if [ -z "${IMAGE_AVAIL}" ]; then
			echo "Error: Unable to locate autotune docker image: ${AUTOTUNE_DOCKER_IMAGE}"
		fi
	fi

	NET_NAME=`docker network ls -f "name=${NETWORK}" --format {{.Name}} | tail -n 1`

	if [[ -z $NET_NAME ]];  then
		echo "Creating autotune network: ${NETWORK}"
		docker network create ${NETWORK}
	else
		echo "${NETWORK} already exists"
	fi
}

#
function docker_setup() {
	echo "Starting cadvisor container"
	docker run -d --rm --name=cadvisor   -p ${CADVISOR_PORT}:${CADVISOR_PORT}      --net=${NETWORK}   --cpus=1   --volume=/:/rootfs:ro  --volume=/var/run:/var/run:ro   --volume=/sys:/sys:ro   --volume=/var/lib/docker/:/var/lib/docker:ro   --volume=/dev/disk/:/dev/disk:ro  ${CADVISOR_DOCKER_IMAGE}
	check_err "Error: cadvisor did not start up"

	echo "Starting prometheus container"
	docker run -d --rm --name=prometheus -p ${PROMETHEUS_PORT}:${PROMETHEUS_PORT}  --net=${NETWORK} -v ${ROOT_DIR}/${PROMETHEUS_MANIFEST}:/etc/prometheus/prometheus.yml ${PROMETHEUS_DOCKER_IMAGE}
	check_err "Error: prometheus did not start up"

	echo "Starting grafana container"
	docker run -d --rm --name=grafana    -p ${GRAFANA_PORT}:${GRAFANA_PORT}        --net=${NETWORK} -v ${ROOT_DIR}/${GRAFANA_MANIFESTS}:/etc/grafana/provisioning/ ${GRAFANA_DOCKER_IMAGE}
	check_err "Error: grafana did not start up"
}

#
function docker_deploy() {
	echo 
	create_dummy_json_file
	echo "Info: Waiting for prometheus/grafana/cadvisor to be up and running"
	sleep 2
	echo "Starting autotune container"
	docker run -d --rm --name=autotune     -p ${AUTOTUNE_PORT}:${AUTOTUNE_PORT}          --net=${NETWORK} --env CLUSTER_TYPE="DOCKER" --env MONITORING_AGENT_ENDPOINT="http://prometheus:${PROMETHEUS_PORT}" --env MONITORING_AGENT="Prometheus" -v ${ROOT_DIR}/${DOCKER_JSON}:/opt/app/autotune-docker.json ${AUTOTUNE_DOCKER_IMAGE}
	check_err "Error: autotune did not start up"
	echo "Waiting for autotune container to come up"
	sleep 10
	app_monitor_loop
}

# 
function docker_start() {
	echo
	echo "###   Installing autotune for docker..."
	echo
	docker_prereq
	docker_setup
	docker_deploy
}

function docker_terminate() {
	echo -n "###   Uninstalling autotune for docker..."
	docker stop autotune grafana prometheus cadvisor 2>/dev/null
	rm -f ${DOCKER_TMP_JSON} ${DOCKER_JSON}
	echo "done"
}
