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

# include the common_utils.sh script to access methods
current_dir="$(dirname "$0")"
source ${current_dir}/common_utils.sh
non_interactive=0
# Call setup by default (and not terminate)
setup=1

# Prometheus release default
tag="v0.8.0"

function install_prometheus() {
	echo
	echo "Info: Checking pre requisites for prometheus..."
	check_kustomize

	prometheus_ns="monitoring"
	kubectl_cmd="kubectl -n ${prometheus_ns}"
	prometheus_pod_running=$(${kubectl_cmd} get pods | grep "prometheus-k8s-1")

	if [ "${prometheus_pod_running}" != "" ]; then
		echo "Prometheus is already installed and running."
		return;
	fi

	if [ "${non_interactive}" == 0 ]; then
		echo "Info: Prometheus needs cadvisor/grafana"
		echo -n "Download and install these software to KIND(y/n)? "
		read inst
		linst=$(echo ${inst} | tr A-Z a-z)
		if [ "${linst}" == "n" ]; then
			echo "Info: prometheus not installed"
			exit 0
		fi
	fi

	mkdir kind_downloads 2>/dev/null
	pushd kind_downloads >/dev/null
		echo "Info: Downloading cadvisor git"
		git clone https://github.com/google/cadvisor.git 2>/dev/null
		pushd cadvisor/deploy/kubernetes/base >/dev/null
		echo
		echo "Info: Installing cadvisor"
		kubectl kustomize . | kubectl apply -f-
		check_err "Error: Unable to install cadvisor"
		popd >/dev/null
		echo
		echo "Info: Downloading prometheus git release - ${tag}"
		# Commenting the below lines as the latest prometheus requires more than 2 CPUs and the PR checks fail on github hosted runners
		# as they have only 2 CPUs on the host. Hence switching back to prometheus release 0.8.0 (Apr 2021)
		# git clone https://github.com/coreos/kube-prometheus.git 2>/dev/null
		git clone -b ${tag} https://github.com/coreos/kube-prometheus.git 2>/dev/null
		pushd kube-prometheus/manifests >/dev/null
		echo
		echo "Info: Installing prometheus"
		kubectl apply -f setup --server-side
		check_err "Error: Unable to setup prometheus"
		kubectl apply -f . --server-side
		check_err "Error: Unable to install prometheus"
		popd >/dev/null
	popd >/dev/null

	echo -n "Info: Waiting for all Prometheus Pods to get spawned..."
	while true;
	do
		# Wait for prometheus docker images to get downloaded and spawn the main pod
		pod_started=$(${kubectl_cmd} get pods | grep "prometheus-k8s-1")
		if [ "${pod_started}" == "" ]; then
			# prometheus-k8s-1 not yet spawned
			echo -n "."
			sleep 5
		else
			echo "done"
			break;
		fi
	done
	check_running prometheus-k8s-1 ${prometheus_ns}
	sleep 2
}

function delete_prometheus() {
	echo -n "###   Removing cadvisor and prometheus"
	pushd kind_downloads > /dev/null
		echo
		echo "Removing cadvisor"
		pushd cadvisor/deploy/kubernetes/base > /dev/null
		kubectl kustomize . | kubectl delete -f-
		popd > /dev/null

		echo
		echo "Removing prometheus"
		pushd kube-prometheus/manifests > /dev/null
		kubectl delete -f . 2>/dev/null
		kubectl delete -f setup 2>/dev/null
		popd > /dev/null
	popd > /dev/null
	rm -rf kind_downloads
}

# Input from user to install/delete prometheus
function usage() {
	echo >&2 "usage: $0 [-r <prometheus release tag (default - v0.8.0)] [-a] [-s|t] where -a= non-interactive mode,  -s=start, -t=terminate ";
	exit 1;
}

# empty argument validation
if [ $# -eq 0 ]; then
	usage
fi

while getopts "r:ast" option;
do
	case "${option}" in
	r)
		tag=${OPTARG}
		;;
	a)
		non_interactive=1
		;;
	s) # For option s to install the prometheus
		setup=1
		;;
	t) # For option t terminating and deleting the prometheus
		setup=0
		;;
	\?) # For invalid option
		usage
		;;
	esac
done

if [ "${setup}" == 1 ]; then
	echo "Info: installing prometheus..."
	install_prometheus
else
	echo "Info: deleting prometheus..."
	delete_prometheus
fi
