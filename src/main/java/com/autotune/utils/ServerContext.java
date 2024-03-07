/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.utils;

/**
 * Holds the server context of the dependency analyzer.
 * <p>
 * All endpoints are having CORS enabled by default
 */
public class ServerContext {
    public static final int KRUIZE_SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("AUTOTUNE_SERVER_PORT", "8080"));
    public static final int KRUIZE_HTTP_THREAD_POOL_COUNT = Integer.parseInt(System.getenv().getOrDefault("KRUIZE_HTTP_THREAD_POOL_COUNT", "10"));
    public static final int HPO_SERVER_PORT = 8085;

    // AnalyzerConstants end points
    public static final String ROOT_CONTEXT = "/";
    public static final String HEALTH_SERVICE = ROOT_CONTEXT + "health";
    public static final String METRICS_SERVICE = ROOT_CONTEXT + "metrics";
    public static final String LIST_KRUIZE_TUNABLES = ROOT_CONTEXT + "listKruizeTunables";
    public static final String LIST_STACKS = ROOT_CONTEXT + "listStacks";
    public static final String LIST_STACK_LAYERS = ROOT_CONTEXT + "listStackLayers";
    public static final String LIST_STACK_TUNABLES = ROOT_CONTEXT + "listStackTunables";
    public static final String SEARCH_SPACE = ROOT_CONTEXT + "searchSpace";
    public static final String LIST_EXPERIMENTS = ROOT_CONTEXT + "listExperiments";
    public static final String EXPERIMENTS_SUMMARY = ROOT_CONTEXT + "experimentsSummary";
    public static final String CREATE_EXPERIMENT = ROOT_CONTEXT + "createExperiment";
    public static final String UPDATE_RESULTS = ROOT_CONTEXT + "updateResults";
    public static final String UPDATE_RECOMMENDATIONS = ROOT_CONTEXT + "updateRecommendations";

    public static final String GENERATE_RECOMMENDATIONS = ROOT_CONTEXT + "generateRecommendations";
    public static final String RECOMMEND_RESULTS = ROOT_CONTEXT + "listRecommendations";
    public static final String CREATE_PERF_PROFILE = ROOT_CONTEXT + "createPerformanceProfile";
    public static final String LIST_PERF_PROFILES = ROOT_CONTEXT + "listPerformanceProfiles";

    public static final String KRUIZE_SERVER_URL = "http://localhost:" + KRUIZE_SERVER_PORT;
    public static final String SEARCH_SPACE_END_POINT = KRUIZE_SERVER_URL + SEARCH_SPACE;
    public static final String LIST_EXPERIMENTS_END_POINT = KRUIZE_SERVER_URL + LIST_EXPERIMENTS;
    public static final String UPDATE_RESULTS_END_POINT = KRUIZE_SERVER_URL + UPDATE_RESULTS;
    public static final String UPDATE_RECOMMENDATIONS_END_POINT = KRUIZE_SERVER_URL + UPDATE_RECOMMENDATIONS;

    // HPO End Points
    public static final String HPO_SERVER_URL = "http://localhost:" + HPO_SERVER_PORT;
    public static final String HPO_TRIALS = ROOT_CONTEXT + "experiment_trials";
    public static final String HPO_TRIALS_END_POINT = HPO_SERVER_URL + HPO_TRIALS;

    public static final String EXPERIMENT_MANAGER_SERVER_URL = "http://localhost:" + KRUIZE_SERVER_PORT;
    public static final String EXPERIMENT_MANAGER_CREATE_TRIAL = ROOT_CONTEXT + "createExperimentTrial";
    public static final String EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT = EXPERIMENT_MANAGER_SERVER_URL + EXPERIMENT_MANAGER_CREATE_TRIAL;
    public static final String EXPERIMENT_MANAGER_LIST_EXPERIMENT_TRIAL = ROOT_CONTEXT + "listExperimentTrial";
    public static final String EXPERIMENT_MANAGER_LIST_EXPERIMENT_END_POINT = EXPERIMENT_MANAGER_SERVER_URL + EXPERIMENT_MANAGER_LIST_EXPERIMENT_TRIAL;
    public static final String EXPERIMENT_MANAGER_LIST_TRIAL_STATUS = ROOT_CONTEXT + "listTrialStatus";
    public static final String EXPERIMENT_MANAGER_LIST_TRIAL_STATUS_END_POINT = EXPERIMENT_MANAGER_SERVER_URL + EXPERIMENT_MANAGER_LIST_TRIAL_STATUS;

    //Datasource EndPoints
    public static final String LIST_DATASOURCES = ROOT_CONTEXT + "datasources";
    public static final String DATASOURCE_METADATA = ROOT_CONTEXT + "dsmetadata";

    // UI support EndPoints
    public static final String QUERY_CONTEXT = ROOT_CONTEXT + "query/";
    public static final String LIST_NAMESPACES = QUERY_CONTEXT + "listNamespaces";
    public static final String LIST_DEPLOYMENTS = QUERY_CONTEXT + "listDeployments";
    public static final String LIST_K8S_OBJECTS = QUERY_CONTEXT + "listK8sObjects";
}
