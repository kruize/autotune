# **Autotune tests**

Autotune tests repository contains tests to validate individual autotune modules. Going forward we will be adding end-to-end tests and some non-functional tests to test other aspects like stability, scaling and resilience of Autotune. 

## Autotune Modules

Autotune functional tests validate individual modules of Autotune. Autotune has the following high level modules:

- Analyzer module
	- Dependency Analyzer
	- Recommendation Manager
- Experiment manager module

Refer [Autotune modules](https://github.com/kruize/autotune/blob/master/docs/autotune_modules.md) for details.

## High level Test Scenarios

- IT Admin adds Autotune object (application config for autotune to tune the app)
- SME adds / modifies kruize layer
- SME adds / modifies Kruize Tunable
- Autotune REST APIs

## Functional tests description

### Dependency Analyzer module tests

- **Application autotune yaml tests**

  Here we validate if a user is able to add an autotune object for the application that needs to be monitored by autotune and if autotune rejects the invalid autotune yamls with appropriate error messages.
   
   The test does the following:
   - Deploys autotune and its dependencies using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Applies the application autotune yaml 
   - Checks if autotune validates the yaml 

- **Autotune config yaml tests**

  Here we validate if a user can add a new kruize layer configuration with the required tunables or modify the existing layer configuration. We also check if autotune handles invalid configurations well giving out appropriate error messages to the users.
   
   The test does the following:
   - Deploys autotune and its dependencies using the deploy script from the autotune repo
   - Applies the autotune config yaml 
   - Checks if autotune validates the yaml

- **Basic API tests**

  Here we validate all the [Autotune REST APIs](/design/ExperimentModeAPI.md).
  
  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Deploys multiple instances of spring petclinic application 
  - Applies the autotune application yamls for the petclinic deployments
  - Validates the JSON output for all the Autotune REST APIs 

- **Modify kruize layer tests**

  Here we modify the layer config and validate the listKruizeTunables Autotune REST API.
  
  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Modify the layer config and apply
  - Validate if the modified config is reflected in the JSON output from listKruizeTunables Autotune API

- **ConfigMap yaml tests**

  Here we modify the configmap yaml and validate if the behaviour is reflected in autotune. We also check if autotune identifies invalid configurations well by giving out appropriate error messages to the users.
  
  The test does the following:
  - Modifies the configmap yaml
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Checks if autotune validates the configmap yaml
  - Test validates the following scenarios:
  	1. Invalid-cluster-type 
  	2. Invalid-k8s-type
  	3. Invalid-monitoring-agent
  	4. Invalid-monitoring-service
  	5. Change the logging_level to debug and check if the behaviour is reflected in autotune
  
- **Autotune object id tests**

  Here we validate the autotune object id for different scenarios.
  
  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Deploys benchmark applications and required application autotune yamls
  - Validate autotune id for following scenarios:
  	1. Check the uniqueness of the autotune object ids
  	2. Check if re-applying the autotune object without modifying the yaml does not change the autotune object id
  	3. Update and apply the application autotune yaml and compare the ids
  	4. Deploy multiple applications and check if the autotune object ids are unique
    
- **kruize layer object id tests**

  Here we validate the kruize layer object id for different scenarios.
  
  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Validate kruize layer id for following scenarios:
  	1. Check the uniqueness of the kruize layer ids
  	2. Re-apply the layer config without modifying yaml and check if both the ids are same
  	3. Update and apply the layer config yaml and compare the ids
  	4. Apply new layer config and validate the id

### Experiment manager module tests

- **ABTesting workflow test**
   
  Here the ABTesting workflow in the Experiment manager is validated.

  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Deploys the benchmark application
  - Posts an experiment using the input json (<AUTOTUNE_REPO>/tests/resources/em_input_json/ABTesting.json) and createExperiment API to the Experiment Manager
  - Validates the trial result summary obtained from the listTrialStatus API once the experiment is completed.
	
- **General Performance Experiment workflow test**
   
  Here the General Performance Experiment workflow in the Experiment manager is validated.

  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Deploys the benchmark application
  - Posts an experiment using the input json (<AUTOTUNE_REPO>/tests/resources/em_input_json/GeneralPerfExp.json) and createExperiment API to the Experiment Manager
  - Validates the trial result summary obtained from the listTrialStatus API once the experiment is completed.

### Remote monitoring tests

  Here we test Kruize [Remote monitoring APIs](/design/MonitoringModeAPI.md). 

#### API tests

  The tests does the following:
  - Deploys kruize in non-CRD mode using the deploy script from the autotune repo
  - Validates the behaviour of createExperiment, updateResults and listRecommendations APIs in various scenarios covering both positive and negative usecases.

  For details refer this [doc](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)

#### Stress test

To run the stress test refer the Stress test [README](/tests/scripts/remote_monitoring_tests/README.md)

## Supported Clusters
- Minikube

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
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -r [location of benchmarks]  [-i autotune image] [--tctype=functional] [--testmodule=Autotune module to be tested] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to test] [-n namespace] [--resultsdir=results directory] [--skipsetup]
```

Where values for test_autotune.sh are:

```
usage: test_autotune.sh [ -c ] : cluster type. Supported type - minikube
                        [ -i ] : optional. Autotune docker image to be used for testing, default - kruize/autotune_operator:test
			[ -r ] : Location of benchmarks
			[ --tctype ] : optional. Testcases type to run, default is functional (runs all functional tests)
			[ --testmodule ]: Module to be tested. Use testmodule=help, to list the modules to be tested
			[ --testsuite ] : Testsuite to run. Use testsuite=help, to list the supported testsuites
			[ --testcase ] : Testcase to run. Use testcase=help along with the testsuite name to list the supported testcases in that testsuite
			[ -n ] : optional. Namespace to deploy autotune
			[ --resultsdir ] : optional. Results directory location, by default it creates the results directory in current working directory
			[ --skipsetup ] : optional. Specifying this option skips the autotune setup & application deployment

Note: If you want to run a particular testcase then it is mandatory to specify the testsuite

```

For example,

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube --tctype=functional --testsuite=app_autotune_yaml_tests --testcase=slo_class -r /home/benchmarks --resultsdir=/home/results
```

To run remote monitoring tests,

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -i kruize/autotune_operator:0.0.11_mvp --testsuite=remote_monitoring_tests --resultsdir=/home/results
```

## How to test a specific autotune module?

To run the tests specific to a autotune module use the "testmodule" option. For example, to run all the tests for dependency analyzer module execute the below command:
```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube --testmodule=da -r /home/benchmarks --resultsdir=/home/results
```
