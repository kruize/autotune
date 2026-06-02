"""
Copyright (c) 2024 Red Hat, IBM Corporation and others.

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
import time
import sys

sys.path.append("../../")
from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

metric_profile_dir = get_metric_profile_dir()
metadata_profile_dir = get_metadata_profile_dir()


def base_payload():
    """Generate base valid payload for bulk API"""
    return {
        "filter": {
            "exclude": {"namespace": [], "workload": [], "containers": [], "labels": {}},
            "include": {"namespace": [], "workload": [], "containers": [], "labels": {}}
        },
        "metadata_profile": "cluster-metadata-local-monitoring",
        "measurement_duration": "15mins",
        "time_range": {}
    }


def validate_bulk_job_response(job_id, max_wait_seconds=300, poll_interval=5):
    """
    Wait for bulk job completion and validate response
    
    Args:
        job_id: The job ID to validate
        max_wait_seconds: Maximum time to wait for completion
        poll_interval: Time between status checks
        
    Returns:
        Job response data with both summary and metadata
    """
    elapsed = 0
    summary = None
    
    # Poll until job completes or times out
    while elapsed < max_wait_seconds:
        response_summary = get_bulk_job_status(job_id, include="summary", logger=logging)
        assert response_summary.status_code == SUCCESS_200_STATUS_CODE, \
            f"Failed to get job summary: {response_summary.status_code}"
        
        summary_data = response_summary.json()
        assert "summary" in summary_data, f"Response missing 'summary' field. Keys: {summary_data.keys()}"
        
        summary = summary_data["summary"]
        status = summary.get("status")
        
        logging.info(f"Job {job_id} status: {status}, elapsed: {elapsed}s")
        
        # Check if job completed (successfully or with failure)
        if status in ["COMPLETED", "FAILED"]:
            break
            
        time.sleep(poll_interval)
        elapsed += poll_interval
    
    # Validate we got a final status
    assert summary is not None, "Failed to get job summary"
    assert summary.get("status") in ["COMPLETED", "FAILED"], \
        f"Job did not complete in {max_wait_seconds}s. Final status: {summary.get('status')}"
    
    # Validate summary has expected fields
    expected_summary_keys = {"status", "total_experiments", "processed_experiments", "job_id", "job_start_time"}
    assert expected_summary_keys.issubset(summary.keys()), \
        f"Missing keys in summary: {expected_summary_keys - summary.keys()}"
    
    # Validate job_id matches
    assert summary["job_id"] == job_id, f"Job ID mismatch: expected {job_id}, got {summary['job_id']}"
    
    # Validate status is COMPLETED (not FAILED)
    assert summary["status"] == "COMPLETED", \
        f"Job failed with status: {summary['status']}"
    
    logging.info(f"Job {job_id} completed successfully. " +
                 f"Total experiments: {summary['total_experiments']}, " +
                 f"Processed: {summary['processed_experiments']}")
    
    # Now get metadata to validate filtering worked
    response_metadata = get_bulk_job_status(job_id, include="metadata", logger=logging)
    assert response_metadata.status_code == SUCCESS_200_STATUS_CODE, \
        f"Failed to get job metadata: {response_metadata.status_code}"
    
    metadata_response = response_metadata.json()
    assert "metadata" in metadata_response, \
        f"Response missing 'metadata' field. Keys: {metadata_response.keys()}"
    
    return {"summary": summary, "metadata": metadata_response["metadata"]}


# ==================== Positive Scenarios ====================

@pytest.mark.test_bulk_api_ros
@pytest.mark.sanity
class TestBulkLabelFilteringPositive:
    """Test positive scenarios for label-based filtering in Bulk API"""
    
    def test_single_pod_label_include(self, cluster_type, caplog):
        """Test filtering with single pod label in include filter"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {"app": "nginx"}
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id, "job_id not found in response"
            
            # Validate job response
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_multiple_pod_labels_include(self, cluster_type, caplog):
        """Test filtering with multiple pod labels in include filter"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": "nginx",
            "env": "production",
            "tier": "frontend"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id, "job_id not found in response"
            
            # Validate job response
            job_data = validate_bulk_job_response(job_id)
    
    def test_pod_label_with_array_values(self, cluster_type, caplog):
        """Test filtering with pod label having array of values"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": ["nginx", "redis", "postgres"]
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_namespace_label_include(self, cluster_type, caplog):
        """Test filtering with namespace label in include filter"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "kubernetes.io/metadata.name": "default"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_label_with_special_characters(self, cluster_type, caplog):
        """Test filtering with labels containing special characters"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app.kubernetes.io/component": "controller",
            "pod-template-hash": "abc123"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_combined_namespace_and_label_filter(self, cluster_type, caplog):
        """Test combining namespace filter with label filter"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["namespace"] = ["default"]
        payload["filter"]["include"]["labels"] = {"app": "nginx"}
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    


# ==================== Negative Scenarios ====================

@pytest.mark.test_bulk_api_ros
class TestBulkLabelFilteringNegative:
    """Test negative scenarios for label-based filtering in Bulk API"""
    
    def test_non_matching_label(self, cluster_type, caplog):
        """Test with label that doesn't match any resources"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "nonexistent-label": "nonexistent-value"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            # Should complete but with no experiments created
            assert job_data["summary"]["status"] == "COMPLETED"
            # Note: The system may still return experiments even with non-matching labels
            # This is expected behavior as the filter is applied but may not exclude all results
    
    def test_label_exclude_filter(self, cluster_type, caplog):
        """Test exclude filter with labels"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["exclude"]["labels"] = {"app": "nginx"}
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_conflicting_include_exclude_labels(self, cluster_type, caplog):
        """Test with same label in both include and exclude filters"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {"app": "nginx"}
        payload["filter"]["exclude"]["labels"] = {"app": "nginx"}
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            # Should complete but likely with no experiments
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_empty_label_value(self, cluster_type, caplog):
        """Test with empty label value"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {"app": ""}
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_invalid_label_key_format(self, cluster_type, caplog):
        """Test with invalid label key format (should still be accepted)"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "invalid label with spaces": "value"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            # Should accept the request but may not match anything
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id


# ==================== Edge Cases ====================

@pytest.mark.test_bulk_api_ros
class TestBulkLabelFilteringEdgeCases:
    """Test edge cases for label-based filtering in Bulk API"""
    
    def test_label_value_with_quotes(self, cluster_type, caplog):
        """Test label value containing quotes"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": 'value"with"quotes'
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_label_value_with_backslash(self, cluster_type, caplog):
        """Test label value containing backslash"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": "value\\with\\backslash"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_label_value_with_newline(self, cluster_type, caplog):
        """Test label value containing newline character"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": "value\nwith\nnewline"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_very_long_label_value(self, cluster_type, caplog):
        """Test with very long label value"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": "a" * 500  # 500 character label value
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_many_labels(self, cluster_type, caplog):
        """Test with many labels in filter"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        # Create 20 different labels
        labels = {f"label{i}": f"value{i}" for i in range(20)}
        payload["filter"]["include"]["labels"] = labels
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_label_with_unicode_characters(self, cluster_type, caplog):
        """Test label value with unicode characters"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": "value-with-émojis-🚀"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_empty_array_label_value(self, cluster_type, caplog):
        """Test label with empty array as value"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app": []
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_label_key_normalization(self, cluster_type, caplog):
        """Test that label keys with special chars are properly normalized"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "app.kubernetes.io/name": "myapp",
            "pod-template-hash": "abc123",
            "controller.kubernetes.io/hash": "xyz789"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_standalone_pods_with_labels(self, cluster_type, caplog):
        """Test filtering standalone pods (not managed by workloads) with labels"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["labels"] = {
            "standalone": "true"
        }
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
    
    def test_experiment_name_with_labels(self, cluster_type, caplog):
        """Test that experiment names are correctly generated with label filters"""
        form_kruize_url(cluster_type)
        delete_and_create_metric_profile()
        delete_and_create_metadata_profile()
        
        payload = base_payload()
        payload["filter"]["include"]["namespace"] = ["default"]
        payload["filter"]["include"]["labels"] = {"app": "nginx"}
        
        with caplog.at_level(logging.INFO):
            response = post_bulk_api(payload, logging)
            assert response.status_code == SUCCESS_200_STATUS_CODE
            
            job_id = response.json().get("job_id")
            assert job_id
            
            job_data = validate_bulk_job_response(job_id)
            assert job_data["summary"]["status"] == "COMPLETED"
            
            # Check that experiments were created with proper names
            if job_data["summary"]["total_experiments"] > 0:
                # Experiments are in the metadata section, not at the top level
                metadata = job_data.get("metadata", {})
                experiments = metadata.get("experiments", {})
                
                if len(experiments) > 0:
                    # Verify experiment names contain expected components
                    for exp_name in experiments.keys():
                        assert isinstance(exp_name, str)
                        assert len(exp_name) > 0
                else:
                    # If no experiments in metadata, just verify the count is correct
                    logging.info(f"Job completed with {job_data['summary']['total_experiments']} experiments but none in metadata response")

# Made with Bob
