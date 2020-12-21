package com.autotune.collection;

/**
 * Holds information about the sla key in the autotune object yaml
 *
 * Example:
 *   sla:
 *     sla_class: "response_time"
 *     direction: "lower"
 */
public class SlaInfo
{
    private String slaClass;
    private String direction;

    public String getSlaClass() {
        return slaClass;
    }

    public void setSlaClass(String slaClass) {
        this.slaClass = slaClass;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "typeName='" + slaClass + '\'' +
                ", typeValue='" + direction + '\'' +
                '}';
    }
}
