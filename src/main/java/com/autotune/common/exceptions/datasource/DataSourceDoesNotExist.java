package com.autotune.common.exceptions.datasource;

public class DataSourceDoesNotExist extends Exception {
    public DataSourceDoesNotExist() {
    }

    public DataSourceDoesNotExist(String message) {
        super(message);
    }
}
