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
| `stability`   | 98th CPU percentile (static), opt-in only                    | No      |

### Requesting flex and stability

`flex` and `stability` are opt-in. Neither is part of the default configuration, so they must be listed explicitly in
`recommendation_settings`. They are intended to be used together, but this is not enforced.

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

In the recommendations response, the term is keyed as `flex_term` and the model as `stability`.

### Validation

Kruize validates each value independently:

| Field      | Accepted values                          | Error for any other value                                          |
|------------|------------------------------------------|--------------------------------------------------------------------|
| `terms`    | `short`, `medium`, `long`, `flex`        | `Term name is not supported. Use short, medium, long or flex term.` (400) |
| `models`   | `cost`, `performance`, `stability`       | `Model name is not supported. Use cost, performance or stability.` (400) |

Empty or whitespace-only term or model values are rejected with `Empty term or model value.` (400).

There is **no cross-validation** between terms and models. `flex` and `stability` are not required to be paired, and
`flex` may be combined with other terms. Every requested model is evaluated for every requested term. For example:

| Request                                   | Result                                                          |
|-------------------------------------------|-----------------------------------------------------------------|
| terms `["flex"]`, models `["stability"]`  | `stability` recommendations for `flex_term`                     |
| terms `["flex"]`, no models               | default models (`cost`, `performance`) for `flex_term`          |
| terms `["short"]`, models `["stability"]` | `stability` recommendations for `short_term`                    |
| terms `["short", "flex"]`, models `["cost", "stability"]` | `cost` and `stability` recommendations for both `short_term` and `flex_term` |

### Defaults and mode restrictions

| Mode                 | No `term_settings`           | No `model_settings`   | Multiple terms / models |
|----------------------|------------------------------|-----------------------|-------------------------|
| `monitor`            | `short`, `medium`, `long`    | `cost`, `performance` | Allowed                 |
| `auto` / `recreate`  | `short`                      | `performance`         | Not allowed (400)       |

In `auto` and `recreate` mode, exactly one term and one model can be given, so `flex` and `stability` can each be
selected as that single term or model. Omitting a setting never defaults to `flex` or `stability`.

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
