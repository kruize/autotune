# **Kruize Bulk scalability test**

Kruize Bulk scalability test validates the behaviour of [Kruize Bulk APIs](/design/BulkAPI.md) by scaling kruize pods and invoking the bulk API with different time ranges of metrics in tsdb blocks.
Invokes List recommendations to fetch the recommendations after the bulk job completes generating recommendations. 

## Test description

   The test does the following:

   - Registers thanos as datasource by updating the [manifest file](https://github.com/kruize/autotune/blob/master/manifests/crc/default-db-included-installation/openshift/kruize-crc-openshift.yaml)
   - Deploys kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Scales kruize replicas to 3
   - Exposes kruize service
   - Creates a resource optimization performance profile using the [createMetricProfile API](/design/MetricProfileAPI.md) 
   - Creates the specified no. of threads that run in parallel, invoking bulk service for the specified time range of tsdb blocks and labels
   - Invokes list recommendations after the job completes for the processed experiments
  
## Prerequisites for running the tests:
- Openshift cluster
- Tools like kubectl, oc
- Thanos setup with minio containing the resource usage metrics in tsdb blocks

## How to run the test?

Use the below command to test :

```
cd <KRUIZE_REPO>/tests/scripts/local_monitoring_tests/bulk_scale_test
./bulk_scale_test.sh [-i Kruize image] [-r results directory path] [-u No. of experiments (default - 5000)] [-d No. of days of results (default - 15)] [-n No. of clients (default - 20)] [-m results duration interval in mins, (default - 15)] [-t interval hours (default - 6)] [-s Initial start date (default - 2023-01-10T00:00:00.000Z)] [-q query db interval in mins, (default - 10)]
```

Where values for bulk_scale_test_bulk.sh are:

```
usage: bulk_scale_test.sh 
       [-i Kruize image]
       [-w No. of workers (default - 5)]
       [-t interval hours (default - 2)]
       [-s Initial end date of tsdb block (default - 2024-11-11T00:00:00.000Z)]
       [-a kruize replicas (default - 3)]
       [-r <resultsdir path>]
       [--skipsetup skip kruize setup]
       [--url Datasource url (default - http://thanos-query-frontend.thanos-bench.svc.cluster.local:9090/]
       [-o No. of orgs (default - 10)]
       [-c No. of clusters / org (default - 10)]
       
```

For example,

```
cd <KRUIZE_REPO>/tests/scripts/local_monitoring_tests/bulk_scale_test
./bulk_scale_test.sh -i quay.io/kruize/autotune_operator:0.3  -w 10 -s "2024-12-10T11:50:00.000Z" 

```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes. 

Below commands are used in the script to capture the execution time and the count of experiments and results from the database:

Commands used to capture the execution time:

```
grep -m28 -H 'Time elapsed:' *.log | awk -F '[:.]' '{ sum[$1] += ($4 * 3600) + ($5 * 60) + $6 } END { for (key in sum) { printf "%s: Total time elapsed: %02d:%02d:%02d\n", key, sum[key] / 3600, (sum[key] / 60) % 60, sum[key] % 60 } }' | sort

```

The above command captures the execution time for 7 days of metrics data upload, modify -m28 (-m<4 * 7> ) to -m<4 * num_days_of_res>

Commands to fetch the count of experiments and results from the DB:

```
kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;"; kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;"; kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_recommendations ;"

```
