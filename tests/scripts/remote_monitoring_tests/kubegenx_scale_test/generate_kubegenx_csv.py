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
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests
import urllib3


MAX_RETRIES = 5
RETRY_BACKOFF = [5, 10, 30, 60, 120]

CSV_COLUMNS = [
    "start_timestamp",
    "end_timestamp",
    "container_name",
    "image_name",
    "namespace",
    "k8_object_name",
    "k8_object_type",
    "cluster_name",
    "cpu_request_container_avg",
    "cpu_request_container_sum",
    "cpu_limit_container_avg",
    "cpu_limit_container_sum",
    "cpu_usage_container_avg",
    "cpu_usage_container_min",
    "cpu_usage_container_max",
    "cpu_usage_container_sum",
    "cpu_throttle_container_avg",
    "cpu_throttle_container_max",
    "cpu_throttle_container_sum",
    "memory_request_container_avg",
    "memory_request_container_sum",
    "memory_limit_container_avg",
    "memory_limit_container_sum",
    "memory_usage_container_avg",
    "memory_usage_container_min",
    "memory_usage_container_max",
    "memory_usage_container_sum",
    "memory_rss_usage_container_avg",
    "memory_rss_usage_container_min",
    "memory_rss_usage_container_max",
    "memory_rss_usage_container_sum",
]


def normalize_timestamp(ts):
    if ts.endswith("Z") and "." not in ts:
        return ts[:-1] + ".000Z"
    return ts


def fetch_with_retry(url, verify=False, timeout=60):
    for attempt in range(MAX_RETRIES):
        try:
            resp = requests.get(url, verify=verify, timeout=timeout)
            resp.raise_for_status()
            return resp
        except (requests.exceptions.ConnectionError, requests.exceptions.Timeout) as e:
            wait = RETRY_BACKOFF[min(attempt, len(RETRY_BACKOFF) - 1)]
            if attempt < MAX_RETRIES - 1:
                print(f"\n    Retry {attempt + 1}/{MAX_RETRIES} after {wait}s: {e}", flush=True)
                time.sleep(wait)
            else:
                raise


def fetch_all_pages(url, verify=False, timeout=60, page_size=0):
    all_items = []
    page = 1
    total_pages = 1

    while page <= total_pages:
        sep = "&" if "?" in url else "?"
        paged_url = f"{url}{sep}page={page}"
        if page_size > 0:
            paged_url += f"&pageSize={page_size}"
        resp = fetch_with_retry(paged_url, verify=verify, timeout=timeout)
        data = resp.json()

        items = data.get("items", data.get("workloads", []))
        all_items.extend(items)
        total_pages = data.get("pages", 1)
        page += 1

    return all_items, data


def fetch_namespaces(base_url, dataset, session_id, verify=False, page_size=0):
    url = f"{base_url}/api/v1/datasets/{dataset}/sessions/{session_id}/namespaces"
    items, _ = fetch_all_pages(url, verify=verify, page_size=page_size)
    return items


def fetch_workloads(base_url, dataset, session_id, namespace, verify=False, page_size=0):
    url = f"{base_url}/api/v1/datasets/{dataset}/sessions/{session_id}/namespaces/{namespace}/workloads"
    items, _ = fetch_all_pages(url, verify=verify, page_size=page_size)
    return items


def fetch_results(base_url, dataset, session_id, namespace, workload, verify=False, page_size=0):
    url = f"{base_url}/api/v1/datasets/{dataset}/sessions/{session_id}/results?namespace={namespace}&workload={workload}"
    items, meta = fetch_all_pages(url, verify=verify, page_size=page_size)
    return items, meta


def get_metric_value(metrics, metric_name, agg_key, default=0):
    metric = metrics.get(metric_name, {})
    return metric.get(agg_key, default)


def result_to_csv_row(result_item, ns_item, workload):
    metrics = result_item.get("metrics", {})
    cluster_id = workload.get("clusterId", ns_item.get("clusterId", "cluster-one"))
    container = result_item.get("container", "app")

    return {
        "start_timestamp": normalize_timestamp(result_item["start"]),
        "end_timestamp": normalize_timestamp(result_item["end"]),
        "container_name": container,
        "image_name": f"kubegenx/{workload['name']}:latest",
        "namespace": ns_item["namespace"],
        "k8_object_name": workload["name"],
        "k8_object_type": workload["kind"].lower(),
        "cluster_name": cluster_id,
        "cpu_request_container_avg": get_metric_value(metrics, "cpuRequest", "avg"),
        "cpu_request_container_sum": get_metric_value(metrics, "cpuRequest", "sum"),
        "cpu_limit_container_avg": get_metric_value(metrics, "cpuLimit", "avg"),
        "cpu_limit_container_sum": get_metric_value(metrics, "cpuLimit", "sum"),
        "cpu_usage_container_avg": get_metric_value(metrics, "cpuUsage", "avg"),
        "cpu_usage_container_min": get_metric_value(metrics, "cpuUsage", "min"),
        "cpu_usage_container_max": get_metric_value(metrics, "cpuUsage", "max"),
        "cpu_usage_container_sum": get_metric_value(metrics, "cpuUsage", "sum"),
        "cpu_throttle_container_avg": get_metric_value(metrics, "cpuThrottle", "avg"),
        "cpu_throttle_container_max": get_metric_value(metrics, "cpuThrottle", "max"),
        "cpu_throttle_container_sum": get_metric_value(metrics, "cpuThrottle", "sum"),
        "memory_request_container_avg": get_metric_value(metrics, "memoryRequest", "avg"),
        "memory_request_container_sum": get_metric_value(metrics, "memoryRequest", "sum"),
        "memory_limit_container_avg": get_metric_value(metrics, "memoryLimit", "avg"),
        "memory_limit_container_sum": get_metric_value(metrics, "memoryLimit", "sum"),
        "memory_usage_container_avg": get_metric_value(metrics, "memoryUsage", "avg"),
        "memory_usage_container_min": get_metric_value(metrics, "memoryUsage", "min"),
        "memory_usage_container_max": get_metric_value(metrics, "memoryUsage", "max"),
        "memory_usage_container_sum": get_metric_value(metrics, "memoryUsage", "sum"),
        "memory_rss_usage_container_avg": get_metric_value(metrics, "memoryRSS", "avg"),
        "memory_rss_usage_container_min": get_metric_value(metrics, "memoryRSS", "min"),
        "memory_rss_usage_container_max": get_metric_value(metrics, "memoryRSS", "max"),
        "memory_rss_usage_container_sum": get_metric_value(metrics, "memoryRSS", "sum"),
    }


def main():
    parser = argparse.ArgumentParser(
        description="Fetch data from KubeGenX APIs and generate CSVs for Kruize/ROS-OCP testing"
    )
    parser.add_argument("--url", required=True, help="KubeGenX base URL")
    parser.add_argument("--dataset", required=True, help="Dataset name")
    parser.add_argument("--session-id", required=True, help="Session ID")
    parser.add_argument("--output-dir", required=True, help="Output directory for CSVs")
    parser.add_argument("--insecure", action="store_true", help="Skip TLS verification")
    parser.add_argument("--limit-namespaces", type=int, default=0, help="Limit namespaces (0 = all)")
    parser.add_argument("--limit-workloads", type=int, default=0, help="Limit workloads per namespace (0 = all)")
    parser.add_argument("--page-size", type=int, default=500,
                        help="API page size for fetching results (default: 500, 0 = API default)")
    parser.add_argument("--parallel", type=int, default=10,
                        help="Number of parallel workers for fetching workloads (default: 10, 1 = serial)")
    args = parser.parse_args()

    if args.insecure:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

    verify = not args.insecure

    per_workload_dir = os.path.join(args.output_dir, "per_workload")
    os.makedirs(per_workload_dir, exist_ok=True)

    page_size = args.page_size

    print(f"Fetching namespaces from {args.url} (pageSize={page_size})...", flush=True)
    ns_items = fetch_namespaces(args.url, args.dataset, args.session_id, verify=verify, page_size=page_size)
    namespaces = list({item["namespace"]: item for item in ns_items}.values())
    print(f"Found {len(namespaces)} namespaces", flush=True)

    if args.limit_namespaces > 0:
        namespaces = namespaces[:args.limit_namespaces]
        print(f"Limited to {len(namespaces)} namespaces", flush=True)

    total_workloads = 0
    total_intervals = 0
    skipped_existing = 0
    max_intervals_per_workload = 0
    start_time = time.time()

    all_tasks = []
    for ns_item in namespaces:
        ns = ns_item["namespace"]
        print(f"\nNamespace: {ns}", flush=True)
        workloads = fetch_workloads(args.url, args.dataset, args.session_id, ns, verify=verify, page_size=page_size)
        print(f"  {len(workloads)} workloads", flush=True)

        if args.limit_workloads > 0:
            workloads = workloads[:args.limit_workloads]

        for wl in workloads:
            all_tasks.append((ns_item, wl))

    print(f"\nTotal workloads to process: {len(all_tasks)} (parallel={args.parallel})", flush=True)

    progress_lock = threading.Lock()
    completed_count = [0]

    def fetch_one_workload(task_tuple):
        ns_item, wl = task_tuple
        ns = ns_item["namespace"]
        csv_filename = f"{ns}_{wl['name']}.csv"
        csv_path = os.path.join(per_workload_dir, csv_filename)

        if os.path.exists(csv_path) and os.path.getsize(csv_path) > 0:
            with progress_lock:
                completed_count[0] += 1
                idx = completed_count[0]
            print(f"  [{idx}/{len(all_tasks)}] {ns}/{wl['name']} (already exists, skipped)", flush=True)
            return {"skipped": True, "csv_filename": csv_filename}

        result_items, _ = fetch_results(
            args.url, args.dataset, args.session_id, ns, wl["name"], verify=verify, page_size=page_size
        )

        rows = [result_to_csv_row(item, ns_item, wl) for item in result_items]

        with progress_lock:
            completed_count[0] += 1
            idx = completed_count[0]

        if not rows:
            print(f"  [{idx}/{len(all_tasks)}] {ns}/{wl['name']} 0 results (skipped)", flush=True)
            return None

        with open(csv_path, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=CSV_COLUMNS)
            writer.writeheader()
            writer.writerows(rows)

        print(f"  [{idx}/{len(all_tasks)}] {ns}/{wl['name']} {len(rows)} intervals -> {csv_filename}", flush=True)
        return {"skipped": False, "rows": rows, "csv_filename": csv_filename}

    with ThreadPoolExecutor(max_workers=args.parallel) as executor:
        futures = {executor.submit(fetch_one_workload, task): task for task in all_tasks}
        for future in as_completed(futures):
            try:
                result = future.result()
            except Exception as e:
                task = futures[future]
                print(f"  ERROR fetching {task[0]['namespace']}/{task[1]['name']}: {e}", flush=True)
                continue

            if result is None:
                continue

            total_workloads += 1
            if result["skipped"]:
                skipped_existing += 1
            else:
                total_intervals += len(result["rows"])
                if len(result["rows"]) > max_intervals_per_workload:
                    max_intervals_per_workload = len(result["rows"])

    metadata = {
        "total_workloads": total_workloads,
        "total_intervals": total_intervals,
        "max_intervals_per_workload": max_intervals_per_workload,
        "namespaces": [n["namespace"] for n in namespaces],
        "csv_format": "kruize_compatible",
        "memory_unit": "bytes",
        "cpu_unit": "cores",
        "source": {
            "url": args.url,
            "dataset": args.dataset,
            "session_id": args.session_id,
        },
    }
    metadata_path = os.path.join(args.output_dir, "metadata.json")
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    elapsed = time.time() - start_time
    hours, rem = divmod(elapsed, 3600)
    minutes, seconds = divmod(rem, 60)
    print(f"\nDone! {total_workloads} workloads, {total_intervals} total intervals")
    if skipped_existing > 0:
        print(f"Resumed: {skipped_existing} workloads already had CSVs on disk")
    print(f"Per-workload CSVs: {per_workload_dir}/")
    print(f"Metadata: {metadata_path}")
    print(f"Time elapsed: {int(hours):02d}:{int(minutes):02d}:{seconds:05.2f}")


if __name__ == "__main__":
    main()
