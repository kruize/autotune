package com.autotune.common.auth;

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
