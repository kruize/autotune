package com.autotune.common.datasource.auth;

public class APIKeyAuthenticationStrategy implements AuthenticationStrategy {
    private final String apiKey;

    public APIKeyAuthenticationStrategy(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String applyAuthentication() {
        return "Api-Key " + apiKey;
    }
}
