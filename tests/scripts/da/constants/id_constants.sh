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
##### script containing constants for autotune object id and autotuneconfig object id test constants #####
#

autotune_id=("get_listapplication_json_app" "get_listapplication_json" "get_listapplayer_json_app" "get_listapplayer_json" "get_listapptunables_json_app_layer" "get_listapptunables_json_app" "get_listapptunables_json" "get_searchspace_json_app" "get_searchspace_json")

old_autotune_id=("get_listapplication_json_app" "get_listapplication_json" "get_listapplayer_json_app" "get_listapplayer_json" "get_listapptunables_json_app_layer" "get_listapptunables_json_app" "get_listapptunables_json" "get_searchspace_json_app" "get_searchspace_json")

# Breif description about the autotune id tests
declare -A autotune_id_test_description
autotune_id_test_description=([check_uniqueness]="Deploy autotune objects and check if it has unique ids"
[re_apply]="Delete the autotune object and re-apply without making any changes, the autotune object should have same ids" 
[update_app_autotune_yaml]="Update and apply the application autotune yamls and validate the ids"
[multiple_apps]="Deploy multiple applications and check if the autotune object ids are unique")

# Expected behaviour for each id test
declare -A autotune_id_expected_behaviour
autotune_id_expected_behaviour=([check_uniqueness]="The autotune objects should have unique ids"
[re_apply]="The autotune object should have same ids" 
[update_app_autotune_yaml]="The autotune object id should not be same as previous"
[multiple_apps]="The autotune object should have unique ids")

##### script containing constants for autotune/autotune_layer_config object id test constants #####
#

layer_config_id=("get_list_autotune_tunables_json_slo_layer" "get_list_autotune_tunables_json_slo" "get_list_autotune_tunables_json")

old_layer_config_id=( "get_list_autotune_tunables_json_slo_layer" "get_list_autotune_tunables_json_slo" "get_list_autotune_tunables_json")

# Breif description about the layer config id tests
declare -A layer_config_id_test_description
layer_config_id_test_description=([check_uniqueness_test]="Deploy autotune and check if it has unique autotune config object ids"
[re_apply_config_test]="Delete the autotune config object and re-apply without making any changes, validate if the autotune config objects have same ids" 
[update_layer_config_yaml_test]="update and apply the autotune config yaml and validate the ids"
[new_layer_config_test]="Apply new layer config, check if the new autotune layer config object id is added and it is unique")

# Expected behaviour for each layer config id test
declare -A layer_config_id_expected_behaviour
layer_config_id_expected_behaviour=([check_uniqueness_test]="The autotune layer objects should have unique ids"
[re_apply_config_test]="The autotune layer object should have same ids" 
[update_layer_config_yaml_test]="The autotune layer object id should not be same as previous"
[new_layer_config_test]="The new autotune layer config object id should be present and should be unique id")
