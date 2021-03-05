package com.autotune.em.fsm.api;

import java.util.Date;

import com.autotune.em.utils.EMUtils;


public abstract class EMAbstractEvent implements EMEvent {

    protected String name;
    protected long timestamp;
    protected String stateStatus;
    
    protected EMAbstractEvent() {
        this.name = EMUtils.DEFAULT_EVENT_NAME;
        timestamp = System.currentTimeMillis();
    }

    protected EMAbstractEvent(final String name) {
        this.name = name;
        timestamp = System.currentTimeMillis();
    }

    public String getEMEventName() {
        return name;
    }

    public long getEMEventTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Event" +
                "{name='" + name + '\'' +
                ", timestamp=" + new Date(timestamp) +
                '}';
    }

}
