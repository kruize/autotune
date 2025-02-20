package com.autotune.common.auth;

import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AuthenticationConfig {
    private AuthType type;
    private Credentials credentials;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationConfig.class);

    public AuthenticationConfig(AuthType type, Credentials credentials) {
        this.type = type;
        this.credentials = credentials;
    }

    public AuthenticationConfig() {
    }

    public AuthType getType() {
        return type;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public static AuthenticationConfig createAuthenticationConfigObject(JSONObject authenticationObj) {
        // Parse and map authentication methods if they exist
        if (authenticationObj != null) {
            AuthType type = AuthType.valueOf(authenticationObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TYPE).toUpperCase());
            JSONObject credentialsObj = authenticationObj.getJSONObject(KruizeConstants.AuthenticationConstants.AUTHENTICATION_CREDENTIALS);

            Credentials credentials = null;  // Initialize credentials as null, and create specific subclass instances based on the type
            switch (type) {
                case BASIC:
                    BasicAuthCredentials basicCredentials = new BasicAuthCredentials();
                    basicCredentials.setUsername(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_USERNAME));
                    basicCredentials.setPassword(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_PASSWORD));
                    credentials = basicCredentials;
                    break;
                case BEARER:
                    BearerTokenCredentials bearerCredentials = new BearerTokenCredentials();
                    bearerCredentials.setTokenFilePath(credentialsObj.optString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TOKEN_FILE,null));
                    bearerCredentials.setToken(credentialsObj.optString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TOKEN,null));
                    credentials = bearerCredentials;
                    break;
                case API_KEY:
                    ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
                    apiKeyCredentials.setApiKey(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_API_KEY));
                    apiKeyCredentials.setHeaderName(credentialsObj.optString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_HEADER_NAME, "X-API-Key"));
                    credentials = apiKeyCredentials;
                    break;
                case OAUTH2:
                    OAuth2Credentials oauth2Credentials = new OAuth2Credentials();
                    oauth2Credentials.setTokenEndpoint(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TOKEN_ENDPOINT));
                    oauth2Credentials.setClientId(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_CLIENT_ID));
                    oauth2Credentials.setClientSecret(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_CLIENT_SECRET));
                    oauth2Credentials.setGrantType(credentialsObj.getString(KruizeConstants.AuthenticationConstants.AUTHENTICATION_GRANT_TYPE));
                    credentials = oauth2Credentials;
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
        return new AuthenticationConfig(AuthType.NONE, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationConfig that = (AuthenticationConfig) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(credentials, that.credentials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, credentials);
    }
}
