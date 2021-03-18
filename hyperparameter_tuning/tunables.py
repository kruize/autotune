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
    Query Autotune API for the application_name, direction, hpo_algo_impl, id, objective_function, tunables and
    value_type, and return them.

    Returns:
        application_name (str): The name of the application that is being optimized.
        direction (str): Direction of optimization, minimize or maximize.
        hpo_algo_impl (str): Hyperparameter optimization library to perform Bayesian Optimization.
        id (str): The id of the application that is being optimized.
        objective_function (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.
        value_type (string): Value type of the objective function.
    """
    # JSON returned by the Autotune API
    # Placeholder code until the actual API parsing code is added
    search_space_json = '{"id": "auto123", "application_name": "petclinic-deployment-6d4c8678d4-jmz8x", ' \
                        '"objective_function": "transaction_response_time", "value_type": "double", "direction": ' \
                        '"minimize", "hpo_algo_impl": "optuna_tpe", "tunables": [{"name": "cpuRequest", "value_type": ' \
                        '"double", "upper_bound": 6, "lower_bound": 1, "step": 0.01}, {"name": "memoryRequest", ' \
                        '"value_type": "integer", "upper_bound": 1024, "lower_bound": 100, "step": 1}]} '
    search_space = json.loads(search_space_json)
    id = search_space["id"]
    application_name = search_space["application_name"]
    objective_function = search_space["objective_function"]
    value_type = search_space["value_type"]
    direction = search_space["direction"]
    hpo_algo_impl = search_space["hpo_algo_impl"]
    tunables = search_space["tunables"]
    return application_name, direction, hpo_algo_impl, id, objective_function, tunables, value_type
