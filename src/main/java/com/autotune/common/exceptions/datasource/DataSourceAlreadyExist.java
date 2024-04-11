package com.autotune.common.exceptions.datasource;

public class DataSourceAlreadyExist extends Exception {
    public DataSourceAlreadyExist() {
    }

    public DataSourceAlreadyExist(String message) {
        super(message);
    }
}
