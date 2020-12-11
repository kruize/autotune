#!/bin/bash
#
# Copyright (c) 2020, 2020 Red Hat, IBM Corporation and others.
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

###############################  v MiniKube v #################################
# Default mode is interactive
non_interactive=0

function install_prometheus() {
    echo
	echo "Info: Checking pre requisites for prometheus..."
	kubectl_tool=$(which kubectl)
	check_err "Error: Please install the kubectl tool"

	kubectl kustomize --help >/dev/null 2>/dev/null
	check_err "Error: Please install a newer version of kubectl tool that supports the kustomize option (>=v1.12)"

    prometheus_ns="monitoring"
	kubectl_cmd="kubectl -n ${prometheus_ns}"
	prometheus_pod_running=$(${kubectl_cmd} get pods | grep "prometheus-k8s-1")

	if [ "${prometheus_pod_running}" == "" ]; then
		if [ ${non_interactive} == 0 ]; then
			echo "Info:You can install cadvisor/prometheus in minikube"
			echo -n "Download and install these software to minikube(y/n)? "
			read inst
			linst=$(echo ${inst} | tr A-Z a-z)
			if [ ${linst} == "n" ]; then
				echo "Info: prometheus not installed"
				exit 0
			fi
		fi

		mkdir minikube_downloads 2>/dev/null
		pushd minikube_downloads >/dev/null
			echo "Info: Downloading cadvisor git"
			git clone https://github.com/google/cadvisor.git 2>/dev/null
			pushd cadvisor/deploy/kubernetes/base >/dev/null
			echo
			echo "Info: Installing cadvisor"
			kubectl kustomize . | kubectl apply -f-
			check_err "Error: Unable to install cadvisor"
			popd >/dev/null
			echo
			echo "Info: Downloading prometheus git"
			git clone https://github.com/coreos/kube-prometheus.git 2>/dev/null
			pushd kube-prometheus/manifests >/dev/null
			echo
			echo "Info: Installing prometheus"
			kubectl apply -f setup
			check_err "Error: Unable to setup prometheus"
			kubectl apply -f .
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
		check_running prometheus-k8s-1
		sleep 2
	else
		echo "Prometheus is already installed and running."
	fi	
}

function delete_prometheus() {
	echo -n "###   Removing cadvisor and prometheus"
	pushd minikube_downloads > /dev/null
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
	rm -rf minikube_downloads
}

function check_running() {
	
	check_pod=$1
    prometheus_ns="monitoring"
	kubectl_cmd="kubectl -n ${prometheus_ns}"

	echo "Info: Waiting for ${check_pod} to come up..."
	while true;
	do
		sleep 2
		${kubectl_cmd} get pods | grep ${check_pod}
		pod_stat=$(${kubectl_cmd} get pods | grep ${check_pod} | awk '{ print $3 }' | grep -v 'Terminating')
		case "${pod_stat}" in
			"ContainerCreating"|"Terminating"|"Pending")
				sleep 2
				;;
			"Running")
				echo "Info: ${check_pod} deploy succeeded: ${pod_stat}"
				err=0
				break;
				;;
			*)
				echo "Error: ${check_pod} deploy failed: ${pod_stat}"
				err=-1
				break;
				;;
		esac
	done

	${kubectl_cmd} get pods | grep ${check_pod}
	echo
}

#Taking input from user to test the script

echo Would you like to install/delete prometheus:? 
read option
if [ ${option} == "install" ]; then
	install_prometheus
elif [ ${option} == "delete" ]; then
	delete_prometheus 

else
	echo "Entered input is not valid option, please provide a valid option !"	
fi		

