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
from collections import defaultdict

MEBIBYTE = 1048576

VALID_EXP_TYPES = ("container", "namespace", "container_ns")

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


NS_METRIC_MAP = {
    "namespaceCpuRequest": {
        "container_cols": {"sum": "cpu_request_container_sum"},
        "format": "cores",
    },
    "namespaceCpuLimit": {
        "container_cols": {"sum": "cpu_limit_container_sum"},
        "format": "cores",
    },
    "namespaceCpuUsage": {
        "container_cols": {
            "min": "cpu_usage_container_min",
            "max": "cpu_usage_container_max",
            "avg": "cpu_usage_container_avg",
        },
        "format": "cores",
    },
    "namespaceCpuThrottle": {
        "container_cols": {
            "max": "cpu_throttle_container_max",
            "avg": "cpu_throttle_container_avg",
        },
        "format": "cores",
    },
    "namespaceMemoryRequest": {
        "container_cols": {"sum": "memory_request_container_sum"},
        "format": "MiB",
        "divisor": MEBIBYTE,
    },
    "namespaceMemoryLimit": {
        "container_cols": {"sum": "memory_limit_container_sum"},
        "format": "MiB",
        "divisor": MEBIBYTE,
    },
    "namespaceMemoryUsage": {
        "container_cols": {
            "min": "memory_usage_container_min",
            "max": "memory_usage_container_max",
            "avg": "memory_usage_container_avg",
        },
        "format": "MiB",
        "divisor": MEBIBYTE,
    },
    "namespaceMemoryRSS": {
        "container_cols": {
            "min": "memory_rss_usage_container_min",
            "max": "memory_rss_usage_container_max",
            "avg": "memory_rss_usage_container_avg",
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


def convert_csv_to_json(csv_path, exp_dir, res_dir):
    with open(csv_path, "r", newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    if not rows:
        return None, 0

    first = rows[0]
    experiment_name = f"{first['namespace']}_{first['k8_object_name']}"
    container_name = first["container_name"]
    image_name = first["image_name"]
    k8_type = first["k8_object_type"]
    k8_name = first["k8_object_name"]
    namespace = first["namespace"]
    cluster_name = first["cluster_name"]

    create_exp = {
        "version": "v2.0",
        "experiment_name": experiment_name,
        "cluster_name": cluster_name,
        "performance_profile": "resource-optimization-openshift",
        "mode": "monitor",
        "target_cluster": "remote",
        "kubernetes_objects": [
            {
                "type": k8_type,
                "name": k8_name,
                "namespace": namespace,
                "containers": [
                    {
                        "container_image_name": image_name,
                        "container_name": container_name,
                    }
                ],
            }
        ],
        "trial_settings": {"measurement_duration": "15min"},
        "recommendation_settings": {"threshold": "0.1"},
    }

    results = []
    for row in rows:
        metrics = row_to_metrics(row)
        result_entry = {
            "version": "v2.0",
            "experiment_name": experiment_name,
            "interval_start_time": row["start_timestamp"],
            "interval_end_time": row["end_timestamp"],
            "kubernetes_objects": [
                {
                    "type": k8_type,
                    "name": k8_name,
                    "namespace": namespace,
                    "containers": [
                        {
                            "container_image_name": image_name,
                            "container_name": container_name,
                            "metrics": metrics,
                        }
                    ],
                }
            ],
        }
        results.append(result_entry)

    with open(os.path.join(exp_dir, f"{experiment_name}.json"), "w", encoding="utf-8") as f:
        json.dump(create_exp, f, indent=2)

    with open(os.path.join(res_dir, f"{experiment_name}.json"), "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)

    return experiment_name, len(results)


def aggregate_rows_to_ns_metrics(rows):
    """Aggregate container-level CSV rows (same namespace + timestamp) into namespace metrics."""
    metrics = []
    for metric_name, spec in NS_METRIC_MAP.items():
        divisor = spec.get("divisor", 1)
        agg = {"format": spec["format"]}
        has_data = False
        for agg_key, col_name in spec["container_cols"].items():
            total = 0.0
            found = False
            for row in rows:
                val = row.get(col_name, "")
                if val != "" and val is not None:
                    total += float(val)
                    found = True
            if found:
                agg[agg_key] = round(total / divisor, 6)
                has_data = True
        if has_data:
            metrics.append({"name": metric_name, "results": {"aggregation_info": agg}})

    num_workloads = len({row.get("k8_object_name", "") for row in rows})
    metrics.append({"name": "namespaceTotalPods", "results": {"aggregation_info": {"sum": num_workloads}}})
    metrics.append({"name": "namespaceRunningPods", "results": {"aggregation_info": {"sum": num_workloads}}})
    return metrics


def convert_csvs_to_namespace_json(csv_dir, csv_files, exp_dir, res_dir):
    """Read all CSVs, group by namespace, aggregate into namespace experiments."""
    ns_rows = defaultdict(list)
    ns_cluster = {}

    for csv_file in csv_files:
        csv_path = os.path.join(csv_dir, csv_file)
        with open(csv_path, "r", newline="", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                ns = row["namespace"]
                ns_rows[ns].append(row)
                if ns not in ns_cluster:
                    ns_cluster[ns] = row.get("cluster_name", "default")

    experiments = []
    total_results = 0
    max_results = 0

    for ns, rows in sorted(ns_rows.items()):
        experiment_name = f"ns_{ns}"
        cluster_name = ns_cluster[ns]

        create_exp = {
            "version": "v2.0",
            "experiment_name": experiment_name,
            "cluster_name": cluster_name,
            "performance_profile": "resource-optimization-openshift",
            "mode": "monitor",
            "target_cluster": "remote",
            "experiment_type": "namespace",
            "kubernetes_objects": [{"namespaces": {"namespace": ns}}],
            "trial_settings": {"measurement_duration": "15min"},
            "recommendation_settings": {"threshold": "0.1"},
        }

        interval_groups = defaultdict(list)
        for row in rows:
            key = (row["start_timestamp"], row["end_timestamp"])
            interval_groups[key].append(row)

        results = []
        for (start_ts, end_ts), interval_rows in sorted(interval_groups.items()):
            ns_metrics = aggregate_rows_to_ns_metrics(interval_rows)
            results.append({
                "version": "v2.0",
                "experiment_name": experiment_name,
                "interval_start_time": start_ts,
                "interval_end_time": end_ts,
                "kubernetes_objects": [
                    {"namespaces": {"namespace": ns, "metrics": ns_metrics}}
                ],
            })

        with open(os.path.join(exp_dir, f"{experiment_name}.json"), "w", encoding="utf-8") as f:
            json.dump(create_exp, f, indent=2)
        with open(os.path.join(res_dir, f"{experiment_name}.json"), "w", encoding="utf-8") as f:
            json.dump(results, f, indent=2)

        experiments.append(experiment_name)
        total_results += len(results)
        if len(results) > max_results:
            max_results = len(results)
        print(f"  ns:{ns} -> {experiment_name} ({len(results)} results from {len(rows)} container rows)")

    return len(experiments), total_results, max_results


def main():
    parser = argparse.ArgumentParser(
        description="Convert per-workload CSVs to Kruize JSON format for the scale test pipeline"
    )
    parser.add_argument("--csv-dir", required=True, help="Directory with per-workload CSV files")
    parser.add_argument("--output-dir", required=True, help="Output directory for JSONs (create_experiments/ + results/ + metadata.json)")
    parser.add_argument("-c", "--exp-type", default="container", choices=VALID_EXP_TYPES,
                        help="Experiment type: container (default), namespace, or container_ns")
    args = parser.parse_args()

    exp_dir = os.path.join(args.output_dir, "create_experiments")
    res_dir = os.path.join(args.output_dir, "results")
    os.makedirs(exp_dir, exist_ok=True)
    os.makedirs(res_dir, exist_ok=True)

    csv_files = sorted([f for f in os.listdir(args.csv_dir) if f.endswith(".csv")])
    print(f"Found {len(csv_files)} CSV files in {args.csv_dir}")
    print(f"Experiment type: {args.exp_type}")

    total_experiments = 0
    total_results = 0
    max_results_per_experiment = 0

    if args.exp_type in ("container", "container_ns"):
        for csv_file in csv_files:
            csv_path = os.path.join(args.csv_dir, csv_file)
            exp_name, num_results = convert_csv_to_json(csv_path, exp_dir, res_dir)
            if exp_name is None:
                print(f"  {csv_file}: empty (skipped)")
                continue
            total_experiments += 1
            total_results += num_results
            if num_results > max_results_per_experiment:
                max_results_per_experiment = num_results
            print(f"  {csv_file} -> {exp_name} ({num_results} results)")

    if args.exp_type in ("namespace", "container_ns"):
        print(f"\nGenerating namespace experiments...")
        ns_exps, ns_results, ns_max = convert_csvs_to_namespace_json(
            args.csv_dir, csv_files, exp_dir, res_dir
        )
        total_experiments += ns_exps
        total_results += ns_results
        if ns_max > max_results_per_experiment:
            max_results_per_experiment = ns_max

    metadata = {
        "total_experiments": total_experiments,
        "total_results": total_results,
        "max_results_per_experiment": max_results_per_experiment,
        "experiment_type": args.exp_type,
    }
    metadata_path = os.path.join(args.output_dir, "metadata.json")
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print(f"\nDone! {total_experiments} experiments, {total_results} total results")
    print(f"Output: {args.output_dir}")


if __name__ == "__main__":
    main()
