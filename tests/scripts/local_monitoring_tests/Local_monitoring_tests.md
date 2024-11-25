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

### **List Recommendations API tests**

Here are the test scenarios:

- List recommendations for a valid namespace experiment


The above tests are developed using pytest framework and the tests are run using shell script wrapper that does the following:
- Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
- Creates a resource optimization metric profile using the [createMetricProfile API](/design/MetricProfileAPI.md)
- Runs the above tests using pytest

### **Bulk API tests**
This test script validates the functionality of a bulk POST [API](/design/MonitoringModeAPI.md) and associated GET job status API for a Kubernetes resource optimization service, focusing on the creation and monitoring of job IDs.

Here are the test scenarios:
- Validate the bulk POST API's ability to generate a job_id for given payloads.
  - Empty Payload: Ensures a job_id is generated even when no data is sent.
  - Sample JSON Payload: Verifies the API correctly processes a structured payload and generates a job_id.
- Verify the response of the GET job status API for the generated job_id.
  - Tests both verbose=false and verbose=true GET requests for comprehensive verification.

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
- Create the performance profile by using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md)
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

