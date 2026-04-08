/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.autotune.common.auth;

import com.autotune.utils.KruizeConstants;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * MTLSAuthenticationStrategy handles mutual TLS authentication.
 * Unlike other authentication strategies that return an Authorization header,
 * mTLS authentication is configured at the SSL/TLS layer through the HTTP client.
 * This strategy stores the certificate paths and provides them to the HTTP client
 * for SSL context configuration.
 */
public class MTLSAuthenticationStrategy implements AuthenticationStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MTLSAuthenticationStrategy.class);
    
    private final String clientCertPath;
    private final String clientKeyPath;
    private final String caCertPath;
    private final String keyPassword;

    public MTLSAuthenticationStrategy(String clientCertPath, String clientKeyPath, String caCertPath, String keyPassword) {
        this.clientCertPath = clientCertPath;
        this.clientKeyPath = clientKeyPath;
        this.caCertPath = caCertPath;
        this.keyPassword = keyPassword;
        
        LOGGER.debug("MTLSAuthenticationStrategy initialized with clientCert: {}, clientKey: {}, caCert: {}",
                     clientCertPath, clientKeyPath, caCertPath);
    }

    /**
     * For mTLS, authentication is handled at the SSL/TLS layer, not via HTTP headers.
     * This method returns null as no Authorization header is needed.
     * The actual authentication happens when the HTTP client is configured with
     * the client certificates.
     *
     * @return null (no Authorization header for mTLS)
     */
    @Override
    public String applyAuthentication() {
        // mTLS authentication is handled at the SSL/TLS layer, not via headers
        return null;
    }

    /**
     * Creates an SSL context configured for mutual TLS authentication.
     * Loads the client certificate, private key, and optionally a CA certificate.
     *
     * @return Configured SSLContext for mTLS
     * @throws Exception if certificate loading or SSL context creation fails
     */
    public SSLContext createSSLContext() throws Exception {
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

    public String getClientCertPath() {
        return clientCertPath;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public String getCaCertPath() {
        return caCertPath;
    }

    public String getKeyPassword() {
        return keyPassword;
    }
}
