package com.autotune.common.exceptions;

public class DataSourceMissingRequiredFiled extends Exception {
    public DataSourceMissingRequiredFiled() {
    }

    public DataSourceMissingRequiredFiled(String message) {
        super(message);
    }
}