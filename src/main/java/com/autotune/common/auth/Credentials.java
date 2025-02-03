package com.autotune.common.auth;

import java.util.Objects;

public abstract class Credentials {
}

class OAuth2Credentials extends Credentials {
    private String grantType;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OAuth2Credentials that = (OAuth2Credentials) o;
        return Objects.equals(grantType, that.grantType) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(clientSecret, that.clientSecret) &&
                Objects.equals(tokenEndpoint, that.tokenEndpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grantType, clientId, clientSecret, tokenEndpoint);
    }
}

class BasicAuthCredentials extends Credentials {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

class BearerTokenCredentials extends Credentials {
    private String tokenFilePath;
    private String token;
    public String getTokenFilePath() {
        return tokenFilePath;
    }

    public String getToken() {
        return token;
    }

    public void setTokenFilePath(String tokenFilePath) {
        this.tokenFilePath = tokenFilePath;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BearerTokenCredentials that = (BearerTokenCredentials) o;
        return Objects.equals(tokenFilePath, that.tokenFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenFilePath);
    }
    @Override
    public String toString() {
        return "BearerTokenCredentials{" +
                "tokenFilePath='" + tokenFilePath + '\'' +
                '}';
    }
}

class ApiKeyCredentials extends Credentials {
    private String apiKey;
    private String headerName;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyCredentials that = (ApiKeyCredentials) o;
        return Objects.equals(apiKey, that.apiKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey);
    }
}

