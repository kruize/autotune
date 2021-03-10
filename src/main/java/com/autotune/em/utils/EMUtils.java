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

package com.autotune.em.utils;

/**
 * This is utility class for Experiment Manager
 *
 */
public final class EMUtils {

    private EMUtils() { }

    /**
     * Default event name in FSM.
     */
    public static final String DEFAULT_EVENT_NAME = "event";

    /**
     * Default transition name in FSM.
     */
    public static final String DEFAULT_TRANSITION_NAME = "transition";

    /**
     * Utility method to print states names as string.
     * @param states the states set to dump
     * @return string concatenation of states names
     */
//    public static String dumpFSMStates(final Set<EMState> states) {
//        StringBuilder result = new StringBuilder();
//        for (EMState state : states) {
//            result.append(state.getName()).append(";");
//        }
//        return result.toString();
//    }
    
    // Processor Type used to integrate with differnt modules.
    public enum EMProcessorType {
		DAPROCESSOR, MLPROCESSOR, EXPMGRPROCESSOR, RECMGRPROCESSOR
	}
	
	// experiment manager finite state machine states
	public enum EMFSMStates {
		
		RECOMMENDED_CONFIG_STATE, DEPLOYED_RECOMMENDED_CONFIG_STATE, LOAD_APPEARING_STATE, COLLECT_MATRIX_STATE, BENCH_MARK_RESULT_STATE, USER_RESPONSE_STATE
	}
	
	// experiment manager transmission 
	public enum EMFSMTransition {
		
		RECOMMENDED_CONFIG_TRANS, DEPLOYED_RECOMMENDED_CONFIG_TRANS, LOAD_APPEARING_TRANS, COLLECT_MATRIX_TRANS, BENCH_MARK_RESULT_TRANS, USER_RESPONSE_TRANS 
	}
	
	// FSM states
	public enum EMFSMStateStaus {
		STARTED, RUNNING, ERROR, COMPLETED
	}
	
	// Blocking queue names used in Autotune
	public enum QueueName {
		RECMGRQUEUE, EXPMGRQUEUE
	}
}
