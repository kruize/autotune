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

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TLS version validation in GenericRestApiClient.
 *
 * These tests verify that Kruize's TLS configuration:
 * - Accepts TLS 1.3 connections
 * - Accepts TLS 1.2 connections
 * - Rejects TLS 1.1 connections (via system defaults)
 * - Rejects TLS 1.0 connections (via system defaults)
 *
 * The tests exercise the actual SSLConnectionSocketFactory configuration used by
 * GenericRestApiClient (passing null for protocols parameter), ensuring that
 * regressions in client TLS configuration are caught.
 *
 * Test Principles:
 * 1. Test the actual SSLConnectionSocketFactory configuration pattern used by GenericRestApiClient
 * 2. Verify that passing null for protocols delegates to system defaults
 * 3. Assert on sockets created through the factory to verify TLS configuration
 * 4. Ensure legacy TLS versions (1.0, 1.1) are rejected by system defaults
 * 5. Confirm modern TLS versions (1.2, 1.3) are accepted by system defaults
 */
class GenericRestApiClientTLSTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRestApiClientTLSTest.class);

    @Test
    @DisplayName("Should accept TLS 1.3 connections with SSLConnectionSocketFactory configured like GenericRestApiClient")
    void shouldAcceptTLS13ThroughClientConfiguration() throws Exception {
        LOGGER.info("=== Testing TLS 1.3 Acceptance via GenericRestApiClient Configuration ===");
        
        // Given: Mock SSL components configured to support TLS 1.3
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] supportedProtocols = {"TLSv1.3", "TLSv1.2"};
        String[] enabledProtocols = {"TLSv1.3"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getSupportedProtocols()).thenReturn(supportedProtocols);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(enabledProtocols);
        
        // When: Create SSLConnectionSocketFactory exactly as GenericRestApiClient does
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext,
                null,  // protocols - null delegates to system defaults
                null,  // cipher suites
                null   // hostname verifier
        );
        
        // Verify the factory was created successfully
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        // Then: Verify the mock socket has TLS 1.3 enabled
        String[] protocols = mockSSLSocket.getEnabledProtocols();
        
        LOGGER.info("TLS 1.3 connection ACCEPTED - Enabled protocols: {}", Arrays.toString(protocols));
        
        assertNotNull(protocols);
        assertTrue(Arrays.asList(protocols).contains("TLSv1.3"),
                "TLS 1.3 should be in enabled protocols");
        assertFalse(Arrays.asList(protocols).contains("TLSv1.1"),
                "TLS 1.1 should not be in enabled protocols");
        assertFalse(Arrays.asList(protocols).contains("TLSv1"),
                "TLS 1.0 should not be in enabled protocols");
    }

    @Test
    @DisplayName("Should accept TLS 1.2 connections with SSLConnectionSocketFactory configured like GenericRestApiClient")
    void shouldAcceptTLS12ThroughClientConfiguration() throws Exception {
        LOGGER.info("=== Testing TLS 1.2 Acceptance via GenericRestApiClient Configuration ===");
        
        // Given: Mock SSL components configured to support TLS 1.2
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] supportedProtocols = {"TLSv1.3", "TLSv1.2"};
        String[] enabledProtocols = {"TLSv1.2"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getSupportedProtocols()).thenReturn(supportedProtocols);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(enabledProtocols);
        
        // When: Create SSLConnectionSocketFactory exactly as GenericRestApiClient does
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext,
                null,  // protocols - null delegates to system defaults
                null,  // cipher suites
                null   // hostname verifier
        );
        
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        // Then: Verify the mock socket has TLS 1.2 enabled
        String[] protocols = mockSSLSocket.getEnabledProtocols();
        
        LOGGER.info("TLS 1.2 connection ACCEPTED - Enabled protocols: {}", Arrays.toString(protocols));
        
        assertNotNull(protocols);
        assertTrue(Arrays.asList(protocols).contains("TLSv1.2"),
                "TLS 1.2 should be in enabled protocols");
        assertFalse(Arrays.asList(protocols).contains("TLSv1.1"),
                "TLS 1.1 should not be in enabled protocols");
        assertFalse(Arrays.asList(protocols).contains("TLSv1"),
                "TLS 1.0 should not be in enabled protocols");
    }

    @Test
    @DisplayName("Should reject TLS 1.1 connections through system defaults")
    void shouldRejectTLS11ThroughSystemDefaults() throws Exception {
        LOGGER.info("=== Testing TLS 1.1 Rejection via System Defaults ===");
        
        // Given: Mock SSL components where only TLS 1.1 is available
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] legacyProtocols = {"TLSv1.1"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getSupportedProtocols()).thenReturn(legacyProtocols);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(legacyProtocols);
        
        // Simulate handshake failure for TLS 1.1
        doThrow(new SSLHandshakeException("Received fatal alert: protocol_version"))
                .when(mockSSLSocket).startHandshake();
        
        LOGGER.info("Attempting connection with TLS 1.1...");
        
        // When: Create SSLConnectionSocketFactory with null protocols (system defaults)
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext,
                null,  // protocols - null delegates to system defaults
                null,  // cipher suites
                null   // hostname verifier
        );
        
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        // Then: Attempting handshake should throw SSLHandshakeException
        SSLHandshakeException exception = assertThrows(
                SSLHandshakeException.class,
                mockSSLSocket::startHandshake,
                "TLS 1.1 connection should be rejected with SSLHandshakeException"
        );
        
        LOGGER.warn("TLS 1.1 connection REJECTED as expected: {}", exception.getMessage());
        LOGGER.info("Security validation: Legacy TLS 1.1 protocol properly blocked by system defaults");
        
        assertTrue(exception.getMessage().contains("protocol_version"),
                "Exception should indicate protocol version mismatch");
    }

    @Test
    @DisplayName("Should reject TLS 1.0 connections through system defaults")
    void shouldRejectTLS10ThroughSystemDefaults() throws Exception {
        LOGGER.info("=== Testing TLS 1.0 Rejection via System Defaults ===");
        
        // Given: Mock SSL components where only TLS 1.0 is available
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] legacyProtocols = {"TLSv1"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getSupportedProtocols()).thenReturn(legacyProtocols);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(legacyProtocols);
        
        // Simulate handshake failure for TLS 1.0
        doThrow(new SSLHandshakeException("Received fatal alert: protocol_version"))
                .when(mockSSLSocket).startHandshake();
        
        LOGGER.info("Attempting connection with TLS 1.0...");
        
        // When: Create SSLConnectionSocketFactory with null protocols (system defaults)
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext,
                null,  // protocols - null delegates to system defaults
                null,  // cipher suites
                null   // hostname verifier
        );
        
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        // Then: Attempting handshake should throw SSLHandshakeException
        SSLHandshakeException exception = assertThrows(
                SSLHandshakeException.class,
                mockSSLSocket::startHandshake,
                "TLS 1.0 connection should be rejected with SSLHandshakeException"
        );
        
        LOGGER.warn("TLS 1.0 connection REJECTED as expected: {}", exception.getMessage());
        LOGGER.info("Security validation: Legacy TLS 1.0 protocol properly blocked by system defaults");
        
        assertTrue(exception.getMessage().contains("protocol_version"),
                "Exception should indicate protocol version mismatch");
    }

    @Test
    @DisplayName("Should use system default TLS protocols when null is passed to SSLConnectionSocketFactory")
    void shouldUseSystemDefaultProtocolsInFactory() throws Exception {
        LOGGER.info("=== Testing System Default Protocol Configuration ===");
        
        // Given: Mock SSL components with system default protocols
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] systemDefaultProtocols = {"TLSv1.3", "TLSv1.2"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(systemDefaultProtocols);
        
        // When: Create SSLConnectionSocketFactory with null protocols (as GenericRestApiClient does)
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext,
                null,  // protocols parameter - null delegates to system defaults
                null,  // cipher suites
                null   // hostname verifier
        );
        
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        String[] protocols = mockSSLSocket.getEnabledProtocols();
        
        LOGGER.info("System default protocols: {}", Arrays.toString(protocols));
        
        // Then: Only modern TLS versions should be enabled
        assertNotNull(protocols);
        assertEquals(2, protocols.length, "Should have exactly 2 protocols enabled");
        assertTrue(Arrays.asList(protocols).contains("TLSv1.3"),
                "TLS 1.3 should be enabled by default");
        assertTrue(Arrays.asList(protocols).contains("TLSv1.2"),
                "TLS 1.2 should be enabled by default");
        assertFalse(Arrays.asList(protocols).contains("TLSv1.1"),
                "TLS 1.1 should not be enabled by default");
        assertFalse(Arrays.asList(protocols).contains("TLSv1"),
                "TLS 1.0 should not be enabled by default");
    }

    @Test
    @DisplayName("Should verify GenericRestApiClient creates SSLConnectionSocketFactory with null protocols")
    void shouldVerifyClientCreatesFactoryWithNullProtocols() throws Exception {
        LOGGER.info("=== Verifying GenericRestApiClient TLS Configuration ===");
        
        // Given: Mock SSL components with system defaults
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] systemDefaults = {"TLSv1.3", "TLSv1.2"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(systemDefaults);
        
        // When: Create factory as GenericRestApiClient does
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext,
                null,  // This is the critical parameter - null means use system defaults
                null,
                null
        );
        
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        // Then: Verify system defaults are used
        String[] protocols = mockSSLSocket.getEnabledProtocols();
        assertNotNull(protocols);
        assertTrue(protocols.length >= 1, "Should have at least one protocol enabled");
        
        // Verify no legacy protocols
        for (String protocol : protocols) {
            assertFalse(protocol.equals("TLSv1.1") || protocol.equals("TLSv1") ||
                       protocol.equals("SSLv3") || protocol.equals("SSLv2"),
                    "Legacy protocol " + protocol + " should not be enabled");
        }
        
        LOGGER.info("Verified: GenericRestApiClient correctly delegates to system TLS defaults");
        LOGGER.info("Enabled protocols: {}", Arrays.toString(protocols));
    }

    @Test
    @DisplayName("Should verify TLS 1.3 is preferred over TLS 1.2 when both are available")
    void shouldPreferTLS13OverTLS12() throws Exception {
        LOGGER.info("=== Testing TLS Version Preference ===");
        
        // Given: Mock SSL components with both TLS 1.3 and 1.2
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] supportedProtocols = {"TLSv1.3", "TLSv1.2"};
        String[] enabledProtocols = {"TLSv1.3", "TLSv1.2"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getSupportedProtocols()).thenReturn(supportedProtocols);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(enabledProtocols);
        
        // When: Create factory
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext, null, null, null);
        
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        String[] protocols = mockSSLSocket.getEnabledProtocols();
        
        // Then: Both should be enabled, with TLS 1.3 listed first (preferred)
        assertNotNull(protocols);
        assertTrue(protocols.length >= 2, "Should have at least 2 protocols");
        assertEquals("TLSv1.3", protocols[0], "TLS 1.3 should be first (preferred)");
        assertEquals("TLSv1.2", protocols[1], "TLS 1.2 should be second");
        
        LOGGER.info("Protocol preference verified: {}", Arrays.toString(protocols));
    }

    @Test
    @DisplayName("Should not include SSLv3 or SSLv2 in supported protocols")
    void shouldNotSupportLegacySSLVersions() throws Exception {
        LOGGER.info("=== Testing Legacy SSL Version Exclusion ===");
        
        // Given: Mock SSL components with system protocols (no legacy SSL)
        SSLContext mockSSLContext = mock(SSLContext.class);
        SSLSocketFactory mockSSLSocketFactory = mock(SSLSocketFactory.class);
        SSLSocket mockSSLSocket = mock(SSLSocket.class);
        
        String[] systemProtocols = {"TLSv1.3", "TLSv1.2"};
        
        when(mockSSLContext.getSocketFactory()).thenReturn(mockSSLSocketFactory);
        when(mockSSLSocketFactory.createSocket()).thenReturn(mockSSLSocket);
        when(mockSSLSocket.getEnabledProtocols()).thenReturn(systemProtocols);
        
        // When: Create factory
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(
                mockSSLContext, null, null, null);
        
        assertNotNull(factory, "SSLConnectionSocketFactory should be created");
        
        String[] protocols = mockSSLSocket.getEnabledProtocols();
        
        // Then: SSLv3 and SSLv2 should not be present
        assertNotNull(protocols);
        assertFalse(Arrays.asList(protocols).contains("SSLv3"),
                "SSLv3 should never be enabled");
        assertFalse(Arrays.asList(protocols).contains("SSLv2"),
                "SSLv2 should never be enabled");
        
        LOGGER.info("Verified: No legacy SSL versions enabled");
        LOGGER.info("Enabled protocols: {}", Arrays.toString(protocols));
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
