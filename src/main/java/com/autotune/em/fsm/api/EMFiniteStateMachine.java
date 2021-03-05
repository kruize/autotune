package com.autotune.em.fsm.api;

import java.util.Set;
public interface EMFiniteStateMachine {

    /**
     * Return current FSM state.
     * @return current FSM state
     */
    EMState getCurrentState();

    /**
     * Return FSM initial state.
     * @return FSM initial state
     */
    EMState getInitialState();

    /**
     * Return FSM final states.
     * @return FSM final states
     */
    Set<EMState> getFinalStates();

    /**
     * Return FSM registered states.
     * @return FSM registered states
     */
    Set<EMState> getStates();

    /**
     * Return FSM registered transitions.
     * @return FSM registered transitions
     */
    Set<EMTransition> getTransitions();

    /**
     * Return the last triggered event.
     * @return the last triggered event
     */
    EMEvent getLastEvent();

    /**
     * Return the last transition made.
     * @return the last transition made
     */
    EMTransition getLastTransition();

    /**
     * Fire an event. According to event type, the FSM will make the right transition.
     * @param event to fire
     * @return The next FSM state defined by the transition to make
     * @throws EMFiniteStateMachineException thrown if an exception occurs during event handling
     */
    EMState fire(EMEvent event) throws EMFiniteStateMachineException;

}
