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
##### script containing constants for configmap yaml tests #####
#

# get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"

testcase_matched=0

# testcases for configmap yaml 
configmap_tests=("configmap_minikube")

# tests on minikube
configmap_minikube_testcases=("minikube-invalid-cluster-type"
"minikube-invalid-k8s-type"
"minikube-invalid-monitoring-agent"
"minikube-invalid-monitoring-service"
"minikube-debug-config")

# String to be searched in minikube configmap
declare -A configmap_minikube_find
configmap_minikube_find=([minikube-invalid-cluster-type]='kubernetes'
[minikube-invalid-k8s-type]='minikube'
[minikube-invalid-monitoring-agent]='prometheus'
[minikube-invalid-monitoring-service]='prometheus-k8s'
[minikube-debug-config]='info')

# String to be replaced in minikube configmap
declare -A configmap_minikube_replace
configmap_minikube_replace=([minikube-invalid-cluster-type]='kube8'
[minikube-invalid-k8s-type]='docker'
[minikube-invalid-monitoring-agent]='promo'
[minikube-invalid-monitoring-service]='prom-k8s'
[minikube-debug-config]='debug')

# Expected autotune object for minikube configmap
declare -A configmap_minikube_autotune_objects
configmap_minikube_autotune_objects=([minikube-invalid-cluster-type]='true' 
[minikube-invalid-k8s-type]='true'  
[minikube-invalid-monitoring-agent]='true'
[minikube-invalid-monitoring-service]='true'
[minikube-debug-config]='true' )

# Expected log message for minikube configmap
declare -A configmap_minikube_expected_log_msgs
deployment_info='com.autotune.analyzer.deployment.DeploymentInfo'
configmap_minikube_expected_log_msgs=([minikube-invalid-cluster-type]=''${deployment_info}' - Cluster type kube8 is not supported'
[minikube-invalid-k8s-type]=''${deployment_info}' - k8s type docker is not suppported'
[minikube-invalid-monitoring-agent]=''${deployment_info}' - Monitoring agent promo  is not supported'
[minikube-invalid-monitoring-service]='com.autotune.analyzer.datasource.DataSourceFactory - Monitoring agent endpoint not found'
[minikube-debug-config]='DEBUG' )
