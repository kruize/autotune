# **Kruize Recommendation API Tests**

Kruize Recommendation API tests validate the behavior of the new [Kruize Recommendations API v1.0](./../../../design/MonitoringModeAPI.md) 
using various positive and negative scenarios. These tests are developed using pytest framework and are applicable to both 
local and remote monitoring modes.

## Overview

The Recommendation API v1.0 introduces an enhanced schema that includes:
- **Replicas field** in current config, recommendation config, and variation
- **Nested resources structure** with requests and limits under a resources map
- **Pod count metrics** with aggregation (min, max, avg, sum) in metrics_info
- Support for both **local** and **remote** monitoring targets

## Tests Description

The new `/kruize/api/v1/recommendations` endpoint supports the updated recommendation schema with replicas, nested resources structure, and pod_count metrics.

### **GET /kruize/api/v1/recommendations API Tests**

The GET endpoint retrieves recommendations for experiments with the new v1.0 schema.

#### Sanity Tests:
- **Single experiment recommendations**: Validate complete recommendation structure for a single experiment
- **Multiple experiments recommendations**: List recommendations for multiple experiments
- **Parameterized queries**: Test various query parameter combinations
  - `/recommendations?experiment_name=<name>` - Get recommendations for specific experiment
  - `/recommendations?experiment_name=<name>&interval_end_time=<timestamp>` - Get recommendations for specific time
  - `/recommendations?latest=true` - Get only latest recommendations
  - `/recommendations?latest=false` - Get all recommendations
- **Namespace experiments**: Validate recommendations for namespace-level experiments
- **Pod count aggregation validation**: Comprehensive validation of pod_count metrics
  - Verify presence of min, max, avg, sum fields
  - Validate numeric types and non-negative values
  - Validate logical relationships (min ≤ avg ≤ max)
  - Verify actual values are populated correctly

#### Negative Tests:
- **Invalid experiment name**: Request recommendations for non-existing experiment
- **Invalid timestamp format**: Request with malformed interval_end_time
- **Non-existing timestamp**: Request with valid format but non-existing timestamp
- **Experiment without results**: Request recommendations before updating results

### **POST /kruize/api/v1/recommendations API Tests**

The POST endpoint generates recommendations with support for both local and remote monitoring targets.

#### Sanity Tests:
- **Generate with target=remote**: Default behavior for remote monitoring
- **Generate with target=local**: Local monitoring mode support
- **Default target behavior**: Generate without specifying target parameter
- **Complete validation**: Use reusable validation functions
- **Replicas validation**: Comprehensive replicas field validation
- **Pod count with actual values**: Validate pod_count with real data

#### Negative Tests:
- **Invalid target parameter**: Request with unsupported target value
- **Missing experiment_name**: POST without experiment_name parameter
- **Missing interval_end_time**: POST for remote target without interval_end_time
- **Invalid timestamp format**: POST with malformed interval_end_time
- **Non-existing experiment**: Generate recommendations for non-existing experiment

### **Extended Tests**

- **Multiple experiments with different targets**: Test mixed local and remote experiments
- **All terms validation**: Validate replicas and pod_count across short, medium, and long terms
- **Varying pod counts**: Test with different pod count scenarios and validate replica recommendations
- **Accelerator workloads**: Validate pod_count metrics for GPU/accelerator workloads


## Prerequisites for Running the Tests

- Minikube setup or access to Openshift cluster
- Tools: kubectl, oc, curl, jq, python3
- Python modules: pytest, JSON, pytest-html, requests, jinja2
  (these modules will be automatically installed during test execution)

## How to Run the Tests

### Using test_autotune.sh (Recommended)

Run all recommendation tests:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --resultsdir=/home/results
```

Run only sanity tests:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --testcase=sanity --resultsdir=/home/results
```

Run only negative tests:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --testcase=negative --resultsdir=/home/results
```

Run with custom Kruize image:
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube -i quay.io/kruize/autotune:latest --testsuite=recommendation_tests --resultsdir=/home/results
```

Skip Kruize setup (if already deployed):
```bash
<KRUIZE_REPO>/tests/test_autotune.sh -c minikube --testsuite=recommendation_tests --testcase=sanity --skipsetup --resultsdir=/home/results
```

### Using pytest Directly

Recommendation tests can also be run without using test_autotune.sh:

1. Deploy Kruize using deploy.sh from the Kruize autotune repo
2. Create the performance profile using the [createPerformanceProfile API](./../../../design/PerformanceProfileAPI.md)
3. Navigate to test directory:
   ```bash
   cd <KRUIZE_REPO>/tests/scripts/recommendation_tests
   ```
4. Install Python dependencies:
   ```bash
   python3 -m pip install --user -r requirements.txt
   ```
5. Navigate to rest_apis directory:
   ```bash
   cd rest_apis
   ```
6. Run tests:

   Run all sanity tests:
   ```bash
   pytest -m sanity --html=<dir>/report.html --cluster_type <minikube|openshift>
   ```

   Run all negative tests:
   ```bash
   pytest -m negative --html=<dir>/report.html --cluster_type <minikube|openshift>
   ```

   Run specific test file:
   ```bash
   pytest --html=<dir>/report.html test_recommendation_resource_api.py --cluster_type <minikube|openshift>
   ```

   Run specific test:
   ```bash
   pytest -s test_recommendation_resource_api.py::test_get_recommendations_v1_single_experiment --cluster_type <minikube|openshift>
   ```

**Note:** Check the report.html for results as it provides better readability with detailed test execution information.

## Test Suite Structure

```
tests/scripts/recommendation_tests/
├── Recommendation_tests.md          # This file
├── recommendation_tests.sh          # Shell script wrapper for running tests
├── requirements.txt                 # Python dependencies
└── rest_apis/
    └── test_recommendation_resource_api.py  # Main test file
```
