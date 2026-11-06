# ADR: Conditional Execution of Runtime Metric Queries Based on Layer Detection

## Status
Proposed

## Context

Currently, Kruize executes **all** metric queries defined in the performance profile, including runtime-related queries (e.g., `jvm_info`, `jvm_info_total`, and other JVM/runtime metrics), irrespective of whether a runtime layer has been detected in the container. This results in:

1. **Unnecessary query executions** - Prometheus queries are executed even when no runtime layer exists
2. **Performance overhead** - Additional load on Prometheus and the recommendation engine
4. **Potential errors** - Queries may return empty results or errors when runtime metrics don't exist
5. **Log noise** - Failed or empty query results clutter logs

### Current Behavior

The recommendation engine processes all metrics defined in the performance profile without any mechanism to conditionally execute queries based on detected layers. For example:

```java
// Current implementation - ALL queries executed unconditionally
for (Map.Entry<String, AggregationFunctions> aggregationFunctionsEntry : aggregationFunctions.entrySet()) {
    String promQL = aggregationFunctionsEntry.getValue().getQuery();
    // Execute query regardless of whether the layer is present
    executeQuery(promQL);
}
```

**Problem:** Even if a container has no JVM runtime, queries like `jvm_info` are still executed, wasting resources.

### Problem Statement

**JIRA Requirement:**
> Modify the logic so that runtime-specific queries are executed only when a runtime layer is detected during layer analysis.

**Expected Behavior:**
- Detect whether a runtime layer (e.g., JVM or other supported runtime) is present
- Execute runtime metric queries such as `jvm_info`, `heap_memory`, etc., only if the runtime layer detection flag is true
- Skip runtime queries when no runtime layer is identified

**Definition of Done:**
- Runtime metric queries are triggered only when runtime layer detection succeeds
- No runtime queries are executed when runtime layer detection is false
- Existing functionality for runtime metric processing remains unaffected when a runtime layer is present

## Decision

We will introduce a **new optional field** `layers_required` at the **metric level** within performance profiles. This field will specify which layer(s) must be detected before executing any queries for that metric.

### Design Changes

#### 1. Performance Profile Schema Enhancement

**New Field Introduction:**

We are adding an optional `layers_required` field at the metric level (not inside aggregation_functions). This field accepts an array of layer names and applies to all aggregation functions for that metric.

**Before (No layer checking):**
```json
{
  "name": "jvmInfo",
  "datasource": "prometheus",
  "value_type": "double",
  "kubernetes_object": "container",
  "aggregation_functions": [
    {
      "function": "sum",
      "query": "sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"})"
    }
  ]
}
```

**After (With layer requirement):**
```json
{
  "name": "jvmInfo",
  "datasource": "prometheus",
  "value_type": "double",
  "kubernetes_object": "container",
  "layers_required": ["hotspot", "semeru"],
  "aggregation_functions": [
    {
      "function": "sum",
      "query": "sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"})"
    }
  ]
}
```

**YAML Representation:**
```yaml
- name: jvmInfo
  datasource: prometheus
  value_type: "double"
  kubernetes_object: "container"
  layers_required:
    - "hotspot"
    - "semeru"
  aggregation_functions:
    - function: sum
      query: 'sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace="$NAMESPACE$", container="$CONTAINER_NAME$"})'
```

**Key Points:**
- `layers_required` is **optional** - if not specified, all queries for the metric are always executed (backward compatible)
- Placed at **metric level**, not inside aggregation_functions - applies to ALL aggregation functions for that metric
- Accepts an **array of strings** representing layer names
- Uses **OR logic** - metric queries execute if ANY of the specified layers is detected
- Empty array or null means no layer requirement (always execute)
- **Field name:** `layers_required` (not `required_layer`) for better clarity

#### 2. Data Model Update

**File:** `src/main/java/com/autotune/common/data/metrics/Metric.java`

**New Field Addition:**
```java
import java.util.List;
import com.fasterxml.jackson.annotation.SerializedName;

public final class Metric {
    private String name;
    private String query;
    private String datasource;
    @SerializedName("value_type")
    private String valueType;
    @SerializedName("kubernetes_object")
    private String kubernetesObject;
    @SerializedName("layers_required")
    private List<String> layersRequired;  // NEW FIELD
    @SerializedName("aggregation_functions")
    private HashMap<String, AggregationFunctions> aggregationFunctionsMap;

    // Existing getters/setters...

    // NEW: Getter for layers required
    public List<String> getLayersRequired() {
        return layersRequired;
    }

    // NEW: Setter for layers required
    public void setLayersRequired(List<String> layersRequired) {
        this.layersRequired = layersRequired;
    }
}
```

**Design Choice:** 
- Field placed at **Metric level** (not AggregationFunctions level)
- Using `List<String>` provides type safety, no parsing overhead, natural JSON/YAML representation
- `@SerializedName("layers_required")` maps to JSON field name
- Applies to entire metric, not individual aggregation functions

#### 3. Recommendation Engine Logic Update

**File:** `src/main/java/com/autotune/analyzer/recommendations/engine/RecommendationEngine.java`

**New Logic Addition:**

Check is performed at metric level before processing any aggregation functions:

```java
// NEW: Check if this metric requires specific layers (at metric level)
List<String> layersRequired = metricEntry.getLayersRequired();
if (layersRequired != null && !layersRequired.isEmpty()) {
    // Check if any of the required layers are detected
    boolean layerDetected = false;
    for (String layer : layersRequired) {
        if (containerData.getLayerMap() != null &&
                containerData.getLayerMap().containsKey(layer)) {
            layerDetected = true;
            break;
        }
    }
    // Skip this metric entirely if required layer is not detected
    if (!layerDetected) {
        LOGGER.debug("Skipping metric {} - required layer(s) {} not detected for container {}",
                metricEntry.getName(), layersRequired, containerName);
        continue;  // Skip entire metric (all aggregation functions)
    }
}

// Proceed with aggregation functions only if layer check passed
HashMap<String, AggregationFunctions> aggregationFunctions = metricEntry.getAggregationFunctionsMap();
for (Map.Entry<String, AggregationFunctions> aggregationFunctionsEntry : aggregationFunctions.entrySet()) {
    String promQL = aggregationFunctionsEntry.getValue().getQuery();
    executeQuery(promQL);
}
```

**Logic Flow:**
1. Check if `layers_required` is specified at the metric level
2. If specified, verify at least one required layer is detected in the container
3. If no required layer is detected, skip the **entire metric** (all its aggregation functions) and log the decision
4. If layer is detected (or no requirement specified), proceed with all aggregation functions for that metric

**Key Advantage:** Single check per metric instead of per aggregation function - more efficient

### Key Design Decisions

1. **Optional Field** - Backward compatible; existing metrics without `layers_required` work unchanged
2. **Metric Level Placement** - Field at metric level (not aggregation function level) for simplicity and efficiency
3. **Array Format** - Using `List<String>` for type safety and clean representation
4. **OR Logic** - Metric queries execute if ANY specified layer is detected (flexible for multi-runtime support)
5. **Early Exit** - Skip entire metric if layer not detected (performance optimization)
6. **Debug Logging** - Log skipped metrics for troubleshooting and monitoring
7. **Field Naming** - `layers_required` (plural) is more descriptive than `required_layer`

## Consequences

### Positive

1. **Performance Optimization**
   - Reduces unnecessary Prometheus queries

2. **Resource Efficiency**
   - Minimizes memory usage for storing empty results

3. **Better User Experience**
   - Faster recommendation generation
   - Reduced noise in logs (no failed queries for non-existent metrics)

4. **Maintainability**
   - Cleaner data structure (array at metric level)
   - Easier to add new runtime layers
   - Type-safe implementation
   - Single check per metric (not per aggregation function)

5. **Extensibility**
   - Pattern can be applied to other layer-specific metrics
   - Supports multi-layer requirements (OR logic)

### Negative

1. **Schema Change**
   - Requires documentation updates
   - No change with respect to schema since it's part of SLO JsonNode column

2. **Testing Requirements**
   - Need to test with various layer combinations
   - Verify backward compatibility (metrics without `layers_required`)
   - Validate query skipping logic
   - Test OR logic with multiple layers

3. **Potential for Misconfiguration**
   - Incorrect layer names in `layers_required` will cause queries to be skipped
   - Need validation to ensure specified layers are valid/supported

### Neutral

1. **Documentation Updates**
   - Performance profile documentation needs updates
   - API examples need to reflect new format
   - Design documentation updates

## Implementation Plan

### Phase 1: Core Changes (Completed)
- [x] Add `layersRequired` field to `Metric.java` as `List<String>`
- [x] Implement layer checking logic in `RecommendationEngine.java` at metric level
- [x] Update performance profile JSON files with `layers_required` for runtime metrics
- [x] Update performance profile YAML files with `layers_required` for runtime metrics

### Phase 2: Testing
- [ ] Unit tests for layer detection logic
- [ ] Integration tests with various layer combinations
- [ ] Performance benchmarking (query reduction metrics)
- [ ] Validation with real workloads

### Phase 3: Documentation
- [ ] Update Performance Profile API documentation
- [ ] Update design documentation
- [ ] Add examples to API samples

### Phase 4: Deployment
- [ ] Release notes highlighting new feature
- [ ] Monitoring for query reduction metrics
- [ ] User communication about new capability

## Alternatives Considered

### Alternative 1: Use Single String Instead of Array
**Approach:** Use a single string field for one layer only

**Pros:**
- Simpler implementation
- Easier to parse

**Cons:**
- Cannot handle metrics applicable to multiple runtimes
- Would require duplicate metric definitions for multi-runtime support
- Less flexible for future extensions

**Decision:** Rejected - Array format provides necessary flexibility

### Alternative 2: Use Comma-Separated String
**Approach:** Store multiple layers as comma-separated string (e.g., "hotspot,semeru")

**Pros:**
- Single field
- Compact representation

**Cons:**
- Requires string parsing (`.split(",")`)
- Less type-safe
- Needs trimming
- Not idiomatic for modern JSON/YAML APIs
- More error-prone

**Decision:** Rejected - Array format is cleaner and more maintainable

### Alternative 3: Single Layer Only (Strict Matching)
**Approach:** Allow only one layer per metric query

**Pros:**
- Simpler implementation
- No OR logic needed

**Cons:**
- Less flexible
- Cannot handle metrics applicable to multiple runtimes
- Would require duplicate metric definitions

**Decision:** Rejected - Multiple layers support is essential for metrics like `jvm_info` that apply to both Hotspot and Semeru

### Alternative 4: Layer Detection at Profile Load Time
**Approach:** Filter metrics when loading performance profile

**Pros:**
- One-time check
- Potentially faster at query time

**Cons:**
- Less dynamic
- Cannot adapt to runtime changes
- More complex profile loading logic

**Decision:** Rejected - Runtime checking is more flexible and accurate

## Related Work

- **Layer Detection System:** [KruizeLayers.md](./KruizeLayers.md)
- **Performance Profiles:** [PerformanceProfile.md](./PerformanceProfile.md)
- **Recommendation Engine:** Core recommendation logic in `RecommendationEngine.java`

## References

- JIRA Ticket: [KRUIZE-1187](https://redhat.atlassian.net/browse/KRUIZE-1187) Conditionally Execute Runtime Metric Queries Only When Runtime Layer Is Detected
- PR: https://github.com/kruize/autotune/pull/1866
- Layer Detection Documentation: [KruizeLayers.md](./KruizeLayers.md)

## Notes

### Supported Runtime Layers
Currently supported runtime layers that can be specified in `layers_required`:
- `hotspot` - OpenJDK Hotspot JVM
- `semeru` - IBM Semeru JVM
- `quarkus` - Quarkus framework (future runtime metrics)

### Example Usage

**Container-level metrics (no layer requirement - backward compatible):**
```json
{
  "name": "cpuUsage",
  "datasource": "prometheus",
  "value_type": "double",
  "kubernetes_object": "container",
  "aggregation_functions": [
    {
      "function": "avg",
      "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{...}[$MEASUREMENT_DURATION_IN_MIN$m]))"
    }
  ]
}
```

**Runtime-specific metrics (new feature):**
```json
{
  "name": "jvmInfo",
  "datasource": "prometheus",
  "value_type": "double",
  "kubernetes_object": "container",
  "layers_required": ["hotspot", "semeru"],
  "aggregation_functions": [
    {
      "function": "sum",
      "query": "sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"})"
    }
  ]
}
```

**Behavior:**
- If container has Hotspot JVM → All `jvmInfo` queries are executed
- If container has Semeru JVM → All `jvmInfo` queries are executed  
- If container has no JVM runtime → All `jvmInfo` queries are **skipped**
- `cpuUsage` queries are **always** executed (no layer requirement)

**Efficiency Benefit:**
- Single layer check per metric (not per aggregation function)
- If metric has 5 aggregation functions, only 1 check is performed instead of 5

## Decision Date
2026-04-16

## Decision Makers
- Kruize Development Team

## Review Status
- [ ] Technical Review
- [ ] Architecture Review