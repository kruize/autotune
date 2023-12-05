import pytest
from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from jinja2 import Environment, FileSystemLoader

csvfile = "/tmp/update_results_test_data.csv"

# Interval end time that is acceptable is measurement_duration + or - 30s
# interval_start_time - "2022-01-23T18:25:43.511Z"
interval_end_times = [
    ("invalid_zero_diff", "2022-01-23T18:25:43.511Z"),
    ("invalid_minus_more_than_30s", "2022-01-23T18:40:12.511Z"),
    ("valid_minus_30s", "2022-01-23T18:40:13.511Z"),
    ("invalid_plus_more_than_30s", "2022-01-23T18:41:14.511Z"),
    ("valid_plus_30s", "2022-01-23T18:41:13.511Z")
]

missing_metrics = [
    ("Missing_metrics_single_res_single_container", "../json_files/missing_metrics_jsons/update_results_missing_metrics_single_container.json", "Out of a total of 1 records, 1 failed to save", "Performance profile: [Metric data is not present for container : tfb-server-0 for experiment: quarkus-resteasy-kruize-min-http-response-time-db"),
    ("Missing_metrics_single_res_all_containers", "../json_files/missing_metrics_jsons/update_results_missing_metrics_all_containers.json", "Out of a total of 1 records, 1 failed to save", "Performance profile: [Metric data is not present for container : tfb-server-0 for experiment: quarkus-resteasy-kruize-min-http-response-time-db. , Metric data is not present for container : tfb-server-1 for experiment: quarkus-resteasy-kruize-min-http-response-time-db"),
    ("Missing_metrics_bulk_res_single_container", "../json_files/missing_metrics_jsons/bulk_update_results_missing_metrics_single_container.json", "Out of a total of 100 records, 1 failed to save", "Performance profile: [Metric data is not present for container : tfb-server-1 for experiment: quarkus-resteasy-kruize-min-http-response-time-db"),
    ("Missing_metrics_bulk_res_few_containers", "../json_files/missing_metrics_jsons/bulk_update_results_missing_metrics_few_containers.json", "Out of a total of 100 records, 2 failed to save", "Performance profile: [Metric data is not present for container : tfb-server-1 for experiment: quarkus-resteasy-kruize-min-http-response-time-db"),
    ("Missing_metrics_bulk_res_few_containers_few_individual_metrics_missing", "../json_files/missing_metrics_jsons/bulk_update_results_missing_metrics_few_containers_few_individual_metrics_missing.json", "Out of a total of 100 records, 4 failed to save", "Metric data is not present for container")
]


@pytest.mark.negative
@pytest.mark.parametrize(
    "test_name, expected_status_code, version, experiment_name, interval_start_time, interval_end_time, kubernetes_obj_type, name, namespace, container_image_name, container_name, cpuRequest_name, cpuRequest_sum, cpuRequest_avg, cpuRequest_format, cpuLimit_name, cpuLimit_sum, cpuLimit_avg, cpuLimit_format, cpuUsage_name, cpuUsage_sum, cpuUsage_max, cpuUsage_avg, cpuUsage_min, cpuUsage_format, cpuThrottle_name, cpuThrottle_sum, cpuThrottle_max, cpuThrottle_avg, cpuThrottle_format, memoryRequest_name, memoryRequest_sum, memoryRequest_avg, memoryRequest_format, memoryLimit_name, memoryLimit_sum, memoryLimit_avg, memoryLimit_format, memoryUsage_name, memoryUsage_sum, memoryUsage_max, memoryUsage_avg, memoryUsage_min, memoryUsage_format, memoryRSS_name, memoryRSS_sum, memoryRSS_max, memoryRSS_avg, memoryRSS_min, memoryRSS_format",
    generate_test_data(csvfile, update_results_test_data))
def test_update_results_invalid_tests(test_name, expected_status_code, version, experiment_name, interval_start_time,
                                      interval_end_time, kubernetes_obj_type, name, namespace, container_image_name,
                                      container_name, cpuRequest_name, cpuRequest_sum, cpuRequest_avg,
                                      cpuRequest_format, cpuLimit_name, cpuLimit_sum, cpuLimit_avg, cpuLimit_format,
                                      cpuUsage_name, cpuUsage_sum, cpuUsage_max, cpuUsage_avg, cpuUsage_min,
                                      cpuUsage_format, cpuThrottle_name, cpuThrottle_sum, cpuThrottle_max,
                                      cpuThrottle_avg, cpuThrottle_format, memoryRequest_name, memoryRequest_sum,
                                      memoryRequest_avg, memoryRequest_format, memoryLimit_name, memoryLimit_sum,
                                      memoryLimit_avg, memoryLimit_format, memoryUsage_name, memoryUsage_sum,
                                      memoryUsage_max, memoryUsage_avg, memoryUsage_min, memoryUsage_format,
                                      memoryRSS_name, memoryRSS_sum, memoryRSS_max, memoryRSS_avg, memoryRSS_min,
                                      memoryRSS_format, cluster_type):
    print("\n*******************************************************")
    print("Test - ", test_name)
    print("*******************************************************\n")
    input_json_file = "../json_files/create_exp.json"

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

    # Create experiment using the specified json
    result_json_file = "../json_files/update_results_template.json"
    tmp_json_file = "/tmp/update_results_" + test_name + ".json"

    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("update_results_template.json")

    if "null" in test_name:
        field = test_name.replace("null_", "")
        json_file = "../json_files/update_results_template.json"
        filename = "/tmp/update_results_template.json"

        strip_double_quotes_for_field(json_file, field, filename)
        environment = Environment(loader=FileSystemLoader("/tmp/"))
        template = environment.get_template("update_results_template.json")

    filename = f"/tmp/update_results_{test_name}.json"
    content = template.render(
        version=version,
        experiment_name=experiment_name,
        interval_start_time=interval_start_time,
        interval_end_time=interval_end_time,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        container_image_name=container_image_name,
        container_name=container_name,
        cpuRequest_name=cpuRequest_name,
        cpuRequest_sum=cpuRequest_sum,
        cpuRequest_avg=cpuRequest_avg,
        cpuRequest_format=cpuRequest_format,
        cpuLimit_name=cpuLimit_name,
        cpuLimit_sum=cpuLimit_sum,
        cpuLimit_avg=cpuLimit_avg,
        cpuLimit_format=cpuLimit_format,
        cpuUsage_name=cpuUsage_name,
        cpuUsage_sum=cpuUsage_sum,
        cpuUsage_max=cpuUsage_max,
        cpuUsage_avg=cpuUsage_avg,
        cpuUsage_min=cpuUsage_min,
        cpuUsage_format=cpuUsage_format,
        cpuThrottle_name=cpuThrottle_name,
        cpuThrottle_sum=cpuThrottle_sum,
        cpuThrottle_max=cpuThrottle_max,
        cpuThrottle_avg=cpuThrottle_avg,
        cpuThrottle_format=cpuThrottle_format,
        memoryRequest_name=memoryRequest_name,
        memoryRequest_sum=memoryRequest_sum,
        memoryRequest_avg=memoryRequest_avg,
        memoryRequest_format=memoryRequest_format,
        memoryLimit_name=memoryLimit_name,
        memoryLimit_sum=memoryLimit_sum,
        memoryLimit_avg=memoryLimit_avg,
        memoryLimit_format=memoryLimit_format,
        memoryUsage_name=memoryUsage_name,
        memoryUsage_sum=memoryUsage_sum,
        memoryUsage_max=memoryUsage_max,
        memoryUsage_avg=memoryUsage_avg,
        memoryUsage_min=memoryUsage_min,
        memoryUsage_format=memoryUsage_format,
        memoryRSS_name=memoryRSS_name,
        memoryRSS_sum=memoryRSS_sum,
        memoryRSS_max=memoryRSS_max,
        memoryRSS_avg=memoryRSS_avg,
        memoryRSS_min=memoryRSS_min,
        memoryRSS_format=memoryRSS_format
    )
    with open(filename, mode="w", encoding="utf-8") as message:
        message.write(content)

    response = update_results(tmp_json_file)

    data = response.json()
    print(data['message'])
    assert response.status_code == int(expected_status_code)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
@pytest.mark.parametrize("test_name, result_json_file, expected_message, error_message", missing_metrics)
def test_update_results_with_missing_metrics_section(test_name, result_json_file, expected_message, error_message, cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
                      by updating results with entire metrics section missing
    """
    input_json_file = "../json_files/create_exp.json"

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
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    print("**************************")
    print(data['message'])
    print("**************************")

    # add assertion of expected message
    assert data['message'] == expected_message 

    # add assertion of expected error message
    msg_data=data['data']
    for d in msg_data:
        error_data=d["errors"]
        for err in error_data:
            actual_error_message = err["message"]
            if test_name == "Missing_metrics_bulk_res_few_containers" and d["interval_end_time"] == "2023-04-13T23:29:20.982Z":
                error_message = "Performance profile: [Metric data is not present for container : tfb-server-1 for experiment: quarkus-resteasy-kruize-min-http-response-time-db. ] , " \
                                "Performance profile: [Metric data is not present for container : tfb-server-1 for experiment: quarkus-resteasy-kruize-min-http-response-time-db. , " \
                                "Metric data is not present for container : tfb-server-0 for experiment: quarkus-resteasy-kruize-min-http-response-time-db"
            if test_name == "Missing_metrics_bulk_res_few_containers_few_individual_metrics_missing" and d["interval_end_time"] == "2023-04-13T23:44:20.982Z" or d["interval_end_time"] == "2023-04-13T23:59:20.982Z":
                error_message = "Performance profile: [Missing one of the following mandatory parameters for experiment - quarkus-resteasy-kruize-min-http-response-time-db : [cpuUsage, memoryUsage, memoryRSS]]"
            assert error_message in actual_error_message
    
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_update_valid_results_after_create_exp(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_update_multiple_valid_results_single_json_after_create_exp(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment by posting multiple results
    """
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/multiple_results_single_exp.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_update_multiple_valid_results_after_create_exp(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    # Update results for the experiment
    num_res = 5
    find_start_ts = "2022-01-23T18:25:43.511Z"
    find_end_ts = "2022-01-23T18:40:43.570Z"

    result_json_file = "../json_files/update_results.json"
    filename = "/tmp/result.json"
    for i in range(num_res):

        with open(result_json_file, 'r') as file:
            data = file.read()

        if i == 0:
            start_ts = get_datetime()
        else:
            start_ts = end_ts

        print("start_ts = ", start_ts)
        data = data.replace(find_start_ts, start_ts)

        end_ts = increment_timestamp_by_given_mins(start_ts, 15)
        print("end_ts = ", end_ts)
        data = data.replace(find_end_ts, end_ts)

        with open(filename, 'w') as file:
            file.write(data)

        response = update_results(filename)

        data = response.json()
        print("message = ", data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
@pytest.mark.parametrize("format_type", ["cores", "m"])
def test_update_multiple_valid_results_with_supported_cpu_format_types(format_type, cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    # Update results for the experiment
    num_res = 96
    find_start_ts = "2022-01-23T18:25:43.511Z"
    find_end_ts = "2022-01-23T18:40:43.570Z"

    cpu_format = "cores"

    result_json_file = "../json_files/update_results.json"
    filename = "/tmp/result.json"
    for i in range(num_res):

        with open(result_json_file, 'r') as file:
            data = file.read()

        if i == 0:
            start_ts = get_datetime()
        else:
            start_ts = end_ts

        print("start_ts = ", start_ts)
        data = data.replace(find_start_ts, start_ts)

        end_ts = increment_timestamp_by_given_mins(start_ts, 15)
        print("end_ts = ", end_ts)
        data = data.replace(find_end_ts, end_ts)

        data = data.replace(cpu_format, format_type)

        with open(filename, 'w') as file:
            file.write(data)

        response = update_results(filename)

        data = response.json()
        print("message = ", data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS

@pytest.mark.sanity
@pytest.mark.parametrize("format_type", ["bytes", "Bytes", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "Ki", "Mi", "Gi", "Ti", "Pi", "Ei", "kB", "KB", "MB", "GB", "TB", "PB", "EB", "K", "k", "M", "G", "T", "P", "E"])
def test_update_multiple_valid_results_with_supported_mem_format_types(format_type, cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    # Update results for the experiment
    num_res = 96
    find_start_ts = "2022-01-23T18:25:43.511Z"
    find_end_ts = "2022-01-23T18:40:43.570Z"

    memory_format = "MiB"

    result_json_file = "../json_files/update_results.json"
    filename = "/tmp/result.json"
    for i in range(num_res):

        with open(result_json_file, 'r') as file:
            data = file.read()

        if i == 0:
            start_ts = get_datetime()
        else:
            start_ts = end_ts

        print("start_ts = ", start_ts)
        data = data.replace(find_start_ts, start_ts)

        end_ts = increment_timestamp_by_given_mins(start_ts, 15)
        print("end_ts = ", end_ts)
        data = data.replace(find_end_ts, end_ts)

        data = data.replace(memory_format, format_type)

        with open(filename, 'w') as file:
            file.write(data)

        response = update_results(filename)

        data = response.json()
        print("message = ", data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_update_results_multiple_exps_from_same_json_file(cluster_type):
    """
    Test Description: This test validates the response status code of updateResults API by posting
    results of multiple experiments in the same json file.
    """
    input_json_file = "../json_files/create_multiple_exps.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print("message = ", data['message'])
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == CREATE_EXP_BULK_ERROR_MSG

    # Update results for the experiment
    result_json_file = "../json_files/multiple_exps_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == 'Out of a total of 3 records, 3 failed to save'

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_update_results_multiple_exps_multiple_containers_from_same_json_file(cluster_type):
    """
    Test Description: This test validates the response status code of updateResults API by posting
    results of multiple experiments with multiple containers in the same json file.
    """
    input_json_file = "../json_files/create_multiple_exps_multiple_containers.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print("message = ", data['message'])
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == CREATE_EXP_BULK_ERROR_MSG

    # Update results for the experiment
    result_json_file = "../json_files/multiple_exps_multiple_containers_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == 'Out of a total of 3 records, 3 failed to save'

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_update_results_for_containers_not_present(cluster_type):
    """
    Test Description: This test validates the response status code of updateResults API by posting
    results of multiple experiments with multiple containers in the same json file.
    """
    input_json_file = "../json_files/create_multiple_exps.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print("message = ", data['message'])
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == CREATE_EXP_BULK_ERROR_MSG

    # Update results for the experiment
    result_json_file = "../json_files/multiple_exps_multiple_containers_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == 'Out of a total of 3 records, 3 failed to save'


@pytest.mark.sanity
def test_update_results_multiple_exps_from_diff_json_files(cluster_type):
    """
    Test Description: This test validates the updation of results for multiple experiments using different json files
    """

    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]["kubernetes_objects"][0]['name'])
    find.append(json_data[0]["kubernetes_objects"][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 10
    for i in range(num_exps):
        json_file = "/tmp/create_exp.json"
        generate_json(find, input_json_file, json_file, i)

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        # Update results for the experiment
        json_file = "/tmp/update_results.json"
        generate_json(find, result_json_file, json_file, i)
        response = update_results(json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)


# @pytest.mark.negative
def test_update_valid_results_without_create_exp(cluster_type):
    """
    Test Description: This test validates the behavior of updateResults API by posting results for a non-existing experiment
    """
    input_json_file = "../json_files/create_exp.json"
    json_data = json.load(open(input_json_file))

    experiment_name = json_data[0]['experiment_name']
    print("experiment_name = ", experiment_name)

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    result_json_file = "../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])

    EXP_NAME_NOT_FOUND_MSG = "Experiment name : " + experiment_name + " not found"
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == EXP_NAME_NOT_FOUND_MSG


@pytest.mark.sanity
def test_update_results_with_same_result(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    # Post the same result again
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    exp_json_data = read_json_data_from_file(input_json_file)
    experiment_name = exp_json_data[0]['experiment_name']

    result_json_data = read_json_data_from_file(result_json_file)
    interval_end_time = result_json_data[0]['interval_end_time']
    interval_start_time = result_json_data[0]['interval_start_time']

    TIMESTAMP_PRESENT_MSG = 'An entry for this record already exists!'

    print(TIMESTAMP_PRESENT_MSG)
    print(data['data'][0]['errors'][0]['message'])
    assert data['message'] == 'Out of a total of 1 records, 1 failed to save'
    assert data['data'][0]['errors'][0]['message'] == TIMESTAMP_PRESENT_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("test_name, interval_end_time", interval_end_times)
def test_update_results_with_valid_and_invalid_interval_duration(test_name, interval_end_time, cluster_type):
    """
    Test Description: This test validates update results by posting results with interval time difference that is not valid for
    the given measurement duration
    """
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/update_results.json"

    json_data = read_json_data_from_file(result_json_file)
    json_data[0]['interval_end_time'] = interval_end_time
    json_file = "/tmp/update_results.json"

    write_json_data_to_file(json_file, json_data)

    response = update_results(json_file)

    data = response.json()
    if test_name == "valid_plus_30s" or test_name == "valid_minus_30s":
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
    elif test_name == "invalid_zero_diff":
        assert response.status_code == ERROR_STATUS_CODE
        assert data['status'] == ERROR_STATUS
        assert data['message'] == 'Out of a total of 1 records, 1 failed to save'
        assert data['data'][0]['errors'][0]['message'] == UPDATE_RESULTS_DATE_PRECEDE_ERROR_MSG
    else:
        assert response.status_code == ERROR_STATUS_CODE
        assert data['status'] == ERROR_STATUS
        assert data['message'] == 'Out of a total of 1 records, 1 failed to save'
        assert data['data'][0]['errors'][0]['message'] == INVALID_INTERVAL_DURATION_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)
