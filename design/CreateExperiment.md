# Create Experiment

Client will invoke the Kruize createExperiment API for each deployment + namespace. Documentation still in progress stay
tuned.

# Attributes

### Experiment
- **version** \
  Signifies the API version.<br><br>
- **experiment_name** \
  A unique string name is specified for identifying individual experiments.<br><br>
- **cluster_name** \
  Name of the cluster where the app is deployed.

### Performance Objective
- **performance_profile** \
    `performance_profile` is a pre-defined performance objective that is baked into Kruize. <br><br>
- **slo** \
  Autotune accepts a user provided Service Level Objective or "slo" goal to optimize application performance. It uses Prometheus to identify "layers" of an application that it is monitoring and matches tunables from those layers to the user provided slo. Service Level Objective to be defined in the `performance_profile` as shown in [Performance Profile](https://github.com/kruize/autotune/blob/master/manifests/autotune/performance-profiles/resource_optimization_openshift.yaml) section, or it can be defined here in the experiment JSON and a Default Performance Profile will be created.
<br><br>
  - **objective_function**
  
    specifies a tuning goal in the form of a monitoring system (Eg Prometheus) query. For example, if the overall goal for the IT Admin is to minimize response time of the application deployment, the objective function that defines what exactly constitutes response time will be: `request_sum/request_count`    
    The performance objective can be specified either as an algebraic expression as shown in the example above directly or as a java source file having 
   complex objectives.<br><br>  
    - **type**

      represents the type of objective function. It can be one of `expression` or `source`. `expression` refers to an algebraic expression that details the
   calculation using function variables. `source` means that the calculation is backed by a java source file.<br><br>
  - **function_variables**

    defines the individual variables of the expression used in the objective function.<br><br>
    
    - **name**
    
      name of the variable used in the objective function expression. For example, `request_sum` and `request_count` as shown above.<br><br>   
    - **query**
    
      query used by the datasource like Prometheus to get the value of the individual function variables.<br><br>   
    - **datasource**
      
      represents the source used to get the metrics. It can be `Prometheus`, `Dynatrace`, `Splunk`, etc <br><br>
    - **value_type**
    
      represents the data type of the value being fetched. Supported types are `double` or `integer` <br><br>
  
    - **kubernetes_object**
      
      Any k8s object that this query is tied to. Eg: `deployment`, `pod`, `container`. <br><br>
     
    - **aggregation_functions**
    
      represents aggregate functions and corresponding queries associated with this variable. One of query or aggregation_functions is mandatory. Both can be present together. <br><br>
      
      - **function**
      
        can be one of `avg`, `sum`, `max`, `min`, etc <br><br>
      - **query**
      
        query used to get the value for the corresponding function.

    
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
  It can be either `monitor` or `experiment` <br><br>
- **target_cluster** \
   Target cluster can be one of `remote` or `local`

### Experiment Target
- **kubernetes_objects** \
   contains details of the deployment, containers and namespace of the application to be monitored. 
  - **type** \
    A type can either be a `Deployment`, `Replicaset`, `Statefulset`, `DeploymentConfig`, `ReplicationController`, 
    `Daemonset`, etc. <br><br>
  - **name** \
    A string name is specified for identifying individual kubernetes_objects. <br><br>
  - **namespace** \
    Kubernetes/Openshift namespace name in which the application is present. E.g: `default` <br><br>
  - **containers** \
    it's an array containing the details of the different container objects like the container names and the images.<br><br> 
    - **container_name** \
      Name of the container in which image(s) is deployed. E.g: `tfb-server` <br><br>
    - **container_image_name** \
      Name of the image deployed in the container. E.g: `kruize/tfb-db:0.1`<br><br>
- **Selector** \
  stay tuned

### Settings
- **trial_settings** \
    contains details of the trials like the `measurement_duration` <br><br>
  - **measurement_duration** \
    Value in mins depicting the duration of monitoring. E.g: `15min`  <br><br>
- **recommendation_settings** \
  contains details of the recommendations like the `threshold value`<br><br>
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
