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
   - Invokes the get bulk job status to fetch the recommendations after the job completes for the processed experiments
  
## Prerequisites for running the tests:
- Openshift cluster
- Tools like kubectl, oc
- Thanos setup with minio containing the resource usage metrics in tsdb blocks

## How to run the test?

Use the below command to test :

```
cd <KRUIZE_REPO>/tests/scripts/local_monitoring_tests/bulk_scale_test
./bulk_scale_test.sh [-i Kruize image] [-r results directory path]  [-w No. of workers] [-d No. of days of results ] 
           [-t interval hours] [-s Initial end date of tsdb block ] [-a kruize replicas ] [--skipsetup skip kruize setup]
           [--url Datasource url ] [-o No. of orgs ] [-c No. of clusters / org]
```

Where values for bulk_scale_test_bulk.sh are:

```
usage: bulk_scale_test.sh 
       [-i Kruize image]
       [-w No. of workers (default - 5)]
       [-d No. of days of results (default - 15)]
       [-t interval hours (default - 2)]
       [-s Initial end date of tsdb block (default - 2024-12-10T11:50:00.000Z)]
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
./bulk_scale_test.sh -i quay.io/kruize/autotune_operator:0.3  -w 10 -s "2025-01-31T06:20:00.000Z" 

```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes. 
