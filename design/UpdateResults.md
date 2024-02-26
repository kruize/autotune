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
      "message": "Not Found: experiment_name does not exist: <experiment-name>",
      "httpcode": 400,
      "documentationLink": "",
      "status": "ERROR"
  }
  ```
    * Duplicate Experiment name and Timestamp.
  ```
  {
      "message": "An entry for this record already exists!",
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
    * Invalid Input JSON Format.
      * For example,  `interval_start_time` or `interval_end_time` is not following the correct UTC format or it's blank:
      ```
        {
            "message": "Failed to parse the JSON. Please check the input payload ",
            "httpcode": 400,
            "documentationLink": "",
            "status": "ERROR"
        }
      ```
    * Parameters Mismatch
      * `version` name passed is different from what was passed while creating the corresponding experiment:
      ```
      {
        "message": "Version number mismatch found. Expected: <existing-version> , Found: <new-version>",
        "httpcode": 400,
        "documentationLink": "",
        "status": "ERROR"
      }
      ```
      * `namespace` in the `kubernetes_objects` is different from what was passed while creating the corresponding experiment:
      ```
      {
        "message": "kubernetes_objects : Kubernetes Object Namespaces MisMatched. Expected Namespace: <existing-ns>, Found: <new-ns> in Results for experiment: <experiment-name> ",
        "httpcode": 400,
        "documentationLink": "",
        "status": "ERROR"
      }
      ```
      * Similar response will be returned for other parameters when there is a mismatch.
    * Invalid Metric variable names.
  ```
    {
        "message": "Performance profile: [Metric variable name should be among these values: [cpuRequest, cpuLimit, cpuUsage, cpuThrottle, memoryRequest, memoryLimit, memoryUsage, memoryRSS] for container : <container-name> for experiment: <exp-name>]",
        "httpcode": 400,
        "documentationLink": "",
        "status": "ERROR"
    }
  ```
    * Invalid Aggregation_Info Format
  ```
    {
      "message": "Performance profile: [ Format value should be among these values: [GiB, Gi, Ei, KiB, E, MiB, G, PiB, K, TiB, M, P, Bytes, cores, T, Ti, MB, KB, Pi, GB, EB, k, m, TB, PB, bytes, kB, Mi, Ki, EiB] for container : <container-name> for experiment: <exp-name>]",
      "httpcode": 400,
      "documentationLink": "",
      "status": "ERROR"
    }
  ```
    * Invalid Aggregation_Info Values
  ```
    {
      "message": "Invalid value type for aggregation_info objects. Expected a numeric value (Double).",
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
