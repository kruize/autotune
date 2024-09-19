package com.autotune.common.auth;

public class NoAuthStrategy implements AuthenticationStrategy {
    @Override
    public String applyAuthentication() {
        // No authentication is applied
        return null;
    }
}
