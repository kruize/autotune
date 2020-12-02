from install_package import install

install('optuna')

import optuna, random
from optuna.samplers import TPESampler

import subprocess

trials = []

from tunables import get_all_tunables
from experiment import perform_experiment


# define objective function
class Objective(object):
    def __init__(self, sla, tunables):
        # Hold this implementation specific arguments as the fields of the class.
        self.sla = sla
        self.tunables = tunables

    def __call__(self, trial):
        global trials

        # define search space
        for tunable in tunables:
            if tunable['name'] == 'cpu-request':
                cpu_request = trial.suggest_uniform(tunable['name'], tunable['min_value'], tunable['max_value'])
                break
        
        for tunable in tunables:
            if tunable['name'] == 'memory-request':
                memory_request = trial.suggest_uniform(tunable['name'], tunable['min_value'], tunable['max_value'])
                break
        
        config = {'cpu_request': cpu_request, 'memory_request': memory_request, 'flag': 0}

        print(cpu_request, memory_request)

        actual_sla, is_success = perform_experiment(config)
        
        if is_success == 1:
            config['flag'] = 1
        
        trials.append(config)

        # predicted_sla = random.randint(10, 800) 

        return actual_sla

# arguments
sla, direction, tunables = get_all_tunables()

# create a study object
study = optuna.create_study(direction=direction, sampler=TPESampler())

# Execute an optimization by using an `Objective` instance.
study.optimize(Objective(sla, tunables), n_trials=5, n_jobs=1)

# get the best parameter 
print(study.best_params)
# get the best value 
print(study.best_value)
# get the best trial 
print(study.best_trial)

print(trials)

