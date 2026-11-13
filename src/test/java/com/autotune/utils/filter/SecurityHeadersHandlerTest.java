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
package com.autotune.utils.filter;

import com.autotune.analyzer.exceptions.KruizeErrorHandler;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.utils.KruizeConstants;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ZAP scan fixes in the HTTP layer:
 * - {@link SecurityHeadersHandler}: X-Content-Type-Options header and Server header suppression
 * - {@link KruizeErrorHandler}: JSON error body and Content-Type for all HTTP methods
 * - {@link KruizeCORSHandler}: CORS allowed methods
 *
 * The tests run against an in-memory Jetty {@link LocalConnector}, no network port is opened.
 */
class SecurityHeadersHandlerTest {

    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String INTERNAL_DETAIL = "org.hibernate.HibernateException: connection to internal-db:5432 refused";

    private Server server;
    private LocalConnector connector;

    @BeforeEach
    void setUp() throws Exception {
        server = new Server();
        HttpConfiguration httpConfiguration = SecurityHeadersHandler.createHttpConfiguration();
        connector = new LocalConnector(server, new HttpConnectionFactory(httpConfiguration));
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setErrorHandler(new KruizeErrorHandler());
        context.addServlet(new ServletHolder(new TestServlet()), "/ok");
        context.addServlet(new ServletHolder(new TestServlet()), "/bad-request");
        context.addServlet(new ServletHolder(new TestServlet()), "/server-error");

        SecurityHeadersHandler securityHeadersHandler = new SecurityHeadersHandler();
        securityHeadersHandler.setHandler(context);
        server.setHandler(securityHeadersHandler);
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    private HttpTester.Response send(String method, String path) throws Exception {
        String request = method + " " + path + " HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        return HttpTester.parseResponse(connector.getResponse(request));
    }

    @Test
    @DisplayName("HttpConfiguration does not send the Server header")
    void httpConfigurationDisablesServerVersion() {
        assertFalse(SecurityHeadersHandler.createHttpConfiguration().getSendServerVersion());
    }

    @Test
    @DisplayName("Success responses carry X-Content-Type-Options: nosniff and no Server header")
    void successResponseHasSecurityHeaders() throws Exception {
        HttpTester.Response response = send("GET", "/ok");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(KruizeConstants.SecurityHeaders.NOSNIFF, response.get(X_CONTENT_TYPE_OPTIONS));
        assertNull(response.get("Server"), "Server header must not disclose server type/version");
    }

    @Test
    @DisplayName("Unknown path (404) carries X-Content-Type-Options: nosniff and no Server header")
    void notFoundResponseHasSecurityHeaders() throws Exception {
        HttpTester.Response response = send("GET", "/does-not-exist");

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        assertEquals(KruizeConstants.SecurityHeaders.NOSNIFF, response.get(X_CONTENT_TYPE_OPTIONS));
        assertNull(response.get("Server"));
    }

    @Test
    @DisplayName("Error responses for GET/POST/PUT/DELETE have JSON content type, body and security headers")
    void errorResponseIsJsonForAllMethods() throws Exception {
        for (String method : new String[]{"GET", "POST", "PUT", "DELETE"}) {
            HttpTester.Response response = send(method, "/bad-request");

            assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), method);
            String contentType = response.get("Content-Type");
            assertNotNull(contentType, method + " error response must set Content-Type");
            assertTrue(contentType.startsWith("application/json"), method + " unexpected Content-Type: " + contentType);
            assertTrue(response.getContent().contains("\"status\":\"ERROR\""),
                    method + " error body must be the Kruize JSON error response but was: " + response.getContent());
            assertTrue(response.getContent().contains("test error message"), method);
            assertEquals(KruizeConstants.SecurityHeaders.NOSNIFF, response.get(X_CONTENT_TYPE_OPTIONS), method);
            assertNull(response.get("Server"), method);
        }
    }

    @Test
    @DisplayName("Raw messages of server errors (HTTP 5xx) raised by any servlet are never returned to the client")
    void serverErrorMessageIsNotReturnedForAnyMethod() throws Exception {
        for (String method : new String[]{"GET", "POST", "PUT", "DELETE"}) {
            HttpTester.Response response = send(method, "/server-error");

            assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus(), method);
            assertFalse(response.getContent().contains(INTERNAL_DETAIL), method + ": " + response.getContent());
            assertTrue(response.getContent().contains(AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG),
                    method + ": " + response.getContent());
        }
    }

    @Test
    @DisplayName("KruizeErrorHandler generates an error page for every HTTP method")
    void errorHandlerGeneratesErrorPageForAllMethods() {
        KruizeErrorHandler errorHandler = new KruizeErrorHandler();
        for (String method : new String[]{"GET", "POST", "PUT", "DELETE", "HEAD"}) {
            assertTrue(errorHandler.errorPageForMethod(method), method);
        }
    }

    @Test
    @DisplayName("CORS allowed methods include every method used by the Kruize endpoints")
    void corsAllowedMethodsIncludePutAndDelete() {
        CrossOriginHandler handler = KruizeCORSHandler.getHandler();
        assertTrue(handler.getAllowedMethods().containsAll(Set.of("GET", "POST", "PUT", "DELETE")),
                "Allowed methods were: " + handler.getAllowedMethods());
    }

    /**
     * Servlet that replies 200 on /ok and sends an error with a message for every method on /bad-request.
     */
    private static class TestServlet extends HttpServlet {
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (req.getRequestURI().endsWith("/bad-request")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "test error message");
                return;
            }
            if (req.getRequestURI().endsWith("/server-error")) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, INTERNAL_DETAIL);
                return;
            }
            resp.setContentType("application/json");
            resp.getWriter().write("{}");
        }
    }
}
