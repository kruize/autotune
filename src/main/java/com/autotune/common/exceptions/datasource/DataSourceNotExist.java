package com.autotune.common.exceptions.datasource;

public class DataSourceNotExist extends Exception {
    public DataSourceNotExist() {
    }

    public DataSourceNotExist(String message) {
        super(message);
    }
}
