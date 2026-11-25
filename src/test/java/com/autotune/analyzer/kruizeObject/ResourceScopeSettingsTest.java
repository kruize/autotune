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
package com.autotune.analyzer.kruizeObject;

import com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationScope;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResourceScopeSettings and RecommendationScope parsing.
 */
class ResourceScopeSettingsTest {

    @Nested
    @DisplayName("RecommendationScope.fromString")
    class FromStringTests {

        @Test
        @DisplayName("Should match canonical values")
        void shouldMatchCanonicalValues() {
            assertEquals(RecommendationScope.UNDER_PROVISIONED, RecommendationScope.fromString("under_provisioned"));
            assertEquals(RecommendationScope.OVER_PROVISIONED, RecommendationScope.fromString("over_provisioned"));
            assertEquals(RecommendationScope.OPTIMISED, RecommendationScope.fromString("optimised"));
            assertEquals(RecommendationScope.IDLE, RecommendationScope.fromString("idle"));
            assertEquals(RecommendationScope.ALL, RecommendationScope.fromString("all"));
        }

        @Test
        @DisplayName("Should be case insensitive and accept hyphens and surrounding whitespace")
        void shouldAcceptMixedCaseAndHyphens() {
            assertEquals(RecommendationScope.UNDER_PROVISIONED, RecommendationScope.fromString("Under_Provisioned"));
            assertEquals(RecommendationScope.OVER_PROVISIONED, RecommendationScope.fromString("OVER-PROVISIONED"));
            assertEquals(RecommendationScope.ALL, RecommendationScope.fromString("  ALL  "));
        }

        @Test
        @DisplayName("Should accept the American spelling of optimised")
        void shouldAcceptOptimizedAlias() {
            assertEquals(RecommendationScope.OPTIMISED, RecommendationScope.fromString("optimized"));
            assertEquals(RecommendationScope.OPTIMISED, RecommendationScope.fromString("Optimized"));
        }

        @Test
        @DisplayName("Should return null for unknown, empty and null input")
        void shouldReturnNullForInvalidInput() {
            assertNull(RecommendationScope.fromString("bogus"));
            assertNull(RecommendationScope.fromString(""));
            assertNull(RecommendationScope.fromString("   "));
            assertNull(RecommendationScope.fromString(null));
        }

        @Test
        @DisplayName("Should not depend on the default locale (Turkish dotted and dotless i)")
        void shouldIgnoreDefaultLocale() {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                assertEquals(RecommendationScope.IDLE, RecommendationScope.fromString("IDLE"));
                assertEquals(RecommendationScope.OPTIMISED, RecommendationScope.fromString("OPTIMISED"));
                assertEquals(RecommendationScope.UNDER_PROVISIONED, RecommendationScope.fromString("Under-Provisioned"));
            } finally {
                Locale.setDefault(original);
            }
        }
    }

    @Nested
    @DisplayName("RecommendationScope.parseScopes")
    class ParseScopesTests {

        @Test
        @DisplayName("Should return null for null and empty lists")
        void shouldReturnNullForNullAndEmpty() {
            assertNull(RecommendationScope.parseScopes(null));
            assertNull(RecommendationScope.parseScopes(List.of()));
        }

        @Test
        @DisplayName("Should return the set for a valid list")
        void shouldParseValidList() {
            EnumSet<RecommendationScope> result = RecommendationScope.parseScopes(List.of("under_provisioned", "idle"));
            assertEquals(EnumSet.of(RecommendationScope.UNDER_PROVISIONED, RecommendationScope.IDLE), result);
        }

        @Test
        @DisplayName("Should skip unknown strings")
        void shouldSkipUnknownStrings() {
            EnumSet<RecommendationScope> result = RecommendationScope.parseScopes(Arrays.asList("bogus", "over_provisioned", null));
            assertEquals(EnumSet.of(RecommendationScope.OVER_PROVISIONED), result);
        }

        @Test
        @DisplayName("Should skip scopes that are not supported for the resource")
        void shouldSkipUnsupportedScopes() {
            EnumSet<RecommendationScope> result = RecommendationScope.parseScopes(
                    List.of("idle", "optimised"), RecommendationScope.MEMORY_SCOPES);
            assertEquals(EnumSet.of(RecommendationScope.OPTIMISED), result);
        }
    }

    @Nested
    @DisplayName("ResourceScopeSettings.parse")
    class ParseTests {

        @Test
        @DisplayName("Should keep idle for cpu and drop it for memory")
        void shouldKeepIdleForCpuOnly() {
            ResourceScopeSettings settings = new ResourceScopeSettings(List.of("idle"), List.of("idle", "over_provisioned"));
            settings.parse();

            assertEquals(EnumSet.of(RecommendationScope.IDLE), settings.getParsedCpuScopes());
            assertEquals(EnumSet.of(RecommendationScope.OVER_PROVISIONED), settings.getParsedMemoryScopes());
        }

        @Test
        @DisplayName("Should keep all for memory")
        void shouldKeepAllForMemory() {
            ResourceScopeSettings settings = new ResourceScopeSettings(null, List.of("all"));
            settings.parse();

            assertEquals(EnumSet.of(RecommendationScope.ALL), settings.getParsedMemoryScopes());
        }

        @Test
        @DisplayName("Should leave the parsed set null when the key is absent")
        void shouldLeaveAbsentKeyNull() {
            ResourceScopeSettings cpuOnly = new ResourceScopeSettings(List.of("optimised"), null);
            cpuOnly.parse();
            assertNotNull(cpuOnly.getParsedCpuScopes());
            assertNull(cpuOnly.getParsedMemoryScopes());

            ResourceScopeSettings memoryOnly = new ResourceScopeSettings(null, List.of("optimised"));
            memoryOnly.parse();
            assertNull(memoryOnly.getParsedCpuScopes());
            assertNotNull(memoryOnly.getParsedMemoryScopes());
        }
    }

    @Nested
    @DisplayName("Gson handling")
    class GsonTests {

        private static final String JSON = "{\"cpu\":[\"under_provisioned\"],\"memory\":[\"over_provisioned\"]}";

        @Test
        @DisplayName("Should leave parsed sets null after Gson, and populate them via RecommendationSettings setter")
        void shouldPopulateParsedSetsThroughSetter() {
            ResourceScopeSettings fromGson = new Gson().fromJson(JSON, ResourceScopeSettings.class);
            assertEquals(List.of("under_provisioned"), fromGson.getCpu());
            assertEquals(List.of("over_provisioned"), fromGson.getMemory());
            assertNull(fromGson.getParsedCpuScopes(), "Gson bypasses setters, so the sets stay unparsed");
            assertNull(fromGson.getParsedMemoryScopes());

            RecommendationSettings recommendationSettings = new RecommendationSettings();
            recommendationSettings.setResourceScopeSettings(fromGson);

            assertEquals(EnumSet.of(RecommendationScope.UNDER_PROVISIONED), fromGson.getParsedCpuScopes());
            assertEquals(EnumSet.of(RecommendationScope.OVER_PROVISIONED), fromGson.getParsedMemoryScopes());
        }

        @Test
        @DisplayName("Should not serialize the parsed sets")
        void shouldNotSerializeParsedSets() {
            ResourceScopeSettings settings = new ResourceScopeSettings(List.of("idle"), List.of("all"));
            settings.parse();

            assertEquals("{\"cpu\":[\"idle\"],\"memory\":[\"all\"]}", new Gson().toJson(settings));
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Should escape CR and LF so scope values cannot forge log lines")
        void shouldEscapeLineBreaks() {
            ResourceScopeSettings settings = new ResourceScopeSettings(List.of("idle\r\nFAKE LOG LINE"), List.of("all"));

            String text = settings.toString();

            assertFalse(text.contains("\r"));
            assertFalse(text.contains("\n"));
            assertTrue(text.contains("\\u000d\\u000a"));
        }

        @Test
        @DisplayName("Should handle null lists")
        void shouldHandleNullLists() {
            assertEquals("ResourceScopeSettings{cpu=null, memory=null}", new ResourceScopeSettings().toString());
        }
    }
}
