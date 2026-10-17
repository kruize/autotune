# **Kruize Bulk scalability test**

Kruize Bulk scalability test validates the behaviour of [Kruize Bulk APIs](/design/BulkAPI.md) by scaling kruize pods and invoking the bulk API with different time ranges of metrics in tsdb blocks.
Invokes List recommendations to fetch the recommendations after the bulk job completes generating recommendations. 

## Test description

The scalability test evaluates Kruize Bulk API throughput and stability under high concurrency and large volume across three key dimensions:

1. **Time Blocks (TSDB Blocks)**:
   - Iterates through metric time windows in discrete intervals (e.g., 6-hour blocks over `N` days) moving sequentially backwards in time.
   - Total TSDB blocks = `(days_of_res * 24) / interval_hours`.

2. **Workload Partitioning (`configs.json`)**:
   - Workloads are partitioned declaratively using a JSON configuration file containing unique names, label selectors, and filter definitions for each batch.
   - Decouples test definitions from cluster/tenant counts and allows scaling up to 100,000+ workloads in controlled batches.

3. **Parallel Concurrency (Workers)**:
   - Spawns parallel worker threads based on the `--workers` parameter.
   - Configurations from `configs.json` are distributed across workers in round-robin fashion.
   - For each TSDB time block, workers run in parallel, executing their assigned configurations sequentially (`POST /bulk`), polling for completion (`GET /bulk?job_id=...`), and collecting metrics.

### Step-by-Step Execution Flow:
- **Environment Setup**:
  - Registers Thanos as a datasource by updating the deployment [manifest file](https://github.com/kruize/autotune/blob/master/manifests/crc/default-db-included-installation/openshift/kruize-crc-openshift.yaml).
  - Deploys Kruize in non-CRD mode using [`deploy.sh`](https://github.com/kruize/autotune/blob/master/deploy.sh) and scales Kruize replicas.
  - Exposes the Kruize service and registers the resource optimization metric profile using the [createMetricProfile API](/design/MetricProfileAPI.md).
- **Scale Execution**:
  - For each TSDB time block, parallel worker threads invoke the Bulk API (`POST /bulk`) with the corresponding time range and workload filters from `configs.json`.
  - Polls `GET /bulk?job_id=<job_id>` until jobs complete or reach timeout.
  - Collects and records resource utilization metrics (`kruize_metrics.py`) per worker per TSDB block.
  - Validates recommendations generated for processed experiments.

---

## Configuration File Format (`configs.json`)

`configs.json` defines the workload partitions and label filters assigned across workers:

```json
[
  {
    "name": "org-1_cluster-1",
    "labels": {
      "org_id": "org-1",
      "cluster_id": "eu-1-1"
    },
    "filter": {
      "include": {
        "namespace": ["prod-namespace"]
      }
    }
  },
  {
    "name": "org-2_cluster-1",
    "labels": {
      "org_id": "org-2",
      "cluster_id": "eu-2-1"
    }
  }
]
```
  
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
           [--url Datasource url ] [-k configs json file]
```

Where values for bulk_scale_test.sh are:

```
usage: bulk_scale_test.sh
       [-i Kruize image]
       [-w No. of workers (default - 20)]
       [-d No. of days of results (default - 15)]
       [-t interval hours (default - 6)]
       [-s Initial end date of tsdb block (default - 2024-12-10T11:50:00.000Z)]
       [-a kruize replicas (default - 3)]
       [-r <resultsdir path>]
       [--skipsetup skip kruize setup]
       [--url Datasource url (default - http://thanos-query-frontend-example-query.thanos-operator-system.svc.cluster.local:9090/)]
       [-k <configs json file> (default - configs.json)]
       
```

For example:

```bash
cd <KRUIZE_REPO>/tests/scripts/local_monitoring_tests/bulk_scale_test

# Run scale test using default configs.json with 20 parallel workers across 15 days of 6-hour TSDB blocks
./bulk_scale_test.sh -i quay.io/kruize/autotune_operator:0.3 -w 20 -d 15 -t 6 -s "2024-12-10T11:50:00.000Z" -k configs.json
```

Once the tests are complete, check the results directory (`kruize_bulk_scale_test_results/`) for execution logs (`scale_logs/`), job status responses (`job_status_jsons/`), and Kruize resource metrics (`results/`).
