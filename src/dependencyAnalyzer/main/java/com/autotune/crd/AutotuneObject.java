package com.autotune.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;

public class AutotuneObject extends CustomResource implements Namespaced
{
    private AutotuneObjectSpec spec;

    @Override
    public ObjectMeta getMetadata() {
        return super.getMetadata();
    }

    public AutotuneObjectSpec getSpec() {
        return spec;
    }

    public void setSpec(AutotuneObjectSpec spec) {
        this.spec = spec;
    }

    @Override
    public String toString()
    {
        return "Autotune{"+
                "apiVersion='" + getApiVersion() + "'" +
                ", metadata=" + getMetadata() +
                ", spec=" + spec +
                "}";
    }
}
