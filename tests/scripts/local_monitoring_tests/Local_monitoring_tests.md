# **Kruize Local monitoring tests**

Kruize Local monitoring tests validates the behaviour of [Kruize local monitoring APIs](/design/KruizeLocalAPI.md)
using various positive and negative scenarios. These tests are developed using pytest framework.

## Tests description
### **List Datasources API tests**

Here are the test scenarios:
- List all datasources
- List datasources with name query parameter:
    - /datasources?name=<datasource_name>
- List datasources with invalid parameter value for datasource name tested with empty, NULL or invalid.

### **Import Metadata API tests**

Here are the test scenarios:

- Importing metadata for a valid datasource to the API.
- Post the same datasource again
- Test with invalid values such as blank, null or an invalid value for various keys in the dsmetadata input request json
- Validate error messages when the mandatory fields are missing
- Repeated metadata imports for the same datasource by dynamically creating and deleting namespaces between two metadata
  import actions by validating the newly created namespaces by verifying listing metadata output after the second import
  metadata invocation
- Repeated metadata imports without datasource connection by dynamically scaling down prometheus resources to zero replicas
  and validating the behaviour of import metadata resulting in an error when datasource cannot be connected. Additionally,
  to verify the output of list metadata returns the metadata from the DB after the second import metadata invocation.

### **List Metadata API tests**

Here are the test scenarios:

- List dsmetadata specifying a valid datasource
- List dsmetadata for a datasource with parameters by specifying the following parameters:
  -   /dsmetadata?datasource=<datasource_name>&verbose=false
  -   /dsmetadata?datasource=<datasource_name>&verbose=true
  -   /dsmetadata?datasource=<datasource_name>&cluster_name=<cluster_name>&verbose=false
  -   /dsmetadata?datasource=<datasource_name>&cluster_name=<cluster_name>&verbose=true
  -   /dsmetadata?datasource=<datasource_name>&cluster_name=<cluster_name>&namespace=<namespace_name>&verbose=false
  -   /dsmetadata?datasource=<datasource_name>&cluster_name=<cluster_name>&namespace=<namespace_name>&verbose=true
- List dsmetadata with invalid parameter values for datasource, cluster_name and namespace
  -   Non-existing datasource
  -   Non-existing cluster_name
  -   Non-existing namespace
- List dsmetadata without specifying any parameters
- List dsmetadata after creating a datasource but without importing metadata
- List dsmetadata with datasource and namespace but without cluster_name
- List the dsmetadata after deleting imported metadata

### **Create Metric Profile API tests**

Here are the test scenarios:

- Create metric profile passing a valid input JSON payload with all the metric queries 
- Post the same metric profile again - creating it twice and validate the error as metric profile name is a unique field 
- Create multiple valid metric profiles using different jsons
- Create Metric profile missing mandatory fields and validate error messages when the mandatory fields are missing


### **List Metric Profile API tests**

Here are the test scenarios:

- List metric profiles without specifying any query parameters
- List metric profiles specifying profile name query parameter
- List metric profiles specifying verbose query parameter
- List metric profiles specifying profile name and verbose query parameters
- Test with invalid values such as blank, null or an invalid value for name query parameter in listMetricProfiles API
- List metric profiles without creating metric profile


### **Create Metadata Profile API tests**

Here are the test scenarios:

- Create metadata profile passing a valid input JSON payload with all the metadata queries
- Post the same metadata profile again - creating it twice and validate the error as metadata profile name is a unique field
- Create multiple valid metadata profiles using different jsons
- Create metadata profile missing mandatory fields and validate error messages when the mandatory fields are missing


### **List Metadata Profile API tests**

Here are the test scenarios:

- List metadata profiles without specifying any query parameters
- List metadata profiles specifying profile name query parameter
- List metadata profiles specifying verbose query parameter
- List metadata profiles specifying profile name and verbose query parameters
- Test with invalid values such as blank, null or an invalid value for name query parameter in listMetadataProfiles API
- List metadata profiles without creating metadata profile

### **Update Metadata Profile API tests**

Here are the test scenarios:

- Update a metadata profile with a valid json using the API, list and validate the metadata profiles before and after 
calling /updateMetadataProfile API and validate the updated queries using /dsmetadata API.
- Update metadata profile twice using the API, list and validate the updated profiles and validate the updated queries using /dsmetadata API. Checks if different workloads can be imported by updating the MetadataProfile with corresponding workload details.
- Update metadata profile with a valid json using the API, omitting required `name` in the query parameter. Verifies that the API returns an error when a profile update is attempted with a valid JSON body but omits the required `name` query parameter.
- Update metadata profile by passing invalid `name` query parameter: Empty, NULL, invalid profile names. Verifies that the `name` parameter cannot be invalid when a profile update is attempted with a valid JSON body.
- Update metadata profile with an invalid json missing the mandatory fields, validate for appropriate error message. Ensures the API returns a specific validation error when the request's JSON payload is missing mandatory fields.
- Update metadata profile with a valid json using the API but pass invalid query parameter other than `name`. Checks that the API correctly rejects requests that use an invalid query parameter other than the accepted `name` parameter.
- Update metadata profile with mismatch in profile names of input JSON payload, query parameter and validate for appropriate error message. Ensuring that the `name` field and input parameter match for successfully updating the MetadataProfile.


### **Delete Metadata Profile API tests**

Here are the test scenarios:

- Delete metadata profile passing a valid `name` parameter.
- Delete metadata profile by missing `name` query parameter. Verifies that the API returns an error when a profile is attempted to delete without the `name` query parameter.
- Delete metadata profile by passing invalid `name` query parameter: Empty, NULL, invalid profile names. Verifies that the `name` parameter cannot be invalid to successfully delete the profile.
- Multiple delete attempts: Delete same metadata profile multiple times and validate the expected error message, ensuring redundant delete requests are handled gracefully.
- Delete metadata profile, try to import cluster metadata and validate the expected error message. This testcase ensures that the cluster metadata cannot be imported once the MetadataProfile is deleted.

### **Create Layer API tests**

Here are the test scenarios:

#### Positive Test Scenarios:
- Create layer with different tunable types:
  - Bounded tunables (lower_bound, upper_bound, step)
  - Categorical tunables (choices list)
  - Mixed tunables (combination of both)
- Create layer with different layer_presence configurations:
  - presence='always' (always applicable)
  - queries (query-based detection using Prometheus/datasource queries)
  - label (label-based detection using Kubernetes labels)
- Create layer with minimum required fields

#### Negative Test Scenarios:

**A. Mandatory Fields Missing/NULL/Empty**
- Create layer with null metadata.name
- Create layer with empty metadata.name
- Create layer with null layer_name
- Create layer with empty layer_name
- Create layer with null layer_presence
- Create layer with null tunables
- Create layer with empty tunables array

**B. Invalid/Negative/Duplicate Values**
- Create layer with negative layer_level value
- Create layer with duplicate tunable names
- Create layer with duplicate layer name (attempting to create same layer twice)

**C. Wrong layer_presence Combinations**
- Create layer with empty layer_presence (no type specified)
- Create layer with both presence AND queries specified
- Create layer with both presence AND label specified
- Create layer with both queries AND label specified
- Create layer with all three types specified (presence, queries, label)

**D. Tunable Bounds/Step Validation**
- Create layer with tunable having null upper_bound
- Create layer with tunable having null lower_bound
- Create layer with tunable having non-numeric upper_bound
- Create layer with tunable having non-numeric lower_bound
- Create layer with tunable having null step
- Create layer with tunable having zero step
- Create layer with tunable having negative step
- Create layer with tunable having negative upper_bound
- Create layer with tunable having negative lower_bound
- Create layer with tunable where lower_bound >= upper_bound
- Create layer with tunable where step > (upper_bound - lower_bound)

**E. Categorical Tunable Validation**
- Create layer with categorical tunable having null choices
- Create layer with categorical tunable having empty choices array
- Create layer with categorical tunable having both choices and bounds (mixed configuration)

### **List Layers API tests**

Here are the test scenarios:

- List all layers without specifying any query parameters
- List specific layer by name using query parameter
- List layers with invalid layer name (non-existing layer)
- List layers before creating any layers
- Validate layer response structure and field values

### **Create Experiment API tests**

Here are the test scenarios:

- Create namespace experiment specifying namespace experiment type
- Create namespace experiment without specifying experiment type
- Create container experiment specifying container experiment type
- Create container experiment without specifying experiment type
- Create experiment specifying both namespaces and containers without specifying the experiment type
- Create experiment specifying both namespaces and containers specifying the namespace experiment type
- Create experiment specifying both namespaces and containers specifying the container experiment type
- Create namespace experiment specifying containers 
- Create container experiment specifying namespaces
- Create multiple experiments with valid namespace 
- Create container experiment without specifying experiment_type and with auto mode 
- Create container experiment without specifying experiment_type and with recreate mode 
- Create container experiment specifying experiment_type and auto mode 
- Create container experiment specifying experiment_type and recreate mode 
- Create namespace experiment with auto mode 
- Create namespace experiment with recreate mode 
- Create container experiment specifying experiment_type, target_cluster as remote and mode as recreate 
- Create container experiment specifying experiment_type, target_cluster as remote and mode as auto 
- Create container experiment without specifying experiment_type, target_cluster as remote and mode as recreate
- Create container experiment without specifying experiment_type, target_cluster as remote and mode as auto
- Create container experiment with auto mode with invalid kubernetes object type 
- Create container experiment with recreate mode with invalid kubernetes object type

### **List Recommendations API tests**

Here are the test scenarios:

- List recommendations for a valid namespace experiment


The above tests are developed using pytest framework and the tests are run using shell script wrapper that does the following:
- Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
- Creates a resource optimization metric profile using the [createMetricProfile API](/design/MetricProfileAPI.md)
- Creates cluster metadata profile using the [createMetadataProfile API](/design/MetadataProfileAPI.md)
- Runs the above tests using pytest

### **Bulk API tests**
This test script validates the functionality of a bulk POST [API](/design/MonitoringModeAPI.md) and associated GET job status API for a Kubernetes resource optimization service, focusing on the creation and monitoring of job IDs.

Here are the test scenarios:
- Validate the bulk POST API's ability to generate a job_id for given payloads.
  - Empty Payload: Ensures a job_id is generated even when no data is sent.
  - Sample JSON Payload: Verifies the API correctly processes a structured payload and generates a job_id.
- Verify the response of the GET job status API for the generated job_id.
  - Tests both verbose=false and verbose=true GET requests for comprehensive verification.
- Validate bulk API response by passing a valid and multiple invalid time range values.
  - Job_id will be generated in case of the valid scenario
  - Corresponding error message will be sent back in case of invalid scenario with Response code 400.
- Validate bulk API response by passing an invalid datasource name in the input JSON.
  - Error message will be sent back with the response code 400

### Runtime Recommendations Test:

Kruize supports Runtime Recommendations for JVM workloads (OpenJDK/Hotspot and Semeru/OpenJ9). The recommendations provide GC policy and related JVM options (e.g., `-XX:+UseG1GC`) as `JDK_JAVA_OPTIONS` or `JAVA_OPTIONS` environment variables.

The test `test_generate_recommendation.py::test_runtime_recommendation` validates that runtime recommendations are generated for JVM workloads when the necessary metrics and layers are available.

#### Prerequisites to run the test:

In addition to the pre-requisites mentioned above:

- A JVM workload (e.g., TechEmpower Quarkus `tfb-qrh-sample` or Spring Petclinic) must be running and exposing `jvm_info` (and optionally `jvm_memory_max_bytes`) Prometheus metrics
- Kruize layers (hotspot, semeru) must be loaded via createLayer API
- The performance profile must include `jvmInfo`, `jvmInfoTotal` and `jvmMemoryMaxBytes` metrics (as in `resource_optimization_local_monitoring.json`)

#### Test scenarios for runtime recommendations:

- Hotspot GC policy (OpenJDK/Hotspot): JVM workload with OpenJDK or Hotspot runtime and layer `hotspot` present. 
  - **Expected**: `JDK_JAVA_OPTIONS` or `JAVA_OPTIONS` contains appropriate GC flags (e.g., `-XX:+UseG1GC` for G1)

The following tests are yet to be added:

- Semeru GC policy (Semeru/OpenJ9): JVM workload with Semeru or OpenJ9 runtime and layer `semeru` present. 
  - **Expected**: `JAVA_OPTIONS` contains `-Xgcpolicy:gencon`, `-Xgcpolicy:balanced`, or `-Xgcpolicy:optthruput`
- Missing JVM metadata: Workload without `jvm_info` metrics or layer presence. 
  - **Expected**: No runtime recommendations; `env` absent or empty
- Missing JVM version: `jvm_info` present but version label missing. 
  - **Expected**: No GC recommendation (null version handling)
- Layer vs runtime mismatch: Layer name does not match effective runtime (e.g., hotspot layer for OpenJ9 workload). 
  - **Expected**: No recommendation for mismatched layer
- JVM heap from jvmMemoryMaxBytes: `jvm_memory_max_bytes` metric available. 
  - **Expected**: Heap size used for GC policy decision (e.g., G1 vs ZGC for large heaps)
- Non-runtime datasource: Datasource that does not support runtime recommendations. 
  - **Expected**: API succeeds; server logs `RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE`

Note: The test will skip or may not assert runtime recommendations if the workload does not expose JVM metrics or layers are not configured. Adjust workload name and namespace to match your JVM deployment.


### **Autoscaler tests**
This test script validates the functionality of the Vertical Pod Autoscaler (VPA) integration with Kruize, 
ensuring that the workflow operates as expected from creation to recommendation patching.

Here are the test scenarios:
- Validate VPA object creation 
  - Ensure that a VPA object is created successfully with the expected name.
  - Ensure that VPA object contains correct containers
  - Validate VPA object is patched with recommendations
  - Ensure that the VPA object contains appropriate resource recommendations for each container, 
including CPU and memory recommendations.


## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq, python
- Various python modules pytest, json, pytest-html, requests, jinja2
  (these modules will be automatically installed while the test is run)

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube -r [location of benchmarks]  [-i kruize image] [--tctype=functional] [--testmodule=Autotune module to be tested] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to test] [-n namespace] [--resultsdir=results directory] [--skipsetup]
```

Where values for test_autotune.sh are:

```
usage: test_autotune.sh [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
                        [ -i ] : optional. Kruize docker image to be used for testing, default - kruize/autotune_operator:test
			[ -r ] : Location of benchmarks. Not required for local_monitoring_tests
			[ --tctype ] : optional. Testcases type to run, default is functional (runs all functional tests)
			[ --testmodule ]: Module to be tested. Use testmodule=help, to list the modules to be tested
			[ --testsuite ] : Testsuite to run. Use testsuite=help, to list the supported testsuites
			[ --testcase ] : Testcase to run. Use testcase=help along with the testsuite name to list the supported testcases in that testsuite
			[ -n ] : optional. Namespace to deploy autotune
			[ --resultsdir ] : optional. Results directory location, by default it creates the results directory in current working directory
			[ --skipsetup ] : optional. Specifying this option skips the Kruize setup and performance profile creation in case of local_monitoring_tests

Note: If you want to run a particular testcase then it is mandatory to specify the testsuite
Test cases supported are sanity, negative, extended and test_e2e

```

To run all the local monitoring tests,

```
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=local_monitoring_tests --resultsdir=/home/results
```

To run only the sanity local monitoring tests,

```
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=local_monitoring_tests --testcase=sanity --resultsdir=/home/results
```

Local monitoring tests can also be run without using the test_autotune.sh. To do this, follow the below steps:

- Deploy Kruize using the deploy.sh from the kruize autotune repo
- Create the metric profile by using the [createMetricProfile API](/design/MetricProfileAPI.md)
- Create the metadata profile by using the [createMetadataProfile API](/design/MetadataProfileAPI.md)
- cd <KRUIZE_REPO>/tests/scripts/local_monitoring_tests
- python3 -m pip install --user -r requirements.txt
- cd rest_apis
- To run all sanity tests
```
	pytest -m sanity --html=<dir>/report.html --cluster_type <minikube|openshift>
```
- To run only sanity tests for List datasources API --cluster_type <minikube|openshift>
```
	pytest -m sanity --html=<dir>/report.html test_list_datasources.py
```
- To run only a specific test within List datasources API
```
	pytest -s test_list_datasources.py::test_list_datasources_with_name --cluster_type <minikube|openshift>
```

Note: You can check the report.html for the results as it provides better readability


### Accelerator Test:

Kruize 0.1 supports the Accelerator Recommendations which provide right sized MIG config as recommendations.

The test `test_list_recommendations.py::test_accelerator_recommendation_if_exists` is created to check if the accelerator recommendations are in expected format.

#### Prerequisites to run the test:

In addition to the pre-requisites mentioned above we need to make sure that a workload with name `human-eval-benchmark` is running in the namespace `unpartitioned` and has the accelerator usage data.

Check this out for running the benchmark: [How to run the human eval benchmark?](https://github.com/kruize/benchmarks/tree/master/human-eval-benchmark)

Else, you can change the workload name and namespace name in the test to match with your workload.


Note: The test will fail if it's run as is if there are no matching workloads that the test looks for. This test result can be ignored in case of a non-gpu workload

### Authentication Test:

Kruize 0.2 supports the authentication which provides the user an option to pass authentication details in the yaml for the service they are using.

The authentication test is part of functional bucket and has a separate script similar to local_monitoring tests. It contains various valid and invalid scenarios for testing.

It can be run as shown in the example below:

`/test_autotune.sh -c <cluster-type> -i <image-name> -r benchmarks/ --testsuite=authentication_tests`

#### Scenarios
**_valid_**: a valid path to the token

**_expired_**: an expired token value

**_invalid_**: an invalid path to the token

**_empty_**: a blank input in place of the token file path

### Datasource Availability/Serviceability Test:

Kruize supports multiple datasources such as Prometheus and Thanos Querier. During startup, Kruize validates the reachability of all configured datasources before proceeding.

The datasource availability/serviceability test is part of the functional test bucket and is implemented as a standalone shell script, similar to the authentication tests.
It validates Kruize behavior when one or more datasources are reachable or unreachable.

Kruize startup behavior follows these rules:

* Kruize continues startup if at least one datasource is reachable.
* Kruize logs an error for each unreachable datasource.
* Kruize fails startup only when all configured datasources are unreachable.

The test can be run using the command below:

```
./test_autotune.sh -c <cluster-type> -i <image-name> -r benchmarks/ --testsuite=datasource_tests
```

#### Scenarios

**both-valid**

Both Prometheus and Thanos Querier datasources are reachable.

**✔ Expected:** Kruize starts successfully.

**prom-valid-thanos-invalid**

Prometheus is reachable and Thanos Querier is unreachable.

**✔ Expected:** Kruize starts successfully and logs an error for Thanos.

**prom-invalid-thanos-valid**

Prometheus is unreachable and Thanos Querier is reachable.

**✔ Expected:** Kruize starts successfully and logs an error for Prometheus.

**both-invalid**

Both Prometheus and Thanos Querier datasources are unreachable.

**❌ Expected:** Kruize fails to start and exits with an error.
