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
 */
public class ServerContext
{
	public static final int AUTOTUNE_PORT = 8080;
	public static final int OPTUNA_PORT = 8085;

	// Analyzer end points
	public static final String ROOT_CONTEXT = "/";
	public static final String HEALTH_SERVICE = ROOT_CONTEXT + "health";
	public static final String METRICS_SERVICE = ROOT_CONTEXT + "metrics";
	public static final String LIST_AUTOTUNE_TUNABLES = ROOT_CONTEXT + "listAutotuneTunables";
	public static final String LIST_STACKS = ROOT_CONTEXT + "listStacks";
	public static final String LIST_STACK_LAYERS = ROOT_CONTEXT + "listStackLayers";
	public static final String LIST_STACK_TUNABLES = ROOT_CONTEXT + "listStackTunables";
	public static final String SEARCH_SPACE = ROOT_CONTEXT + "searchSpace";
	public static final String LIST_EXPERIMENTS = ROOT_CONTEXT + "listExperiments";
	public static final String EXPERIMENTS_SUMMARY = ROOT_CONTEXT + "experimentsSummary";


	public static final String AUTOTUNE_SERVER_PORT = "http://localhost:" + AUTOTUNE_PORT;
	public static final String SEARCH_SPACE_END_POINT = AUTOTUNE_SERVER_PORT + SEARCH_SPACE;

	// Optuna End Points
	public static final String OPTUNA_SERVER_PORT = "http://localhost:" + OPTUNA_PORT;
	public static final String OPTUNA_TRIALS = ROOT_CONTEXT + "experiment_trials";
	public static final String OPTUNA_TRIALS_END_POINT = OPTUNA_SERVER_PORT + OPTUNA_TRIALS;

	public static final String EXPERIMENT_MANAGER_SERVER_PORT = "http://localhost:" + AUTOTUNE_PORT;
	public static final String EXPERIMENT_MANAGER_CREATE_TRIAL = ROOT_CONTEXT + "createExperimentTrial";
	public static final String EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT = EXPERIMENT_MANAGER_SERVER_PORT + EXPERIMENT_MANAGER_CREATE_TRIAL;
	public static final String EXPERIMENT_MANAGER_LIST_EXPERIMENT_TRIAL = ROOT_CONTEXT + "listExperimentTrial";
	public static final String EXPERIMENT_MANAGER_LIST_EXPERIMENT_END_POINT = EXPERIMENT_MANAGER_SERVER_PORT + EXPERIMENT_MANAGER_LIST_EXPERIMENT_TRIAL;
	public static final String EXPERIMENT_MANAGER_LIST_TRIAL_STATUS = ROOT_CONTEXT + "listTrialStatus";
	public static final String EXPERIMENT_MANAGER_LIST_TRIAL_STATUS_END_POINT = EXPERIMENT_MANAGER_SERVER_PORT + EXPERIMENT_MANAGER_LIST_TRIAL_STATUS;
}
