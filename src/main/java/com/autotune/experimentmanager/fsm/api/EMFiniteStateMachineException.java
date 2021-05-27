
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
package com.autotune.experimentmanager.fsm.api;
/**
 * Finite state machine exception handling and propagation manage using this class. 
 * @author Bipin Kumar
 *
 * @Date Mar 30, 2021
 */
public class EMFiniteStateMachineException extends Exception {
	
	private static final long serialVersionUID = -3420121349966610018L;

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
