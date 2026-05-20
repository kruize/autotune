# Kruize Bulk API — Hyperfoil Load Test

This directory contains a [Hyperfoil](https://hyperfoil.io/) benchmark for load-testing the Kruize Bulk API endpoint.

## Prerequisites

| Requirement | Notes |
|---|---|
| Hyperfoil CLI / controller | Install with `./install_hyperfoil.sh` in this directory. Tested with 0.27+. |
| Running Kruize instance | Remote-monitoring mode with a Prometheus datasource named `prometheus-1`. |
| `cluster-metadata-local-monitoring` metadata profile | Must exist in Kruize before running the test (see note below). |

> **Known Kruize bug**: The Bulk API internally looks for the `cluster-metadata-local-monitoring` metadata profile regardless of the `metadata_profile` field in the request body. Make sure this profile exists in your Kruize instance before running the test.

## Files

| File | Description |
|---|---|
| `kruize-bulk-hyperfoil.yaml` | Hyperfoil benchmark definition — submits one bulk job and polls its status. |
| `hyperfoil.md` | This documentation. |

## Quick Start

### 1. Set the Kruize URL

Install Hyperfoil if needed:

```bash
./install_hyperfoil.sh install-only
```

Edit `kruize-bulk-hyperfoil.yaml` and replace `KRUIZE_URL_PLACEHOLDER` with your Kruize API route, e.g.:

```yaml
http:
  host: http://kruize-route-openshift-tuning.apps.mycluster.example.com
```

### 2. Run the benchmark

**Using the Hyperfoil CLI (interactive):**

```bash
# Start the CLI
./install_hyperfoil.sh cli

# Inside the CLI:
[hyperfoil]$ upload /path/to/kruize-bulk-hyperfoil.yaml
[hyperfoil]$ run kruize-bulk-api-test
[hyperfoil]$ stats
```

**Using the standalone controller:**

```bash
# Start controller in the background
./install_hyperfoil.sh standalone &

# Upload and run via REST
curl -s -X POST http://localhost:8090/benchmark \
     -H 'Content-Type: application/yaml' \
     --data-binary @kruize-bulk-hyperfoil.yaml

curl -s -X POST "http://localhost:8090/benchmark/kruize-bulk-api-test/start"
```

**Run with one command (install + benchmark):**

```bash
./install_hyperfoil.sh
```

## What the Test Does

1. **POST /bulk** — Submits a bulk job requesting recommendations for all experiments discovered by `prometheus-1`, using a 24-hour measurement window.
2. **Wait 3 s** — Gives Kruize time to start processing the job.
3. **GET /bulk?job_id=\<id\>** — Polls the job status once and captures `summary.status` and `summary.total_experiments`.

The test runs at a constant rate of **1 request per second** for **1 second** (i.e., a single scenario run). Adjust `usersPerSec` and `duration` in the YAML to increase load.

## Customisation

| Parameter | Location in YAML | Description |
|---|---|---|
| `host` | `http.host` | Kruize API base URL |
| `datasource` | `POST /bulk` body | Name of the Prometheus datasource registered in Kruize |
| `metadata_profile` | `POST /bulk` body | Metadata profile to use (see known bug note above) |
| `measurement_duration` | `POST /bulk` body | Lookback window for recommendations (e.g. `15min`, `1h`, `24h`) |
| `usersPerSec` | `phases.main.constantRate` | Concurrent request rate |
| `duration` | `phases.main.constantRate` | How long the phase runs |
| `thinkTime` | between requests | Pause between submitting the job and polling its status |

## Interpreting Results

After a run, the Hyperfoil CLI `stats` command shows per-phase latency percentiles and error counts. Key things to check:

- **Status errors** — any non-2xx HTTP responses indicate Kruize API failures.
- **`job.status`** — captured from the poll response; should be `IN_PROGRESS` or `COMPLETED` depending on cluster size and `thinkTime`.
- **`total.experiments`** — number of experiments Kruize found; useful for scaling the `thinkTime` and `duration` for longer runs.
