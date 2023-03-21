# Create Performance Profile

Client will invoke the Kruize createPerformanceProfile API for each deployment. Documentation still in progress stay
tuned.

# Attributes

- **name** \
  A unique string name for identifying each performance profile.
- **profile_version** \
  a double value specifying the current version of the profile.
- **slo** \
  Service Level Objective containing the _direction_, _objective_function_ and _function_variables_
  - **slo_class** \
    a standard slo "bucket" defined by Kruize. Can be "_resource_usage_", "_throughput_" or "_response_time_"
  - **direction** \
    based on the slo_class, it can be '_maximize_' or '_minimize_'
  - **objective_function** \
    Define the performance objective here.
    - **function_type** \
      can be specified as '_source_' (a java file) or as an '_expression_'(algebraic). If it's an expression, it needs to defined below.
    - **expression** \
      an algebraic expression that details the calculation using function variables. Only valid if the "_function_type_" is "expression"
  - **function_variables** \
    Define the variables used in the _objective_function_
    - **name** \
      name of the variable
    - **datasource** \
      datasource of the query
    - **value_type** \
      can be double or integer
    - **query** \
      one of the query or _aggregation_functions_ is mandatory. Both can be present.
    - **kubernetes_object** \
      k8s object that this query is tied to: "_deployment_", "_pod_" or "_container_"
    - **aggregation_functions** \
      aggregate functions associated with this variable
      - **function** \
        can be '_avg_', '_sum_', '_min_', '_max_'
      - **query** \
        corresponding query 
      - **versions** \
        Any specific versions that this query is tied to 
      
    

# Response

* Success

```
{
    "message": "Performance Profile : <name> created successfully. View all performance profiles at /listPerformanceProfiles",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

* Failure
    * Duplicate Performance Profile name.
  ```
  {
  "message": "Performance Profile already exists",
  "httpcode": 409,
  "documentationLink": "",
  "status": "ERROR"
  }
  ```
    * Mandatory attribute value are missing.
  ```
  {
  "message": "Missing mandatory parameters",
  "httpcode": 400,
  "documentationLink": "",
  "status": "ERROR"
  }
  ```
  * Server Error
  ```
  {
  "message": "Internal Server Error",
  "httpcode": 500,
  "documentationLink": "",
  "status": "ERROR"
  }
  ```
* #####  You can get the API details [here](/design/PerformanceProfileAPI.md)
