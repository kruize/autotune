#!/bin/bash
#
# Copyright (c) 2020, 2021 IBM Corporation, RedHat and others.
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
### Script to get pod and cluster information through prometheus queries###
#
# checks if the previous command is executed successfully
# input:Return value of previous command
# output:Prompts the error message if the return value is not zero
function err_exit()
{
	if [ $? != 0 ]; then
		printf "$*"
		echo
		exit -1
	fi
}

function cpu_metrics()
{
        URL=$1
        TOKEN=$2
        RESULTS_DIR=$3
        ITER=$4
        APP_NAME=$5
        DEPLOYMENT_NAME=$6
        CONTAINER_NAME=$7
        NAMESPACE=$8
	INTERVAL=$9
	CLUSTER_TYPE=${10}

        while true
        do
		if [[ ${CLUSTER_TYPE} == "openshift" ]]; then
        		TOKEN=`oc whoami --show-token`
		fi

		interval_start_time=`date`
                # Processing curl output "timestamp value" using jq tool.
		# cpu_request_avg_container
                cpu_request_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(kube_pod_container_resource_requests{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="cpu", unit="core"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# cpu_request_sum_container
		cpu_request_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(kube_pod_container_resource_requests{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="cpu", unit="core"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# cpu_limit_avg_container
                cpu_limit_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(kube_pod_container_resource_limits{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="cpu", unit="core"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# cpu_limit_sum_container
                cpu_limit_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(kube_pod_container_resource_limits{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="cpu", unit="core"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		#if [[ ${queryVersion} == "4.9" ]]; then
		if [[ ${CLUSTER_TYPE} == "openshift" ]]; then
			# cpu_usage_sum_container
                        cpu_usage_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

			# cpu_usage_avg_container
                        cpu_usage_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

                        # cpu_usage_max_container
                        cpu_usage_max_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

                        # cpu_usage_min_container
                        cpu_usage_min_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		else
			# cpu_usage_sum_container
                        cpu_usage_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

			# cpu_usage_avg_container
        	        cpu_usage_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

			# cpu_usage_max_container
	                cpu_usage_max_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

			# cpu_usage_min_container
                	cpu_usage_min_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`
		fi

		# cpu_throttle_avg_container
                cpu_throttle_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(rate(container_cpu_cfs_throttled_seconds_total{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`
		cpu_throttle_max_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=max(rate(container_cpu_cfs_throttled_seconds_total{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`
		cpu_throttle_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(rate(container_cpu_cfs_throttled_seconds_total{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		sleep ${INTERVAL}
		interval_end_time=`date`
		echo "${interval_start_time},${interval_end_time},${cpu_request_avg_container},${cpu_request_sum_container},${cpu_limit_avg_container},${cpu_limit_sum_container},${cpu_usage_sum_container},${cpu_usage_avg_container},${cpu_usage_max_container},${cpu_usage_min_container},${cpu_throttle_sum_container},${cpu_throttle_avg_container},${cpu_throttle_max_container}" >> ${RESULTS_DIR}/cpu_metrics.csv
        done
}

function mem_metrics()
{
        URL=$1
        TOKEN=$2
        RESULTS_DIR=$3
        ITER=$4
        APP_NAME=$5
        DEPLOYMENT_NAME=$6
        CONTAINER_NAME=$7
        NAMESPACE=$8
	INTERVAL=$9
	CLUSTER_TYPE=${10}

        # Delete the old json file if any
        rm -rf ${RESULTS_DIR}/mem_request_avg_container-${ITER}.json
        while true
        do
		if [[ ${CLUSTER_TYPE} == "openshift" ]]; then
                        TOKEN=`oc whoami --show-token`
                fi

		interval_start_time=`date`
                # Processing curl output "timestamp value" using jq tool.
		# mem_request_avg_container
                mem_request_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(kube_pod_container_resource_requests{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="memory", unit="byte"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_request_sum_container
		mem_request_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(kube_pod_container_resource_requests{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="memory", unit="byte"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_limit_avg_container
                mem_limit_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(kube_pod_container_resource_limits{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="memory", unit="byte"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_limit_sum_container
                mem_limit_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(kube_pod_container_resource_limits{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'", resource="memory", unit="byte"})' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_usage_sum_container
                mem_usage_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(avg_over_time(container_memory_working_set_bytes{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_usage_avg_container
                mem_usage_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(avg_over_time(container_memory_working_set_bytes{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_usage_min_container
                mem_usage_min_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=min(min_over_time(container_memory_working_set_bytes{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_usage_max_container
                mem_usage_max_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=max(max_over_time(container_memory_working_set_bytes{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_rss_sum_container
                mem_rss_sum_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(avg_over_time(container_memory_rss{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_rss_avg_container
                mem_rss_avg_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(avg_over_time(container_memory_rss{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_rss_min_container
                mem_rss_min_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=min(min_over_time(container_memory_rss{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		# mem_rss_max_container
                mem_rss_max_container=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=max(max_over_time(container_memory_rss{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", container="'"${CONTAINER_NAME}"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

                sleep ${INTERVAL}
		interval_end_time=`date`
		echo ",${mem_request_avg_container},${mem_request_sum_container},${mem_limit_avg_container},${mem_limit_sum_container},${mem_usage_sum_container},${mem_usage_avg_container},${mem_usage_max_container},${mem_usage_min_container},${mem_rss_sum_container},${mem_rss_avg_container},${mem_rss_max_container},${mem_rss_min_container}" >> ${RESULTS_DIR}/mem_metrics.csv
        done
}

function load_metrics()
{
        URL=$1
        TOKEN=$2
        RESULTS_DIR=$3
        ITER=$4
        APP_NAME=$5
        DEPLOYMENT_NAME=$6
        CONTAINER_NAME=$7
        NAMESPACE=$8
	INTERVAL=$9
	CLUSTER_TYPE=${10}

        while true
        do
		if [[ ${CLUSTER_TYPE} == "openshift" ]]; then
                        TOKEN=`oc whoami --show-token`
                fi

		interval_start_time=`date`
		# network_avg_pod
		network_sum_pod=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=sum(rate(container_network_receive_bytes_total{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`
		network_avg_pod=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=avg(rate(container_network_receive_bytes_total{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`
		network_max_pod=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=max(rate(container_network_receive_bytes_total{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`
		network_min_pod=`curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=min(rate(container_network_receive_bytes_total{pod=~"'"${DEPLOYMENT_NAME}-[^-]*-[^-]*$"'", namespace="'"${NAMESPACE}"'"}['"${INTERVAL}"']))' ${URL} | jq -c '[ .data.result[] | .value[1]] | .[]'`

		sleep ${INTERVAL}
		interval_end_time=`date`
		echo ",${network_sum_pod},${network_avg_pod},${network_max_pod},${network_min_pod}" >> ${RESULTS_DIR}/load_metrics.csv
	done

}

function getversion()
{
	echo "$@" | awk -F. '{ printf("%d%03d%03d%03d\n", $1,$2,$3,$4); }';
}

ITER=$1
TIMEOUT=$2
RESULTS_DIR=$3
BENCHMARK_SERVER=$4
APP_NAME=$5
CLUSTER_TYPE=$6
DEPLOYMENT_NAME=$7
CONTAINER_NAME=$8
NAMESPACE=$9
#INTERVAL=${10}
#DEPLOYMENT_NAME="tfb-qrh-sample-0"
#CONTAINER_NAME="tfb-server"
#NAMESPACE="autotune-tfb"
INTERVAL=1m

mkdir -p ${RESULTS_DIR}
#QUERY_APP=prometheus-k8s-openshift-monitoring.apps
if [[ ${CLUSTER_TYPE} == "openshift" ]]; then
	QUERY_APP=thanos-querier-openshift-monitoring.apps
	URL=https://${QUERY_APP}.${BENCHMARK_SERVER}/api/v1/query
	echo "URL = $URL"
	TOKEN=`oc whoami --show-token`
	VERSION=`oc version | grep "Server" | cut -d " " -f3`
	if [[ $(getversion $VERSION) -ge $(getversion "4.9") ]]; then
        	export queryVersion="4.9"
	fi
elif [[ ${CLUSTER_TYPE} == "minikube" ]]; then
	#QUERY_IP=`minikibe ip`
	QUERY_APP="${BENCHMARK_SERVER}:9090"
	URL=http://${QUERY_APP}/api/v1/query
	TOKEN=TOKEN
fi

export -f err_exit cpu_metrics mem_metrics load_metrics

echo "interval_start_time,interval_end_time,cpu_request_avg_container,cpu_request_sum_container,cpu_limit_avg_container,cpu_limit_sum_container,cpu_usage_sum_container,cpu_usage_avg_container,cpu_usage_max_container,cpu_usage_min_container,cpu_throttle_sum_container,cpu_throttle_avg_container,cpu_throttle_max_container" > ${RESULTS_DIR}/cpu_metrics.csv
echo ",mem_request_avg_container,mem_request_sum_container,mem_limit_avg_container,mem_limit_sum_container,mem_usage_sum_container,mem_usage_avg_container,mem_usage_max_container,mem_usage_min_container,mem_rss_sum_container,mem_rss_avg_container,mem_rss_max_container,mem_rss_min_container" > ${RESULTS_DIR}/mem_metrics.csv
echo ",network_sum_pod,network_avg_pod,network_max_pod,network_min_pod" > ${RESULTS_DIR}/load_metrics.csv

echo "Collecting metric data" >> setup.log
interval_start_time=`date`
timeout ${TIMEOUT} bash -c  "cpu_metrics ${URL} ${TOKEN} ${RESULTS_DIR} ${ITER} ${APP_NAME} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} ${INTERVAL} ${CLUSTER_TYPE}" &
timeout ${TIMEOUT} bash -c  "mem_metrics ${URL} ${TOKEN} ${RESULTS_DIR} ${ITER} ${APP_NAME} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} ${INTERVAL} ${CLUSTER_TYPE}" &
timeout ${TIMEOUT} bash -c  "load_metrics ${URL} ${TOKEN} ${RESULTS_DIR} ${ITER} ${APP_NAME} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} ${INTERVAL} ${CLUSTER_TYPE}" &
sleep ${TIMEOUT}
paste ${RESULTS_DIR}/cpu_metrics.csv ${RESULTS_DIR}/mem_metrics.csv ${RESULTS_DIR}/load_metrics.csv > ${RESULTS_DIR}/../monitoring_metrics.csv
