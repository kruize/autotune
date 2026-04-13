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


### Problem Statement

Even if a container has no JVM runtime, queries like `jvm_info` are still executed, wasting resources.


**Expected Behavior:**
- Detect whether a runtime layer (e.g., JVM or other supported runtime) is present
- Execute runtime metric queries such as `jvm_info`, `heap_memory`, etc., only if the runtime layer detection flag is true
- Skip runtime queries when no runtime layer is identified

**Definition of Done:**
- Runtime metric queries are triggered only when runtime layer detection succeeds
- No runtime queries are executed when runtime layer detection is false
- Existing functionality for runtime metric processing remains unaffected when a runtime layer is present

## Decision

We will introduce a **new optional field** `required_layer` in the aggregation function definition within performance profiles. This field will specify which layer(s) must be detected before executing the associated metric query.

### Design Changes

#### 1. Performance Profile Schema Enhancement

**New Field Introduction:**

We are adding an optional `required_layer` field to aggregation functions. This field accepts an array of layer names.

**Before (No layer checking):**
```json
{
  "name": "jvmInfo",
  "aggregation_functions": [
    {
      "function": "sum",
      "query": "sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"})"
      // No layer requirement - query always executed
    }
  ]
}
```

**After (With layer requirement):**
```json
{
  "name": "jvmInfo",
  "aggregation_functions": [
    {
      "function": "sum",
      "query": "sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"})",
      "required_layer": ["hotspot", "semeru"]
      // Query executed only if hotspot OR semeru layer detected
    }
  ]
}
```

**YAML Representation:**
```yaml
- name: jvmInfo
  aggregation_functions:
    - function: sum
      query: 'sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace="$NAMESPACE$", container="$CONTAINER_NAME$"})'
      required_layer:
        - "hotspot"
        - "semeru"
```

**Key Points:**
- `required_layer` is **optional** - if not specified, query is always executed (backward compatible)
- Accepts an **array of strings** representing layer names
- Uses **OR logic** - query executes if ANY of the specified layers is detected
- Empty array or null means no layer requirement (always execute)

### Key Design Decisions

1. **Optional Field** - Backward compatible; existing metrics without `required_layer` work unchanged
2. **Array Format** - Using `List<String>` for type safety and clean representation
3. **OR Logic** - Query executes if ANY specified layer is detected (flexible for multi-runtime support)
4. **Early Exit** - Skip query execution immediately if layer not detected (performance optimization)
5. **Debug Logging** - Log skipped queries for troubleshooting and monitoring

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
   - Cleaner data structure
   - Easier to add new runtime layers
   - Type-safe implementation

5. **Extensibility**
   - Pattern can be applied to other layer-specific metrics
   - Supports multi-layer requirements (OR logic)

### Negative

1. **Schema Change**
   - Performance profile schema is extended with new optional field
   - Requires documentation updates

2. **Testing Requirements**
   - Need to test with various layer combinations
   - Verify backward compatibility (metrics without `required_layer`)
   - Validate query skipping logic
   - Test OR logic with multiple layers

3. **Potential for Misconfiguration**
   - Incorrect layer names in `required_layer` will cause queries to be skipped
   - Need validation to ensure specified layers are valid/supported

### Neutral

1. **Documentation Updates**
   - Performance profile documentation needs updates
   - API examples need to reflect new format
   - Migration guide for existing users

## Implementation Plan

### Phase 1: Core Changes (Completed)
- [x] Add `requiredLayer` field to `AggregationFunctions.java` as `List<String>`
- [x] Implement layer checking logic in `RecommendationEngine.java`
- [x] Update performance profile JSON files with `required_layer` for runtime metrics
- [x] Update performance profile YAML files with `required_layer` for runtime metrics

### Phase 2: Testing
- [ ] Unit tests for layer detection logic
- [ ] Integration tests with various layer combinations
- [ ] Performance benchmarking (query reduction metrics)
- [ ] Validation with real workloads

### Phase 3: Documentation
- [ ] Update Performance Profile API documentation
- [ ] Create migration guide for existing users
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
Currently supported runtime layers that can be specified in `required_layer`:
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
      // No required_layer field - query always executed (backward compatible)
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
  "aggregation_functions": [
    {
      "function": "sum",
      "query": "sum by(container, namespace, runtime, vendor, version)(jvm_info{namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"})",
      "required_layer": ["hotspot", "semeru"]
      // NEW: Only executed if hotspot OR semeru layer detected
    }
  ]
}
```

**Behavior:**
- If container has Hotspot JVM → `jvmInfo` query is executed
- If container has Semeru JVM → `jvmInfo` query is executed
- If container has no JVM runtime → `jvmInfo` query is **skipped**
- `cpuUsage` query is **always** executed (no layer requirement)

## Decision Date
2026-04-10

## Decision Makers
- Kruize Development Team

## Review Status
- [ ] Technical Review
- [ ] Architecture Review