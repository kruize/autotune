## Usecases

****Use Case 1**: Autotune - Config Recommendation for a User Provided Performance Objective**

Provide container sizing recommendations to customer applications that are being run in customer OpenShift clusters and being monitored through the 
ROS OpenShift Insights framework. The overall goal is to reduce costs for the customer by providing container recommendations that aim to right size 
containers.

**Workflow**

The Proposed work flow will be as follows
1.The ROS-OCP-backend will invoke the Kruize createExperiment API for each deployment + namespace that ROS is monitoring.

2.ROS-OCP-backend will then call the updateResults API for each deployment + namespace as and when it gets new data from the target clusters. 
This can happen multiple times in a day

3.The ROS-OCP-backend will then call the listRecommendations API for each deployment + namespace after a predetermined amount of time 
has elapsed from the first call to updateResults to obtain the recommendations for that specific deployment + namespace combination.

**Queries**

This includes the Prometheus Queries that will need to be run against the target clusters for each deployment that is being monitored.

**The queries assume the following**

1.Data is gathered for 15 min intervals

2.The data unless specified needs to be gathered at the Container granularity

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

1.Remote (RH Insights Resource Optimization Service for OpenShift)



2.Local (Advanced Cluster Management)


**Use Case 4: Provide tooling for automating performance regressions instead of manual testing (A/B/n testing as part of the build pipeline)**

