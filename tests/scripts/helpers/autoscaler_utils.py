"""
Copyright (c) 2025 Red Hat, IBM Corporation and others.

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

import subprocess
import json
import re

# This function verifies if the vpa exists with specified name in specified namespace
def check_vpa_exists(vpa_name, namespace):
    command = f"kubectl get vpa -n {namespace} | grep -w {vpa_name} | awk '{{print $1}}'"

    try:
        output = subprocess.check_output(command, shell=True, stderr=subprocess.DEVNULL)
        found_vpa = output.decode('utf-8').strip()

        asse
        if found_vpa == vpa_name:
            print(f"VPA '{vpa_name}' exists in namespace '{namespace}'.")
            return True
        else:
            print(f"VPA '{vpa_name}' does not exist in namespace '{namespace}'.")
            return False

    except subprocess.CalledProcessError:
        print(f"Unable to verify if VPA '{vpa_name}' exist in namespace '{namespace}'.")
        return False


# This function verifies if vpa is created with correct containers
def check_vpa_containers(vpa_name, namespace, expected_containers):
    command = f"kubectl describe vpa {vpa_name} -n {namespace} | grep 'Container Name:' | awk '{{print $3}}'"

    try:
        output = subprocess.check_output(command, shell=True, stderr=subprocess.DEVNULL)
        vpa_containers = set(output.decode('utf-8').strip().split("\n"))

        expected_containers_set = set(expected_containers)

        if expected_containers_set == vpa_containers:
            print(f"VPA containers for '{vpa_name}' in namespace '{namespace}' verified.")
            print(f"Expected: {expected_containers_set}, Found: {vpa_containers}")
            return True
        else:
            print(f"Mismatch in VPA containers for '{vpa_name}' in namespace '{namespace}'.")
            print(f"Expected: {expected_containers_set}, Found: {vpa_containers}")
            return False

    except subprocess.CalledProcessError:
        print(f"Failed to retrieve VPA details for '{vpa_name}' in namespace '{namespace}'")
        return False


def check_vpa_resources(vpa_name, namespace):
    command = f"kubectl describe vpa {vpa_name} -n {namespace}"

    try:
        output = subprocess.check_output(command, shell=True, stderr=subprocess.DEVNULL)
        vpa_details = output.decode('utf-8')

        # Find all container recommendations
        container_blocks = re.findall(
            r"Container Name:\s+(\S+).*?Target:\s+Cpu:\s+(\S+)\s+Memory:\s+(\S+)",
            vpa_details, re.DOTALL
        )

        if not container_blocks:
            print(f"No resource recommendations found in VPA '{vpa_name}' in namespace '{namespace}'")
            return False

        all_valid = True
        for container_name, cpu, memory in container_blocks:
            if cpu and memory:
                print(f"Verified: Container '{container_name}' has CPU={cpu}, Memory={memory}")
            else:
                print(f"Missing CPU/Memory values for container '{container_name}' in VPA '{vpa_name}'")
                all_valid = False

        return all_valid

    except subprocess.CalledProcessError:
        print(f"Failed to retrieve VPA details for '{vpa_name}' in namespace '{namespace}'")
        return False


