# **Kruize Remote monitoring tests**

Kruize Remote monitoring tests validates the behaviour of [Kruize remote monitoring APIs](/design/MonitoringModeAPI.md) 
using various positive and negative scenarios. These tests are developed using pytest framework. 

## Tests description
### **Create Experiment API tests**
   
Here are the test scenarios:
- Create a single valid experiment json to the API
- Create multiple valid experiments using a single json
- Create multiple valid experiments using different jsons
- Create multiple experiments with the same kubernetes obj type, name and namespace
- Post an experiment with multiple containers with different images & container names
- Test with invalid values such as blank, null or an invalid value for various keys in the createExperiment json
- Post an invalid header content type
- Post the same experiment again
- Post experiments with conflicting parameters
- PerformanceProfile & Slo
- Deployment name & selector
- Validate error messages when the mandatory fields are missing

### **Update Results API tests**

Here are the test scenarios:

- Update results for a single valid experiment 
- Update multiple results for a single valid experiment
- Update results for multiple experiments using a single json
- Update results for multiple experiments using multiple jsons
- Update results for multiple experiments with multiple containers using a single json
- Update results for containers that are not present during creation of the experiment
- Update results for an invalid experiment or a non-existing experiment
- Test with invalid values such as blank, null or an invalid value for various keys in the updateResults json
- Update the same results twice for the same experiment


### **List Recommendation API tests**


Here are the test scenarios:

- List recommendations specifying a valid experiment name
- List recommendations without specifying any parameters
- List recommendations for a single experiment with multiple results posted in a single json
- List recommendations for multiple experiments created using multiple jsons and results from multiple jsons
- List recommendations for multiple experiments after updating results with 
	- some of the non-mandatory metrics (for example, memoryLimit/memoryRequest etc) missing
	- with invalid kubernetes object type
- List recommendations with parameters by specifying the following parameters:
	/listRecommendations?experiment_name=<experiment_name>&latest=false
	/listRecommendations?experiment_name=<experiment_name>&latest=true
	/listRecommendations?experiment_name=<experiment_name>&monitoring_end_time=<valid_timestamp>
- List recommendations after creating an experiment but without updating results
- List recommendations with invalid parameter values for experiment_name & monitoring_end_time
	- Non-existing experiment_name
	- Non-existing time stamp, incorrect timestamp format
- List recommendations with valid and invalid notification codes
- List recommendations with valid and invalid minimum data threshold
  	- with valid contiguous and non-contiguous minimum data points for each term
  	- with invalid data points for each term
- List recommendations with minimum data threshold exceeding the max duration for each term
	- with non-contiguous data points exceeding the max duration fixed for each term

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
			[ -r ] : Location of benchmarks. Not required for remote_monitoring_tests
			[ --tctype ] : optional. Testcases type to run, default is functional (runs all functional tests)
			[ --testmodule ]: Module to be tested. Use testmodule=help, to list the modules to be tested
			[ --testsuite ] : Testsuite to run. Use testsuite=help, to list the supported testsuites
			[ --testcase ] : Testcase to run. Use testcase=help along with the testsuite name to list the supported testcases in that testsuite
			[ -n ] : optional. Namespace to deploy autotune
			[ --resultsdir ] : optional. Results directory location, by default it creates the results directory in current working directory
			[ --skipsetup ] : optional. Specifying this option skips the Kruize setup and performance profile creation in case of remote_monitoring_tests

Note: If you want to run a particular testcase then it is mandatory to specify the testsuite
Test cases supported are sanity, negative, extended and test_e2e

```

To run all the remote monitoring tests,

```
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=remote_monitoring_tests --resultsdir=/home/results
```

To run only the sanity remote monitoring tests,

```
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=remote_monitoring_tests --testcase=sanity --resultsdir=/home/results
```

Remote monitoring tests can also be run without using the test_autotune.sh. To do this, follow the below steps:

- Deploy Kruize using the deploy.sh from the kruize autotune repo
- Create the performance profile by using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md)
- cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests
- python3 -m pip install --user -r requirements.txt
- cd rest_apis
- To run all sanity tests
```
	pytest -m sanity --html=<dir>/report.html --cluster_type <minikube|openshift>
```
- To run only sanity tests for listRecommendations API --cluster_type <minikube|openshift>
```
	pytest -m sanity --html=<dir>/report.html test_list_recommendations.py
```
- To run only a specific test within listRecommendations API
```
	pytest -s test_list_recommendations.py::test_list_recommendations_single_exp --cluster_type <minikube|openshift>
```

Note: You can check the report.html for the results as it provides better readability

