# **Kruize Remote monitoring fault tolerant test**

Kruize Remote monitoring fault tolerant test validates the behaviour of [Kruize remote monitoring APIs](/design/MonitoringModeAPI.md) by running the experiments by mimicking the remote monitoring workflow as described below by restarting Kruize in between to check the stability of the APIs and database restore functionality.

## Tests description
- **Kruize remote monitoring pod restart test**

   The test does the following:
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) 
   - Runs the test that does the following:
	- For the no. of iterations it starts a loop
	- In the loop, it creates the specified number of experiments 
	- For each experiment created, 100 results are updated (each result corresponds to 15 mins interval)
	- Once the results are posted output of listRecommendations and listExperiments API are captured
	- Now the kruize pod is restarted and performance profile is created (temporary step until performance profile is loaded from DB)
	- Again the listRecommendations and listExperiment API output is captured and compared with the ones before restart
	- If kruize restart fails test is exited if not the loop continues
  
## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/fault_tolerant_tests/remote_monitoring_fault_tolerant_tests.sh -c [minikube|openshift] [-i Kruize image] [-r results directory path] [ -u No. of experiments ] [ -d Number of iterations to test kruize restart ]
```

Where values for remote_monitoring_fault_tolerant_tests.sh are:

```
usage: remote_monitoring_fault_tolerant_tests.sh 
        [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
        [ -i ] : optional. Kruize docker image to be used for testing
                 default - kruize/autotune_operator:test
	[ -r ] : Results directory path
	[ -u ] : Number of experiments, default - 1
	[ -d ] : Number of iterations to test kruize restart, default - 2
```

For example,

```
<AUTOTUNE_REPO>/tests/scripts/remote_monitoring_tests/fault_tolerant_tests/remote_monitoring_fault_tolerant_tests.sh -r /tmp/fault-tolerant-test-results -i kruize/autotune_operator:0.0.12_mvp -u 3 -d 2
```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes.  
