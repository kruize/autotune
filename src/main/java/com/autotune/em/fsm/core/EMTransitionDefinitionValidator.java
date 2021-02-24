package com.autotune.em.fsm.core;

import com.autotune.em.fsm.api.EMFiniteStateMachine;
import com.autotune.em.fsm.api.EMState;
import com.autotune.em.fsm.api.EMTransition;

class EMTransitionDefinitionValidator {
    void validateTransitionDefinition(final EMTransition transition, EMFiniteStateMachine finiteStateMachine) {

        String transitionName = transition.getName();
        EMState sourceState = transition.getSourceState();
        EMState targetState = transition.getTargetState();

        if (sourceState == null) {
            throw new IllegalArgumentException("No source state is defined for transition '" + transitionName + "'");
        }
        if (targetState == null) {
            throw new IllegalArgumentException("No target state is defined for transition '" + transitionName + "'");
        }
        if (transition.getEventType() == null) {
            throw new IllegalArgumentException("No event type is defined for transition '" + transitionName + "'");
        }
        if (!finiteStateMachine.getStates().contains(sourceState)) {
            throw new IllegalArgumentException("Source state '" + sourceState.getName() + "' is not registered in FSM states for transition '" + transitionName + "'");
        }
        if (!finiteStateMachine.getStates().contains(targetState)) {
            throw new IllegalArgumentException("target state '" + targetState.getName() + "' is not registered in FSM states for transition '" + transitionName + "'");
        }
    }

}
