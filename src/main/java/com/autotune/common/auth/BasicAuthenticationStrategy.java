package com.autotune.common.auth;

import java.util.Base64;

public class BasicAuthenticationStrategy implements AuthenticationStrategy {
    private final String username;
    private final String password;

    public BasicAuthenticationStrategy(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String applyAuthentication() {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
