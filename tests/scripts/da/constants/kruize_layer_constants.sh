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
kruize_layer_testsuite="kruize_layer_yaml"
testcase_matched=0
module="da"
yaml_path="${MANIFESTS}/${module}/${kruize_layer_testsuite}"
kruize_layer_obj_create_msg='[AutotuneDeployment.java([0-9]*)]-Added autotuneconfig'
exception="com.autotune.analyzer.exceptions.InvalidValueException:"
invalid_bound_exception='com.autotune.analyzer.exceptions.InvalidBoundsException'

# testcases for autotune config yaml
kruize_layer_tests=("layer_name"
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
"step"
"tunable_query"
"tunable_datasource_name"
"tunable_slo_class"
"tunables")

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
"zero-tunable-upper-bound"
"integer-tunable-upper-bound"
"valid-tunable-upper-bound")

# tests for tunable lower bound
tunable_lower_bound_testcases=("blank-tunable-lower-bound"
"invalid-tunable-lower-bound"
"no-tunable-lower-bound"
"no-tunable-lower-bound-value"
"null-tunable-lower-bound"
"char-tunable-lower-bound"
"zero-tunable-lower-bound"
"integer-tunable-lower-bound"
"valid-tunable-lower-bound")

# tests for step
step_testcases=("invalid-step"
"no-step-value"
"null-step"
"char-step"
"zero-step"
"valid-step")

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

# tests for tunable slo class
tunable_slo_class_testcases=("blank-tunable-slo-class"
"invalid-tunable-slo-class"
"empty-tunable-slo-class"
"no-tunable-slo-class"
"no-tunable-slo-class-value"
"null-tunable-slo-class"
"numerical-tunable-slo-class"
"valid-tunable-slo-class")

# tests for tunables
tunables_testcases=("interchanged-bound"
"no-tunables"
"no-tunables-queries"
"no-tunables-slo-class"
"valid-tunables")

# other test cases
autotuneconfig_other_testcases=("incomplete-autotuneconfig")

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
layer_name_yaml_path="${yaml_path}/${kruize_layer_tests[0]}"
layer_name_kubectl_error=': error validating data: ValidationError(KruizeLayer): missing required field "layer_name" in com.recommender.v1.KruizeLayer; if you choose to ignore these errors, turn validation off with --validate=false'
layer_name_expected_log_msgs=([blank-layer-name]=''${exception}' KruizeLayer object name cannot be null or empty'
[no-layer-name]='error: error validating "'${layer_name_yaml_path}/no-layer-name.yaml'"'${layer_name_kubectl_error}''
[no-layer-name-value]='error: error validating "'${layer_name_yaml_path}/no-layer-name-value.yaml'"'${layer_name_kubectl_error}''
[null-layer-name]='error: error validating "'${layer_name_yaml_path}/null-layer-name.yaml'"'${layer_name_kubectl_error}''
[numerical-layer-name]='The KruizeLayer "numerical-layer-name" is invalid: layer_name: Invalid value: "integer": layer_name in body must be of type string: "integer"'
[valid-layer-name]=''${kruize_layer_obj_create_msg}' valid-layer-name')

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
layer_level_yaml_path="${yaml_path}/${kruize_layer_tests[1]}"
layer_level_kubectl_error=': error validating data: ValidationError(KruizeLayer): missing required field "layer_level" in com.recommender.v1.KruizeLayer; if you choose to ignore these errors, turn validation off with --validate=false'
layer_level_expected_log_msgs=([char-layer-level]='error: error validating "'${layer_level_yaml_path}/char-layer-level.yaml'": error validating data: ValidationError(KruizeLayer.layer_level): invalid type for com.recommender.v1.KruizeLayer.layer_level: got "string", expected "integer"; if you choose to ignore these errors, turn validation off with --validate=false'
[invalid-layer-level]=''${exception}' Layer level must be a non-negative integer'
[no-layer-level]='error: error validating "'${layer_level_yaml_path}/no-layer-level.yaml'"'${layer_level_kubectl_error}''
[no-layer-level-value]='error: error validating "'${layer_level_yaml_path}/no-layer-level-value.yaml'"'${layer_level_kubectl_error}''
[null-layer-level]='error: error validating "'${layer_level_yaml_path}/null-layer-level.yaml'"'${layer_level_kubectl_error}''
[valid-layer-level]=''${kruize_layer_obj_create_msg}' valid-layer-level')

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
presence_yaml_path="${yaml_path}/${kruize_layer_tests[2]}"
presence_exception='Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel'
presence_expected_log_msgs=([blank-presence]=''${exception}' '${presence_exception}''
[invalid-presence]=''${exception}' '${presence_exception}''
[no-presence]='error: error validating "'${presence_yaml_path}/no-presence.yaml'": error validating data: ValidationError(KruizeLayer): missing required field "layerPresence" in com.recommender.v1.KruizeLayer; if you choose to ignore these errors, turn validation off with --validate=false'
[no-presence-value]=''${exception}' '${presence_exception}''
[null-presence]=''${exception}' '${presence_exception}''
[numerical-presence]='The KruizeLayer "numerical-presence" is invalid: layerPresence.presence: Invalid value: "integer": layerPresence.presence in body must be of type string: "integer"'
[valid-presence]=''${kruize_layer_obj_create_msg}' valid-presence')

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
layer_presence_query_ds_yaml_path="${yaml_path}/${kruize_layer_tests[3]}"
layer_presence_query_ds_kubectl_error=': error validating data: ValidationError(KruizeLayer.layerPresence.query.datasource\[0\]): missing required field "name" in com.recommender.v1.KruizeLayer.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
layer_presence_query_datasource_expected_log_msgs=([blank-layer-presence-query-datasource]=''${exception}' '${presence_exception}''
[invalid-layer-presence-query-datasource]=''${exception}' '${presence_exception}''
[no-layer-presence-query-datasource]='error: error validating "'${layer_presence_query_ds_yaml_path}/no-layer-presence-query-datasource.yaml'": error validating data: ValidationError(KruizeLayer.layerPresence.query.datasource): invalid type for com.recommender.v1.KruizeLayer.layerPresence.query.datasource: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false'
[no-layer-presence-query-datasource-value]='error: error validating "'${layer_presence_query_ds_yaml_path}/no-layer-presence-query-datasource-value.yaml'"'${layer_presence_query_ds_kubectl_error}''
[null-layer-presence-query-datasource]='error: error validating "'${layer_presence_query_ds_yaml_path}/null-layer-presence-query-datasource.yaml'"'${layer_presence_query_ds_kubectl_error}''
[numerical-layer-presence-query-datasource]='The KruizeLayer "numerical-layer-presence-query-datasource" is invalid: layerPresence.query.datasource.name: Invalid value: "integer": layerPresence.query.datasource.name in body must be of type string: "integer"'
[valid-layer-presence-query-datasource]=''${kruize_layer_obj_create_msg}' valid-layer-presence-query-datasource')

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
layer_presence_query_yaml_path="${yaml_path}/${kruize_layer_tests[4]}"
layer_presence_query_kubectl_error=': error validating data: ValidationError(KruizeLayer.layerPresence.query.datasource\[0\]): missing required field "query" in com.recommender.v1.KruizeLayer.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
layer_presence_query_expected_log_msgs=([blank-layer-presence-query]='com.autotune.analyzer.deployment.AutotuneDeployment - Could not get the applications for the layer blank-layer-presence-query'
[invalid-layer-presence-query]='validation from da'
[no-layer-presence-query]='error: error validating "'${layer_presence_query_yaml_path}/no-layer-presence-query.yaml'"'${layer_presence_query_kubectl_error}''
[no-layer-presence-query-value]='error: error validating "'${layer_presence_query_yaml_path}/no-layer-presence-query-value.yaml'"'${layer_presence_query_kubectl_error}''
[null-layer-presence-query]='error: error validating "'${layer_presence_query_yaml_path}/null-layer-presence-query.yaml'"'${layer_presence_query_kubectl_error}''
[numerical-layer-presence-query]='The KruizeLayer "numerical-layer-presence-query" is invalid: layerPresence.query.datasource.query: Invalid value: "integer": layerPresence.query.datasource.query in body must be of type string: "integer"'
[valid-layer-presence-query]=''${kruize_layer_obj_create_msg}' valid-layer-presence-query')

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
layer_presence_query_key_yaml_path="${yaml_path}/${kruize_layer_tests[5]}"
layer_presence_query_key_kubectl_error=': error validating data: ValidationError(KruizeLayer.layerPresence.query.datasource\[0\]): missing required field "key" in com.recommender.v1.KruizeLayer.layerPresence.query.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
layer_presence_query_key_expected_log_msgs=([blank-layer-presence-query-key]='validation from da'
[invalid-layer-presence-query-key]='validation from da'
[no-layer-presence-query-key]='error: error validating "'${layer_presence_query_key_yaml_path}/no-layer-presence-query-key.yaml'"'${layer_presence_query_key_kubectl_error}''
[no-layer-presence-query-key-value]='error: error validating "'${layer_presence_query_key_yaml_path}/no-layer-presence-query-key-value.yaml'"'${layer_presence_query_key_kubectl_error}''
[null-layer-presence-query-key]='error: error validating "'${layer_presence_query_key_yaml_path}/null-layer-presence-query-key.yaml'"'${layer_presence_query_key_kubectl_error}''
[numerical-layer-presence-query-key]='The KruizeLayer "numerical-layer-presence-query-key" is invalid: layerPresence.query.datasource.key: Invalid value: "integer": layerPresence.query.datasource.key in body must be of type string: "integer"'
[valid-layer-presence-query-key]=''${kruize_layer_obj_create_msg}' valid-layer-presence-query-key')

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
[no-layer-presence-label-name]='error: error validating "'${yaml_path}/${kruize_layer_tests[6]}/no-layer-presence-label-name.yaml'": error validating data: ValidationError(KruizeLayer.layerPresence.label): invalid type for com.recommender.v1.KruizeLayer.layerPresence.label: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false'
[no-layer-presence-label-name-value]='validation from crd'
[null-layer-presence-label-name]='validation from crd'
[numerical-layer-presence-label-name]='The KruizeLayer "numerical-layer-presence-label-name" is invalid: layerPresence.label.name: Invalid value: "integer": layerPresence.label.name in body must be of type string: "integer"'
[valid-layer-presence-label-name]=''${kruize_layer_obj_create_msg}' valid-layer-presence-label-name')

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
layer_presence_labelvalue_error=': layerPresence.label.value in body must be of type string:'
layer_presence_labelvalue_expected_log_msgs=([blank-layer-presence-labelvalue]='Validation from da'
[invalid-layer-presence-labelvalue]='validation from da'
[no-layer-presence-labelvalue]='validation from crd'
[no-layer-presence-labelvalue-value]='The KruizeLayer "no-layer-presence-labelvalue-value" is invalid: layerPresence.label.value: Invalid value: "null"'${layer_presence_labelvalue_error}' "null"'
[null-layer-presence-labelvalue]='The KruizeLayer "null-layer-presence-labelvalue" is invalid: layerPresence.label.value: Invalid value: "null"'${layer_presence_labelvalue_error}' "null"'
[numerical-layer-presence-labelvalue]='The KruizeLayer "numerical-layer-presence-labelvalue" is invalid: layerPresence.label.value: Invalid value: "integer"'${layer_presence_labelvalue_error}' "integer"'
[valid-layer-presence-labelvalue]=''${kruize_layer_obj_create_msg}' valid-layer-presence-labelvalue')

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
layer_presence_yaml_path="${yaml_path}/${kruize_layer_tests[8]}"
layer_presence_kubectl_error=': error validating data: ValidationError(KruizeLayer): missing required field "layerPresence" in com.recommender.v1.KruizeLayer; if you choose to ignore these errors, turn validation off with --validate=false'
layer_presence_expected_log_msgs=([complete-layer-presence]=''${exception}' Both layerPresenceQuery and layerPresenceLabel cannot be set'
[empty-layer-presence]='error: error validating "'${layer_presence_yaml_path}/empty-layer-presence.yaml'"'${layer_presence_kubectl_error}''
[no-label-layer-presence]=''${kruize_layer_obj_create_msg}' no-label-layer-presence'
[no-layer-presence]='error: error validating "'${layer_presence_yaml_path}/no-layer-presence.yaml'"'${layer_presence_kubectl_error}''
[no-presence-layer-presence]=''${exception}' Both layerPresenceQuery and layerPresenceLabel cannot be set'
[no-query-layer-presence]=''${kruize_layer_obj_create_msg}' no-query-layer-presence'
[only-label-layer-presence]=''${kruize_layer_obj_create_msg}' only-label-layer-presence'
[only-query-layer-presence]=''${kruize_layer_obj_create_msg}' only-query-layer-presence'
[valid-layer-presence]=''${kruize_layer_obj_create_msg}' valid-layer-presence')

# Expected autotune object for tunable-name
declare -A tunable_name_autotune_objects
tunable_name_autotune_objects=([blank-tunable-name]='true'
[no-tunable-name-value]='false'
[null-tunable-name]='false'
[numerical-tunable-name]='false'
[valid-tunable-name]='true')

# Expected log message for tunable-name
declare -A tunable_name_expected_log_msgs
tunable_name_yaml_path="${yaml_path}/${kruize_layer_tests[9]}"
tunable_name_kubectl_error=': error validating data: ValidationError(KruizeLayer.tunables\[0\]): missing required field "name" in com.recommender.v1.KruizeLayer.tunables; if you choose to ignore these errors, turn validation off with --validate=false'
tunable_name_expected_log_msgs=([blank-tunable-name]=''${exception}' Tunable name cannot be empty'
[no-tunable-name-value]='error: error validating "'${tunable_name_yaml_path}/no-tunable-name-value.yaml'"'${tunable_name_kubectl_error}''
[null-tunable-name]='error: error validating "'${tunable_name_yaml_path}/null-tunable-name.yaml'"'${tunable_name_kubectl_error}''
[numerical-tunable-name]='The KruizeLayer "numerical-tunable-name" is invalid: tunables.name: Invalid value: "integer": tunables.name in body must be of type string: "integer"'
[valid-tunable-name]=''${kruize_layer_obj_create_msg}' valid-tunable-name')

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
tunable_value_yaml_path="${yaml_path}/${kruize_layer_tests[10]}"
tunable_value_kubectl_error=': error validating data: ValidationError(KruizeLayer.tunables\[0\]): missing required field "value_type" in com.recommender.v1.KruizeLayer.tunables; if you choose to ignore these errors, turn validation off with --validate=false'
tunable_value_type_expected_log_msgs=([blank-tunable-value-type]='Validation from da'
[invalid-tunable-value-type]='validation from da'
[no-tunable-value-type]='error: error validating "'${tunable_value_yaml_path}/no-tunable-value-type.yaml'"'${tunable_value_kubectl_error}''
[no-tunable-value-type-value]='error: error validating "'${tunable_value_yaml_path}/no-tunable-value-type-value.yaml'": error validating data: \[ValidationError(KruizeLayer.tunables\[0\]): missing required field "value_type" in com.recommender.v1.KruizeLayer.tunables, ValidationError(KruizeLayer): missing required field "layer_name" in com.recommender.v1.KruizeLayer\]; if you choose to ignore these errors, turn validation off with --validate=false'
[null-tunable-value-type]='error: error validating "'${tunable_value_yaml_path}/null-tunable-value-type.yaml'"'${tunable_value_kubectl_error}''
[numerical-tunable-value-type]='The KruizeLayer "numerical-tunable-value-type" is invalid: tunables.value_type: Invalid value: "integer": tunables.value_type in body must be of type string: "integer"'
[valid-tunable-value-type]=''${kruize_layer_obj_create_msg}' valid-tunable-value-type')

# Expected autotune object for tunable upper bound
declare -A tunable_upper_bound_autotune_objects
tunable_upper_bound_autotune_objects=([blank-tunable-upper-bound]='true'
[invalid-tunable-upper-bound]='false'
[no-tunable-upper-bound]='true'
[no-tunable-upper-bound-value]='true'
[null-tunable-upper-bound]='true'
[char-tunable-upper-bound]='true'
[zero-tunable-upper-bound]='true'
[integer-tunable-upper-bound]='false'
[valid-tunable-upper-bound]='true')


# Expected log message for tunable-upper-bound
declare -A tunable_upper_bound_expected_log_msgs
memory_tuneable="memoryRequest"
tunable_upper_bound_yaml_path="${yaml_path}/${kruize_layer_tests[11]}"
tunable_upper_bound_kubectl_error=': error validating data: ValidationError(KruizeLayer.tunables\[0\]): missing required field "upper_bound" in com.recommender.v1.KruizeLayer.tunables; if you choose to ignore these errors, turn validation off with --validate=false'
invalid_upper_bound_error='The KruizeLayer "invalid-tunable-upper-bound" is invalid:'
tunable_upper_bound_expected_log_msgs=([blank-tunable-upper-bound]=''${invalid_bound_exception}': Bounds value(s) are empty'
[invalid-tunable-upper-bound]=''${invalid_upper_bound_error}''
[no-tunable-upper-bound]=''${invalid_bound_exception}': Bounds value(s) cannot be null'
[integer-tunable-upper-bound]='The KruizeLayer "integer-tunable-upper-bound" is invalid:'
[no-tunable-upper-bound-value]=''${invalid_bound_exception}': Bounds value(s) cannot be null'
[null-tunable-upper-bound]=''${invalid_bound_exception}': Bounds value(s) cannot be null'
[char-tunable-upper-bound]=''${invalid_bound_exception}': Error: Upper bound value is not a valid number'
[zero-tunable-upper-bound]=''${invalid_bound_exception}': ERROR: Tunable: '${memory_tuneable}' has invalid bounds;'
[valid-tunable-upper-bound]='Added autotuneconfig valid-tunable-upper-bound')


# Expected autotune object for tunable lower bound
declare -A tunable_lower_bound_autotune_objects
invalid_lower_bound_error='The KruizeLayer "invalid-tunable-lower-bound" is invalid:'
tunable_lower_bound_autotune_objects=([blank-tunable-lower-bound]='true'
[invalid-tunable-lower-bound]='false'
[no-tunable-lower-bound]='true'
[no-tunable-lower-bound-value]='true'
[null-tunable-lower-bound]='true'
[char-tunable-lower-bound]='true'
[zero-tunable-lower-bound]='true'
[integer-tunable-lower-bound]='false'
[zero-tunable-nonstring-lower-bound]='true'
[valid-tunable-lower-bound]='true')

# Expected log message for tunable-lower-bound
declare -A tunable_lower_bound_expected_log_msgs
memory_tuneable="memoryRequest"
tunable_lower_bound_yaml_path="${yaml_path}/${kruize_layer_tests[12]}"
tunable_lower_bound_kubectl_error=': error validating data: ValidationError(KruizeLayer.tunables\[0\]): missing required field "lower_bound" in com.recommender.v1.KruizeLayer.tunables; if you choose to ignore these errors, turn validation off with --validate=false'
invalid_lower_bound_error='The KruizeLayer "invalid-tunable-lower-bound" is invalid:'
tunable_lower_bound_expected_log_msgs=([blank-tunable-lower-bound]=''${invalid_bound_exception}': Bounds value(s) are empty'
[invalid-tunable-lower-bound]=''${invalid_lower_bound_error}''
[no-tunable-lower-bound]=''${invalid_bound_exception}': Bounds value(s) cannot be null'
[no-tunable-lower-bound-value]=''${invalid_bound_exception}': Bounds value(s) cannot be null'
[null-tunable-lower-bound]=''${invalid_bound_exception}': Bounds value(s) cannot be null'
[char-tunable-lower-bound]=''${invalid_bound_exception}': Error: Lower bound value is not a valid number'
[zero-tunable-lower-bound]='Test yet to be decided'
[integer-tunable-lower-bound]='The KruizeLayer "integer-tunable-lower-bound" is invalid:'
[valid-tunable-lower-bound]='Added autotuneconfig valid-tunable-lower-bound')

# Expected autotune object for step
declare -A step_autotune_objects
step_autotune_objects=([invalid-step]='true'
[no-step-value]='true'
[null-step]='true'
[char-step]='false'
[zero-step]='true'
[valid-step]='true')

# Expected log message for tunable-lower-bound
declare -A step_expected_log_msgs
step_yaml_path="${yaml_path}/${kruize_layer_tests[13]}"
step_expected_log_msgs=([invalid-step]=''${invalid_bound_exception}''
[no-step-value]='validation from da'
[null-step]='validation from da'
[char-step]='error: error validating "'${step_yaml_path}/char-step.yaml'": error validating data: \[ValidationError(KruizeLayer.tunables\[0\].step): invalid type for com.recommender.v1.KruizeLayer.tunables.step: got "string", expected "number", ValidationError(KruizeLayer.tunables\[1\].step): invalid type for com.recommender.v1.KruizeLayer.tunables.step: got "string", expected "number"\]; if you choose to ignore these errors, turn validation off with --validate=false'
[zero-step]=''${exception}' Tunable step cannot be 0'
[valid-step]=''${kruize_layer_obj_create_msg}' valid-step')

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
tunable_query_yaml_path="${yaml_path}/${kruize_layer_tests[14]}"
tunable_query_kubectl_error=': error validating data: ValidationError(KruizeLayer.tunables\[0\].queries.datasource\[0\]): missing required field "query" in com.recommender.v1.KruizeLayer.tunables.queries.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
tunable_query_expected_log_msgs=([blank-tunable-query]='validation from da'
[invalid-tunable-query]='validation from da'
[no-tunable-query]='error: error validating "'${tunable_query_yaml_path}/no-tunable-query.yaml'"'${tunable_query_kubectl_error}''
[no-tunable-query-value]='error: error validating "'${tunable_query_yaml_path}/no-tunable-query-value.yaml'"'${tunable_query_kubectl_error}''
[null-tunable-query]='error: error validating "'${tunable_query_yaml_path}/null-tunable-query.yaml'"'${tunable_query_kubectl_error}''
[numerical-tunable-query]='The KruizeLayer "numerical-tunable-query" is invalid: tunables.queries.datasource.query: Invalid value: "integer": tunables.queries.datasource.query in body must be of type string: "integer"'
[valid-tunable-query]=''${kruize_layer_obj_create_msg}' valid-tunable-query' )

# Expected autotune object for tunable datasource name
declare -A tunable_datasource_name_autotune_objects
tunable_datasource_name_autotune_objects=([blank-tunable-datasource-name]='true'
[invalid-tunable-datasource-name]='true'
[no-tunable-datasource-name]='false'
[no-tunable-datasource-name-value]='false'
[null-tunable-datasource-name]='false'
[numerical-tunable-datasource-name]='false'
[valid-tunable-datasource-name]='true')

# Expected log message for tunable datasource name
declare -A tunable_datasource_name_expected_log_msgs
tunable_datasource_name_yaml_path="${yaml_path}/${kruize_layer_tests[15]}"
tunable_datasource_name_kubectl_error=': error validating data: ValidationError(KruizeLayer.tunables\[0\].queries.datasource\[0\]): missing required field "name" in com.recommender.v1.KruizeLayer.tunables.queries.datasource; if you choose to ignore these errors, turn validation off with --validate=false'
tunable_datasource_name_expected_log_msgs=([blank-tunable-datasource-name]='validation form da'
[invalid-tunable-datasource-name]='error from da'
[no-tunable-datasource-name]='error: error validating "'${tunable_datasource_name_yaml_path}/no-tunable-datasource-name.yaml'": error validating data: ValidationError(KruizeLayer.tunables\[0\].queries.datasource): invalid type for com.recommender.v1.KruizeLayer.tunables.queries.datasource: got "map", expected "array"; if you choose to ignore these errors, turn validation off with --validate=false'
[no-tunable-datasource-name-value]='error: error validating "'${tunable_datasource_name_yaml_path}/no-tunable-datasource-name-value.yaml'"'${tunable_datasource_name_kubectl_error}''
[null-tunable-datasource-name]='error: error validating "'${tunable_datasource_name_yaml_path}/null-tunable-datasource-name.yaml'"'${tunable_datasource_name_kubectl_error}''
[numerical-tunable-datasource-name]='The KruizeLayer "numerical-tunable-datasource-name" is invalid: tunables.queries.datasource.name: Invalid value: "integer": tunables.queries.datasource.name in body must be of type string: "integer"'
[valid-tunable-datasource-name]=''${kruize_layer_obj_create_msg}' valid-tunable-datasource-name' )

# Expected autotune object for slo class
declare -A tunable_slo_class_autotune_objects
tunable_slo_class_autotune_objects=([blank-tunable-slo-class]='true'
[invalid-tunable-slo-class]='true'
[empty-tunable-slo-class]='false'
[no-slo-tunable-class]='false'
[no-tunable-slo-class-value]='false'
[null-tunable-slo-class]='false'
[numerical-tunable-slo-value]='false'
[valid-tunable-slo-class]='true')

# Expected log message for slo class
declare -A tunable_slo_class_expected_log_msgs
tunable_slo_class_yaml_path="${yaml_path}/${kruize_layer_tests[16]}"
tunable_slo_class_kubectl_error=': error validating data: ValidationError(KruizeLayer.tunables\[0\]): missing required field "slo_class" in com.recommender.v1.KruizeLayer.tunables; if you choose to ignore these errors, turn validation off with --validate=false'
validation_error='ValidationError(KruizeLayer.tunables\[0\].slo_class): unknown object type "nil" in KruizeLayer.tunables\[0\]'
tunable_slo_class_expected_log_msgs=([blank-tunable-slo-class]=''${exception}' Invalid slo_class for tunable memoryRequest'
[invalid-tunable-slo-class]=''${exception}' Invalid slo_class for tunable memoryRequest'
[empty-tunable-slo-class]='error: error validating "'${tunable_slo_class_yaml_path}/empty-tunable-slo-class.yaml'"'${tunable_slo_class_kubectl_error}''
[no-tunable-slo-class]='error: error validating "'${tunable_slo_class_yaml_path}/no-tunable-slo-class.yaml'"'${tunable_slo_class_kubectl_error}''
[no-tunable-slo-class-value]='error: error validating "'${tunable_slo_class_yaml_path}/no-tunable-slo-class-value.yaml'": error validating data: \['${validation_error}'.slo_class\[0\], '${validation_error}'.slo_class\[1\], '${validation_error}'.slo_class\[2\]\]; if you choose to ignore these errors, turn validation off with --validate=false'
[null-tunable-slo-class]='error: error validating "'${tunable_slo_class_yaml_path}/null-tunable-slo-class.yaml'": error validating data: '${validation_error}'.slo_class\[0\]; if you choose to ignore these errors, turn validation off with --validate=false'
[numerical-tunable-slo-class]='The KruizeLayer "numerical-tunable-slo-class" is invalid: tunables.slo_class: Invalid value: "integer": tunables.slo_class in body must be of type string: "integer"'
[valid-tunable-slo-class]=''${kruize_layer_obj_create_msg}' valid-tunable-slo-class')

# Expected autotune object for tunables
declare -A tunables_autotune_objects
tunables_autotune_objects=([interchanged-bound]='true'
[no-tunables]='false'
[no-tunables-queries]='true'
[no-tunables-slo-class]='false'
[valid-tunables]='true')

# Expected log message for tunables
declare -A tunables_expected_log_msgs
tunables_yaml_path="${yaml_path}/${kruize_layer_tests[17]}"
tunables_expected_log_msgs=([interchanged-bound]=''${invalid_bound_exception}''
[no-tunables]='error: error validating "'${tunables_yaml_path}/no-tunables.yaml'": error validating data: ValidationError(KruizeLayer): missing required field "tunables" in com.recommender.v1.KruizeLayer; if you choose to ignore these errors, turn validation off with --validate=false'
[no-tunables-queries]=''${kruize_layer_obj_create_msg}' no-tunables-queries'
[no-tunables-slo-class]='error: error validating "'${tunables_yaml_path}/no-tunables-slo-class.yaml'": error validating data: ValidationError(KruizeLayer.tunables\[0\]): missing required field "slo_class" in com.recommender.v1.KruizeLayer.tunables; if you choose to ignore these errors, turn validation off with --validate=false'
 [valid-tunables]=''${kruize_layer_obj_create_msg}' valid-tunables' )

# Expected autotune object for other test cases
declare -A autotuneconfig_other_autotune_objects
autotuneconfig_other_autotune_objects=([incomplete-autotuneconfig]='false')
# Expected log message for other test cases
declare -A autotuneconfig_other_expected_log_msgs
autotuneconfig_other_expected_log_msgs=([incomplete-autotuneconfig]='error: error validating "'${yaml_path}/autotuneconfig_other/incomplete-autotuneconfig.yaml'": error validating data: \[ValidationError(KruizeLayer): missing required field "layer_name" in com.recommender.v1.KruizeLayer, ValidationError(KruizeLayer): missing required field "layer_level" in com.recommender.v1.KruizeLayer, ValidationError(KruizeLayer): missing required field "layerPresence" in com.recommender.v1.KruizeLayer, ValidationError(KruizeLayer): missing required field "tunables" in com.recommender.v1.KruizeLayer\]; if you choose to ignore these errors, turn validation off with --validate=false')
