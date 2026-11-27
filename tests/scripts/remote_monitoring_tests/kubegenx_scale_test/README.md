# **Kruize KubeGenX Scale Test**

Kruize KubeGenX scale test validates the scalability of [Kruize remote monitoring APIs](/design/MonitoringModeAPI.md) using real workload data fetched from a KubeGenX service. Unlike the existing scale test that uses static JSON templates with synthetic data, this test uses actual namespace, workload, and metric data from a running cluster via KubeGenX APIs.

## Test description

The test runs in two phases:

- **Phase 1 - Data Fetch**: Fetches namespaces, workloads, and metric results from KubeGenX APIs, transforms them into Kruize-compatible JSON format, and saves them to disk for reuse.
- **Phase 2 - Data Post**: Deploys Kruize, scales it to the desired number of replicas, and posts the fetched data to Kruize using parallel clients. Results are posted in batches (default 24 per iteration), with Prometheus metrics collected after each iteration.

### What the test does

1. Fetches all namespaces, workloads, and results from KubeGenX APIs (with pagination)
2. Converts metric data from KubeGenX format to Kruize format (bytes to MiB, timestamp normalization)
3. Saves experiment and result JSONs to disk (reusable across runs)
4. Deploys Kruize in non-CRD mode, patches resource limits for scale, and scales to N replicas
5. Launches N parallel clients, each iterating over batches:
   - `POST /createExperiment` for each workload
   - `POST /updateResults` with a batch of 24 result intervals per experiment
   - `POST /updateRecommendations` at the max timestamp of each batch
6. Collects Kruize Prometheus metrics after every iteration (per-client CSV)
7. Validates expected vs actual results count in the database
8. Produces a summary report with API latencies, resource usage, and execution time milestones

### Script call chain

```
kubegenx_scale_test.sh                          (orchestrator)
  ├── fetch_kubegenx_data.py                    (Phase 1: fetch from KubeGenX)
  ├── Kruize deploy + scale replicas
  ├── N x kubegenx_scalability_wrapper.sh       (Phase 2: per-client loop)
  │     └── loops over batch iterations
  │         ├── post_kubegenx_data.py           (post one batch to Kruize)
  │         └── kruize_metrics.py               (collect Prometheus metrics)
  ├── execution_time()                          (milestone tracking)
  ├── DB validation                             (expected vs actual)
  └── parse_metrics.py                          (final summary report)

generate_kubegenx_csv.py                        (standalone: CSV generation)
  └── fetches KubeGenX data → per-workload CSVs for Kruize/ROS-OCP testing
```

## Prerequisites

- Access to an OpenShift cluster with Kruize deployed (or will be deployed by the script)
- A running KubeGenX service with data available
- Tools: `kubectl`, `oc`, `curl`, `jq`, `python3`
- Python packages: `requests`
- Prometheus monitoring stack installed on the cluster

## How to run the test

### Full test (fetch + setup + post)

```bash
./kubegenx_scale_test.sh \
  --kubegenx-url https://<kubegenx-host> \
  --dataset <dataset-name> \
  --session-id <session-id> \
  --insecure \
  -n 10 -g 10 \
  --api-version legacy
```

### Fetch data only (inspect before posting)

```bash
./kubegenx_scale_test.sh \
  --kubegenx-url https://<kubegenx-host> \
  --dataset <dataset-name> \
  --session-id <session-id> \
  --insecure \
  --fetch-only
```

### Re-post previously fetched data (skip fetch and Kruize setup)

```bash
./kubegenx_scale_test.sh \
  --post-only \
  -b false \
  -n 10 \
  --api-version legacy
```

### Small test run (limit scope for quick validation)

```bash
./kubegenx_scale_test.sh \
  --kubegenx-url https://<kubegenx-host> \
  --dataset <dataset-name> \
  --session-id <session-id> \
  --insecure \
  --limit-namespaces 2 \
  --limit-workloads 10 \
  -n 2 -g 2
```

## Parameters

```
Usage: ./kubegenx_scale_test.sh --kubegenx-url <URL> --dataset <DATASET> --session-id <SESSION_ID> [options]

Required:
  --kubegenx-url <URL>       KubeGenX base URL (e.g. https://kubegenx-host.apps.cluster.opentlc.com)
  --dataset <DATASET>        KubeGenX dataset name (e.g. infallible-nash)
  --session-id <SESSION_ID>  KubeGenX session ID (e.g. gdmr4mreqe9vm)

Options:
  -r <results_dir>           Results directory (default: kruize_kubegenx_scale_test_results)
  -i <kruize_image>          Kruize docker image (default: quay.io/kruizehub/autotune-test-image:mvp_demo)
  -n <num_clients>           Number of parallel clients (default: 10)
  -g <replicas>              Number of Kruize replicas (default: 10)
  -b <true|false>            Run Kruize setup (default: true)
  -q <interval_mins>         DB query interval in minutes (default: 5)
  --batch-size <N>           Results per POST per experiment (default: 24)
  --insecure                 Skip TLS certificate verification for KubeGenX
  --limit-namespaces <N>     Limit namespaces to fetch (default: 0 = all)
  --limit-workloads <N>      Limit workloads per namespace (default: 0 = all)
  --fetch-only               Only fetch data from KubeGenX, do not post to Kruize
  --post-only                Only post data (assumes data already fetched in a prior run)
  --api-version <v1|legacy>  Kruize API version (default: legacy)
  -h                         Show help
```

## 5K scale test example

With a KubeGenX dataset containing 20 namespaces x 250 workloads = 5000 experiments:

```bash
./kubegenx_scale_test.sh \
  --kubegenx-url https://kubegenx-kube-gen-x.apps.cluster-xxx.opentlc.com \
  --dataset infallible-nash \
  --session-id gdmr4mreqe9vm \
  --insecure \
  -n 10 \
  -g 10 \
  --batch-size 24 \
  --api-version legacy \
  -r kruize_5k_scale_results
```

This will:
- Fetch 5000 experiments with ~1344 result intervals each from KubeGenX
- Deploy and scale Kruize to 10 replicas
- Launch 10 parallel clients (500 experiments each)
- Each client iterates 56 times (1344 / 24 batch size), posting 24 results per experiment per iteration
- Collect Prometheus metrics after every iteration
- Validate DB counts and produce a summary report

## Output

Results are saved under the results directory:

```
kruize_5k_scale_results/
  kubegenx-scale-test-<timestamp>/
    kubegenx-scale-test.log           # Main test log
    kruize_setup.log                  # Kruize deployment log
    kubegenx_data/                    # Fetched data (reusable)
      metadata.json
      create_experiments/*.json
      results/*.json
    scale_logs/                       # Per-client logs
      kubegenx-client-1.log
      kubegenx-client-2.log
      ...
    results/                          # Prometheus metrics CSVs
      increase_kubegenxMetrics-*.csv
      total_kubegenxMetrics-*.csv
    pod_logs/                         # Kruize pod logs
    exec_time.log                     # Execution time milestones
```

## Key differences from existing scale test

| Aspect | Existing `scale_test/` | KubeGenX `kubegenx_scale_test/` |
|---|---|---|
| Data source | Static JSON templates with synthetic values | Real metrics from KubeGenX APIs |
| Results generation | Increments timestamps in a loop, same metric values | Pre-fetched real metric values per interval |
| Two-phase design | No - generates and posts inline | Yes - fetch once to disk, post separately |
| Reusability | Must re-run from scratch | Can re-post same data with `--post-only` |
| Experiment identity | `scaletest250-{client}_{i}` | `{namespace}_{workload_name}` |
| Work distribution | Each client creates its own N experiments | All experiments split across clients by modulo |

## Individual script usage

### fetch_kubegenx_data.py

Fetch data from KubeGenX and save as Kruize-compatible JSONs:

```bash
python3 fetch_kubegenx_data.py \
  --url https://<kubegenx-host> \
  --dataset infallible-nash \
  --session-id gdmr4mreqe9vm \
  --output-dir ./kubegenx_data \
  --insecure
```

### post_kubegenx_data.py

Post a single batch of results for a subset of experiments:

```bash
python3 post_kubegenx_data.py \
  --ip <kruize-ip> \
  --port 0 \
  --data-dir ./kubegenx_data \
  --client-id 0 \
  --total-clients 1 \
  --batch-index 0 \
  --batch-size 24 \
  --api-version legacy
```

### generate_kubegenx_csv.py

Generate per-workload CSVs from KubeGenX data for Kruize and ROS-OCP testing:

```bash
# Generate per-workload CSVs
python3 generate_kubegenx_csv.py \
  --url https://<kubegenx-host> \
  --dataset infallible-nash \
  --session-id gdmr4mreqe9vm \
  --output-dir ./kubegenx_csvs \
  --insecure

# Also generate a single combined CSV with all workloads
python3 generate_kubegenx_csv.py \
  --url https://<kubegenx-host> \
  --dataset infallible-nash \
  --session-id gdmr4mreqe9vm \
  --output-dir ./kubegenx_csvs \
  --insecure \
  --combined-csv

# Limit scope for quick testing
python3 generate_kubegenx_csv.py \
  --url https://<kubegenx-host> \
  --dataset infallible-nash \
  --session-id gdmr4mreqe9vm \
  --output-dir ./kubegenx_csvs \
  --insecure \
  --limit-namespaces 2 \
  --limit-workloads 5
```

Output structure:

```
kubegenx_csvs/
  metadata.json                          # Summary: workload count, interval count, source info
  all_workloads.csv                      # Combined CSV (if --combined-csv used)
  per_workload/
    ecuador-angren_kazakhstan-kladno.csv  # Per-workload CSV (one per workload)
    ecuador-angren_haiti-rivne-raion.csv
    ...
```

CSV columns are compatible with Kruize's `create_json_from_csv()` (memory in bytes, timestamps in ISO format).
These CSVs can be used directly with the `recommendation_experiment.py` flow in `kruize-demos` or shared
with the ROS-OCP team for testing (use `replaceheaders.sh` to convert column names if needed).

### kubegenx_scalability_wrapper.sh

Per-client wrapper that iterates over all batches and collects metrics:

```bash
./kubegenx_scalability_wrapper.sh \
  --ip <kruize-ip> \
  --port 0 \
  --data-dir ./kubegenx_data \
  --client-id 0 \
  --total-clients 1 \
  --batch-size 24 \
  --api-version legacy \
  --prometheusserver <prometheus-server> \
  --outputdir ./results
```
