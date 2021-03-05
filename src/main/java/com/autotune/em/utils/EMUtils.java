package com.autotune.em.utils;

import java.util.Set;

import com.autotune.em.fsm.api.EMState;
public final class EMUtils {

    private EMUtils() { }

    /**
     * Default event name.
     */
    public static final String DEFAULT_EVENT_NAME = "event";

    /**
     * Default transition name.
     */
    public static final String DEFAULT_TRANSITION_NAME = "transition";

    /**
     * Utility method to print states names as string.
     * @param states the states set to dump
     * @return string concatenation of states names
     */
    public static String dumpFSMStates(final Set<EMState> states) {
        StringBuilder result = new StringBuilder();
        for (EMState state : states) {
            result.append(state.getName()).append(";");
        }
        return result.toString();
    }
    
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
	
	public enum EMFSMStateStaus {
		STARTED, RUNNING, ERROR, COMPLETED
	}
	
	public enum QueueName {
		RECMGRQUEUE, EXPMGRQUEUE
	}
}
