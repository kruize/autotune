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

# testcases for application autotune yaml
app_autotune_tests=("objective_function"
"sla_class"
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
objective_function_testcases=("blank-objective-function"
"invalid-objective-function"
"no-objective-function"
"no-objective-function-value"
"null-objective-function"
"numerical-objective-function"
"valid-objective-function")

# tests for sla class
sla_class_testcases=("blank-slaclass"
"invalid-slaclass"
"no-sla"
"no-slaclass"
"no-sla-value"
"no-slaclass-value"
"null-slaclass"
"numerical-slaclass"
"valid-slaclass")

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
"invalid-direction-for-slaclass")

# Expected autotune object for objective function
declare -A objective_function_autotune_objects
objective_function_autotune_objects=([blank-objective-function]='true'
[invalid-objective-function]='true'
[no-objective-function]='false'
[no-objective-function-value]='false'
[null-objective-function]='false'
[numerical-objective-function]='false'
[valid-objective-function]='true')

# Expected log message for objective function
declare -A objective_function_expected_log_msgs
objective_function_expected_log_msgs=([blank-objective-function]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable transaction_response_time missing in objective_function' 
[invalid-objective-function]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable transaction_response_time missing in objective_function'
[no-objective-function]='error: error validating "'${path}/${app_autotune_tests[0]}/no-objective-function.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "objective_function" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false'
[no-objective-function-value]='error: error validating "'${path}/${app_autotune_tests[0]}/no-objective-function-value.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "objective_function" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false'
[null-objective-function]='error: error validating "'${path}/${app_autotune_tests[0]}/null-objective-function.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "objective_function" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-objective-function]='The Autotune "numerical-objective-function" is invalid: spec.sla.objective_function: Invalid value: "integer": spec.sla.objective_function in body must be of type string: "integer"'

# Expected autotune object for sla class
declare -A sla_class_autotune_objects
sla_class_autotune_objects=([blank-slaclass]='true'
[invalid-slaclass]='true'
[no-sla]='false'
[no-slaclass]='true'
[no-sla-value]='false'
[no-slaclass-value]='true'
[null-slaclass]='true'
[numerical-slaclass-value]='false'
[valid-slaclass]='true')

# Expected log message for sla class
declare -A sla_class_expected_log_msgs
sla_class_expected_log_msgs=([blank-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object blank-slaclass'
[invalid-slaclass]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: sla_class: rgyedg not supported'
[no-sla]='error: error validating "'${path}/${app_autotune_tests[1]}/no-sla.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "sla" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false'
[no-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object no-slaclass'
[no-sla-value]='error: error validating "'${path}/${app_autotune_tests[1]}/no-sla-value.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "sla" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false'
[no-slaclass-value]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object no-slaclass-value-slaclass'
[null-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object null-slaclass'
[numerical-slaclass]='The Autotune "numerical-slaclass" is invalid: spec.sla.sla_class: Invalid value: "integer": spec.sla.sla_class in body must be of type string: "integer"'
[valid-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-slaclass')

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
direction_expected_log_msgs=([blank-direction]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid direction for autotune kind'
[invalid-direction]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid direction for autotune kind'
[no-direction]='error: error validating "'${path}/${app_autotune_tests[2]}/no-direction.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "direction" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false'
[no-direction-value]='error: error validating "'${path}/${app_autotune_tests[2]}/no-direction-value.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "direction" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false'
[null-direction]='error: error validating "'${path}/${app_autotune_tests[2]}/null-direction.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "direction" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-direction]='The Autotune "numerical-direction" is invalid: spec.sla.direction: Invalid value: "integer": spec.sla.direction in body must be of type string: "integer"'
[valid-direction]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-direction')

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
function_variable_name_expected_log_msgs=([blank-function-variable-name]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable   missing in objective_function'
[invalid-function-variable-name]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable transme missing in objective_function'
[no-function-variable-name]='error: error validating "'${path}/${app_autotune_tests[3]}/no-function-variable-name.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables): invalid type for com.recommender.v1.Autotune.spec.sla.function_variables: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false'
[no-function-variable-name-value]='error: error validating "'${path}/${app_autotune_tests[3]}/no-function-variable-name-value.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "name" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[null-function-variable-name]='error: error validating "'${path}/${app_autotune_tests[3]}/null-function-variable-name.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "name" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-function-variable-name]='The Autotune "numerical-function-variable-name" is invalid: spec.sla.function_variables.name: Invalid value: "integer": spec.sla.function_variables.name in body must be of type string: "integer"'
[valid-function-variable-name]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-name')

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
function_variable_query_expected_log_msgs=([blank-function-variable-query]='validation from da'
[invalid-function-variable-query]='validation from da'
[no-function-variable-query]='error: error validating "'${path}/${app_autotune_tests[4]}/no-function-variable-query.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[no-function-variable-query-value]='error: error validating "'${path}/${app_autotune_tests[4]}/no-function-variable-query-value.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[null-function-variable-query]='error: error validating "'${path}/${app_autotune_tests[4]}/null-function-variable-query.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-function-variable-query]='The Autotune "numerical-function-variable-query" is invalid: spec.sla.function_variables.query: Invalid value: "integer": spec.sla.function_variables.query in body must be of type string: "integer"'
[valid-function-variable-query]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-query')

# Expected autotune object for function variable datasource
declare -A function_variable_datasource_autotune_objects
function_variable_datasource_autotune_objects=([blank-function-variable-datasource]='true'
[invalid-function-variable-datasource]='true'
[no-function-variable-datasource]='true'
[no-function-variable-datasource-value]='true'
[null-function-variable-datasource]='true'
[numerical-function-variable-datasource]='false'
[valid-function-variable-datasource]='true')

# Expected log message for sla function variable datasource
declare -A function_variable_datasource_expected_log_msgs
function_variable_datasource_expected_log_msgs=([blank-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported'
[invalid-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported'
[no-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported'
[no-function-variable-datasource-value]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported'
[null-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported'
[numerical-function-variable-datasource]='The Autotune "numerical-function-variable-datasource" is invalid: spec.sla.function_variables.datasource: Invalid value: "integer": spec.sla.function_variables.datasource in body must be of type string: "integer"'
[valid-function-variable-datasource]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-datasource')

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
function_variable_value_type_expected_log_msgs=([blank-function-variable-value-type]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time value_type not supported'
[invalid-function-variable-value-type]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time value_type not supported'
[no-function-variable-value-type]='error: error validating "'${path}/${app_autotune_tests[6]}/no-function-variable-value-type.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[no-function-variable-value-type-value]='error: error validating "'${path}/${app_autotune_tests[6]}/no-function-variable-value-type-value.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[null-function-variable-value-type]='error: error validating "'${path}/${app_autotune_tests[6]}/null-function-variable-value-type.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-function-variable-value-type]='The Autotune "numerical-function-variable-value-type" is invalid: spec.sla.function_variables.value_type: Invalid value: "integer": spec.sla.function_variables.value_type in body must be of type string: "integer"'
[valid-function-variable-value-type]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-value-type')

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
hpo_algo_impl_expected_log_msgs=([blank-hpo-algo-impl]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Hyperparameter Optimization Algorithm   not supported'
[invalid-hpo-algo-impl]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Hyperparameter Optimization Algorithm xyz not supported'
[no-hpo-algo-impl-value]='validation from da'
[null-hpo-algo-impl]='validation from da'
[numerical-hpo-algo-impl]='The Autotune "numerical-hpo-algo-impl" is invalid: spec.sla.hpo_algo_impl: Invalid value: "integer": spec.sla.hpo_algo_impl in body must be of type string: "integer"'
[valid-hpo-algo-impl]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-hpo-algo-impl')

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
mode_expected_log_msgs=([blank-mode]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Autotune object mode not supported'
[invalid-mode]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Autotune object mode not supported'
[no-mode]='error: error validating "'${path}/${app_autotune_tests[8]}/no-mode.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "mode" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false'
[no-mode-value]='error: error validating "'${path}/${app_autotune_tests[8]}/no-mode-value.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "mode" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false'
[null-mode]='error: error validating "'${path}/${app_autotune_tests[8]}/null-mode.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "mode" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-mode]='The Autotune "numerical-mode" is invalid: spec.mode: Invalid value: "integer": spec.mode in body must be of type string: "integer"'
[valid-mode]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-mode')

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
label_expected_log_msgs=([blank-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: label:   not supported'
[invalid-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: label:   not supported'
[no-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabel'
[no-label-value]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabel'
[null-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabel'
[numerical-label]='The Autotune "numerical-label" is invalid: spec.selector.matchLabel: Invalid value: "integer": spec.selector.matchLabel in body must be of type string: "integer"'
[valid-label]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-label')

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
labelvalue_expected_log_msgs=([blank-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: labelvalue:   not supported'
[invalid-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: labelvalue:   not supported'
[no-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabelValue'
[no-labelvalue-value]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabelValue'
[null-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabelValue'
[numerical-labelvalue]='The Autotune "numerical-labelvalue" is invalid: spec.selector.matchLabelValue: Invalid value: "integer": spec.selector.matchLabelValue in body must be of type string: "integer"'
[valid-labelvalue]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-labelvalue')

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
datasource_name_expected_log_msgs=([blank-datasource-name]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: datasource-name:   not supported'
[invalid-datasource-name]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: datasource-name:   not supported'
[no-datasource-name]='error: error validating "'${path}/${app_autotune_tests[11]}/no-datasource-name.yaml'": error validating data: ValidationError(Autotune.spec.datasource): missing required field "name" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
[no-datasource-name-value]='error: error validating "'${path}/${app_autotune_tests[11]}/no-datasource-name-value.yaml'": error validating data: ValidationError(Autotune.spec.datasource): missing required field "name" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
[null-datasource-name]='error: error validating "'${path}/${app_autotune_tests[11]}/null-datasource-name.yaml'": error validating data: ValidationError(Autotune.spec.datasource): missing required field "name" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-datasource-name]='The Autotune "numerical-datasource-name" is invalid: spec.datasource.name: Invalid value: "integer": spec.datasource.name in body must be of type string: "integer"'
[valid-datasource-name]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-datasource-name')

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
datasource_url_expected_log_msgs=([blank-datasource-url]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: datasource-url:   not supported'
[invalid-datasource-url]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: datasource-name:   not supported'
[no-datasource-url]='error: error validating "'${path}/${app_autotune_tests[12]}/no-datasource-url.yaml'": error validating data: ValidationError(Autotune.spec.datasource): missing required field "value" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
[no-datasource-url-value]='error: error validating "'${path}/${app_autotune_tests[12]}/no-datasource-url-value.yaml'": error validating data: ValidationError(Autotune.spec.datasource): missing required field "value" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
[null-datasource-url]='error: error validating "'${path}/${app_autotune_tests[12]}/null-datasource-url.yaml'": error validating data: ValidationError(Autotune.spec.datasource): missing required field "value" in com.recommender.v1.Autotune.spec.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-datasource-url]='The Autotune "numerical-datasource-url" is invalid: spec.datasource.value: Invalid value: "integer": spec.datasource.value in body must be of type string: "integer"'
[valid-datasource-url]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-datasource-url')

# Expected autotune object for other test cases
declare -A autotune_other_autotune_objects
autotune_other_autotune_objects=([incomplete-autotune]='false'
[invalid-direction-for-slaclass]='true')

# Expected log message for other test cases
declare -A autotune_other_expected_log_msgs
autotune_other_expected_log_msgs=([incomplete-autotune]='error: error validating "'${path}/autotune_other/incomplete-autotune.yaml'": error validating data: ValidationError(Autotune): missing required field "spec" in com.recommender.v1.Autotune; if you choose to ignore these errors, turn validation off with --validate=false'
[invalid-direction-for-slaclass]='validation from da')
