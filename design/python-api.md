# Python REST API

The Python REST API design is as follows:

##  experiment_trials

-   Start or continue an experiment.
    
    `POST /experiment_trials`
    
    Example Request:
    
    ```
    POST /experiment_trials
    Host: <URL>:<PORT>
    Content-Type: application/json
    
    {
        "id" : "sha256sum of autotune object",
        "url" : "http://localhost:8080/searchSpace",
        "operation" : "EXP_TRIAL_GENERATE_NEW | EXP_TRIAL_GENERATE_SUBSEQUENT"
    }
    ```
    
    Response:
    
    | Status code | Response body |
    | --- | --- |
    | 200 | trial_number |
    | 400 | 1 |
    | 403 | 1 |

-   Send result of a trial to Python.
    
    `POST /experiment_trials`
    
    Example Request:
    
    ```
    POST /experiment_trials
    Host: <URL>:<PORT>
    Content-Type: application/json
    
    {
        "id" : "sha256sum of autotune object",
        "trial_number": "xyz",
        "trial_result": "success | failure | error",
        "result_value_type": "double",
        "result_value": "abc",
        "operation" : "EXP_TRIAL_RESULT"
    }
    ```
    
    Response:
    
    | Status code | Response body |
    | --- | --- |
    | 200 | 1 |
    | 400 | 1 |
    | 403 | 1 |

-   Get trial JSON object.
    
    `GET /experiment_trials?id={sha256sum-of-autotune-object}&trial_number={trial-number}`
    
    Example Request:
    
    `curl -H 'Accept: application/json' http://<URL>:<PORT>/experiment_trials?id='sha256sum'&trial_number=0`
    
    Example Response:
    
    ```
    [
        {
            "tunable_name": "cpu_request",
            “tunable_value”: 3.47
        },
        {
            "tunable_name": "memory_request",
            “tunable_value”: 728
        }
    ]
    ```
    