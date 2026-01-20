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
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube [-i autotune image] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to test] [-n namespace] [--resultsdir=results directory] [--skipsetup] [--cleanup_prometheus] [-t cleanup kruize setup]
```

Where values for test_autotune.sh are:

```
usage: test_autotune.sh [ -c ] : cluster type. Supported type - minikube
                        [ -i ] : optional. Autotune docker image to be used for testing, default - kruize/autotune_operator:test
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
