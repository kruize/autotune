package com.autotune.common.exceptions;

public class UnsupportedDataSourceProvider extends Exception{
    public UnsupportedDataSourceProvider() {
    }

    public UnsupportedDataSourceProvider(String message) {
        super(message);
    }
}
