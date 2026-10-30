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

import csv
import os
import argparse
import re

def find_max_exec_time(exec_file):
    if not os.path.exists(exec_file):
        print(f"Execution time log not found: {exec_file}")
        return

    pattern = r"scaletest\d+-\d+: Total time elapsed: (\d{2,3}:\d{2}:\d{2})"

    with open(exec_file, "r") as file:
        lines = file.readlines()

    time_str_list = []

    for line in lines:
        match = re.search(pattern, line)
        if match:
            time_elapsed = match.group(1)
            time_str_list.append(time_elapsed)

    max_secs = -1
    max_time_str = ""

    for time_str in time_str_list:
        hrs, mins, secs = map(int, time_str.split(':'))
        total_secs = hrs * 3600 + mins * 60 + secs
        if total_secs > max_secs:
            max_secs = total_secs
            max_time_str = time_str

    if max_time_str:
        print(f"Execution time - {max_time_str}")
    else:
        print(f"No execution time found in {exec_file}")


def parse_csv_metrics(csv_file):
    stats = {}
    if not os.path.exists(csv_file):
        return stats

    with open(csv_file, mode='r') as file:
        reader = csv.DictReader(file)
        for row in reader:
            for col, value_str in row.items():
                if not col or value_str is None:
                    continue
                value_str = value_str.strip()
                if value_str:
                    try:
                        value = float(value_str)
                        if col not in stats:
                            stats[col] = {'max': value, 'sum': value, 'count': 1}
                        else:
                            stats[col]['max'] = max(stats[col]['max'], value)
                            stats[col]['sum'] += value
                            stats[col]['count'] += 1
                    except ValueError:
                        pass

    metrics = {}
    for col, data in stats.items():
        if data['count'] > 0:
            metrics[col] = (data['max'], data['sum'] / data['count'])
    return metrics


def get_max_avg(metrics, column_name):
    return metrics.get(column_name, (None, None))


def print_metric(metrics, column_name, label):
    max_val, avg_val = get_max_avg(metrics, column_name)
    if max_val is not None and avg_val is not None:
        print(f"{label} Latency Max / Avg value: {round(max_val, 2)} / {round(avg_val, 2)}")
    else:
        print(f"No valid values found in the specified column - {column_name}")


def print_ratio_metric(metrics, sum_column, count_column, label):
    sum_max_val, sum_avg_val = get_max_avg(metrics, sum_column)
    count_max_val, count_avg_val = get_max_avg(metrics, count_column)
    if count_max_val and count_avg_val and sum_max_val is not None and sum_avg_val is not None:
        max_val = round(sum_max_val / count_max_val, 2)
        avg_val = round(sum_avg_val / count_avg_val, 2)
        print(f"{label} Latency Max / Avg value: {max_val} / {avg_val}")
    else:
        print(f"No valid values found in the specified column - {count_column}")


def print_resource_metric(metrics, column_name, label, divisor=1.0, unit=""):
    max_val, _ = get_max_avg(metrics, column_name)
    if max_val is not None:
        scaled_val = round(max_val / divisor, 2)
        unit_str = f" {unit}" if unit else ""
        print(f"{label} Max value: {scaled_val}{unit_str}")
    else:
        print(f"No valid values found in the specified column - {column_name}")


def find_file_with_value(directory, column_name, target_value):
    matching_file = ""
    if not os.path.isdir(directory):
        return matching_file

    for filename in os.listdir(directory):
        if filename.startswith('total') and filename.endswith('.csv'):
            file_path = os.path.join(directory, filename)
            with open(file_path, newline='') as csvfile:
                reader = csv.DictReader(csvfile)
                for row in reader:
                    if column_name in row and row[column_name] == target_value:
                        matching_file = filename
                        break

    return matching_file


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', type=str, help='csv directory path', required=True)
    parser.add_argument('-r', type=str, help='Total recommendations count', required=True)

    args = parser.parse_args()

    directory_path = args.d
    target_value_to_find = args.r

    print(f"Directory path - {directory_path}")
    print(f"Recommendations count - {target_value_to_find}")


    column_name = 'kruize_recommendations'

    matched_filename = find_file_with_value(directory_path, column_name, target_value_to_find)

    if matched_filename:
        print(matched_filename)
        csv_file_path = os.path.join(directory_path, matched_filename)
        metrics = parse_csv_metrics(csv_file_path)

        print_metric(metrics, 'updateRecommendationsPerCall_success', 'Update Reco')
        print_metric(metrics, 'updateResultsPerCall_success', 'Update Results')
        print_metric(metrics, 'BulkJobPerCall_success', 'Bulk Job')
        #print_ratio_metric(metrics, 'loadResultsByExperimentName_sum_success', 'loadResultsByExperimentName_count_success', 'LoadResultsByExpName')
        print_ratio_metric(metrics, 'generatePlots_sum_success', 'generatePlots_count_success', 'Generate Plots')
        print_resource_metric(metrics, 'kruize_memory', 'Kruize memory', divisor=1024**3, unit="GB")
        print_resource_metric(metrics, 'kruize_cpu_max', 'Kruize cpu')
        print_resource_metric(metrics, 'kruizedb_memory', 'Kruizedb memory', divisor=1024**3, unit="GB")
        print_resource_metric(metrics, 'kruizedb_cpu_max', 'Kruizedb cpu')
    else:
        print(f"No files found containing '{target_value_to_find}' in '{column_name}' column.")

    #exec_time_log = os.path.join(directory_path, "..", "exec_time.log")
    #find_max_exec_time(exec_time_log)


if __name__ == '__main__':
    main()
