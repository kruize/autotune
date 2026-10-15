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

import com.autotune.operator.KruizeDeploymentInfo;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CloudWatchAppender#configureLoggerForCloudWatchLog()}.
 *
 * Covers three scenarios:
 *
 * 1. CREDENTIALS ABSENT (simulates test/dev cluster — why the bug was hidden)
 *    When credentials are null or empty, the method short-circuits at the
 *    credential guard.  The AWS SDK is never touched, so neither jdk.net nor
 *    Apache HC5 are loaded.  No exception, no appender registered.
 *
 * 2. NEGATIVE — documents the production crash condition
 *    Asserts that the {@code jdk.net} module is present in the running JRE.
 *    When {@code jdk.net} is absent (e.g. a stripped jlink JRE that omits the
 *    module), Apache HttpClient 5 — pulled in by {@code awssdk:apache5-client}
 *    — fails at startup with {@code NoClassDefFoundError: jdk/net/Sockets}.
 *    This test fails with a descriptive message when run in such a JRE,
 *    documenting the exact condition that caused the production crash.
 *
 * 3. POSITIVE — verifies the runtime is correctly configured
 *    Asserts that {@code jdk.net.Sockets} is loadable and that
 *    {@code configureLoggerForCloudWatchLog()} completes without any
 *    class-loading error when credentials are present.
 */
class CloudWatchAppenderTest {

    // --- saved static state -------------------------------------------------

    private String savedAccessKeyId;
    private String savedSecretAccessKey;
    private String savedRegion;
    private String savedLogGroup;
    private String savedLogStream;
    private String savedLogLevel;

    // --- lifecycle ----------------------------------------------------------

    @BeforeEach
    void saveDeploymentInfo() {
        savedAccessKeyId     = KruizeDeploymentInfo.cloudwatch_logs_access_key_id;
        savedSecretAccessKey = KruizeDeploymentInfo.cloudwatch_logs_secret_access_key;
        savedRegion          = KruizeDeploymentInfo.cloudwatch_logs_region;
        savedLogGroup        = KruizeDeploymentInfo.cloudwatch_logs_log_group;
        savedLogStream       = KruizeDeploymentInfo.cloudwatch_logs_log_stream;
        savedLogLevel        = KruizeDeploymentInfo.cloudwatch_logs_log_level;
    }

    @AfterEach
    void restoreDeploymentInfo() {
        KruizeDeploymentInfo.cloudwatch_logs_access_key_id     = savedAccessKeyId;
        KruizeDeploymentInfo.cloudwatch_logs_secret_access_key = savedSecretAccessKey;
        KruizeDeploymentInfo.cloudwatch_logs_region            = savedRegion;
        KruizeDeploymentInfo.cloudwatch_logs_log_group         = savedLogGroup;
        KruizeDeploymentInfo.cloudwatch_logs_log_stream        = savedLogStream;
        KruizeDeploymentInfo.cloudwatch_logs_log_level         = savedLogLevel;
    }

    // =========================================================================
    // 1. CREDENTIALS ABSENT — simulates test/dev cluster
    // =========================================================================

    @Test
    @DisplayName("No appender registered when all credentials are null")
    void shouldSkipConfigurationWhenAllCredentialsAreNull() {
        // Given – no CloudWatch credentials (mirrors test cluster config)
        KruizeDeploymentInfo.cloudwatch_logs_access_key_id     = null;
        KruizeDeploymentInfo.cloudwatch_logs_secret_access_key = null;
        KruizeDeploymentInfo.cloudwatch_logs_region            = null;

        // When – should not throw and should not register any appender
        assertDoesNotThrow(CloudWatchAppender::configureLoggerForCloudWatchLog);

        // Then – cloudwatchRootAppender must NOT be present in the log4j config
        assertFalse(
                cloudWatchAppenderRegistered(),
                "CloudWatch appender must not be registered when credentials are absent"
        );
    }

    @Test
    @DisplayName("No appender registered when credentials are empty strings")
    void shouldSkipConfigurationWhenCredentialsAreEmpty() {
        // Given
        KruizeDeploymentInfo.cloudwatch_logs_access_key_id     = "";
        KruizeDeploymentInfo.cloudwatch_logs_secret_access_key = "";
        KruizeDeploymentInfo.cloudwatch_logs_region            = "";

        // When
        assertDoesNotThrow(CloudWatchAppender::configureLoggerForCloudWatchLog);

        // Then
        assertFalse(
                cloudWatchAppenderRegistered(),
                "CloudWatch appender must not be registered when credentials are empty"
        );
    }

    @Test
    @DisplayName("No appender registered when only access key is missing")
    void shouldSkipConfigurationWhenAccessKeyIdIsMissing() {
        // Given – secret + region present but access key absent
        KruizeDeploymentInfo.cloudwatch_logs_access_key_id     = null;
        KruizeDeploymentInfo.cloudwatch_logs_secret_access_key = "dummy-secret";
        KruizeDeploymentInfo.cloudwatch_logs_region            = "us-east-1";

        // When
        assertDoesNotThrow(CloudWatchAppender::configureLoggerForCloudWatchLog);

        // Then
        assertFalse(
                cloudWatchAppenderRegistered(),
                "CloudWatch appender must not be registered when access key id is missing"
        );
    }

    // =========================================================================
    // 2. NEGATIVE
    //
    //    The pod log showed:
    //      Exception in thread "main" java.lang.NoClassDefFoundError: jdk/net/Sockets
    //        at DefaultHttpClientConnectionOperator.<clinit>
    //        at Apache5HttpClient.createClient
    //        at CloudWatchAppender.configureLoggerForCloudWatchLog
    //        at Autotune.main
    //      Caused by: java.lang.ClassNotFoundException: jdk.net.Sockets
    //
    //    Root cause: the jlink-built JRE shipped in the container image did not
    //    include the jdk.net module, so jdk.net.Sockets was unavailable when
    //    Apache HttpClient 5 tried to load it in a static initializer.
    //
    //    Note: it is not possible to reproduce the NoClassDefFoundError
    //    in-process on a full JDK — JPMS resolves named-module classes
    //    directly from the module layer, bypassing the classloader hierarchy,
    //    so a custom excluding ClassLoader has no effect.  The test below
    //    therefore asserts the precondition of the crash: jdk.net must be
    //    present.  Run it inside a jlink JRE that omits jdk.net to see it fail.
    // =========================================================================

    @Test
    @DisplayName("NEGATIVE: jdk.net module must be present — absence causes NoClassDefFoundError: jdk/net/Sockets at startup")
    void jdkNetModuleMustBePresentToPreventStartupCrash() {
        /*
         * Apache HttpClient 5 (awssdk:apache5-client >= 2.46.x) references
         * jdk.net.Sockets in a static initializer.  If the jdk.net module is
         * absent from the JRE, the JVM throws:
         *   NoClassDefFoundError: jdk/net/Sockets
         *   Caused by: ClassNotFoundException: jdk.net.Sockets
         * crashing the process before it can serve any requests.
         *
         * On a full JDK jdk.net is always present, so this test always passes
         * in local development.  Inside a stripped jlink JRE that omits jdk.net,
         * ModuleLayer.boot().findModule("jdk.net") returns an empty Optional and
         * this assertion fails — surfacing the missing-module condition before it
         * manifests as a production crash.
         */
        assertTrue(
                ModuleLayer.boot().findModule("jdk.net").isPresent(),
                "The 'jdk.net' module is absent from this JRE. " +
                "Apache HttpClient 5 requires it and will crash at startup with: " +
                "NoClassDefFoundError: jdk/net/Sockets. " +
                "Ensure the jlink --add-modules list used to build this JRE includes jdk.net."
        );
    }

    // =========================================================================
    // 3. POSITIVE — verifies the runtime is correctly configured
    //
    //    On a developer JDK these always pass (full JDK always has jdk.net).
    //    Inside a jlink JRE they pass only when jdk.net is included in the
    //    --add-modules list used to build the image.
    // =========================================================================

    @Test
    @DisplayName("POSITIVE: jdk.net.Sockets is loadable — jdk.net module is present in the JRE")
    void jdkNetSocketsMustBeLoadable() throws ClassNotFoundException {
        /*
         * Apache HttpClient 5 loads jdk.net.Sockets in a static initializer.
         * If this throws ClassNotFoundException the JRE is missing the jdk.net
         * module and Apache HC5 will crash at startup with:
         *   NoClassDefFoundError: jdk/net/Sockets
         */
        Class<?> socketsClass = Class.forName("jdk.net.Sockets");
        assertNotNull(
                socketsClass,
                "jdk.net.Sockets must be loadable — ensure the jdk.net module " +
                "is included in the --add-modules list used to build the JRE."
        );
    }

    @Test
    @DisplayName("POSITIVE: configureLoggerForCloudWatchLog must not throw NoClassDefFoundError when credentials are present")
    void shouldNotThrowNoClassDefFoundErrorWhenCredentialsPresent() {
        // Given – credentials present, identical to the stage cluster config
        KruizeDeploymentInfo.cloudwatch_logs_access_key_id     = "AKIADUMMYACCESSKEY";
        KruizeDeploymentInfo.cloudwatch_logs_secret_access_key = "dummySecretAccessKey0000000000000000000";
        KruizeDeploymentInfo.cloudwatch_logs_region            = "us-east-1";
        KruizeDeploymentInfo.cloudwatch_logs_log_group         = "kruize-test-logs";
        KruizeDeploymentInfo.cloudwatch_logs_log_stream        = "kruize-test-stream";
        KruizeDeploymentInfo.cloudwatch_logs_log_level         = "INFO";

        // When / Then
        // Any AWS auth/network error is caught and logged inside the method.
        // A NoClassDefFoundError: jdk/net/Sockets is NOT caught there —
        // it would propagate and fail this assertion, reproducing the prod crash.
        assertDoesNotThrow(
                CloudWatchAppender::configureLoggerForCloudWatchLog,
                "configureLoggerForCloudWatchLog() threw an unexpected error. " +
                "If the cause is NoClassDefFoundError: jdk/net/Sockets, " +
                "the jdk.net module is missing from the JRE."
        );
    }

    // --- helpers ------------------------------------------------------------

    private boolean cloudWatchAppenderRegistered() {
        LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        return config.getAppenders().containsKey("cloudwatchRootAppender");
    }
}
