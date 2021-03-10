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

# __main__.py is the entrypoint for the ML module. Depending upon the value of ml_algo_impl,
# the appropriate hyperparameter optimization library module is called to perform Bayesian Optimization.

from tunables import get_all_tunables

from bayes_optuna import optuna_hpo

# application_name (str): The name of the application that is being optimized.
# direction (str): Direction of optimization, minimize or maximize.
# hpo_algo_impl (str): Hyperparameter optimization library to perform Bayesian Optimization.
# id (str): The id of the application that is being optimized.
# objective_function (str): The objective function that is being optimized.
# tunables (list): A list containing the details of each tunable in a dictionary format.
# value_type (string): Value type of the objective function.
application_name, direction, hpo_algo_impl, id, objective_function, tunables, value_type = get_all_tunables(search_space_json)

if hpo_algo_impl in ("optuna_tpe", "optuna_tpe_multivariate", "optuna_skopt"):
    optuna_hpo.recommend(application_name, direction, hpo_algo_impl, id, objective_function, tunables, value_type)
