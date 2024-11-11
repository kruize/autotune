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

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.common.auth.AuthenticationStrategy;
import com.autotune.common.auth.AuthenticationStrategyFactory;
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
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second
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

            LOGGER.info("Executing Prometheus metrics request: {}", httpRequestBase.getRequestLine());

            // Execute the request and get the HttpResponse
            HttpResponse response = httpclient.execute(httpRequestBase);

            // Get and print the response code
            int responseCode = response.getStatusLine().getStatusCode();
            LOGGER.info("Response code: {}", responseCode);

            // Get the response body if needed
            jsonResponse = new StringResponseHandler().handleResponse(response);
            LOGGER.info("jsonResponse {}", jsonResponse);

            // Parse the JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultNode = rootNode.path("data").path("result");
            JsonNode warningsNode = rootNode.path("warnings");

            // Check if the result is empty and if there are specific warnings
            if (resultNode.isArray() && resultNode.size() == 0) {
                LOGGER.info("resultNode is empty");
                for (JsonNode warning : warningsNode) {
                    String warningMessage = warning.asText();
                    LOGGER.info("warnings is {}", warningMessage);
                    if (warningMessage.contains("error reading from server") || warningMessage.contains("Please reduce your request rate")) {
                        LOGGER.warn("Warning detected: {}", warningMessage);
                        throw new IOException(warningMessage);
                    } else {
                        LOGGER.info("no warnings detected");
                    }
                }
            } else {
                LOGGER.info("resultNode is not empty");
            }
        }
        return new JSONObject(jsonResponse);
    }


    /**
     * Common method to setup SSL context for trust-all certificates.
     *
     * @return CloseableHttpClient
     */
    private CloseableHttpClient setupHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();  // Trust all certificates
        SSLConnectionSocketFactory sslConnectionSocketFactory =
                new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
        return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
    }

    /**
     * Common method to apply authentication to the HTTP request.
     *
     * @param httpRequestBase the HTTP request (GET, POST, etc.)
     */
    private void applyAuthentication(HttpRequestBase httpRequestBase) {
        if (authenticationStrategy != null) {
            String authHeader = authenticationStrategy.applyAuthentication();
            httpRequestBase.setHeader(KruizeConstants.AuthenticationConstants.AUTHORIZATION, authHeader);
        }
    }

    /**
     * Method to call the Experiment API (e.g., to create an experiment) using POST request.
     *
     * @param payload JSON payload containing the experiment details
     * @return API response code
     * @throws IOException
     */
    public HttpResponseWrapper callKruizeAPI(String payload) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, FetchMetricsError {
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
            } catch (Exception e) {
                LOGGER.error("Error occurred while calling Kruize API: {}", e.getMessage());
                throw new FetchMetricsError(e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while calling Kruize API: {}", e.getMessage());
            throw new FetchMetricsError(e.getMessage());
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
