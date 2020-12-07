package com.autotune.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class AutotuneObjectSpec implements KubernetesResource
{
    private String autotuneSpec;
    private AutotuneSla autotuneSla;
    private String replicas;
    private String mode;
    private AutotuneSelector autotuneSelector;

    public String getAutotuneSpec() {
        return autotuneSpec;
    }

    public void setAutotuneSpec(String autotuneSpec) {
        this.autotuneSpec = autotuneSpec;
    }

    public AutotuneSla getAutotuneSla() {
        return autotuneSla;
    }

    public void setAutotuneSla(AutotuneSla autotuneSla) {
        this.autotuneSla = autotuneSla;
    }

    public String getReplicas() {
        return replicas;
    }

    public void setReplicas(String replicas) {
        this.replicas = replicas;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public AutotuneSelector getAutotuneSelector() {
        return autotuneSelector;
    }

    public void setAutotuneSelector(AutotuneSelector autotuneSelector) {
        this.autotuneSelector = autotuneSelector;
    }

    @Override
    public String toString() {
        return "AutotuneObjectSpec{" +
                "autotuneSpec='" + autotuneSpec + '\'' +
                ", autotuneSla=" + autotuneSla +
                ", replicas='" + replicas + '\'' +
                ", mode='" + mode + '\'' +
                ", autotuneSelector=" + autotuneSelector +
                '}';
    }
}
