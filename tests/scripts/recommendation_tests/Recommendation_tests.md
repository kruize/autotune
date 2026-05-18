# **Kruize Recommendation API Tests**

Kruize Recommendation API tests validate the behavior of the new [Kruize Recommendations API v1.0](./../../../design/MonitoringModeAPI.md)
using various positive and negative scenarios. These tests are developed using pytest framework and support both
**Remote Monitoring** and **Local Monitoring** modes with proper test categorization.

## Overview

The Recommendation API v1.0 introduces an enhanced schema that includes:
- **Replicas field** in current config, recommendation config, and variation
- **Nested resources structure** with requests and limits under a resources map
- **Pod count metrics** with aggregation (min, max, avg, sum) in metrics_info
- Support for both **local** and **remote** monitoring targets

## Test Categorization

Tests are organized using pytest markers to support different monitoring modes:

| Marker                  | Description                                |
|-------------------------|--------------------------------------------|
| `@pytest.mark.remote`   | Tests for remote monitoring mode (rm=True) |
| `@pytest.mark.local`    | Tests for local monitoring mode (rm=False) |

## Tests Description

The `/kruize/api/v1/recommendations` endpoint supports the updated recommendation schema with replicas, nested resources structure, and pod_count metrics.

### **Remote Monitoring Tests** (`@pytest.mark.remote`)

#### POST /kruize/api/v1/recommendations API Tests

**test_get_recommendations_v1_remote_e2e_workflow**:
- End-to-end workflow for both container and namespace experiments
- Creates experiments, updates results, generates recommendations
- Validates complete recommendation structure with new v1.0 schema
- Validates replicas field presence and values
- Validates nested resources structure (requests/limits)
- Validates pod_count metrics with aggregation
- Tests both container-level and namespace-level experiments

**test_get_recommendations_v1_invalid_experiment**:
- Request recommendations for non-existing experiment
- Expected: 400 Bad Request with proper error message

**test_get_recommendations_v1_invalid_timestamp**:
- Request with malformed interval_end_time parameter
- Expected: 400 Bad Request with proper error message

#### POST /kruize/api/v1/recommendations API Tests

**test_post_recommendations_v1_without_experiment_name**:
- POST request without experiment_name parameter
- Expected: 400 Bad Request with proper error message

**test_post_recommendations_v1_without_interval_end_time**:
- POST request for remote target without interval_end_time
- Expected: 400 Bad Request with proper error message

### **Local Monitoring Tests** (`@pytest.mark.local`)

#### POST /kruize/api/v1/recommendations API Tests

**test_get_recommendations_v1_local_e2e_workflow**:
- End-to-end workflow for local monitoring mode
- Sets up datasources, metadata profiles, and metric profiles
- Creates both container (sysbench) and namespace experiments
- Waits for auto-generation of recommendations (5 minutes)
- Validates complete recommendation structure with v1.0 schema
- Validates replicas field and nested resources
- Validates pod_count metrics
- Tests both container-level and namespace-level experiments
- Includes proper cleanup of all resources

**test_get_recommendations_v1_invalid_experiment_local**:
- Request recommendations for non-existing experiment in local mode
- Expected: 400 Bad Request with proper error message

**test_post_recommendations_v1_without_experiment_name_local**:
- POST request without experiment_name in local mode
- Expected: 400 Bad Request with proper error message


## Prerequisites for Running the Tests

- Minikube setup or access to Openshift cluster
- Tools: kubectl, oc, curl, jq, python3
- Python modules: pytest, JSON, pytest-html, requests, jinja2
  (these modules will be automatically installed during test execution)

## How to Run the Tests

### Using test_autotune.sh (Recommended)

The shell script automatically applies the correct Kruize configuration based on the test category.

Run all recommendation tests:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --resultsdir=/home/results
```

Run only remote monitoring tests:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --testcase=remote --resultsdir=/home/results
```

Run only local monitoring tests:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --testcase=local --resultsdir=/home/results
```

Run with custom Kruize image:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube -i quay.io/kruize/autotune:latest --testsuite=recommendation_tests --resultsdir=/home/results
```

Skip Kruize setup (if already deployed):
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --testcase=remote --skipsetup --resultsdir=/home/results
```

### Using recommendation_tests.sh Directly

The shell script handles configuration patching automatically:

```bash
cd <KRUIZE_REPO>/tests/scripts/recommendation_tests
./recommendation_tests.sh <cluster_type> <testcase>
```

Examples:
```bash
# Run remote monitoring tests (applies remote patch: local=false)
./recommendation_tests.sh minikube remote

# Run local monitoring tests (uses default config: local=true)
./recommendation_tests.sh minikube local
```

### Using pytest Directly

**Important:** When running pytest directly, ensure the correct Kruize configuration is applied beforehand.

1. Deploy Kruize using deploy.sh from the Kruize autotune repo
2. For remote tests, apply the remote monitoring patch to set `local=false` in config
3. For local tests, use default configuration with `local=true`
4. Create the performance profile using the [createPerformanceProfile API](./../../../design/PerformanceProfileAPI.md)
5. Navigate to test directory:
   ```bash
   cd <KRUIZE_REPO>/tests/scripts/recommendation_tests/rest_apis
   ```
6. Install Python dependencies:
   ```bash
   python3 -m pip install --user -r ../requirements.txt
   ```
7. Run tests:

   Run all remote monitoring tests:
   ```bash
   pytest -m remote --html=<dir>/report.html --cluster_type <minikube|openshift>
   ```

   Run all local monitoring tests:
   ```bash
   pytest -m local --html=<dir>/report.html --cluster_type <minikube|openshift>
   ```

   Run all tests:
   ```bash
   pytest --html=<dir>/report.html --cluster_type <minikube|openshift>
   ```

   Run specific test:
   ```bash
   pytest -s test_recommendation_resource_api.py::test_get_recommendations_v1_remote_e2e_workflow --cluster_type <minikube|openshift>
   ```

**Note:** Check the report.html for results as it provides better readability with detailed test execution information.

## Test Suite Structure

```
tests/scripts/recommendation_tests/
├── Recommendation_tests.md                      # This file
├── recommendation_tests.sh                      # Shell script wrapper for running tests
├── requirements.txt                             # Python dependencies
└── rest_apis/
    ├── test_recommendation_resource_api.py      # Main test file with remote and local tests
    └── TEST_CATEGORIZATION_README.md            # Detailed test categorization documentation
```

## Configuration Details

The shell script automatically applies the correct Kruize configuration based on test category:

| Test Category | Shell Script Command                        | Patch Applied         | Config local Flag | API rm Parameter |
|---------------|---------------------------------------------|-----------------------|-------------------|------------------|
| remote        | `./recommendation_tests.sh minikube remote` | `kruize_remote_patch` | `false`           | `True`           |
| local         | `./recommendation_tests.sh minikube local`  | None (default)        | `true`            | `False`          |

### How Configuration Patching Works

- **Remote tests**: The script applies `kruize_remote_patch` which sets `local=false` in the Kruize configuration YAML
- **Local tests**: The script uses the default configuration with `local=true`

