package com.autotune.common.auth;

public class AuthenticationStrategyFactory {

    public static AuthenticationStrategy createAuthenticationStrategy(AuthenticationConfig authConfig) {
        String type = authConfig.getType();
        switch (type) {
            case "basic":
                String username = authConfig.getCredentials().getUsername();
                String password = authConfig.getCredentials().getPassword();
                return new BasicAuthenticationStrategy(username, password);
            case "bearer":
                String tokenFilePath = authConfig.getCredentials().getTokenFilePath();
                return new BearerAuthenticationStrategy(tokenFilePath);
            case "apiKey":
                String apiKey = authConfig.getCredentials().getApiKey();
                return new APIKeyAuthenticationStrategy(apiKey);
            case "oauth2":
                String tokenEndpoint = authConfig.getCredentials().getTokenEndpoint();
                String clientId = authConfig.getCredentials().getClientId();
                String clientSecret = authConfig.getCredentials().getClientSecret();
                return new OAuth2AuthenticationStrategy(tokenEndpoint, clientId, clientSecret);
            default:
                throw new IllegalArgumentException("Unknown authentication type: " + type);
        }
    }
}
