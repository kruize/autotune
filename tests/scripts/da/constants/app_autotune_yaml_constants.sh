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
##### script containing constants for application autotune yaml #####
#

# get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"
pushd ${CURRENT_DIR}/.. >> setup.log

# Path to the directory containing yaml files
MANIFESTS="${PWD}/autotune_test_yamls/manifests"
app_autotune_yaml="app_autotune_yaml"
testcase_matched=0
module="da"
path="${MANIFESTS}/${module}/${app_autotune_yaml}"
autotune_object_create_msg="[AutotuneDeployment.java([0-9]*)]-Added autotune object"
autotune_exception="com.autotune.analyzer.exceptions.InvalidValueException:"

# testcases for application autotune yaml
app_autotune_tests=("objective_function"
"slo_class"
"direction"
"function_variable_name"
"function_variable_query"
"function_variable_datasource"
"function_variable_value_type"
"hpo_algo_impl"
"mode"
"label"
"labelvalue"
"datasource_name"
"datasource_url")

# tests for objective function
objective_function_testcases=(
"no-objective-function"
"no-objective-function-value"
"null-objective-function"
"valid-objective-function"
"numerical-expression"
"invalid-expression"
"no-expression"
"blank-expression"
"null-expression"
"no-expression-value"
"numerical-type"
"invalid-type"
"no-type"
"blank-type"
"null-type"
"no-type-value"
)

# tests for slo class
slo_class_testcases=("blank-sloclass"
"invalid-sloclass"
"no-slo"
"no-sloclass"
"no-slo-value"
"no-sloclass-value"
"null-sloclass"
"numerical-sloclass"
"valid-sloclass")

# tests for direction
direction_testcases=("blank-direction"
"invalid-direction"
"no-direction"
"no-direction-value"
"null-direction"
"numerical-direction"
"valid-direction")

# tests for function variable name
function_variable_name_testcases=("blank-function-variable-name"
"invalid-function-variable-name"
"no-function-variable-name"
"no-function-variable-name-value"
"null-function-variable-name"
"numerical-function-variable-name"
"valid-function-variable-name")

# tests for function variable query
function_variable_query_testcases=("blank-function-variable-query"
"invalid-function-variable-query"
"no-function-variable-query"
"no-function-variable-query-value"
"null-function-variable-query"
"numerical-function-variable-query"
"valid-function-variable-query")

# tests for function variable datasource
function_variable_datasource_testcases=("blank-function-variable-datasource"
"invalid-function-variable-datasource"
"no-function-variable-datasource"
"no-function-variable-datasource-value"
"null-function-variable-datasource"
"numerical-function-variable-datasource"
"valid-function-variable-datasource")

# tests for function variable value type
function_variable_value_type_testcases=("blank-function-variable-value-type"
"invalid-function-variable-value-type"
"no-function-variable-value-type"
"no-function-variable-value-type-value"
"null-function-variable-value-type"
"numerical-function-variable-value-type"
"valid-function-variable-value-type")

# tests for hpo_algo_impl
hpo_algo_impl_testcases=("blank-hpo-algo-impl"
"invalid-hpo-algo-impl"
"no-hpo-algo-impl-value"
"null-hpo-algo-impl"
"numerical-hpo-algo-impl"
"valid-hpo-algo-impl")

# tets for mode
mode_testcases=("blank-mode"
"invalid-mode"
"no-mode"
"no-mode-value"
"null-mode"
"numerical-mode"
"valid-mode")

# tests for label
label_testcases=("blank-label"
"invalid-label"
"no-label"
"no-label-value"
"null-label"
"numerical-label"
"valid-label")

# tests for labelvalue
labelvalue_testcases=("blank-labelvalue"
"invalid-labelvalue"
"no-labelvalue"
"no-labelvalue-value"
"null-labelvalue"
"numerical-labelvalue"
"valid-labelvalue")

# tests for datasource name
datasource_name_testcases=("blank-datasource-name"
"invalid-datasource-name"
"no-datasource-name"
"no-datasource-name-value"
"null-datasource-name"
"numerical-datasource-name"
"valid-datasource-name")

#tests for datasource url
datasource_url_testcases=("blank-datasource-url"
"invalid-datasource-url"
"no-datasource-url"
"no-datasource-url-value"
"null-datasource-url"
"numerical-datasource-url"
"valid-datasource-url")

# other test cases
autotune_other_testcases=("incomplete-autotune"
"invalid-direction-for-sloclass")

# Expected autotune object for objective function
declare -A objective_function_autotune_objects
objective_function_autotune_objects=([blank-objective-function]='true'
[no-objective-function]='false'
[no-objective-function-value]='false'
[null-objective-function]='false'
[invalid-expression]='false'
[numerical-expression]='false'
[no-expression]='false'
[blank-expression]='false'
[numerical-expression]='false'
[null-expression]='false'
[no-expression-value]='false'
[invalid-type]='false'
[no-type]='false'
[blank-type]='false'
[numerical-type]='false'
[null-type]='false'
[no-type-value]='false'
[valid-objective-function]='true'
)

# Expected log message for objective function
declare -A objective_function_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[0]}"
obj_fun_kubectl_error=': error validating data: ValidationError(Autotune.spec.slo): missing required field "objective_function" in com.recommender.v1.Autotune.spec.slo; if you choose to ignore these errors, turn validation off with --validate=false'
obj_fun_type_kubectl_error=': error validating data: ValidationError(Autotune.spec.slo.objective_function): missing required field "type" in com.recommender.v1.Autotune.spec.slo.objective_function; if you choose to ignore these errors, turn validation off with --validate=false'
no_value_kubectl_error=': error validating data: ValidationError(Autotune.spec.slo.objective_function): invalid type for com.recommender.v1.Autotune.spec.slo.objective_function: got "array", expected "map"; if you choose to ignore these errors, turn validation off with --validate=false'
objective_function_expected_log_msgs=([blank-objective-function]=''${autotune_exception}' function_variable transaction_response_time missing in objective_function'
[no-objective-function]='error: error validating "'${yaml_test_path}/no-objective-function.yaml'"'${obj_fun_kubectl_error}''
[no-objective-function-value]='error: error validating "'${yaml_test_path}/no-objective-function-value.yaml'"'${obj_fun_kubectl_error}''
[null-objective-function]='error: error validating "'${yaml_test_path}/null-objective-function.yaml'"'${obj_fun_kubectl_error}''
[valid-objective-function]=''${autotune_object_create_msg}' valid-objective-function'
[invalid-expression]='function_variable: transaction_response_time missing in objective_function'
[no-expression]='Expression value is missing or null!'
[blank-expression]='function_variable: transaction_response_time missing in objective_function'
[numerical-expression]='The Autotune "numerical-expression" is invalid: spec.slo.objective_function: Invalid value: "integer": spec.slo.objective_function in body must be of type string: "integer"'
[null-expression]='Expression value is missing or null!'
[no-expression-value]='error: error validating "'${yaml_test_path}/no-expression-value.yaml'"'${no_value_kubectl_error}''
[invalid-type]='Objective function type can only be either expression or source'
[no-type]='error: error validating "'${yaml_test_path}/no-type.yaml'"'${obj_fun_kubectl_error}''
[blank-type]='Objective function type can only be either expression or source'
[numerical-type]='The Autotune "numerical-type" is invalid: spec.slo.objective_function.type: Invalid value: "integer": spec.slo.objective_function.type in body must be of type string: "integer"'
[null-type]='error: error validating "'${yaml_test_path}/null-type.yaml'"'${obj_fun_type_kubectl_error}''
[no-type-value]='error: error validating "'${yaml_test_path}/no-type-value.yaml'"'${no_value_kubectl_error}'')

# Expected autotune object for slo class
declare -A slo_class_autotune_objects
slo_class_autotune_objects=([blank-sloclass]='true'
[invalid-sloclass]='true'
[no-slo]='false'
[no-sloclass]='true'
[no-slo-value]='false'
[no-sloclass-value]='true'
[null-sloclass]='true'
[numerical-sloclass-value]='false'
[valid-sloclass]='true')

# Expected log message for slo class
declare -A slo_class_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[1]}"
slo_kubectl_error=': error validating data: ValidationError(Autotune.spec): missing required field "slo" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false'
slo_class_expected_log_msgs=([blank-sloclass]=''${autotune_object_create_msg}' blank-sloclass'
[invalid-sloclass]=''${autotune_exception}' slo_class: rgyedg not supported'
[no-slo]='error: error validating "'${yaml_test_path}/no-slo.yaml'"'${slo_kubectl_error}''
[no-sloclass]=''${autotune_object_create_msg}' no-sloclass'
[no-slo-value]='error: error validating "'${yaml_test_path}/no-slo-value.yaml'"'${slo_kubectl_error}'' 
[no-sloclass-value]=''${autotune_object_create_msg}' no-sloclass-value' 
[null-sloclass]=''${autotune_object_create_msg}' null-sloclass' 
[numerical-sloclass]='The Autotune "numerical-sloclass" is invalid: spec.slo.slo_class: Invalid value: "integer": spec.slo.slo_class in body must be of type string: "integer"' 
[valid-sloclass]=''${autotune_object_create_msg}' valid-sloclass')

# Expected autotune object for direction
declare -A direction_autotune_objects
direction_autotune_objects=([blank-direction]='true'
[invalid-direction]='true'
[no-direction]='false'
[no-direction-value]='false'
[null-direction]='false'
[numerical-direction]='false'
[valid-direction]='true')

# Expected log message for direction
declare -A direction_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[2]}"
direction_kubectl_error=': error validating data: ValidationError(Autotune.spec.slo): missing required field "direction" in com.recommender.v1.Autotune.spec.slo; if you choose to ignore these errors, turn validation off with --validate=false'
direction_expected_log_msgs=([blank-direction]=''${autotune_exception}' Invalid direction for autotune kind' 
[invalid-direction]=''${autotune_exception}' Invalid direction for autotune kind' 
[no-direction]='error: error validating "'${yaml_test_path}/no-direction.yaml'"'${direction_kubectl_error}'' 
[no-direction-value]='error: error validating "'${yaml_test_path}/no-direction-value.yaml'"'${direction_kubectl_error}'' 
[null-direction]='error: error validating "'${yaml_test_path}/null-direction.yaml'"'${direction_kubectl_error}'' 
[numerical-direction]='The Autotune "numerical-direction" is invalid: spec.slo.direction: Invalid value: "integer": spec.slo.direction in body must be of type string: "integer"' 
[valid-direction]=''${autotune_object_create_msg}' valid-direction')

# Expected autotune object for function variable name
declare -A function_variable_name_autotune_objects
function_variable_name_autotune_objects=([blank-function-variable-name]='true'
[invalid-function-variable-name]='true'
[no-function-variable-name]='false'
[no-function-variable-name-value]='false'
[null-function-variable-name]='false'
[numerical-function-variable-name]='false'
[valid-function-variable-name]='true')

# Expected log message for function variable name
declare -A function_variable_name_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[3]}"
fun_var_name_kubectl_error=': error validating data: ValidationError(Autotune.spec.slo.function_variables\[0\]): missing required field "name" in com.recommender.v1.Autotune.spec.slo.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
function_variable_name_expected_log_msgs=([blank-function-variable-name]=''${autotune_exception}' function_variable   missing in objective_function' 
[invalid-function-variable-name]=''${autotune_exception}' function_variable transme missing in objective_function' 
[no-function-variable-name]='error: error validating "'${yaml_test_path}/no-function-variable-name.yaml'": error validating data: ValidationError(Autotune.spec.slo.function_variables): invalid type for com.recommender.v1.Autotune.spec.slo.function_variables: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-function-variable-name-value]='error: error validating "'${yaml_test_path}/no-function-variable-name-value.yaml'"'${fun_var_name_kubectl_error}'' 
[null-function-variable-name]='error: error validating "'${yaml_test_path}/null-function-variable-name.yaml'"'${fun_var_name_kubectl_error}'' 
[numerical-function-variable-name]='The Autotune "numerical-function-variable-name" is invalid: spec.slo.function_variables.name: Invalid value: "integer": spec.slo.function_variables.name in body must be of type string: "integer"' 
[valid-function-variable-name]=''${autotune_object_create_msg}' valid-function-variable-name')

# Expected autotune object for function variable query
declare -A function_variable_query_autotune_objects
function_variable_query_autotune_objects=([blank-function-variable-query]='true'
[invalid-function-variable-query]='true'
[no-function-variable-query]='false'
[no-function-variable-query-value]='false'
[null-function-variable-query]='false'
[numerical-function-variable-query]='false'
[valid-function-variable-query]='true')

# Expected log message for function variable query
declare -A function_variable_query_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[4]}"
fun_var_query_kubectl_error=': error validating data: ValidationError(Autotune.spec.slo.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.slo.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
function_variable_query_expected_log_msgs=([blank-function-variable-query]='validation from da' 
[invalid-function-variable-query]='validation from da' 
[no-function-variable-query]='error: error validating "'${yaml_test_path}/no-function-variable-query.yaml'"'${fun_var_query_kubectl_error}'' 
[no-function-variable-query-value]='error: error validating "'${yaml_test_path}/no-function-variable-query-value.yaml'"'${fun_var_query_kubectl_error}'' 
[null-function-variable-query]='error: error validating "'${yaml_test_path}/null-function-variable-query.yaml'"'${fun_var_query_kubectl_error}'' 
[numerical-function-variable-query]='The Autotune "numerical-function-variable-query" is invalid: spec.slo.function_variables.query: Invalid value: "integer": spec.slo.function_variables.query in body must be of type string: "integer"' 
[valid-function-variable-query]=''${autotune_object_create_msg}' valid-function-variable-query')

# Expected autotune object for function variable datasource
declare -A function_variable_datasource_autotune_objects
function_variable_datasource_autotune_objects=([blank-function-variable-datasource]='true'
[invalid-function-variable-datasource]='true'
[no-function-variable-datasource]='true'
[no-function-variable-datasource-value]='true'
[null-function-variable-datasource]='true'
[numerical-function-variable-datasource]='false'
[valid-function-variable-datasource]='true')

# Expected log message for slo function variable datasource
declare -A function_variable_datasource_expected_log_msgs
function_var_exception="${autotune_exception} function_variable: transaction_response_time datasource not supported"
function_variable_datasource_expected_log_msgs=([blank-function-variable-datasource]=''${function_var_exception}'' 
[invalid-function-variable-datasource]=''${function_var_exception}'' 
[no-function-variable-datasource]=''${function_var_exception}'' 
[no-function-variable-datasource-value]=''${function_var_exception}'' 
[null-function-variable-datasource]=''${function_var_exception}'' 
[numerical-function-variable-datasource]='The Autotune "numerical-function-variable-datasource" is invalid: spec.slo.function_variables.datasource: Invalid value: "integer": spec.slo.function_variables.datasource in body must be of type string: "integer"' 
[valid-function-variable-datasource]=''${autotune_object_create_msg}' valid-function-variable-datasource' )

# Expected autotune object for function variable value type
declare -A function_variable_value_type_autotune_objects
function_variable_value_type_autotune_objects=([blank-function-variable-value-type]='true'
[invalid-function-variable-value-type]='true'
[no-function-variable-value-type]='false'
[no-function-variable-value-type-value]='false'
[null-function-variable-value-type]='false'
[numerical-function-variable-value-type]='false'
[valid-function-variable-value-type]='true')

# Expected log message for function variable value type
declare -A function_variable_value_type_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[6]}"
fun_var_type_kubectl_error=': error validating data: ValidationError(Autotune.spec.slo.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.slo.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
function_variable_value_type_expected_log_msgs=([blank-function-variable-value-type]=''${autotune_exception}' function_variable: transaction_response_time value_type not supported' 
[invalid-function-variable-value-type]=''${autotune_exception}' function_variable: transaction_response_time value_type not supported' 
[no-function-variable-value-type]='error: error validating "'${yaml_test_path}/no-function-variable-value-type.yaml'"'${fun_var_type_kubectl_error}'' 
[no-function-variable-value-type-value]='error: error validating "'${yaml_test_path}/no-function-variable-value-type-value.yaml'"'${fun_var_type_kubectl_error}'' 
[null-function-variable-value-type]='error: error validating "'${yaml_test_path}/null-function-variable-value-type.yaml'"'${fun_var_type_kubectl_error}'' 
[numerical-function-variable-value-type]='The Autotune "numerical-function-variable-value-type" is invalid: spec.slo.function_variables.value_type: Invalid value: "integer": spec.slo.function_variables.value_type in body must be of type string: "integer"' 
[valid-function-variable-value-type]=''${autotune_object_create_msg}' valid-function-variable-value-type' )

# Expected autotune object for hpo_algo_impl
declare -A hpo_algo_impl_autotune_objects
hpo_algo_impl_autotune_objects=([blank-hpo-algo-impl]='true'
[invalid-hpo-algo-impl]='true'
[no-hpo-algo-impl-value]='true'
[null-hpo-algo-impl]='true'
[numerical-hpo-algo-impl]='false'
[valid-hpo-algo-impl]='true')

# Expected log message for hpo_algo_impl
declare -A hpo_algo_impl_expected_log_msgs
hpo_algo_impl_expected_log_msgs=([blank-hpo-algo-impl]=''${autotune_exception}' Hyperparameter Optimization Algorithm   not supported'
[invalid-hpo-algo-impl]=''${autotune_exception}' Hyperparameter Optimization Algorithm xyz not supported'
[no-hpo-algo-impl-value]='validation from da'
[null-hpo-algo-impl]='validation from da'
[numerical-hpo-algo-impl]='The Autotune "numerical-hpo-algo-impl" is invalid: spec.slo.hpo_algo_impl: Invalid value: "integer": spec.slo.hpo_algo_impl in body must be of type string: "integer"'
[valid-hpo-algo-impl]=''${autotune_object_create_msg}' valid-hpo-algo-impl')

# Expected autotune object for mode
declare -A mode_autotune_objects
mode_autotune_objects=([blank-mode]='true'
[invalid-mode]='true'
[no-mode]='false'
[no-mode-value]='false'
[null-mode]='false'
[numerical-mode]='false'
[valid-mode]='true')

# Expected log message for mode
declare -A mode_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[8]}"
mode_kubectl_error=': error validating data: ValidationError(Autotune.spec): missing required field "mode" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false'
mode_expected_log_msgs=([blank-mode]=''${autotune_exception}' Autotune object mode not supported' 
[invalid-mode]=''${autotune_exception}' Autotune object mode not supported' 
[no-mode]='error: error validating "'${yaml_test_path}/no-mode.yaml'"'${mode_kubectl_error}'' 
[no-mode-value]='error: error validating "'${yaml_test_path}/no-mode-value.yaml'"'${mode_kubectl_error}'' 
[null-mode]='error: error validating "'${yaml_test_path}/null-mode.yaml'"'${mode_kubectl_error}'' 
[numerical-mode]='The Autotune "numerical-mode" is invalid: spec.mode: Invalid value: "integer": spec.mode in body must be of type string: "integer"' 
[valid-mode]=''${autotune_object_create_msg}' valid-mode' )

# Expected autotune object for label
declare -A label_autotune_objects
label_autotune_objects=([blank-label]='true'
[invalid-label]='true'
[no-label]='true'
[no-label-value]='true'
[null-label]='true'
[numerical-label]='false'
[valid-label]='true')

# Expected log message for label
declare -A label_expected_log_msgs
exception_invalid_label=''${autotune_exception}' Invalid MatchLabel'
label_expected_log_msgs=([blank-label]=''${exception_invalid_label}' in selector' 
[invalid-label]=''${autotune_exception}' label:   not supported' 
[no-label]=''${exception_invalid_label}'' 
[no-label-value]=''${exception_invalid_label}'' 
[null-label]=''${exception_invalid_label}'' 
[numerical-label]='The Autotune "numerical-label" is invalid: spec.selector.matchLabel: Invalid value: "integer": spec.selector.matchLabel in body must be of type string: "integer"' 
[valid-label]=''${autotune_object_create_msg}' valid-label' )

# Expected autotune object for label value
declare -A labelvalue_autotune_objects
labelvalue_autotune_objects=([blank-labelvalue]='true'
[invalid-labelvalue]='true'
[no-labelvalue]='true'
[no-labelvalue-value]='true'
[null-labelvalue]='true'
[numerical-labelvalue]='false'
[valid-labelvalue]='true')

# Expected log message for label value
declare -A labelvalue_expected_log_msgs
labelvalue_expected_log_msgs=([blank-labelvalue]=''${autotune_exception}' Invalid or blank MatchLabelValue in selector' 
[invalid-labelvalue]=''${autotune_exception}' labelvalue:   not supported' 
[no-labelvalue]=''${exception_invalid_label}'Value' 
[no-labelvalue-value]=''${exception_invalid_label}'Value' 
[null-labelvalue]=''${exception_invalid_label}'Value' 
[numerical-labelvalue]='The Autotune "numerical-labelvalue" is invalid: spec.selector.matchLabelValue: Invalid value: "integer": spec.selector.matchLabelValue in body must be of type string: "integer"' 
[valid-labelvalue]=''${autotune_object_create_msg}' valid-labelvalue' )

# Expected autotune object for datasource name
declare -A datasource_name_autotune_objects
datasource_name_autotune_objects=([blank-datasource-name]='true'
[invalid-datasource-name]='true'
[no-datasource-name]='false'
[no-datasource-name-value]='false'
[null-datasource-name]='false'
[numerical-datasource-name]='false'
[valid-datasource-name]='true')

# Expected log message for datasource name
declare -A datasource_name_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[11]}"
datasource_name_kubectl_error=': error validating data: ValidationError(Autotune.spec.datasource): missing required field "name" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
datasource_name_expected_log_msgs=([blank-datasource-name]=''${autotune_exception}' datasource-name:   not supported'
[invalid-datasource-name]=''${autotune_exception}' datasource-name:   not supported'
[no-datasource-name]='error: error validating "'${yaml_test_path}/no-datasource-name.yaml'"'${datasource_name_kubectl_error}''
[no-datasource-name-value]='error: error validating "'${yaml_test_path}/no-datasource-name-value.yaml'"'${datasource_name_kubectl_error}''
[null-datasource-name]='error: error validating "'${yaml_test_path}/null-datasource-name.yaml'"'${datasource_name_kubectl_error}''
[numerical-datasource-name]='The Autotune "numerical-datasource-name" is invalid: spec.datasource.name: Invalid value: "integer": spec.datasource.name in body must be of type string: "integer"'
[valid-datasource-name]=''${autotune_object_create_msg}' valid-datasource-name')

# Expected autotune object for datasource url
declare -A datasource_url_autotune_objects
datasource_url_autotune_objects=([blank-datasource-url]='true'
[invalid-datasource-url]='true'
[no-datasource-url]='false'
[no-datasource-url-value]='false'
[null-datasource-url]='false'
[numerical-datasource-url]='false'
[valid-datasource-url]='true')

# Expected log message for datasource url
declare -A datasource_url_expected_log_msgs
yaml_test_path="${path}/${app_autotune_tests[12]}"
datasource_url_kubectl_error=': error validating data: ValidationError(Autotune.spec.datasource): missing required field "value" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
datasource_url_expected_log_msgs=([blank-datasource-url]=''${autotune_exception}' datasource-url:   not supported'
[invalid-datasource-url]=''${autotune_exception}' datasource-url:   not supported'
[no-datasource-url]='error: error validating "'${yaml_test_path}/no-datasource-url.yaml'"'${datasource_url_kubectl_error}''
[no-datasource-url-value]='error: error validating "'${yaml_test_path}/no-datasource-url-value.yaml'"'${datasource_url_kubectl_error}''
[null-datasource-url]='error: error validating "'${yaml_test_path}/null-datasource-url.yaml'"'${datasource_url_kubectl_error}''
[numerical-datasource-url]='The Autotune "numerical-datasource-url" is invalid: spec.datasource.value: Invalid value: "integer": spec.datasource.value in body must be of type string: "integer"'
[valid-datasource-url]=''${autotune_object_create_msg}' valid-datasource-url')

# Expected autotune object for other test cases
declare -A autotune_other_autotune_objects
autotune_other_autotune_objects=([incomplete-autotune]='false'
[invalid-direction-for-sloclass]='true')

# Expected log message for other test cases
declare -A autotune_other_expected_log_msgs
autotune_other_expected_log_msgs=([incomplete-autotune]='error: error validating "'${path}/autotune_other/incomplete-autotune.yaml'": error validating data: ValidationError(Autotune): missing required field "spec" in com.recommender.v1.Autotune; if you choose to ignore these errors, turn validation off with --validate=false'
[invalid-direction-for-sloclass]='validation from da')
