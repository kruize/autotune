# **Kruize Recommendation API Tests**

Kruize Recommendation API tests validate the behavior of the new [Kruize Recommendations API v1.0](/design/RecommendationResourceAPI.md) 
using various positive and negative scenarios. These tests are developed using pytest framework and are applicable to both 
local and remote monitoring modes.

## Overview

The Recommendation API v1.0 introduces an enhanced schema that includes:
- **Replicas field** in current config, recommendation config, and variation
- **Nested resources structure** with requests and limits under a resources map
- **Pod count metrics** with aggregation (min, max, avg, sum) in metrics_info
- Support for both **local** and **remote** monitoring targets

## Tests Description

### **GET /recommendations API Tests**

The GET endpoint retrieves recommendations for experiments with the new v1.0 schema.

#### Sanity Tests:
- **Single experiment recommendations**: Validate complete recommendation structure for a single experiment
  - Verify experiment_name and kubernetes_objects presence
  - Validate replicas field in current config
  - Validate nested resources structure (requests/limits under resources map)
  - Validate pod_count metrics with aggregation fields

- **Multiple experiments recommendations**: List recommendations for multiple experiments
  - Validate each experiment follows v1.0 schema
  - Verify consistent structure across all experiments

- **Parameterized queries**: Test various query parameter combinations
  - `/recommendations?experiment_name=<name>` - Get recommendations for specific experiment
  - `/recommendations?experiment_name=<name>&interval_end_time=<timestamp>` - Get recommendations for specific time
  - `/recommendations?latest=true` - Get only latest recommendations
  - `/recommendations?latest=false` - Get all recommendations

- **Namespace experiments**: Validate recommendations for namespace-level experiments
  - Verify replicas field at namespace level
  - Validate pod_count aggregation for namespace workloads

- **Pod count aggregation validation**: Comprehensive validation of pod_count metrics
  - Verify presence of min, max, avg, sum fields
  - Validate numeric types and non-negative values
  - Validate logical relationships (min ≤ avg ≤ max)
  - Verify actual values are populated correctly

#### Negative Tests:
- **Invalid experiment name**: Request recommendations for non-existing experiment
  - Verify 400 status code
  - Validate error message content

- **Invalid timestamp format**: Request with malformed interval_end_time
  - Verify 400 status code
  - Validate error message describes the issue

- **Non-existing timestamp**: Request with valid format but non-existing timestamp
  - Verify 400 status code
  - Validate appropriate error response

- **Experiment without results**: Request recommendations before updating results
  - Verify appropriate notification/error response

### **POST /recommendations API Tests**

The POST endpoint generates recommendations with support for both local and remote monitoring targets.

#### Sanity Tests:
- **Generate with target=remote**: Default behavior for remote monitoring
  - Create experiment and update results
  - Generate recommendations with target=remote
  - Validate response includes replicas and pod_count
  - Verify nested resources structure

- **Generate with target=local**: Local monitoring mode support
  - Setup datasource, metadata profile, and metric profile
  - Create experiment with target_cluster='local'
  - Generate recommendations with target='local'
  - Validate local monitoring specific schema
  - Verify pod_count metrics for local workloads

- **Default target behavior**: Generate without specifying target parameter
  - Should default to 'remote'
  - Validate same behavior as explicit target=remote

- **Complete validation**: Use reusable validation functions
  - `validate_complete_v1_recommendations()` for full structure
  - `validate_v1_kubernetes_object()` for k8s objects
  - Verify all validation results are positive

- **Replicas validation**: Comprehensive replicas field validation
  - Verify replicas in current config
  - Verify replicas in recommendation config
  - Verify replicas in variation
  - Validate numeric types and non-negative values

- **Pod count with actual values**: Validate pod_count with real data
  - Update multiple results to generate aggregation data
  - Verify min, max, avg, sum are calculated correctly
  - Validate logical relationships between aggregation values

#### Negative Tests:
- **Invalid target parameter**: Request with unsupported target value
  - Verify 400 status code
  - Validate error message mentions "Invalid target cluster"

- **Missing experiment_name**: POST without experiment_name parameter
  - Verify 400 status code
  - Validate error message indicates missing parameter

- **Missing interval_end_time**: POST for remote target without interval_end_time
  - Verify 400 status code
  - Validate error message indicates missing parameter

- **Invalid timestamp format**: POST with malformed interval_end_time
  - Verify 400 status code
  - Validate error message describes format issue

- **Non-existing experiment**: Generate recommendations for non-existing experiment
  - Verify 400 status code
  - Validate appropriate error response

### **Extended Tests**

- **Multiple experiments with different targets**: Test mixed local and remote experiments
- **All terms validation**: Validate replicas and pod_count across short, medium, and long terms
- **Varying pod counts**: Test with different pod count scenarios and validate replica recommendations
- **Accelerator workloads**: Validate pod_count metrics for GPU/accelerator workloads

## New Helper Functions

The tests utilize three new helper functions added to `tests/scripts/helpers/utils.py`:

### 1. `validate_v1_kubernetes_object(k8s_obj, validate_replicas=True, validate_pod_count=True)`
Validates kubernetes object structure in v1.0 recommendations format.

**Parameters:**
- `k8s_obj`: Kubernetes object from recommendations response
- `validate_replicas`: Whether to validate replicas field (default: True)
- `validate_pod_count`: Whether to validate pod_count metrics (default: True)

**Returns:** Dictionary with validation results
```python
{
    'replicas_validated': bool,
    'pod_count_validated': bool,
    'resources_validated': bool,
    'recommendations_found': bool
}
```

**Validations performed:**
- Replicas field presence and numeric type
- Non-negative replicas values
- Nested resources structure (requests/limits under resources map)
- Pod count aggregation fields (min, max, avg, sum)
- Numeric types for all aggregation values
- Non-negative aggregation values
- Logical relationships (min ≤ avg ≤ max)

### 2. `validate_complete_v1_recommendations(experiment_data, validate_replicas=True, validate_pod_count=True)`
Performs comprehensive validation of entire experiment recommendations.

**Parameters:**
- `experiment_data`: Single experiment object from recommendations response
- `validate_replicas`: Whether to validate replicas field (default: True)
- `validate_pod_count`: Whether to validate pod_count metrics (default: True)

**Returns:** Dictionary with comprehensive validation results
```python
{
    'experiment_name_present': bool,
    'kubernetes_objects_present': bool,
    'k8s_obj_validations': [list of k8s_obj validation results]
}
```

**Validations performed:**
- Experiment name presence
- Kubernetes objects array presence and structure
- Validates each kubernetes object using `validate_v1_kubernetes_object()`

### 3. `validate_error_response(response, expected_status_code=400, expected_message_fragment=None)`
Validates error response structure and content.

**Parameters:**
- `response`: HTTP response object
- `expected_status_code`: Expected HTTP status code (default: 400)
- `expected_message_fragment`: Optional fragment that should be in error message

**Returns:** True if validation passed

**Validations performed:**
- Status code matches expected value
- Error message is present and non-empty
- Error message contains expected fragment (if provided)
- Handles both JSON and text error responses

## Prerequisites for Running the Tests

- Minikube setup or access to Openshift cluster
- Tools: kubectl, oc, curl, jq, python3
- Python modules: pytest, json, pytest-html, requests, jinja2
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

1. Deploy Kruize using deploy.sh from the kruize autotune repo
2. Create the performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md)
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
├── __init__.py                      # Python package marker
└── rest_apis/
    ├── __init__.py                  # Python package marker
    └── test_recommendation_resource_api.py  # Main test file
```

## Key Differences from Remote Monitoring Tests

The recommendation tests have been separated from remote_monitoring_tests because:

1. **Applicable to both modes**: These tests work for both local and remote monitoring
2. **New API version**: Tests the v1.0 recommendations API with enhanced schema
3. **Focused scope**: Specifically tests recommendation generation and retrieval
4. **Reusable validations**: Provides helper functions that can be used across test suites

## Integration with CI/CD

The recommendation tests are integrated into the main test suite and can be run as part of:
- Pull request validation
- Nightly test runs
- Release validation

The test results are captured in HTML reports and JUnit XML format for easy integration with CI/CD pipelines.

## Troubleshooting

### Common Issues

**Issue**: Tests fail with "Invalid target cluster" error
- **Solution**: Ensure Kruize is deployed with ROS enabled (`isROSEnabled=true`)

**Issue**: Local monitoring tests fail
- **Solution**: Verify datasource, metric profile, and metadata profile are properly configured

**Issue**: Pod count validation fails
- **Solution**: Ensure sufficient results are updated to generate aggregation data

**Issue**: Import errors for helper functions
- **Solution**: Verify `tests/scripts/helpers/utils.py` contains the new validation functions

### Debug Mode

Run tests in verbose mode to see detailed output:
```bash
pytest -v -s test_recommendation_resource_api.py::test_name --cluster_type minikube
```

### Logs

Check Kruize pod logs for detailed error information:
```bash
kubectl logs -n monitoring <kruize-pod-name>
```

## Contributing

When adding new tests:
1. Follow the existing test structure and naming conventions
2. Use the provided helper functions for validation
3. Add appropriate pytest markers (@pytest.mark.sanity, @pytest.mark.negative, etc.)
4. Update this documentation with new test scenarios
5. Ensure tests work for both minikube and openshift clusters

## Related Documentation

- [Recommendation Resource API Design](/design/RecommendationResourceAPI.md)
- [Monitoring Mode API](/design/MonitoringModeAPI.md)
- [Performance Profile API](/design/PerformanceProfileAPI.md)
- [Metric Profile API](/design/MetricProfileAPI.md)
- [Metadata Profile API](/design/MetadataProfileAPI.md)