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