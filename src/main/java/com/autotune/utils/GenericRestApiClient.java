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

import com.autotune.common.auth.AuthenticationStrategy;
import com.autotune.common.auth.AuthenticationStrategyFactory;
import com.autotune.common.auth.MTLSAuthenticationStrategy;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.authModels.APIKeysAuthentication;
import com.autotune.utils.authModels.BasicAuthentication;
import com.autotune.utils.authModels.BearerAccessToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This is generic wrapper class used to retrieve RESTAPI response.
 * This class support following RESTAPI authentication mode
 * Basic , Bearer , APIKey , OAUTH2 , MTLS , NO Auth
 */
public class GenericRestApiClient {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRestApiClient.class);
    private String baseURL;
    private BasicAuthentication basicAuthentication;
    private BearerAccessToken bearerAccessToken;
    private APIKeysAuthentication apiKeysAuthentication;
    private AuthenticationStrategy authenticationStrategy;

    /**
     * constructor to set the authentication based on the datasourceInfo object
     *
     * @param dataSourceInfo object containing the datasource details
     */
    public GenericRestApiClient(DataSourceInfo dataSourceInfo) {
        // TODO: add partial URL as well as part of this constructor
        this.authenticationStrategy = AuthenticationStrategyFactory.createAuthenticationStrategy(
                dataSourceInfo.getAuthenticationConfig());
    }

    public GenericRestApiClient() {
    }

    /**
     * This method appends queryString with baseURL and returns response in JSON using specified authentication.
     *
     * @param methodType  Http methods like GET,POST,PATCH etc
     * @param queryString
     * @return Json object which contains API response.
     * @throws IOException
     */
    public JSONObject fetchMetricsJson(String methodType, String queryString) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String jsonResponse;
        try (CloseableHttpClient httpclient = setupHttpClient()) {

            HttpRequestBase httpRequestBase;
            if (methodType.equalsIgnoreCase("GET")) {
                httpRequestBase = new HttpGet(baseURL + URLEncoder.encode(queryString, StandardCharsets.UTF_8));
            } else {
                throw new UnsupportedOperationException("Unsupported method type: " + methodType);
            }

            // Apply authentication
            applyAuthentication(httpRequestBase);

            LOGGER.debug("Executing Prometheus metrics request: {}", httpRequestBase.getRequestLine());

            // Execute the request and get the HttpResponse
            HttpResponse response = httpclient.execute(httpRequestBase);

            // Get and print the response code
            int responseCode = response.getStatusLine().getStatusCode();
            LOGGER.debug("Response code: {}", responseCode);

            // Get the response body if needed
            jsonResponse = new StringResponseHandler().handleResponse(response);

            // Parse the JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultNode = rootNode.path("data").path("result");
            JsonNode warningsNode = rootNode.path("warnings");

            // Check if the result is empty and if there are specific warnings
            if (resultNode.isArray() && resultNode.size() == 0) {
                for (JsonNode warning : warningsNode) {
                    String warningMessage = warning.asText();
                    if (warningMessage.contains("error reading from server") || warningMessage.contains("Please reduce your request rate")) {
                        LOGGER.warn("Warning detected: {}", warningMessage);
                        throw new IOException(warningMessage);
                    }
                }
            }
        }
        return new JSONObject(jsonResponse);
    }


    /**
     * Common method to setup SSL context for trust-all certificates or mTLS.
     * If the authentication strategy is mTLS, it configures the SSL context with client certificates.
     * Otherwise, it uses a trust-all configuration.
     *
     * @return CloseableHttpClient
     */
    private CloseableHttpClient setupHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslContext;
        
        // Check if mTLS authentication is being used
        if (authenticationStrategy instanceof MTLSAuthenticationStrategy) {
            MTLSAuthenticationStrategy mtlsStrategy = (MTLSAuthenticationStrategy) authenticationStrategy;
            try {
                sslContext = createMTLSContext(mtlsStrategy);
                LOGGER.debug("mTLS SSL context created successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to create mTLS SSL context: {}", e.getMessage(), e);
                throw new KeyManagementException("Failed to setup mTLS: " + e.getMessage(), e);
            }
        } else {
            // Default trust-all configuration for non-mTLS authentication
            sslContext = SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();
        }
        
        SSLConnectionSocketFactory sslConnectionSocketFactory =
                new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1.2", "TLSv1.3"}, null, NoopHostnameVerifier.INSTANCE);
        return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
    }

    /**
     * Creates an SSL context configured for mutual TLS authentication.
     * Loads the client certificate, private key, and optionally a CA certificate.
     *
     * @param mtlsStrategy The mTLS authentication strategy containing certificate paths
     * @return Configured SSLContext for mTLS
     * @throws Exception if certificate loading or SSL context creation fails
     */
    private SSLContext createMTLSContext(MTLSAuthenticationStrategy mtlsStrategy) throws Exception {
        String clientCertPath = mtlsStrategy.getClientCertPath();
        String clientKeyPath = mtlsStrategy.getClientKeyPath();
        String caCertPath = mtlsStrategy.getCaCertPath();
        String keyPassword = mtlsStrategy.getKeyPassword();

        // Load client certificate
        X509Certificate clientCert = loadCertificate(clientCertPath);
        
        // Load private key
        PrivateKey privateKey = loadPrivateKey(clientKeyPath, keyPassword);
        
        // Create KeyStore and add client certificate with private key
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("client", privateKey,
                            (keyPassword != null ? keyPassword.toCharArray() : new char[0]),
                            new X509Certificate[]{clientCert});
        
        // Create TrustStore
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        
        if (caCertPath != null && !caCertPath.isEmpty()) {
            // Load CA certificate if provided
            X509Certificate caCert = loadCertificate(caCertPath);
            trustStore.setCertificateEntry("ca", caCert);
            LOGGER.debug("Loaded CA certificate from: {}", caCertPath);
        } else {
            // If no CA cert provided, trust the client cert itself (self-signed scenario)
            trustStore.setCertificateEntry("client-ca", clientCert);
            LOGGER.debug("No CA certificate provided, using client certificate for trust");
        }
        
        // Build SSL context with both keystore and truststore
        return SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyPassword != null ? keyPassword.toCharArray() : new char[0])
                .loadTrustMaterial(trustStore, null)
                .build();
    }

    /**
     * Loads an X.509 certificate from a PEM file.
     *
     * @param certPath Path to the certificate file
     * @return X509Certificate object
     * @throws Exception if certificate loading fails
     */
    private X509Certificate loadCertificate(String certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        } catch (Exception e) {
            LOGGER.error("Failed to load certificate from {}: {}", certPath, e.getMessage());
            throw new IOException("Failed to load certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a private key from a PEM file.
     * Supports both encrypted and unencrypted PKCS#8 format keys.
     *
     * @param keyPath Path to the private key file
     * @param password Password for encrypted keys (can be null)
     * @return PrivateKey object
     * @throws Exception if key loading fails
     */
    private PrivateKey loadPrivateKey(String keyPath, String password) throws Exception {
        try {
            String keyContent = new String(Files.readAllBytes(Paths.get(keyPath)));
            
            // Remove PEM headers and footers to extract the Base64-encoded key data
            keyContent = keyContent
                    .replace(KruizeConstants.AuthenticationConstants.PEM_PRIVATE_KEY_HEADER, KruizeConstants.AuthenticationConstants.EMPTY_STRING)
                    .replace(KruizeConstants.AuthenticationConstants.PEM_PRIVATE_KEY_FOOTER, KruizeConstants.AuthenticationConstants.EMPTY_STRING)
                    .replace(KruizeConstants.AuthenticationConstants.PEM_RSA_PRIVATE_KEY_HEADER, KruizeConstants.AuthenticationConstants.EMPTY_STRING)
                    .replace(KruizeConstants.AuthenticationConstants.PEM_RSA_PRIVATE_KEY_FOOTER, KruizeConstants.AuthenticationConstants.EMPTY_STRING)
                    .replace(KruizeConstants.AuthenticationConstants.PEM_EC_PRIVATE_KEY_HEADER, KruizeConstants.AuthenticationConstants.EMPTY_STRING)
                    .replace(KruizeConstants.AuthenticationConstants.PEM_EC_PRIVATE_KEY_FOOTER, KruizeConstants.AuthenticationConstants.EMPTY_STRING)
                    .replaceAll(KruizeConstants.AuthenticationConstants.WHITESPACE_REGEX, KruizeConstants.AuthenticationConstants.EMPTY_STRING);
            
            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            
            // Try RSA first, then EC
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception e) {
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                return keyFactory.generatePrivate(keySpec);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load private key from {}: {}", keyPath, e.getMessage());
            throw new IOException("Failed to load private key: " + e.getMessage(), e);
        }
    }

    /**
     * Common method to apply authentication to the HTTP request.
     * For mTLS, authentication is handled at the SSL/TLS layer, so no header is set.
     * For other authentication types, the Authorization header is set.
     *
     * @param httpRequestBase the HTTP request (GET, POST, etc.)
     */
    private void applyAuthentication(HttpRequestBase httpRequestBase) {
        if (authenticationStrategy != null) {
            String authHeader = authenticationStrategy.applyAuthentication();
            // Only set the Authorization header if it's not null (mTLS returns null)
            if (authHeader != null) {
                httpRequestBase.setHeader(KruizeConstants.AuthenticationConstants.AUTHORIZATION, authHeader);
            }
        }
    }

    /**
     * Method to call the Experiment API (e.g., to create an experiment) using POST request.
     *
     * @param payload JSON payload containing the experiment details
     * @return API response code
     * @throws IOException
     */
    public HttpResponseWrapper callKruizeAPI(String payload) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HttpResponseWrapper httpResponseWrapper = null;
        // Create an HTTP client
        try (CloseableHttpClient httpclient = setupHttpClient()) {
            // Prepare the HTTP POST request
            HttpPost httpPost = new HttpPost(baseURL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            // If payload is present, set it in the request body
            if (payload != null) {
                StringEntity entity = new StringEntity(payload, StandardCharsets.UTF_8);
                httpPost.setEntity(entity);
            }
            // Execute the request and return the response code
            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                // Get the status code from the response
                int responseCode = response.getStatusLine().getStatusCode();
                LOGGER.debug("Response code: {}", responseCode);
                if (response.getEntity() != null) {
                    // Convert response entity to string
                    String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                    try {
                        // Attempt to parse as JSON
                        JSONObject json = new JSONObject(responseBody);
                        httpResponseWrapper = new HttpResponseWrapper(responseCode, json);
                    } catch (JSONException e) {
                        // If JSON parsing fails, return as plain string
                        httpResponseWrapper = new HttpResponseWrapper(responseCode, responseBody);
                    }
                }
            }
        }
        return httpResponseWrapper;
    }

    /**
     * Sends an HTTP GET request to the Kruize API and returns the response wrapped in an {@link HttpResponseWrapper}.
     *
     * <p>This method creates an HTTP client, prepares a GET request to the configured base URL,
     * and sets appropriate headers for JSON communication. It then executes the request, retrieves
     * the response, and attempts to parse it as JSON. If JSON parsing fails, the response is returned
     * as a plain string.</p>
     *
     * @param payload The request payload (not currently used in this method).
     * @return An {@link HttpResponseWrapper} containing the response status code and either a JSON object
     *         or a plain string, depending on the response content.
     * @throws IOException If an I/O error occurs while executing the request.
     * @throws NoSuchAlgorithmException If the specified algorithm for SSL context is not available.
     * @throws KeyStoreException If an issue occurs while initializing the key store.
     * @throws KeyManagementException If an issue occurs while managing the SSL keys.
     */
    public HttpResponseWrapper getKruizeAPI(String payload) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HttpResponseWrapper httpResponseWrapper = null;
        // Create an HTTP client
        try (CloseableHttpClient httpclient = setupHttpClient()) {
            // Prepare the HTTP POST request
            HttpGet httpget = new HttpGet(baseURL);
            httpget.setHeader("Content-Type", "application/json");
            httpget.setHeader("Accept", "application/json");
      
            // Execute the request and return the response code
            try (CloseableHttpResponse response = httpclient.execute(httpget)) {
                // Get the status code from the response
                int responseCode = response.getStatusLine().getStatusCode();
                LOGGER.debug("Response code: {}", responseCode);
                if (response.getEntity() != null) {
                    // Convert response entity to string
                    String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                    try {
                        // Attempt to parse as JSON
                        JSONObject json = new JSONObject(responseBody);
                        httpResponseWrapper = new HttpResponseWrapper(responseCode, json);
                    } catch (JSONException e) {
                        // If JSON parsing fails, return as plain string
                        httpResponseWrapper = new HttpResponseWrapper(responseCode, responseBody);
                    }
                }
            }
        }
        return httpResponseWrapper;
    }


    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
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

    public class HttpResponseWrapper {
        private int statusCode;
        private Object responseBody;

        public HttpResponseWrapper(int statusCode, Object responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public Object getResponseBody() {
            return responseBody;
        }

        @Override
        public String toString() {
            return "HttpResponseWrapper{" +
                    "statusCode=" + statusCode +
                    ", responseBody=" + responseBody +
                    '}';
        }
    }
}
