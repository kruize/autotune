package com.autotune.em.fsm.api;
public interface EMTransition  {

    /**
     * Return transition name.
     * @return transition name
     */
    String getName();

    /**
     * Return transition source state.
     * @return transition source state
     */
    EMState getSourceState();

    /**
     * Return transition target state.
     * @return transition target state
     */
    EMState getTargetState();

    /**
     * Return fired event type upon which the transition should be made.
     * @return Event type class
     */
    Class<? extends EMEvent> getEventType();

    /**
     * Return event handler to execute when an event is fired.
     * @return transition event handler
     */
    EMEventHandler getEventHandler();

}
