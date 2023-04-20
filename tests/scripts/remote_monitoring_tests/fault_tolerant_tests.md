# **Kruize Remote monitoring fault tolerant test**

Kruize Remote monitoring fault tolerant test validates the behaviour of [Kruize remote monitoring APIs](/design/MonitoringModeAPI.md) by running the experiments by mimicking the remote monitoring workflow as described below by restarting Kruize in between to check the stability of the APIs and database restore functionality.

## Tests description
- **Kruize remote monitoring pod restart test**

   The test does the following:
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) 
   - Runs the test that does the following:
	- For the specified hours of metrics data it starts a loop to post 6 hours data each time
	- In the loop, it creates the specified number of experiments 
	- For each experiment created, 6 hours of metrics data is updated (6 hours equal to 24 data points considering 
	  each data point / metrics corresponds to 15 mins interval)
	- Once the 6 hours results are posted for all the experiments, recommendations are fetched for all the experiments
	- Now the output of listExperiments API output is captured and kruize pod is restarted
	- After restarting the kruize pod, performance profile is created and listExperiments API output is captured
	- Both the listExperiment API outputs, that contains both metrics and recommendations data are compared
	- Test is failed if the output doesn't match if not the loop continues
  
## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/fault_tolerant_tests/remote_monitoring_fault_tolerant_tests.sh -c [minikube|openshift] [-i Kruize image] [-r results directory path] [ -u No. of experiments ] [ -d Number of hours of metrics data available] 
```

Where values for remote_monitoring_fault_tolerant_tests.sh are:

```
usage: remote_monitoring_fault_tolerant_tests.sh 
        [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
        [ -i ] : optional. Kruize docker image to be used for testing
                 default - kruize/autotune_operator:test
	[ -r ] : Results directory path
	[ -u ] : Number of experiments
	[ -d ] : Number of hours of metrics data available
```

For example,

```
<AUTOTUNE_REPO>/tests/scripts/remote_monitoring_tests/fault_tolerant_tests/remote_monitoring_fault_tolerant_tests.sh -r /tmp/fault-tolerant-test-results -i kruize/autotune_operator:0.0.12_mvp -u 3 -d 12
```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes.  
