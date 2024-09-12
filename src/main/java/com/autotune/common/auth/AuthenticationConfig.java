package com.autotune.common.auth;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationConfig {
    private final String type; // "basic", "bearer", "apiKey", "oauth2"
    private final Credentials credentials;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationConfig.class);

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

    public static AuthenticationConfig createAuthenticationConfigObject(JSONObject authenticationObj) {
        // Parse and map authentication methods if they exist
        if (authenticationObj != null) {
            String type = authenticationObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TYPE);
            JSONObject credentialsObj = authenticationObj.getJSONObject(KruizeConstants.AuthenticationConstants.AUTHENTICATION_CREDENTIALS);

            Credentials credentials = new Credentials();
            switch (type.toLowerCase()) {
                case KruizeConstants.AuthenticationConstants.BASIC:
                    credentials.setUsername(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_USERNAME));
                    credentials.setPassword(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_PASSWORD));
                    break;
                case KruizeConstants.AuthenticationConstants.BEARER:
                    credentials.setTokenFilePath(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TOKEN_FILE));
                    break;
                case KruizeConstants.AuthenticationConstants.API_KEY:
                    credentials.setApiKey(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_API_KEY));
                    credentials.setHeaderName(credentialsObj.optString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_HEADER_NAME, "X-API-Key"));
                    break;
                case KruizeConstants.AuthenticationConstants.OAUTH2:
                    credentials.setTokenEndpoint(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TOKEN_ENDPOINT));
                    credentials.setClientId(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_CLIENT_ID));
                    credentials.setClientSecret(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_CLIENT_SECRET));
                    credentials.setGrantType(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_GRANT_TYPE));
                    break;
                default:
                    LOGGER.error(KruizeConstants.AuthenticationConstants.UNKNOWN_AUTHENTICATION + "{}", type);
            }

            return new AuthenticationConfig(type, credentials);
        }
        return noAuth();
    }

    // Static method to return a no-auth config
    public static AuthenticationConfig noAuth() {
        return new AuthenticationConfig(AnalyzerConstants.NONE, null);
    }

    @Override
    public String toString() {
        return "AuthenticationConfig{" +
                "type='" + type + '\'' +
                ", credentials=" + credentials +
                '}';
    }
}