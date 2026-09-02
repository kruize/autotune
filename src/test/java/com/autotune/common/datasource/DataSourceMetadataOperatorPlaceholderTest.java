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
package com.autotune.common.datasource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DataSourceMetadataOperator#substituteWorkloadQueryPlaceholders}.
 *
 * Validates:
 *   Commas are never baked into the template around placeholders — they are prepended
 *   only when the variable is substituted with a non-empty value.
 *   LABEL_FILTER is always replaced (never left raw in PromQL), even when
 *   hasLabelFilter is false.
 */
class DataSourceMetadataOperatorPlaceholderTest {

    /**
     * Template string mirroring the manifest entry in
     * bulk_cluster_metadata_local_monitoring.json / .yaml.
     * Placeholders are space-separated; commas are prepended by the substitution logic.
     */
    private static final String TEMPLATE =
            "sum by (namespace, workload, workload_type) " +
            "(max_over_time(kube_pod_labels{pod!=\"\" LABEL_FILTER ADDITIONAL_LABEL}" +
            "[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) " +
            "group_left(workload, workload_type) " +
            "max_over_time(namespace_workload_pod:kube_pod_owner:relabel" +
            "{workload!=\"\", $UNSUPPORTED_WORKLOAD_TYPES$ ADDITIONAL_LABEL}" +
            "[$MEASUREMENT_DURATION_IN_MIN$m]))";

    // -------------------------------------------------------------------------
    // Error-2: LABEL_FILTER is always replaced (never left raw)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Error-2: LABEL_FILTER is always replaced (never left raw)")
    class LabelFilterAlwaysReplaced {

        @Test
        @DisplayName("hasLabelFilter=false: LABEL_FILTER placeholder removed, no raw token in result")
        void labelFilterRemovedWhenFalse() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "", "", false);

            assertFalse(result.contains("LABEL_FILTER"),
                    "Raw LABEL_FILTER must not survive when hasLabelFilter=false");
            assertFalse(result.contains("ADDITIONAL_LABEL"),
                    "Raw ADDITIONAL_LABEL must not survive");
        }

        @Test
        @DisplayName("hasLabelFilter=true: LABEL_FILTER replaced with include filter value")
        void labelFilterReplacedWithInclude() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "label_app=\"heap-oom\"", "", true);

            assertFalse(result.contains("LABEL_FILTER"),
                    "Raw LABEL_FILTER must not survive when hasLabelFilter=true");
            assertTrue(result.contains("label_app=\"heap-oom\""),
                    "Include filter value must appear in result");
        }

        @Test
        @DisplayName("hasLabelFilter=true: LABEL_FILTER replaced with exclude filter value")
        void labelFilterReplacedWithExclude() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "", "label_app!=\"heap-oom\"", true);

            assertFalse(result.contains("LABEL_FILTER"));
            assertTrue(result.contains("label_app!=\"heap-oom\""));
        }

        @Test
        @DisplayName("hasLabelFilter=true with both include and exclude: comma-joined")
        void labelFilterIncludeAndExcludeJoined() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "label_app=\"heap-oom\"", "label_env!=\"prod\"", true);

            assertFalse(result.contains("LABEL_FILTER"));
            assertTrue(result.contains("label_app=\"heap-oom\",label_env!=\"prod\""),
                    "Include and exclude filters must be comma-joined");
        }
    }

    // -------------------------------------------------------------------------
    // No stray spaces or commas after placeholder substitution
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("No stray spaces or commas after placeholder substitution")
    class NoStrayArtifacts {

        @Test
        @DisplayName("hasLabelFilter=false: no extra spaces inside braces after cleanup")
        void noStraySpacesWhenLabelFilterAbsent() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "", "", false);

            assertFalse(result.contains(" }"),
                    "Trailing space before } must be cleaned up");
            assertFalse(result.contains("{ "),
                    "Leading space after { must be cleaned up");
        }

        @Test
        @DisplayName("hasLabelFilter=true: no trailing space before } after ADDITIONAL_LABEL removed")
        void noTrailingSpaceWhenAdditionalLabelEmpty() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "label_app=\"heap-oom\"", "", true);

            assertFalse(result.contains(" }"),
                    "Trailing space before } must be cleaned up");
        }

        @Test
        @DisplayName("hasLabelFilter=false: kube_pod_labels selector contains only pod!=\\\"\\\" (clean)")
        void podSelectorRemainsValid() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "", "", false);

            assertTrue(result.contains("kube_pod_labels{pod!=\"\"}"),
                    "kube_pod_labels selector must be {pod!=\"\"} with no extra spaces; got: " + result);
        }

        @Test
        @DisplayName("hasLabelFilter=true: label filter embedded cleanly in selector with comma prepended")
        void labelFilterEmbeddedCleanly() {
            String result = DataSourceMetadataOperator.substituteWorkloadQueryPlaceholders(
                    TEMPLATE, "label_app=\"foo\"", "", true);

            assertTrue(result.contains("kube_pod_labels{pod!=\"\",label_app=\"foo\"}"),
                    "kube_pod_labels selector must contain label filter cleanly; got: " + result);
        }
    }
}
