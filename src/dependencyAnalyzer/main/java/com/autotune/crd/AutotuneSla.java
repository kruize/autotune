package com.autotune.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

/*
      sla:
    name: "response-time"
    value: "lower"
 */
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class AutotuneSla implements KubernetesResource
{
    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "AutotuneSla{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
