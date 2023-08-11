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

###############################  utilities  #################################

function check_running() {

  check_pod=$1
  check_pod_ns=$2
  ignore_pod=$3
  kubectl_cmd="kubectl -n ${check_pod_ns}"

  echo "Info: Waiting for ${check_pod} to come up....."
  err_wait=0
  counter=0
  while true; do
    sleep 2
    #		Check if ignore pod is sent
    if [[ ! -z "${ignore_pod}" ]]; then
      ${kubectl_cmd} get pods | grep ${check_pod} | grep -v ${ignore_pod}
      pod_stat=$(${kubectl_cmd} get pods | grep ${check_pod} | grep -v ${ignore_pod} | awk '{ print $3 }')
    else
      ${kubectl_cmd} get pods | grep ${check_pod}
      pod_stat=$(${kubectl_cmd} get pods | grep ${check_pod} | awk '{ print $3 }')
    fi

    case "${pod_stat}" in
    "Running")
      echo "Info: ${check_pod} deploy succeeded: ${pod_stat}"
      err=0
      break
      ;;
    "Error")
      # On Error, wait for 10 seconds before exiting.
      err_wait=$((err_wait + 1))
      if [ ${err_wait} -gt 5 ]; then
        echo "Error: ${check_pod} deploy failed: ${pod_stat}"
        err=-1
        break
      fi
      ;;
    *)
      sleep 2
      if [ $counter == 200 ]; then
        ${kubectl_cmd} describe pod ${check_pod}
        echo "ERROR: Prometheus Pods failed to come up!"
        exit -1
      fi
      ((counter++))
      ;;
    esac
  done

  ${kubectl_cmd} get pods | grep ${check_pod}
  echo
}

function check_kustomize() {
  kubectl_tool=$(which kubectl)
  check_err "Error: Please install the kubectl tool"
  # Check to see if kubectl supports kustomize
  kubectl --help | grep "kustomize" >/dev/null
  check_err "Error: Please install a newer version of kubectl tool that supports the kustomize option (>=v1.12)"
}

# Check error code from last command, exit on error
check_err() {
  err=$?
  if [ ${err} -ne 0 ]; then
    echo "$*"
    exit -1
  fi
}

# Deploy kruize in remote monitoring mode with the specified docker image
kruize_crc_start() {
  kubectl_cmd="kubectl -n ${autotune_ns}"
  CRC_MANIFEST_FILE_OLD="${CRC_DIR}/${cluster_type}/kruize_${cluster_type}.yaml"

  cp ${CRC_MANIFEST_FILE} ${CRC_MANIFEST_FILE_OLD}
  awk -v image_name=${AUTOTUNE_DOCKER_IMAGE} -v ui_image_name=${KRUIZE_UI_DOCKER_IMAGE} '{
			if ($2=="name:") {
				prev=$3;
				print
			} else if ($1=="image:" && prev=="kruize") {
				$2=image_name;
				printf"          %s %s\n", $1, $2;
			} else if ($1=="image:" && prev=="kruize-ui-nginx-container") {
        $2=ui_image_name;
        printf"      %s %s\n", $1, $2;
      } else { print }
		 }' ${CRC_MANIFEST_FILE_OLD} >${CRC_MANIFEST_FILE}
  ${kubectl_cmd} apply -f ${CRC_MANIFEST_FILE}
  check_running kruize ${autotune_ns} kruize-ui
  if [ "${err}" != "0" ]; then
    # Indicate deploy failed on error
    exit 1
  fi
  cp ${CRC_MANIFEST_FILE_OLD} ${CRC_MANIFEST_FILE}
  rm ${CRC_MANIFEST_FILE_OLD}
}
