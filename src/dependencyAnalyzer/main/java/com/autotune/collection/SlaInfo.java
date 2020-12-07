package com.autotune.collection;

public class SlaInfo
{
    private String slaName;
    private String slaValue;

    public String getSlaName() {
        return slaName;
    }

    public void setSlaName(String slaName) {
        this.slaName = slaName;
    }

    public String getSlaValue() {
        return slaValue;
    }

    public void setSlaValue(String slaValue) {
        this.slaValue = slaValue;
    }

    @Override
    public String toString() {
        return "SlaInfo{" +
                "slaName='" + slaName + '\'' +
                ", slaValue='" + slaValue + '\'' +
                '}';
    }
}
