package com.autotune.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableAutotuneObject extends CustomResourceDoneable<AutotuneObject>
{
    public DoneableAutotuneObject(AutotuneObject resource, Function<AutotuneObject, AutotuneObject> function) {
        super(resource, function);
    }
}
