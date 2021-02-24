package com.autotune.em.fsm.core;

import java.util.Set;

import com.autotune.em.fsm.api.EMFiniteStateMachine;
import com.autotune.em.fsm.api.EMState;
import com.autotune.em.utils.EMUtils;

class EMFiniteStateMachineDefinitionValidator {

    /**
     * Deterministic FSM validation : for each state, exactly one outgoing transition for an event type must be defined.
     */
    void validateFiniteStateMachineDefinition(EMFiniteStateMachine finiteStateMachine) {

        Set<EMState> states = finiteStateMachine.getStates();

        //check if initial state belongs to FSM declared states.
        EMState initialState = finiteStateMachine.getInitialState();
        if (!states.contains(initialState)) {
            throw new IllegalStateException("Initial state '" + initialState.getName() + "' must belong to FSM states: " +
                    EMUtils.dumpFSMStates(states));
        }

        //check if registered final states belong to FSM declared states.
        for (EMState finalState : finiteStateMachine.getFinalStates()) {
            if (!states.contains(finalState)) {
                throw new IllegalStateException("Final state '" + finalState.getName() + "' must belong to FSM states: " +
                        EMUtils.dumpFSMStates(states));
            }
        }
    }
}
