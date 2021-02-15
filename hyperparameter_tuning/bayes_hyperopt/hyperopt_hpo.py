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

from functools import partial
from hyperopt import fmin, tpe, hp, STATUS_OK, STATUS_FAIL, Trials

import os

from experiment import perform_experiment
from logger import get_logger

from dotenv import load_dotenv

load_dotenv()

max_evals = int(os.getenv("max_evals"))

logger = get_logger(__name__)

trials_info = []


def objective(params, direction, sla_class, tunables):
    """
    Return a dictionary containing the actual sla value and status.

    Parameters:
        params (dict): The hyperparameter search space.
        direction (str): Direction of optimization, minimize or maximize.
        sla_class (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.

    Returns:
        loss (float): The function value that we are trying to minimize, if the status is 'ok' then this has to be present.
        status (string): One of the keys from hyperopt.STATUS_STRINGS, such as 'ok' for successful completion, and
        'fail' in cases where the function turned out to be undefined.
    """

    global trials_info

    experiment_tunables = []
    config = {}

    for tunable in tunables:
        experiment_tunables.append({"tunable_name": tunable["name"], "tunable_value": params[tunable["name"]]})

    config["experiment_tunables"] = experiment_tunables

    logger.debug("Experiment tunables: " + str(experiment_tunables))

    actual_sla_value, experiment_status = perform_experiment(experiment_tunables)

    config["experiment_status"] = experiment_status

    trials_info.append(config)

    if experiment_status in ("failure", "prune"):
        return {'status': STATUS_FAIL}

    if direction == "maximize":
        actual_sla_value = -actual_sla_value

    return {'loss': actual_sla_value, 'status': STATUS_OK}


def recommend(direction, sla_class, tunables):
    """
    Perform Bayesian Optimization with Hyperopt and recommend the best config.

    Parameters:
        direction (str): Direction of optimization, minimize or maximize.
        sla_class (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.
    """
    search_space = {}

    # Define search space
    for tunable in tunables:
        if tunable["value_type"] == "double":
            tunable_value = hp.quniform(
                tunable["name"], tunable["lower_bound"], tunable["upper_bound"], tunable["step"]
            )
        search_space[tunable["name"]] = tunable_value

    # Default database to store info at each time step
    trials = Trials()

    fmin_objective = partial(objective, direction=direction, sla_class=sla_class, tunables=tunables)

    # Perform an optimization
    best = fmin(fn=fmin_objective, space=search_space, algo=tpe.suggest, max_evals=max_evals, trials=trials)

    if direction == "minimize":
        best_sla_value = trials.best_trial['result']['loss']
    else:
        best_sla_value = -trials.best_trial['result']['loss']

    # Get the best parameter
    logger.info("Best parameter: " + str(best))
    # Get the best value
    logger.info("Best value: " + str(best_sla_value))
    # Get the best trial
    logger.info("Best trial: " + str(trials.best_trial))

    logger.debug("All trials: " + str(trials_info))

    best_config = {}

    optimal_value = {
        "tunables": best,
        "sla": best_sla_value
    }

    best_config["sla_class"] = sla_class
    best_config["optimal_value"] = optimal_value

    logger.info("Best config: " + str(best_config))
