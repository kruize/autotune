# **Kruize Remote monitoring scale test**

Kruize Remote monitoring scale test validates the behaviour of [Kruize remote monitoring APIs](/design/MonitoringModeAPI.md) by running the experiments by mimicking the remote monitoring workflow as described below and also captures the resource usage (cpu/memory) metrics.

## Tests description
- **Kruize remote monitoring scale test**
   The test does the following:
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) 
   - Runs a script to capture resource usage metrics in the background
   - Runs the scale test that does the following:
	- For the specified hours of metrics data it starts a loop to post 6 hours data each time
	- In the loop, it creates the specified number of experiments 
	- For each experiment created, 6 hours of metrics data is updated (6 hours equal to 24 data points considering 
	  each data point / metrics corresponds to 15 mins interval)
	- Once the 6 hours results are posted for all the experiments, recommendations are fetched for all the experiments
	- And the same repeats for the remaining data
  
## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/scale_test/remote_monitoring_scale_test.sh -c [minikube|openshift] [-i Kruize image] [-r results directory path] [ -u No. of experiments ] [ -d Number of hours of metrics data available ] [ -t timeout in seconds for monitoring metrics ]
```

Where values for remote_monitoring_scale_test.sh are:

```
usage: remote_monitoring_scale_test.sh 
        [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
        [ -i ] : optional. Kruize docker image to be used for testing
                 default - kruize/autotune_operator:test
	[ -r ] : Results directory path
	[ -u ] : Number of experiments
	[ -d ] : Number of hours of metrics data available
	[ -t ] : Monitoring metrics timeout duration in seconds
```

For example,

```
<AUTOTUNE_REPO>/tests/scripts/remote_monitoring_tests/scale_test/remote_monitoring_scale_test.sh -r /tmp/scale-test-results -i kruize/autotune_operator:0.0.11_mvp -u 10 -d 90 -t 2400
```

Once the tests are complete, manually check the logs for any exceptions or errors. Kruize resource usage metrics can be found in monitoring_metrics.csv in the results directory
