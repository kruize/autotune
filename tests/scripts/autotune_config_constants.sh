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
##### Constants for autotuneconfig yaml tests #####
#

# get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"
pushd ${CURRENT_DIR}/.. >> setup.log

# Path to the directory containing yaml files
MANIFESTS="${PWD}/autotune_test_yamls/manifests"
autotune_config_testsuite="autotune_config_yaml"
testcase_matched=0
module="da"
path="${MANIFESTS}/${module}/${autotune_config_testsuite}"

# testcases for autotune config yaml 
autotune_config_tests=("layer_name" 
"layer_level"
"presence"
"layer_presence_query_datasource" 
"layer_presence_query" 
"layer_presence_query_key" 
"layer_presence_label_name" 
"layer_presence_labelvalue" 
"layer_presence" 
"tunable_name" 
"tunable_value_type" 
"tunable_upper_bound" 
"tunable_lower_bound" 
"tunable_query" 
"tunable_datasource_name" 
"tunable_sla_class" 
"tunables") 

# other tests for autotune config yaml 
other_tests=("other")

# tests for layer name
layer_name_testcases=("blank-layer-name" 
"no-layer-name" 
"no-layer-name-value" 
"null-layer-name" 
"numerical-layer-name" 
"valid-layer-name")

# tests for layer level
layer_level_testcases=("char-layer-level" 
"invalid-layer-level" 
"no-layer-level" 
"no-layer-level-value" 
"null-layer-level" 
"valid-layer-level")

# tests for presence
presence_testcases=("blank-presence" 
"invalid-presence" 
"no-presence" 
"no-presence-value" 
"null-presence" 
"numerical-presence" 
"valid-presence")

# tests for layer presence query datasource
layer_presence_query_datasource_testcases=("blank-layer-presence-query-datasource" 
"invalid-layer-presence-query-datasource" 
"no-layer-presence-query-datasource" 
"no-layer-presence-query-datasource-value" 
"null-layer-presence-query-datasource" 
"numerical-layer-presence-query-datasource" 
"valid-layer-presence-query-datasource")

# tests for layer presence query
layer_presence_query_testcases=("blank-layer-presence-query" 
"invalid-layer-presence-query" 
"no-layer-presence-query" 
"no-layer-presence-query-value" 
"null-layer-presence-query" 
"numerical-layer-presence-query" 
"valid-layer-presence-query")

# tests for layer presence query key
layer_presence_query_key_testcases=("blank-layer-presence-query-key" 
"invalid-layer-presence-query-key" 
"no-layer-presence-query-key" 
"no-layer-presence-query-key-value" 
"null-layer-presence-query-key" 
"numerical-layer-presence-query-key" 
"valid-layer-presence-query-key")

# tests for layer presence label name
layer_presence_label_name_testcases=("blank-layer-presence-label-name" 
"invalid-layer-presence-label-name" 
"no-layer-presence-label-name"
"no-layer-presence-label-name-value" 
"null-layer-presence-label-name" 
"numerical-layer-presence-label-name" 
"valid-layer-presence-label-name")

# tests for layer presence label value
layer_presence_labelvalue_testcases=("blank-layer-presence-labelvalue" 
"invalid-layer-presence-labelvalue" 
"no-layer-presence-labelvalue" 
"no-layer-presence-labelvalue-value" 
"null-layer-presence-labelvalue" 
"numerical-layer-presence-labelvalue" 
"valid-layer-presence-labelvalue")

# tests for layer presence 
layer_presence_testcases=("complete-layer-presence" 
"empty-layer-presence" 
"no-label-layer-presence" 
"no-layer-presence" 
"no-presence-layer-presence" 
"no-query-layer-presence" 
"only-label-layer-presence" 
"only-query-layer-presence" 
"valid-layer-presence")

# tests for tunable name
tunable_name_testcases=("blank-tunable-name" 
"invalid-tunable-name" 
"no-tunable-name-value" 
"null-tunable-name" 
"numerical-tunable-name" 
"valid-tunable-name")

# tests for tunable value type
tunable_value_type_testcases=("blank-tunable-value-type" 
"invalid-tunable-value-type" 
"no-tunable-value-type" 
"no-tunable-value-type-value" 
"null-tunable-value-type" 
"numerical-tunable-value-type" 
"valid-tunable-value-type")

# tests for tunable upper bound
tunable_upper_bound_testcases=("blank-tunable-upper-bound" 
"invalid-tunable-upper-bound" 
"no-tunable-upper-bound" 
"no-tunable-upper-bound-value" 
"null-tunable-upper-bound" 
"char-tunable-upper-bound" 
"valid-tunable-upper-bound")

# tests for tunable lower bound
tunable_lower_bound_testcases=("blank-tunable-lower-bound" 
"invalid-tunable-lower-bound" 
"no-tunable-lower-bound" 
"no-tunable-lower-bound-value" 
"null-tunable-lower-bound" 
"char-tunable-lower-bound" 
"valid-tunable-lower-bound")

# tests for tunable query
tunable_query_testcases=("blank-tunable-query" 
"invalid-tunable-query" 
"no-tunable-query" 
"no-tunable-query-value" 
"null-tunable-query" 
"numerical-tunable-query" 
"valid-tunable-query")

# tests for tunable datasource name
tunable_datasource_name_testcases=("blank-tunable-datasource-name" 
"invalid-tunable-datasource-name" 
"no-tunable-datasource-name" 
"no-tunable-datasource-name-value" 
"null-tunable-datasource-name" 
"numerical-tunable-datasource-name" 
"valid-tunable-datasource-name")

# tests for tunable sla class
tunable_sla_class_testcases=("blank-tunable-sla-class" 
"invalid-tunable-sla-class" 
"empty-tunable-sla-class" 
"no-tunable-sla-class" 
"no-tunable-sla-class-value" 
"null-tunable-sla-class" 
"numerical-tunable-sla-class" 
"valid-tunable-sla-class")

# tests for tunables
tunables_testcases=("interchanged-bound" 
"no-tunables" 
"no-tunables-queries" 
"no-tunables-sla-class" 
"valid-tunables")

# other test cases
other_testcases=("incomplete-autotuneconfig" )

# Expected autotune object for layer name
declare -A layer_name_autotune_objects
layer_name_autotune_objects=([blank-layer-name]='true' 
[no-layer-name]='false' 
[no-layer-name-value]='false' 
[null-layer-name]='false' 
[numerical-layer-name]='false' 
[valid-layer-name]='true')
# Expected log message for layer-name
declare -A layer_name_expected_log_msgs
layer_name_expected_log_msgs=([blank-layer-name]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: AutotuneConfig object name cannot be null or empty' 
[no-layer-name]='error: error validating "'${path}/${autotune_config_tests[0]}/no-layer-name.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layer_name" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-layer-name-value]='error: error validating "'${path}/${autotune_config_tests[0]}/no-layer-name-value.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layer_name" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-layer-name]='error: error validating "'${path}/${autotune_config_tests[0]}/null-layer-name.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layer_name" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-layer-name]='The AutotuneConfig "numerical-layer-name" is invalid: layer_name: Invalid value: "integer": layer_name in body must be of type string: "integer"' 
[valid-layer-name]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-name' )

# Expected autotune object for layer-level
declare -A layer_level_autotune_objects
layer_level_autotune_objects=([char-layer-level]='false' 
[invalid-layer-level]='true' 
[no-layer-level]='false' 
[no-layer-level-value]='false' 
[null-layer-level]='false' 
[char-layer-level]='false' 
[valid-layer-level]='true')
# Expected log message for layer-level
declare -A layer_level_expected_log_msgs
layer_level_expected_log_msgs=([char-layer-level]='error: error validating "'${path}/${autotune_config_tests[1]}/char-layer-level.yaml'": error validating data: ValidationError(AutotuneConfig.layer_level): invalid type for com.recommender.v1.AutotuneConfig.layer_level: got "string", expected "integer"; if you choose to ignore these errors, turn validation off with --validate=false' 
[invalid-layer-level]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Layer level must be a non-negative integer' 
[no-layer-level]='error: error validating "'${path}/${autotune_config_tests[1]}/no-layer-level.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layer_level" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-layer-level-value]='error: error validating "'${path}/${autotune_config_tests[1]}/no-layer-level-value.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layer_level" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false'
[null-layer-level]='error: error validating "'${path}/${autotune_config_tests[1]}/null-layer-level.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layer_level" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false' 
[valid-layer-level]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-level' )

# Expected autotune object for presence
declare -A presence_autotune_objects
presence_autotune_objects=([blank-presence]='true' 
[invalid-presence]='true' 
[no-presence]='false' 
[no-presence-value]='true' 
[null-presence]='true' 
[numerical-presence]='false' 
[valid-presence]='true')
#Expected log message for presence
declare -A presence_expected_log_msgs
presence_expected_log_msgs=([blank-presence]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel' 
[invalid-presence]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel' 
[no-presence]='error: error validating "'${path}/${autotune_config_tests[2]}/no-presence.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layerPresence" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-presence-value]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel' 
[null-presence]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel' 
[numerical-presence]='The AutotuneConfig "numerical-presence" is invalid: layerPresence.presence: Invalid value: "integer": layerPresence.presence in body must be of type string: "integer"' 
[valid-presence]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-presence')

# Expected autotune object for layer preseence query
declare -A layer_presence_query_datasource_autotune_objects
layer_presence_query_datasource_autotune_objects=([blank-layer-presence-query-datasource]='true' 
[invalid-layer-presence-query-datasource]='true' 
[no-layer-presence-query-datasource]='false' 
[no-layer-presence-query-datasource-value]='false' 
[null-layer-presence-query-datasource]='false' 
[numerical-layer-presence-query-datasource]='false' 
[valid-layer-presence-query-datasource]='true')
#Expected log message for layer preseence query
declare -A layer_presence_query_datasource_expected_log_msgs
layer_presence_query_datasource_expected_log_msgs=([blank-layer-presence-query-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel' 
[invalid-layer-presence-query-datasource]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel' 
[no-layer-presence-query-datasource]='error: error validating "'${path}/${autotune_config_tests[3]}/no-layer-presence-query-datasource.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource): invalid type for com.recommender.v1.AutotuneConfig.layerPresence.query.datasource: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-layer-presence-query-datasource-value]='error: error validating "'${path}/${autotune_config_tests[3]}/no-layer-presence-query-datasource-value.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "name" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-layer-presence-query-datasource]='error: error validating "'${path}/${autotune_config_tests[3]}/null-layer-presence-query-datasource.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "name" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-layer-presence-query-datasource]='The AutotuneConfig "numerical-layer-presence-query-datasource" is invalid: layerPresence.query.datasource.name: Invalid value: "integer": layerPresence.query.datasource.name in body must be of type string: "integer"' 
[valid-layer-presence-query-datasource]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-presence-query-datasource')

# Expected autotune object for layer presence query 
declare -A layer_presence_query_autotune_objects
layer_presence_query_autotune_objects=([blank-layer-presence-query]='true' 
[invalid-layer-presence-query]='true' 
[no-layer-presence-query]='false' 
[no-layer-presence-query-value]='false' 
[null-layer-presence-query]='false' 
[numerical-layer-presence-query]='false' 
[valid-layer-presence-query]='true')
#Expected log message for layer presence query
declare -A layer_presence_query_expected_log_msgs
layer_presence_query_expected_log_msgs=([blank-layer-presence-query]='validation from da' 
[invalid-layer-presence-query]='validation from da' 
[no-layer-presence-query]='error: error validating "'${path}/${autotune_config_tests[4]}/no-layer-presence-query.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "query" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-layer-presence-query-value]='error: error validating "'${path}/${autotune_config_tests[4]}/no-layer-presence-query-value.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "query" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-layer-presence-query]='error: error validating "'${path}/${autotune_config_tests[4]}/null-layer-presence-query.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "query" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-layer-presence-query]='The AutotuneConfig "numerical-layer-presence-query" is invalid: layerPresence.query.datasource.query: Invalid value: "integer": layerPresence.query.datasource.query in body must be of type string: "integer"' 
[valid-layer-presence-query]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-presence-query')

# Expected autotune object for layer presence query key
declare -A layer_presence_query_key_autotune_objects
layer_presence_query_key_autotune_objects=([blank-layer-presence-query-key]='true' 
[invalid-layer-presence-query-key]='true' 
[no-layer-presence-query-key]='false' 
[no-layer-presence-query-key-value]='false' 
[null-layer-presence-query-key]='false' 
[numerical-layer-presence-query-key]='false' 
[valid-layer-presence-query-key]='true')
# Expected autotune object for layer presence query key
declare -A layer_presence_query_key_expected_log_msgs
layer_presence_query_key_expected_log_msgs=([blank-layer-presence-query-key]='validation from da' 
[invalid-layer-presence-query-key]='validation from da' 
[no-layer-presence-query-key]='error: error validating "'${path}/${autotune_config_tests[5]}/no-layer-presence-query-key.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "key" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-layer-presence-query-key-value]='error: error validating "'${path}/${autotune_config_tests[5]}/no-layer-presence-query-key-value.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "key" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-layer-presence-query-key]='error: error validating "'${path}/${autotune_config_tests[5]}/null-layer-presence-query-key.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.query.datasource\[0\]): missing required field "key" in com.recommender.v1.AutotuneConfig.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-layer-presence-query-key]='The AutotuneConfig "numerical-layer-presence-query-key" is invalid: layerPresence.query.datasource.key: Invalid value: "integer": layerPresence.query.datasource.key in body must be of type string: "integer"' 
[valid-layer-presence-query-key]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-presence-query-key')

# Expected autotune object for layer presence label name
declare -A layer_presence_label_name_autotune_objects
layer_presence_label_name_autotune_objects=([blank-layer-presence-label-name]='true' 
[invalid-layer-presence-label-name]='true' 
[no-layer-presence-label-name]='false' 
[no-layer-presence-label-name-value]='false' 
[null-layer-presence-label-name]='false' 
[numerical-layer-presence-label-name]='false' 
[valid-layer-presence-label-name]='true')
# Expected log message for layer presence label name
declare -A layer_presence_label_name_expected_log_msgs
layer_presence_label_name_expected_log_msgs=([blank-layer-presence-label-name]='validation from da' 
[invalid-layer-presence-label-name]='validation from da' 
[no-layer-presence-label-name]='error: error validating "'${path}/${autotune_config_tests[6]}/no-layer-presence-label-name.yaml'": error validating data: ValidationError(AutotuneConfig.layerPresence.label): invalid type for com.recommender.v1.AutotuneConfig.layerPresence.label: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-layer-presence-label-name-value]='validation from crd' 
[null-layer-presence-label-name]='validation from crd' 
[numerical-layer-presence-label-name]='The AutotuneConfig "numerical-layer-presence-label-name" is invalid: layerPresence.label.name: Invalid value: "integer": layerPresence.label.name in body must be of type string: "integer"' 
[valid-layer-presence-label-name]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-presence-label-name')

# Expected autotune object for layer-presence-labelvalue
declare -A layer_presence_labelvalue_autotune_objects
layer_presence_labelvalue_autotune_objects=([blank-layer-presence-labelvalue]='true' 
[invalid-layer-presence-labelvalue]='true' 
[no-layer-presence-labelvalue]='false' 
[no-layer-presence-labelvalue-value]='false' 
[null-layer-presence-labelvalue]='false' 
[numerical-layer-presence-labelvalue]='false' 
[valid-layer-presence-labelvalue]='true')
# Expected log message for layer-presence-labelvalue
declare -A layer_presence_labelvalue_expected_log_msgs
layer_presence_labelvalue_expected_log_msgs=([blank-layer-presence-labelvalue]='Validation from da' 
[invalid-layer-presence-labelvalue]='validation from da' 
[no-layer-presence-labelvalue]='validation from crd' 
[no-layer-presence-labelvalue-value]='The AutotuneConfig "no-layer-presence-labelvalue-value" is invalid: layerPresence.label.value: Invalid value: "null": layerPresence.label.value in body must be of type string: "null"' 
[null-layer-presence-labelvalue]='The AutotuneConfig "null-layer-presence-labelvalue" is invalid: layerPresence.label.value: Invalid value: "null": layerPresence.label.value in body must be of type string: "null"' 
[numerical-layer-presence-labelvalue]='The AutotuneConfig "numerical-layer-presence-labelvalue" is invalid: layerPresence.label.value: Invalid value: "integer": layerPresence.label.value in body must be of type string: "integer"' 
[valid-layer-presence-labelvalue]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-presence-labelvalue' )

# Expected autotune object for layer presence
declare -A layer_presence_autotune_objects
layer_presence_autotune_objects=([complete-layer-presence]='true' 
[empty-layer-presence]='false' 
[no-label-layer-presence]='true' 
[no-layer-presence]='false' 
[no-presence-layer-presence]='true' 
[no-query-layer-presence]='true' 
[only-label-layer-presence]='true' 
[only-query-layer-presence]='true' 
[valid-layer-presence]='true')
# Expected log message for layer presence
declare -A layer_presence_expected_log_msgs
layer_presence_expected_log_msgs=([complete-layer-presence]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Both layerPresenceQuery and layerPresenceLabel cannot be set' 
[empty-layer-presence]='error: error validating "'${path}/${autotune_config_tests[8]}/empty-layer-presence.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layerPresence" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-label-layer-presence]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig no-label-layer-presence' 
[no-layer-presence]='error: error validating "'${path}/${autotune_config_tests[8]}/no-layer-presence.yaml'": error validating data: ValidationError(AutotuneConfig): missing required field "layerPresence" in com.recommender.v1.AutotuneConfig; if you choose to ignore these errors, turn validation off with --validate=false'  
[no-presence-layer-presence]='com.autotune.dependencyAnalyzer.exceptions.InvalidValueException: Both layerPresenceQuery and layerPresenceLabel cannot be set' 
[no-query-layer-presence]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig no-query-layer-presence' 
[only-label-layer-presence]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig only-label-layer-presence' 
[only-query-layer-presence]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig only-query-layer-presence' 
[valid-layer-presence]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-layer-presence' )

# Expected autotune object for tunable-name
declare -A tunable_name_autotune_objects
tunable_name_autotune_objects=([blank-tunable-name]='true' 
[invalid-tunable-name]='true' 
[no-tunable-name-value]='false' 
[null-tunable-name]='false' 
[numerical-tunable-name]='false' 
[valid-tunable-name]='true')
# Expected log message for tunable-name
declare -A tunable_name_expected_log_msgs
tunable_name_expected_log_msgs=([blank-tunable-name]='Validation from da' 
[invalid-tunable-name]='validation from da' 
[no-tunable-name-value]='error: error validating "'${path}/${autotune_config_tests[9]}/no-tunable-name-value.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "name" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-tunable-name]='error: error validating "'${path}/${autotune_config_tests[9]}/null-tunable-name.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "name" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-tunable-name]='The AutotuneConfig "numerical-tunable-name" is invalid: tunables.name: Invalid value: "integer": tunables.name in body must be of type string: "integer"' 
[valid-tunable-name]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunable-name' )

# Expected autotune object for tunable-value-type
declare -A tunable_value_type_autotune_objects
tunable_value_type_autotune_objects=([blank-tunable-value-type]='true' 
[invalid-tunable-value-type]='true' 
[no-tunable-value-type]='false' 
[no-tunable-value-type-value]='false' 
[null-tunable-value-type]='false' 
[numerical-tunable-value-type]='false' 
[valid-tunable-value-type]='true')
# Expected log message for tunable-value-type
declare -A tunable_value_type_expected_log_msgs
tunable_value_type_expected_log_msgs=([blank-tunable-value-type]='Validation from da' 
[invalid-tunable-value-type]='validation from da' 
[no-tunable-value-type]='error: error validating "'${path}/${autotune_config_tests[10]}/no-tunable-value-type.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "value_type" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-tunable-value-type-value]='error: error validating '${path}/${autotune_config_tests[10]}/no-tunable-value-type-value.yaml'": error validating data: \[ValidationError(AutotuneConfig.tunables\[0\]): missing required field "value_type" in com.recommender.v1.AutotuneConfig.tunables, ValidationError(AutotuneConfig): missing required field "layer_name" in com.recommender.v1.AutotuneConfig\]; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-tunable-value-type]='error: error validating "'${path}/${autotune_config_tests[10]}/null-tunable-value-type.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "value_type" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-tunable-value-type]='The AutotuneConfig "numerical-tunable-value-type" is invalid: tunables.value_type: Invalid value: "integer": tunables.value_type in body must be of type string: "integer"' 
[valid-tunable-value-type]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunable-value-type' )

# Expected autotune object for tunable upper bound
declare -A tunable_upper_bound_autotune_objects
tunable_upper_bound_autotune_objects=([blank-tunable-upper-bound]='true'
[invalid-tunable-upper-bound]='true' 
[no-tunable-upper-bound]='false' 
[no-tunable-upper-bound-value]='false' 
[null-tunable-upper-bound]='false' 
[char-tunable-upper-bound]='false' 
[valid-tunable-upper-bound]='true')
# Expected log message for tunable-upper-bound
declare -A tunable_upper_bound_expected_log_msgs
tunable_upper_bound_expected_log_msgs=([blank-tunable-upper-bound]='Validation from da' 
[invalid-tunable-upper-bound]='validation from da' 
[no-tunable-upper-bound]='error: error validating "'${path}/${autotune_config_tests[11]}/no-tunable-upper-bound.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "upper_bound" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-tunable-upper-bound-value]='error: error validating "'${path}/${autotune_config_tests[11]}/no-tunable-upper-bound-value.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "upper_bound" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-tunable-upper-bound]='error: error validating "'${path}/${autotune_config_tests[11]}/null-tunable-upper-bound.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "upper_bound" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[char-tunable-upper-bound]='validation from da' 
[valid-tunable-upper-bound]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunable-upper-bound' )

# Expected autotune object for tunable lower bound
declare -A tunable_lower_bound_autotune_objects
tunable_lower_bound_autotune_objects=([blank-tunable-lower-bound]='true' 
[invalid-tunable-lower-bound]='true' 
[no-tunable-lower-bound]='false' 
[no-tunable-lower-bound-value]='false' 
[null-tunable-lower-bound]='false' 
[char-tunable-lower-bound]='false' 
[valid-tunable-lower-bound]='true')
# Expected log message for tunable-lower-bound
declare -A tunable_lower_bound_expected_log_msgs
tunable_lower_bound_expected_log_msgs=([blank-tunable-lower-bound]='validation from da' 
[invalid-tunable-lower-bound]='validation from da' 
[no-tunable-lower-bound]='error: error validating "'${path}/${autotune_config_tests[12]}/no-tunable-lower-bound.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "lower_bound" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-tunable-lower-bound-value]='error: error validating "'${path}/${autotune_config_tests[12]}/no-tunable-lower-bound-value.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "lower_bound" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-tunable-lower-bound]='error: error validating "'${path}/${autotune_config_tests[12]}/null-tunable-lower-bound.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "lower_bound" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[char-tunable-lower-bound]='validation from da' 
[valid-tunable-lower-bound]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunable-lower-bound' )

# Expected autotune object for tunable query
declare -A tunable_query_autotune_objects
tunable_query_autotune_objects=([blank-tunable-query]='true' 
[invalid-tunable-query]='true' 
[no-tunable-query]='false' 
[no-tunable-query-value]='false' 
[null-tunable-query]='false' 
[numerical-tunable-query]='false' 
[valid-tunable-query]='true')
# Expected log message for tunable query
declare -A tunable_query_expected_log_msgs
tunable_query_expected_log_msgs=([blank-tunable-query]='validation from da' 
[invalid-tunable-query]='validation from da' 
[no-tunable-query]='error: error validating "'${path}/${autotune_config_tests[13]}/no-tunable-query.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\].queries.datasource\[0\]): missing required field "query" in com.recommender.v1.AutotuneConfig.tunables.queries.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-tunable-query-value]='error: error validating "'${path}/${autotune_config_tests[13]}/no-tunable-query-value.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\].queries.datasource\[0\]): missing required field "query" in com.recommender.v1.AutotuneConfig.tunables.queries.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-tunable-query]='error: error validating "'${path}/${autotune_config_tests[13]}/null-tunable-query.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\].queries.datasource\[0\]): missing required field "query" in com.recommender.v1.AutotuneConfig.tunables.queries.datasource; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-tunable-query]='The AutotuneConfig "numerical-tunable-query" is invalid: tunables.queries.datasource.query: Invalid value: "integer": tunables.queries.datasource.query in body must be of type string: "integer"' 
[valid-tunable-query]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunable-query' )

# Expected autotune object for tunable datasource name
declare -A tunable_datasource_name_autotune_objects
tunable_datasource_name_autotune_objects=([blank-tunable-datasource-name]='true' 
[invalid-tunable-datasource-name]='true' 
[no-tunable-datasource-name]='true' 
[no-tunable-datasource-name-value]='true' 
[null-tunable-datasource-name]='true' 
[numerical-tunable-datasource-name]='false' 
[valid-tunable-datasource-name]='true')
# Expected log message for tunable-query
declare -A tunable_datasource_name_expected_log_msgs
tunable_datasource_name_expected_log_msgs=([blank-tunable-datasource-name]='error from da' 
[invalid-tunable-datasource-name]='error from da' 
[no-tunable-datasource-name]='validation from da' 
[no-tunable-datasource-name-value]='validation from da' 
[null-tunable-datasource-name]='validation from da' 
[numerical-tunable-datasource-name]='The AutotuneConfig "numerical-tunable-datasource-name" is invalid: tunables.queries.datasource.name: Invalid value: "integer": tunables.queries.datasource.name in body must be of type string: "integer"' 
[valid-tunable-datasource-name]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunable-datasource-name' )

# Expected autotune object for sla class
declare -A tunable_sla_class_autotune_objects
tunable_sla_class_autotune_objects=([blank-tunable-sla-class]='true' 
[invalid-tunable-sla-class]='true' 
[empty-tunable-sla-class]='false' 
[no-sla-tunable-class]='true' 
[no-tunable-sla-class-value]='false' 
[null-tunable-sla-class]='false' 
[numerical-tunable-sla-value]='false' 
[valid-tunable-sla-class]='true')
# Expected log message for sla class
declare -A tunable_sla_class_expected_log_msgs
tunable_sla_class_expected_log_msgs=([blank-tunable-sla-class]='Expecting Error message from DA' 
[invalid-tunable-sla-class]='Expecting Error message from DA' 
[empty-tunable-sla-class]='error message from crd' 
[no-tunable-sla-class]='error: error validating "'${path}/${autotune_config_tests[15]}/no-tunable-sla-class.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "sla_class" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false' 
[no-tunable-sla-class-value]='error: error validating "'${path}/${autotune_config_tests[15]}/no-tunable-sla-class-value.yaml'": error validating data: \[ValidationError(AutotuneConfig.tunables\[0\].sla_class): unknown object type "nil" in AutotuneConfig.tunables\[0\].sla_class\[0\], ValidationError(AutotuneConfig.tunables\[0\].sla_class): unknown object type "nil" in AutotuneConfig.tunables\[0\].sla_class\[1\], ValidationError(AutotuneConfig.tunables\[0\].sla_class): unknown object type "nil" in AutotuneConfig.tunables\[0\].sla_class\[2\]\]; if you choose to ignore these errors, turn validation off with --validate=false' 
[null-tunable-sla-class]='error: error validating "'${path}/${autotune_config_tests[15]}/null-tunable-sla-class.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\].sla_class): unknown object type "nil" in AutotuneConfig.tunables\[0\].sla_class\[0\]; if you choose to ignore these errors, turn validation off with --validate=false' 
[numerical-tunable-sla-class]='The AutotuneConfig "numerical-tunable-sla-class" is invalid: tunables.sla_class: Invalid value: "integer": tunables.sla_class in body must be of type string: "integer"' 
[valid-tunable-sla-class]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunable-sla-class')

# Expected autotune object for tunable-query
declare -A tunables_autotune_objects
tunables_autotune_objects=([interchanged-bound]='true' 
[no-tunables]='true' 
[no-tunables-queries]='true' 
[no-tunables-sla-class]='false' 
[valid-tunables]='true')
# Expected log message for tunable-query
declare -A tunables_expected_log_msgs
tunables_expected_log_msgs=([interchanged-bound]='validation from da' 
[no-tunables]='Expected validation from da' 
[no-tunables-queries]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig no-tunables-queries' 
[no-tunables-sla-class]='error: error validating "'${path}/${autotune_config_tests[16]}/no-tunables-sla-class.yaml'": error validating data: ValidationError(AutotuneConfig.tunables\[0\]): missing required field "sla_class" in com.recommender.v1.AutotuneConfig.tunables; if you choose to ignore these errors, turn validation off with --validate=false'
 [valid-tunables]='com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment - Added autotuneconfig valid-tunables' )

# Expected autotune object for other test cases
declare -A other_autotune_objects
other_autotune_objects=([incomplete-autotuneconfig]='false')
# Expected log message for other test cases
declare -A other_expected_log_msgs
other_expected_log_msgs=([incomplete-autotuneconfig]='error: error validating "'${path}/${other_tests[0]}/incomplete-autotuneconfig.yaml'": error validating data: \[ValidationError(AutotuneConfig): missing required field "layer_name" in com.recommender.v1.AutotuneConfig, ValidationError(AutotuneConfig): missing required field "layer_level" in com.recommender.v1.AutotuneConfig, ValidationError(AutotuneConfig): missing required field "layerPresence" in com.recommender.v1.AutotuneConfig\]; if you choose to ignore these errors, turn validation off with --validate=false')

