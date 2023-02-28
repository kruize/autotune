# Create Experiment

Client will invoke the Kruize createExperiment API for each deployment + namespace. Documentation still in progress stay
tuned.

# Attributes

* `version` \
  API version
* `experiment_name` \
  A unique string name is specified for identifying individual experiments.
* `kubernetes_objects` \
  List of kubernetes objects (of type `Deployment`, `Replicaset`, `Statefulset` etc)
* `cluster_name` \
  Name of the cluster
* `performance_profile` \
  Name of the performance profile you want apply
* `mode` \
  Mode of the experiment (Whether it is a `Monitoring` or an `Experiment`)
* `target_cluster` \
  Type of the cluster to target (If it's a `local` or `remote` cluster)
* `trial_settings` \
  Settings for the trial
* `recommendation_settings` \
  Settings for the recommendation

# Example input JSON

```
[
  {
    "version": "1.0",
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
    "cluster_name": "cluster-one-division-bell",
    "performance_profile": "resource-optimization-openshift",
    "mode": "monitor",
    "target_cluster": "remote",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment",
        "namespace": "default",
        "containers": [
          {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          },
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          }
        ]
      }
    ],
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    }
  }
]
```
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
  "message": "Experiment name already exsist with Kruize. View registered experiments at /listExperiments",
  "httpcode": 400,
  "documentationLink": "",
  "status": "FAILED"
  }
  ```
    * Mandatory attribute value are missing.
  ```
  {
  "message": "Mandatory attributes value missing.",
  "httpcode": 400,
  "documentationLink": "",
  "status": "FAILED"
  }
  ```