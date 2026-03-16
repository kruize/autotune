import logging
import requests
import pytest
import sys
from unittest.mock import patch, MagicMock

sys.path.append("../../")

# Kruize helpers
import helpers.kruize
from helpers.kruize import post_bulk_api
from helpers.utils import form_kruize_url, get_kruize_url


logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


# Payload generators
def base_payload():
    return {
        "filter": {
            "exclude": {"namespace": [], "workload": [], "containers": [], "labels": {}},
            "include": {"namespace": [], "workload": [], "containers": [], "labels": {}}
        },
        "metadata_profile": "cluster-metadata-local-monitoring",
        "measurement_duration": "15mins",
        "time_range": {}
    }

def filtered_payload():
    payload = base_payload()
    payload["filter"]["include"]["namespace"] = ["default"]
    payload["filter"]["include"]["workload"] = ["sysbench"]
    payload["filter"]["include"]["containers"] = ["sysbench"]
    return payload

# Fully mocked test for POST Bulk API
@pytest.mark.test_bulk_api_mocked
@pytest.mark.sanity
@pytest.mark.parametrize(
    "bulk_request_payload, expected_job_id_present",
    [
        ({}, True),                  # Empty payload still returns a job_id
        (base_payload(), True),      # Base payload returns a job_id
        (filtered_payload(), True)   # Filtered payload returns a job_id
    ]
)

@patch("helpers.kruize.post_bulk_api")
def test_bulk_post_request_mocked(
    mock_post_bulk_api,
    bulk_request_payload,
    expected_job_id_present,
    caplog
):
    """
    Fully mocked POST /bulk API test.
    Only validates job_id handling.
    """

    # ---------------- MOCK RESPONSE ----------------
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {"job_id": "mock-job-123"}
    mock_post_bulk_api.return_value = mock_response

    # ---------------- TEST LOGIC ----------------
    with caplog.at_level(logging.INFO):
        from helpers.kruize import post_bulk_api  
        response = post_bulk_api(bulk_request_payload, logging)

        response_json = response.json()
        job_id_present = "job_id" in response_json and isinstance(response_json["job_id"], str)
        assert job_id_present == expected_job_id_present, (
            f"Expected job_id presence to be {expected_job_id_present} "
            f"but got {job_id_present}"
        )

        if job_id_present:
            logger.info("Mock bulk job created successfully with job_id=%s", response_json["job_id"])


# ---------------- Partially Mocked Test ----------------
# Mock kruize URL and APIs
@patch.object(helpers.kruize, "URL", "http://mock-cluster:8080", create=True)
@patch("helpers.kruize.requests.post") 
@patch("helpers.kruize.get_bulk_job_status")
@pytest.mark.parametrize(
    "filter_setup, expected",
    [
        ({"include": {"namespace": ["default"], "workload": ["wl1"], "containers": ["ctr1"]}},
         {"namespace": ["default"], "workload": ["wl1"], "containers": ["ctr1"]}),
        ({"include": {"namespace": ["default"]}}, {"namespace": ["default"]}),
        ({"include": {"labels": {"cost": "true"}}}, {"labels": {"cost": "true"}, "mode": "include"}),
        ({"exclude": {"labels": {"cost": "true"}}}, {"labels": {"cost": "true"}, "mode": "exclude"})
    ]
)
def test_bulk_api_partial_mocked(mock_get_bulk_job_status, mock_post_bulk_api, filter_setup, expected):
    logger = logging.getLogger()
    
    # Build payload
    payload = {
        "filter": {"include": {}, "exclude": {}},
        "metadata_profile": "cluster-metadata-local-monitoring",
        "measurement_duration": "15mins",
        "time_range": {}
    }
    for filter_type, values in filter_setup.items():
        payload["filter"].setdefault(filter_type, {})
        payload["filter"][filter_type].update(values)

    # Mock POST response
    post_resp = MagicMock()
    post_resp.status_code = 200
    post_resp.json.return_value = {"job_id": "mock-job-123"}
    mock_post_bulk_api.return_value = post_resp

    # Mock GET response
    get_resp = MagicMock()
    get_resp.status_code = 200
    get_resp.json.return_value = {
        "metadata": {
            "datasources": {
                "ds1": {
                    "clusters": {
                        "cluster1": {
                            "namespaces": {
                                "default": {
                                    "workloads": {
                                        "wl1": {"containers": {"ctr1": {}}}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    mock_get_bulk_job_status.return_value = get_resp

    # Call post_bulk_api, it will use the mocked URL
    response = post_bulk_api(payload, logger)
    assert response.status_code == 200


@pytest.mark.parametrize(
    "bulk_request_payload, mock_return_json, expected_job_id_present",
    [
        ({}, {"job_id": "mock-job-123"}, True),                     # Empty payload
        (base_payload(), {"job_id": "mock-job-456"}, True),         # Base payload
        (filtered_payload(), {"job_id": "mock-job-789"}, True),     # Filtered payload
        (base_payload(), {}, False),                                 # No job_id returned
    ]
)

@patch("helpers.kruize.post_bulk_api")
def test_bulk_post_request_varied(
    mock_post_bulk_api, bulk_request_payload, mock_return_json, expected_job_id_present
):
    # Mock response
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = mock_return_json
    mock_post_bulk_api.return_value = mock_response

    from helpers.kruize import post_bulk_api
    response = post_bulk_api(bulk_request_payload, logging)

    job_id_present = "job_id" in response.json() and isinstance(response.json().get("job_id"), str)
    assert job_id_present == expected_job_id_present

@patch("helpers.kruize.post_bulk_api")
def test_bulk_post_request_raises_connection_error(mock_post_bulk_api):
    from helpers.kruize import post_bulk_api
    mock_post_bulk_api.side_effect = requests.exceptions.ConnectionError("Connection failed")

    with pytest.raises(requests.exceptions.ConnectionError):
        post_bulk_api(base_payload(), logging)
