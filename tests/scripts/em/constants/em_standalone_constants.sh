#!/bin/bash
#
# Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
#
##### Constants for EM standalone tests #####

space=" "
# Brief description about the experiment manager tests
declare -A em_standalone_test_description
em_standalone_test_description=([validate_single_deployment]="Post the input JSON to Experiment manager and validate if the new application deployed by the experiment manager has the configuration mentioned in the input JSON",
[validate_multiple_deployments]="Post multiple input JSONs to Experiment manager and validate if the new application deployed by the experiment manager has the configuration mentioned in the input JSON",
[invalid_input_json]="Post invalid input JSON to Experiment manager and validate the result")

invalid_input_json_testcases=("deployment_policy")
#invalid_input_json_testcases=("cpu" "memory" "training_metric_datasource")
#invalid_input_json_testcases=("exp_id" "exp_name" "deployment_policy" "deployment_name" "namespace" "cpu" "memory" "metric_name" "metric_query" "metric_datasource") 

same_trial_testcases=("same_trial_diff_metrics")

declare -A invalid_input_json_find
invalid_input_json_find=([exp_id]='"experiment_id": "f7cc7393db1dd9aa28ef9ad038b956fe72a570fba775a8c3568ebafaf3c0eb1d",' 
[exp_name]='"experiment_name": "autotune-max-http-throughput",'
[deployment_policy]='"type": "rollingUpdate"'
[deployment_name]='"deployment_name": "petclinic-sample-0",'
[namespace]='"namespace": "default",'
[cpu]='"cpu": "2"'
[memory]='"memory": "223Mi",'
[metric_name]='"name": "request_count"'
[metric_query]='"query": "rate(http_server_requests_seconds_count{exception=\"None\",method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/webjars/**\",}[1m])",'
[metric_datasource]='"datasource": "prometheus",')


# experiment details 

exp_id_tests=("invalid-exp-id" "blank-exp-id" "null-exp-id" "no-exp-id")

exp_name_tests=("invalid-app-name" "blank-app-name" "null-app-name" "no-app-name")

declare -A exp_id_replace=([invalid-exp-id]='"experiment_id": "xyz",' [blank-exp-id]='"experiment_id": " ",' [null-exp-id]='"experiment_id": null,' [no-exp-id]=' ')

declare -A exp_name_replace=([invalid-app-name]='"experiment_name": "xyz",' [blank-app-name]='"experiment_name": " ",' [null-app-name]='"experiment_name": null,' [no-app-name]=' ')


# Settings section

deployment_policy_tests=("invalid-deployment-policy" "blank-deployment-policy" "null-deployment-policy" "no-deployment-policy")

declare -A deployment_policy_replace=([invalid-deployment-policy]='"type": "xyyz"' [blank-deployment-policy]='"type": " "' [null-deployment-policy]='"type": null' [no-deployment-policy]='' )


# Deployment section

deployment_name_tests=("invalid-deployment-name" "blank-deployment-name" "null-deployment-name" "no-deployment-name")

namespace_tests=("invalid-namespace" "blank-namespace" "null-namespace" "no-namespace")

declare -A deployment_name_replace=([invalid-deployment-name]='"deployment_name": "xyz",' [blank-deployment-name]='"deployment_name": " ",' [null-deployment-name]='"deployment_name": null,' [no-deployment-name]='' )

declare -A namespace_replace=([invalid-namespace]='"namespace": "xyz",' [blank-namespace]='"namespace": " ",' [null-namespace]='"namespace": null,' [no-namespace]='')


# config section

cpu_tests=("invalid-cpu" "blank-cpu" "null-cpu" "max-cpu")

memory_tests=("invalid-memory" "blank-memory" "null-memory" "max-memory")

declare -A cpu_replace=([invalid-cpu]='"cpu": "-1"' [blank-cpu]='"cpu": " "' [null-cpu]='"cpu": null' [max-cpu]='"cpu": "90"')

declare -A memory_replace=([invalid-memory]='"memory": "-1",' [blank-memory]='"memory": " ",' [null-memory]='"memory": null,' [max-memory]='"memory": "3325000Mi",')


# Metrics section

metric_name_tests=("invalid-metric-name" "blank-metric-name" "null-metric-name" "no-metric-name")

metric_query_tests=("invalid-metric-query" "blank-metric-query" "null-metric-query" "no-metric-query")

metric_datasource_tests=("invalid--metric-datasource" "blank-metric-datasource" "null-metric-datasource" "no-metric-datasource")

same_trial_diff_metrics_tests=("add-new-tunable" "remove-tunable")

declare -A metric_name_replace=([invalid-metric-name]='"name": "xyz"' [blank-metric-name]='"name": " "' [null-metric-name]='"name": null' [no-metric-name]='')

declare -A metric_query_replace=([invalid-training-metric-query]='"query": "xyz",' [blank-training-metric-query]='"query": " ",' [null-metric-query]='"query": null,' [no-metric-query]=' ')

declare -A metric_datasource_replace=([invalid-metric-datasource]='"datasource": "xyz",' [blank-metric-datasource]='"datasource": " ",' [null-metric-datasource]='"datasource": null,' [no-metric-datasource]=' ')

declare -A same_trial_find=([same_trial_diff_metrics]='{\n'$(printf "%-21s" "${space}")'"name": "request_sum",\n'$(printf "%-21s" "${space}")'"query": "request_sum_query",\n'$(printf "%-21s" "${space}")'"datasource": "prometheus"\n'$(printf "%-18s" "${space}")'},')

declare -A same_trial_diff_metrics_replace=([add-new-tunable]='{\n'$(printf "%-21s" "${space}")'"name": "request_sum",\n'$(printf "%-21s" "${space}")'"query": "request_sum_query",\n'$(printf "%-21s" "${space}")'"datasource": "prometheus"\n'$(printf "%-18s" "${space}")'},'$(printf "%-18s" "${space}")'{\n'$(printf "%-21s" "${space}")'"name": "cpu_limit",\n'$(printf "%-21s" "${space}")'"query": "cpu_limit_query",\n'$(printf "%-21s" "${space}")'"datasource": "prometheus"\n'$(printf "%-18s" "${space}")'},' [remove-tunable]=' ')
