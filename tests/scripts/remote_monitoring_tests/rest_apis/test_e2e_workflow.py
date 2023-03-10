import requests
import pytest
from jinja2 import Environment, FileSystemLoader
from helpers.utils import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.fixtures import *
import time
import json

@pytest.mark.test_e2e
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
    num_exps = 10
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        generate_json(find, input_json_file, json_file, i)

        # Delete the experiment
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)

        # Create the experiment
        response = create_experiment(json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

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

        # sleep for a while before fetching recommendations
        time.sleep(20)

        # Get the experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)

@pytest.mark.extended
def test_list_recommendations_multiple_exps_from_diff_json_files_2(cluster_type):
    num_exps = 10
    metrics_csv = "../csv_data/tfb_data.csv"

    # Create the create experiment jsons
    create_exp_jsons()

    # Create the update result jsons
    create_update_results_jsons(metrics_csv)
   
    # Form the Kruize service URL
    form_kruize_url(cluster_type)

    for i in range(num_exps):

        # Create experiment using the specified json
        create_exp_json_file = "/tmp/exp_jsons/create_exp_" + str(i) + ".json"

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
        result_json_file = "/tmp/result_jsons/result_" + str(i) + ".json"
        
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

    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)
