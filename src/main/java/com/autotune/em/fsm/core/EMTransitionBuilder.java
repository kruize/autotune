/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.autotune.em.fsm.core;

import com.autotune.em.fsm.api.EMEvent;
import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.api.EMState;
import com.autotune.em.fsm.api.EMTransition;

public class EMTransitionBuilder {

    private final EMTransitionImpl transition;

    /**
     * Create a new {@link EMTransitionBuilder}.
     */
    public EMTransitionBuilder() {
        transition = new EMTransitionImpl();
    }

    /**
     * Set the name of the transition.
     * @param name of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder name(final String name) {
        transition.setName(name);
        return this;
    }

    /**
     * Set the source state of the transition.
     * @param sourceState of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder sourceState(final EMState sourceState) {
        transition.setSourceState(sourceState);
        return this;
    }

    /**
     * Set the target state of the transition.
     * @param targetState of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder targetState(final EMState targetState) {
        transition.setTargetState(targetState);
        return this;
    }

    /**
     * Set event type upon which the transition should be triggered.
     * @param eventType of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder eventType(final Class<? extends EMEvent> eventType) {
        transition.setEventType(eventType);
        return this;
    }

    /**
     * Set the event handler of the transition.
     * @param eventHandler of the transition
     * @return FSM transition builder
     */
    public <E extends EMEvent> EMTransitionBuilder eventHandler(final EMEventHandler<E> eventHandler) {
        transition.setEventHandler(eventHandler);
        return this;
    }

    /**
     * Build a transition instance.
     * @return a transition instance.
     */
    public EMTransition build() {
        return transition;
    }

}
