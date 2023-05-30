# Create Experiment

Client will invoke the Kruize createExperiment API for each deployment + namespace. Documentation still in progress stay
tuned.

# Attributes

### Experiment
- **version** \
  Signifies the API version.
- **experiment_name** \
  A unique string name is specified for identifying individual experiments.
- **cluster_name** \
  Name of the cluster where the app is deployed.

### Performance Objective
- **performance_profile** \
    `performance_profile` is a pre-defined performance objective that is baked into Kruize. 
- **slo** \
  Service Level Objective to be defined in the `performance_profile` as shown in [Performance Profile](https://github.com/kruize/autotune/blob/master/manifests/autotune/performance-profiles/resource_optimization_openshift.yaml) section, or it can be defined here in the experiment JSON and a Default Performance Profile will be created. 
  

 `performance_profile` consists of two parts:

  - A yaml file defines the metrics to be monitored and the queries used to derive those metrics.
  - Source code that defines the performance objective using the metrics provided

Currently, we have the following pre-defined profiles

| Profile Name                           | yaml                                        | Associated Source | Comments                                       |
|----------------------------------------|---------------------------------------------|-------------------|------------------------------------------------|
| resource-optimization-openshift        | [resource-optimization-openshift yaml](https://github.com/kruize/autotune/blob/master/manifests/autotune/performance-profiles/resource_optimization_openshift.yaml)        | [ResourceOptimizationOpenshiftImpl.java](https://github.com/kruize/autotune/blob/master/src/main/java/com/autotune/analyzer/performanceProfiles/PerformanceProfileInterface/ResourceOptimizationOpenshiftImpl.java)            | This is used for Remote Monitoring Usecase     |
| resource-optimization-local-monitoring | [resource-optimization-local-monitoring yaml](https://github.com/kruize/autotune/blob/master/manifests/autotune/performance-profiles/resource_optimization_local_monitoring.yaml) | [ResourceOptimizationOpenshiftImpl.java](https://github.com/kruize/autotune/blob/master/src/main/java/com/autotune/analyzer/performanceProfiles/PerformanceProfileInterface/ResourceOptimizationOpenshiftImpl.java)            | This is used for Local Monitoring Usecase     |
| default                                | NA (User defined SLO)                       | [source](https://github.com/kruize/autotune/blob/master/src/main/java/com/autotune/analyzer/performanceProfiles/PerformanceProfileInterface/DefaultImpl.java)            | This is applicable to all to Autotune Usecases |

***Note: One of Performance Profile name or SLO data needs to be present. Both cannot be present together!***

### Experiment Type
- **mode** \
  It can be either `monitor` or `experiment` 
- **target_cluster** \
   Target cluster can be one of `remote` or `local`

### Experiment Target
- **kubernetes_objects** \
   contains details of the deployment, containers and namespace of the application to be monitored. 
  - **type** \
    A type can either be a `Deployment`, `Replicaset`, `Statefulset`, `Deployment_Config`, `Replication_Controller`, 
    `Daemonset`, etc.
  - **name** \
    A string name is specified for identifying individual deployments. Each deployment can have multiple experiments.
  - **namespace** \
    Kubernetes/Openshift namespace name in which the application is present. E.g: `default`
  - **containers** \
    it's an array containing the details of the different container objects like the container names and the images. 
    - **container_name** \
      Name of the container in which image(s) is deployed. E.g: `tfb-server`
    - **container_image_name** \
      Name of the image deployed in the container. E.g: `kruize/tfb-db:0.1`
- **Selector** \
  stay tuned

### Settings
- **trial_settings** \
    contains details of the trials like the `measurement_duration`
  - **measurement_duration** \
    Value in mins depicting the duration of monitoring. E.g: `15min`  
- **recommendation_settings** \
  contains details of the recommendations like the `threshold value`
  - **threshold** \
    Double value depicting the threshold value of the recommendation. E.g: `0.1`

### Datasource
  stay tuned

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
