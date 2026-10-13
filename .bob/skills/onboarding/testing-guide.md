# Kruize Autotune Testing Guide

## Testing Philosophy

Kruize follows a comprehensive testing strategy:
1. **Unit Tests** - Java JUnit tests for business logic
2. **Integration Tests** - API-level tests using Python pytest
3. **Stress Tests** - Load testing for scalability
4. **Fault Tolerance Tests** - Resilience testing

## Test Structure Overview

```
tests/
├── README.md                          # Testing overview
├── test_autotune.sh                   # Main test runner script
├── scripts/
│   ├── helpers/                       # Test utilities and fixtures
│   │   ├── fixtures.py               # Pytest fixtures
│   │   ├── kruize.py                 # Kruize API helper functions
│   │   ├── utils.py                  # Constants and utilities
│   │   └── *_validate.py             # JSON schema validators
│   ├── local_monitoring_tests/        # Local monitoring mode tests
│   │   ├── rest_apis/                # API tests
│   │   │   ├── test_*.py             # Test files
│   │   │   └── ...
│   │   └── json_files/               # Test data files
│   └── remote_monitoring_tests/       # Remote monitoring mode tests
│       ├── rest_apis/                # API tests
│       ├── fault_tolerant_tests/     # Fault tolerance tests
│       └── stress_test/              # Stress tests
└── requirements.txt                   # Python dependencies
```

## Running Tests

### Quick Start

```bash
cd tests

# Run all local monitoring sanity tests
cd scripts/local_monitoring_tests/rest_apis
pytest -m sanity -v

# Run all remote monitoring sanity tests
cd scripts/remote_monitoring_tests/rest_apis
pytest -m sanity -v

# Run specific test file
pytest test_create_experiment.py -v

# Run specific test function
pytest test_create_experiment.py::test_create_exp_with_different_cluster_type -v
```

### Test Markers

Kruize uses pytest markers to categorize tests:

```python
@pytest.mark.sanity         # Core functionality tests
@pytest.mark.negative       # Error handling tests
@pytest.mark.security       # Security-focused tests
@pytest.mark.layers         # Layer API tests
@pytest.mark.experimental   # Experimental features
@pytest.mark.slow           # Long-running tests
```

**Run specific marker:**
```bash
pytest -m sanity          # Run only sanity tests
pytest -m negative        # Run only negative tests
pytest -m "sanity and not slow"  # Sanity but skip slow tests
```

### Using test_autotune.sh

The main test runner provides comprehensive testing:

```bash
cd tests

# Run all tests with cleanup
./test_autotune.sh -c minikube -t

# Run specific testsuite
./test_autotune.sh -c minikube --testsuite=local_monitoring_tests

# Run specific testcase
./test_autotune.sh -c minikube \
  --testsuite=local_monitoring_tests \
  --testcase=test_list_datasources

# Skip setup (if Kruize already deployed)
./test_autotune.sh -c minikube --skipsetup

# Specify custom results directory
./test_autotune.sh -c minikube --resultsdir=/tmp/kruize-results
```

**Common Options:**
- `-c` : Cluster type (minikube, kind, openshift)
- `-i` : Custom Kruize Docker image
- `--testsuite` : Specific test suite to run
- `--testcase` : Specific test case to run
- `--skipsetup` : Skip Kruize deployment
- `-t` : Cleanup after tests
- `--cleanup_prometheus` : Also cleanup Prometheus

## Test Architecture

### 1. Fixtures (`helpers/fixtures.py`)

Fixtures provide setup/teardown for tests:

```python
@pytest.fixture
def cluster_type(request):
    """Provides cluster type from command line"""
    return request.config.getoption("--cluster-type")

@pytest.fixture(scope="function", autouse=True)
def setup_and_teardown():
    """Runs before and after each test"""
    # Setup code
    yield
    # Teardown code
```

**Key Fixtures:**
- `cluster_type` - Get cluster type (minikube/kind/openshift)
- `setup_and_teardown` - Auto cleanup after each test

### 2. Helper Functions (`helpers/kruize.py`)

Reusable API interaction functions:

```python
# Create experiment
def create_experiment(input_json_file):
    url = URL + "/createExperiment"
    response = requests.post(url, json=input_json)
    return response

# Update results
def update_results(input_json_file):
    url = URL + "/updateResults"
    response = requests.post(url, json=input_json)
    return response

# List recommendations
def list_recommendations(experiment_name):
    url = URL + f"/listRecommendations?experiment_name={experiment_name}"
    response = requests.get(url)
    return response
```

### 3. Constants and Utilities (`helpers/utils.py`)

All error messages and status codes:

```python
SUCCESS_STATUS_CODE = 201
ERROR_STATUS_CODE = 400
SUCCESS_STATUS = "SUCCESS"
ERROR_STATUS = "ERROR"

CREATE_EXP_SUCCESS_MSG = "Experiment registered successfully..."
LAYER_DUPLICATE_MSG = "Layer already exists with name: %s"
# ... hundreds more
```

### 4. JSON Test Data

Tests use JSON templates with Jinja2:

**Template** (`json_files/create_layer_template.json`):
```json
{
  "apiVersion": "{{apiVersion}}",
  "layer_name": {{layer_name|tojson}},
  "layer_presence": {{layer_presence}},
  "tunables": {{tunables}}
}
```

**Usage in test:**
```python
from jinja2 import Environment, FileSystemLoader

environment = Environment(loader=FileSystemLoader("../json_files/"))
template = environment.get_template("create_layer_template.json")

content = template.render(
    apiVersion="recommender.com/v1",
    layer_name="test-layer",
    layer_presence='{"presence": "always"}',
    tunables='[...]'
)
```

## Writing Tests

### Test Structure Pattern

```python
@pytest.mark.layers
@pytest.mark.sanity
@pytest.mark.parametrize("test_name, expected_value, input_data", [
    ("valid_input", "success", "data1"),
    ("another_case", "success", "data2"),
])
def test_create_layer_positive(test_name, expected_value, input_data, cluster_type):
    """
    Test Description: What this test validates
    """
    # 1. Setup
    form_kruize_url(cluster_type)
    tmp_json_file = f"/tmp/test_{test_name}.json"
    
    # 2. Prepare test data
    # Use template or build JSON manually
    
    try:
        # 3. Execute API call
        response = create_layer(tmp_json_file)
        data = response.json()
        
        # 4. Assertions
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == expected_msg
        
        # 5. Cleanup
        delete_layer(layer_name)
    finally:
        # 6. Always cleanup temp files
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)
```

### Parametrized Tests

Use `@pytest.mark.parametrize` for multiple test cases:

```python
@pytest.mark.parametrize("test_name, error_msg, layer_name", [
    ("null_name", LAYER_NAME_NULL_MSG, None),
    ("empty_name", LAYER_NAME_EMPTY_MSG, ""),
    ("invalid_chars", LAYER_NAME_INVALID_MSG, "test@layer"),
])
def test_create_layer_name_validation(test_name, error_msg, layer_name, cluster_type):
    # Test implementation
```

### Negative Tests

Always test error cases:

```python
@pytest.mark.layers
@pytest.mark.negative
def test_create_layer_duplicate(cluster_type):
    """
    Test Description: Validates API rejects duplicate layer names
    """
    form_kruize_url(cluster_type)
    
    # Create first layer
    response1 = create_layer(input_json_file)
    assert response1.status_code == SUCCESS_STATUS_CODE
    
    # Attempt duplicate
    response2 = create_layer(input_json_file)
    data2 = response2.json()
    
    # Verify rejection
    assert response2.status_code == ERROR_409_STATUS_CODE
    assert data2['message'] == LAYER_DUPLICATE_MSG % layer_name
    
    # Cleanup
    delete_layer(layer_name)
```

## Common Test Patterns

### 1. Test with Cleanup

```python
def test_feature(cluster_type):
    layer_name = "test-layer"
    
    # Cleanup before test (ensure clean state)
    delete_layer(layer_name)
    
    # Run test
    response = create_layer(...)
    assert response.status_code == SUCCESS_STATUS_CODE
    
    # Cleanup after test
    delete_layer(layer_name)
```

### 2. Test with Temp Files

```python
def test_with_json(cluster_type):
    tmp_json_file = f"/tmp/test_{test_name}.json"
    
    try:
        # Create temp file
        with open(tmp_json_file, "w") as f:
            json.dump(data, f)
        
        # Use it
        response = create_layer(tmp_json_file)
        
    finally:
        # Always cleanup
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)
```

### 3. Test Existing Manifests

```python
from helpers.utils import get_layer_dir

def test_manifest_layers(cluster_type):
    layer_dir = get_layer_dir()  # Points to manifests/autotune/layers/
    
    input_json_file = layer_dir / 'container-config.json'
    response = create_layer(input_json_file)
    
    assert response.status_code == SUCCESS_STATUS_CODE
```

## Test Examples

### Example 1: Simple Positive Test

```python
@pytest.mark.layers
@pytest.mark.sanity
def test_create_layer_basic(cluster_type):
    """
    Test Description: Creates a layer with minimum required fields
    """
    form_kruize_url(cluster_type)
    layer_name = "test-basic-layer"
    
    json_obj = {
        "apiVersion": "recommender.com/v1",
        "kind": "KruizeLayer",
        "metadata": {"name": layer_name},
        "layer_name": layer_name,
        "layer_presence": {"presence": "always"},
        "tunables": [{
            "name": "t1",
            "value_type": "double",
            "upper_bound": "100",
            "lower_bound": "10",
            "step": 1
        }]
    }
    
    tmp_file = "/tmp/test_basic.json"
    try:
        with open(tmp_file, "w") as f:
            json.dump(json_obj, f)
        
        delete_layer(layer_name)  # Clean state
        
        response = create_layer(tmp_file)
        data = response.json()
        
        assert response.status_code == 201
        assert data['status'] == "SUCCESS"
        
        delete_layer(layer_name)  # Cleanup
    finally:
        if os.path.exists(tmp_file):
            os.remove(tmp_file)
```

### Example 2: Parametrized Negative Test

```python
@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_error, tunables", [
    ("null_tunables", LAYER_TUNABLES_NULL_MSG, 'null'),
    ("empty_tunables", LAYER_TUNABLES_EMPTY_MSG, '[]'),
])
def test_tunable_validation(test_name, expected_error, tunables, cluster_type):
    form_kruize_url(cluster_type)
    
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_layer_template.json")
    
    content = template.render(
        apiVersion="recommender.com/v1",
        kind="KruizeLayer",
        metadata_name="test",
        layer_name="test",
        details="test",
        layer_presence='{"presence": "always"}',
        tunables=tunables
    )
    
    tmp_file = f"/tmp/test_{test_name}.json"
    try:
        with open(tmp_file, "w") as f:
            f.write(content)
        
        response = create_layer(tmp_file)
        data = response.json()
        
        assert response.status_code == 400
        assert data['message'] == expected_error
        
        print(f"✓ Correctly rejected: {test_name}")
    finally:
        if os.path.exists(tmp_file):
            os.remove(tmp_file)
```

## Debugging Tests

### View Test Output

```bash
# Verbose output
pytest test_file.py -v

# Very verbose (show print statements)
pytest test_file.py -vv -s

# Show locals on failure
pytest test_file.py -l

# Stop on first failure
pytest test_file.py -x
```

### Check Kruize Logs During Tests

```bash
# In separate terminal
kubectl logs -n monitoring -l app=kruize --tail=100 -f
```

### Run Single Test with Debugging

```python
# Add breakpoint in test
import pdb; pdb.set_trace()

# Run test
pytest test_file.py::test_function_name -s
```

## Best Practices

1. **Always cleanup** - Use `delete_layer()` before and after tests
2. **Use fixtures** - Share setup code via fixtures
3. **Parametrize** - Test multiple scenarios with one function
4. **Clear names** - Test names should describe what they test
5. **Good descriptions** - Add docstrings explaining the test
6. **Test data isolation** - Use unique names per test
7. **Error messages** - Check exact error message strings
8. **Status codes** - Always verify HTTP status codes
9. **Response structure** - Validate JSON structure, not just values

## Running Specific Test Suites

```bash
# Local monitoring tests
cd tests/scripts/local_monitoring_tests/rest_apis
pytest -m sanity

# Remote monitoring tests
cd tests/scripts/remote_monitoring_tests/rest_apis
pytest -m sanity

# Layer tests only
pytest -m layers

# All sanity tests except slow ones
pytest -m "sanity and not slow"
```

## Test Coverage Areas

### Local Monitoring Tests
- List datasources
- Create/List metadata profiles
- Import/List metadata
- Create/Update/List/Delete metric profiles
- Create/Update/List/Delete performance profiles
- Create/List/Delete layers

### Remote Monitoring Tests
- Create experiments
- Update results
- List recommendations
- Bulk operations
- Stress testing
- Fault tolerance

## Next Steps

1. Run existing tests to understand patterns
2. Pick a test file and read through it
3. Modify a test slightly and run it
4. Write a simple new test case
5. Graduate to complex parametrized tests

## Resources

- `/tests/README.md` - Official testing documentation
- `/tests/scripts/helpers/` - Helper functions and fixtures
- Existing test files - Best examples of patterns
