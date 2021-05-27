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

package com.autotune.experimentmanager.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.autotune.experimentmanager.fsm.api.EMState;

/**
 * Utility class for keeping the experiment manager specific constants, common code, utility functions etc.
 * @author Bipin Kumar
 *
 * @Date Mar 31, 2021
 */
public final class EMUtils {

    private EMUtils() { }
    public static final String DEFAULT_EVENT_NAME = "event";
    public static final String DEFAULT_TRANSITION_NAME = "transition";
    
	// experiment manager finite state machine states
	public enum EMFSMStates {
		RECOMMENDED_CONFIG_STATE, DEPLOYED_RECOMMENDED_CONFIG_STATE, LOAD_APPEARING_STATE, COLLECT_MATRIX_STATE, BENCH_MARK_RESULT_STATE, USER_RESPONSE_STATE
	}
	
	// experiment manager transmission 
	public enum EMFSMTransition {
		RECOMMENDED_CONFIG_TRANS, DEPLOYED_RECOMMENDED_CONFIG_TRANS, LOAD_APPEARING_TRANS, COLLECT_MATRIX_TRANS, BENCH_MARK_RESULT_TRANS, USER_RESPONSE_TRANS 
	}
	public static String NAMESPACE="default";
	public static String APPLICATION_NAME= "experiment_result";
	
	public static String TRIALS ="trials";
	public static String DEPLOYMENT_NAME_KEY = "deployment_name";
	public static String UPDATE_CONFIG = "update_config";
	public static String ID="id";
	public static String APP_VERSION="app-version";
	public static String DEPLOYMENT_NAME="deployment_name";
    public static String TRIAL_NUM= "trial_num";
    public static String TRIAL_RUN="trial_run";
    public static String TRIAL_MEASUREMENT_TIME="trial_measurement_time";
    public static String METRICS="metrics";
    public static String NAME="name";
    public static String QUERY="query";
    public static String DATASOURCE="datasource";
    
   public static String NEW_DEPLOYMENT_NAME_SUFIX="autotune-trial"; 
   
   public static String dumpFSMStates(final Set<EMState> states) {
       StringBuilder result = new StringBuilder();
       for (EMState state : states) {
           result.append(state.getName()).append(";");
       }
       return result.toString();
   }

   
   public static JSONObject getFileFromResourceAsStream(Class cls, String fileName) {

       ClassLoader classLoader = cls.getClassLoader();
       InputStream instr = classLoader.getResourceAsStream(fileName);
       
       if (instr == null) {
           throw new IllegalArgumentException("file not found! " + fileName);
       } else {
       	String inputJsonStr = new BufferedReader(new InputStreamReader(instr, StandardCharsets.UTF_8))
           		.lines()
           		.collect(Collectors.joining("\n"));
           
           return new JSONObject(inputJsonStr);
       }

   }
}
