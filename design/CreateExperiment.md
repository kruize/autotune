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
* recommendation_settings.resource_scope_settings \
  An optional object that limits which recommendations are generated, based on whether the workload's CPU and memory
  are under-provisioned, over-provisioned, optimised or idle. See [Resource scope settings](#resource-scope-settings).
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

## Resource scope settings

`resource_scope_settings` sits under `recommendation_settings` and has two optional keys, `cpu` and `memory`. Each is a
list of the provisioning states for which a recommendation is kept.

```json
"recommendation_settings": {
  "threshold": "0.1",
  "resource_scope_settings": {
    "cpu":    ["under_provisioned", "idle"],
    "memory": ["over_provisioned"]
  }
}
```

| Resource | Valid scopes                                                      | Notes                                            |
|----------|-------------------------------------------------------------------|--------------------------------------------------|
| `cpu`    | `under_provisioned`, `over_provisioned`, `optimised`, `idle`, `all` | `idle` means CPU usage below 1 millicore         |
| `memory` | `under_provisioned`, `over_provisioned`, `optimised`, `all`         | Memory has no idle state, so `idle` is rejected  |

Values are case-insensitive. `optimized` is accepted as an alias for `optimised`.

**How a state is decided.** The state comes from the variation of the recommended *requests* against the current
requests (limits are derived from requests and are not used). A variation within the experiment's `threshold` counts as
`optimised`, a positive variation as `under_provisioned` and a negative one as `over_provisioned`. A CPU that is idle is
`idle` whatever its variation.

**How scopes are combined.** A recommendation is kept only if every resource you specify matches (AND). A resource key
you leave out is unconstrained. `all` matches any state, so it switches filtering off for that resource.

| `resource_scope_settings`                          | Recommendation is kept when                                   |
|----------------------------------------------------|---------------------------------------------------------------|
| `{"memory": ["under_provisioned"]}`                | memory is under-provisioned, whatever the CPU state           |
| `{"cpu": ["over_provisioned"]}`                    | CPU is over-provisioned, whatever the memory state            |
| `{"cpu": ["idle"], "memory": ["over_provisioned"]}`| CPU is idle and memory is over-provisioned                    |
| `{"cpu": ["all"], "memory": ["all"]}`              | always (no filtering)                                         |

**Defaults.** When `resource_scope_settings` is absent, each model applies its own default, so existing experiments
behave as before:

| Model                    | Default                                                   |
|--------------------------|-----------------------------------------------------------|
| `cost`, `performance`    | `cpu: ["all"]`, `memory: ["all"]` (no filtering)          |
| `stability`              | `cpu: ["under_provisioned"]`, `memory: ["under_provisioned"]` |

The filter is applied per model and per term. A model whose recommendation is outside the scope is left out of that
term. If every model is filtered out for every term, no recommendation is available and the response carries the
"not enough data" notification, the same as when there is no data yet.

**Validation.** The request is rejected with HTTP 400 for an empty `resource_scope_settings` object, an empty list for
`cpu` or `memory`, an unknown scope, or `idle` under `memory`. `all` combined with other scopes is accepted and a
warning is logged, as the other values are redundant.

**Limitations.**
* Idle detection relies on the CPU idle notification (code 323001) of the model's recommendation.
* If the current request of a container is not set, the variation equals the full recommended value and the resource is
  classified as `under_provisioned`.


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
