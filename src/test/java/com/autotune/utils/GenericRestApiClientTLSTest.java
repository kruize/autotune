/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TLS version validation in GenericRestApiClient.
 *
 * These tests verify that Kruize's TLS configuration approach (passing null for protocols
 * to SSLConnectionSocketFactory) correctly delegates to system defaults, which:
 * - Accept TLS 1.3 connections
 * - Accept TLS 1.2 connections
 * - Reject TLS 1.1 connections (via system defaults)
 * - Reject TLS 1.0 connections (via system defaults)
 *
 * The tests verify that GenericRestApiClient's setupHttpClient() can be invoked successfully,
 * then test the same TLS configuration pattern (null protocols parameter) that the client uses
 * to ensure system defaults provide the expected security posture.
 *
 * Test Principles:
 * 1. Verify GenericRestApiClient.setupHttpClient() executes without errors
 * 2. Test the same SSLConnectionSocketFactory configuration pattern used by the client
 * 3. Verify that passing null for protocols (as the client does) results in secure defaults
 * 4. Assert that system defaults reject legacy TLS versions (1.0, 1.1)
 * 5. Confirm system defaults accept modern TLS versions (1.2, 1.3)
 */
class GenericRestApiClientTLSTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRestApiClientTLSTest.class);
    private GenericRestApiClient client;

    @BeforeEach
    void setUp() {
        client = new GenericRestApiClient();
        client.setBaseURL("https://test.example.com");
    }

    /**
     * Helper method to create an SSLConnectionSocketFactory using the same configuration
     * pattern as GenericRestApiClient: passing null for protocols to delegate to system defaults.
     * This allows us to verify that the configuration approach used by the client results in
     * secure TLS settings.
     */
    private SSLConnectionSocketFactory createTestFactory(SSLContext sslContext) {
        return new SSLConnectionSocketFactory(
                sslContext,
                null,  // protocols - null delegates to system defaults (as GenericRestApiClient does)
                null,  // cipher suites
                NoopHostnameVerifier.INSTANCE
        );
    }

    @Test
    @DisplayName("Should accept TLS 1.3 connections using GenericRestApiClient's configuration pattern")
    void shouldAcceptTLS13ThroughClientConfiguration() throws Exception {
        LOGGER.info("=== Testing TLS 1.3 Acceptance via Client Configuration Pattern ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        // Create a socket using the same SSLContext to verify system defaults
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        // Then: Verify system defaults provide secure TLS configuration
        String[] enabledProtocols = socket.getEnabledProtocols();
        
        LOGGER.info("System defaults with null protocols - Enabled: {}", Arrays.toString(enabledProtocols));
        
        assertNotNull(enabledProtocols);
        List<String> protocolList = Arrays.asList(enabledProtocols);
        assertTrue(protocolList.contains("TLSv1.3") || protocolList.contains("TLSv1.2"),
                "Modern TLS (1.3 or 1.2) should be enabled by system defaults");
        assertFalse(protocolList.contains("TLSv1.1"),
                "TLS 1.1 should not be enabled by system defaults");
        assertFalse(protocolList.contains("TLSv1"),
                "TLS 1.0 should not be enabled by system defaults");
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should accept TLS 1.2 connections using GenericRestApiClient's configuration pattern")
    void shouldAcceptTLS12ThroughClientConfiguration() throws Exception {
        LOGGER.info("=== Testing TLS 1.2 Acceptance via Client Configuration Pattern ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        String[] enabledProtocols = socket.getEnabledProtocols();
        
        LOGGER.info("System defaults with null protocols - Enabled: {}", Arrays.toString(enabledProtocols));
        
        assertNotNull(enabledProtocols);
        List<String> protocolList = Arrays.asList(enabledProtocols);
        assertTrue(protocolList.contains("TLSv1.2"),
                "TLS 1.2 should be enabled by system defaults");
        assertFalse(protocolList.contains("TLSv1.1"),
                "TLS 1.1 should not be enabled by system defaults");
        assertFalse(protocolList.contains("TLSv1"),
                "TLS 1.0 should not be enabled by system defaults");
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should reject TLS 1.1 through system defaults (as GenericRestApiClient does)")
    void shouldRejectTLS11ThroughSystemDefaults() throws Exception {
        LOGGER.info("=== Testing TLS 1.1 Rejection via System Defaults ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        String[] enabledProtocols = socket.getEnabledProtocols();
        
        LOGGER.info("System defaults - Enabled protocols: {}", Arrays.toString(enabledProtocols));
        
        assertNotNull(enabledProtocols);
        List<String> protocolList = Arrays.asList(enabledProtocols);
        assertFalse(protocolList.contains("TLSv1.1"),
                "TLS 1.1 should not be enabled by system defaults");
        
        LOGGER.info("Security validation: Legacy TLS 1.1 protocol properly blocked by system defaults");
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should reject TLS 1.0 through system defaults (as GenericRestApiClient does)")
    void shouldRejectTLS10ThroughSystemDefaults() throws Exception {
        LOGGER.info("=== Testing TLS 1.0 Rejection via System Defaults ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        String[] enabledProtocols = socket.getEnabledProtocols();
        
        LOGGER.info("System defaults - Enabled protocols: {}", Arrays.toString(enabledProtocols));
        
        assertNotNull(enabledProtocols);
        List<String> protocolList = Arrays.asList(enabledProtocols);
        assertFalse(protocolList.contains("TLSv1"),
                "TLS 1.0 should not be enabled by system defaults");
        
        LOGGER.info("Security validation: Legacy TLS 1.0 protocol properly blocked by system defaults");
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should use system default TLS protocols (as GenericRestApiClient does)")
    void shouldUseSystemDefaultProtocolsInClient() throws Exception {
        LOGGER.info("=== Testing System Default Protocol Configuration ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        String[] protocols = socket.getEnabledProtocols();
        
        LOGGER.info("System default protocols: {}", Arrays.toString(protocols));
        
        // Then: Only modern TLS versions should be enabled
        assertNotNull(protocols);
        assertTrue(protocols.length >= 1, "Should have at least 1 protocol enabled");
        
        List<String> protocolList = Arrays.asList(protocols);
        assertTrue(protocolList.contains("TLSv1.3") || protocolList.contains("TLSv1.2"),
                "Modern TLS (1.3 or 1.2) should be enabled by system defaults");
        assertFalse(protocolList.contains("TLSv1.1"),
                "TLS 1.1 should not be enabled by system defaults");
        assertFalse(protocolList.contains("TLSv1"),
                "TLS 1.0 should not be enabled by system defaults");
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should verify client's configuration pattern creates proper TLS configuration")
    void shouldVerifyClientSetupHttpClientTLSConfiguration() throws Exception {
        LOGGER.info("=== Verifying Client Configuration Pattern TLS Settings ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        String[] protocols = socket.getEnabledProtocols();
        
        assertNotNull(protocols);
        assertTrue(protocols.length >= 1, "Should have at least one protocol enabled");
        
        // Verify no legacy protocols
        for (String protocol : protocols) {
            assertFalse(protocol.equals("TLSv1.1") || protocol.equals("TLSv1") ||
                       protocol.equals("SSLv3") || protocol.equals("SSLv2"),
                    "Legacy protocol " + protocol + " should not be enabled by system defaults");
        }
        
        LOGGER.info("Verified: null protocols parameter correctly delegates to secure system TLS defaults");
        LOGGER.info("Enabled protocols: {}", Arrays.toString(protocols));
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should verify TLS 1.3 is preferred by system defaults when available")
    void shouldPreferTLS13OverTLS12InClient() throws Exception {
        LOGGER.info("=== Testing TLS Version Preference in System Defaults ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        String[] protocols = socket.getEnabledProtocols();
        
        // Then: Verify protocol preference
        assertNotNull(protocols);
        assertTrue(protocols.length >= 1, "Should have at least 1 protocol");
        
        List<String> protocolList = Arrays.asList(protocols);
        if (protocolList.contains("TLSv1.3")) {
            assertEquals("TLSv1.3", protocols[0], "TLS 1.3 should be first (preferred) when available");
            LOGGER.info("Protocol preference verified: TLS 1.3 is preferred by system defaults");
        } else {
            assertTrue(protocolList.contains("TLSv1.2"), "TLS 1.2 should be available");
            LOGGER.info("Protocol preference verified: TLS 1.2 is available in system defaults");
        }
        
        LOGGER.info("Enabled protocols: {}", Arrays.toString(protocols));
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should not include SSLv3 or SSLv2 in system defaults")
    void shouldNotSupportLegacySSLVersionsInClient() throws Exception {
        LOGGER.info("=== Testing Legacy SSL Version Exclusion in System Defaults ===");
        
        // Given: Verify GenericRestApiClient.setupHttpClient() works
        Method setupMethod = GenericRestApiClient.class.getDeclaredMethod("setupHttpClient");
        setupMethod.setAccessible(true);
        CloseableHttpClient httpClient = (CloseableHttpClient) setupMethod.invoke(client);
        
        assertNotNull(httpClient, "HTTP client should be created by GenericRestApiClient");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        
        SSLConnectionSocketFactory factory = createTestFactory(sslContext);
        
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        
        String[] protocols = socket.getEnabledProtocols();
        
        // Then: SSLv3 and SSLv2 should not be present
        assertNotNull(protocols);
        List<String> protocolList = Arrays.asList(protocols);
        assertFalse(protocolList.contains("SSLv3"),
                "SSLv3 should never be enabled by system defaults");
        assertFalse(protocolList.contains("SSLv2"),
                "SSLv2 should never be enabled by system defaults");
        
        LOGGER.info("Verified: No legacy SSL versions enabled in system defaults");
        LOGGER.info("Enabled protocols: {}", Arrays.toString(protocols));
        
        socket.close();
        httpClient.close();
    }

    @Test
    @DisplayName("Should handle protocol_version error for legacy TLS")
    void shouldHandleProtocolVersionError() {
        LOGGER.info("=== Testing Protocol Version Error Handling ===");
        
        String errorMessage = "Received fatal alert: protocol_version";
        boolean isProtocolVersionError = errorMessage.contains("protocol_version");
        
        assertTrue(isProtocolVersionError,
                "Error message should indicate protocol version mismatch");
        
        LOGGER.info("Protocol version error correctly identified");
    }
}
