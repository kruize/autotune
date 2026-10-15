#!/bin/bash
#
# Copyright (c) 2023, 2023 IBM Corporation, RedHat and others.
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
### Script to run stress test with Kruize in remote monitoring mode ##
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO_PATH="${CURRENT_DIR}/../../../.."
PERFORMANCE_PROFILE_DIR="${KRUIZE_REPO_PATH}/manifests/autotune/performance-profiles"

# Source the common functions scripts
. ${CURRENT_DIR}/../../common/common_functions.sh

ITER=1
TIMEOUT=2000
RESULTS_DIR=/tmp/kruize_stress_test_results
BENCHMARK_SERVER=localhost
APP_NAME=kruize
CLUSTER_TYPE=minikube
DEPLOYMENT_NAME=kruize
CONTAINER_NAME=kruize
NAMESPACE=monitoring
users=100
rampup=200
num_res=30
loop=1
exp_type="container"

RESOURCE_OPTIMIZATION_JSON="${PERFORMANCE_PROFILE_DIR}/resource_optimization_openshift.json"

target="crc"
KRUIZE_IMAGE="quay.io/kruize/autotune:mvp_demo"

declare -l api_version

function usage() {
	echo
	echo "Usage: -c cluster_type [minikube|openshift] [-i Kruize image] [-u users] [-e No. of results] [-d ramp up time in seconds] [-r <resultsdir path> ] [-t TIMEOUT for metrics script] [-b Experiment type [container|namespace] default - container] [--api-version=<v1|legacy>]"
	exit -1
}

function get_kruize_pod_log() {
	log=$1

	# Fetch the kruize pod log

	echo ""
	echo "Fetch the kruize pod logs and store in ${log}..."
	kruize_pod=$(kubectl get pod -n ${NAMESPACE} | grep kruize | grep -v kruize-ui | grep -v kruize-db | cut -d " " -f1)
	kubectl logs -f ${kruize_pod} -n ${NAMESPACE} > ${log} 2>&1 &
}

# check_jmeter_results <jtl_csv> <stats_log>
# Three checks are performed in order; the first failure encountered marks the
# overall result as FAIL but all three checks always run so every issue is
# reported in a single pass.
#
#  1. Per-sampler HTTP pass/fail table from the JTL CSV (jmeter -l output).
#  2. "Recommendations Are Available" present in every reco JSON saved under
#     ${JMETER_LOG_DIR}/reco_jsons/.
#  3. No lines matching ERROR|FAILED|EXCEPTION (case-insensitive) in the
#     JMeter stats log (jmeter -j) or the top-level jmeter.log.
#
# Exits 0 when all three checks pass, exits 1 otherwise.
function check_jmeter_results() {
	local jtl_file="$1"
	local stats_file="$2"
	local reco_dir="${JMETER_LOG_DIR}/reco_jsons"
	local overall_rc=0

	echo "" | tee -a ${LOG}
	echo "============================================================" | tee -a ${LOG}
	echo "  JMeter Results Check" | tee -a ${LOG}
	echo "============================================================" | tee -a ${LOG}
	echo "Results directory : ${LOG_DIR}" | tee -a ${LOG}
	echo "JTL log           : ${jtl_file}" | tee -a ${LOG}
	echo "Stats log         : ${stats_file}" | tee -a ${LOG}
	echo "Reco JSONs dir    : ${reco_dir}" | tee -a ${LOG}
	echo "" | tee -a ${LOG}

	# ------------------------------------------------------------------
	# CHECK 1 — JTL sampler pass/fail
	# ------------------------------------------------------------------
	echo "------------------------------------------------------------" | tee -a ${LOG}
	echo "CHECK 1: JTL sampler results" | tee -a ${LOG}
	echo "------------------------------------------------------------" | tee -a ${LOG}

	if [ ! -f "${jtl_file}" ]; then
		echo "ERROR: JTL result file not found: ${jtl_file}" | tee -a ${LOG}
		exit 1
	fi

	local line_count
	line_count=$(wc -l < "${jtl_file}")
	if [ "${line_count}" -le 1 ]; then
		echo "ERROR: JTL result file is empty (no samples recorded): ${jtl_file}" | tee -a ${LOG}
		exit 1
	fi

	# JTL CSV columns (17 total, 1-based):
	#   1=timeStamp  2=elapsed  3=label  4=responseCode  5=responseMessage
	#   6=threadName  7=dataType  8=success  9=failureMessage  10=bytes
	#   11=sentBytes  12=grpThreads  13=allThreads  14=URL  15=Latency
	#   16=IdleTime  17=Connect
	# Rows with URL==null are internal JSR223 samplers — skip them.

	printf "  %-55s %8s %8s %8s %8s\n" "Sampler" "Total" "Pass" "Fail" "Fail%" | tee -a ${LOG}
	printf "  %-55s %8s %8s %8s %8s\n" \
		"-------------------------------------------------------" "-----" "-----" "-----" "-----" | tee -a ${LOG}

	local awk_output
	awk_output=$(awk -F',' '
		NR == 1 { next }
		$14 == "null" { next }
		{
			label = $3
			ok    = $8
			total[label]++
			if (ok == "true") pass[label]++
			else              fail[label]++
		}
		END {
			for (lbl in total) {
				t = total[lbl]
				p = pass[lbl] + 0
				f = fail[lbl] + 0
				pct = (t > 0) ? (f * 100.0 / t) : 0
				printf "%s|%d|%d|%d|%.1f\n", lbl, t, p, f, pct
			}
		}
	' "${jtl_file}" | sort)

	local grand_total=0 total_pass=0 total_fail=0 has_failures=0
	while IFS='|' read -r lbl t p f pct; do
		printf "  %-55s %8d %8d %8d %7s%%\n" "${lbl}" "${t}" "${p}" "${f}" "${pct}" | tee -a ${LOG}
		grand_total=$((grand_total + t))
		total_pass=$((total_pass + p))
		total_fail=$((total_fail + f))
		if [ "${f}" -gt 0 ]; then
			has_failures=1
		fi
	done <<< "${awk_output}"

	local fail_pct=0
	if [ "${grand_total}" -gt 0 ]; then
		fail_pct=$(awk "BEGIN {printf \"%.1f\", ${total_fail} * 100.0 / ${grand_total}}")
	fi
	printf "  %-55s %8s %8s %8s %8s\n" \
		"-------------------------------------------------------" "-----" "-----" "-----" "-----" | tee -a ${LOG}
	printf "  %-55s %8d %8d %8d %7s%%\n" \
		"TOTAL" "${grand_total}" "${total_pass}" "${total_fail}" "${fail_pct}" | tee -a ${LOG}
	echo "" | tee -a ${LOG}

	echo "JMeter summariser:" | tee -a ${LOG}
	if [ -f "${stats_file}" ]; then
		grep "Summariser: summary =" "${stats_file}" | tail -1 | tee -a ${LOG}
	else
		echo "  (stats file not found: ${stats_file})" | tee -a ${LOG}
	fi
	echo "" | tee -a ${LOG}

	if [ "${has_failures}" -eq 1 ]; then
		echo "CHECK 1 RESULT: FAIL - one or more HTTP samplers recorded failures." | tee -a ${LOG}
		overall_rc=1
	else
		echo "CHECK 1 RESULT: PASS - all HTTP samplers succeeded." | tee -a ${LOG}
	fi

	# ------------------------------------------------------------------
	# CHECK 2 — "Recommendations Are Available" in every reco JSON
	# ------------------------------------------------------------------
	echo "" | tee -a ${LOG}
	echo "------------------------------------------------------------" | tee -a ${LOG}
	echo "CHECK 2: Recommendations Are Available in reco JSONs" | tee -a ${LOG}
	echo "------------------------------------------------------------" | tee -a ${LOG}

	local reco_total=0 reco_pass=0 reco_fail=0
	local reco_failed_files=()

	if [ ! -d "${reco_dir}" ]; then
		echo "ERROR: reco_jsons directory not found: ${reco_dir}" | tee -a ${LOG}
		echo "CHECK 2 RESULT: FAIL - reco_jsons directory missing." | tee -a ${LOG}
		overall_rc=1
	else
		local json_files
		mapfile -t json_files < <(find "${reco_dir}" -maxdepth 1 -name "*.json" | sort)
		reco_total=${#json_files[@]}

		if [ "${reco_total}" -eq 0 ]; then
			echo "ERROR: No JSON files found in ${reco_dir}" | tee -a ${LOG}
			echo "CHECK 2 RESULT: FAIL - no reco JSON files present." | tee -a ${LOG}
			overall_rc=1
		else
			for json_file in "${json_files[@]}"; do
				if grep -qi "Recommendations Are Available" "${json_file}"; then
					reco_pass=$((reco_pass + 1))
				else
					reco_fail=$((reco_fail + 1))
					reco_failed_files+=("$(basename "${json_file}")")
				fi
			done

			echo "  Reco JSONs checked : ${reco_total}" | tee -a ${LOG}
			echo "  With recommendations: ${reco_pass}" | tee -a ${LOG}
			echo "  Missing recommendations: ${reco_fail}" | tee -a ${LOG}

			if [ "${reco_fail}" -gt 0 ]; then
				echo "" | tee -a ${LOG}
				echo "  Files missing 'Recommendations Are Available':" | tee -a ${LOG}
				for f in "${reco_failed_files[@]}"; do
					echo "    - ${f}" | tee -a ${LOG}
				done
				echo "" | tee -a ${LOG}
				echo "CHECK 2 RESULT: FAIL - ${reco_fail} of ${reco_total} reco JSON(s) missing 'Recommendations Are Available'." | tee -a ${LOG}
				overall_rc=1
			else
				echo "" | tee -a ${LOG}
				echo "CHECK 2 RESULT: PASS - all ${reco_total} reco JSON(s) contain 'Recommendations Are Available'." | tee -a ${LOG}
			fi
		fi
	fi

	# ------------------------------------------------------------------
	# CHECK 3 — No ERROR / FAILED / EXCEPTION in JMeter log files
	# ------------------------------------------------------------------
	echo "" | tee -a ${LOG}
	echo "------------------------------------------------------------" | tee -a ${LOG}
	echo "CHECK 3: No errors or exceptions in JMeter logs" | tee -a ${LOG}
	echo "------------------------------------------------------------" | tee -a ${LOG}

	# Scan the stats log (jmeter -j), the JMeter stdout log, and the default
	# jmeter.log written by JMeter to the working directory.
	# Match lines where the JMeter log level is ERROR (3rd field on a timestamped
	# log line), or stack-trace continuation lines containing "exception" or
	# "failed".  This deliberately excludes the benign INFO-level phrase
	# "Thread will continue on error" which appears in every normal run.
	local log_scan_files=()
	[ -f "${stats_file}" ] && log_scan_files+=("${stats_file}")
	[ -f "${JMETER_LOG}" ] && log_scan_files+=("${JMETER_LOG}")
	[ -f "${CURRENT_DIR}/jmeter.log" ] && log_scan_files+=("${CURRENT_DIR}/jmeter.log")

	local log_has_errors=0
	for scan_file in "${log_scan_files[@]}"; do
		local matches
		matches=$(grep -in "^[0-9-]* [0-9:,]* ERROR\|exception\|failed" "${scan_file}" 2>/dev/null)
		if [ -n "${matches}" ]; then
			log_has_errors=1
			local match_count
			match_count=$(echo "${matches}" | wc -l)
			echo "  Issues found in: $(basename "${scan_file}") (${match_count} line(s))" | tee -a ${LOG}
			echo "${matches}" | head -10 | while IFS= read -r line; do
				echo "    ${line}" | tee -a ${LOG}
			done
			if [ "${match_count}" -gt 10 ]; then
				echo "    ... (${match_count} total matching lines — see full log for details)" | tee -a ${LOG}
			fi
			echo "" | tee -a ${LOG}
		else
			echo "  No issues in: $(basename "${scan_file}")" | tee -a ${LOG}
		fi
	done

	echo "" | tee -a ${LOG}
	if [ "${log_has_errors}" -eq 1 ]; then
		echo "CHECK 3 RESULT: FAIL - ERROR/FAILED/EXCEPTION found in JMeter logs." | tee -a ${LOG}
		overall_rc=1
	else
		echo "CHECK 3 RESULT: PASS - no errors or exceptions in JMeter logs." | tee -a ${LOG}
	fi

	# ------------------------------------------------------------------
	# Overall verdict
	# ------------------------------------------------------------------
	echo "" | tee -a ${LOG}
	echo "============================================================" | tee -a ${LOG}
	if [ "${overall_rc}" -eq 1 ]; then
		echo "OVERALL RESULT: FAIL" | tee -a ${LOG}
	else
		echo "OVERALL RESULT: PASS" | tee -a ${LOG}
	fi
	echo "============================================================" | tee -a ${LOG}
	exit ${overall_rc}
}

function java21_install() {
	JAVA_VERSION=21.0.11_10
	JAVA_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.11+10/OpenJDK21U-jdk_x64_linux_hotspot_${JAVA_VERSION}.tar.gz"
	JAVA_INSTALL_DIR=/tmp/temurin21

	# Check if Temurin 21 is already installed at the expected location
	if [ -x "${JAVA_INSTALL_DIR}/bin/java" ]; then
		echo "Java is already installed at ${JAVA_INSTALL_DIR}." | tee -a ${LOG}
	else
		echo "Java not found. Downloading from ${JAVA_URL} ..." | tee -a ${LOG}
		local tarball
		tarball=$(mktemp --suffix=.tar.gz)
		if ! wget -q -O "${tarball}" "${JAVA_URL}"; then
			echo "ERROR: Failed to download Java from ${JAVA_URL}" | tee -a ${LOG}
			rm -f "${tarball}"
			exit 1
		fi
		mkdir -p "${JAVA_INSTALL_DIR}"
		tar -xzf "${tarball}" --strip-components=1 -C "${JAVA_INSTALL_DIR}"
		rm -f "${tarball}"
		echo "Java installation complete." | tee -a ${LOG}
	fi

	export JAVA_HOME="${JAVA_INSTALL_DIR}"
	export PATH="${JAVA_HOME}/bin:${PATH}"
	echo "JAVA_HOME set to ${JAVA_HOME}" | tee -a ${LOG}
	java -version 2>&1 | tee -a ${LOG}
}

function jmeter_setup() {
	JMETER_VERSION="5.6.3"

	if [ ! -d ${CURRENT_DIR}/apache-jmeter-${JMETER_VERSION} ]; then
		echo "Downloading jmeter..." | tee -a ${LOG}
		wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz
		tar -xzf apache-jmeter-${JMETER_VERSION}.tgz
		rm apache-jmeter-${JMETER_VERSION}.tgz
	else
		echo "Skipping jmeter install as it is already present here - ${CURRENT_DIR}/apache-jmeter-${JMETER_VERSION}"
	fi
	export JMETER_HOME=${CURRENT_DIR}/apache-jmeter-${JMETER_VERSION}
	export PATH=${JMETER_HOME}/bin:${PATH}
}

while getopts c:r:i:u:d:t:e:b:-: gopts
do
	case ${gopts} in
	-)
		case "${OPTARG}" in
			api-version=*)
				api_version=${OPTARG#*=}
				;;
		esac
		;;
	c)
		CLUSTER_TYPE=${OPTARG}
		;;
	r)
		RESULTS_DIR="${OPTARG}"		
		;;
	i)
		KRUIZE_IMAGE="${OPTARG}"		
		;;
	u)
		users="${OPTARG}"		
		;;
	e)
		num_res="${OPTARG}"
		;;
	d)
		rampup="${OPTARG}"		
		;;
	t)
		TIMEOUT="${OPTARG}"		
		;;
	b)
		exp_type="${OPTARG}"
		;;
	esac
done

echo "remote_monitoring_stress_test.sh :: api_version = ${api_version}"

if [ -z "${CLUSTER_TYPE}" ]; then
	usage
fi

if [[ "${exp_type}" != "container" && "${exp_type}" != "namespace" ]]; then
	echo "-b option values should be container or namespace, if not specified default is container"
	usage
fi

LOG_DIR="${RESULTS_DIR}/remote-monitoring-stress-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/remote-monitoring-stress.log"
METRICS_LOG_DIR="${LOG_DIR}/resource_usage"
mkdir -p ${METRICS_LOG_DIR}

prometheus_pod_running=$(kubectl get pods --all-namespaces | grep "prometheus-k8s-0")
if [ "${prometheus_pod_running}" == "" ]; then
	echo "Install prometheus required to fetch the resource usage metrics for kruize"
	exit 1

fi

JMETER_LOG_DIR="${LOG_DIR}/jmeter_logs" 
mkdir -p ${JMETER_LOG_DIR}

echo "Installing Java 21 prerequisite for jmeter..." | tee -a ${LOG}
java21_install
echo "Installing Java 21 prerequisite for jmeter...done" | tee -a ${LOG}

echo "Invoking jmeter setup" | tee -a ${LOG}
jmeter_setup
echo "Invoking jmeter setup...done" | tee -a ${LOG}

echo "Setting up kruize..." | tee -a ${LOG}
echo "Removing isROSEnabled=false and local=true"
cluster_type=${CLUSTER_TYPE}
pushd ${KRUIZE_REPO_PATH} > /dev/null
	kruize_remote_patch
	echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${LOG_DIR}/kruize_setup.log"
	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${LOG_DIR}/kruize_setup.log 2>&1

	sleep 5
	echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${LOG_DIR}/kruize_setup.log"
	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${LOG_DIR}/kruize_setup.log 2>&1
	sleep 20
popd > /dev/null
echo "Setting up kruize...Done" | tee -a ${LOG}


case ${CLUSTER_TYPE} in
	minikube)
		if [ -z "${SERVER_IP_ADDR}" ]; then
			SERVER_IP_ADDR=$(minikube ip)
			echo "Port forward prometheus..." | tee -a ${LOG}
			kubectl port-forward svc/prometheus-k8s 9090:9090 -n ${NAMESPACE} > /dev/null 2>/dev/null &
			echo "Port forward prometheus...done" | tee -a ${LOG}
			port=$(kubectl -n ${NAMESPACE} get svc ${APP_NAME} --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
			if [ "${port}" == "" ]; then
				echo "Failed to get the Kruize port, Check if kruize is runnning!" | tee -a ${LOG}
				exit -1
			fi
			BENCHMARK_SERVER="localhost"
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} BENCHMARK_SERVER = ${BENCHMARK_SERVER} port = ${port}"
		fi
		;;
	openshift)
		NAMESPACE="openshift-tuning"
		if [ -z "${SERVER_IP_ADDR}" ]; then
			oc expose svc/kruize -n ${NAMESPACE}

			SERVER_IP_ADDR=($(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3))
			port=""
			BENCHMARK_SERVER=$(echo ${SERVER_IP_ADDR} | cut -d "." -f3-)
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} BENCHMARK_SERVER = ${BENCHMARK_SERVER}"
		fi
		;;
	*)
		err_exit "Error: Cluster type ${CLUSTER_TYPE} is not supported" | tee -a ${LOG}
		;;
esac	

# Start monitoring metrics
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo ""
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" | tee -a ${LOG}
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} > ${LOG_DIR}/monitor-metrics.log 2>&1 &

	# Create the performance profile
	# If kruize service is exposed then do not specify the port	
	cmd="curl http://${SERVER_IP_ADDR}/createPerformanceProfile -d @${RESOURCE_OPTIMIZATION_JSON}"
	echo ""
	echo "cmd = ${cmd}"
	curl http://${SERVER_IP_ADDR}/createPerformanceProfile -d @"${RESOURCE_OPTIMIZATION_JSON}"
else
	echo ""
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" | tee -a ${LOG}
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &

	# Create the performance profile
	cmd="curl http://${SERVER_IP_ADDR}:${port}/createPerformanceProfile -d @${RESOURCE_OPTIMIZATION_JSON}"
	echo ""
	echo "cmd = ${cmd}"
	curl http://${SERVER_IP_ADDR}:${port}/createPerformanceProfile -d @"${RESOURCE_OPTIMIZATION_JSON}"
fi

echo | tee -a ${LOG}

kruize_stats="${JMETER_LOG_DIR}/jmeter_kruize.stats"
kruize_log="${JMETER_LOG_DIR}/jmeter_kruize.log"
	
host=${SERVER_IP_ADDR}

get_kruize_pod_log ${LOG_DIR}/kruize_pod.log

# sleep for sometime before starting the experiments to capture initial resource usage of kruize
sleep 200

JMETER_LOG="${LOG_DIR}/jmeter.log"
# Run the jmeter load
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo ""
	echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
	jmx_file="jmx/kruize_remote_monitoring_stress_openshift.jmx"
	if [[ "${api_version}" == "v1" ]]; then
	  jmx_file="jmx/kruize_remote_monitoring_stress_openshift_v1.jmx"
	fi
	if [ "${exp_type}" == "namespace" ]; then
		jmx_file="jmx/kruize_ns_remote_monitoring_stress_openshift.jmx"
		if [[ "${api_version}" == "v1" ]]; then
	    jmx_file="jmx/kruize_ns_remote_monitoring_stress_openshift_v1.jmx"
	  fi
	fi

	echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}" | tee -a ${LOG}
	jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jport="" -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}

else
	echo ""
	jmx_file="jmx/kruize_remote_monitoring_stress.jmx"
	if [[ "${api_version}" == "v1" ]]; then
	  jmx_file="jmx/kruize_remote_monitoring_stress_v1.jmx"
	fi
	if [ "${exp_type}" == "namespace" ]; then
		jmx_file="jmx/kruize_ns_remote_monitoring_stress.jmx"
		if [[ "${api_version}" == "v1" ]]; then
	    jmx_file="jmx/kruize_ns_remote_monitoring_stress_v1.jmx"
	  fi
	fi
	echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
	echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}" | tee -a ${LOG}
	#exec jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}
	cmd="jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop}"
	${cmd} > ${JMETER_LOG}

fi

check_jmeter_results "${kruize_log}" "${kruize_stats}"
