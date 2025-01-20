# **Kruize Bulk API stress test**

Kruize Bulk API stress test validates the behaviour of [Kruize Bulk APIs](/design/BulkAPI.md) by loading these APIs with multiple requests to generate recommendations 

## Tests description
- **Kruize Bulk API stress test**
   The test does the following:
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Creates a resource optimization metric profile using the [createMetricProfile API](/design/MetricProfileAPI.md) 
   - Runs any of the specified tests below:
     - No config test - In this test the Bulk API is invoked parallely without an empty bulk configuration
     - Time range test - In this test the Bulk API is invoked parallely with the same time range as specified in the configuration
     - Split Time range test - In this test the Bulk API is invoked parallely with different time ranges as specified in the configuration.
   - Once the Bulk job is created, the test gets the bulk job status and on completion fetches the recommendations for the processed experiments
  
## Prerequisites for running the tests:
- Minikube setup or access to Openshift cluster
- Tools like kubectl, oc, curl, jq

- To test with Thanos datasource, Thanos setup with tsdb blocks containing usage metrics is required

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/scripts/local_monitoring_tests/bulk_stress_test/bulk_stress_test.sh -c [minikube|openshift] [-i Kruize image] [-w No. of workers] [-t interval hours (default - 2)] [-s End date of tsdb block] [-a kruize replicas] [-r <resultsdir path>] [--skipsetup skip kruize setup] [ -z to test with prometheus datasource] [--test Specify the test to be run] [--url Thanos Datasource url]
```

Where values for bulk_stress_test.sh are:

```
usage: bulk_stress_test.sh 
        [ -c ] : cluster type. Supported type - minikube, openshift. Default - minikube
        [ -i ] : optional. Kruize docker image to be used for testing
                 default - quay.io/kruize/autotune:mvp_demo 
	[ -r ] : Results directory path
	[ -w ] : No. of workers (default - 5)
        [ -t ] : interval hours (default - 2)
        [ -s ] : Initial start date (default - 2024-12-18T06:20:00.000Z)
        [ -a ] : kruize replicas (default - 3)
        [ -z ] : To register prometheus datasource with kruize
        [ --test ] : Specify the test to be run [time_range/no_config/time_range_split] (default - time_range)
        [ --url ]: Datasource url (default - ]"
        [ --skipsetup ] : skip kruize setup] 
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
