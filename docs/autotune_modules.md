## Autotune Modules

Autotune functional tests validate individual modules of Autotune. Autotune has the following high level modules:

- Analyzer module
	- Dependency Analyzer
	- Recommendation Manager
- Hyperparameter Optimization module
- Experiment manager module

### Analyzer module

Analyzer module is a combination of Dependency Analyzer and Recommendation Manager sub-modules. 

Dependency Analyzer does the following:

- Dependency Analyzer validates the autotune yaml file provided by the user. This yaml file defines the "slo" goal along with the objective function to optimize application performance.
- It uses Prometheus to identify "layers" of an application that it is monitoring and matches tunables from those layers to the user provided slo. 
- It creates the search space for all the layer tunables and function variables to derive the objective function

Recommendation Manager does the following:

- Recommendation Manager (RM) module posts the search space to the Hyperparameter Optimization module (HPO)
- Obtains the trial config for the tunables from the HPO module and posts it to the Experiment manager module (EM)
- It then computes the object function for the slo based on the metrics values from Experiment manager and posts the results to HPO module
- It repeats step 2 & 3 until the specified number of trials are completed and then obtains the optimal values for the tunables from HPO and returns it to the user 


### Hyperparameter Optimization module

- HPO module accepts a search space of the tunables from RM module and generates the trial values for the tunables using the Hyperparameter optimization framework.
- It processes the metrics gathered and sends it back to the RM module 


### Experiment Manager module

- Experiment manager runs the experiments by re-deploying the application with the provided tunable values. It either does a rolling update or new deployment
  of the application based on the deployment policy.
- It continuously monitors for load, processes the prometheus queries and uses them to gather metrics for a specified duration and interval.
- It processes the metrics gathered and returns them to the RM module.

