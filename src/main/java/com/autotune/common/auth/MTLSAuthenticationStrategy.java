package com.autotune.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
