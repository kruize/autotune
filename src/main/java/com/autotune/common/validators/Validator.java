package com.autotune.common.validators;

public class Validator {
    private Validator() {

    }

    public static MetricsValidator getMetricsValidator() {
        return MetricsValidator.getInstance();
    }
}
