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

install('optuna')

import optuna, random
from optuna.samplers import TPESampler

import subprocess

trials = []

from tunables import get_all_tunables
from experiment import perform_experiment


class Objective(object):
    """
    A class used to define search space and return the actual sla value.
    
    Parameters:
        sla (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.
    """

    def __init__(self, sla, tunables):
        self.sla = sla
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

        print(experiment_tunables)

        actual_sla_value, is_success = perform_experiment(experiment_tunables)
        
        if is_success == True:
            config["is_success"] = True
        else:
            config["is_success"] = False
        
        trials.append(config)

        return actual_sla_value


# arguments
sla, direction, tunables = get_all_tunables()

# Create a study object
study = optuna.create_study(direction=direction, sampler=TPESampler())

# Execute an optimization by using an 'Objective' instance.
study.optimize(Objective(sla, tunables), n_trials=5, n_jobs=1)

# Get the best parameter 
print(study.best_params)
# Get the best value 
print(study.best_value)
# Get the best trial 
print(study.best_trial)

print(trials)

