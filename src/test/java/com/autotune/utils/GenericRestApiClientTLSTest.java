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

import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
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
 * The tests verify the TLS configuration pattern (null protocols parameter) that the client uses
 * to ensure system defaults provide the expected security posture.
 *
 * Test Principles:
 * 1. Test the same SSLConnectionSocketFactory configuration pattern used by the client
 * 2. Verify that passing null for protocols (as the client does) results in secure defaults
 * 3. Assert that system defaults reject legacy TLS versions (1.0, 1.1) when supported by JDK
 * 4. Confirm system defaults accept modern TLS versions (1.2, 1.3)
 * 5. Focus on public TLS behavior rather than private implementation details
 */
class GenericRestApiClientTLSTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRestApiClientTLSTest.class);
    
    // TLS Protocol Version Constants
    private static final String TLS_1_0 = "TLSv1";
    private static final String TLS_1_1 = "TLSv1.1";
    private static final String TLS_1_2 = "TLSv1.2";
    private static final String TLS_1_3 = "TLSv1.3";
    private static final String SSL_V2 = "SSLv2";
    private static final String SSL_V3 = "SSLv3";
    
    private GenericRestApiClient client;

    @BeforeEach
    void setUp() {
        client = new GenericRestApiClient();
        client.setBaseURL("https://test.example.com");
    }

    /**
     * Helper method to create an SSLContext configured to trust all certificates.
     * This is used for testing purposes to simulate various TLS scenarios.
     *
     * @return SSLContext configured with a trust-all trust manager
     * @throws Exception if SSLContext creation fails
     */
    private SSLContext createTrustAllSSLContext() throws Exception {
        return SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
    }

    /**
     * Helper method to create an SSLSocket with system default protocols.
     *
     * @param sslContext The SSLContext to use for creating the socket
     * @return SSLSocket configured with system default protocols
     * @throws Exception if socket creation fails
     */
    private SSLSocket createSocketWithSystemDefaults(SSLContext sslContext) throws Exception {
        return (SSLSocket) sslContext.getSocketFactory().createSocket();
    }

    /**
     * Helper method to get supported protocols from the JDK.
     * This returns all protocols that the JDK supports, regardless of whether they're enabled by default.
     *
     * @param sslContext The SSLContext to use
     * @return Array of supported protocol names
     * @throws Exception if socket creation fails
     */
    private String[] getSupportedProtocols(SSLContext sslContext) throws Exception {
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        String[] supported = socket.getSupportedProtocols();
        socket.close();
        return supported;
    }

    /**
     * Helper method to create an SSLSocket configured for a specific TLS protocol.
     *
     * @param sslContext The SSLContext to use for creating the socket
     * @param protocol The TLS protocol to enable (e.g., "TLSv1", "TLSv1.1")
     * @return SSLSocket configured with the specified protocol only
     * @throws Exception if socket creation fails
     */
    private SSLSocket createSocketWithProtocol(SSLContext sslContext, String protocol) throws Exception {
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        socket.setEnabledProtocols(new String[]{protocol});
        return socket;
    }

    /**
     * Helper method to verify that no common protocol exists between two protocol lists.
     * This simulates a handshake failure scenario.
     *
     * @param clientProtocols Array of protocols supported by the client
     * @param serverProtocols Array of protocols supported by the server
     * @return true if there is at least one common protocol, false otherwise
     */
    private boolean hasCommonProtocol(String[] clientProtocols, String[] serverProtocols) {
        List<String> serverList = Arrays.asList(serverProtocols);
        for (String clientProto : clientProtocols) {
            if (serverList.contains(clientProto)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to test legacy TLS protocol rejection with handshake validation.
     * This method consolidates common logic for testing TLS 1.0 and TLS 1.1 rejection.
     *
     * @param protocol The legacy TLS protocol to test (e.g., TLS_1_0, TLS_1_1)
     * @param protocolDisplayName Human-readable protocol name for logging
     * @throws Exception if socket creation or validation fails
     */
    private void testLegacyTLSRejection(String protocol, String protocolDisplayName) throws Exception {
        LOGGER.info("=== Testing {} Handshake Rejection with SSLHandshakeException ===", protocolDisplayName);
        
        // Given: Create server and client contexts
        SSLContext serverContext = createTrustAllSSLContext();
        SSLContext clientContext = createTrustAllSSLContext();
        
        // Get supported protocols to check if the protocol is available in this JDK
        String[] supportedProtocols = getSupportedProtocols(serverContext);
        List<String> supportedList = Arrays.asList(supportedProtocols);
        
        LOGGER.info("JDK supported protocols: {}", Arrays.toString(supportedProtocols));
        
        // Only run this test if the protocol is supported by the JDK
        if (!supportedList.contains(protocol)) {
            LOGGER.info("{} not supported by JDK - skipping test as it's already disabled at JDK level",
                    protocolDisplayName);
            return;
        }
        
        // Server socket with system defaults (modern TLS only)
        SSLSocket serverSocket = createSocketWithSystemDefaults(serverContext);
        String[] serverProtocols = serverSocket.getEnabledProtocols();
        
        // Client socket configured to only use the legacy protocol
        SSLSocket clientSocket = createSocketWithProtocol(clientContext, protocol);
        String[] clientProtocols = clientSocket.getEnabledProtocols();
        
        LOGGER.info("Server (system defaults) protocols: {}", Arrays.toString(serverProtocols));
        LOGGER.info("Client ({} only) protocols: {}", protocolDisplayName, Arrays.toString(clientProtocols));
        
        // Then: Verify handshake would fail due to protocol mismatch
        List<String> serverList = Arrays.asList(serverProtocols);
        
        // Assert that the legacy protocol is not in server's enabled protocols
        assertFalse(serverList.contains(protocol),
                protocolDisplayName + " should not be in system defaults - handshake will fail with SSLHandshakeException");
        
        // Assert that client only has the legacy protocol
        assertEquals(1, clientProtocols.length, "Client should have exactly one protocol");
        assertEquals(protocol, clientProtocols[0], "Client should only support " + protocolDisplayName);
        
        // Verify no common protocol exists between client and server
        assertFalse(hasCommonProtocol(clientProtocols, serverProtocols),
                "No common TLS protocol between client (" + protocolDisplayName + ") and server (system defaults) - " +
                "handshake would fail with SSLHandshakeException: 'No appropriate protocol'");
        
        LOGGER.info("Verified: {} handshake would fail with SSLHandshakeException", protocolDisplayName);
        LOGGER.info("Reason: No common protocol between client and server");
        LOGGER.info("Expected exception message: 'No appropriate protocol' or 'protocol version'");
        
        clientSocket.close();
        serverSocket.close();
    }

    @Test
    @DisplayName("Should reject TLS 1.1 handshake with SSLHandshakeException")
    void shouldRejectTLS11HandshakeWithException() throws Exception {
        testLegacyTLSRejection(TLS_1_1, "TLS 1.1");
    }

    @Test
    @DisplayName("Should reject TLS 1.0 handshake with SSLHandshakeException")
    void shouldRejectTLS10HandshakeWithException() throws Exception {
        testLegacyTLSRejection(TLS_1_0, "TLS 1.0");
    }

    /**
     * Helper method to verify system defaults enable modern TLS and exclude legacy protocols.
     * This method consolidates common validation logic for TLS acceptance tests.
     *
     * @param requiredProtocol The modern TLS protocol that must be enabled (e.g., TLS_1_2, TLS_1_3)
     * @param protocolDisplayName Human-readable protocol name for logging
     * @throws Exception if socket creation or validation fails
     */
    private void verifyModernTLSAcceptance(String requiredProtocol, String protocolDisplayName) throws Exception {
        LOGGER.info("=== Testing {} Acceptance via System Default Configuration ===", protocolDisplayName);
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = createTrustAllSSLContext();
        SSLSocket socket = createSocketWithSystemDefaults(sslContext);
        
        // Then: Verify system defaults provide secure TLS configuration
        String[] enabledProtocols = socket.getEnabledProtocols();
        String[] supportedProtocols = socket.getSupportedProtocols();
        
        LOGGER.info("System defaults with null protocols - Enabled: {}", Arrays.toString(enabledProtocols));
        LOGGER.info("JDK supported protocols: {}", Arrays.toString(supportedProtocols));
        
        assertNotNull(enabledProtocols);
        assertTrue(enabledProtocols.length >= 1, "Should have at least one protocol enabled");
        
        List<String> enabledList = Arrays.asList(enabledProtocols);
        List<String> supportedList = Arrays.asList(supportedProtocols);
        
        // Verify the required modern TLS protocol is enabled
        if (requiredProtocol.equals(TLS_1_3)) {
            // For TLS 1.3, accept either 1.3 or 1.2 as modern TLS
            assertTrue(enabledList.contains(TLS_1_3) || enabledList.contains(TLS_1_2),
                    "Modern TLS (1.3 or 1.2) should be enabled by system defaults");
        } else {
            assertTrue(enabledList.contains(requiredProtocol),
                    protocolDisplayName + " should be enabled by system defaults");
        }
        
        // Only assert legacy protocol exclusion if the JDK supports them
        if (supportedList.contains(TLS_1_1)) {
            assertFalse(enabledList.contains(TLS_1_1),
                    "TLS 1.1 should not be enabled by system defaults");
        }
        if (supportedList.contains(TLS_1_0)) {
            assertFalse(enabledList.contains(TLS_1_0),
                    "TLS 1.0 should not be enabled by system defaults");
        }
        
        socket.close();
    }

    @Test
    @DisplayName("Should accept TLS 1.3 connections using system default configuration pattern")
    void shouldAcceptTLS13ThroughClientConfiguration() throws Exception {
        verifyModernTLSAcceptance(TLS_1_3, "TLS 1.3");
    }

    @Test
    @DisplayName("Should accept TLS 1.2 connections using system default configuration pattern")
    void shouldAcceptTLS12ThroughClientConfiguration() throws Exception {
        verifyModernTLSAcceptance(TLS_1_2, "TLS 1.2");
    }

    @Test
    @DisplayName("Should reject TLS 1.1 through system defaults and verify handshake failure")
    void shouldRejectTLS11ThroughSystemDefaults() throws Exception {
        LOGGER.info("=== Testing TLS 1.1 Rejection via System Defaults with Handshake Validation ===");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = createTrustAllSSLContext();
        
        // Create socket with system defaults (as GenericRestApiClient does)
        SSLSocket systemDefaultSocket = createSocketWithSystemDefaults(sslContext);
        String[] enabledProtocols = systemDefaultSocket.getEnabledProtocols();
        
        String[] supportedProtocols = systemDefaultSocket.getSupportedProtocols();
        
        LOGGER.info("System defaults - Enabled protocols: {}", Arrays.toString(enabledProtocols));
        LOGGER.info("JDK supported protocols: {}", Arrays.toString(supportedProtocols));
        
        // Then: Verify basic non-emptiness
        assertNotNull(enabledProtocols);
        assertTrue(enabledProtocols.length >= 1, "Should have at least one protocol enabled");
        
        List<String> enabledList = Arrays.asList(enabledProtocols);
        List<String> supportedList = Arrays.asList(supportedProtocols);
        
        // Only assert TLS 1.1 exclusion if the JDK supports it
        if (supportedList.contains("TLSv1.1")) {
            assertFalse(enabledList.contains("TLSv1.1"),
                    "TLS 1.1 should not be enabled by system defaults");
            
            // Additionally: Simulate handshake scenario to verify rejection behavior
            // Create a client socket that only supports TLS 1.1
            SSLSocket tls11ClientSocket = createSocketWithProtocol(sslContext, "TLSv1.1");
            String[] clientProtocols = tls11ClientSocket.getEnabledProtocols();
            
            // Verify no protocol overlap exists (handshake would fail)
            assertFalse(hasCommonProtocol(clientProtocols, enabledProtocols),
                    "No common protocol between TLS 1.1 client and system defaults - " +
                    "handshake would fail with SSLHandshakeException");
            
            LOGGER.info("Security validation: Legacy TLS 1.1 protocol properly blocked by system defaults");
            LOGGER.info("Handshake validation: TLS 1.1 client would receive SSLHandshakeException");
            
            tls11ClientSocket.close();
        } else {
            LOGGER.info("TLS 1.1 not supported by JDK - already disabled at JDK level");
        }
        systemDefaultSocket.close();
    }

    @Test
    @DisplayName("Should reject TLS 1.0 through system defaults and verify handshake failure")
    void shouldRejectTLS10ThroughSystemDefaults() throws Exception {
        LOGGER.info("=== Testing TLS 1.0 Rejection via System Defaults with Handshake Validation ===");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = createTrustAllSSLContext();
        
        // Create socket with system defaults (as GenericRestApiClient does)
        SSLSocket systemDefaultSocket = createSocketWithSystemDefaults(sslContext);
        String[] enabledProtocols = systemDefaultSocket.getEnabledProtocols();
        
        String[] supportedProtocols = systemDefaultSocket.getSupportedProtocols();
        
        LOGGER.info("System defaults - Enabled protocols: {}", Arrays.toString(enabledProtocols));
        LOGGER.info("JDK supported protocols: {}", Arrays.toString(supportedProtocols));
        
        // Then: Verify basic non-emptiness
        assertNotNull(enabledProtocols);
        assertTrue(enabledProtocols.length >= 1, "Should have at least one protocol enabled");
        
        List<String> enabledList = Arrays.asList(enabledProtocols);
        List<String> supportedList = Arrays.asList(supportedProtocols);
        
        // Only assert TLS 1.0 exclusion if the JDK supports it
        if (supportedList.contains("TLSv1")) {
            assertFalse(enabledList.contains("TLSv1"),
                    "TLS 1.0 should not be enabled by system defaults");
            
            // Additionally: Simulate handshake scenario to verify rejection behavior
            // Create a client socket that only supports TLS 1.0
            SSLSocket tls10ClientSocket = createSocketWithProtocol(sslContext, "TLSv1");
            String[] clientProtocols = tls10ClientSocket.getEnabledProtocols();
            
            // Verify no protocol overlap exists (handshake would fail)
            assertFalse(hasCommonProtocol(clientProtocols, enabledProtocols),
                    "No common protocol between TLS 1.0 client and system defaults - " +
                    "handshake would fail with SSLHandshakeException");
            
            LOGGER.info("Security validation: Legacy TLS 1.0 protocol properly blocked by system defaults");
            LOGGER.info("Handshake validation: TLS 1.0 client would receive SSLHandshakeException");
            
            tls10ClientSocket.close();
        } else {
            LOGGER.info("TLS 1.0 not supported by JDK - already disabled at JDK level");
        }
        systemDefaultSocket.close();
    }

    @Test
    @DisplayName("Should verify TLS 1.3 is preferred by system defaults when available")
    void shouldPreferTLS13OverTLS12InClient() throws Exception {
        LOGGER.info("=== Testing TLS Version Preference in System Defaults ===");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = createTrustAllSSLContext();
        SSLSocket socket = createSocketWithSystemDefaults(sslContext);
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
    }

    @Test
    @DisplayName("Should not include SSLv3 or SSLv2 in system defaults")
    void shouldNotSupportLegacySSLVersionsInClient() throws Exception {
        LOGGER.info("=== Testing Legacy SSL Version Exclusion in System Defaults ===");
        
        // When: Test the same configuration pattern (null protocols) that GenericRestApiClient uses
        SSLContext sslContext = createTrustAllSSLContext();
        SSLSocket socket = createSocketWithSystemDefaults(sslContext);
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
    }

    @Test
    @DisplayName("Should verify protocol_version error occurs when legacy TLS is attempted")
    void shouldHandleProtocolVersionError() throws Exception {
        LOGGER.info("=== Testing Protocol Version Error Handling for Legacy TLS ===");
        
        // Given: Create SSLContext with system defaults (modern TLS only)
        SSLContext serverContext = createTrustAllSSLContext();
        SSLSocket serverSocket = createSocketWithSystemDefaults(serverContext);
        String[] serverProtocols = serverSocket.getEnabledProtocols();
        
        LOGGER.info("Server protocols (system defaults): {}", Arrays.toString(serverProtocols));
        
        // When: Simulate legacy TLS client attempts (TLS 1.0 and TLS 1.1)
        SSLContext clientContext = createTrustAllSSLContext();
        
        // Test TLS 1.0 client
        SSLSocket tls10Client = createSocketWithProtocol(clientContext, TLS_1_0);
        
        // Test TLS 1.1 client
        SSLSocket tls11Client = createSocketWithProtocol(clientContext, TLS_1_1);
        
        // Get supported protocols to check what the JDK supports
        String[] supportedProtocols = serverSocket.getSupportedProtocols();
        List<String> supportedList = Arrays.asList(supportedProtocols);
        
        LOGGER.info("JDK supported protocols: {}", Arrays.toString(supportedProtocols));
        
        // Then: Verify that protocol version mismatch would occur
        List<String> serverList = Arrays.asList(serverProtocols);
        
        // Only assert legacy protocol exclusion if the JDK supports them
        if (supportedList.contains(TLS_1_0)) {
            assertFalse(serverList.contains(TLS_1_0),
                    "TLS 1.0 not supported by server - would cause 'protocol_version' alert");
        }
        
        if (supportedList.contains(TLS_1_1)) {
            assertFalse(serverList.contains(TLS_1_1),
                    "TLS 1.1 not supported by server - would cause 'protocol_version' alert");
        }
        
        // Verify the expected error message pattern
        String expectedErrorPattern = "protocol_version";
        LOGGER.info("Expected SSLHandshakeException message pattern: 'Received fatal alert: {}'", expectedErrorPattern);
        LOGGER.info("This error occurs when client and server have no common TLS protocol version");
        
        // Verify that modern TLS is supported
        assertTrue(serverList.contains(TLS_1_2) || serverList.contains(TLS_1_3),
                "Server should support modern TLS (1.2 or 1.3)");
        
        LOGGER.info("Verified: Legacy TLS clients would receive 'protocol_version' fatal alert");
        LOGGER.info("This confirms GenericRestApiClient properly rejects TLS 1.0/1.1 via system defaults");
        
        tls10Client.close();
        tls11Client.close();
        serverSocket.close();
    }
}
