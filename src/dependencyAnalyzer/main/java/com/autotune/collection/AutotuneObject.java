package com.autotune.collection;

import com.autotune.query.ApplicationTunables;

import java.util.HashMap;
import java.util.Map;

public class AutotuneObject
{
    private String autotuneObject;
    private String mode;
    private int replicas;
    private SlaInfo slaInfo;
    private SelectorInfo selectorInfo;
    public Map<String, ApplicationTunables> applicationTunablesMap= new HashMap<>();

    public String getAutotuneObject() {
        return autotuneObject;
    }

    public void setAutotuneObject(String autotuneObject) {
        this.autotuneObject = autotuneObject;
    }

    public SlaInfo getSlaInfo() {
        return slaInfo;
    }

    public void setSlaInfo(SlaInfo slaInfo) {
        this.slaInfo = slaInfo;
    }

    public SelectorInfo getSelectorInfo() {
        return selectorInfo;
    }

    public void setSelectorInfo(SelectorInfo selectorInfo) {
        this.selectorInfo = selectorInfo;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    @Override
    public String toString() {
        return "AutotuneObjectInfo{" +
                "autotuneObject='" + autotuneObject + '\'' +
                ", slaInfo=" + slaInfo +
                ", selectorInfo=" + selectorInfo +
                '}';
    }
}
