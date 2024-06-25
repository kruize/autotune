# **Kruize Remote monitoring scalability test**

Kruize Remote monitoring scalability test validates the behaviour of [Kruize remote monitoring APIs](/design/MonitoringModeAPI.md) by scaling kruize pods and running multiple experiments with 15 days of metrics uploaded and captures various metrics.

## Test description

   The test does the following:
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Scales kruize replicas to 10
   - Exposes kruize service
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) 
   - Creates the specified no. of client threads that run in parallel, creating multiple experiments and uploading the metrics results for 15 days
  
## Prerequisites for running the tests:
- Openshift cluster
- Tools like kubectl, oc

## How to run the test?

Use the below command to test :

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/scale_test
./remote_monitoring_tests/scale_test/remote_monitoring_scale_test_bulk.sh [-i Kruize image] [-r results directory path] [-u No. of experiments (default - 5000)] [-d No. of days of results (default - 15)] [-n No. of clients (default - 20)] [-m results duration interval in mins, (default - 15)] [-t interval hours (default - 6)] [-s Initial start date (default - 2023-01-10T00:00:00.000Z)] [-q query db interval in mins, (default - 10)]
```

Where values for remote_monitoring_scale_test_bulk.sh are:

```
usage: remote_monitoring_fault_tolerant_tests.sh 
        [ -i ] : optional. Kruize docker image to be used for testing
                 default - quay.io/kruize/autotune:mvp_demo
	[ -r ] : Results directory path
	[-u No. of experiments (default - 5000)]
	[-d No. of days of results (default - 15)] 
	[-n No. of clients (default - 20)]
	[-m results duration interval in mins, (default - 15)]
	[-t interval hours (default - 6)]
	[-s Initial start date (default - 2023-01-10T00:00:00.000Z)]
	[-q query db interval in mins, (default - 10)]
```

For example,

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/scale_test
./remote_monitoring_tests/scale_test/remote_monitoring_scale_test_bulk.sh -i quay.io/kruize/autotune_operator:0.0.20_mvp  -u 250  -d 15  -n 20 -t 6  -q 10  -s 2023-01-10T00:00:00.000Z  -r /tmp/scale_test_results

```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes. Verify if the execution times captured in exec_time.log are as expected.

Below commands are used in the script to capture the execution time and the count of experiments and results from the database:

Commands used to capture the execution time:

```
grep -m28 -H 'Time elapsed:' *.log | awk -F '[:.]' '{ sum[$1] += ($4 * 3600) + ($5 * 60) + $6 } END { for (key in sum) { printf "%s: Total time elapsed: %02d:%02d:%02d\n", key, sum[key] / 3600, (sum[key] / 60) % 60, sum[key] % 60 } }' | sort

```

The above command captures the execution time for 7 days of metrics data upload, modify -m28 (-m<4 * 7> ) to -m<4 * num_days_of_res>

Commands to fetch the count of experiments and results from the DB:

```
kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;"; kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;"

```
