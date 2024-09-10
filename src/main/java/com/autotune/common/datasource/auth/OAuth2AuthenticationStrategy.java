package com.autotune.common.datasource.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OAuth2AuthenticationStrategy implements AuthenticationStrategy {
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;

    public OAuth2AuthenticationStrategy(String tokenEndpoint, String clientId, String clientSecret) {
        this.tokenEndpoint = tokenEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String applyAuthentication() {
        try {
            // Fetch the OAuth2 token using client credentials
            HttpClient client = HttpClient.newHttpClient();

            // Create the request body for the OAuth2 token request
            String form = "grant_type=client_credentials"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret;

            // Send the POST request to fetch the token
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse the token from the response JSON (assuming GitHub returns a JSON)
                String token = parseToken(response.body());
                return "Bearer " + token; // Return the token in Bearer format
            } else {
                throw new RuntimeException("Failed to fetch OAuth2 token: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching OAuth2 token", e);
        }
    }

    // You will need a method to parse the token from the response body
    private String parseToken(String responseBody) {
        // Parse the response and extract the access token.
        // GitHub returns a form-encoded response, so you need to parse the "access_token"
        String[] pairs = responseBody.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue[0].equals("access_token")) {
                return keyValue[1];
            }
        }
        throw new RuntimeException("No access token found in response");
    }
}
