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

from install_package import install

install('python-dotenv')
install('optuna')

import optuna, random
from optuna.samplers import TPESampler

import logging
import os
import subprocess

from dotenv import load_dotenv

load_dotenv()

n_trials = int(os.getenv("n_trials"))
n_jobs = int(os.getenv("n_jobs"))

trials = []

from tunables import get_all_tunables
from experiment import perform_experiment


class Objective(object):
    """
    A class used to define search space and return the actual sla value.
    
    Parameters:
        sla_class (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.
    """

    def __init__(self, sla_class, tunables):
        self.sla_class = sla_class
        self.tunables = tunables

    def __call__(self, trial):
        global trials

        experiment_tunables = []
        config = {}

        # Define search space
        for tunable in tunables:
            if tunable["value_type"] == "double":
                tunable_value = trial.suggest_uniform(tunable["name"], tunable["lower_bound"], tunable["upper_bound"])
                tunable_value = round(tunable_value, 2)
            experiment_tunables.append({"tunable_name": tunable["name"], "tunable_value": tunable_value})

        config["experiment_tunables"] = experiment_tunables

        logger.debug("Experiment tunables: " + str(experiment_tunables))

        actual_sla_value, is_success = perform_experiment(experiment_tunables)
        
        if is_success == True:
            config["is_success"] = True
        else:
            config["is_success"] = False
        
        trials.append(config)

        if is_success == False:
            raise optuna.TrialPruned()

        return actual_sla_value


# Create and configure logger
logging.basicConfig(filename='optuna.log',
        format='%(asctime)s - %(levelname)s - %(message)s',
        datefmt='%d-%b-%y %H:%M:%S',
        filemode='a',
        level=logging.DEBUG)

logger = logging.getLogger()

sla_class, direction, tunables = get_all_tunables()

# Create a study object
study = optuna.create_study(direction=direction, sampler=TPESampler())

# Execute an optimization by using an 'Objective' instance
study.optimize(Objective(sla_class, tunables), n_trials=n_trials, n_jobs=n_jobs)

# Get the best parameter
logger.info("Best parameter: " + str(study.best_params))
# Get the best value
logger.info("Best value: " + str(study.best_value))
# Get the best trial
logger.info("Best trial: " + str(study.best_trial))

logger.info("All trials: " + str(trials))

best_config = {}
optimal_value = {}

best_config["sla_class"] = sla_class
optimal_value["tunables"] = study.best_params
optimal_value["sla"] = study.best_value
best_config["optimal_value"] = optimal_value

logger.info("Best config: " + str(best_config))

