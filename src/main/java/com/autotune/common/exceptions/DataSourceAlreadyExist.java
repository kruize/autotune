package com.autotune.common.exceptions;

public class DataSourceAlreadyExist extends Exception {
    public DataSourceAlreadyExist() {
    }

    public DataSourceAlreadyExist(String message) {
        super(message);
    }
}