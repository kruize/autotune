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

import optuna

import os
import time

from logger import get_logger

n_trials = int(os.getenv("N_TRIALS"))
n_jobs = int(os.getenv("N_JOBS"))

logger = get_logger(__name__)

trials = []


class TrialDetails:
    """
    A class containing the details of a trial such as trial number, tunable values suggested by Optuna, status of the
    experiment and the objective function value type and value.
    """

    trial_number = -1
    trial_json_object = {}
    trial_result_received = -1
    trial_result = ""
    result_value_type = ""
    result_value = 0


def perform_experiment():
    # Loop to be replaced by a queueing mechanism to determine if the result has been received
    while TrialDetails.trial_result_received == -1:
        time.sleep(1)
    return TrialDetails.result_value, TrialDetails.trial_result


class Objective(TrialDetails):
    """
    A class used to define search space and return the actual slo value.

    Parameters:
        tunables (list): A list containing the details of each tunable in a dictionary format.
    """

    def __init__(self, tunables):
        self.tunables = tunables

    def __call__(self, trial):
        global trials

        experiment_tunables = []
        config = {}

        TrialDetails.trial_number += 1

        # Define search space
        for tunable in self.tunables:
            if tunable["value_type"].lower() == "double":
                tunable_value = trial.suggest_discrete_uniform(
                    tunable["name"], tunable["lower_bound"], tunable["upper_bound"], tunable["step"]
                )
            elif tunable["value_type"].lower() == "integer":
                tunable_value = trial.suggest_int(
                    tunable["name"], tunable["lower_bound"], tunable["upper_bound"], tunable["step"]
                )
            elif tunable["value_type"].lower() == "categorical":
                tunable_value = trial.suggest_categorical(tunable["name"], tunable["choices"])

            experiment_tunables.append({"tunable_name": tunable["name"], "tunable_value": tunable_value})

        config["experiment_tunables"] = experiment_tunables

        logger.debug("Experiment tunables: " + str(experiment_tunables))

        TrialDetails.trial_json_object = experiment_tunables

        actual_slo_value, experiment_status = perform_experiment()

        TrialDetails.trial_result_received = -1

        config["experiment_status"] = experiment_status

        trials.append(config)

        if experiment_status == "prune":
            raise optuna.TrialPruned()

        actual_slo_value = round(float(actual_slo_value), 2)
        return actual_slo_value


def recommend(experiment_name, direction, hpo_algo_impl, id_, objective_function, tunables, value_type):
    """
    Perform Bayesian Optimization with Optuna using the appropriate sampler and recommend the best config.

    Parameters:
        experiment_name (str): The name of the application that is being optimized.
        direction (str): Direction of optimization, minimize or maximize.
        hpo_algo_impl (str): Hyperparameter optimization library to perform Bayesian Optimization.
        id_ (str): The id of the application that is being optimized.
        objective_function (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.
        value_type (string): Value type of the objective function.
    """
    # Set the logging level for the Optuna’s root logger
    optuna.logging.set_verbosity(optuna.logging.WARNING)
    # Propagate all of Optuna log outputs to the root logger
    optuna.logging.enable_propagation()
    # Disable the default handler of the Optuna’s root logger
    optuna.logging.disable_default_handler()

    # Choose a sampler based on the value of ml_algo_impl
    if hpo_algo_impl == "optuna_tpe":
        sampler = optuna.samplers.TPESampler()
    elif hpo_algo_impl == "optuna_tpe_multivariate":
        sampler = optuna.samplers.TPESampler(multivariate=True)
    elif hpo_algo_impl == "optuna_skopt":
        sampler = optuna.integration.SkoptSampler()

    # Create a study object
    study = optuna.create_study(direction=direction, sampler=sampler)

    # Execute an optimization by using an 'Objective' instance
    study.optimize(Objective(tunables), n_trials=n_trials, n_jobs=n_jobs)

    TrialDetails.trial_number = -1

    # Get the best parameter
    logger.info("Best parameter: " + str(study.best_params))
    # Get the best value
    logger.info("Best value: " + str(study.best_value))
    # Get the best trial
    logger.info("Best trial: " + str(study.best_trial))

    logger.debug("All trials: " + str(trials))

    # recommended_config (json): A JSON containing the recommended config.
    recommended_config = {}

    optimal_value = {"objective_function": {
        "name": objective_function,
        "value": study.best_value,
        "value_type": value_type
    }, "tunables": []}

    for tunable in tunables:
        for key, value in study.best_params.items():
            if key == tunable["name"]:
                tunable_value = value
        optimal_value["tunables"].append(
            {
                "name": tunable["name"],
                "value": tunable_value,
                "value_type": tunable["value_type"],
                "step": tunable["step"]
            }
        )

    recommended_config["experiment_id"] = id_
    recommended_config["experiment_name"] = experiment_name
    recommended_config["direction"] = direction
    recommended_config["optimal_value"] = optimal_value

    logger.info("Recommended config: " + str(recommended_config))
