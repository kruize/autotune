package com.autotune.common.auth;

public class Credentials {
    private String grantType;      // OAuth2
    private String clientId;       // OAuth2
    private String clientSecret;   // OAuth2
    private String username;       // Basic auth
    private String password;       // Basic auth
    private String tokenEndpoint;  // OAuth2
    private String tokenFilePath;  // Bearer token
    private String apiKey;         // API key
    private String headerName;     // API key header name

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Credentials() {
    }

    public String getUsername() {
        return username;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getTokenFilePath() {
        return tokenFilePath;
    }

    public String getPassword() {
        return password;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public void setTokenFilePath(String tokenFilePath) {
        this.tokenFilePath = tokenFilePath;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "grantType='" + grantType + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", tokenEndpoint='" + tokenEndpoint + '\'' +
                ", tokenFilePath='" + tokenFilePath + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", headerName='" + headerName + '\'' +
                '}';
    }
}
