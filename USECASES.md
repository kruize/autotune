## Usecases

Kruize usecases include Autotune, Remote Monitoring and Local Monitoring

**Autotune**

Autotune accepts a user provided "slo" goal to optimize application performance. It uses Prometheus to identify "layers" of an application that it is monitoring and matches tunables from those layers to the user provided slo. It then runs experiments with the help of a hyperparameter optimization framework to arrive at the most optimal values for the identified set of tunables to get a better result for the user provided slo.
The trials provided by the HPO is fed to the Experiment Manager which deploys it to the application and then monitors the pods. After all the trials are completed, EM reports the result and generates the recommendation.

**Remote Monitoring**

In this, an experiment is created using mode as monitor and targetted cluster as remote. This usecases uses the REST APIs instead of the kubernetes yaml to create experiment. In this, external agents are involved in monitoring the pods and reporting the result, using which the recommendations are generated and provided as a response by the listRecommendations API.

**Local Monitoring**

This is a planned usecase which is under active development. In this, the targetted cluster will be local and it will make use of experiment manager to monitor the pods and providing the results to generated recommendations.


# ****Use Case 1**: Autotune - Config Recommendation for a User Provided Performance Objective**

Autotune accepts a user provided "slo" goal to optimize application performance. It uses Prometheus to identify "layers" of an application that it is monitoring and matches tunables from those layers to the user provided slo. It then runs experiments with the help of a hyperparameter optimization framework to arrive at the most optimal values for the identified set of tunables to get a better result for the user provided slo.
The trials provided by the HPO is fed to the Experiment Manager which deploys it to the application and then monitors the pods. After all the trials are completed, EM reports the result and generates the recommendation.

**1.Using Bayesian Optimization to tune the OS**

OS tuning settings are improved using Bayesian optimisation. Using the Node Tuning Operator, OpenShift enables the establishment of performance profiles for
specific nodes or containers. For subsequent OS versions, the present OS tunables, which were manually created on RHEL 7, need to be updated. Although it is
still in development, Autotune seeks to optimise microservices inside of containers and might be modified for OS tweaking. Objective function definition, the addition of an OS layer, the creation of a Tuned YAML file, and synchronisation with benchmark load generation scripts are among the changes made. For executing these actions, technology and automation are essential.

**2.Using Autotune to tune nodes and schedule appropriate microservices onto them**

Autotune is used to improve the performance of a group of microservices, which includes a database. Through a series of experiments, Autotune determines the
best configuration for the given objective function. Based on OS performance profiles, Autotune categorises the microservices and objective functions and
offers the best profiles to apply to the nodes. By doing this, resource usage is maximised and the microservices are able to reach their performance goals
from the OS layer to the runtime framework


**Use Case 2: Hyper Parameter Optimization as a Service - Performance**

Provide Kruize Hyper Parameter Optimization (HPO) to choose the optimal values for hyperparameters provided by the user for any model.

Hyperparameter optimization(HPO) is choosing a set of optimal hyperparameters that yields an optimal performance based on the
predefined objective function.

The current architecture of Kruize HPO consists of a thin abstraction layer that provides a common REST API and gRPC
interface. It provides an interface for integrating with Open Source projects / modules that provide HPO functionality. Currently it only supports the Optuna OSS Project. It provides a simple HTTP server through which the REST APIs can be accessed.

Kruize HPO supports the following ways of deployment:

1. Bare Metal

2. Container

3. Kubernetes (Minikube / Openshift)

**Use Case 3: Monitor a deployment over a long term and provide recommendations to help reduce cost (Production scenario with no experiment trials)**

Provide recommendations on container sizing for customer apps running in their OpenShift clusters. By offering container suggestions that focus on the
proper size containers, the overarching objective is to lower expenses for the customer

**1.Remote (RH Insights Resource Optimization Service for OpenShift)**

**Workflow**

1.The client backend will invoke the Kruize createExperiment API for each deployment + namespace that it is monitoring.

2.It will then call the Kruize updateResults API for each deployment + namespace as and when it gets new data from the target clusters. This can happen
multiple times in a day.

3.After that it will call the listRecommendations API for each deployment + namespace after a predetermined amount of time has elapsed from the first call to
updateResults to obtain the recommendations for that specific deployment + namespace combination.

**Queries**

This includes the Prometheus Queries that will need to be run against the target clusters for each deployment that is being monitored.

**The queries assume the following**

1.Data is gathered for a fixed interval. For e.g: It can be 15min, 30min or 60min etc.

2.The data unless specified needs to be gathered at the Container granularity

**2.Local (Advanced Cluster Management)**


**Use Case 4: Provide tooling for automating performance regressions instead of manual testing (A/B/n testing as part of the build pipeline)**
