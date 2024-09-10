package com.autotune.common.datasource.auth;

import org.apache.http.client.methods.HttpRequestBase;

import java.util.Base64;

public class AuthenticationConfig {
    private final String type; // "basic", "bearer", "apiKey", "oauth2"
    private final Credentials credentials;

    public AuthenticationConfig(String type, Credentials credentials) {
        this.type = type;
        this.credentials = credentials;
    }

    public String getType() {
        return type;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void applyAuthentication(HttpRequestBase httpRequestBase) {
        switch (type) {
            case "Basic":
                String basicAuth = Base64.getEncoder().encodeToString((credentials.getUsername() + ":" + credentials.getPassword()).getBytes());
                httpRequestBase.setHeader("Authorization", "Basic " + basicAuth);
                break;
            case "Bearer":
                httpRequestBase.setHeader("Authorization", "Bearer " + credentials.getTokenFilePath());
                break;
            case "APIKey":
                httpRequestBase.setHeader("Authorization", "ApiKey " + credentials.getApiKey());
                break;
            case "OAuth2":
                // Assume the token is already retrieved and set
                httpRequestBase.setHeader("Authorization", "Bearer " + credentials.getTokenFilePath());
                break;
            default:
                throw new IllegalArgumentException("Unsupported authentication type: " + type);
        }
    }

    // Static method to return a no-auth config
    public static AuthenticationConfig noAuth() {
        return new AuthenticationConfig("none", null); // Type "none" or similar to indicate no auth
    }

    @Override
    public String toString() {
        return "AuthenticationConfig{" +
                "type='" + type + '\'' +
                ", credentials=" + credentials +
                '}';
    }
}