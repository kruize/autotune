package com.autotune.common.auth;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BearerAuthenticationStrategy implements AuthenticationStrategy {
    private final String tokenFilePath;
    private final String tokenString;

    public BearerAuthenticationStrategy(String tokenFilePath,  String tokenString) {
        this.tokenFilePath = tokenFilePath;
        this.tokenString = tokenString;
    }

    @Override
    public String applyAuthentication() {
        if(null == tokenString) {
            // Read token from file
            try {
                BufferedReader reader = new BufferedReader(new FileReader(tokenFilePath));
                String token = reader.readLine();
                reader.close();
                return "Bearer " + token;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read Bearer token: " + e.getMessage());
            }
        }else{
            return "Bearer " + tokenString;
        }

    }
}
