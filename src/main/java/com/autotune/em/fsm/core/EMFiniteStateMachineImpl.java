package com.autotune.em.fsm.core;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.autotune.em.fsm.api.EMEvent;
import com.autotune.em.fsm.api.EMFiniteStateMachine;
import com.autotune.em.fsm.api.EMFiniteStateMachineException;
import com.autotune.em.fsm.api.EMState;
import com.autotune.em.fsm.api.EMTransition;

final class EMFiniteStateMachineImpl implements EMFiniteStateMachine {

    private static final Logger LOGGER = Logger.getLogger(EMFiniteStateMachineImpl.class.getSimpleName());

    private EMState currentState;
    private final EMState initialState;
    private final Set<EMState> finalStates;
    private final Set<EMState> states;
    private final Set<EMTransition> transitions;
    private EMEvent lastEvent;
    private EMTransition lastTransition;

    EMFiniteStateMachineImpl(final Set<EMState> states, final EMState initialState) {
        this.states = states;
        this.initialState = initialState;
        currentState = initialState;
        transitions = new HashSet<>();
        finalStates = new HashSet<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final synchronized EMState fire(final EMEvent event) throws EMFiniteStateMachineException {

        if (!finalStates.isEmpty() && finalStates.contains(currentState)) {
            LOGGER.log(Level.WARNING, "FSM is in final state '" + currentState.getName() + "', event " + event + " is ignored.");
            return currentState;
        }

        if (event == null) {
            LOGGER.log(Level.WARNING, "Null event fired, FSM state unchanged");
            return currentState;
        }

        for (EMTransition transition : transitions) {
            if (
                    currentState.equals(transition.getSourceState()) && //fsm is in the right state as expected by transition definition
                    transition.getEventType().equals(event.getClass()) && //fired event type is as expected by transition definition
                    states.contains(transition.getTargetState()) //target state is defined
                    ) {
                try {
                    //perform action, if any
                    if (transition.getEventHandler() != null) {
                        transition.getEventHandler().handleEvent(event);
                    }
                    //transit to target state
                    currentState = transition.getTargetState();

                    //save last triggered event and transition
                    lastEvent = event;
                    lastTransition = transition;

                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "An exception occurred during handling event " + event + " of transition " + transition, e);
                    throw new EMFiniteStateMachineException(transition, event, e);
                }
            }
        }
        return currentState;
    }

    void registerTransition(final EMTransition transition) {
        transitions.add(transition);
    }

    void registerFinalState(final EMState finalState) {
        finalStates.add(finalState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EMState getCurrentState() {
        return currentState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EMState getInitialState() {
        return initialState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<EMState> getFinalStates() {
        return finalStates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<EMState> getStates() {
        return states;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<EMTransition> getTransitions() {
        return transitions;
    }

    @Override
    public EMEvent getLastEvent() {
        return lastEvent;
    }

    @Override
    public EMTransition getLastTransition() {
        return lastTransition;
    }

}
