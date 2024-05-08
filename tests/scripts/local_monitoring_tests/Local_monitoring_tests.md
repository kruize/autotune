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
- Post an invalid header content type
- Post the same datasource again
- Test with invalid values such as blank, null or an invalid value for various keys in the dsmetadata input request json
- Validate error messages when the mandatory fields are missing

The above tests are developed using pytest framework and the tests are run using shell script wrapper that does the following:
- Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
- Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md)
- Runs the above tests using pytest

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
usage: test_autotune.sh [ -c ] : cluster type. Supported type - minikube
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

