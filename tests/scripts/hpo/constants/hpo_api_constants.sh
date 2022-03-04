#!/bin/bash
#
# Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
##### Constants for HPO API tests #####
#

# Breif description about the HPO API tests
declare -A hpo_api_test_description
hpo_api_test_description=([hpo_post_experiment]="Start the required HPO services, post an experiment json to HPO /experiment_trials API with various combinations and validate the result"
                          [hpo_get_trial_json]="Start the required HPO services post a valid experiment json to HPO /experiment_trials API, query the API using different combinations of experiment id and trial_number and validate the result" 
                          [hpo_post_exp_result]="Start the required HPO services,  post a valid experiment json to HPO /experiment_trials API and then post valid and invalid combinations of experiment result to the API and validate the result")

# Tests to be carried out for HPO (Hyper Parameter Optimization) module API to post an experiment
run_post_experiment_tests=("invalid-id"
"empty-id"
"no-id"
"null-id"
"multiple-id"
"invalid-url"
"empty-url"
"no-url"
"null-url"
"multiple-url"
"invalid-operation"
"empty-operation"
"no-operation"
"null-operation"
"multiple-operation"
"valid-experiment"
"additional-field"
"generate-subsequent"
"invalid-searchspace")

other_post_experiment_tests=("post-duplicate-experiments" "operation-generate-subsequent")

# Tests to be carried out for HPO module API to get trial json  
declare -A rm_hpo_get_trial_json_tests
rm_hpo_get_trial_json_tests=([get_trial_json_invalid_tests]='invalid-id empty-id no-id null-id only-valid-id invalid-trial-number empty-trial-number no-trial-number null-trial-number only-valid-trial-number'
                             [get_trial_json_valid_tests]='valid-exp-trial valid-exp-trial-generate-subsequent')

# Tests to be carried out for HPO module API to post experiment results 
run_post_exp_result_tests=("invalid-id"
"empty-id"
"no-id"
"null-id"
"multiple-id"
"invalid-trial-number"
"no-trial-number"
"null-trial-number"
"multiple-trial-number"
"invalid-trial-result"
"empty-trial-result"
"no-trial-result"
"null-trial-result"
"multiple-trial-result"
"invalid-result-value-type"
"empty-result-value-type"
"no-result-value-type"
"null-result-value-type"
"multiple-result-value-type"
"invalid-result-value"
"no-result-value"
"null-result-value"
"multiple-result-value"
"invalid-operation"
"empty-operation"
"no-operation"
"null-operation"
"multiple-operation"
"valid-experiment-result"
"additional-field")

other_exp_result_post_tests=("post-duplicate-exp-result" "post-same-id-different-exp-result")

declare -A hpo_post_experiment_json
# Json array for different test cases
# input: Current autotune object id
# output: Generate the json array with given id
function create_post_exp_json_array() {
	current_id=$1
	hpo_post_experiment_json=([invalid-id]='{"experiment_id" : "0123456789012345678901234567890123456789012345678901234567890123456789", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[empty-id]='{"experiment_id" : " ", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[no-id]='{"url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[null-id]='{"experiment_id" : null, "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[multiple-id]='{"experiment_id" : "'${current_id}'", "id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[invalid-url]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpaces", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[empty-url]='{"experiment_id" : "'${current_id}'", "url" : "", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[no-url]='{"experiment_id" : "'${current_id}'", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[null-url]='{"experiment_id" : "'${current_id}'", "url" : null, "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[multiple-url]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[invalid-operation]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_CURRENT"}'
	[empty-operation]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : ""}'
	[no-operation]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace"}'
	[null-operation]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : null}'
	[multiple-operation]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW", "operation" : "EXP_TRIAL_GENERATE_SUBSEQUENT"}'
	[additional-field]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW", "tunable_name" : "cpuRequest"}'
	[valid-experiment]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[generate-subsequent]='{"experiment_id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_SUBSEQUENT" }')
}

declare -A hpo_error_messages
hpo_error_messages=([invalid-id]="KeyError: '0123456789012345678901234567890123456789012345678901234567890123456789'"
[empty-id]="KeyError: ' '"
[no-id]="KeyError: 'id'"
[null-id]="KeyError: None"
[empty-url]="Invalid URL ''"
[no-url]="KeyError: 'url'"
[null-url]="Invalid URL 'None'"
[no-operation]="KeyError: 'operation'")

declare -A hpo_post_exp_result_json
# Experiment results for different test cases
# input: Current experiment id
# output: Generate the experiment result with given experiment_id
function create_post_exp_result_json_array() {
	current_id=$1
	trial_num=$2
	hpo_post_exp_result_json=([invalid-id]='{"experiment_id" : "xyz", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[empty-id]='{"experiment_id" : " ", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[no-id]='{"trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[null-id]='{"experiment_id" : null, "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[multiple-id]='{"experiment_id" : "'${current_id}'", "id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[invalid-trial-number]='{"experiment_id" : "'${current_id}'", "trial_number": 10000, "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[no-trial-number]='{"experiment_id" : "'${current_id}'", "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[null-trial-number]='{"experiment_id" : "'${current_id}'", "trial_number": null, "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[multiple-trial-number]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_number": 1, "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[invalid-trial-result]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "xyz", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[empty-trial-result]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": " ", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[no-trial-result]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[null-trial-result]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": null, "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[multiple-trial-result]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "trial_result": "failure", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[invalid-result-value-type]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "xyz", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[empty-result-value-type]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": " ", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[no-result-value-type]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[null-result-value-type]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": null, "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[multiple-result-value-type]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value_type": "int", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[invalid-result-value]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": -98.68, "operation" : "EXP_TRIAL_RESULT"}'
	[no-result-value]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "operation" : "EXP_TRIAL_RESULT"}'
	[null-result-value]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": null, "operation" : "EXP_TRIAL_RESULT"}'
	[multiple-result-value]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78,  "result_value": 96.78, "operation" : "EXP_TRIAL_RESULT"}'
	[invalid-operation]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "xyz"}'
	[empty-operation]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : " "}'
	[no-operation]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78}'
	[null-operation]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : null}'
	[multiple-operation]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT", "operation" : "EXP_TRIAL_RESULT"}'
	[additional-field]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT", "tunable_name" : "cpuRequest"}'
	[valid-experiment-result]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"}'
	[valid-different-result]='{"experiment_id" : "'${current_id}'", "trial_number": '${trial_num}', "trial_result": "success", "result_value_type": "double", "result_value": 89.78, "operation" : "EXP_TRIAL_RESULT"}')
}

declare -A hpo_exp_result_error_messages
hpo_exp_result_error_messages=([no-id]="KeyError: 'id'"
[no-trial-number]="KeyError: 'trial_number'"
[no-trial-result]="KeyError: 'trial_result'"
[no-result-value-type]="KeyError: 'result_value_type'"
[no-result-value]="KeyError: 'result_value'"
[no-operation]="KeyError: 'operation'")

