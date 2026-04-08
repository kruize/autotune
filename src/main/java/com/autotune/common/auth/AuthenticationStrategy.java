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

import javax.net.ssl.SSLContext;

/**
 * AuthenticationStrategy defines the contract for all authentication mechanisms.
 * It handles both HTTP header-based authentication (Basic, Bearer, API Key, OAuth2)
 * and SSL/TLS-based authentication (mTLS).
 */
public interface AuthenticationStrategy {
    /**
     * Returns the authentication header value for HTTP-based authentication.
     * For SSL/TLS-based authentication (like mTLS), this returns null.
     *
     * @return Authorization header value, or null if authentication is handled at SSL/TLS layer
     */
    String applyAuthentication();
    
    /**
     * Returns a custom SSLContext if the authentication strategy requires special SSL configuration.
     * Most authentication strategies (Basic, Bearer, API Key, OAuth2) return null to use default SSL.
     * mTLS authentication returns a configured SSLContext with client certificates.
     *
     * @return Custom SSLContext, or null to use default SSL configuration
     * @throws Exception if SSL context creation fails
     */
    default SSLContext getSSLContext() throws Exception {
        return null;
    }
}
