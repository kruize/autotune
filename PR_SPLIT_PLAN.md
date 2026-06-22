# PR Split Plan for feature/bulk-api-cluster-settings-clean

## Overview
The current branch contains 3 major features that need to be split into separate PRs:
1. **cluster_name support in Kruize config for datasources**
2. **Bulk payload model and term settings support**
3. **Notifications (Over/Under provisioned, Optimized)**

---

## PR 1: cluster_name Support in Kruize Config

### Description
Add support for cluster_name in datasource configuration and use it instead of hardcoded "default" cluster name.

### Files to Include

#### Documentation
- `design/KruizeDatasource.md` - Add cluster_name field documentation (PARTIAL - only cluster_name sections)
- `design/BulkAPI.md` - Add cluster_name field in bulk API (PARTIAL - only cluster_name sections, lines 62, 108-111)

#### Database Migration
- `migrations/lm/v111__add_cluster_to_datasources.sql` - Add clusters column to datasources table

#### Source Code - Datasource Related
- `src/main/java/com/autotune/analyzer/adapters/DataSourceInfoAdapter.java` - Add cluster list handling
- `src/main/java/com/autotune/common/datasource/DataSourceInfo.java` - Add clusterList field and methods
- `src/main/java/com/autotune/common/datasource/DataSourceCollection.java` - Update to handle cluster lists
- `src/main/java/com/autotune/database/table/KruizeDataSourceEntry.java` - Add clusters field with getClusterList/setClusterList methods
- `src/main/java/com/autotune/database/helper/DBHelpers.java` - Update datasource queries to include clusters
- `src/main/java/com/autotune/analyzer/services/ListDatasources.java` - Include cluster info in response

#### Source Code - Bulk API Related (cluster_name only)
- `src/main/java/com/autotune/analyzer/serviceObjects/BulkInput.java` - Add cluster_name field (PARTIAL - only cluster_name, not model/term settings)
- `src/main/java/com/autotune/analyzer/workerimpl/BulkJobManager.java` - Use cluster_name from bulk input (PARTIAL - only cluster_name logic)
- `src/main/java/com/autotune/common/bulk/BulkServiceValidation.java` - Add cluster_name validation

#### Tests
- `tests/src/test/java/com/autotune/common/bulk/BulkServiceValidationTest.java` - Add cluster_name validation tests (PARTIAL - only cluster_name tests)
- `tests/src/test/java/com/autotune/database/table/KruizeDataSourceEntryTest.java` - Complete file
- `tests/src/test/java/com/autotune/analyzer/workerimpl/BulkJobManagerPassthroughTest.java` - Complete file
- `tests/scripts/local_monitoring_tests/rest_apis/test_bulkAPI.py` - Add cluster_name validation tests (lines 380-445)

### Key Changes
1. Add `clusters` column to datasources table (comma-separated list)
2. Add `clusterList` field to DataSourceInfo and related classes
3. Add `cluster_name` field to BulkInput with validation
4. Update BulkJobManager to use cluster_name from payload or datasource metadata
5. Add comprehensive unit tests for cluster functionality

---

## PR 2: Bulk Payload Model and Term Settings Support

### Description
Add support for model_settings and term_settings in bulk API payload to allow selective recommendation generation.

### Files to Include

#### Documentation
- `design/BulkAPI.md` - Add model_settings and term_settings documentation (PARTIAL - lines 65-74, 112-161)

#### Source Code
- `src/main/java/com/autotune/analyzer/serviceObjects/BulkInput.java` - Add modelSettings and termSettings fields (PARTIAL - only these fields)
- `src/main/java/com/autotune/analyzer/workerimpl/BulkJobManager.java` - Pass settings to experiment creation (PARTIAL - only model/term settings logic)
- `src/main/java/com/autotune/common/bulk/BulkServiceValidation.java` - Add model_settings and term_settings validation (PARTIAL)

#### Tests
- `tests/src/test/java/com/autotune/common/bulk/BulkServiceValidationTest.java` - Add model/term settings validation tests (PARTIAL - only model/term tests)
- `tests/scripts/local_monitoring_tests/rest_apis/test_bulkAPI.py` - Add model/term settings tests (lines 446-628)

### Key Changes
1. Add `model_settings` field with `models` array (valid: "performance", "cost")
2. Add `term_settings` field with `terms` array (valid: "short", "medium", "long")
3. Add validation for model and term names (case-insensitive)
4. Pass settings to experiment creation for selective recommendation generation
5. Add comprehensive validation tests

---

## PR 3: Notifications (Over/Under Provisioned, Optimized)

### Description
Add provisioning status notifications to indicate if workloads are over-provisioned, under-provisioned, or optimized.

### Files to Include

#### Documentation
- `design/NotificationCodes.md` - Add new notification codes for provisioning status

#### Source Code
- `src/main/java/com/autotune/analyzer/recommendations/RecommendationConstants.java` - Add notification constants (PARTIAL - only provisioning notifications)
- `src/main/java/com/autotune/analyzer/recommendations/engine/RecommendationEngine.java` - Add provisioning status logic (PARTIAL - only notification logic around lines 928-935, 1020-1027)

#### Tests
- Tests for provisioning notifications (if any specific tests exist)

### Key Changes
1. Add notification codes:
   - `NOTICE_CPU_REQUESTS_OPTIMISED` (321003)
   - `NOTICE_CPU_LIMITS_OPTIMISED` (321004)
   - `NOTICE_CPU_REQUESTS_UNDER_PROVISIONED` (321005)
   - `NOTICE_CPU_REQUESTS_OVER_PROVISIONED` (321006)
   - `NOTICE_MEMORY_REQUESTS_OPTIMISED` (321007)
   - `NOTICE_MEMORY_LIMITS_OPTIMISED` (321008)
   - `NOTICE_MEMORY_REQUESTS_UNDER_PROVISIONED` (321009)
   - `NOTICE_MEMORY_REQUESTS_OVER_PROVISIONED` (321010)

2. Add logic in RecommendationEngine to:
   - Compare recommended vs current values
   - Check if difference exceeds threshold
   - Add appropriate notification (optimized/under/over provisioned)

---

## Implementation Steps

### For PR 1 (cluster_name support):
```bash
# Already on feature/cluster-name-support branch
# Cherry-pick and manually extract cluster_name related changes
git cherry-pick 9f4d8d62  # Contains datasource adapter and validation
git cherry-pick 12982bf2  # Contains validation tests
git cherry-pick 38e89315  # Contains remaining files

# Then manually remove model_settings, term_settings, and notification changes
# Commit the cleaned up changes
git add .
git commit -m "Add cluster_name support in datasource config and bulk API"
```

### For PR 2 (model and term settings):
```bash
git checkout -b feature/bulk-model-term-settings origin/runtimes-iirj
# Cherry-pick and extract model/term settings changes
# Manually extract only model_settings and term_settings related code
git add .
git commit -m "Add model_settings and term_settings support in bulk API"
```

### For PR 3 (notifications):
```bash
git checkout -b feature/provisioning-notifications origin/runtimes-iirj
# Cherry-pick and extract notification changes
# Manually extract only provisioning notification code
git add .
git commit -m "Add provisioning status notifications (over/under/optimized)"
```

---

## Notes
- Some files contain changes for multiple features and need to be split carefully
- The commits are not cleanly separated by feature, so manual extraction is required
- Each PR should be tested independently before raising
- PRs should be raised in order: 1 → 2 → 3 (as PR 2 and 3 may depend on PR 1)