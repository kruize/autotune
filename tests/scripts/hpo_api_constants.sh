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

# Breif description about the autotune id tests
declare -A rm_hpo_api_test_description
rm_hpo_api_test_description=([rm_hpo_post_experiment]="Start the required HPO services, post a json from recommendation manager to HPO code with various combinations and validate the result"
[rm_hpo_get_trial_json]="Start the required HPO services and query HPO using different combinations of id and trial_number and validate the result" 
[rm_hpo_post_exp_result]="Start the required HPO services, query the experiment manager and post the result from recommendation manager to HPO and validate the result")

# Tests to be carried out for RM-HPO (RM-Recommendation Manager module HPO-Hyper Parameter Optimization module)
invalid_post_tests=("invalid-id"
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

declare -A rm_hpo_post_experiment_json
# Json array for different test cases
# input: Current autotune object id
# output: Generate the json array with given id
function create_post_exp_json_array() {
	current_id=$1
	rm_hpo_post_experiment_json=([invalid-id]='{"id" : "0123456789012345678901234567890123456789012345678901234567890123456789", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[empty-id]='{"id" : " ", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[no-id]='{"url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[null-id]='{"id" : null, "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[multiple-id]='{"id" : "'${current_id}'", "id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[invalid-url]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpaces", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[empty-url]='{"id" : "'${current_id}'", "url" : "", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[no-url]='{"id" : "'${current_id}'", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[null-url]='{"id" : "'${current_id}'", "url" : null, "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[multiple-url]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[invalid-operation]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_CURRENT"}'
	[empty-operation]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : ""}'
	[no-operation]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace"}'
	[null-operation]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : null}'
	[multiple-operation]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW", "operation" : "EXP_TRIAL_GENERATE_SUBSEQUENT"}'
	[additional-field]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW", "tunable_name" : "cpuRequest"}'
	[valid-experiment]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}'
	[generate-subsequent]='{"id" : "'${current_id}'", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_SUBSEQUENT" }')
}

declare -A rm_hpo_error_messages
rm_hpo_error_messages=([invalid-id]="KeyError: '0123456789012345678901234567890123456789012345678901234567890123456789'"
[empty-id]="KeyError: ' '"
[no-id]="KeyError: 'id'"
[null-id]="KeyError: None"
[empty-url]="Invalid URL ''"
[no-url]="KeyError: 'url'"
[null-url]="Invalid URL 'None'"
[no-operation]="KeyError: 'operation'")
