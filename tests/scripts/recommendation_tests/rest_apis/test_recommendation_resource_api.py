"""
Copyright (c) 2026 IBM Corporation and others.

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
import shutil
import sys

import pytest
import requests
from jinja2 import Environment, FileSystemLoader

from helpers import v1_list_reco_json_local_monitoring_schema
from helpers.list_datasources_json_schema import list_datasources_json_schema
from helpers.list_datasources_json_validate import validate_list_datasources_json
from helpers.list_metadata_profiles_schema import list_metadata_profiles_schema
from helpers.list_metadata_profiles_validate import validate_list_metadata_profiles_json
from helpers.list_metric_profiles_schema import list_metric_profiles_schema
from helpers.list_metric_profiles_validate import validate_list_metric_profiles_json
from helpers.list_reco_json_local_monitoring_schema import list_reco_json_local_monitoring_schema, \
    list_reco_namespace_json_local_monitoring_schema
from helpers.list_reco_json_validate import validate_list_reco_json
from helpers.fixtures import *

sys.path.append("../../")
from helpers.utils import *


@pytest.mark.remote
def test_get_recommendations_v1_remote_e2e_workflow(cluster_type):
    """
    Test POST /kruize/api/v1/recommendations API with new schema for container and namespace experiments
    follows below steps:
    - Create container and namespace experiments
    - Update results for container and namespace experiments
    - call recommendations API and validates response structure with the new schema
    - validates the presence of replicas field
    - validates nested resources structure
    - validates Pod count metrics
    """
    input_json_file_container = "../../remote_monitoring_tests/json_files/create_exp.json"
    input_json_file_namespace = "../../remote_monitoring_tests/json_files/create_exp_namespace.json"
    result_json_file_container = "../../remote_monitoring_tests/json_files/multiple_results_single_exp.json"
    result_json_file_namespace = "../../remote_monitoring_tests/json_files/multiple_results_single_exp_namespace.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file_container, rm=True)
    print("delete container exp = ", response.status_code)
    response = delete_experiment(input_json_file_namespace, rm=True)
    print("delete namespace exp = ", response.status_code)

    try:
        # Create container experiment
        response = create_experiment(input_json_file_container)
        data = response.json()
        print("container exp response = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG
        # Create namespace experiment
        response = create_experiment(input_json_file_namespace)
        data = response.json()
        print("namespace exp response = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        # Update results for the container experiment
        response = update_results(result_json_file_container, False)

        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        # Get the experiment name
        json_data = json.load(open(input_json_file_container))
        container_experiment_name = json_data[0]['experiment_name']
        end_time = "2023-04-14T23:59:20.982Z"

        # Generate recommendations for container using POST - this returns the recommendations in v1.0 format
        print("container_experiment_name = ", container_experiment_name)
        response = generate_recommendations_v1(container_experiment_name, interval_end_time=end_time)
        data = response.json()
        print("recommendations response = ", data)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        assert data[0]['experiment_name'] == container_experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                   NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE

        response = list_recommendations_v1(container_experiment_name, rm=True)
        list_reco_json_container = response.json()
        assert response.status_code == SUCCESS_200_STATUS_CODE

        # Validate the json against the json schema
        error_msg = validate_list_reco_json(list_reco_json_container, v1_list_reco_json_local_monitoring_schema.v1_list_reco_json_local_monitoring_schema)
        assert error_msg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(input_json_file_container)
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

        update_results_json = []
        result_json_arr = read_json_data_from_file(result_json_file_container)
        update_results_json.append(result_json_arr[len(result_json_arr) - 1])
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json_container[0],
                           expected_duration_in_hours, v1=True)


        # Update results for the namespace experiment
        response = update_results(result_json_file_namespace, False)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        end_time = "2022-01-23T19:25:43.602Z"
        json_data = json.load(open(input_json_file_namespace))
        namespace_experiment_name = json_data[0]['experiment_name']
        # Generate recommendations for namespace using POST - this returns the recommendations in v1.0 format
        response = generate_recommendations_v1(namespace_experiment_name, interval_end_time=end_time)
        data = response.json()
        print("recommendations response = ", data)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        assert data[0]['experiment_name'] == namespace_experiment_name
        assert data[0]['kubernetes_objects'][0]['namespaces']['recommendations']['notifications'][
                   INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                   'message'] == RECOMMENDATIONS_AVAILABLE

        response = list_recommendations_v1(namespace_experiment_name, rm=True)
        list_reco_json_namespace = response.json()
        assert response.status_code == SUCCESS_200_STATUS_CODE

        # Validate the json against the json schema
        error_msg = validate_list_reco_json(list_reco_json_namespace, v1_list_reco_json_local_monitoring_schema.v1_list_reco_namespace_json_local_monitoring_schema)
        assert error_msg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(input_json_file_namespace)
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN

        result_json_arr = read_json_data_from_file(result_json_file_namespace)
        update_results_json = [result_json_arr[len(result_json_arr) - 1]]
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json_namespace[0],
                           expected_duration_in_hours, v1=True)
    finally:
        # Cleanup: Delete the experiments
        # response = delete_experiment(input_json_file_container, rm=True)
        # print("delete container exp = ", response.status_code)
        # response = delete_experiment(input_json_file_namespace, rm=True)
        print("delete namespace exp = ", response.status_code)


@pytest.mark.remote
def test_get_recommendations_v1_invalid_experiment(cluster_type):
    """
    Test GET /kruize/api/v1/recommendations API with non-existing experiment
    Expected: 400 Bad Request with proper error message
    """
    form_kruize_url(cluster_type)

    # Try to get recommendations for non-existing experiment
    response = list_recommendations_v1("non_existing_experiment_12345")
    
    # Validate error response
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)


@pytest.mark.remote
def test_get_recommendations_v1_invalid_timestamp(cluster_type):
    """
    Test GET /kruize/api/v1/recommendations API with invalid timestamp format
    Expected: 400 Bad Request with proper error message
    """
    input_json_file = "../../remote_monitoring_tests/json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG
        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Try to get recommendations with invalid timestamp
        response = list_recommendations_v1(experiment_name, interval_end_time="invalid-timestamp")

        # Validate error response
        validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file, rm=True)
        print("delete exp = ", response.status_code)


@pytest.mark.remote
def test_post_recommendations_v1_without_experiment_name(cluster_type):
    """
    Test POST /kruize/api/v1/recommendations API without experiment_name
    Expected: 400 Bad Request with proper error message
    """
    form_kruize_url(cluster_type)

    # Try to generate recommendations without experiment name
    url = get_kruize_url()
    api_url = f"{url}{RECOMMENDATIONS_API_V1}"
    response = requests.post(api_url, json={})
    
    # Validate error response with status message
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)


@pytest.mark.remote
def test_post_recommendations_v1_without_interval_end_time(cluster_type):
    """
    Test POST /kruize/api/v1/recommendations API without interval_end_time for remote target
    Expected: 400 Bad Request with proper error message
    """
    input_json_file = "../../remote_monitoring_tests/json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)

    # Create experiment
    response = create_experiment(input_json_file)
    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG
    # Get experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    # Try to generate recommendations without interval_end_time
    url = get_kruize_url()
    api_url = f"{url}{RECOMMENDATIONS_API_V1}?experiment_name={experiment_name}"
    response = requests.post(api_url, json={})
    
    # Validate error response with status message
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)



# ============================================================================
# LOCAL MONITORING TESTS
# ============================================================================

@pytest.mark.local
def test_get_recommendations_v1_local_e2e_workflow(cluster_type):
    """
    Test GET /kruize/api/v1/recommendations API with new schema for a single experiment (LOCAL MODE)
    Validates:
    - Response structure with new schema
    - Presence of replicas field
    - Nested resources structure
    - Pod count metrics
    - Complete recommendations validation
    """
    clone_repo("https://github.com/kruize/benchmarks")
    benchmarks_install(name="sysbench", manifests="sysbench.yaml")

    input_json_file = "../../local_monitoring_tests/json_files/create_tfb_exp.json"
    
    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    # Setup paths
    metric_profile_dir = get_metric_profile_dir()
    metadata_profile_dir = get_metadata_profile_dir()

    # List all datasources
    datasource_name = None
    response = list_datasources(datasource_name)

    list_datasource_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    error_msg = validate_list_datasources_json(list_datasource_json, list_datasources_json_schema)
    assert error_msg == ""

    # Install metadata profile
    metadata_profile_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    # Create metadata profile using the specified json
    response = create_metadata_profile(metadata_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    error_msg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert error_msg == ""

    # Import datasource metadata
    input_json_file = "../json_files/import_metadata.json"

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    # Validate the json against the json schema
    error_msg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert error_msg == ""

    # Display metadata from prometheus-1 datasource
    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']

    response = list_metadata(datasource)

    list_metadata_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    error_msg = validate_list_metadata_json(list_metadata_json, list_metadata_json_schema)
    assert error_msg == ""

    # Display metadata for default namespace
    # Currently only default cluster is supported by Kruize
    cluster_name = "default"

    response = list_metadata(datasource=datasource, cluster_name=cluster_name, verbose="true")

    list_metadata_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    error_msg = validate_list_metadata_json(list_metadata_json, list_metadata_json_verbose_true_schema)
    assert error_msg == ""

    # Generate a temporary JSON filename
    tmp_container_exp_json_file = "/tmp/create_exp_sysbench" + ".json"
    tmp_namespace_exp_json_file = "/tmp/create_exp_default_ns" + ".json"
    print("tmp_json_file for container exp = ", tmp_container_exp_json_file)
    print("tmp_json_file for namespace exp = ", tmp_namespace_exp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template

    container_exp_content = template.render(
        version="v2.0", experiment_name="monitor-sysbench", cluster_name="default",
        performance_profile="resource-optimization-local-monitoring",
        metadata_profile="cluster-metadata-local-monitoring", mode="monitor", target_cluster="local",
        datasource="prometheus-1",
        experiment_type="container", kubernetes_obj_type="deployment", name="sysbench", namespace="default",
        namespace_name=None,
        container_image_name="quay.io/kruizehub/sysbench", container_name="sysbench", measurement_duration="2min",
        threshold="0.1"
    )

    namespace_exp_content = template.render(
        version="v2.0", experiment_name="monitor-ns", cluster_name="default",
        performance_profile="resource-optimization-local-monitoring",
        metadata_profile="cluster-metadata-local-monitoring", mode="monitor", target_cluster="local",
        datasource="prometheus-1",
        experiment_type="namespace", kubernetes_obj_type=None, name=None, namespace=None, namespace_name="default",
        container_image_name=None, container_name=None, measurement_duration="2min", threshold="0.1"
    )

    # Convert rendered content to a dictionary
    container_exp_json_content = json.loads(container_exp_content)
    container_exp_json_content[0]["kubernetes_objects"][0].pop("namespaces")

    # Convert rendered content to a dictionary
    namespace_exp_json_content = json.loads(namespace_exp_content)
    namespace_exp_json_content[0]["kubernetes_objects"][0].pop("type")
    namespace_exp_json_content[0]["kubernetes_objects"][0].pop("name")
    namespace_exp_json_content[0]["kubernetes_objects"][0].pop("namespace")
    namespace_exp_json_content[0]["kubernetes_objects"][0].pop("containers")

    # Write the final JSON to the temp file
    with open(tmp_container_exp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(container_exp_json_content, message, indent=4)

    with open(tmp_namespace_exp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(namespace_exp_json_content, message, indent=4)

    # exp json file
    container_exp_json_file = tmp_container_exp_json_file
    namespace_exp_json_file = tmp_namespace_exp_json_file

    response = delete_experiment(container_exp_json_file, rm=False)
    print("delete sysbench container exp = ", response.status_code)

    response = delete_experiment(namespace_exp_json_file, rm=False)
    print("delete namespace exp = ", response.status_code)

    # Install default metric profile
    if cluster_type == "minikube" or cluster_type == "kind" :
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring_norecordingrules.json'

    elif cluster_type == "openshift":
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
    error_msg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert error_msg == ""

    # Create container experiments using the specified json
    response = create_experiment(container_exp_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # create namespace experiment
    response = create_experiment(namespace_exp_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Wait for the threshold for short term recommendations
    time.sleep(300)

    # generate recommendations
    json_file = open(container_exp_json_file, "r")
    input_json = json.loads(json_file.read())
    container_exp_name = input_json[0]['experiment_name']

    json_file = open(namespace_exp_json_file, "r")
    input_json = json.loads(json_file.read())
    namespace_exp_name = input_json[0]['experiment_name']

    response = generate_recommendations_v1(container_exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Invoke list recommendations for the specified experiment
    response = list_recommendations_v1(container_exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    # Validate the json against the json schema
    error_msg = validate_list_reco_json(list_reco_json, v1_list_reco_json_local_monitoring_schema)
    assert error_msg == ""

    # Validate the json values
    validate_local_monitoring_recommendation_data_present(list_reco_json)
    sysbench_exp_json = read_json_data_from_file(container_exp_json_file)
    validate_local_monitoring_reco_json(sysbench_exp_json[0], list_reco_json[0], v1=True)

    response = generate_recommendations_v1(container_exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    response = generate_recommendations_v1(namespace_exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Invoke list recommendations for the specified experiment
    response = list_recommendations_v1(namespace_exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    # Validate the json against the json schema
    error_msg = validate_list_reco_json(list_reco_json, v1_list_reco_json_local_monitoring_schema)
    assert error_msg == ""

    # Validate the json values
    validate_local_monitoring_recommendation_data_present(list_reco_json)
    namespace_exp_json = read_json_data_from_file(namespace_exp_json_file)
    validate_local_monitoring_reco_json(namespace_exp_json[0], list_reco_json[0], v1=True)
    # Delete sysbench container experiment
    response = delete_experiment(container_exp_json_file, rm=False)
    print("delete exp = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Delete namespace experiment
    response = delete_experiment(namespace_exp_json_file, rm=False)
    print("delete exp = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Delete Metric Profile
    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Remove benchmarks directory
    shutil.rmtree("benchmarks")


@pytest.mark.local
def test_get_recommendations_v1_invalid_experiment_local(cluster_type):
    """
    Test GET /kruize/api/v1/recommendations API with non-existing experiment (LOCAL MODE)
    Expected: 400 Bad Request with proper error message
    """
    form_kruize_url(cluster_type)

    # Try to get recommendations for non-existing experiment
    response = list_recommendations_v1("non_existing_experiment_local_12345", rm=False)
    
    # Validate error response
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)


@pytest.mark.local
def test_post_recommendations_v1_without_experiment_name_local(cluster_type):
    """
    Test POST /kruize/api/v1/recommendations API without experiment_name
    Expected: 400 Bad Request with proper error message
    """
    form_kruize_url(cluster_type)

    # Try to generate recommendations without experiment name
    url = get_kruize_url()
    api_url = f"{url}{RECOMMENDATIONS_API_V1}"
    response = requests.post(api_url, json={})
    
    # Validate error response with status message
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)
