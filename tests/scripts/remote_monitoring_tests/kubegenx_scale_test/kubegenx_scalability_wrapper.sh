#!/bin/bash
#
# Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.
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
### Per-client wrapper: posts all data to Kruize and collects metrics ###

CURRENT_DIR="$(dirname "$(realpath "$0")")"

# Parse command-line arguments
ip=""
port="0"
data_dir=""
client_id=0
total_clients=1
chunk_size=24
limit_experiments=0
api_version="legacy"
prometheus_server=""
outputdir="results"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ip) ip="$2"; shift 2 ;;
        --port) port="$2"; shift 2 ;;
        --data-dir) data_dir="$2"; shift 2 ;;
        --client-id) client_id="$2"; shift 2 ;;
        --total-clients) total_clients="$2"; shift 2 ;;
        --chunk-size) chunk_size="$2"; shift 2 ;;
        --limit-experiments) limit_experiments="$2"; shift 2 ;;
        --api-version) api_version="$2"; shift 2 ;;
        --prometheusserver) prometheus_server="$2"; shift 2 ;;
        --outputdir) outputdir="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

if [[ -z "$ip" || -z "$data_dir" ]]; then
    echo "Missing required arguments: --ip and --data-dir"
    exit 1
fi

echo "kubegenx_scalability_wrapper.sh :: api_version = ${api_version}"
if [ -z "${api_version}" ]; then
    api_version="legacy"
fi

mkdir -p "${outputdir}/results"

limit_flag=""
if [ ${limit_experiments} -gt 0 ]; then
    limit_flag="--limit-experiments ${limit_experiments}"
fi

full_command="python3 -u ${CURRENT_DIR}/post_kubegenx_data.py --ip ${ip} --port ${port} --data-dir ${data_dir} --client-id ${client_id} --total-clients ${total_clients} --chunk-size ${chunk_size} ${limit_flag} --api-version ${api_version}"

echo "Executing: ${full_command}"
eval "${full_command}"

wait

echo "Collecting kruize metrics"
client_thread=$((client_id + 1))
metrics_command="python3 ${CURRENT_DIR}/../../../../scripts/kruize_metrics.py -c openshift -s ${prometheus_server} -t 360m -e ${outputdir}/results -r kubegenxMetrics-${client_thread}.csv --api-version ${api_version}"
eval "${metrics_command}"
