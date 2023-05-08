# Create Experiment

Client will invoke the Kruize createExperiment API for each deployment + namespace. Documentation still in progress stay
tuned.

# Attributes

- **version** \
  Signifies the API version.
- **experiment_name** \
  A unique string name is specified for identifying individual experiments.
- **cluster_name** \
  Name of the cluster where the app is deployed.
- **performance_profile** \
  Performance profile name which contains the `SLO` data and the `objective function` data. 
- **mode** \
  It can be either `monitor` or `experiment` 
- **target_cluster** \
   Target cluster can be one of `remote` or `local` 
- **kubernetes_objects** \
   contains details of the deployment, containers and namespace of the application to be monitored. 
  - **type** \
    A type can either be a `Deployment`, `Replicaset`, `Statefulset`, etc.
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
- **trial_settings** \
    contains details of the trials like the `measurement_duration`
  - **measurement_duration** \
    Value in mins depicting the duration of monitoring. E.g: `15min`  
- **recommendation_settings** \
  contains details of the recommendations like the `threshold value`
  - **threshold** \
    Double value depicting the threshold value of the recommendation. E.g: `0.1`

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
