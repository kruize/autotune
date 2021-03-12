import json


def get_all_tunables(search_space_json):
    """
    Query Autotune API for the application_name, direction, hpo_algo_impl, id, objective_function, tunables and
    value_type, and return them.

    Parameters:
        search_space_json (json): A JSON containing the input search space to hyperparameter optimization module.

    Returns:
        application_name (str): The name of the application that is being optimized.
        direction (str): Direction of optimization, minimize or maximize.
        hpo_algo_impl (str): Hyperparameter optimization library to perform Bayesian Optimization.
        id (str): The id of the application that is being optimized.
        objective_function (str): The objective function that is being optimized.
        tunables (list): A list containing the details of each tunable in a dictionary format.
        value_type (string): Value type of the objective function.
    """
    # JSON returned by the Autotune API
    # search_space_json = {"id": "auto123", "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
    # "objective_function": "transaction_response_time", "value_type": "double", "direction": "minimize",
    # "hpo_algo_impl": "optuna_tpe", "tunables": [{"name": "cpu_request", "value_type": "double", "upper_bound": 6,
    # "lower_bound": 1, "step": 0.01}, {"name": "memory_request", "value_type": "integer", "upper_bound": 1024,
    # "lower_bound": 100, "step": 1}]}
    id = search_space_json["id"]
    application_name = search_space_json["application_name"]
    objective_function = search_space_json["objective_function"]
    value_type = search_space_json["value_type"]
    direction = search_space_json["direction"]
    hpo_algo_impl = search_space_json["hpo_algo_impl"]
    tunables = search_space_json["tunables"]
    return application_name, direction, hpo_algo_impl, id, objective_function, tunables, value_type
