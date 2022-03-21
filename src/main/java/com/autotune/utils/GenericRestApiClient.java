/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.utils;

import com.autotune.utils.auth_models.APIKeysAuthentication;
import com.autotune.utils.auth_models.BasicAuthentication;
import com.autotune.utils.auth_models.BearerAccessToken;
import com.autotune.utils.auth_models.OAuth2Config;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * This is generic wrapper class used to retrieve RESTAPI response.
 * This class support following RESTAPI authentication mode
 * Basic , Bearer , APIKey , OAUTH2 , NO Auth
 */
public class GenericRestApiClient {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRestApiClient.class);
    private final String baseURL;
    private BasicAuthentication basicAuthentication;
    private BearerAccessToken bearerAccessToken;
    private APIKeysAuthentication apiKeysAuthentication;
    private OAuth2Config oAuth2Config;  //Yet to implement
    private String authHeaderString;

    /**
     * Initializes a new instance just by passing baseURL which does not need any authentication.
     * @param baseURL
     */
    public GenericRestApiClient(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Use this constructor to initializes a new instance if RESTAPI need Basic authentication.
     * @param baseURL
     * @param basicAuthentication
     */
    public GenericRestApiClient(String baseURL, BasicAuthentication basicAuthentication) {
        this.baseURL = baseURL;
        this.basicAuthentication = basicAuthentication;
        this.setAuthHeaderString(this.basicAuthentication.getAuthHeader());
    }

    /**
     * Use this constructor to initializes a new instance if RESTAPI need Bearer authentication.
     * @param baseURL
     * @param bearerAccessToken
     */
    public GenericRestApiClient(String baseURL, BearerAccessToken bearerAccessToken) {
        this.baseURL = baseURL;
        this.bearerAccessToken = bearerAccessToken;
        this.setAuthHeaderString(this.bearerAccessToken.getAuthHeader());
    }

    /**
     * Use this constructor to initializes a new instance if RESTAPI need APIKeys authentication.
     * @param baseURL
     * @param apiKeysAuthentication
     */
    public GenericRestApiClient(String baseURL, APIKeysAuthentication apiKeysAuthentication) {
        this.baseURL = baseURL;
        this.apiKeysAuthentication = apiKeysAuthentication;
        this.setAuthHeaderString(this.apiKeysAuthentication.getAuthHeader());
    }

    /**
     * Use this constructor to initializes a new instance if RESTAPI need OAuth2 authentication.
     * @param baseURL
     * @param oAuth2Config
     */
    public GenericRestApiClient(String baseURL, OAuth2Config oAuth2Config) {
        this.baseURL = baseURL;
        this.oAuth2Config = oAuth2Config;
    }

    public String getAuthHeaderString() {
        return authHeaderString;
    }

    public void setAuthHeaderString(String authHeaderString) {
        this.authHeaderString = authHeaderString;
    }

    /**
     * This methode appends aueryString with baseURL and returns response in JSON using specified authentication.
     * @param methodType    Http methods like GET,POST,PATCH etc
     * @param queryString
     * @return Json object which contains API response.
     * @throws IOException
     */
    public JSONObject fetchMetricsJson(String methodType, String queryString) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        java.lang.System.setProperty("https.protocols", "TLSv1.2");
        String jsonOutputInString = "";
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();  //overriding the standard certificate verification process and trust all certificate chains regardless of their validity
        SSLConnectionSocketFactory sslConnectionSocketFactory =
                new SSLConnectionSocketFactory(sslContext, new String[]
                        {"TLSv1.2" }, null,
                        NoopHostnameVerifier.INSTANCE);
        try (CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build()) {
            HttpRequestBase httpRequestBase = null;
            if (methodType.equalsIgnoreCase("GET")) {
                httpRequestBase = new HttpGet(this.baseURL
                        + URLEncoder.encode(queryString, StandardCharsets.UTF_8)
                );
            }
            if (this.authHeaderString != null && !this.authHeaderString.isEmpty()) {
                httpRequestBase.setHeader("Authorization", this.authHeaderString);
            }
            LOGGER.debug("Executing request " + httpRequestBase.getRequestLine());
            jsonOutputInString = httpclient.execute(httpRequestBase, new StringResponseHandler());

        }
        return new JSONObject(jsonOutputInString);
    }

    private static class StringResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }


    }


}
