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
from helpers.list_metric_profiles_validate import *
from helpers.list_metric_profiles_without_parameters_schema import *
from helpers.short_and_long_term_list_reco_json_schema import short_and_long_term_list_reco_json_schema
from helpers.short_and_medium_term_list_reco_json_schema import short_and_medium_term_list_reco_json_schema
from helpers.short_term_list_reco_json_schema import short_term_list_reco_json_schema
from helpers.utils import *
from jinja2 import Environment, FileSystemLoader


metric_profile_dir = get_metric_profile_dir()

@pytest.mark.sanity
@pytest.mark.namespace_tests
@pytest.mark.parametrize("test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
    [
        ("list_reco_default_cluster1", SUCCESS_STATUS_CODE, "v2.0", "test-default-ns", "cluster-1", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1"),
        ("list_reco_default_cluster2", SUCCESS_STATUS_CODE, "v2.0", "test-default-ns", "cluster-2", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1")
    ]
)
def test_list_recommendations_namespace_single_result(test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid
    namespace experiment name
    """
    # Generate a temporary JSON filename
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"
    print("tmp_json_file = ", tmp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # In case of test_name with "null", strip the specific fields
    if "null" in test_name:
         field = test_name.replace("null_", "")
         json_file = "../json_files/create_exp_template.json"
         filename = "/tmp/create_exp_template.json"
         strip_double_quotes_for_field(json_file, field, filename)
         environment = Environment(loader=FileSystemLoader("/tmp/"))
         template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template
    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
        mode=mode,
        target_cluster=target_cluster,
        datasource=datasource,
        experiment_type=experiment_type,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        namespace_name=namespace_name,
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold
    )

    # Convert rendered content to a dictionary
    json_content = json.loads(content)

    if json_content[0]["kubernetes_objects"][0]["type"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("type")
    if json_content[0]["kubernetes_objects"][0]["name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("name")
    if json_content[0]["kubernetes_objects"][0]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespace")
    if json_content[0]["kubernetes_objects"][0]["containers"][0]["container_image_name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("containers")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    #Install default metric profile
    metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(metric_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(metric_profile_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(name=metric_profile_name, logging=False)
    metric_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert errorMsg == ""

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