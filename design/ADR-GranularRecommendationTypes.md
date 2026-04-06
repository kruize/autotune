# ADR: Granular Recommendation Types Configuration

## Status
**PROPOSED** - Awaiting team review and approval

## Context

### Current State
Currently, Kruize does **not** have a `recommendation_types` configuration field in `recommendation_settings`. The system generates all recommendation types by default without user control.

**Current behavior:**
- All resource recommendations (CPU and memory) are generated
- All detected runtime layer recommendations (hotspot, quarkus, semeru) are generated
- All accelerator recommendations are generated (if applicable)
- No way for users to selectively enable/disable specific types

**Limitations:**
- **No user control**: Cannot disable unwanted recommendation types
- **All-or-nothing**: Either get all recommendations or none
- **No runtime granularity**: Cannot select specific runtime layers (e.g., only quarkus, not hotspot)
- **No resource granularity**: Cannot select only CPU or only memory recommendations
- **Performance impact**: Processing all types even when not needed
- **User feedback**: Users want to select specific runtimes (e.g., only quarkus, not hotspot/java)

### Problem Statement
Users need granular control to:
1. Select specific runtime layers independently (e.g., only quarkus recommendations, excluding hotspot)
2. Choose specific resource types (CPU vs memory)
3. Disable certain recommendation categories they don't need
4. Support future runtime additions (nodejs, springboot, etc.)
5. Reduce recommendation processing time by skipping unwanted types

### User Story
> "As a Quarkus application developer, I want to receive only Quarkus-specific runtime recommendations without Hotspot JVM recommendations, so that I get relevant tuning suggestions for my framework."

## Decision

### Proposed Solution
Introduce a **new** nested object structure `recommendation_types_config` in `recommendation_settings`:

```json
{
  "recommendation_types_config": {
    "resources": ["cpu", "memory"],
    "runtimes": ["hotspot", "quarkus", "semeru"],
    "accelerators": ["gpu"]
  }
}
```

### Design Principles
1. **Granularity**: Each category (resources, runtimes, accelerators) is independently configurable
2. **Extensibility**: New runtime types can be added without API changes
3. **Default Behavior**: Null/empty values enable all types (maintains current behavior)
4. **Optional**: Field is completely optional - omitting it generates all recommendations (current behavior)
5. **Future-Proof**: Structure supports unlimited expansion

## Implementation Details

### 1. New Data Structure

**Class: `RecommendationTypesConfig`**
```java
public class RecommendationTypesConfig {
    private List<String> resources;      // ["cpu", "memory"]
    private List<String> runtimes;       // ["hotspot", "quarkus", "semeru", "nodejs", "springboot"]
    private List<String> accelerators;   // ["gpu"]
    
    // Helper methods
    public boolean isResourceEnabled(String resourceType);
    public boolean isRuntimeEnabled(String runtimeLayer);
    public boolean isAcceleratorEnabled(String acceleratorType);
}
```

### 2. Constants Organization

**New constant classes in `KruizeConstants`:**
```java
public static final class ResourceTypes {
    public static final String CPU = "cpu";
    public static final String MEMORY = "memory";
}

public static final class RuntimeLayers {
    public static final String HOTSPOT = "hotspot";
    public static final String QUARKUS = "quarkus";
    public static final String SEMERU = "semeru";
    public static final String NODEJS = "nodejs";
    public static final String SPRINGBOOT = "springboot";
}

public static final class AcceleratorTypes {
    public static final String GPU = "gpu";
}
```

### 3. API Changes

**This is a new feature addition:**
- New optional field `recommendation_types_config` in `recommendation_settings`
- When omitted, all recommendation types are generated (current default behavior)
- No breaking changes to existing API
- Fully backward compatible - existing experiments continue to work unchanged

## Examples

### Example 1: Only Quarkus Runtime (No Hotspot/Semeru)
```json
{
  "recommendation_settings": {
    "threshold": "0.1",
    "recommendation_types_config": {
      "runtimes": ["quarkus"]
    }
  }
}
```

### Example 2: CPU + Memory with Hotspot Only
```json
{
  "recommendation_settings": {
    "threshold": "0.1",
    "recommendation_types_config": {
      "resources": ["cpu", "memory"],
      "runtimes": ["hotspot"]
    }
  }
}
```

### Example 3: CPU Only with Multiple Runtimes
```json
{
  "recommendation_settings": {
    "threshold": "0.1",
    "recommendation_types_config": {
      "resources": ["cpu"],
      "runtimes": ["hotspot", "quarkus", "springboot"]
    }
  }
}
```

## Consequences

### Positive
1. ✅ **User Empowerment**: Users get fine-grained control over recommendations
2. ✅ **Flexibility**: Mix and match any combination of types
3. ✅ **Extensibility**: Easy to add new runtime layers (nodejs, springboot, etc.)
4. ✅ **Backward Compatible**: No breaking changes for existing users
5. ✅ **Clear Structure**: Nested object is more intuitive than flat array
6. ✅ **Performance**: Skip processing for disabled layers
7. ✅ **Future-Proof**: Structure supports unlimited expansion

### Negative
1. ⚠️ **Testing**: More test cases needed for various combinations
2. ⚠️ **Learning Curve**: Users need to learn new format (optional feature)
3. ⚠️ **Validation**: Need to validate runtime layer names against supported types

### Neutral
1. 📝 **API Size**: Slightly larger JSON payload (negligible impact)
2. 📝 **Code Size**: Additional class and methods (well-organized)

## Alternatives Considered

### Alternative 1: Extend Flat Array with Specific Types
```json
{
  "recommendation_types": ["resource", "hotspot", "quarkus"]
}
```
**Rejected because:**
- Mixing category-level and item-level types is confusing
- Unclear semantics: Does "resource" mean all resources or just a category?
- Not extensible for future categories

### Alternative 2: Separate Fields for Each Category
```json
{
  "resource_types": ["cpu", "memory"],
  "runtime_types": ["hotspot", "quarkus"],
  "accelerator_types": ["gpu"]
}
```
**Rejected because:**
- Proliferates top-level fields
- Less cohesive than nested structure
- Harder to add new categories

### Alternative 3: String-based Filtering
```json
{
  "recommendation_filter": "resources.cpu,runtimes.hotspot,runtimes.quarkus"
}
```
**Rejected because:**
- String parsing is error-prone
- Not type-safe
- Poor developer experience


## Open Questions for Team Discussion

1. **Naming Convention**: Is `recommendation_types_config` the best name, or should it be `recommendation_types`, `recommendation_config`, or something else?

2. **Default Behavior**: Should empty arrays mean "all enabled" or "none enabled"? Current implementation: empty = all enabled (maintains current behavior)

3. **Validation**: Should we validate that specified runtime layers are actually supported/detected? Or allow any string for future extensibility?

4. **Future Categories**: Are there other categories we should plan for? (e.g., `storage`, `network`, `security`?)

5. **Extensibility**: Should we allow custom/plugin runtime types, or keep it to predefined constants only?

6. **Runtime Layer Detection**: Should we skip recommendation generation if a specified runtime layer is not detected in the application? Or generate recommendations anyway?

7. **Error Handling**: How should we handle invalid/unknown runtime layer names? Ignore them, log warnings, or return errors?

8. **Documentation**: Should we provide migration guides even though this is a new feature? (To help users understand when to use it)

## References

- PR: https://github.com/kruize/autotune/pull/1851
- User Story: "User should be given an option to select which runtime recommendation they would need"
- Implementation Details: `RUNTIME_RECOMMENDATIONS_CHANGES_V2.md`
- API Documentation: `design/KruizeLocalAPI.md`

## Decision Makers

- [ ] Architecture Team
- [ ] API Team
- [ ] Product Owner
- [ ] Engineering Lead

## Review Comments

_Space for team members to add comments and feedback_

---

**Date**: 2026-04-06  
**Author**: Development Team  
**Reviewers**: _To be assigned_  
**Status**: PROPOSED