"""
Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.

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
    Query Dependency Analyzer API for the sla_class, direction, ml_algo and tunables, and return them.

    Returns:
        direction (str): Direction of optimization, minimize or maximize.
        ml_algo_impl (str): Hyperparameter optimization library to perform Bayesian Optimization.
        sla_class (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.
    """
    # JSON returned by the Dependency Analyzer
    # Placeholder code until the actual API parsing code is added
    tuning_set_json = '{"sla_class": "response_time", "direction": "minimize", "ml_algo_impl": "optuna_tpe", ' \
                      '"tunables": [{"name": "cpuRequest", "value_type": "double", "upper_bound": 4, "lower_bound": ' \
                      '1, "step": 0.01}, {"name": "memoryRequest", "value_type": "double", "upper_bound": 1024, ' \
                      '"lower_bound": 100, "step": 0.01}]} '
    tuning_set = json.loads(tuning_set_json)
    sla_class = tuning_set["sla_class"]
    direction = tuning_set["direction"]
    ml_algo_impl = tuning_set["ml_algo_impl"]
    tunables = tuning_set["tunables"]
    return direction, ml_algo_impl, sla_class, tunables
