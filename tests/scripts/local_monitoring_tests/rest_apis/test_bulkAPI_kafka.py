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
def test_bulk_post_request(cluster_type):
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
         "time_range": {
 	    "start": "2025-01-20T00:00:00.000Z",
	    "end": "2025-01-20T00:10:00.000Z"
         }
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
    consume_messages_from_kafka("recommendations-topic")


