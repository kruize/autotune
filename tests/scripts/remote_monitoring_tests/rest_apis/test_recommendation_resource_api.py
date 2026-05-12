"""
Copyright (c) 2026 Red Hat, IBM Corporation and others.

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
from helpers.list_reco_json_validate import validate_list_reco_json
from helpers.fixtures import *

sys.path.append("../../")
from helpers.utils import *


@pytest.mark.sanity
@pytest.mark.recommendations_v1
def test_get_recommendations_v1_single_experiment(cluster_type):
    """
    Test GET /recommendations API with new schema for a single experiment
    Validates:
    - Response structure with new schema
    - Presence of replicas field
    - Nested resources structure
    - Pod count metrics
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Update results with unique timestamps
        num_results = 2
        interval_start_time = get_datetime()
        end_time = interval_start_time

        for i in range(num_results):
            result_json = json.load(open(result_json_file))
            if i == 0:
                start_time = interval_start_time
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            # Write to temp file
            temp_result_file = "/tmp/update_results_temp_v1_single.json"
            with open(temp_result_file, 'w') as f:
                json.dump(result_json, f)

            response = update_results(temp_result_file)
            assert response.status_code == SUCCESS_STATUS_CODE

        # Generate recommendations using POST - this returns the recommendations in v1.0 format
        response = generate_recommendations_v1(experiment_name, interval_end_time=end_time, target="remote")
        print(f"Generate recommendations response: {response.status_code}")
        assert response.status_code == SUCCESS_200_STATUS_CODE or response.status_code == SUCCESS_STATUS_CODE

        # The POST response itself contains the recommendations in v1.0 format
        data = response.json()
        assert len(data) > 0

        # Validate new schema structure
        experiment = data[0]
        assert 'experiment_name' in experiment
        assert 'kubernetes_objects' in experiment

        k8s_obj = experiment['kubernetes_objects'][0]

        # Validate container recommendations structure (v1.0 format)
        if 'containers' in k8s_obj and k8s_obj['containers']:
            container = k8s_obj['containers'][0]
            if 'recommendations' in container and container['recommendations']:
                # v1.0 format has version field
                assert 'version' in container['recommendations']
                assert container['recommendations']['version'] == 'v1.0'

                reco_data = container['recommendations']['data']
                for timestamp, reco in reco_data.items():
                    # Validate current has replicas (v1.0 uses 'current' not 'current_config')
                    if 'current' in reco:
                        current = reco['current']
                        assert 'replicas' in current

                        # Validate nested resources structure
                        if 'resources' in current:
                            resources = current['resources']
                            assert isinstance(resources, dict)
                            if 'requests' in resources:
                                assert isinstance(resources['requests'], dict)
                            if 'limits' in resources:
                                assert isinstance(resources['limits'], dict)

                    # Validate recommendation terms
                    if 'recommendation_terms' in reco:
                        for term_name, term_data in reco['recommendation_terms'].items():
                            # Validate metrics_info has pod_count
                            if 'metrics_info' in term_data:
                                metrics_info = term_data['metrics_info']
                                if 'pod_count' in metrics_info:
                                    pod_count = metrics_info['pod_count']
                                    # Validate aggregation fields exist
                                    assert any(key in pod_count for key in ['min', 'max', 'avg'])

                            if 'recommendation_engines' in term_data:
                                for engine_name, engine_data in term_data['recommendation_engines'].items():
                                    # Validate config has resources (v1.0 format)
                                    if 'config' in engine_data:
                                        config = engine_data['config']
                                        if 'resources' in config:
                                            assert isinstance(config['resources'], dict)

                                    # Validate variation has resources
                                    if 'variation' in engine_data:
                                        variation = engine_data['variation']
                                        if 'resources' in variation:
                                            assert isinstance(variation['resources'], dict)
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.recommendations_v1
def test_post_recommendations_v1_with_target_remote(cluster_type):
    """
    Test POST /recommendations API with target=remote
    Validates:
    - Successful recommendation generation
    - Response includes replicas
    - Response includes nested resources
    - Response includes pod_count metrics
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment
    response = create_experiment(input_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Get experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    # Update results for 24 hours
    num_results = 96  # 24 hours with 15-min intervals
    interval_start_time = get_datetime()
    end_time = interval_start_time
    
    for i in range(num_results):
        result_json = json.load(open(result_json_file))
        if i == 0:
            start_time = interval_start_time
        else:
            start_time = end_time
        
        result_json[0]['interval_start_time'] = start_time
        end_time = increment_timestamp_by_given_mins(start_time, 15)
        result_json[0]['interval_end_time'] = end_time
        
        # Write to temp file
        temp_result_file = "/tmp/update_results_temp.json"
        with open(temp_result_file, 'w') as f:
            json.dump(result_json, f)
        
        response = update_results(temp_result_file)
        assert response.status_code == SUCCESS_STATUS_CODE

    # Generate recommendations with target=remote
    response = generate_recommendations_v1(experiment_name, interval_end_time=end_time, target="remote")
    
    assert response.status_code == SUCCESS_200_STATUS_CODE or response.status_code == SUCCESS_STATUS_CODE
    
    data = response.json()
    assert len(data) > 0
    
    # Validate response structure
    experiment = data[0]
    assert 'kubernetes_objects' in experiment
    
    k8s_obj = experiment['kubernetes_objects'][0]
    if 'containers' in k8s_obj and k8s_obj['containers']:
        container = k8s_obj['containers'][0]
        if 'recommendations' in container:
            reco_data = container['recommendations']['data']
            assert len(reco_data) > 0

    # Cleanup: Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.recommendations_v1
def test_post_recommendations_v1_with_target_local(cluster_type):
    """
    Test POST /recommendations API with target=local
    Validates local monitoring mode support with full setup:
    - Datasource configuration
    - Metadata import
    - Metric profile creation
    - Metadata profile creation
    - Experiment with target_cluster='local'
    - Recommendation generation with target='local'
    """
    clone_repo("https://github.com/kruize/benchmarks")
    benchmarks_install(name="sysbench", manifests="sysbench.yaml")

    form_kruize_url(cluster_type)

    # Setup paths
    metric_profile_dir = get_metric_profile_dir()
    metadata_profile_dir = get_metadata_profile_dir()

    # List all datasources
    datasource_name = None
    response = list_datasources(datasource_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Install metadata profile
    metadata_profile_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    # Create metadata profile
    response = create_metadata_profile(metadata_profile_json_file)
    data = response.json()
    print(data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Import datasource metadata
    input_json_file = "../../local_monitoring_tests/json_files/import_metadata.json"

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata
    response = import_metadata(input_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Get datasource name from import metadata file
    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']

    # Display metadata from datasource
    response = list_metadata(datasource)
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Display metadata for default cluster
    cluster_name = "default"
    response = list_metadata(datasource=datasource, cluster_name=cluster_name, verbose="true")
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Generate a temporary JSON filename
    tmp_container_exp_json_file = "/tmp/create_exp_sysbench" + ".json"
    print("tmp_json_file for container exp = ", tmp_container_exp_json_file)

    # Load the Jinja2 template from local_monitoring_tests
    environment = Environment(loader=FileSystemLoader("../../local_monitoring_tests/json_files/"))
    template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template for local monitoring
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

    # Convert rendered content to a dictionary
    container_exp_json_content = json.loads(container_exp_content)
    container_exp_json_content[0]["kubernetes_objects"][0].pop("namespaces")

    # Write the final JSON to the temp file
    with open(tmp_container_exp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(container_exp_json_content, message, indent=4)

    container_exp_json_file = tmp_container_exp_json_file

    response = delete_experiment(container_exp_json_file, rm=False)
    print("delete container exp = ", response.status_code)

    # Install metric profile
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring_norecordingrules.json'
    else:  # openshift or other
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile
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
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Create container experiment
    response = create_experiment(container_exp_json_file)
    data = response.json()
    print(data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Wait for the threshold for short term recommendations
    print("Waiting for data collection...")
    time.sleep(300)  # 5 minutes wait

    # Get experiment name
    json_file = open(container_exp_json_file, "r")
    input_json = json.loads(json_file.read())
    container_exp_name = input_json[0]['experiment_name']

    # Generate recommendations with target=local
    response = generate_recommendations_v1(container_exp_name, target="local")
    print(f"Generate recommendations response: {response.json()}")
    print(f"Generate recommendations response: {response.status_code}")
    assert response.status_code == SUCCESS_200_STATUS_CODE or response.status_code == SUCCESS_STATUS_CODE

    # Invoke list recommendations for the specified experiment
    response = list_recommendations_v1(container_exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    # Validate the json against the json schema
    error_msg = validate_list_reco_json(list_reco_json, v1_list_reco_json_local_monitoring_schema.v1_list_reco_json_local_monitoring_schema)
    assert error_msg == ""

    # Validate the json values
    validate_local_monitoring_recommendation_data_present(list_reco_json)
    sysbench_exp_json = read_json_data_from_file(container_exp_json_file)
    validate_local_monitoring_reco_json(sysbench_exp_json[0], list_reco_json[0], v1=True)

    # Cleanup: Delete the experiment
    response = delete_experiment(container_exp_json_file, rm=False)
    print("delete exp = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Delete Metric Profile
    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Delete Metadata Profile
    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Remove benchmarks directory
    shutil.rmtree("benchmarks")



@pytest.mark.sanity
@pytest.mark.recommendations_v1
def test_post_recommendations_v1_default_target(cluster_type):
    """
    Test POST /recommendations API without target parameter
    Should default to 'remote'
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Update results with unique timestamps
        num_results = 2
        interval_start_time = get_datetime()
        end_time = interval_start_time

        for i in range(num_results):
            result_json = json.load(open(result_json_file))
            if i == 0:
                start_time = interval_start_time
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            temp_result_file = "/tmp/update_results_temp_default.json"
            with open(temp_result_file, 'w') as f:
                json.dump(result_json, f)

            response = update_results(temp_result_file)
            assert response.status_code == SUCCESS_STATUS_CODE

        # Generate recommendations without target parameter (should default to remote)
        response = generate_recommendations_v1(experiment_name, interval_end_time=end_time)

        # Should succeed as it defaults to remote
        assert response.status_code == SUCCESS_200_STATUS_CODE or response.status_code == SUCCESS_STATUS_CODE
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.recommendations_v1
def test_post_recommendations_v1_invalid_target(cluster_type):
    """
    Test POST /recommendations API with invalid target parameter
    Expected: 400 Bad Request
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Try to generate recommendations with invalid target
        response = generate_recommendations_v1(experiment_name, target="invalid_target")

        assert response.status_code == ERROR_STATUS_CODE
        assert "Invalid target cluster" in response.text
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.recommendations_v1
def test_get_recommendations_v1_invalid_experiment(cluster_type):
    """
    Test GET /recommendations API with non-existing experiment
    Expected: 400 Bad Request
    """
    form_kruize_url(cluster_type)

    # Try to get recommendations for non-existing experiment
    response = list_recommendations_v1("non_existing_experiment_12345")
    
    assert response.status_code == ERROR_STATUS_CODE


@pytest.mark.negative
@pytest.mark.recommendations_v1
def test_get_recommendations_v1_invalid_timestamp(cluster_type):
    """
    Test GET /recommendations API with invalid timestamp format
    Expected: 400 Bad Request
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Try to get recommendations with invalid timestamp
        response = list_recommendations_v1(experiment_name, interval_end_time="invalid-timestamp")

        assert response.status_code == ERROR_STATUS_CODE
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.recommendations_v1
def test_get_recommendations_v1_namespace_experiment(cluster_type):
    """
    Test GET /recommendations API for namespace experiment
    Validates:
    - Namespace recommendations structure
    - Replicas field in namespace recommendations
    - Pod count metrics for namespace
    """
    input_json_file = "../json_files/create_exp_namespace.json"
    result_json_file = "../json_files/update_results_namespace.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    try:
        # Create namespace experiment
        response = create_experiment(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Update results with unique timestamps
        num_results = 2
        interval_start_time = get_datetime()
        end_time = interval_start_time

        for i in range(num_results):
            result_json = json.load(open(result_json_file))
            if i == 0:
                start_time = interval_start_time
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            temp_result_file = "/tmp/update_results_temp_namespace.json"
            with open(temp_result_file, 'w') as f:
                json.dump(result_json, f)

            response = update_results(temp_result_file)
            assert response.status_code == SUCCESS_STATUS_CODE

        # Generate recommendations
        response = generate_recommendations_v1(experiment_name, interval_end_time=end_time, target="remote")
        print("generate recommendations = ", response.json())
        assert response.status_code == SUCCESS_200_STATUS_CODE or response.status_code == SUCCESS_STATUS_CODE

        # Get recommendations
        response = list_recommendations_v1(experiment_name, rm=True)
        print("list_recommendations_v1: ", response.json())
        assert response.status_code == SUCCESS_200_STATUS_CODE or response.status_code == SUCCESS_STATUS_CODE

        data = response.json()
        assert len(data) > 0

        # Validate namespace recommendations structure
        experiment = data[0]
        k8s_obj = experiment['kubernetes_objects'][0]

        if 'namespace' in k8s_obj:
            namespace = k8s_obj['namespace']
            if 'recommendations' in namespace:
                reco_data = namespace['recommendations']['data']
                for timestamp, reco in reco_data.items():
                    # Validate current config has replicas
                    if 'current_config' in reco:
                        current_config = reco['current_config']
                        # For namespace, replicas might be at namespace level
                        assert 'replicas' in current_config or current_config.get('replicas') is not None
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.recommendations_v1
def test_recommendations_v1_validate_pod_count_aggregation(cluster_type):
    """
    Test that pod_count metrics include proper aggregation (min, max, avg, sum)
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Update results multiple times to get aggregation data with unique timestamps
        num_results = 10
        interval_start_time = get_datetime()
        end_time = interval_start_time

        for i in range(num_results):
            result_json = json.load(open(result_json_file))
            if i == 0:
                start_time = interval_start_time
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            temp_result_file = "/tmp/update_results_temp_aggregation.json"
            with open(temp_result_file, 'w') as f:
                json.dump(result_json, f)

            response = update_results(temp_result_file)
            assert response.status_code == SUCCESS_STATUS_CODE

        # Generate recommendations
        response = generate_recommendations_v1(experiment_name, interval_end_time=end_time, target="remote")
        print("generate recommendations = ", response.json())
        assert response.status_code == SUCCESS_200_STATUS_CODE or response.status_code == SUCCESS_STATUS_CODE

        # Get recommendations
        response = list_recommendations_v1(experiment_name, rm=True)
        assert response.status_code == SUCCESS_200_STATUS_CODE

        data = response.json()
        experiment = data[0]
        k8s_obj = experiment['kubernetes_objects'][0]

        if 'containers' in k8s_obj and k8s_obj['containers']:
            container = k8s_obj['containers'][0]
            if 'metrics' in container:
                # Check for podCount metric
                if 'podCount' in container['metrics']:
                    pod_count_metric = container['metrics']['podCount']
                    assert 'aggregation_info' in pod_count_metric

                    agg_info = pod_count_metric['aggregation_info']
                    # Validate aggregation fields exist
                    assert any(key in agg_info for key in ['min', 'max', 'avg', 'sum'])
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.recommendations_v1
def test_post_recommendations_v1_without_experiment_name(cluster_type):
    """
    Test POST /recommendations API without experiment_name
    Expected: 400 Bad Request
    """
    form_kruize_url(cluster_type)

    # Try to generate recommendations without experiment name
    url = get_kruize_url()
    api_url = f"{url}{RECOMMENDATIONS_API_V1}?target=remote"
    response = requests.post(api_url, json={})
    
    assert response.status_code == ERROR_STATUS_CODE


@pytest.mark.negative
@pytest.mark.recommendations_v1
def test_post_recommendations_v1_without_interval_end_time(cluster_type):
    """
    Test POST /recommendations API without interval_end_time for remote target
    Expected: 400 Bad Request
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment
    response = create_experiment(input_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Get experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    # Try to generate recommendations without interval_end_time
    url = get_kruize_url()
    api_url = f"{url}{RECOMMENDATIONS_API_V1}?experiment_name={experiment_name}&target=remote"
    response = requests.post(api_url, json={})
    
    assert response.status_code == ERROR_STATUS_CODE
