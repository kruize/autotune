import argparse
from datetime import datetime, timedelta
import subprocess
import requests
import csv
import sched
import time
import sys, getopt
import threading
import os
import csv
from datetime import datetime, timedelta
from collections import defaultdict

def get_pod_info(start_time, end_time):
    # Query Prometheus for pod information within the specified time range
    # Update the labels in this query to avoid or include specific namespaces
    pods_query = f"""
    avg_over_time(kube_pod_container_info{{}}[{step}])
    """
    response = run_prometheus_query(pods_query, start_time, end_time)
    result = response['data']['result']
    processed_data = []

    for item in result:
        metric = item['metric']
        timestamp, value = item['value']
        timestamp = datetime.fromtimestamp(float(timestamp))
        data_entry = {
                'timestamp': timestamp.isoformat(),
                'namespace': metric.get('namespace', 'unknown_namespace'),
                'pod': metric.get('pod', 'unknown_pod'),
                'container': metric.get('container', 'unknown_container'),
                'value': float(value)
                }
        processed_data.append(data_entry)

    return processed_data

def get_pod_owners(container,pod,namespace):
    owners_query = f"""
    (max_over_time(kube_pod_container_info{{container='{container}', container!='POD'}}[{step}])) * on(pod, namespace) group_left(owner_kind, owner_name) max by(pod, namespace, owner_kind, owner_name) (max_over_time(kube_pod_owner{{container='{container}', container!='POD', pod='{pod}', namespace='{namespace}'}}[{step}]))
    """
    result = run_prometheus_query(owners_query, start_time, end_time)

    for row in result.get('data', {}).get('result', []):
        if row['metric'].get('pod') == pod:
            return {
                'owner_kind': row['metric'].get('owner_kind', ''),
                'owner_name': row['metric'].get('owner_name', '')
            }
    return {
                'owner_kind': '',
                'owner_name': ''
            }

def get_pod_workload(container,pod,namespace):
    # Query Prometheus for pod workload details
    workloads_query = f"""
    (max_over_time(kube_pod_container_info{{container='{container}', container!='POD', namespace='{namespace}'}}[{step}])) * on(pod, namespace) group_left(workload, workload_type) max by(pod, namespace, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{{pod='{pod}'}}[{step}]))
    """
    result = run_prometheus_query(workloads_query, start_time, end_time)
    for row in result.get('data', {}).get('result', []):
        if row['metric'].get('pod') == pod:
            return {
                'workload_type': row['metric'].get('workload_type', ''),
                'workload': row['metric'].get('workload', '')
            }
    return {
                'workload_type': '',
                'workload': ''
            }

def get_metric_usage(container,pod,namespace,start_time, end_time, step):
    queries_map = {
            "cpu_request_container_avg": f"avg by(container, pod, namespace, node) ((kube_pod_container_resource_requests{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"cpu\", unit=\"core\"}}))",
            "cpu_request_container_sum": f"sum by(container, pod, namespace, node) ((kube_pod_container_resource_requests{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"cpu\", unit=\"core\"}}))",
            "cpu_limit_container_avg": f"avg by(container, pod, namespace, node) ((kube_pod_container_resource_limits{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"cpu\", unit=\"core\"}}))",
            "cpu_limit_container_sum": f"sum by(container, pod, namespace, node) ((kube_pod_container_resource_limits{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"cpu\", unit=\"core\"}}))",
            "cpu_usage_container_avg": f"avg by(container, pod, namespace, node) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "cpu_usage_container_min": f"min by(container, pod, namespace, node) (min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "cpu_usage_container_max": f"max by(container, pod, namespace, node) (max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "cpu_usage_container_sum": f"sum by(container, pod, namespace, node) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "cpu_throttle_container_avg": f"avg by(container, pod, namespace, node) (rate(container_cpu_cfs_throttled_seconds_total{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "cpu_throttle_container_max": f"max by(container, pod, namespace, node) (rate(container_cpu_cfs_throttled_seconds_total{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "cpu_throttle_container_sum": f"sum by(container, pod, namespace, node) (rate(container_cpu_cfs_throttled_seconds_total{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "cpu_throttle_container_min": f"sum by(container, pod, namespace, node) (rate(container_cpu_cfs_throttled_seconds_total{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_request_container_avg": f"avg by(container, pod, namespace, node) ((kube_pod_container_resource_requests{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"memory\", unit=\"byte\"}}))",
            "memory_request_container_sum": f"sum by(container, pod, namespace, node) ((kube_pod_container_resource_requests{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"memory\", unit=\"byte\"}}))",
            "memory_limit_container_avg": f"avg by(container, pod, namespace, node) ((kube_pod_container_resource_limits{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"memory\", unit=\"byte\"}}))",
            "memory_limit_container_sum": f"sum by(container, pod, namespace, node) ((kube_pod_container_resource_limits{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\", resource=\"memory\", unit=\"byte\"}}))",
            "memory_usage_container_avg": f"avg by(container, pod, namespace, node) (avg_over_time(container_memory_working_set_bytes{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_usage_container_min": f"min by(container, pod, namespace, node) (min_over_time(container_memory_working_set_bytes{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_usage_container_max": f"max by(container, pod, namespace, node) (max_over_time(container_memory_working_set_bytes{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_usage_container_sum": f"sum by(container, pod, namespace, node) (avg_over_time(container_memory_working_set_bytes{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_rss_usage_container_avg": f"avg by(container, pod, namespace,node) (avg_over_time(container_memory_rss{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_rss_usage_container_min": f"min by(container, pod, namespace,node) (min_over_time(container_memory_rss{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_rss_usage_container_max": f"max by(container, pod, namespace,node) (max_over_time(container_memory_rss{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
            "memory_rss_usage_container_sum": f"sum by(container, pod, namespace,node) (avg_over_time(container_memory_rss{{container=\"{container}\", container!=\"POD\", pod=\"{pod}\", namespace=\"{namespace}\"}}[{step}]))",
    }

    results = {}
    for query_name, query in queries_map.items():
        result = run_prometheus_query(query, start_time, end_time)
        results[query_name] = ""
        for row in result.get('data', {}).get('result', []):
            value = float(row['value'][1])
            results[query_name] = value
    return results


def run_prometheus_query(query, start_time, end_time):
    prometheus_url = None
    TOKEN = 'TOKEN'
    if prometheus_url is None:
        if cluster_type == "openshift":
            prometheus_url = f"https://thanos-querier-openshift-monitoring.apps.{server}/api/v1/query"
        elif cluster_type == "minikube":
            prometheus_url = f"http://{server}:9090/api/v1/query"

    if cluster_type == "openshift":
        output = subprocess.check_output(['oc', 'whoami', '--show-token'])
        TOKEN = output.decode().strip()
        # Disable the InsecureRequestWarning
        requests.packages.urllib3.disable_warnings(requests.packages.urllib3.exceptions.InsecureRequestWarning)

    headers = {'Authorization': f'Bearer {TOKEN}'}

    params = {
        "query": query,
        "start": start_time,
        "end": end_time,
        "step": step
    }
    response = requests.get(prometheus_url, headers=headers, params=params, verify=False)
    return response.json()

# Function to run the queries and gather data
def gather_data(output_file, interval_minutes):
    current_time = start_time
    #datetime.now()
    #end_time = current_time + timedelta(hours=1) #days=total_duration_days)

    # Open a CSV file to append data
    with open(output_file, 'w', newline='') as csvfile, open("promql2csv.log", 'w') as log_file:
#  with open(output_file, 'w', newline='') as csvfile:
        fieldnames = [
            "interval_start", "interval_end", "cluster_name", "container_name","pod", "owner_name", "owner_kind", "workload", "workload_type", "namespace", "image_name", "node", "cpu_request_container_avg", "cpu_request_container_sum", "cpu_limit_container_avg", "cpu_limit_container_sum", "cpu_usage_container_avg", "cpu_usage_container_min", "cpu_usage_container_max", "cpu_usage_container_sum", "cpu_throttle_container_avg", "cpu_throttle_container_max", "cpu_throttle_container_sum", "cpu_throttle_container_min", "memory_request_container_avg", "memory_request_container_sum", "memory_limit_container_avg", "memory_limit_container_sum", "memory_usage_container_avg", "memory_usage_container_min", "memory_usage_container_max", "memory_usage_container_sum", "memory_rss_usage_container_avg", "memory_rss_usage_container_min", "memory_rss_usage_container_max", "memory_rss_usage_container_sum"
        ]
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        log_file.write("Collecting the metrics for...\n")

        while current_time < end_time:
            try:
                print(f"Querying data from {current_time} to {current_time + timedelta(minutes=interval_minutes)}")

                for pod_info in get_pod_info(current_time, current_time + timedelta(minutes=interval_minutes)):
                    log_file.write(f"Timestamp: {current_time}, Pod: {pod_info['pod']} , Namespace: {pod_info['namespace']} , Container: {pod_info['container']}\n")
                    log_file.flush()

                    interval_end_time = current_time + timedelta(minutes=interval_minutes)
                    pod_name = pod_info['pod']
                    container_name = pod_info['container']
                    namespace = pod_info['namespace']

                    if not pod_name or not container_name or not namespace:
                        continue

                    owners = get_pod_owners(container_name,pod_name,namespace)
                    owner_kind = owners.get('owner_kind', 'N/A')
                    owner_name = owners.get('owner_name', 'N/A')

                    workloads = get_pod_workload(container_name,pod_name,namespace)
                    workload_type = workloads.get('workload_type', '')
                    workload = workloads.get('workload', '')

                    metrics_data = get_metric_usage(container_name, pod_name, namespace, current_time, current_time + timedelta(minutes=interval_minutes), step)

                    aligned_data = []
                    aligned_data.append({
                            'interval_start': current_time.strftime('%Y-%m-%dT%H:%M:%S.%f'),
                            'interval_end': interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%f'),
                            'cluster_name': cluster_name,
                            'container_name': container_name,
                            'pod': pod_name,
                            'namespace': namespace,
                            'owner_kind': owner_kind,
                            'owner_name': owner_name,
                            'workload': workload,
                            'workload_type': workload_type,
                            'cpu_request_container_avg': metrics_data["cpu_request_container_avg"],
                            'cpu_request_container_sum': metrics_data["cpu_request_container_sum"],
                            'cpu_limit_container_avg': metrics_data["cpu_limit_container_avg"],
                            'cpu_limit_container_sum': metrics_data["cpu_limit_container_sum"],
                            'cpu_usage_container_avg': metrics_data["cpu_usage_container_avg"],
                            'cpu_usage_container_min': metrics_data["cpu_usage_container_min"],
                            'cpu_usage_container_max': metrics_data["cpu_usage_container_max"],
                            'cpu_usage_container_sum': metrics_data["cpu_usage_container_sum"],
                            'cpu_throttle_container_avg': metrics_data["cpu_throttle_container_avg"],
                            'cpu_throttle_container_max': metrics_data["cpu_throttle_container_max"],
                            'cpu_throttle_container_sum': metrics_data["cpu_throttle_container_sum"],
                            'cpu_throttle_container_min': metrics_data["cpu_throttle_container_min"],
                            'memory_request_container_avg': metrics_data["memory_request_container_avg"],
                            'memory_request_container_sum': metrics_data["memory_request_container_sum"],
                            'memory_limit_container_avg': metrics_data["memory_limit_container_avg"],
                            'memory_limit_container_sum': metrics_data["memory_limit_container_sum"],
                            'memory_usage_container_avg': metrics_data["memory_usage_container_avg"],
                            'memory_usage_container_min': metrics_data["memory_usage_container_min"],
                            'memory_usage_container_max': metrics_data["memory_usage_container_max"],
                            'memory_usage_container_sum': metrics_data["memory_usage_container_sum"],
                            'memory_rss_usage_container_avg': metrics_data["memory_rss_usage_container_avg"],
                            'memory_rss_usage_container_min': metrics_data["memory_rss_usage_container_min"],
                            'memory_rss_usage_container_max': metrics_data["memory_rss_usage_container_max"],
                            'memory_rss_usage_container_sum': metrics_data["memory_rss_usage_container_sum"],
                        })
                    writer.writerows(aligned_data)
                    csvfile.flush()
            except Exception as e:
                print(f"Error during processing: {e}")

            current_time += timedelta(minutes=interval_minutes)

def convert_step_to_minutes(step):
    unit = step[-1]
    value = int(step[:-1])

    if unit == 'm':  # Minutes
        interval = value
    elif unit == 'h':  # Hours
        interval = value * 60
    elif unit == 'd':  # Days
        interval = value * 24 * 60
    else:
        raise ValueError("Invalid step unit. Use 'm' for minutes, 'h' for hours, or 'd' for days")
    return interval

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Kubernetes Metrics Collector")
    parser.add_argument("--clustertype", type=str, default="openshift")
    parser.add_argument("--server", type=str)
    parser.add_argument("--start", type=str, default=(datetime.now() - timedelta(days=15)).strftime("%Y-%m-%d %H:%M:%S"), help="Start time in YYYY-MM-DD HH:MM:%S format")
    parser.add_argument("--end", type=str, default=datetime.now().strftime("%Y-%m-%d %H:%M:%S"), help="End time in YYYY-MM-DD HH:MM:%S format")
    parser.add_argument("--step", type=str, default="15m")
    args = parser.parse_args()

    start_time = datetime.strptime(args.start, "%Y-%m-%d %H:%M:%S")
    end_time = datetime.strptime(args.end, "%Y-%m-%d %H:%M:%S")
    step = args.step
    server = args.server
    cluster_name = "default"
    cluster_type = args.clustertype
    interval_minutes = convert_step_to_minutes(step)

    gather_data(output_file="pod_metrics.csv", interval_minutes=interval_minutes)

