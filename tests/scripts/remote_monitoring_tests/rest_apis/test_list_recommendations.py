import requests
import pytest
from jinja2 import Environment, FileSystemLoader
from helpers.list_reco_json_validate import *
from helpers.utils import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.fixtures import *
import time
import json

@pytest.mark.sanity
def test_list_recommendations_single_exp(cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
   
    time.sleep(10)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)
    update_results_json = read_json_data_from_file(result_json_file) 

    validate_reco_json(create_exp_json[0], update_results_json[0], list_reco_json[0])

@pytest.mark.sanity
def test_list_recommendations_without_parameters(cluster_type):
    """
    Test Description: This test validates listRecommendations API without parameters
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    time.sleep(10)

    # Get the experiment name
    experiment_name = "" 
    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)
    update_results_json = read_json_data_from_file(result_json_file) 

    validate_reco_json(create_exp_json[0], update_results_json[0], list_reco_json[0])

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
def test_list_recommendations_invalid_exp(cluster_type):
    """
    Test Description: This test validates listRecommendations by passing an invalid experiment name
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
    
    # Get the experiment name
    experiment_name = "xyz"

    response = list_recommendations(experiment_name)

    data = response.json()
    print(data)
    assert response.status_code == ERROR_STATUS_CODE

    # validate error message

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_without_results(cluster_type):
    """
    Test Description: This test validates listRecommendations when there was no updation of results
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    time.sleep(10)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = list_recommendations(experiment_name)

    data = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_single_exp_multiple_results(cluster_type):
    """
    Test Description: This test validates listRecommendations by updating multiple results for a single experiment
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/multiple_results_single_exp.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == "Bulk entries are currently unsupported!"
   
    time.sleep(10)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)
    update_results_json = read_json_data_from_file(result_json_file) 

    validate_reco_json(create_exp_json[0], update_results_json[0], list_reco_json[0])

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_multiple_exps_from_diff_json_files(cluster_type):
    """
    Test Description: This test validates list recommendations for multiple experiments posted using different json files
    """

    input_json_file="../json_files/create_exp.json"
    result_json_file="../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 3 
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        generate_json(find, input_json_file, json_file, i)

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

    for i in range(num_exps):
        # Update results for the experiment
        json_file = "/tmp/update_results_" + str(i) + ".json"
        update_timestamps = True
        generate_json(find, result_json_file, json_file, i, update_timestamps)
        response = update_results(json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    time.sleep(30)

    # Get the experiment name
    json_data = json.load(open(input_json_file))

    response = list_recommendations()

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json)
    assert errorMsg == ""

    # Validate the json values
    for i in range(num_exps):
        input_json_file = "/tmp/create_exp_" + str(i) + ".json"
        create_exp_json = read_json_data_from_file(input_json_file)

        result_json_file = "/tmp/update_results_" + str(i) + ".json"
        update_results_json = read_json_data_from_file(result_json_file) 

        experiment_name = create_exp_json[0]['experiment_name']
        response = list_recommendations(experiment_name)
        list_reco_json = response.json()

        validate_reco_json(create_exp_json[0], update_results_json[0], list_reco_json[0])

    # Delete the experiments    
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)

@pytest.mark.extended
def test_list_recommendations_multiple_exps_from_diff_json_files_2(cluster_type):
    """
    Test Description: This test validates list recommendations for multiple experiments posted using different json files
    """
    num_exps = 10
    metrics_csv = "../csv_data/tfb_data.csv"
    exp_jsons_dir = "/tmp/exp_jsons"
    result_jsons_dir = "/tmp/result_jsons"

    # Create the create experiment jsons
    create_exp_jsons()

    # Create the update result jsons
    create_update_results_jsons(metrics_csv)
   
    # Form the Kruize service URL
    form_kruize_url(cluster_type)

    for i in range(num_exps):

        # Create experiment using the specified json
        create_exp_json_file = exp_jsons_dir + "/create_exp_" + str(i) + ".json"

        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(create_exp_json_file)

        data = response.json()
        json_data = json.load(open(create_exp_json_file))
        obj_type = json_data[0]['kubernetes_objects'][0]["type"]

        print(f"kubernetes_object_type = {obj_type}")

        print("message = ", data['message'])

        if obj_type == "xyz":
            assert response.status_code == ERROR_STATUS_CODE
            assert data['status'] == ERROR_STATUS
        else:
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == CREATE_EXP_SUCCESS_MSG

        # Update results for the experiment
        result_json_file = result_jsons_dir + "/result_" + str(i) + ".json"
        
        response = update_results(result_json_file)
        data = response.json()


        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        time.sleep(20)

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE

        list_reco_json = response.json()

        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json)
        assert errorMsg == ""

        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = read_json_data_from_file(result_json_file)
        
        experiment_name = create_exp_json[0]['experiment_name']
        response = list_recommendations(experiment_name)
        list_reco_json = response.json()
        
        validate_reco_json(create_exp_json[0], update_results_json[0], list_reco_json[0])

    # Delete the experiments    
    for i in range(num_exps):
        create_exp_json_file = exp_jsons_dir + "/create_exp_" + str(i) + ".json"

        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_latest_result_single_exp(cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name and latest as True
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)
    
    # Create experiment using the specified json
    response = create_experiment(input_json_file)
    
    data = response.json()
    print(data['message'])
    
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the same experiment
    result_json_file="../json_files/multiple_results_single_exp.json"

    result_json_arr = read_json_data_from_file(result_json_file)
    for result_json in result_json_arr:
        single_json_arr = []
        json_file = "/tmp/update_results.json"
        single_json_arr.append(result_json)
        write_json_data_to_file(json_file, single_json_arr)

        response = update_results(json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
   
        time.sleep(5)

    time.sleep(20)
    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    #response = list_recommendations(experiment_name, latest = True)
    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)
    update_results_json = read_json_data_from_file(result_json_file) 

    validate_reco_json(create_exp_json[0], update_results_json[0], list_reco_json[0])

