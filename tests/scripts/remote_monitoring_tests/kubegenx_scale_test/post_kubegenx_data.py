"""
Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import argparse
import csv
import json
import os
import time
from pathlib import Path

import requests

HEADERS = {"Content-Type": "application/json"}
TIMEOUT = (60, 120)

MEBIBYTE = 1048576

METRIC_MAP = {
    "cpuRequest": {
        "cols": {"avg": "cpu_request_container_avg", "sum": "cpu_request_container_sum"},
        "format": "cores",
    },
    "cpuLimit": {
        "cols": {"avg": "cpu_limit_container_avg", "sum": "cpu_limit_container_sum"},
        "format": "cores",
    },
    "cpuUsage": {
        "cols": {
            "avg": "cpu_usage_container_avg",
            "min": "cpu_usage_container_min",
            "max": "cpu_usage_container_max",
            "sum": "cpu_usage_container_sum",
        },
        "format": "cores",
    },
    "cpuThrottle": {
        "cols": {
            "avg": "cpu_throttle_container_avg",
            "max": "cpu_throttle_container_max",
            "sum": "cpu_throttle_container_sum",
        },
        "format": "cores",
    },
    "memoryRequest": {
        "cols": {"avg": "memory_request_container_avg", "sum": "memory_request_container_sum"},
        "format": "MiB",
        "divisor": MEBIBYTE,
    },
    "memoryLimit": {
        "cols": {"avg": "memory_limit_container_avg", "sum": "memory_limit_container_sum"},
        "format": "MiB",
        "divisor": MEBIBYTE,
    },
    "memoryUsage": {
        "cols": {
            "avg": "memory_usage_container_avg",
            "min": "memory_usage_container_min",
            "max": "memory_usage_container_max",
            "sum": "memory_usage_container_sum",
        },
        "format": "MiB",
        "divisor": MEBIBYTE,
    },
    "memoryRSS": {
        "cols": {
            "avg": "memory_rss_usage_container_avg",
            "min": "memory_rss_usage_container_min",
            "max": "memory_rss_usage_container_max",
            "sum": "memory_rss_usage_container_sum",
        },
        "format": "MiB",
        "divisor": MEBIBYTE,
    },
}


def row_to_metrics(row):
    metrics = []
    for metric_name, spec in METRIC_MAP.items():
        divisor = spec.get("divisor", 1)
        agg = {"format": spec["format"]}
        has_data = False
        for agg_key, col_name in spec["cols"].items():
            val = row.get(col_name, "")
            if val != "" and val is not None:
                agg[agg_key] = round(float(val) / divisor, 6)
                has_data = True
        if has_data:
            metrics.append({"name": metric_name, "results": {"aggregation_info": agg}})
    return metrics


def csv_to_create_exp(first_row):
    experiment_name = f"{first_row['namespace']}_{first_row['k8_object_name']}"
    return {
        "version": "v2.0",
        "experiment_name": experiment_name,
        "cluster_name": first_row["cluster_name"],
        "performance_profile": "resource-optimization-openshift",
        "mode": "monitor",
        "target_cluster": "remote",
        "kubernetes_objects": [
            {
                "type": first_row["k8_object_type"],
                "name": first_row["k8_object_name"],
                "namespace": first_row["namespace"],
                "containers": [
                    {
                        "container_image_name": first_row["image_name"],
                        "container_name": first_row["container_name"],
                    }
                ],
            }
        ],
        "trial_settings": {"measurement_duration": "15min"},
        "recommendation_settings": {"threshold": "0.1"},
    }


def csv_row_to_result(row, experiment_name):
    metrics = row_to_metrics(row)
    return {
        "version": "v2.0",
        "experiment_name": experiment_name,
        "interval_start_time": row["start_timestamp"],
        "interval_end_time": row["end_timestamp"],
        "kubernetes_objects": [
            {
                "type": row["k8_object_type"],
                "name": row["k8_object_name"],
                "namespace": row["namespace"],
                "containers": [
                    {
                        "container_image_name": row["image_name"],
                        "container_name": row["container_name"],
                        "metrics": metrics,
                    }
                ],
            }
        ],
    }


def build_urls(ip, port, use_new_api):
    if port and int(port) != 0:
        base = f"http://{ip}:{port}"
    else:
        base = f"http://{ip}"

    urls = {
        "createExp": f"{base}/createExperiment",
        "updateResults": f"{base}/updateResults",
        "createProfile": f"{base}/createPerformanceProfile",
    }
    if use_new_api:
        urls["updateRecommendations"] = f"{base}/kruize/api/v1/recommendations"
    else:
        urls["updateRecommendations"] = f"{base}/updateRecommendations"

    return urls


def create_performance_profile(urls):
    script_dir = Path(__file__).resolve().parent
    profile_dir = script_dir.parents[3] / "manifests" / "autotune" / "performance-profiles"
    profile_path = profile_dir / "resource_optimization_openshift.json"
    with open(profile_path, "r", encoding="utf-8") as f:
        profile_data = json.load(f)

    payload = json.dumps(profile_data)
    print(f"calling POST API {urls['createProfile']}")
    resp = requests.post(urls["createProfile"], data=payload, headers=HEADERS, timeout=TIMEOUT)
    if resp.status_code == 201:
        print("Performance profile created")
    else:
        print(f"Profile creation returned {resp.status_code}: {resp.text}")


def create_experiment(urls, create_exp_json):
    payload = json.dumps([create_exp_json])
    try:
        resp = requests.post(urls["createExp"], data=payload, headers=HEADERS, timeout=TIMEOUT)
        if resp.status_code not in (201, 409):
            print(f"  createExperiment failed {resp.status_code}: {resp.text}")
            return False
        return True
    except requests.exceptions.RequestException as e:
        print(f"  createExperiment error: {e}")
        return False


def post_results_bulk(urls, results_data, experiment_name):
    payload = json.dumps(results_data)
    try:
        resp = requests.post(urls["updateResults"], data=payload, headers=HEADERS, timeout=TIMEOUT)
        if resp.status_code != 201:
            print(f"  updateResults failed {resp.status_code}: {resp.text}")
    except requests.exceptions.Timeout:
        print(f"  updateResults timeout for {experiment_name}")
    except requests.exceptions.RequestException as e:
        print(f"  updateResults error: {e}")


def update_recommendations(urls, experiment_name, end_time):
    try:
        reco_url = f"{urls['updateRecommendations']}?experiment_name={experiment_name}&interval_end_time={end_time}"
        resp = requests.post(reco_url, data={}, headers=HEADERS, timeout=TIMEOUT)
        if resp.status_code != 201:
            print(f"  updateRecommendations failed {resp.status_code}: {resp.text}")
    except requests.exceptions.Timeout:
        print(f"  updateRecommendations timeout for {experiment_name}")
    except requests.exceptions.RequestException as e:
        print(f"  updateRecommendations error: {e}")


def get_max_end_time(results_data):
    max_ts = ""
    for r in results_data:
        ts = r.get("interval_end_time", "")
        if ts > max_ts:
            max_ts = ts
    return max_ts


def detect_data_format(data_dir):
    """Detect whether data_dir contains CSVs (per_workload/) or JSONs (create_experiments/)."""
    csv_dir = os.path.join(data_dir, "per_workload")
    json_dir = os.path.join(data_dir, "create_experiments")
    if os.path.isdir(csv_dir) and any(f.endswith(".csv") for f in os.listdir(csv_dir)):
        return "csv"
    if os.path.isdir(json_dir) and any(f.endswith(".json") for f in os.listdir(json_dir)):
        return "json"
    raise FileNotFoundError(f"No per_workload/*.csv or create_experiments/*.json found in {data_dir}")


def list_csv_experiments(data_dir):
    csv_dir = os.path.join(data_dir, "per_workload")
    experiments = []
    for fname in sorted(os.listdir(csv_dir)):
        if fname.endswith(".csv"):
            experiments.append(fname[:-4])
    return experiments


def list_json_experiments(data_dir):
    exp_dir = os.path.join(data_dir, "create_experiments")
    experiments = []
    for fname in sorted(os.listdir(exp_dir)):
        if fname.endswith(".json"):
            experiments.append(fname[:-5])
    return experiments


def load_json_experiment(data_dir, exp_name):
    exp_file = os.path.join(data_dir, "create_experiments", f"{exp_name}.json")
    res_file = os.path.join(data_dir, "results", f"{exp_name}.json")

    with open(exp_file, "r", encoding="utf-8") as f:
        create_exp_json = json.load(f)

    with open(res_file, "r", encoding="utf-8") as f:
        all_results = json.load(f)

    return create_exp_json, all_results


def load_csv_experiment(data_dir, exp_name):
    csv_path = os.path.join(data_dir, "per_workload", f"{exp_name}.csv")

    with open(csv_path, "r", newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    if not rows:
        return None, []

    create_exp_json = csv_to_create_exp(rows[0])
    experiment_name = create_exp_json["experiment_name"]

    results = [csv_row_to_result(row, experiment_name) for row in rows]

    return create_exp_json, results


def main():
    parser = argparse.ArgumentParser(
        description="Post KubeGenX data to Kruize APIs (reads CSVs or JSONs, chunks internally)"
    )
    parser.add_argument("--ip", required=True, help="Kruize IP address or hostname")
    parser.add_argument("--port", type=str, default="0", help="Kruize port (0 = no port)")
    parser.add_argument("--data-dir", required=True, help="Directory with KubeGenX data (CSVs in per_workload/ or JSONs in create_experiments/)")
    parser.add_argument("--client-id", type=int, default=0, help="Client ID (for parallel execution)")
    parser.add_argument("--total-clients", type=int, default=1, help="Total number of parallel clients")
    parser.add_argument("--chunk-size", type=int, default=24, help="Results per updateResults POST, 24=6hrs of 15min data (default: 24)")
    parser.add_argument("--limit-experiments", type=int, default=0, help="Limit total experiments to post (0 = all)")
    parser.add_argument("--api-version", type=str, default="legacy", help="API version (v1/legacy)")
    args = parser.parse_args()

    use_new_api = args.api_version.lower() == "v1"
    urls = build_urls(args.ip, args.port, use_new_api)

    data_format = detect_data_format(args.data_dir)
    print(f"post_kubegenx_data :: api_version = {args.api_version}, data_format = {data_format}")
    print(f"Client {args.client_id + 1} of {args.total_clients}, chunk_size = {args.chunk_size}")

    if data_format == "csv":
        all_experiments = list_csv_experiments(args.data_dir)
    else:
        all_experiments = list_json_experiments(args.data_dir)

    if args.limit_experiments > 0:
        all_experiments = all_experiments[:args.limit_experiments]

    my_experiments = [
        exp for i, exp in enumerate(all_experiments)
        if i % args.total_clients == args.client_id
    ]

    print(f"Total experiments: {len(all_experiments)}, this client: {len(my_experiments)}")

    create_performance_profile(urls)
    if len(my_experiments) > 10:
        time.sleep(5)

    createExp_time = 0.0
    bulkDataPost_time = 0.0
    updateRec_time = 0.0
    total_results_posted = 0

    start_time = time.time()
    chunk_size = args.chunk_size

    def fmt(secs):
        h, rem = divmod(secs, 3600)
        m, s = divmod(rem, 60)
        return f"{int(h):02d}:{int(m):02d}:{s:05.2f}"

    for exp_idx, exp_name in enumerate(my_experiments, 1):
        try:
            if data_format == "csv":
                create_exp_json, all_results = load_csv_experiment(args.data_dir, exp_name)
            else:
                create_exp_json, all_results = load_json_experiment(args.data_dir, exp_name)

            if create_exp_json is None or not all_results:
                continue

            experiment_name = create_exp_json["experiment_name"]
            exp_start = time.time()

            t0 = time.time()
            create_experiment(urls, create_exp_json)
            createExp_time += time.time() - t0

            num_chunks = (len(all_results) + chunk_size - 1) // chunk_size
            for i in range(0, len(all_results), chunk_size):
                chunk = all_results[i:i + chunk_size]
                t0 = time.time()
                post_results_bulk(urls, chunk, experiment_name)
                bulkDataPost_time += time.time() - t0
                total_results_posted += len(chunk)

            max_end_time = get_max_end_time(all_results)
            if max_end_time:
                t0 = time.time()
                update_recommendations(urls, experiment_name, max_end_time)
                updateRec_time += time.time() - t0

            exp_elapsed = time.time() - exp_start
            print(f"[{exp_idx}/{len(my_experiments)}] {experiment_name}: {len(all_results)} results in {num_chunks} chunks, Time: {fmt(exp_elapsed)}")

        except requests.exceptions.Timeout:
            print(f"[{exp_idx}/{len(my_experiments)}] {exp_name}: TIMEOUT")
        except requests.exceptions.RequestException as e:
            print(f"[{exp_idx}/{len(my_experiments)}] {exp_name}: ERROR {e}")
        except Exception as e:
            print(f"[{exp_idx}/{len(my_experiments)}] {exp_name}: ERROR {e}")

    elapsed = time.time() - start_time

    print(f"\nTime elapsed: {fmt(elapsed)}")
    print(f"createExp elapsed time: {fmt(createExp_time)}")
    print(f"bulkDataPost elapsed time: {fmt(bulkDataPost_time)}")
    print(f"updateRec elapsed time: {fmt(updateRec_time)}")
    print(f"Total results posted: {total_results_posted}")


if __name__ == "__main__":
    main()
