package com.autotune.em.fsm.core;

import com.autotune.em.fsm.api.EMEvent;
import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.api.EMState;
import com.autotune.em.fsm.api.EMTransition;
import com.autotune.em.utils.EMUtils;

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
