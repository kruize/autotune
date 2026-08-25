"""
Copyright (c) 2025 IBM Corporation and others.

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
import tempfile

import pytest
import sys


sys.path.append("../../")

from helpers.fixtures import *
from helpers.utils import *


# Custom Terms Auto-Population Tests (via listExperiments)
@pytest.mark.sanity
def test_list_exp_mixed_terms_and_partial_auto_population(cluster_type):
    """
    Test Description: This comprehensive test validates auto-population behavior for:
    1. Default terms (weekly, monthly) are auto-populated with system-defined values
    2. Custom term with 1 field (duration_in_days only) gets all missing fields auto-populated
    3. Custom term with 2 fields (duration_in_days + plots_datapoint) gets remaining fields auto-populated
    4. Custom term with 3 fields (all except plots_datapoint_delta_in_days) gets only missing field auto-populated
    Also ensuring the user provided value is preserved for custom terms
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add mixed default and custom terms with varying levels of specification
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "weekly": {},  # Default term
                "monthly": {},  # Default term
                "custom_sprint": {
                    "duration_in_days": 14.0  # Custom term with 1 field
                },
                "custom_quarter": {
                    "duration_in_days": 90.0,
                    "plots_datapoint": 30  # Custom term with 2 fields
                },
                "custom_partial": {
                    "duration_in_days": 7.0,
                    "duration_threshold": "4 days",
                    "plots_datapoint": 14  # Custom term with 3 fields
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_mixed_partial_auto_population.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment
    response = create_experiment(tmp_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    # List experiments to verify auto-population behavior
    experiment_name = json_data[0]['experiment_name']
    results = "false"
    recommendations = "false"
    latest = "false"
    response = list_experiments(results, recommendations, latest, experiment_name, rm=True)

    list_exp_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    terms = list_exp_json[0]["recommendation_settings"]["term_settings"]["terms"]

    # Verify default terms are auto-populated with system-defined values
    assert "weekly" in terms
    assert "duration_in_days" in terms["weekly"]
    assert terms["weekly"]["duration_in_days"] == 7.0
    assert "plots_datapoint" in terms["weekly"]
    assert terms["weekly"]["plots_datapoint"] == 7
    assert "plots_datapoint_delta_in_days" in terms["weekly"]
    assert terms["weekly"]["plots_datapoint_delta_in_days"] == 1.0

    assert "monthly" in terms
    assert "duration_in_days" in terms["monthly"]
    assert terms["monthly"]["duration_in_days"] == 30.0
    assert "plots_datapoint" in terms["monthly"]
    assert terms["monthly"]["plots_datapoint"] == 30
    assert "plots_datapoint_delta_in_days" in terms["monthly"]
    assert terms["monthly"]["plots_datapoint_delta_in_days"] == 1.0

    # Verify custom_sprint (1 field) has all missing fields auto-populated
    assert "custom_sprint" in terms
    custom_sprint = terms["custom_sprint"]
    assert custom_sprint["duration_in_days"] == 14.0
    assert "duration_threshold" in custom_sprint
    assert "plots_datapoint" in custom_sprint
    assert custom_sprint["plots_datapoint"] == 14  # Auto-populated: equals duration_in_days
    assert "plots_datapoint_delta_in_days" in custom_sprint
    assert custom_sprint["plots_datapoint_delta_in_days"] == 1.0

    # Verify custom_quarter (2 fields) has remaining fields auto-populated
    assert "custom_quarter" in terms
    custom_quarter = terms["custom_quarter"]
    assert custom_quarter["duration_in_days"] == 90.0
    assert "duration_threshold" in custom_quarter
    assert custom_quarter["plots_datapoint"] == 30  # User-provided value preserved
    assert "plots_datapoint_delta_in_days" in custom_quarter
    assert custom_quarter["plots_datapoint_delta_in_days"] == 1.0

    # Verify custom_partial (3 fields) has only missing field auto-populated
    assert "custom_partial" in terms
    custom_partial = terms["custom_partial"]
    assert custom_partial["duration_in_days"] == 7.0  # User-provided
    assert custom_partial["duration_threshold"] == "4 days"  # User-provided
    assert custom_partial["plots_datapoint"] == 14  # User-provided
    assert "plots_datapoint_delta_in_days" in custom_partial
    assert custom_partial["plots_datapoint_delta_in_days"] == 1.0  # Auto-populated

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)