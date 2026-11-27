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

import com.autotune.utils.KruizeConstants;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 * Jetty {@link Handler.Wrapper} that adds security related response headers to every response
 * (including error and CORS pre-flight responses), and a factory for an {@link HttpConfiguration}
 * that does not disclose the server type/version.
 *
 * <p>Wire it as the outermost handler of the {@code Server} so that the headers are present
 * irrespective of which inner handler produces the response.</p>
 */
public class SecurityHeadersHandler extends Handler.Wrapper {

    /**
     * Instructs browsers not to MIME-sniff the response body (ZAP rule 10021).
     */
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        response.getHeaders().put(KruizeConstants.SecurityHeaders.X_CONTENT_TYPE_OPTIONS,
                KruizeConstants.SecurityHeaders.NOSNIFF);
        return super.handle(request, response, callback);
    }

    /**
     * Creates an {@link HttpConfiguration} that suppresses the {@code Server} response header
     * so that the Jetty name and version are not leaked (ZAP rule 10036).
     *
     * @return the HTTP configuration to be used by the {@code ServerConnector}
     */
    public static HttpConfiguration createHttpConfiguration() {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSendServerVersion(false);
        return httpConfiguration;
    }
}
