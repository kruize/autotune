package com.autotune.common.auth;

import com.autotune.utils.KruizeConstants;

public class AuthenticationStrategyFactory {

    public static AuthenticationStrategy createAuthenticationStrategy(AuthenticationConfig authConfig) {
        String type = authConfig.getType();
        switch (type) {
            case KruizeConstants.AuthenticationConstants.BASIC:
                String username = authConfig.getCredentials().getUsername();
                String password = authConfig.getCredentials().getPassword();
                return new BasicAuthenticationStrategy(username, password);
            case KruizeConstants.AuthenticationConstants.BEARER:
                String tokenFilePath = authConfig.getCredentials().getTokenFilePath();
                return new BearerAuthenticationStrategy(tokenFilePath);
            case KruizeConstants.AuthenticationConstants.API_KEY:
                String apiKey = authConfig.getCredentials().getApiKey();
                return new APIKeyAuthenticationStrategy(apiKey);
            case KruizeConstants.AuthenticationConstants.OAUTH2:
                String tokenEndpoint = authConfig.getCredentials().getTokenEndpoint();
                String clientId = authConfig.getCredentials().getClientId();
                String clientSecret = authConfig.getCredentials().getClientSecret();
                return new OAuth2AuthenticationStrategy(tokenEndpoint, clientId, clientSecret);
            default:
                throw new IllegalArgumentException(KruizeConstants.AuthenticationConstants.UNKNOWN_AUTHENTICATION+ type);
        }
    }
}
