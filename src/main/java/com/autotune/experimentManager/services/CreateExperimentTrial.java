/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.autotune.experimentManager.services;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * This Class is the endpoint handler for `/createExperimentTrial`
 *
 * We expect input JSON in the format given below for this endpoint
 *
 * Example JSON:
 *
 * {
 *   "experiment_id": "2190310A384BC90EF",
 *   "name": "petclinic-autotune",
 *   "info": {
 *     "trial_id": "",
 *     "trial_num": 1
 *   },
 *   "settings": {
 *     "trial_settings": {
 *       "total_duration": "20 mins",
 *       "warmup_cycles": 5,
 *       "warmup_duration": "1 min",
 *       "measurement_cycles": 15,
 *       "measurement_duration": "1 min"
 *     },
 *     "deployment_settings": {
 *       "deployment_info": {
 *         "deployment_name" : "petclinic-sample",
 *         "target_env" : "qa"
 *       },
 *       "deployment_policy" : {
 *         "type" : "rollingUpdate"
 *       },
 *       "deployment_tracking": {
 *         "trackers": [
 *           "training",
 *           "production"
 *         ]
 *       }
 *     }
 *   },
 *   "deployments": [
 *     {
 *       "type" : "training",
 *       "deployment_name": "petclinic-sample",
 *       "namespace" : "default",
 *       "state": "",
 *       "result": "",
 *       "result_info": "",
 *       "result_error": "",
 *       "metrics": [
 *         {
 *           "name": "request_sum",
 *           "query": "request_sum_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "request_count",
 *           "query": "request_count_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "hotspot_function",
 *           "query": "hotspot_function_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "cpuRequest",
 *           "query": "cpuRequest_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "memRequest",
 *           "query": "memRequest_query",
 *           "datasource": "prometheus"
 *         }
 *       ],
 *       "config": [
 *         {
 *           "name": "update requests and limits",
 *           "spec": {
 *             "template": {
 *               "spec": {
 *                 "container": {
 *                   "resources": {
 *                     "requests": {
 *                       "cpu": 2,
 *                       "memory": "512Mi"
 *                     },
 *                     "limits": {
 *                       "cpu": 3,
 *                       "memory": "1024Mi"
 *                     }
 *                   }
 *                 }
 *               }
 *             }
 *           }
 *         },
 *         {
 *           "name": "update env",
 *           "spec": {
 *             "template": {
 *               "spec": {
 *                 "container": {
 *                   "env": {
 *                     "JDK_JAVA_OPTIONS": "-XX:MaxInlineLevel=23"
 *                   }
 *                 }
 *               }
 *             }
 *           }
 *         }
 *       ]
 *     },
 *     {
 *       "type" : "production",
 *       "deployment_name": "petclinic-sample-1",
 *       "state": "",
 *       "result": "",
 *       "result_info": "",
 *       "result_error": "",
 *       "metrics": [
 *         {
 *           "name": "request_sum",
 *           "query": "request_sum_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "request_count",
 *           "query": "request_count_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "hotspot_function",
 *           "query": "hotspot_function_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "cpuRequest",
 *           "query": "cpuRequest_query",
 *           "datasource": "prometheus"
 *         },
 *         {
 *           "name": "memRequest",
 *           "query": "memRequest_query",
 *           "datasource": "prometheus"
 *         }
 *       ],
 *       "config": [
 *         {
 *           "name": "update requests and limits",
 *           "spec": {
 *             "template": {
 *               "spec": {
 *                 "container": {
 *                   "resources": {
 *                     "requests": {
 *                       "cpu": 2,
 *                       "memory": "512Mi"
 *                     },
 *                     "limits": {
 *                       "cpu": 3,
 *                       "memory": "1024Mi"
 *                     }
 *                   }
 *                 }
 *               }
 *             }
 *           }
 *         },
 *         {
 *           "name": "update env",
 *           "spec": {
 *             "template": {
 *               "spec": {
 *                 "container": {
 *                   "env": {
 *                     "JDK_JAVA_OPTIONS": "-XX:MaxInlineLevel=23"
 *                   }
 *                 }
 *               }
 *             }
 *           }
 *         }
 *       ]
 *     }
 *   ]
 * }
 *
 * Processes the given input JSON validates it and generates a Run ID to return back to user
 */
public class CreateExperimentTrial extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperimentTrial.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String inputData = req.getReader().lines().collect(Collectors.joining());
        JSONObject json = new JSONObject(inputData);
        // TODO: Need to implement EM API
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
