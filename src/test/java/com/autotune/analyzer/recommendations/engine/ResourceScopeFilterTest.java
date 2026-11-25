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
package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.kruizeObject.ResourceScopeSettings;
import com.autotune.analyzer.recommendations.Config;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationScope;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.model.CostBasedRecommendationModel;
import com.autotune.analyzer.recommendations.model.PerformanceBasedRecommendationModel;
import com.autotune.analyzer.recommendations.model.RecommendationModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.autotune.utils.KruizeConstants.PerformanceBasedRecommendationConstants.PERFORMANCE_RECOMMENDATION_TUNABLES;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the resource scope filtering in BaseRecommendationProcessor.
 * The protected methods are reached through a subclass in the same package.
 */
class ResourceScopeFilterTest {

    private static final class TestProcessor extends BaseRecommendationProcessor {
        TestProcessor() {
            super(null);
        }
    }

    private final TestProcessor processor = new TestProcessor();

    // ---- fixtures ----

    private static final String UNDER = "under_provisioned";
    private static final String OVER = "over_provisioned";
    private static final String OPT = "optimised";
    private static final String IDLE = "idle";
    private static final String ALL = "all";

    /** The 12 states a recommendation can be in: cpu (under, over, optimised, idle) x memory (under, over, optimised). */
    private static final List<String[]> STATES = new ArrayList<>();

    static {
        for (String cpu : new String[]{UNDER, OVER, OPT, IDLE}) {
            for (String memory : new String[]{UNDER, OVER, OPT}) {
                STATES.add(new String[]{cpu, memory});
            }
        }
    }

    private static String key(String cpu, String memory) {
        return cpu + "/" + memory;
    }

    private static RecommendationConfigItem item(double amount) {
        return new RecommendationConfigItem(amount, "cores");
    }

    private static double amountFor(String state) {
        switch (state) {
            case UNDER:
                return 0.25;
            case OVER:
                return -0.25;
            default:
                return 0.0;
        }
    }

    private static MappedRecommendationForModel recommendation(String cpuState, String memoryState) {
        MappedRecommendationForModel rec = new MappedRecommendationForModel();
        Config variation = new Config();
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        requests.put(AnalyzerConstants.RecommendationItem.CPU, item(amountFor(cpuState)));
        requests.put(AnalyzerConstants.RecommendationItem.MEMORY, item(amountFor(memoryState)));
        variation.setRequests(requests);
        rec.setVariation(variation);
        if (IDLE.equals(cpuState)) {
            rec.addNotification(new RecommendationNotification(
                    RecommendationConstants.RecommendationNotification.NOTICE_CPU_RECORDS_ARE_IDLE));
        }
        return rec;
    }

    private static ResourceScopeSettings scopes(List<String> cpu, List<String> memory) {
        ResourceScopeSettings settings = new ResourceScopeSettings(cpu, memory);
        settings.parse();
        return settings;
    }

    private static Set<String> keys(String... pairs) {
        return new TreeSet<>(Arrays.asList(pairs));
    }

    private Set<String> accepted(ResourceScopeSettings filter) {
        Set<String> result = new TreeSet<>();
        for (String[] state : STATES) {
            if (processor.isRecommendationInScope(recommendation(state[0], state[1]), filter)) {
                result.add(key(state[0], state[1]));
            }
        }
        return result;
    }

    // ---- classifyResource ----

    @Test
    @DisplayName("classifyResource should map the requests variation to a state")
    void classifyResourceShouldMapVariation() {
        assertEquals(RecommendationScope.UNDER_PROVISIONED, processor.classifyResource(item(0.1), false));
        assertEquals(RecommendationScope.OVER_PROVISIONED, processor.classifyResource(item(-0.1), false));
        assertEquals(RecommendationScope.OPTIMISED, processor.classifyResource(item(0.0), false));
        assertEquals(RecommendationScope.OPTIMISED, processor.classifyResource(null, false));
        assertEquals(RecommendationScope.OPTIMISED, processor.classifyResource(new RecommendationConfigItem(), false));
    }

    @Test
    @DisplayName("classifyResource should report idle regardless of the variation")
    void classifyResourceShouldPreferIdle() {
        assertEquals(RecommendationScope.IDLE, processor.classifyResource(item(0.1), true));
        assertEquals(RecommendationScope.IDLE, processor.classifyResource(null, true));
    }

    // ---- truth table: 12 states x 7 filters ----

    @Test
    @DisplayName("cpu under_provisioned and memory under_provisioned")
    void cpuUnderMemoryUnder() {
        assertEquals(keys(key(UNDER, UNDER)), accepted(scopes(List.of(UNDER), List.of(UNDER))));
    }

    @Test
    @DisplayName("cpu over_provisioned and memory over_provisioned")
    void cpuOverMemoryOver() {
        assertEquals(keys(key(OVER, OVER)), accepted(scopes(List.of(OVER), List.of(OVER))));
    }

    @Test
    @DisplayName("cpu idle and memory over_provisioned")
    void cpuIdleMemoryOver() {
        assertEquals(keys(key(IDLE, OVER)), accepted(scopes(List.of(IDLE), List.of(OVER))));
    }

    @Test
    @DisplayName("cpu under_provisioned or idle, and memory under_provisioned")
    void cpuUnderOrIdleMemoryUnder() {
        assertEquals(keys(key(UNDER, UNDER), key(IDLE, UNDER)),
                accepted(scopes(List.of(UNDER, IDLE), List.of(UNDER))));
    }

    @Test
    @DisplayName("all for cpu and memory accepts every state")
    void allAcceptsEverything() {
        Set<String> everything = new TreeSet<>();
        for (String[] state : STATES) {
            everything.add(key(state[0], state[1]));
        }
        assertEquals(12, everything.size());
        assertEquals(everything, accepted(scopes(List.of(ALL), List.of(ALL))));
    }

    @Test
    @DisplayName("cpu idle only leaves memory unconstrained")
    void cpuIdleOnly() {
        assertEquals(keys(key(IDLE, UNDER), key(IDLE, OVER), key(IDLE, OPT)),
                accepted(scopes(List.of(IDLE), null)));
    }

    @Test
    @DisplayName("memory under_provisioned only leaves cpu unconstrained")
    void memoryUnderOnly() {
        assertEquals(keys(key(UNDER, UNDER), key(OVER, UNDER), key(OPT, UNDER), key(IDLE, UNDER)),
                accepted(scopes(null, List.of(UNDER))));
    }

    @Test
    @DisplayName("multiple values for one resource are ORed, resources are ANDed")
    void multipleValuesAreOred() {
        assertEquals(keys(key(UNDER, OPT), key(OVER, OPT)),
                accepted(scopes(List.of(UNDER, OVER), List.of(OPT))));
    }

    @Test
    @DisplayName("all mixed with other values behaves like all")
    void allMixedWithOthers() {
        assertEquals(accepted(scopes(List.of(ALL), List.of(ALL))),
                accepted(scopes(List.of(ALL, UNDER), List.of(ALL, OVER))));
    }

    // ---- idle and missing data ----

    @Test
    @DisplayName("Idle is read from the model's own notifications, not from the variation")
    void idleComesFromModelNotifications() {
        MappedRecommendationForModel idle = recommendation(OPT, OPT);
        idle.addNotification(new RecommendationNotification(
                RecommendationConstants.RecommendationNotification.NOTICE_CPU_RECORDS_ARE_IDLE));
        MappedRecommendationForModel notIdle = recommendation(OPT, OPT);

        ResourceScopeSettings idleFilter = scopes(List.of(IDLE), null);
        assertTrue(processor.isRecommendationInScope(idle, idleFilter));
        assertFalse(processor.isRecommendationInScope(notIdle, idleFilter));
    }

    @Test
    @DisplayName("A null variation counts as optimised for both resources")
    void nullVariationIsOptimised() {
        MappedRecommendationForModel rec = new MappedRecommendationForModel();
        rec.setVariation(null);

        assertFalse(processor.isRecommendationInScope(rec, scopes(List.of(UNDER), List.of(UNDER))));
        assertTrue(processor.isRecommendationInScope(rec, scopes(List.of(ALL), List.of(ALL))));
        assertTrue(processor.isRecommendationInScope(rec, scopes(List.of(OPT), List.of(OPT))));
    }

    @Test
    @DisplayName("A null variation with an idle notification is still idle")
    void nullVariationWithIdleNotification() {
        MappedRecommendationForModel rec = new MappedRecommendationForModel();
        rec.setVariation(null);
        rec.addNotification(new RecommendationNotification(
                RecommendationConstants.RecommendationNotification.NOTICE_CPU_RECORDS_ARE_IDLE));

        assertTrue(processor.isRecommendationInScope(rec, scopes(List.of(IDLE), null)));
        assertFalse(processor.isRecommendationInScope(rec, scopes(List.of(UNDER), null)));
    }

    // ---- resolveEffectiveScopes ----

    @Test
    @DisplayName("User scopes win over the model default")
    void userScopesWin() {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setResourceScopeSettings(new ResourceScopeSettings(List.of(OVER), null));

        ResourceScopeSettings effective = processor.resolveEffectiveScopes(settings,
                new PerformanceBasedRecommendationModel(PERFORMANCE_RECOMMENDATION_TUNABLES));

        assertEquals(EnumSet.of(RecommendationScope.OVER_PROVISIONED), effective.getParsedCpuScopes());
        assertNull(effective.getParsedMemoryScopes(), "Absent memory key must stay unconstrained, not take the model default");
    }

    @Test
    @DisplayName("Absent settings and absent scope settings fall back to the model default")
    void absentSettingsFallBackToModelDefault() {
        RecommendationModel performance = new PerformanceBasedRecommendationModel(PERFORMANCE_RECOMMENDATION_TUNABLES);
        RecommendationModel cost = new CostBasedRecommendationModel(KruizeConstants.CostBasedRecommendationConstants.COST_RECOMMENDATION_TUNABLES);

        for (RecommendationSettings settings : Arrays.asList(null, new RecommendationSettings())) {
            for (RecommendationModel model : List.of(performance, cost)) {
                ResourceScopeSettings scopes = processor.resolveEffectiveScopes(settings, model);
                assertEquals(EnumSet.of(RecommendationScope.ALL), scopes.getParsedCpuScopes(), model.getModelName());
                assertEquals(EnumSet.of(RecommendationScope.ALL), scopes.getParsedMemoryScopes(), model.getModelName());
            }
        }
    }

    @Test
    @DisplayName("Settings that went through Gson (unparsed sets) are re-parsed")
    void gsonSettingsAreReparsed() {
        RecommendationSettings settings = new Gson().fromJson(
                "{\"resource_scope_settings\":{\"cpu\":[\"idle\"],\"memory\":[\"over_provisioned\"]}}",
                RecommendationSettings.class);
        assertNull(settings.getResourceScopeSettings().getParsedCpuScopes(), "Precondition: Gson leaves the sets unparsed");

        ResourceScopeSettings effective = processor.resolveEffectiveScopes(settings,
                new PerformanceBasedRecommendationModel(PERFORMANCE_RECOMMENDATION_TUNABLES));

        assertEquals(EnumSet.of(RecommendationScope.IDLE), effective.getParsedCpuScopes());
        assertEquals(EnumSet.of(RecommendationScope.OVER_PROVISIONED), effective.getParsedMemoryScopes());
    }
}
