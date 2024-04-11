package com.autotune.common.exceptions.datasource;

public class UnsupportedDataSourceProvider extends Exception {
    public UnsupportedDataSourceProvider() {
    }

    public UnsupportedDataSourceProvider(String message) {
        super(message);
    }
}
