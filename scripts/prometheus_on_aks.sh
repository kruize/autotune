#!/bin/bash
#
# Copyright (c) 2024, 2025 Red Hat, IBM Corporation and others.
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

current_dir="$(dirname "$0")"
non_interactive=0
# Call setup by default (and not terminate)
setup=1

function install_prometheus(){
    echo
    echo "Info: Prometheus Setup on AKS"

    prometheus_ns="monitoring"
	kubectl_cmd="kubectl -n ${prometheus_ns}"
	prometheus_pod_running=$(${kubectl_cmd} get pods | grep "prometheus-k8s-1")

	if [ "${prometheus_pod_running}" != "" ]; then
		echo "Prometheus is already installed and running."
		return;
	fi

    rm -rf aks_prom_download || true
    mkdir aks_prom_download 2>/dev/null
    pushd aks_prom_download >/dev/null
        echo "Info: Cloning Prometheus Operator & few AKS Customisations git"
        git clone https://github.com/mukrishn/arohcp-workspace
        git clone https://github.com/prometheus-operator/kube-prometheus
        pushd kube-prometheus
        kubectl apply --server-side -f manifests/setup
        kubectl wait \
            --for condition=Established \
            --all CustomResourceDefinition \
            --namespace=monitoring
        kubectl apply -f manifests/
		popd
		echo "Info: Applying Custom config Changes"
		pushd arohcp-workspace/aks-monitoring-config
		kubectl apply -f prometheus-roleBindingSpecificNamespaces.yaml || true
		kubectl apply -f prometheus-roleSpecificNamespaces.yaml || true
		kubectl apply -f prom-public-svc.yaml || true
}

function delete_prometheus(){
   echo "Info: Removing Prometheus Setup from AKS"
   rm -rf aks_remove || true
   mkdir aks_prom_remove 2>/dev/null
   pushd aks_prom_remove
    echo "Info: Cloning Prometheus Operator"
    git clone https://github.com/prometheus-operator/kube-prometheus
    pushd kube-prometheus
    kubectl delete --ignore-not-found=true -f manifests/ -f manifests/setup
}

# Input from user to install/delete prometheus
function usage() {
	echo >&2 "usage: $0 [-a] [-s|t] where -a= non-interactive mode,  -s=start, -t=terminate ";
	exit 1;
}

# empty argument validation
if [ $# -eq 0 ]; then
	usage
fi

while getopts "ast" option;
do
	case "${option}" in
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
	echo "Info: Installing Prometheus on AKS..."
	install_prometheus
else
	echo "Info: Deleting prometheus from AKS..."
	delete_prometheus
fi
