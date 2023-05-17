# Create Experiment

Client will invoke the Kruize createExperiment API for each deployment + namespace. Documentation still in progress stay
tuned.

# Attributes

* experiment_name \
  A unique string name is specified for identifying individual experiments.
* deployment_name \
  Stay tuned
* namespace \
  Stay tuned
* performance_profile \
  `performance_profile` is a pre-defined performance objective that is baked into Kruize.


`performance_profile` consists of two parts:

* A yaml file defines the metrics to be monitored and the queries used to derive those metrics.
* Source code that defines the performance objective using the metrics provided

Currently we have the following pre-defined profiles

| Profile Name                           | yaml                                        | Associated Source | Comments                                       |
|----------------------------------------|---------------------------------------------|-------------------|------------------------------------------------|
| resource-optimization-openshift        | [resource-optimization-openshift yaml](https://github.com/kruize/autotune/blob/master/manifests/autotune/performance-profiles/resource_optimization_openshift.yaml)        | [ResourceOptimizationOpenshiftImpl.java](https://github.com/kruize/autotune/blob/master/src/main/java/com/autotune/analyzer/performanceProfiles/PerformanceProfileInterface/ResourceOptimizationOpenshiftImpl.java)            | This is used for Remote Monitoring Usecase     |
| resource-optimization-local-monitoring | [resource-optimization-local-monitoring yaml](https://github.com/kruize/autotune/blob/master/manifests/autotune/performance-profiles/resource_optimization_local_monitoring.yaml) | [ResourceOptimizationOpenshiftImpl.java](https://github.com/kruize/autotune/blob/master/src/main/java/com/autotune/analyzer/performanceProfiles/PerformanceProfileInterface/ResourceOptimizationOpenshiftImpl.java)            | This is used for Local Monitoring Usecase     |
| default                                | NA (User defined SLO)                       | [source](https://github.com/kruize/autotune/blob/master/src/main/java/com/autotune/analyzer/performanceProfiles/PerformanceProfileInterface/DefaultImpl.java)            | This is applicable to all to Autotune Usecases |


# Response

* Success

```
{
    "message": "Experiment registered successfully with Kruize. View registered experiments at /listExperiments",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

* Failure
  * Duplicate Experiment name.
  ```
  {
    "message": "Experiment name already exists",
    "httpcode": 409,
    "documentationLink": "",
    "status": "ERROR"
  }
  ```
  * Mandatory parameters are missing.
  ```
  {
    "message": "Mandatory parameters missing.",
    "httpcode": 400,
    "documentationLink": "",
    "status": "ERROR"
  }
  ```
  * Any unknown exception on server side
  ```
  {
    "message": "Internal Server Error",
    "httpcode": 500,
    "documentationLink": "",
    "status": "ERROR"
  }
  ```
