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
package com.autotune.experimentmanager.fsm.core;

import com.autotune.experimentmanager.fsm.api.EMEvent;
import com.autotune.experimentmanager.fsm.api.EMEventHandler;
import com.autotune.experimentmanager.fsm.api.EMState;
import com.autotune.experimentmanager.fsm.api.EMTransition;
import com.autotune.experimentmanager.utils.EMUtils;

/**
 * EMTransitionImpl is concrete implementation of the EMTransion contract.
 * @author Bipin Kumar
 *
 * @Date Mar 31, 2021
 */
final class EMTransitionImpl<E extends EMEvent> implements EMTransition {

    private String name;
    private EMState sourceState;
    private EMState targetState;
    private Class<E> eventType;
    private EMEventHandler<E> eventHandler;

    public EMTransitionImpl() {
        name = EMUtils.DEFAULT_TRANSITION_NAME;
    }

    public EMState getSourceState() {
        return sourceState;
    }

    public void setSourceState(EMState sourceState) {
        this.sourceState = sourceState;
    }

    public EMState getTargetState() {
        return targetState;
    }

    public void setTargetState(EMState targetState) {
        this.targetState = targetState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<E> getEventType() {
        return eventType;
    }

    public void setEventType(Class<E> eventType) {
        this.eventType = eventType;
    }

    public EMEventHandler<? extends EMEvent> getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(EMEventHandler<E> eventHandler) {
        this.eventHandler = eventHandler;
    }

    /*
     * Transitions are unique according to source state and triggering event type
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EMTransitionImpl that = (EMTransitionImpl) o;

        return eventType.equals(that.eventType) && sourceState.equals(that.sourceState);

    }

    @Override
    public int hashCode() {
        int result = sourceState.hashCode();
        result = 31 * result + eventType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Transition");
        sb.append("{name='").append(name).append('\'');
        sb.append(", sourceState=").append(sourceState.getName());
        sb.append(", targetState=").append(targetState.getName());
        sb.append(", eventType=").append(eventType);
        if (eventHandler != null) {
            sb.append(", eventHandler=").append(eventHandler.getClass().getName());
        }
        sb.append('}');
        return sb.toString();
    }
}
