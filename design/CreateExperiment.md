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
  Performance profile is a setting which specifies the SLO related information like function variables and aggregration functions. Here we specify the name of already existing Performance profile. If we create a new performance profile we need to map it with appropriate Recommendation Engine Module for Kruize to generate recommendations. \
   \
  Note: Recommendations will not be generated if the newly created Performance Profile is not mapped with a Recommendation Engine. Please refer `perfProfileInstances` in `src/main/java/com/autotune/analyzer/utils/AnalyzerConstants.java` to check how it's mapped.

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
