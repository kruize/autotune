package com.autotune.common.exceptions.datasource;

public class DataSourceAuthFailed extends Exception {
    public DataSourceAuthFailed() {
    }

    public DataSourceAuthFailed(String message) {
        super(message);
    }
}
