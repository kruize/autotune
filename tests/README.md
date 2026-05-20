# **Kruize Autotune tests**

Kruize Autotune tests repository contains tests to validate Kruize in remote monitoring and local monitoring modes. 

## Functional tests description

### Remote monitoring tests

  Here we test Kruize [Remote monitoring APIs](/design/MonitoringModeAPI.md). 

#### API tests

  The tests does the following:
  - Deploys kruize in non-CRD mode using the deploy script from the autotune repo
  - Validates the behaviour of createExperiment, updateResults and listRecommendations APIs in various scenarios covering both positive and negative usecases.

  For details refer this [doc](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)

#### Stress test

To run the stress test refer the Stress test [README](/tests/scripts/remote_monitoring_tests/README.md)

#### Fault tolerant test

To run the fault tolerant test refer the [README](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)

### Local monitoring tests

Here we test Kruize [Local monitoring APIs](/design/KruizeLocalAPI.md).

#### API tests

  The tests does the following:
  - Deploys kruize in non-CRD mode using the deploy script from the autotune repo
  - Validates the behaviour of list datasources, import metadata and list metadata APIs in various scenarios covering both positive and negative usecases.

  For details refer this [doc](/tests/scripts/local_monitoring_tests/Local_monitoring_tests.md)

### Authentication Test:

Kruize 0.2 onwards supports the authentication which provides the user an option to pass authentication details in the YAML for the service they are using.

The authentication test is a standalone shell-based test. It contains various valid and invalid scenarios for testing.

It can be run directly from the shell test location, for example:

`tests/config_tests/authentication_tests.sh`

#### Scenarios
**_valid_**: a valid path to the token

**_expired_**: an expired token value

**_invalid_**: an invalid path to the token

**_empty_**: a blank input in place of the token file path

### Datasource Availability/Serviceability Test:

Kruize supports multiple datasources such as Prometheus and Thanos Querier. During startup, Kruize validates the reachability of all configured datasources before proceeding.

The datasource availability/serviceability test is a standalone shell-based test and is not part of the pytest-based functional/local monitoring suite.
It validates Kruize behavior when one or more datasources are reachable or unreachable.

Kruize startup behavior follows these rules:

* Kruize continues startup if at least one datasource is reachable.
* Kruize logs an error for each unreachable datasource.
* Kruize fails startup only when all configured datasources are unreachable.

The test can be run using the command below:

```
./test_autotune.sh -c <cluster-type> -i <image-name> --testsuite=datasource_tests
```

#### Scenarios

Both cluster types support multiple datasources. Scenarios vary by which datasources are in the cluster YAML:
- **OpenShift:** YAML has Prometheus + Thanos (both available in cluster)
- **Minikube/Kind:** YAML has Prometheus only (Thanos not running; multiple Prometheus datasources are also valid)

**both-valid** (OpenShift)

Both datasources are reachable.

**✔ Expected:** Kruize starts successfully.

**valid-invalid** (OpenShift)

Datasource 1 is reachable, datasource 2 is unreachable.

**✔ Expected:** Kruize starts successfully and logs an error for datasource 2.

**invalid-valid** (OpenShift)

Datasource 1 is unreachable, datasource 2 is reachable.

**✔ Expected:** Kruize starts successfully and logs an error for datasource 1.

**both-invalid** (OpenShift)

Both datasources are unreachable.

**❌ Expected:** Kruize fails to start and exits with an error.

**valid** (Minikube/Kind)

Datasource is reachable.

**✔ Expected:** Kruize starts successfully.

**invalid** (Minikube/Kind)

Datasource is unreachable.

**❌ Expected:** Kruize fails to start and exits with an error.


## Supported Clusters
- Minikube, Openshift

## Prerequisites for running the tests:

- Minikube setup 
- Tools like docker, kubectl, and jq
- python

Clone the kruize/benchmarks repo using the below command:

```
git clone https://github.com/kruize/benchmarks.git

```

## How to run the tests?

First, cleanup any previous instances of autotune using the below command:

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -t
```

Use the below command to test :

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube [-i autotune image] [-o [operator image]] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to test] [-n namespace] [--resultsdir=results directory] [--skipsetup] [--cleanup_prometheus] [-t cleanup kruize setup]
```

Where values for test_autotune.sh are:

```
usage: test_autotune.sh [ -c ] : cluster type. Supported type - minikube
                        [ -i ] : optional. Autotune docker image to be used for testing, default - kruize/autotune_operator:test
			[ -o ] : optional. Deploy Kruize in operator mode (only for local_monitoring_tests). Optionally specify operator image, default - quay.io/kruize/kruize-operator:<version>
			[ --testsuite ] : Testsuite to run. Use testsuite=help, to list the supported testsuites
			[ --testcase ] : Testcase to run. Use testcase=help along with the testsuite name to list the supported testcases in that testsuite
			[ -n ] : optional. Namespace to deploy autotune
			[ --resultsdir ] : optional. Results directory location, by default it creates the results directory in current working directory
			[ --skipsetup ] : optional. Specifying this option skips the autotune setup & application deployment
			[ --cleanup_prometheus ] : optional. Specifying this option along with -t option cleans up prometheus setup

Note: If you want to run a particular testcase then it is mandatory to specify the testsuite

```

For example,

To run remote monitoring tests,

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -i kruize/autotune_operator:0.8 --testsuite=remote_monitoring_tests --resultsdir=/home/results
```

To run local monitoring tests,

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -i kruize/autotune_operator:0.8 --testsuite=local_monitoring_tests --resultsdir=/home/results
```

To run local monitoring tests in operator mode,

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -o --testsuite=local_monitoring_tests --resultsdir=/home/results
```

Or with a specific operator image,

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -o quay.io/kruize/kruize-operator:latest --testsuite=local_monitoring_tests --resultsdir=/home/results
```
