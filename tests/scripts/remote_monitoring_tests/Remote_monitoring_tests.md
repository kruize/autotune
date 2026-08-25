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
- Create namespace experiment specifying namespace experiment type
- Create namespace experiment without specifying experiment type
- Create container experiment specifying container experiment type
- Create container experiment without specifying experiment type
- Create experiment specifying both namespaces and containers without specifying the experiment type
- Create experiment specifying both namespaces and containers specifying the namespace experiment type
- Create experiment specifying both namespaces and containers specifying the container experiment type
- Create namespace experiment specifying containers
- Create container experiment specifying namespaces
- Create multiple namespace experiments with valid namespace
- Validate the experiment for the presence of experiment_type, creation_date and update_date along with its default values using listExperiments

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
- [NEGATIVE] Test with invalid values such as blank, null or an invalid value for Accelerator specific metrics

Namespace Related Test Scenarios:
Sanity Tests
- Update results for a single valid namespace experiment
- Update multiple valid results in a single json for a single namespace experiment
- Update multiple valid results for a namespace experiment (uses a loop)
- Update results test with supported cpu and memory format types
- Update the same results twice for the same namespace experiment
- Update results with valid invalid interval duration

Negative Tests

- Update Results Namespace experiment for invalid tests
- Update Results Namespace experiment for missing metrics & missing mandatory metrics
- Update results with metric values as 0 and generate recommendations (skip)
- Create a container experiment and upload namespace results for this exp
- Create a namespace experiment and upload container results for this exp
- Create a container experiment and upload bulk results with a few of them containing namespace results
- Create a namespace experiment and upload bulk results with a few of them containing container results
- Update Results for Namespace experiment without creating the experiment

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
- List recommendations after sending 15 days of constant results matching requests and limits
- List recommendations with valid and invalid notification codes
- List recommendations with valid and invalid minimum data threshold
  - Valid contiguous and non-contiguous minimum data points for each term
  - Invalid minimum data points for each term
- List recommendations with minimum data threshold exceeding the max duration for each term
  - with non-contiguous data points exceeding the max duration fixed for each term
- List recommendations with data available for some terms
  - for contiguous data:
    - no data available
    - all data available
    - only short_term data available
    - only medium_term data available
    - only long_term data available
    - short_term and medium_term data available
    - short_term and long_term data available
    - medium_term and long_term data available
  - for non-contiguous data:
    - similar tests as mentioned above for contiguous

Namespace Related Test Scenarios:
Sanity Tests
- List recommendations with valid recommendations for all terms
- List recommendations without results

Negative Tests
- _To be updated_


### **Update Recommendation API tests**


Here are the test scenarios:

- Update recommendations with valid results and plots available
- Update recommendations with no plots available when no recommendations available for medium and long term
- Update recommendations with just interval_end_time in input
- Update recommendations without experiment name or end_time
- Update recommendations without end_time
- Update recommendations with invalid end_time format
- Update recommendations with unknown experiment_name
- Update recommendations with unknown end_time
- Update recommendations with end_time preceding start_time
- [SANITY] Update recommendations with valid results for Kruize supported MIG accelerator [A100, H100 and H200]
- [SANITY] Update recommendations with valid results for Non-MIG Accelerator

Namespace Related Test Scenarios:
Sanity Tests
- Update recommendations with valid short term recommendations. Also, Validate the container array, should be blank.

Negative Tests
- _To be updated_

Extended Tests
- Update recommendations with valid recommendations for namespace experiments for all the terms for multiple experiments posted using different json files.

The above tests are developed using pytest framework and the tests are run using shell script wrapper that does the following:
- Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
- Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md)
- Runs the above tests using pytest

### **Create Performance Profile API tests**

Here are the test scenarios:
- Create performance profile with a valid version
- Create performance profile with duplicate data
- Create performance profile with missing mandatory fields

### **List Performance Profile API tests**

Here are the test scenarios:
- List performance profile with no profiles
- List performance profile with only one profile present
- List performance profile with multiple profiles present

### **Update Performance Profile API tests**

Here are the test scenarios:
- Update performance profile with a valid version
- Update performance profile with a missing profile name
- Update performance profile with duplicate data
- Update performance profile with duplicate SLO data
- Update performance profile with invalid superset data 
- Update performance profile with missing mandatory fields

### **Delete Performance Profile API tests**

Here are the test scenarios:
- Delete performance profile with a valid version
- Delete performance profile with invalid profile name scenarios
- Delete performance profile with when its associated with existing experiments


### **CloudWatch Logging Integration tests**

These tests validate the CloudWatch logging setup in Kruize by deploying the pod against two scenarios and inspecting the pod logs.

**Scenario 1 — credentials-absent** *(always runs — no AWS credentials required)*
- Kruize must start successfully with no CloudWatch credentials set
- Pod logs must contain the skip message: `AWS access details are not provided. Skipping sending logs to CloudWatch.`
- Pod logs must NOT contain `NoClassDefFoundError: jdk/net/Sockets`

This scenario simulates the test/dev cluster configuration that **masked the jdk.net bug** — when credentials are absent, the credential guard in `CloudWatchAppender.configureLoggerForCloudWatchLog()` short-circuits before the AWS SDK client is constructed, so the missing `jdk.net` module is never exercised.

**Scenario 2 — credentials-present** *(runs only when AWS env vars are set — see prerequisites below)*
- Kruize must start successfully with valid CloudWatch credentials set
- Pod logs must contain the success message: `CloudWatch logging enabled — region: <r>, log group: <g>, log stream: <s>, log level: <l>`
- Pod logs must NOT contain `NoClassDefFoundError: jdk/net/Sockets`

This scenario replicates the **stage/prod configuration that triggered the production crash** after the `awssdk-version` bump from `2.42.25` to `2.46.5` in `pom.xml` (which switched the default HTTP client from Apache HttpClient 4 to Apache HttpClient 5, requiring the `jdk.net` JDK module that was absent from the `jlink`-built container JRE).

## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq, python
- Various python modules pytest, json, pytest-html, requests, jinja2
  (these modules will be automatically installed while the test is run)

**Additional prerequisites for CloudWatch logging credentials-present test:**
- A dev AWS account (do **not** use production credentials)
- A CloudWatch log group and log stream created on the dev account
- An IAM user with the following permissions scoped to the dev log group:
  `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:DescribeLogGroups`, `logs:DescribeLogStreams`, `logs:PutLogEvents`
- The following environment variables exported before running:

```
export CLOUDWATCH_ACCESS_KEY_ID=<IAM access key id>
export CLOUDWATCH_SECRET_ACCESS_KEY=<IAM secret access key>
export CLOUDWATCH_REGION=<AWS region, e.g. us-east-1>
export CLOUDWATCH_LOG_GROUP=<log group name, e.g. kruize-dev-logs>
export CLOUDWATCH_LOG_STREAM=<log stream name, e.g. kruize-dev-stream>
```

If these variables are not set, the credentials-present test is automatically skipped and only the credentials-absent test runs.

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube -r [location of benchmarks]  [-i kruize image] [--tctype=functional] [--testmodule=Autotune module to be tested] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to test] [-n namespace] [--resultsdir=results directory] [--skipsetup]
```

Where values for test_autotune.sh are:

```
usage: test_autotune.sh [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
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
Test cases supported are sanity, negative, extended, test_e2e and cloudwatch_logging

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

## How to run the CloudWatch logging tests?

### Using test_autotune.sh

To run only the CloudWatch logging integration tests (credentials-absent scenario, no AWS credentials needed):

```
<KRUIZE_REPO>/tests/test_autotune.sh -c openshift --testsuite=remote_monitoring_tests --testcase=cloudwatch_logging --resultsdir=/home/results
```

To run both scenarios (credentials-absent and credentials-present), export the AWS env vars first:

```
export CLOUDWATCH_ACCESS_KEY_ID=<IAM access key id>
export CLOUDWATCH_SECRET_ACCESS_KEY=<IAM secret access key>
export CLOUDWATCH_REGION=us-east-1
export CLOUDWATCH_LOG_GROUP=kruize-dev-logs
export CLOUDWATCH_LOG_STREAM=kruize-dev-stream

<KRUIZE_REPO>/tests/test_autotune.sh -c openshift --testsuite=remote_monitoring_tests --testcase=cloudwatch_logging --resultsdir=/home/results
```

### Running directly using pytest

- Deploy Kruize using the deploy.sh from the kruize autotune repo
- cd `<KRUIZE_REPO>/tests/scripts/remote_monitoring_tests`
- python3 -m pip install --user -r requirements.txt
- cd rest_apis

To run only the credentials-absent test (no AWS credentials needed):
```
pytest -m cloudwatch_logging -k "credentials_absent" --html=<dir>/report.html --cluster_type <minikube|openshift>
```

To run both credentials-absent and credentials-present tests (AWS env vars must be set):
```
pytest -m cloudwatch_logging --html=<dir>/report.html --cluster_type <minikube|openshift>
```

To run a specific CloudWatch test:
```
pytest -s test_cloudwatch_logging.py::test_kruize_starts_and_skips_cloudwatch_when_credentials_absent --cluster_type openshift
pytest -s test_cloudwatch_logging.py::test_kruize_starts_and_enables_cloudwatch_when_credentials_present --cluster_type openshift
```

### Using the shell orchestrator directly

The shell orchestrator patches the deployment YAML, redeploys, reads pod logs, and reports results:

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests
export KRUIZE_REPO=<path to kruize repo>
export cluster_type=openshift
export RESULTS=<results directory>

# credentials-absent only (no AWS needed)
source cloudwatch_logging_tests.sh
cloudwatch_logging_tests

# both scenarios (set AWS env vars first)
export CLOUDWATCH_ACCESS_KEY_ID=<IAM access key id>
export CLOUDWATCH_SECRET_ACCESS_KEY=<IAM secret access key>
export CLOUDWATCH_REGION=us-east-1
export CLOUDWATCH_LOG_GROUP=kruize-dev-logs
export CLOUDWATCH_LOG_STREAM=kruize-dev-stream
cloudwatch_logging_tests
```

Note: You can check the report.html for the results as it provides better readability