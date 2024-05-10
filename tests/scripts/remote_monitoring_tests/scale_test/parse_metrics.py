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
import sys
import re

def find_max_exec_time(exec_file):
    # Define the pattern to match
    pattern = r"scaletest\d+-\d+: Total time elapsed: (\d{2}:\d{2}:\d{2})"

    with open(exec_file, "r") as file:
        lines = file.readlines()

    pattern_found = False
    filtered_lines = []

    # Iterate through lines to find pattern match
    for line in lines:
        match = re.search(pattern, line)
        if match:
            pattern_found = True
            time_elapsed = match.group(1)
            filtered_lines.append(time_elapsed)

    print(f"Execution time - {max(filtered_lines)}")

def compute_max_avg(csv_file, column_name):
    # Initialize max value to infinity
    max_value = float('-inf')
    total_sum = 0
    count = 0

    with open(csv_file, mode='r') as file:
        reader = csv.DictReader(file)
        for row in reader:
            value_str = row[column_name].strip()
            if value_str:  # Check if the value is not empty
                value = float(value_str)
                max_value = max(max_value, value)
                total_sum += value
                count += 1

    if count == 0:
        return None, None

    avg_value = total_sum / count
    
    return max_value, avg_value


def find_file_with_value(directory, column_name, target_value):
    matching_file = ""

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

parser = argparse.ArgumentParser()
parser.add_argument('-d', type=str, help='csv directory path', required=True)
parser.add_argument('-r', type=str, help='Total results count', required=True)

args = parser.parse_args()

directory_path = args.d
target_value_to_find = args.r

print(f"Directory path - {directory_path}")
print(f"Results count - {target_value_to_find}")

column_name = 'kruize_results'

csv_file_path = find_file_with_value(directory_path, column_name, target_value_to_find)

if csv_file_path:
    print(csv_file_path)
else:
    print(f"No files found containing '{target_value_to_find}' in '{column_name}' column.")

csv_file_path = directory_path + '/' + csv_file_path

column_name_to_parse = 'updateRecommendationsPerCall_success'

max_val, avg_val = compute_max_avg(csv_file_path, column_name_to_parse)
if max_val is not None and avg_val is not None:
    max_val = round(max_val, 2)
    avg_val = round(avg_val, 2)

    print(f"Update Reco Latency Max / Avg value: {max_val} / {avg_val}")
else:
    print("No valid values found in the specified column.")

column_name_to_parse = 'updateResultsPerCall_success'
max_val, avg_val = compute_max_avg(csv_file_path, column_name_to_parse)
if max_val is not None and avg_val is not None:
    max_val = round(max_val, 2)
    avg_val = round(avg_val, 2)
    print(f"Update Results Latency Max / Avg value: {max_val} / {avg_val}")
else:
    print("No valid values found in the specified column.")

column_name_to_parse = 'loadResultsByExperimentName_sum_success'
sum_max_val, sum_avg_val = compute_max_avg(csv_file_path, column_name_to_parse)

column_name_to_parse = 'loadResultsByExperimentName_count_success'
count_max_val, count_avg_val = compute_max_avg(csv_file_path, column_name_to_parse)
if count_max_val is not None and count_avg_val is not None and sum_max_val is not None and sum_avg_val is not None:
    max_val = round(sum_max_val/count_max_val, 2)
    avg_val = round(sum_avg_val/count_avg_val, 2)
    print(f"LoadResultsByExpName Latency Max / Avg value: {max_val} / {avg_val}")
else:
    print("No valid values found in the specified column.")

column_name_to_parse = 'kruize_memory'
max_val, avg_val = compute_max_avg(csv_file_path, column_name_to_parse)
max_val = round(max_val/1024/1024/1024, 2)
if max_val is not None:
    print(f"Kruize memory Max value: {max_val} GB")
else:
    print("No valid values found in the specified column.")

column_name_to_parse = 'kruize_cpu_max'
max_val, avg_val = compute_max_avg(csv_file_path, column_name_to_parse)
max_val = round(max_val, 2)
if max_val is not None:
    print(f"Kruize cpu Max value: {max_val}")
else:
    print("No valid values found in the specified column.")

exec_time_log = directory_path + "/../exec_time.log"
find_max_exec_time(exec_time_log)

