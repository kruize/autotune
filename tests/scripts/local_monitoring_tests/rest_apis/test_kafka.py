"""
Copyright (c) 2025 Red Hat, IBM Corporation and others.

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
import sys

sys.path.append("../../")
from helpers.fixtures import *
from helpers.utils import *
from helpers.kafka_service import *

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

@pytest.mark.kafka
@pytest.mark.parametrize("topic", ["recommendations-topic", "summary-topic", "error-topic"])
def test_bulk_post_request_kafka(cluster_type, topic):
    expected_job_id_present = True
    bulk_request_payload = {
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
	    "datasource": "prometheus-1",
        "time_range": {}
     }

    form_kruize_url(cluster_type)
    URL = get_kruize_url()

    # Get the datasources name
    datasource_name = None
    response = list_datasources(datasource_name)
    print(response.json())

    # Invoke Bulk API with the specified bulk configuration
    response = post_bulk_api(bulk_request_payload, logger)

    # Check if job_id is present in the response
    job_id_present = "job_id" in response.json() and isinstance(response.json()["job_id"], str)
    assert job_id_present == expected_job_id_present, f"Expected job_id presence to be {expected_job_id_present} but was {job_id_present}"

    # If a job_id is generated, run the GET request test
    if job_id_present:
        validate_job_status(response.json()["job_id"], URL)

    # Consume messages from the topic
    consume_messages_from_kafka(topic)

@pytest.mark.kafka
def test_bulk_post_request_kafka_error(cluster_type):
    # Prepare a payload that causes an error
    invalid_payload = {
        "filter": {
            "exclude": {
                "namespace": [],
                "workload": [],
                "containers": [],
                "labels": {}
            },
            "include": {
                "namespace": ["abc"],
                "workload": [],
                "containers": [],
                "labels": {}
            }
        },
        "time_range": {}
    }

    # Trigger the Java API with this invalid payload
    response = requests.post(f"{your_java_api_url}/bulkExperiment", json=invalid_payload)
    assert response.status_code != 200  # Expect a failure response

    # Wait for message in error-topic
    error_msg = consume_messages_from_kafka("error-topic", timeout_ms=10000)
    assert error_msg is not None, "Expected error message not found in Kafka error-topic"
    print(f"Received error message: {error_msg}")
