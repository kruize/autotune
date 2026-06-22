# Complete Guide: Split PR into 3 Separate PRs

## ✅ PR 1: cluster_name Support - COMPLETED

**Branch:** `cluster-name-support` in your fork (gulati-aakriti/autotune)
**Status:** ✅ Pushed and ready for PR
**URL:** https://github.com/gulati-aakriti/autotune/tree/cluster-name-support

### What's Included:
- Database migration for clusters column
- cluster_name field in BulkInput with DNS-1123 validation
- Datasource support for multiple clusters
- Comprehensive unit tests
- Documentation updates

### Create PR:
```
Title: Add cluster_name support in datasource config and bulk API
Base: kruize:runtimes-iirj
Head: gulati-aakriti:cluster-name-support

Description:
This PR adds support for cluster_name in datasource configuration and bulk API.

**Changes:**
- Add clusters column to datasources table for multi-cluster support
- Add cluster_name field to BulkInput with DNS-1123 validation  
- Support cluster_name override in bulk API (overrides datasource metadata)
- Add comprehensive unit tests for cluster functionality
- Update documentation with cluster_name usage examples

**Testing:**
- Unit tests: BulkServiceValidationTest, KruizeDataSourceEntryTest
- Integration tests: BulkJobManagerPassthroughTest
```

---

## 🔄 PR 2: Model and Term Settings Support - TODO

### Steps to Create:

1. **Create new branch from base:**
```bash
git checkout -b feature/bulk-model-term-settings origin/runtimes-iirj
```

2. **Cherry-pick commits:**
```bash
git cherry-pick 9f4d8d62 12982bf2 38e89315
```

3. **Remove cluster_name changes:**

**Files to revert/remove:**
```bash
# Remove cluster-name specific files
git rm migrations/lm/v111__add_cluster_to_datasources.sql
git rm src/test/java/com/autotune/database/table/KruizeDataSourceEntryTest.java
git rm src/test/java/com/autotune/analyzer/workerimpl/BulkJobManagerPassthroughTest.java

# Revert files with cluster_name changes
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/adapters/DataSourceInfoAdapter.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/common/datasource/DataSourceInfo.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/common/datasource/DataSourceCollection.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/database/table/KruizeDataSourceEntry.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/database/helper/DBHelpers.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/services/ListDatasources.java

# Revert notification files
git checkout origin/runtimes-iirj -- design/NotificationCodes.md
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/recommendations/RecommendationConstants.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/recommendations/engine/RecommendationEngine.java
```

4. **Edit BulkInput.java - Remove cluster_name, keep model/term settings:**
```java
// Remove these lines:
private String cluster_name;
public String getCluster_name() { return cluster_name; }
public void setCluster_name(String cluster_name) { this.cluster_name = cluster_name; }

// Keep these:
private ModelSettings model_settings;
private TermSettings term_settings;
// ... and their getters/setters
```

5. **Edit BulkServiceValidation.java - Remove cluster validation, keep model/term:**
```java
// Remove:
validateClusterName() method and its call

// Keep:
validateModelSettings() method
validateTermSettings() method
```

6. **Edit BulkServiceValidationTest.java - Remove cluster tests, keep model/term:**
```bash
# Keep lines with ModelSettingsValidationTests and TermSettingsValidationTests
# Remove ClusterNameValidationTests class
```

7. **Edit design/BulkAPI.md - Remove cluster_name docs, keep model/term:**
```markdown
# Remove cluster_name field from examples
# Keep model_settings and term_settings documentation
```

8. **Edit test_bulkAPI.py - Keep model/term tests, remove cluster tests:**
```bash
# Keep lines 446-628 (model/term settings tests)
# Remove lines 380-445 (cluster_name tests)
```

9. **Commit and push:**
```bash
git add .
git commit -S -m "Add model_settings and term_settings support in bulk API

- Add model_settings field to customize recommendation models (performance/cost)
- Add term_settings field to customize recommendation terms (short/medium/long)
- Add validation for model and term names (case-insensitive)
- Support selective recommendation generation for IRI workflows
- Add comprehensive unit tests
- Update documentation with usage examples"

git push aakriti feature/bulk-model-term-settings:bulk-model-term-settings
```

10. **Create PR:**
```
Title: Add model_settings and term_settings support in bulk API
Base: kruize:runtimes-iirj
Head: gulati-aakriti:bulk-model-term-settings
```

---

## 🔔 PR 3: Provisioning Notifications - TODO

### Steps to Create:

1. **Create new branch from base:**
```bash
git checkout -b feature/provisioning-notifications origin/runtimes-iirj
```

2. **Cherry-pick only the last commit:**
```bash
git cherry-pick 38e89315
```

3. **Remove all cluster_name and model/term changes:**

```bash
# Remove cluster-name files
git rm migrations/lm/v111__add_cluster_to_datasources.sql
git rm src/test/java/com/autotune/database/table/KruizeDataSourceEntryTest.java
git rm src/test/java/com/autotune/analyzer/workerimpl/BulkJobManagerPassthroughTest.java

# Revert all files except notification-related ones
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/serviceObjects/BulkInput.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/common/bulk/BulkServiceValidation.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/workerimpl/BulkJobManager.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/adapters/DataSourceInfoAdapter.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/common/datasource/DataSourceInfo.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/common/datasource/DataSourceCollection.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/database/table/KruizeDataSourceEntry.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/database/helper/DBHelpers.java
git checkout origin/runtimes-iirj -- src/main/java/com/autotune/analyzer/services/ListDatasources.java
git checkout origin/runtimes-iirj -- src/test/java/com/autotune/common/bulk/BulkServiceValidationTest.java
git checkout origin/runtimes-iirj -- design/BulkAPI.md
git checkout origin/runtimes-iirj -- tests/scripts/local_monitoring_tests/rest_apis/test_bulkAPI.py
```

4. **Keep only notification files:**
- `design/NotificationCodes.md` - New notification codes
- `src/main/java/com/autotune/analyzer/recommendations/RecommendationConstants.java` - Notification constants
- `src/main/java/com/autotune/analyzer/recommendations/engine/RecommendationEngine.java` - Notification logic

5. **Commit and push:**
```bash
git add .
git commit -S -m "Add provisioning status notifications (over/under/optimized)

- Add OPTIMISED notification codes for CPU and Memory requests/limits
- Add UNDER_PROVISIONED notification codes  
- Add OVER_PROVISIONED notification codes
- Implement logic to compare recommended vs current resource values
- Add notifications based on threshold comparison
- Update notification codes documentation"

git push aakriti feature/provisioning-notifications:provisioning-notifications
```

6. **Create PR:**
```
Title: Add provisioning status notifications (over/under/optimized)
Base: kruize:runtimes-iirj
Head: gulati-aakriti:provisioning-notifications
```

---

## Summary

| PR | Branch | Status | Files Changed |
|----|--------|--------|---------------|
| 1. cluster_name | cluster-name-support | ✅ Ready | ~16 files |
| 2. model/term settings | bulk-model-term-settings | 🔄 TODO | ~8 files |
| 3. notifications | provisioning-notifications | 🔄 TODO | ~3 files |

## Testing Commands

For each branch before creating PR:
```bash
mvn clean test
mvn clean install
mvn test -Dtest=BulkServiceValidationTest
```

## Notes

1. **Order:** Create PRs in order (1 → 2 → 3)
2. **Dependencies:** Each PR is independent and can merge separately
3. **Testing:** Test each branch thoroughly before creating PR
4. **Commits:** All commits are GPG signed (-S flag)
5. **Remote:** Using `aakriti` remote (your fork: gulati-aakriti/autotune)