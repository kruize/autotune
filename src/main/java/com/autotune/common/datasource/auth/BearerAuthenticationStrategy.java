package com.autotune.common.datasource.auth;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BearerAuthenticationStrategy implements AuthenticationStrategy {
    private final String tokenFilePath;

    public BearerAuthenticationStrategy(String tokenFilePath) {
        this.tokenFilePath = tokenFilePath;
    }

    @Override
    public String applyAuthentication() {
        // Read token from file
        try {
            BufferedReader reader = new BufferedReader(new FileReader(tokenFilePath));
            String token = reader.readLine();
            reader.close();
            return "Bearer " + token;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Bearer token: " + e.getMessage());
        }
    }
}
