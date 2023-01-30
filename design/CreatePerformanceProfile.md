# Create Performance Profile

Client will invoke the Kruize createPerformanceProfile API for each deployment. Documentation still in progress stay
tuned.

# Attributes

* name \
  A unique string name for identifying each performance profile.
* profile_version \
  a double value specifying the current version of the profile.
* slo \
  Service Level Objective containing the direction, objective_function and function_variables
  * direction \
    to be updated
  * objective_function \
    to be updated
  * function_variables \
    to be updated
    * name \
      to be updated
    * value_type \
      to be updated
    * query \
      to be updated
    * aggregation_functions \
      to be updated
      * function \
        to be updated
      * query \
        to be updated
      
    

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
  "message": "Performance Profile already exsist with Kruize. View all profiles at /listPerformanceProfiles",
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