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
em_standalone_test_description=([validate_em_ab_workflow]="Deploy autotune and tech empower benchmark application, post the Experiment for ABTesting (ABTestingTemplate.json from examples)
 and validate the Trial status summary")

