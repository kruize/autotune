"""
Copyright (c) 2020, 2020 Red Hat, IBM Corporation and others.

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

def get_all_tunables():
    """
    Query Dependency Analyzer API for the sla, direction and tunables, and return them.

    Returns:
        sla (str): The term that is being optimized.
        direction (str): Direction of optimization, minimize or maximize.
        tunables (list): A list containing the details of each tunable in a dictionary format.
    """
    sla = 'response_time'
    direction = 'minimize'
    tunables = [{"name": "cpu-request", "value_type": "double", "upper_bound": 4, "lower_bound": 1}, {"name": "memory-request", "value_type": "double", "upper_bound": 1024, "lower_bound": 1}] 
    return sla, direction, tunables

