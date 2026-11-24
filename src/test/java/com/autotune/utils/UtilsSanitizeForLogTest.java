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
package com.autotune.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Utils#sanitizeForLog(String)}.
 */
class UtilsSanitizeForLogTest {

    @Test
    @DisplayName("null is returned as null")
    void nullInput() {
        assertNull(Utils.sanitizeForLog(null));
    }

    @Test
    @DisplayName("Plain text is not modified")
    void plainTextUnchanged() {
        String value = "k8s_type openshift: is not supported (name=abc-1.2)";
        assertEquals(value, Utils.sanitizeForLog(value));
    }

    @Test
    @DisplayName("CR, LF and tab characters are replaced so that log entries cannot be forged")
    void controlCharactersAreReplaced() {
        String forged = "openshift\n2026-11-12 ERROR [main] Fake - admin logged in\r\tdone";
        String sanitized = Utils.sanitizeForLog(forged);

        assertFalse(sanitized.contains("\n"));
        assertFalse(sanitized.contains("\r"));
        assertFalse(sanitized.contains("\t"));
        assertEquals("openshift_2026-11-12 ERROR [main] Fake - admin logged in__done", sanitized);
    }

    @Test
    @DisplayName("Other ISO control characters (e.g. ESC, NUL) are replaced")
    void otherControlCharactersAreReplaced() {
        assertEquals("a_b_c", Utils.sanitizeForLog("a\u001bb\u0000c"));
    }
}
