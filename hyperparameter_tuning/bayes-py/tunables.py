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

import json


def get_all_tunables():
    """
    Query Dependency Analyzer API for the sla_class, direction and tunables, and return them.

    Returns:
        sla_class (str): The objective function that is being optimized.
        direction (str): Direction of optimization, minimize or maximize.
        tunables (list): A list containing the details of each tunable in a dictionary format.
    """
    # JSON returned by the Dependency Analyzer
    tuning_set_json = '{"sla_class": "response_time", "direction": "minimize", "tunables": [{"name": "cpuRequest", "value_type": "double", "upper_bound": 6, "lower_bound": 1}, {"name": "memoryRequest", "value_type": "double", "upper_bound": 1024, "lower_bound": 100}]}'
    tuning_set = json.loads(tuning_set_json)
    sla_class = tuning_set["sla_class"]
    direction = tuning_set["direction"]
    tunables = tuning_set["tunables"]
    return sla_class, direction, tunables

