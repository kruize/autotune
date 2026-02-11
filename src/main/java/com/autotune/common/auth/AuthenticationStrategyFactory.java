package com.autotune.common.auth;

import com.autotune.utils.KruizeConstants;

public class AuthenticationStrategyFactory {

    public static AuthenticationStrategy createAuthenticationStrategy(AuthenticationConfig authConfig) {
        AuthType type = authConfig.getType();
        switch (type) {
            case NONE:
                return new NoAuthStrategy();
            case BASIC:
                String username = ((BasicAuthCredentials) authConfig.getCredentials()).getUsername();
                String password = ((BasicAuthCredentials) authConfig.getCredentials()).getPassword();
                return new BasicAuthenticationStrategy(username, password);
            case BEARER:
                String tokenFilePath = ((BearerTokenCredentials) authConfig.getCredentials()).getTokenFilePath();
                String token = ((BearerTokenCredentials) authConfig.getCredentials()).getToken();
                return new BearerAuthenticationStrategy(tokenFilePath,token);
            case API_KEY:
                String apiKey = ((ApiKeyCredentials) authConfig.getCredentials()).getApiKey();
                return new APIKeyAuthenticationStrategy(apiKey);
            case OAUTH2:
                String tokenEndpoint = ((OAuth2Credentials) authConfig.getCredentials()).getTokenEndpoint();
                String clientId = ((OAuth2Credentials) authConfig.getCredentials()).getClientId();
                String clientSecret = ((OAuth2Credentials) authConfig.getCredentials()).getClientSecret();
                return new OAuth2AuthenticationStrategy(tokenEndpoint, clientId, clientSecret);
            case MTLS:
                String clientCertPath = ((MTLSCredentials) authConfig.getCredentials()).getClientCertPath();
                String clientKeyPath = ((MTLSCredentials) authConfig.getCredentials()).getClientKeyPath();
                String caCertPath = ((MTLSCredentials) authConfig.getCredentials()).getCaCertPath();
                String keyPassword = ((MTLSCredentials) authConfig.getCredentials()).getKeyPassword();
                return new MTLSAuthenticationStrategy(clientCertPath, clientKeyPath, caCertPath, keyPassword);
            default:
                throw new IllegalArgumentException(KruizeConstants.AuthenticationConstants.UNKNOWN_AUTHENTICATION+ type);
        }
    }
}
