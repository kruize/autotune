package com.autotune.common.exceptions;

public class ServiceNotFound extends Exception {
    public ServiceNotFound() {
    }

    public ServiceNotFound(String message) {
        super(message);
    }
}