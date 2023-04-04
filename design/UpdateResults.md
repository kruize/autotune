# Update Metric results.

Client will invoke the Kruize updateResults API for each experiment. Documentation still in progress stay
tuned.

# Attributes

* experiment_name \
  A unique string name is specified for identifying individual experiments.
* interval_start_time \
  Stay tuned
* interval_end_time \
  Stay tuned
* kubernetes_objects \
  Stay tuned

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
