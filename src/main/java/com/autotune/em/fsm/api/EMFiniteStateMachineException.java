package com.autotune.em.fsm.api;
public class EMFiniteStateMachineException extends Exception {

    /**
     * The transition where the exception occurred.
     */
    private final EMTransition transition;

    /**
     * The event triggered when the exception occurred.
     */
    private final EMEvent event;

    /**
     * The root cause of the exception.
     */
    private final Throwable cause;

    /**
     * Create a new {@link EMFiniteStateMachineException}.
     *
     * @param transition where the exception occurred
     * @param event triggered when the exception occurred
     * @param cause root cause of the exception
     */
    public EMFiniteStateMachineException(final EMTransition transition, final EMEvent event, final Throwable cause) {
        this.transition = transition;
        this.event = event;
        this.cause = cause;
    }

    /**
     * Get the transition where the exception occurred.
     * @return the transition where the exception occurred.
     */
    public EMTransition getTransition() {
        return transition;
    }

    /**
     * Get the event triggered when the exception occurred.
     * @return the event triggered when the exception occurred.
     */
    public EMEvent getEvent() {
        return event;
    }

    /**
     * Get the root cause of the exception.
     * @return the root cause of the exception
     */
    public Throwable getCause() {
        return cause;
    }
}
