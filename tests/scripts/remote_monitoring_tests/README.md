# **Kruize Remote monitoring stress test**

Kruize Remote monitoring stress test validates the behaviour of [Kruize remote monitoring APIs](/design/MonitoringModeAPI.md) by loading these APIs with multiple requests. These tests use the [Apache jmeter](https://jmeter.apache.org/) tool to generate the load.

## Tests description
- **Kruize remote monitoring stress test**
   The test does the following:
   - Sets up Apache jmeter version 5.5
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) 
   - Runs a script to capture resource usage metrics in the background
   - Runs the jmeter load test with the specified users
  
## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/stress_test/remote_monitoring_stress_test.sh -c [minikube|openshift] [-i Kruize image] [-r results directory path] [ -u users ] [ -e No. of results to be updated ] [ -d rampup time in seconds ] [ -t timeout in seconds ]
```

Where values for remote_monitoring_stress_test.sh are:

```
usage: remote_monitoring_stress_test.sh 
        [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
        [ -i ] : optional. Kruize docker image to be used for testing
                 default - kruize/autotune_operator:test
	[ -r ] : Results directory path
	[ -u ] : Jmeter users / No. of experiments
	[ -e ] : No. of results to be updated
	[ -d ] : Jmeter Rampup time in seconds
	[ -t ] : Monitoring metrics timeout duration in seconds
```

For example,

```
<AUTOTUNE_REPO>/tests/scripts/remote_monitoring_tests/stress_test/remote_monitoring_stress_test.sh -r /tmp/stress-test-results -i kruize/autotune_operator:0.0.10_mvp -u 1000 -e 100 -t 900
```

Test with the following inputs:
100 exps / 100 results
100 exps / 1500 results
1000 exps / 100 results
1000 exps / 1500 results
10000 exps / 100 results

Once the tests are complete, verify if there are no errors or exceptions in jmeter logs. Kruize resource usage metrics can be found in monitoring_metrics.csv in the results directory.
