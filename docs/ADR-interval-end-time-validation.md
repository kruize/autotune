# Architecture Decision Record: Unified Validation Strategy for interval_end_time Parameter

## Status
Proposed

## Context

Kruize operates in two monitoring modes: **local** and **remote**, each with separate API endpoints for generating recommendations:

- **Remote Mode**: `UpdateRecommendations.java` - Makes `interval_end_time` **mandatory**
- **Local Mode**: `GenerateRecommendations.java` - Makes `interval_end_time` **optional** (defaults to last 15 days if not provided)

A new unified API (`RecommendationsResource.java`) has been introduced to:
1. Combine both local and remote API calls into a single endpoint
2. Support the new pod_count feature with enhanced schema

### Current Issues

1. **Redundancy**: The new API currently defaults to remote mode and requires users to explicitly pass `target=local` for local mode, creating unnecessary complexity
2. **Inconsistent Validation**: Two separate validation methods (`validate()` for remote, `validate_local()` for local) exist in `RecommendationEngine.java`
3. **User Experience**: Different expectations for the same parameter based on mode creates confusion
4. **Maintenance Overhead**: Duplicate validation logic increases code complexity and maintenance burden

### Current Implementation

**Remote Mode (`validate()` method)**:
```java
// interval_end_time is MANDATORY
if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
    validationFailureMsg += "interval_end_time is mandatory";
}
```

**Local Mode (`validate_local()` method)**:
```java
// interval_end_time is OPTIONAL
if (intervalEndTimeStr != null) {
    // Only validate format if provided
    if (!Utils.DateUtils.isAValidDate(...)) {
        validationFailureMsg += "Invalid timestamp";
    }
}
```

## Decision Drivers

1. **User Experience**: Minimize user burden and provide intuitive defaults
2. **Consistency**: Uniform behavior across monitoring modes
3. **Backward Compatibility**: Avoid breaking existing integrations
4. **Performance**: Minimize database queries and computational overhead
5. **Maintainability**: Reduce code duplication and complexity
6. **Flexibility**: Support both explicit and implicit mode determination

## Options Considered

### Option 1: Make interval_end_time Mandatory for Both Modes

**Description**: Require users to always provide `interval_end_time` for both local and remote monitoring modes.

**Pros**:
- ✅ Simplest validation logic - single code path
- ✅ Explicit and predictable behavior
- ✅ Users have full control over the time window
- ✅ No ambiguity about which data is being analyzed

**Cons**:
- ❌ Breaks backward compatibility for local mode users
- ❌ Increases user burden - must calculate/provide timestamp
- ❌ Less convenient for common use case (latest recommendations)
- ❌ May require client-side changes for existing local mode integrations

**Implementation Complexity**: Low

**Code Changes**:
```java
public String validate() {
    String validationFailureMsg = "";
    if (experimentName == null || experimentName.isEmpty()) {
        validationFailureMsg += "experiment_name is mandatory, ";
    }
    if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
        validationFailureMsg += "interval_end_time is mandatory";
    } else if (!Utils.DateUtils.isAValidDate(...)) {
        validationFailureMsg += "Invalid timestamp format";
    }
    return validationFailureMsg;
}
```

---

### Option 2: Relax interval_end_time Check - Default to Current Timestamp (RECOMMENDED)

**Description**: Make `interval_end_time` optional for both modes. If not provided, default to the current timestamp for both local and remote monitoring.

**Pros**:
- ✅ Best user experience - works out of the box
- ✅ Backward compatible with local mode
- ✅ Consistent behavior across both modes
- ✅ Single validation method eliminates code duplication
- ✅ Intuitive default (latest available data)
- ✅ Reduces API complexity for common use cases

**Cons**:
- ⚠️ Slight behavior change for remote mode (currently mandatory)
- ⚠️ May return different results if called at different times without explicit timestamp
- ⚠️ ROS needs to be aware of the default behavior

**Implementation Complexity**: Low

**Code Changes**:
```java
public String validate() {
    String validationFailureMsg = "";
    
    // experiment_name is always mandatory
    if (experimentName == null || experimentName.isEmpty()) {
        validationFailureMsg += "experiment_name is mandatory, ";
    }
    
    // interval_end_time is optional - validate format only if provided
    if (intervalEndTimeStr != null && !intervalEndTimeStr.isEmpty()) {
        if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
            validationFailureMsg += String.format("Invalid timestamp format: %s", intervalEndTimeStr);
        }
    }
    // If not provided, will default to current timestamp in prepareRecommendations()
    
    return validationFailureMsg;
}

public KruizeObject prepareRecommendations(int calCount, String target_cluster, String bulkJobID) {
    // Default to current timestamp if not provided
    if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
        interval_end_time = new Timestamp(System.currentTimeMillis());
        LOGGER.info("interval_end_time not provided, defaulting to current timestamp: {}", interval_end_time);
    } else {
        interval_end_time = Utils.DateUtils.getTimeStampFrom(
            KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, 
            intervalEndTimeStr
        );
    }
    setInterval_end_time(interval_end_time);
    // ... rest of the logic
}
```

**Migration Path**:
- Update documentation to reflect optional parameter
- Add deprecation notice for remote mode users relying on mandatory validation
- Provide clear examples in API documentation

---

### Option 3: Rely on Deployment Config Parameter to Decide Mode

**Description**: Use the deployment configuration parameter (`KruizeDeploymentInfo.local` or `KruizeDeploymentInfo.isROSEnabled`) to automatically determine if the system is in local or remote mode, and apply corresponding validation.

**Pros**:
- ✅ No need for users to specify `target` parameter
- ✅ Automatic mode detection based on deployment
- ✅ Maintains current validation behavior for each mode
- ✅ No API changes required from user perspective

**Cons**:
- ❌ Deployment config may not reflect experiment-specific requirements
- ❌ Still maintains duplicate validation logic
- ❌ Doesn't solve the core redundancy problem
- ❌ Tight coupling between deployment config and API behavior
- ❌ Cannot support multi-cluster scenarios where both modes coexist

**Implementation Complexity**: Medium

**Code Changes**:
```java
public String validate() {
    String validationFailureMsg = "";
    
    if (experimentName == null || experimentName.isEmpty()) {
        validationFailureMsg += "experiment_name is mandatory, ";
    }
    
    // Check deployment config to determine validation rules
    if (KruizeDeploymentInfo.is_ros_enabled) {
        // Remote mode - interval_end_time is mandatory
        if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
            validationFailureMsg += "interval_end_time is mandatory for remote monitoring";
        }
    } else {
        // Local mode - interval_end_time is optional
        if (intervalEndTimeStr != null && !intervalEndTimeStr.isEmpty()) {
            if (!Utils.DateUtils.isAValidDate(...)) {
                validationFailureMsg += "Invalid timestamp format";
            }
        }
    }
    
    return validationFailureMsg;
}
```

---

### Option 4: Rely on Experiment's target_cluster and Validate Accordingly

**Description**: Fetch the experiment details from the database at the beginning of the API call to determine the `target_cluster` value, then apply mode-specific validation.

**Pros**:
- ✅ Validation matches the actual experiment configuration
- ✅ Most accurate mode determination
- ✅ Supports per-experiment mode configuration
- ✅ No need for user to specify target parameter

**Cons**:
- ❌ **Additional database query** at the start of every API call (performance impact)
- ❌ Increased latency for validation
- ❌ Still maintains duplicate validation logic
- ❌ Doesn't solve the core redundancy problem
- ❌ Complexity in error handling if experiment doesn't exist
- ❌ Database dependency for validation logic

**Implementation Complexity**: High

**Code Changes**:
```java
public String validate() {
    String validationFailureMsg = "";
    
    if (experimentName == null || experimentName.isEmpty()) {
        validationFailureMsg += "experiment_name is mandatory, ";
        return validationFailureMsg; // Cannot proceed without experiment name
    }
    
    // Fetch experiment from database to determine target_cluster
    Map<String, KruizeObject> experimentMap = new ConcurrentHashMap<>();
    try {
        new ExperimentDBService().loadExperimentFromDBByName(experimentMap, experimentName);
        KruizeObject experiment = experimentMap.get(experimentName);
        
        if (experiment == null) {
            validationFailureMsg += "Experiment not found: " + experimentName;
            return validationFailureMsg;
        }
        
        String targetCluster = experiment.getTarget_cluster();
        
        if (AnalyzerConstants.REMOTE.equalsIgnoreCase(targetCluster)) {
            // Remote mode - interval_end_time is mandatory
            if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
                validationFailureMsg += "interval_end_time is mandatory for remote monitoring";
            }
        } else {
            // Local mode - interval_end_time is optional
            if (intervalEndTimeStr != null && !intervalEndTimeStr.isEmpty()) {
                if (!Utils.DateUtils.isAValidDate(...)) {
                    validationFailureMsg += "Invalid timestamp format";
                }
            }
        }
    } catch (Exception e) {
        validationFailureMsg += "Error fetching experiment: " + e.getMessage();
    }
    
    return validationFailureMsg;
}
```

---

## Decision

**We recommend Option 2: Relax interval_end_time check and default to current timestamp for both local and remote monitoring modes.**

### Rationale

1. **Best User Experience**: Users can get recommendations without calculating timestamps, while still having the option to specify exact time windows when needed

2. **Eliminates Redundancy**: Single validation method (`validate()`) replaces both `validate()` and `validate_local()`, reducing code duplication

3. **Consistent Behavior**: Both modes behave identically, reducing cognitive load and potential for errors

4. **Backward Compatible**: Local mode users experience no breaking changes; remote mode users gain flexibility

5. **Performance**: No additional database queries required (unlike Option 4)

6. **Maintainability**: Simpler codebase with single validation path

7. **Flexibility**: Users can still specify exact timestamps when reproducibility is required

### Implementation Plan

#### Phase 1: Update Validation Logic
1. Merge `validate()` and `validate_local()` into a single `validate()` method
2. Make `interval_end_time` optional with format validation only if provided
3. Remove `validate_local()` method

#### Phase 2: Update Recommendation Preparation
1. Add default timestamp logic in `prepareRecommendations()` method
2. Log when default timestamp is used for debugging/auditing
3. Ensure timestamp is consistently applied across all recommendation calculations

#### Phase 3: Update API Layer
1. Remove separate validation calls in `RecommendationsResource.java`
2. Simplify target parameter handling (can still support explicit target for future use cases)
3. Update response to include the actual `interval_end_time` used

#### Phase 4: Documentation and Communication
1. Update API documentation to reflect optional parameter
2. Add examples showing both explicit and implicit timestamp usage
3. Update migration guide for remote mode users
4. Add release notes highlighting the change

### Code Changes Summary

**RecommendationEngine.java**:
```java
// Remove validate_local() method entirely

public String validate() {
    String validationFailureMsg = "";
    
    if (experimentName == null || experimentName.isEmpty()) {
        validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY + ", ";
    }
    
    // interval_end_time is now optional - only validate format if provided
    if (intervalEndTimeStr != null && !intervalEndTimeStr.isEmpty()) {
        if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
            validationFailureMsg += String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalEndTimeStr);
        }
    }
    
    return validationFailureMsg;
}

public KruizeObject prepareRecommendations(int calCount, String target_cluster, String bulkJobID) throws FetchMetricsError {
    // Default to current timestamp if not provided
    if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
        interval_end_time = new Timestamp(System.currentTimeMillis());
        LOGGER.info("interval_end_time not provided for experiment '{}', defaulting to current timestamp: {}", 
                    experimentName, interval_end_time);
    } else {
        interval_end_time = Utils.DateUtils.getTimeStampFrom(
            KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, 
            intervalEndTimeStr
        );
    }
    setInterval_end_time(interval_end_time);
    
    // ... rest of existing logic
}
```

**RecommendationsResource.java**:
```java
@Override
protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // ... existing setup code ...
    
    RecommendationEngine recommendationEngine = new RecommendationEngine(experimentName, intervalEndTimeStr, intervalStartTimeStr);
    
    // Single validation call for both modes
    String validationMessage = recommendationEngine.validate();
    
    if (validationMessage.isEmpty()) {
        KruizeObject kruizeObject = recommendationEngine.prepareRecommendations(calCount, target, bulkJobID);
        // ... rest of logic
    } else {
        sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, validationMessage);
    }
}
```

### Migration Strategy

**For Remote Mode Users**:
- **Impact**: Low - existing code continues to work
- **Action**: Optional - can remove explicit timestamp calculation if always wanting latest data
- **Timeline**: Immediate (backward compatible)

**For Local Mode Users**:
- **Impact**: None - behavior unchanged
- **Action**: None required
- **Timeline**: N/A

### Monitoring and Rollback

1. **Metrics to Track**:
   - API call success/failure rates
   - Percentage of calls with/without explicit `interval_end_time`
   - Response time distribution

2. **Rollback Plan**:
   - If issues arise, can quickly revert to separate validation methods
   - Feature flag can be added to toggle between old and new behavior
   - Database schema unchanged, so no data migration needed

### Future Considerations

1. **Enhanced Defaults**: Could calculate default based on experiment's data availability (e.g., last available data point) rather than current timestamp
2. **Time Range Recommendations**: Support `interval_start_time` for range-based recommendations
3. **Caching**: Cache recent recommendations to avoid recalculation for same timestamp
4. **Validation Framework**: Consider implementing a pluggable validation framework for future extensibility

## Consequences

### Positive
- ✅ Simplified codebase with single validation path
- ✅ Better user experience with sensible defaults
- ✅ Consistent behavior across monitoring modes
- ✅ Reduced maintenance burden
- ✅ Backward compatible with local mode

### Negative
- ⚠️ Slight behavior change for remote mode (now optional instead of mandatory)
- ⚠️ Users must be aware that omitting timestamp uses current time
- ⚠️ Potential for confusion if users expect historical data but get current timestamp

### Neutral
- 📝 Documentation updates required
- 📝 Need to communicate change to existing users

## References

- [UpdateRecommendations.java](../src/main/java/com/autotune/analyzer/services/UpdateRecommendations.java)
- [GenerateRecommendations.java](../src/main/java/com/autotune/analyzer/services/GenerateRecommendations.java)
- [RecommendationsResource.java](../src/main/java/com/autotune/analyzer/services/RecommendationsResource.java)
- [RecommendationEngine.java](../src/main/java/com/autotune/analyzer/recommendations/engine/RecommendationEngine.java)

## Decision Date
2026-05-14

## Suggestions



