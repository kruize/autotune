package com.autotune.experimentManager.exceptions;

public class EMDataObjectIsInEditingException extends Exception{
    public EMDataObjectIsInEditingException() {

    }

    public EMDataObjectIsInEditingException(String msg) {
        super(msg);
    }
}
