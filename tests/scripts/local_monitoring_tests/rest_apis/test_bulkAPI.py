"""
Copyright (c) 2022, 2024 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
import logging
import pytest
import requests
import sys

sys.path.append("../../")
from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


@pytest.mark.sanity
@pytest.mark.parametrize("bulk_request_payload, expected_job_id_present", [
    ({}, True),  # Test with an empty payload to check if a job_id is created.
    ({
         "filter": {
             "exclude": {
                 "namespace": [],
                 "workload": [],
                 "containers": [],
                 "labels": {}
             },
             "include": {
                 "namespace": [],
                 "workload": [],
                 "containers": [],
                 "labels": {}
             }
         },
         "time_range": {}
     }, True)  # Test with a sample payload with some JSON content
])
def test_bulk_post_request(cluster_type, bulk_request_payload, expected_job_id_present, caplog):
    form_kruize_url(cluster_type)
    URL = get_kruize_url()

    with caplog.at_level(logging.INFO):
        # Log request payload and curl command for POST request
        logger.info("Sending POST request to URL: %s", f"{URL}/bulk")
        logger.info("Request Payload: %s", bulk_request_payload)
        curl_command = f"curl -X POST {URL}/bulk -H 'Content-Type: application/json' -d '{json.dumps(bulk_request_payload)}'"
        logger.info("Equivalent cURL command: %s", curl_command)

        # Send the POST request
        response = requests.post(f"{URL}/bulk", json=bulk_request_payload)
        logger.info("Response Status Code: %s", response.status_code)
        logger.info("Response JSON: %s", response.json())

        # Check if job_id is present in the response
        job_id_present = "job_id" in response.json() and isinstance(response.json()["job_id"], str)
        assert job_id_present == expected_job_id_present, f"Expected job_id presence to be {expected_job_id_present} but was {job_id_present}"

        # If a job_id is generated, run the GET request test
        if job_id_present:
            test_get_job_status(response.json()["job_id"], URL, caplog)


def test_get_job_status(job_id, base_url, caplog):
    # Define URLs for both requests
    url_basic = f"{base_url}/bulk?job_id={job_id}"
    url_verbose = f"{base_url}/bulk?job_id={job_id}&verbose=true"

    # Common keys expected in both responses
    common_keys = {
        "status", "total_experiments", "processed_experiments", "job_id", "job_start_time", "job_end_time"
    }

    # Extra keys expected when verbose=true
    verbose_keys = {
        "experiments"
    }

    with caplog.at_level(logging.INFO):
        # Make the GET request without verbose
        logger.info("Sending GET request to URL (basic): %s", url_basic)
        curl_command_basic = f"curl -X GET '{url_basic}'"
        logger.info("Equivalent cURL command (basic): %s", curl_command_basic)
        response_basic = requests.get(url_basic)

        logger.info("Basic GET Response Status Code: %s", response_basic.status_code)
        logger.info("Basic GET Response JSON: %s", response_basic.json())

        # Verify common keys in the basic response
        assert common_keys.issubset(
            response_basic.json().keys()), f"Missing keys in response: {common_keys - response_basic.json().keys()}"

        # Make the GET request with verbose=true
        logger.info("Sending GET request to URL (verbose): %s", url_verbose)
        curl_command_verbose = f"curl -X GET '{url_verbose}'"
        logger.info("Equivalent cURL command (verbose): %s", curl_command_verbose)
        response_verbose = requests.get(url_verbose)

        logger.info("Verbose GET Response Status Code: %s", response_verbose.status_code)
        logger.info("Verbose GET Response JSON: %s", response_verbose.json())

        # Verify common and verbose keys in the verbose response
        assert common_keys.issubset(
            response_verbose.json().keys()), f"Missing keys in verbose response: {common_keys - response_verbose.json().keys()}"
        assert verbose_keys.issubset(
            response_verbose.json().keys()), f"Missing verbose keys in response: {verbose_keys - response_verbose.json().keys()}"
