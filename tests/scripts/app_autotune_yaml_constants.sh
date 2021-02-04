#!/bin/bash
#
# Copyright (c) 2020, 2021 RedHat, IBM Corporation and others.
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
app_autotune_tests=("objective_function" "sla_class" "direction" "function_variable_name" "function_variable_query" "function_variable_datasource" "function_variable_value_type" "mode" "label" "labelvalue" )

# tests for objective function 
objective_function_testcases=("blank-objective-function" "invalid-objective-function"  "multiple-objective-function-sections" "no-objective-function" "no-objective-function-value" "null-objective-function" "numerical-objective-function" "valid-objective-function")

# tests for sla class 
sla_class_testcases=("blank-slaclass" "invalid-slaclass" "multiple-slaclass" "multiple-sla-sections" "no-sla" "no-slaclass" "no-sla-value" "no-slaclass-value" "null-slaclass" "numerical-slaclass" "valid-slaclass")

# tests for direction 
direction_testcases=("blank-direction" "invalid-direction" "multiple-direction-sections" "no-direction" "no-direction-value" "null-direction" "numerical-direction" "valid-direction")

# tests for function variable name 
function_variable_name_testcases=("blank-function-variable-name" "invalid-function-variable-name" "multiple-function-variable-name-sections" "no-function-variable-name" "no-function-variable-name-value" "null-function-variable-name" "numerical-function-variable-name" "valid-function-variable-name")

# tests for function variable query
function_variable_query_testcases=("blank-function-variable-query" "invalid-function-variable-query" "multiple-function-variable-query-sections" "no-function-variable-query" "no-function-variable-query-value" "null-function-variable-query" "numerical-function-variable-query" "valid-function-variable-query")

# tests for function variable datasource
function_variable_datasource_testcases=("blank-function-variable-datasource" "invalid-function-variable-datasource" "multiple-function-variable-datasource-sections" "no-function-variable-datasource" "no-function-variable-datasource-value" "null-function-variable-datasource" "numerical-function-variable-datasource" "valid-function-variable-datasource")

# tests for function variable value type
function_variable_value_type_testcases=("blank-function-variable-value-type" "invalid-function-variable-value-type" "multiple-function-variable-value-type-sections" "no-function-variable-value-type" "no-function-variable-value-type-value" "null-function-variable-value-type" "numerical-function-variable-value-type" "valid-function-variable-value-type")

# tets for mode
mode_testcases=("blank-mode" "invalid-mode" "multiple-mode-sections" "no-mode" "no-mode-value" "null-mode" "numerical-mode" "valid-mode")

# tests for label
label_testcases=("blank-label" "invalid-label" "multiple-label-sections" "no-label" "no-label-value" "null-label" "numerical-label" "valid-label")

# tests for labelvalue
labelvalue_testcases=("blank-labelvalue" "invalid-labelvalue" "multiple-labelvalue-sections" "no-labelvalue" "no-labelvalue-value" "null-labelvalue" "numerical-labelvalue" "valid-labelvalue")

# Expected autotune object for objective function
declare -A objective_function_autotune_objects
objective_function_autotune_objects=([blank-objective-function]='true' [invalid-objective-function]='true' [multiple-objective-function-sections]='true' [no-objective-function]='false' [no-objective-function-value]='false' [null-objective-function]='false' [numerical-objective-function]='false' [valid-objective-function]='true')
# Expected log message for objective function
declare -A objective_function_expected_log_msgs
objective_function_expected_log_msgs=([blank-objective-function]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable transaction_response_time missing in objective_function' [invalid-objective-function]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable transaction_response_time missing in objective_function' [multiple-objective-function-sections]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable transaction_response_time missing in objective_function' [no-objective-function]='error: error validating "'${path}/${app_autotune_tests[0]}/no-objective-function.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "objective_function" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false' [no-objective-function-value]='error: error validating "'${path}/${app_autotune_tests[0]}/no-objective-function-value.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "objective_function" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false' [null-objective-function]='error: error validating "'${path}/${app_autotune_tests[0]}/null-objective-function.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "objective_function" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false' [numerical-objective-function]='The Autotune "numerical-objective-function" is invalid: spec.sla.objective_function: Invalid value: "integer": spec.sla.objective_function in body must be of type string: "integer"' [valid-objective-function]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-objective-function')

# Expected autotune object for sla class
declare -A sla_class_autotune_objects
sla_class_autotune_objects=([blank-slaclass]='true' [invalid-slaclass]='true' [multiple-slaclassc]='false' [multiple-sla-sections]='false' [no-sla]='false' [no-slaclass]='true' [no-sla-value]='false' [no-slaclass-value]='true' [null-slaclass]='true' [numerical-slaclass-value]='false' [valid-slaclass]='true')
# Expected log message for sla class
declare -A sla_class_expected_log_msgs
sla_class_expected_log_msgs=([blank-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object blank-slaclass' [invalid-slaclass]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: sla_class: rgyedg not supported' [multiple-slaclass]='error from crd' [multiple-sla-sections]='error from crd' [no-sla]='error: error validating "'${path}/${app_autotune_tests[1]}/no-sla.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "sla" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false' [no-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object no-slaclass' [no-sla-value]='error: error validating "'${path}/${app_autotune_tests[1]}/no-sla-value.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "sla" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false' [no-slaclass-value]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object no-slaclass-value-slaclass' [null-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object null-slaclass' [numerical-slaclass]='The Autotune "numerical-slaclass" is invalid: spec.sla.sla_class: Invalid value: "integer": spec.sla.sla_class in body must be of type string: "integer"' [valid-slaclass]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-slaclass')

# Expected autotune object for direction
declare -A direction_autotune_objects
direction_autotune_objects=([blank-direction]='true' [invalid-direction]='true' [multiple-direction-sections]='false' [no-direction]='false' [no-direction-value]='false' [null-direction]='false' [numerical-direction]='false' [valid-direction]='true')
# Expected log message for direction
declare -A direction_expected_log_msgs
direction_expected_log_msgs=([blank-direction]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid direction for autotune kind' [invalid-direction]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid direction for autotune kind' [multiple-direction-sections]='validation from crd' [no-direction]='error: error validating "'${path}/${app_autotune_tests[2]}/no-direction.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "direction" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false' [no-direction-value]='error: error validating "'${path}/${app_autotune_tests[2]}/no-direction-value.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "direction" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false' [null-direction]='error: error validating "'${path}/${app_autotune_tests[2]}/null-direction.yaml'": error validating data: ValidationError(Autotune.spec.sla): missing required field "direction" in com.recommender.v1.Autotune.spec.sla; if you choose to ignore these errors, turn validation off with --validate=false' [numerical-direction]='The Autotune "numerical-direction" is invalid: spec.sla.direction: Invalid value: "integer": spec.sla.direction in body must be of type string: "integer"' [valid-direction]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-direction')

# Expected autotune object for function variable name
declare -A function_variable_name_autotune_objects
function_variable_name_autotune_objects=([blank-function-variable-name]='true' [invalid-function-variable-name]='true' [multiple-function-variable-name-sections]='false' [no-function-variable-name]='false' [no-function-variable-name-value]='false' [null-function-variable-name]='false' [numerical-function-variable-name]='false' [valid-function-variable-name]='true')
# Expected log message for function variable name
declare -A function_variable_name_expected_log_msgs
function_variable_name_expected_log_msgs=([blank-function-variable-name]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable   missing in objective_function' [invalid-function-variable-name]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable transme missing in objective_function' [multiple-function-variable-name-sections]='error: error validating "'${path}/${app_autotune_tests[3]}/multiple-function-variable-name-sections.yaml'": error validating data: \[ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.sla.function_variables, ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.sla.function_variables\]; if you choose to ignore these errors, turn validation off with --validate=false' [no-function-variable-name]='error: error validating "'${path}/${app_autotune_tests[3]}/no-function-variable-name.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables): invalid type for com.recommender.v1.Autotune.spec.sla.function_variables: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false' [no-function-variable-name-value]='error: error validating "'${path}/${app_autotune_tests[3]}/no-function-variable-name-value.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "name" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [null-function-variable-name]='error: error validating "'${path}/${app_autotune_tests[3]}/null-function-variable-name.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "name" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [numerical-function-variable-name]='The Autotune "numerical-function-variable-name" is invalid: spec.sla.function_variables.name: Invalid value: "integer": spec.sla.function_variables.name in body must be of type string: "integer"' [valid-function-variable-name]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-name')

# Expected autotune object for function variable query
declare -A function_variable_query_autotune_objects
function_variable_query_autotune_objects=([blank-function-variable-query]='true' [invalid-function-variable-query]='true' [multiple-function-variable-query-sections]='false' [no-function-variable-query]='false' [no-function-variable-query-value]='false' [null-function-variable-query]='false' [numerical-function-variable-query]='false' [valid-function-variable-query]='true')
# Expected log message for function variable query
declare -A function_variable_query_expected_log_msgs
function_variable_query_expected_log_msgs=([blank-function-variable-query]='validation from da' [invalid-function-variable-query]='validation from da' [multiple-function-variable-query-sections]='validation from crd' [no-function-variable-query]='error: error validating "'${path}/${app_autotune_tests[4]}/no-function-variable-query.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [no-function-variable-query-value]='error: error validating "'${path}/${app_autotune_tests[4]}/no-function-variable-query-value.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [null-function-variable-query]='error: error validating "'${path}/${app_autotune_tests[4]}/null-function-variable-query.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "query" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [numerical-function-variable-query]='The Autotune "numerical-function-variable-query" is invalid: spec.sla.function_variables.query: Invalid value: "integer": spec.sla.function_variables.query in body must be of type string: "integer"' [valid-function-variable-query]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-query')

# Expected autotune object for function variable datasource
declare -A function_variable_datasource_autotune_objects
function_variable_datasource_autotune_objects=([blank-function-variable-datasource]='true' [invalid-function-variable-datasource]='true' [multiple-function-variable-datasource]='true' [multiple-function-variable-datasource-sections]='true' [no-function-variable-datasource]='true' [no-function-variable-datasource-value]='true' [null-function-variable-datasource]='true' [numerical-function-variable-datasource]='false' [valid-function-variable-datasource]='true')
# Expected log message for sla function variable datasource
declare -A function_variable_datasource_expected_log_msgs
function_variable_datasource_expected_log_msgs=([blank-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported' [invalid-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported' [multiple-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported' [multiple-function-variable-datasource-sections]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported' [no-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported' [no-function-variable-datasource-value]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported' [null-function-variable-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time datasource not supported' [numerical-function-variable-datasource]='The Autotune "numerical-function-variable-datasource" is invalid: spec.sla.function_variables.datasource: Invalid value: "integer": spec.sla.function_variables.datasource in body must be of type string: "integer"' [valid-function-variable-datasource]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-datasource' )

# Expected autotune object for function variable value type
declare -A function_variable_value_type_autotune_objects
function_variable_value_type_autotune_objects=([blank-function-variable-value-type]='true' [invalid-function-variable-value-type]='true'  [multiple-function-variable-value-type-sections]='true' [no-function-variable-value-type]='false' [no-function-variable-value-type-value]='false' [null-function-variable-value-type]='false' [numerical-function-variable-value-type]='false' [valid-function-variable-value-type]='true')
# Expected log message for function variable value type
declare -A function_variable_value_type_expected_log_msgs
function_variable_value_type_expected_log_msgs=([blank-function-variable-value-type]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time value_type not supported' [invalid-function-variable-value-type]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time value_type not supported' [multiple-function-variable-value-type-sections]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: function_variable: transaction_response_time value_type not supported' [no-function-variable-value-type]='error: error validating "'${path}/${app_autotune_tests[6]}/no-function-variable-value-type.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [no-function-variable-value-type-value]='error: error validating "'${path}/${app_autotune_tests[6]}/no-function-variable-value-type-value.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [null-function-variable-value-type]='error: error validating "'${path}/${app_autotune_tests[6]}/null-function-variable-value-type.yaml'": error validating data: ValidationError(Autotune.spec.sla.function_variables\[0\]): missing required field "value_type" in com.recommender.v1.Autotune.spec.sla.function_variables; if you choose to ignore these errors, turn validation off with --validate=false' [numerical-function-variable-value-type]='The Autotune "numerical-function-variable-value-type" is invalid: spec.sla.function_variables.value_type: Invalid value: "integer": spec.sla.function_variables.value_type in body must be of type string: "integer"' [valid-function-variable-value-type]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-function-variable-value-type' )

# Expected autotune object for mode
declare -A mode_autotune_objects
mode_autotune_objects=([blank-mode]='true' [invalid-mode]='true' [multiple-mode]='false' [multiple-mode-sections]='false' [no-mode]='false' [no-mode-value]='false' [null-mode]='false' [numerical-mode]='false' [valid-mode]='true')
# Expected log message for mode
declare -A mode_expected_log_msgs
mode_expected_log_msgs=([blank-mode]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Autotune object mode not supported' [invalid-mode]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Autotune object mode not supported' [multiple-mode]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Autotune object mode not supported' [multiple-mode-sections]='error from crd' [no-mode]='error: error validating "'${path}/${app_autotune_tests[7]}/no-mode.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "mode" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false' [no-mode-value]='error: error validating "'${path}/${app_autotune_tests[7]}/no-mode-value.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "mode" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false' [null-mode]='error: error validating "'${path}/${app_autotune_tests[7]}/null-mode.yaml'": error validating data: ValidationError(Autotune.spec): missing required field "mode" in com.recommender.v1.Autotune.spec; if you choose to ignore these errors, turn validation off with --validate=false' [numerical-mode]='The Autotune "numerical-mode" is invalid: spec.mode: Invalid value: "integer": spec.mode in body must be of type string: "integer"' [valid-mode]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-mode' )

# Expected autotune object for label
declare -A label_autotune_objects
label_autotune_objects=([blank-label]='true' [invalid-label]='true' [multiple-label]='false' [multiple-label-sections]='false' [no-label]='true' [no-label-value]='true' [null-label]='true' [numerical-label]='false' [valid-label]='true')
# Expected log message for label
declare -A label_expected_log_msgs
label_expected_log_msgs=([blank-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: label:   not supported' [invalid-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: label:   not supported' [multiple-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: label:   not supported' [multiple-label-sections]='error from crd' [no-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabel' [no-label-value]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabel' [null-label]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabel' [numerical-label]='The Autotune "numerical-label" is invalid: spec.selector.matchLabel: Invalid value: "integer": spec.selector.matchLabel in body must be of type string: "integer"' [valid-label]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-label' )

# Expected autotune object for label value
declare -A labelvalue_autotune_objects
labelvalue_autotune_objects=([blank-labelvalue]='true' [invalid-labelvalue]='true' [multiple-labelvalue-sections]='false' [no-labelvalue]='true' [no-labelvalue-value]='true' [null-labelvalue]='true' [numerical-labelvalue]='false' [valid-labelvalue]='true')
# Expected log message for label value
declare -A labelvalue_expected_log_msgs
labelvalue_expected_log_msgs=([blank-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: labelvalue:   not supported' [invalid-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: labelvalue:   not supported' [multiple-labelvalue-sections]='error from crd' [no-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabelValue' [no-labelvalue-value]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabelValue' [null-labelvalue]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Invalid MatchLabelValue' [numerical-labelvalue]='The Autotune "numerical-labelvalue" is invalid: spec.selector.matchLabelValue: Invalid value: "integer": spec.selector.matchLabelValue in body must be of type string: "integer"' [valid-labelvalue]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotune object valid-labelvalue' )

# Expected autotune object for incomplete app autotune yaml
declare -A incomplete_yaml_autotune_objects
incomplete_yaml_autotune_objects=([incomplete-autotune]='false')
# Expected log message for label value
declare -A incomplete_expected_log_msgs
incomplete_yaml_expected_log_msgs=([incomplete-autotune]='error: error validating "'${path}/incomplete-autotune.yaml'": error validating data: ValidationError(Autotune): missing required field "spec" in com.recommender.v1.Autotune; if you choose to ignore these errors, turn validation off with --validate=false' )


