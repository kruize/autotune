# **Kruize Local monitoring fault tolerant test**

Kruize Local monitoring fault tolerant test validates the behaviour of [Kruize local monitoring APIs](/design/KruizeLocalAPI.md) by running the experiments by mimicking the local monitoring workflow as described below by restarting Kruize in between to check the stability of the APIs and database restore functionality.

## Tests description
- **Kruize local monitoring pod restart test**

   The test does the following:
   - Deploys the Tech Empower workload from the Kruize benchmarks repo
   - Runs some load on the benchmark in the background
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Creates a resource optimization metric profile using the [createMetricProfile API](/design/MetricProfileAPI.md) 
   - Runs the test that does the following:
	- Creates an experiment for the TFB application
	- Waits for the workload to complete and generates recommendations
	- Captures the output of listRecommendations, listExperiments, list datasouces and list metadata
	- Restarts the kruize pod
	- Captures the output of listRecommendations, listExperiments, list datasouces and list metadata
	- Compares the output of all the list APIs before and after kruize restart to ensure they match
  
## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/scripts/local_monitoring_tests/fault_tolerant_tests/local_monitoring_fault_tolerant_tests.sh -c [minikube|openshift] [-i Kruize image] [-r results directory path]
```

Where values for local_monitoring_fault_tolerant_tests.sh are:


```
usage: local_monitoring_fault_tolerant_tests.sh 
        [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
        [ -i ] : optional. Kruize docker image to be used for testing
                 default - kruize/autotune:mvp_demo
	[ -r ] : Results directory path
```

For example,

```
<AUTOTUNE_REPO>/tests/scripts/local_monitoring_tests/fault_tolerant_tests/local_monitoring_fault_tolerant_tests.sh -r /tmp/fault-tolerant-test-results -i kruize/autotune_operator:0.0.23_mvp
```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes.  
