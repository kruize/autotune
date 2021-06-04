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
##### script containing constants for autotune object id test constants #####
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

