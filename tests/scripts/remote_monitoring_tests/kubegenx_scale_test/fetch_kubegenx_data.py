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
import json
import os
import sys
import time

import requests
import urllib3

from collections import defaultdict

BYTES_TO_MIB = 1024 * 1024

VALID_EXP_TYPES = ("container", "namespace", "container_ns")

METRIC_NAMES = [
    "cpuRequest", "cpuLimit", "cpuUsage", "cpuThrottle",
    "memoryRequest", "memoryLimit", "memoryUsage", "memoryRSS"
]

NS_METRIC_DEFS = {
    "namespaceCpuRequest": {"source": "cpuRequest", "agg_keys": ["sum"], "format": "cores"},
    "namespaceCpuLimit": {"source": "cpuLimit", "agg_keys": ["sum"], "format": "cores"},
    "namespaceCpuUsage": {"source": "cpuUsage", "agg_keys": ["min", "max", "avg"], "format": "cores"},
    "namespaceCpuThrottle": {"source": "cpuThrottle", "agg_keys": ["max", "avg"], "format": "cores"},
    "namespaceMemoryRequest": {"source": "memoryRequest", "agg_keys": ["sum"], "format": "MiB"},
    "namespaceMemoryLimit": {"source": "memoryLimit", "agg_keys": ["sum"], "format": "MiB"},
    "namespaceMemoryUsage": {"source": "memoryUsage", "agg_keys": ["min", "max", "avg"], "format": "MiB"},
    "namespaceMemoryRSS": {"source": "memoryRSS", "agg_keys": ["min", "max", "avg"], "format": "MiB"},
}

METRICS_WITH_MIN_MAX = {"cpuUsage", "memoryUsage", "memoryRSS"}
METRICS_WITH_MAX_ONLY = {"cpuThrottle"}


def normalize_timestamp(ts):
    """Ensure timestamp has milliseconds: '2026-10-28T00:00:00Z' -> '2026-10-28T00:00:00.000Z'"""
    if ts.endswith("Z") and "." not in ts:
        return ts[:-1] + ".000Z"
    return ts


def fetch_all_pages(url, verify=False, timeout=60):
    """Fetch all pages from a paginated KubeGenX endpoint."""
    all_items = []
    page = 1
    total_pages = 1

    while page <= total_pages:
        sep = "&" if "?" in url else "?"
        paged_url = f"{url}{sep}page={page}"
        resp = requests.get(paged_url, verify=verify, timeout=timeout)
        resp.raise_for_status()
        data = resp.json()

        items = data.get("items", data.get("workloads", []))
        all_items.extend(items)
        total_pages = data.get("pages", 1)
        page += 1

    return all_items, data


def fetch_namespaces(base_url, dataset, session_id, verify=False):
    url = f"{base_url}/api/v1/datasets/{dataset}/sessions/{session_id}/namespaces"
    items, _ = fetch_all_pages(url, verify=verify)
    return items


def fetch_workloads(base_url, dataset, session_id, namespace, verify=False):
    url = f"{base_url}/api/v1/datasets/{dataset}/sessions/{session_id}/namespaces/{namespace}/workloads"
    items, _ = fetch_all_pages(url, verify=verify)
    return items


def fetch_results(base_url, dataset, session_id, namespace, workload, verify=False):
    url = f"{base_url}/api/v1/datasets/{dataset}/sessions/{session_id}/results?namespace={namespace}&workload={workload}"
    items, meta = fetch_all_pages(url, verify=verify)
    return items, meta


def convert_metric(kubegenx_metric, metric_name):
    """Convert a single KubeGenX metric to Kruize aggregation_info format."""
    is_memory = metric_name.startswith("memory")
    fmt = "MiB" if is_memory else "cores"
    divisor = BYTES_TO_MIB if is_memory else 1

    agg = {"format": fmt}

    if metric_name in METRICS_WITH_MIN_MAX:
        agg["min"] = round(kubegenx_metric["min"] / divisor, 6)
        agg["max"] = round(kubegenx_metric["max"] / divisor, 6)
        agg["sum"] = round(kubegenx_metric["sum"] / divisor, 6)
        agg["avg"] = round(kubegenx_metric["avg"] / divisor, 6)
    elif metric_name in METRICS_WITH_MAX_ONLY:
        agg["sum"] = round(kubegenx_metric["sum"] / divisor, 6)
        agg["max"] = round(kubegenx_metric["max"] / divisor, 6)
        agg["avg"] = round(kubegenx_metric["avg"] / divisor, 6)
    else:
        agg["sum"] = round(kubegenx_metric["sum"] / divisor, 6)
        agg["avg"] = round(kubegenx_metric["avg"] / divisor, 6)

    return {
        "name": metric_name,
        "results": {
            "aggregation_info": agg
        }
    }


def build_create_exp_json(namespace_item, workload):
    """Build a Kruize createExperiment JSON from KubeGenX workload data."""
    experiment_name = f"{namespace_item['namespace']}_{workload['name']}"
    containers = [
        {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": c
        }
        for c in workload.get("containers", ["app"])
    ]

    return {
        "version": "v2.0",
        "experiment_name": experiment_name,
        "cluster_name": workload.get("clusterId", namespace_item.get("clusterId", "cluster-one")),
        "performance_profile": "resource-optimization-openshift",
        "mode": "monitor",
        "target_cluster": "remote",
        "kubernetes_objects": [
            {
                "type": workload["kind"].lower(),
                "name": workload["name"],
                "namespace": namespace_item["namespace"],
                "containers": containers
            }
        ],
        "trial_settings": {
            "measurement_duration": "15min"
        },
        "recommendation_settings": {
            "threshold": "0.1"
        }
    }


def build_result_json(experiment_name, workload, namespace, result_item):
    """Build a Kruize updateResults JSON entry from one KubeGenX result item."""
    metrics = []
    for metric_name in METRIC_NAMES:
        if metric_name in result_item.get("metrics", {}):
            metrics.append(convert_metric(result_item["metrics"][metric_name], metric_name))

    return {
        "version": "v2.0",
        "experiment_name": experiment_name,
        "interval_start_time": normalize_timestamp(result_item["start"]),
        "interval_end_time": normalize_timestamp(result_item["end"]),
        "kubernetes_objects": [
            {
                "type": workload["kind"].lower(),
                "name": workload["name"],
                "namespace": namespace,
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": result_item.get("container", "app"),
                        "metrics": metrics
                    }
                ]
            }
        ]
    }


def build_ns_create_exp_json(namespace, cluster_name):
    experiment_name = f"ns_{namespace}"
    return {
        "version": "v2.0",
        "experiment_name": experiment_name,
        "cluster_name": cluster_name,
        "performance_profile": "resource-optimization-openshift",
        "mode": "monitor",
        "target_cluster": "remote",
        "experiment_type": "namespace",
        "kubernetes_objects": [{"namespaces": {"namespace": namespace}}],
        "trial_settings": {"measurement_duration": "15min"},
        "recommendation_settings": {"threshold": "0.1"},
    }


def aggregate_results_to_ns_metrics(result_items, num_workloads):
    """Aggregate container-level result items (same timestamp) into namespace metrics."""
    metrics = []
    for ns_metric, defn in NS_METRIC_DEFS.items():
        source = defn["source"]
        is_memory = source.startswith("memory")
        divisor = BYTES_TO_MIB if is_memory else 1
        agg = {"format": defn["format"]}
        has_data = False

        for agg_key in defn["agg_keys"]:
            total = 0.0
            found = False
            for item in result_items:
                src_metric = item.get("metrics", {}).get(source, {})
                if agg_key in src_metric:
                    total += src_metric[agg_key]
                    found = True
            if found:
                agg[agg_key] = round(total / divisor, 6)
                has_data = True

        if has_data:
            metrics.append({"name": ns_metric, "results": {"aggregation_info": agg}})

    metrics.append({"name": "namespaceTotalPods", "results": {"aggregation_info": {"sum": num_workloads}}})
    metrics.append({"name": "namespaceRunningPods", "results": {"aggregation_info": {"sum": num_workloads}}})
    return metrics


def build_ns_result_json(experiment_name, namespace, ns_metrics, start_time, end_time):
    return {
        "version": "v2.0",
        "experiment_name": experiment_name,
        "interval_start_time": normalize_timestamp(start_time),
        "interval_end_time": normalize_timestamp(end_time),
        "kubernetes_objects": [
            {"namespaces": {"namespace": namespace, "metrics": ns_metrics}}
        ],
    }


def main():
    parser = argparse.ArgumentParser(
        description="Fetch data from KubeGenX APIs and save as Kruize-compatible JSONs"
    )
    parser.add_argument("--url", required=True, help="KubeGenX base URL (e.g. https://host:port)")
    parser.add_argument("--dataset", required=True, help="Dataset name")
    parser.add_argument("--session-id", required=True, help="Session ID")
    parser.add_argument("--output-dir", required=True, help="Output directory for saved JSONs")
    parser.add_argument("--insecure", action="store_true", help="Skip TLS certificate verification")
    parser.add_argument("--limit-namespaces", type=int, default=0, help="Limit number of namespaces (0 = all)")
    parser.add_argument("--limit-workloads", type=int, default=0, help="Limit workloads per namespace (0 = all)")
    parser.add_argument("-c", "--exp-type", default="container", choices=VALID_EXP_TYPES,
                        help="Experiment type: container (default), namespace, or container_ns")
    args = parser.parse_args()

    if args.insecure:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

    verify = not args.insecure

    exp_dir = os.path.join(args.output_dir, "create_experiments")
    res_dir = os.path.join(args.output_dir, "results")
    os.makedirs(exp_dir, exist_ok=True)
    os.makedirs(res_dir, exist_ok=True)

    exp_type = args.exp_type
    generate_container = exp_type in ("container", "container_ns")
    generate_namespace = exp_type in ("namespace", "container_ns")

    print(f"Fetching namespaces from {args.url} ...")
    print(f"Experiment type: {exp_type}")
    ns_items = fetch_namespaces(args.url, args.dataset, args.session_id, verify=verify)
    namespaces = list({item["namespace"]: item for item in ns_items}.values())
    print(f"Found {len(namespaces)} namespaces")

    if args.limit_namespaces > 0:
        namespaces = namespaces[:args.limit_namespaces]
        print(f"Limited to {len(namespaces)} namespaces")

    total_experiments = 0
    total_results = 0
    max_results_per_experiment = 0
    start_time = time.time()

    for ns_item in namespaces:
        ns = ns_item["namespace"]
        print(f"\nNamespace: {ns}")
        workloads = fetch_workloads(args.url, args.dataset, args.session_id, ns, verify=verify)
        print(f"  {len(workloads)} workloads")

        if args.limit_workloads > 0:
            workloads = workloads[:args.limit_workloads]

        ns_all_results = defaultdict(list)

        for wl in workloads:
            experiment_name = f"{ns}_{wl['name']}"

            result_items, _ = fetch_results(
                args.url, args.dataset, args.session_id, ns, wl["name"], verify=verify
            )

            if generate_container:
                create_exp = build_create_exp_json(ns_item, wl)
                exp_file = os.path.join(exp_dir, f"{experiment_name}.json")
                with open(exp_file, "w", encoding="utf-8") as f:
                    json.dump(create_exp, f, indent=2)

                result_jsons = [
                    build_result_json(experiment_name, wl, ns, item)
                    for item in result_items
                ]

                res_file = os.path.join(res_dir, f"{experiment_name}.json")
                with open(res_file, "w", encoding="utf-8") as f:
                    json.dump(result_jsons, f, indent=2)

                total_experiments += 1
                total_results += len(result_jsons)
                if len(result_jsons) > max_results_per_experiment:
                    max_results_per_experiment = len(result_jsons)
                print(f"  {wl['name']}: {len(result_jsons)} results")

            if generate_namespace:
                for item in result_items:
                    key = (item["start"], item["end"])
                    ns_all_results[key].append(item)

        if generate_namespace and ns_all_results:
            ns_exp_name = f"ns_{ns}"
            cluster_name = workloads[0].get("clusterId", ns_item.get("clusterId", "cluster-one")) if workloads else "cluster-one"
            ns_create_exp = build_ns_create_exp_json(ns, cluster_name)
            with open(os.path.join(exp_dir, f"{ns_exp_name}.json"), "w", encoding="utf-8") as f:
                json.dump(ns_create_exp, f, indent=2)

            ns_result_jsons = []
            for (start_ts, end_ts), items in sorted(ns_all_results.items()):
                ns_metrics = aggregate_results_to_ns_metrics(items, len(workloads))
                ns_result_jsons.append(
                    build_ns_result_json(ns_exp_name, ns, ns_metrics, start_ts, end_ts)
                )

            with open(os.path.join(res_dir, f"{ns_exp_name}.json"), "w", encoding="utf-8") as f:
                json.dump(ns_result_jsons, f, indent=2)

            total_experiments += 1
            total_results += len(ns_result_jsons)
            if len(ns_result_jsons) > max_results_per_experiment:
                max_results_per_experiment = len(ns_result_jsons)
            print(f"  ns:{ns} -> {ns_exp_name} ({len(ns_result_jsons)} namespace results)")

    metadata = {
        "total_experiments": total_experiments,
        "total_results": total_results,
        "max_results_per_experiment": max_results_per_experiment,
        "experiment_type": exp_type,
        "namespaces": [n["namespace"] for n in namespaces],
        "source": {
            "url": args.url,
            "dataset": args.dataset,
            "session_id": args.session_id
        }
    }
    with open(os.path.join(args.output_dir, "metadata.json"), "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    elapsed = time.time() - start_time
    hours, rem = divmod(elapsed, 3600)
    minutes, seconds = divmod(rem, 60)
    print(f"\nDone! {total_experiments} experiments, {total_results} result data points")
    print(f"Saved to {args.output_dir}")
    print(f"Time elapsed: {int(hours):02d}:{int(minutes):02d}:{seconds:05.2f}")


if __name__ == "__main__":
    main()
