# Update Metric results.

Client will invoke the Kruize updateResults API for each experiment. Documentation still in progress stay
tuned.

# Attributes

- **experiment_name** \
  A unique string name is specified for identifying individual experiments.
- **interval_start_time** \
  Timestamp to be specified in which metrics collection started at remote cluster side.
- **interval_end_time** \
  Timestamp to be specified in which metrics collection completed at remote cluster side.
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
    - **metrics** \
      metric variable name and result details to be specified
      - **name** \
        name of the metric variable. It can include `cpuRequest`, `cpuLimit`, `cpuUsage`, `cpuThrottle`, `memoryRequest`, 
        `memoryLimit`, `memoryUsage`, `memoryRSS`, etc
      - **results** \
        includes result details like `value`, `format` and `aggregation_info`
        - **value** \
          value of the result is specified as a `double`
        - **format** \
          format of the result value is specified. For CPU, it should be `cores` and for memory it should be `MiB`
        - **aggregation_info** \
          the aggregation functions associated with this metric variable is specified. It can be `sum`, `avg`, `max`, `min`, etc.

# Response

* Success

```
{
    "message": "Updated metrics results successfully with Autotune. View update results at /listExperiments",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

* Failure
    * Experiment name not found.
  ```
  {
      "message": "Experiment name: <experiment-name> not found",
      "httpcode": 400,
      "documentationLink": "",
      "status": "ERROR"
  }
  ```
    * Duplicate Experiment name and Timestamp.
  ```
  {
      "message": "Experiment name : <experiment-name> already contains result for timestamp : <timestamp>",
      "httpcode": 409,
      "documentationLink": "",
      "status": "ERROR"
  }
  ```
    * Mandatory parameters are missing.
  ```
  {
      "message": "experiment_name and timestamp are mandatory fields.",
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
