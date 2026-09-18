/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for the Jetty {@link CrossOriginHandler} that enforces the project CORS policy.
 *
 * <p>The handler is wired as a {@link org.eclipse.jetty.server.Handler.Wrapper} around the
 * servlet context in {@code Autotune.java}. It replaces the deprecated
 * {@code CrossOriginFilter}-based approach that was removed in Jetty 12.</p>
 *
 * <p>Allowed origins are driven by {@link KruizeConstants.CORSConstants#ALLOWED_ORIGINS}.
 * Override at runtime via the {@code KRUIZE_ALLOWED_ORIGINS} environment variable.</p>
 */
public class KruizeCORSHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeCORSHandler.class);

    // Singleton — must only be set on the server once.
    private static CrossOriginHandler handler = null;

    private KruizeCORSHandler() {
    }

    /**
     * Returns the singleton {@link CrossOriginHandler}.
     *
     * <p>The handler is <em>not</em> started here; callers must start it as part of the
     * normal Jetty server lifecycle.</p>
     *
     * @return a fully configured {@link CrossOriginHandler}
     */
    public static CrossOriginHandler getHandler() {
        if (handler == null) {
            handler = new CrossOriginHandler();

            // Allowed origins
            String originsConfig = KruizeConstants.CORSConstants.ALLOWED_ORIGINS;
            Set<String> origins = parseCommaSeparated(originsConfig);
            if (origins.isEmpty()) {
                LOGGER.warn("CORS: no allowed origins configured — all cross-origin requests will be rejected. " +
                        "Set KRUIZE_ALLOWED_ORIGINS to allow specific origins.");
            }
            handler.setAllowedOriginPatterns(origins);

            // Allowed HTTP methods
            handler.setAllowedMethods(parseCommaSeparated(KruizeConstants.CORSConstants.ALLOWED_METHODS));

            // Allowed request headers
            handler.setAllowedHeaders(parseCommaSeparated(KruizeConstants.CORSConstants.ALLOWED_HEADERS));

            // Preflight max-age
            handler.setPreflightMaxAge(Duration.ofSeconds(Long.parseLong(KruizeConstants.CORSConstants.MAX_AGE)));

            // Credentials are not needed for Kruize API consumers
            handler.setAllowCredentials(false);
        }
        return handler;
    }

    private static Set<String> parseCommaSeparated(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
