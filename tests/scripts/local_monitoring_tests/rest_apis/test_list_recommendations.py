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
import datetime
import json
import time

import pytest
import sys
sys.path.append("../../")

from helpers.all_terms_list_reco_json_schema import all_terms_list_reco_json_schema
from helpers.fixtures import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.list_reco_json_local_monitoring_schema import *
from helpers.medium_and_long_term_list_reco_json_schema import medium_and_long_term_list_reco_json_schema
from helpers.medium_term_list_reco_json_schema import *
from helpers.long_term_list_reco_json_schema import *
from helpers.list_reco_json_validate import *
from helpers.short_and_long_term_list_reco_json_schema import short_and_long_term_list_reco_json_schema
from helpers.short_and_medium_term_list_reco_json_schema import short_and_medium_term_list_reco_json_schema
from helpers.short_term_list_reco_json_schema import short_term_list_reco_json_schema
from helpers.utils import *



@pytest.mark.sanity
def test_list_recommendations_namespace_single_result(cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid
    namespace experiment name
    """
    input_json_file = "../json_files/create_namespace_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create namespace experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # generate recommendations
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    exp_name = input_json[0]['experiment_name']

    response = generate_recommendations(exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Invoke list recommendations for the specified experiment
    response = list_recommendations(exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_namespace_json_local_monitoring_schema)
    assert errorMsg == ""

    # Validate the json values
    namespace_exp_json = read_json_data_from_file(input_json_file)
    validate_local_monitoring_reco_json(namespace_exp_json[0], list_reco_json[0])

    # Delete experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE