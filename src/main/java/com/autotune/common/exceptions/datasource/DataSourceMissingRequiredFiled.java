package com.autotune.common.exceptions.datasource;

public class DataSourceMissingRequiredFiled extends Exception {
    public DataSourceMissingRequiredFiled() {
    }

    public DataSourceMissingRequiredFiled(String message) {
        super(message);
    }
}
