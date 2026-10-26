# Create Experiment

Client will invoke the Kruize createExperiment API for each deployment + namespace. Documentation still in progress stay
tuned.

# Attributes

* experiment_name \
  A unique string name is specified for identifying individual experiments.
* experiment_type \
  An optional string used to indicate whether the experiment is of type `namespace` or `container`. If no experiment type is specified, it will default to `container`.
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


## recommendation_settings

An optional object that controls which terms and models are used to generate recommendations.
If omitted, Kruize uses the default set: `short`, `medium`, and `long` terms with `cost` and `performance` models.

### Supported terms

| Term value | Description                              | Default |
|------------|------------------------------------------|---------|
| `short`    | 1-day window, 30-min minimum threshold   | Yes     |
| `medium`   | 7-day window, 2-day minimum threshold    | Yes     |
| `long`     | 15-day window, 8-day minimum threshold   | Yes     |
| `flex`     | Up to 15-day window, 30-min minimum threshold, opt-in only | No |

### Supported models

| Model value   | Description                                                  | Default |
|---------------|--------------------------------------------------------------|---------|
| `cost`        | 60th CPU percentile — minimise resource spend                | Yes     |
| `performance` | 98th CPU percentile — maximise reliability                   | Yes     |
| `stability`   | 98th CPU percentile (Phase 1 static), opt-in only, paired exclusively with `flex` | No |

### flex + stability opt-in request

`stability` and `flex` must always be specified together. Neither appears by default.

```json
"recommendation_settings": {
  "term_settings": {
    "terms": ["flex"]
  },
  "model_settings": {
    "models": ["stability"]
  }
}
```

### Validation rules for flex and stability

| Rule | Condition | Error |
|------|-----------|-------|
| V-1  | `stability` model paired with any term other than `flex` | `"stability profile only supports the flex term."` |
| V-2  | `flex` term paired with any model other than `stability` | `"flex term only supports the stability profile."` |
| V-3  | `flex` term appears alongside any other term in the same request | `"flex term cannot be combined with other terms."` |

**V-1 example** — `stability` with `short`:
```json
"recommendation_settings": {
  "term_settings": { "terms": ["short"] },
  "model_settings": { "models": ["stability"] }
}
```
Response:
```json
{
  "message": "stability profile only supports the flex term.",
  "httpcode": 400,
  "status": "ERROR"
}
```

**V-2 example** — `flex` with `cost`:
```json
"recommendation_settings": {
  "term_settings": { "terms": ["flex"] },
  "model_settings": { "models": ["cost"] }
}
```
Response:
```json
{
  "message": "flex term only supports the stability profile.",
  "httpcode": 400,
  "status": "ERROR"
}
```

**V-3 example** — `flex` combined with `short`:
```json
"recommendation_settings": {
  "term_settings": { "terms": ["flex", "short"] },
  "model_settings": { "models": ["stability"] }
}
```
Response:
```json
{
  "message": "flex term cannot be combined with other terms.",
  "httpcode": 400,
  "status": "ERROR"
}
```

---

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
