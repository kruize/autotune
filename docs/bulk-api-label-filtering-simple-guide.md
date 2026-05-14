# Bulk API Label Filtering - Simple Implementation Guide

## The Simple Answer

**Question**: What should I fetch and where should I filter?

**Answer**: 
1. **Always start with PODS** - Labels are on pods
2. **Filter pods by labels** - One Kubernetes API call
3. **Extract hierarchy from filtered pods** - Namespace, Workload, Container come from pods
4. **No additional filtering needed** - Everything is already filtered!

---

## Understanding Label Locations

### Where Labels Actually Exist

```yaml
# Pod with labels
apiVersion: v1
kind: Pod
metadata:
  name: myapp-pod-123
  namespace: production              # ← Namespace (no labels here)
  labels:
    app: myapp                        # ← POD LABEL (filter here!)
    version: v1.0                     # ← POD LABEL (filter here!)
    environment: production           # ← POD LABEL (filter here!)
  ownerReferences:
  - kind: ReplicaSet
    name: myapp-rs-abc                # ← Workload info (derived)
spec:
  containers:
  - name: app-container               # ← Container (derived)
    image: myapp:v1.0
```

**Key Point**: Labels are on the **POD**, not on namespace or workload!

---

## What to Fetch for Each Label Type

### Case 1: Pod Labels (RECOMMENDED - 95% of use cases)

#### What User Provides
```json
{
  "filter": {
    "include": {
      "labels": {
        "app": "myapp",
        "version": "v1.0"
      }
    }
  }
}
```

#### What to Fetch
```java
// STEP 1: Fetch pods with labels (ONE API CALL)
String labelSelector = "app=myapp,version=v1.0";
List<Pod> filteredPods = kubernetesServices.getPodsByLabelSelector(null, labelSelector);
// Returns: 10 pods (out of 1000 total)

// STEP 2: Extract hierarchy from these 10 pods
for (Pod pod : filteredPods) {
    String namespace = pod.getMetadata().getNamespace();           // ← From pod
    String workload = extractWorkloadName(pod);                    // ← From pod.ownerReferences
    String workloadType = extractWorkloadType(pod);                // ← From pod.ownerReferences
    List<Container> containers = pod.getSpec().getContainers();    // ← From pod.spec
}

// STEP 3: Build DataSourceMetadataInfo
// This creates the hierarchy: DataSource → Cluster → Namespace → Workload → Container
DataSourceMetadataInfo metadata = buildMetadataFromFilteredPods(filteredPods, datasource);

// STEP 4: Use existing experiment creation loop (NO CHANGES!)
Map<String, CreateExperimentAPIObject> experiments = getExperimentMap(
    labelString, 
    jobData, 
    metadata,  // ← Already filtered to 10 pods!
    datasource
);
```

#### Where Filtering Happens
- ✅ **Kubernetes API** filters pods by labels
- ❌ **NOT in your code** - Kubernetes does it for you!

#### Result
- 10 experiments created (for 10 filtered pods)
- Instead of 1000 experiments (for all pods)

---

### Case 2: Deployment Labels

#### What User Provides
```json
{
  "filter": {
    "include": {
      "deployment_labels": {
        "app.kubernetes.io/name": "myapp"
      }
    }
  }
}
```

#### What to Fetch
```java
// STEP 1: Fetch deployments with labels
String labelSelector = "app.kubernetes.io/name=myapp";
List<Deployment> deployments = kubernetesServices.getDeploymentsByLabelSelector(
    null,  // all namespaces
    labelSelector
);
// Returns: 5 deployments

// STEP 2: For each deployment, get its pods
List<Pod> allFilteredPods = new ArrayList<>();
for (Deployment deployment : deployments) {
    String namespace = deployment.getMetadata().getNamespace();
    String deploymentName = deployment.getMetadata().getName();
    
    // Get pods owned by this deployment
    // Method 1: Use deployment selector labels
    Map<String, String> selector = deployment.getSpec().getSelector().getMatchLabels();
    for (Map.Entry<String, String> entry : selector.entrySet()) {
        List<Pod> pods = kubernetesServices.getPodsBy(
            namespace, 
            entry.getKey(), 
            entry.getValue()
        );
        allFilteredPods.addAll(pods);
    }
}
// Returns: 50 pods (from 5 deployments)

// STEP 3: Build hierarchy from these 50 pods
DataSourceMetadataInfo metadata = buildMetadataFromFilteredPods(allFilteredPods, datasource);

// STEP 4: Use existing experiment creation loop
Map<String, CreateExperimentAPIObject> experiments = getExperimentMap(...);
```

#### Where Filtering Happens
- ✅ **Kubernetes API** filters deployments by labels
- ✅ **Kubernetes API** filters pods by deployment selector
- ❌ **NOT in your code**

---

### Case 3: Namespace Labels

#### What User Provides
```json
{
  "filter": {
    "include": {
      "namespace_labels": {
        "environment": "production",
        "team": "backend"
      }
    }
  }
}
```

#### What to Fetch
```java
// STEP 1: Fetch namespaces with labels
String labelSelector = "environment=production,team=backend";
List<Namespace> namespaces = kubernetesServices.getNamespacesByLabelSelector(labelSelector);
// Returns: 3 namespaces

// STEP 2: Get ALL pods in these namespaces
List<Pod> allFilteredPods = new ArrayList<>();
for (Namespace namespace : namespaces) {
    String namespaceName = namespace.getMetadata().getName();
    List<Pod> pods = kubernetesServices.getPodsBy(namespaceName);
    allFilteredPods.addAll(pods);
}
// Returns: 200 pods (from 3 namespaces)

// STEP 3: Build hierarchy from these 200 pods
DataSourceMetadataInfo metadata = buildMetadataFromFilteredPods(allFilteredPods, datasource);

// STEP 4: Use existing experiment creation loop
Map<String, CreateExperimentAPIObject> experiments = getExperimentMap(...);
```

#### Where Filtering Happens
- ✅ **Kubernetes API** filters namespaces by labels
- ✅ **Kubernetes API** gets all pods in namespace
- ❌ **NOT in your code**

---

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ User Request                                                │
│ {                                                           │
│   "filter": {                                               │
│     "include": {                                            │
│       "labels": {"app": "myapp", "version": "v1.0"}        │
│     }                                                       │
│   }                                                         │
│ }                                                           │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ BulkJobManager.java                                         │
│                                                             │
│ String labelSelector = "app=myapp,version=v1.0";          │
│                                                             │
│ // ONE API CALL - Kubernetes does the filtering!           │
│ List<Pod> pods = kubernetesServices                        │
│     .getPodsByLabelSelector(null, labelSelector);          │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ Kubernetes API                                              │
│                                                             │
│ Searches ALL pods (1000 pods)                              │
│ Filters by labels: app=myapp AND version=v1.0             │
│ Returns: 10 matching pods                                  │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ buildMetadataFromFilteredPods()                            │
│                                                             │
│ For each of 10 pods:                                       │
│   namespace = pod.metadata.namespace                        │
│   workload = pod.metadata.ownerReferences[0].name          │
│   workloadType = pod.metadata.ownerReferences[0].kind      │
│   containers = pod.spec.containers                          │
│                                                             │
│ Builds:                                                     │
│ DataSource                                                  │
│   └─ Cluster                                                │
│       └─ Namespace: production (1 namespace)                │
│           └─ Workload: myapp-deployment (1 workload)        │
│               ├─ Container: app (1 container)               │
│               └─ Container: sidecar (1 container)           │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ getExperimentMap() - EXISTING CODE, NO CHANGES!            │
│                                                             │
│ for (DataSource ds : dataSourceCollection) {               │
│   for (Cluster cluster : ds.getClusters()) {               │
│     for (Namespace ns : cluster.getNamespaces()) {         │
│       for (Workload w : ns.getWorkloads()) {               │
│         for (Container c : w.getContainers()) {            │
│           createExperiment(c);                              │
│         }                                                   │
│       }                                                     │
│     }                                                       │
│   }                                                         │
│ }                                                           │
│                                                             │
│ Result: 2 experiments (for 2 containers in filtered pods)  │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary Table: What to Fetch

| Label Type | What to Fetch | API Call | Returns | Then Extract |
|------------|---------------|----------|---------|--------------|
| **Pod Labels** | Pods with labels | `getPodsByLabelSelector(null, "app=myapp")` | 10 pods | Namespace, Workload, Containers from pods |
| **Deployment Labels** | Deployments, then pods | `getDeploymentsByLabelSelector(null, "app=myapp")` → `getPodsBy(ns, key, val)` | 5 deployments → 50 pods | Namespace, Workload, Containers from pods |
| **Namespace Labels** | Namespaces, then pods | `getNamespacesByLabelSelector("env=prod")` → `getPodsBy(ns)` | 3 namespaces → 200 pods | Namespace, Workload, Containers from pods |

**Pattern**: Always end up with **filtered pods**, then extract hierarchy from pods.

---

## Where Does Filtering Happen?

### ❌ NOT in Your Code

You do **NOT** write code like this:
```java
// WRONG - Don't do this!
for (Pod pod : allPods) {
    if (pod.getMetadata().getLabels().get("app").equals("myapp")) {
        // filter in code
    }
}
```

### ✅ In Kubernetes API

Kubernetes does the filtering:
```java
// CORRECT - Kubernetes filters for you!
List<Pod> filteredPods = kubernetesServices.getPodsByLabelSelector(
    null,  // all namespaces
    "app=myapp,version=v1.0"  // Kubernetes filters by this
);
// Returns only matching pods - already filtered!
```

---

## Implementation Steps

### Step 1: Add New Method to KubernetesServicesImpl

```java
/**
 * Get pods with multiple labels
 * Add this to: src/main/java/com/autotune/common/target/kubernetes/service/impl/KubernetesServicesImpl.java
 */
public List<Pod> getPodsByLabelSelector(String namespace, String labelSelector) {
    List<Pod> podList = null;
    try {
        if (namespace != null) {
            podList = kubernetesClient
                    .pods()
                    .inNamespace(namespace)
                    .withLabelSelector(labelSelector)  // ← Kubernetes filters here!
                    .list()
                    .getItems();
        } else {
            podList = kubernetesClient
                    .pods()
                    .inAnyNamespace()
                    .withLabelSelector(labelSelector)  // ← Kubernetes filters here!
                    .list()
                    .getItems();
        }
        LOGGER.info("Found {} pods matching label selector: {}", 
                    podList != null ? podList.size() : 0, labelSelector);
    } catch (Exception e) {
        LOGGER.error("getPodsByLabelSelector failed: {}", e.getMessage());
        new TargetHandlerException(e, "getPodsByLabelSelector failed!");
    }
    return podList;
}
```

### Step 2: Use in BulkJobManager

```java
// In BulkJobManager.java

private DataSourceMetadataInfo fetchMetadataWithLabelFilter(
    Map<String, String> labelFilter,
    DataSourceInfo datasource
) throws Exception {
    
    if (labelFilter != null && !labelFilter.isEmpty()) {
        // Build label selector from map
        String labelSelector = labelFilter.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(","));
        
        LOGGER.info("[BULK API] Filtering by labels: {}", labelSelector);
        
        // STEP 1: Get filtered pods from Kubernetes
        KubernetesServicesImpl kubeServices = new KubernetesServicesImpl();
        List<Pod> filteredPods = kubeServices.getPodsByLabelSelector(null, labelSelector);
        
        LOGGER.info("[BULK API] Found {} pods matching labels", filteredPods.size());
        
        // STEP 2: Build hierarchy from filtered pods
        DataSourceMetadataInfo metadata = buildMetadataFromFilteredPods(
            filteredPods, 
            datasource
        );
        
        return metadata;
        
    } else {
        // No label filter - use existing Prometheus approach
        return DataSourceMetadataOperator.getInstance().createDataSourceMetadata(...);
    }
}
```

### Step 3: Build Hierarchy from Filtered Pods

```java
private DataSourceMetadataInfo buildMetadataFromFilteredPods(
    List<Pod> filteredPods,
    DataSourceInfo datasource
) {
    Map<String, DataSourceNamespace> namespaces = new HashMap<>();
    
    // Process each filtered pod
    for (Pod pod : filteredPods) {
        String namespaceName = pod.getMetadata().getNamespace();
        
        // Get or create namespace
        DataSourceNamespace namespace = namespaces.computeIfAbsent(
            namespaceName,
            k -> new DataSourceNamespace(namespaceName)
        );
        
        // Extract workload from pod's ownerReferences
        WorkloadInfo workload = extractWorkloadFromPod(pod);
        if (workload != null) {
            // Get or create workload
            DataSourceWorkload dsWorkload = namespace.getWorkloads()
                .computeIfAbsent(
                    workload.getName(),
                    k -> new DataSourceWorkload(
                        workload.getName(),
                        workload.getType(),
                        namespaceName
                    )
                );
            
            // Extract containers from pod
            for (Container container : pod.getSpec().getContainers()) {
                DataSourceContainer dsContainer = new DataSourceContainer(
                    container.getName(),
                    container.getImage(),
                    namespaceName,
                    workload.getName()
                );
                dsWorkload.getContainers().put(container.getName(), dsContainer);
            }
        }
    }
    
    // Build final structure
    DataSourceCluster cluster = new DataSourceCluster("default-cluster");
    cluster.setNamespaces(namespaces);
    
    DataSource ds = new DataSource(datasource.getName());
    ds.setClusters(Map.of("default-cluster", cluster));
    
    return new DataSourceMetadataInfo(Map.of(datasource.getName(), ds));
}
```

---

## Key Takeaways

### 1. Always Start with Pods
- Labels are on pods
- Hierarchy is derived from pods
- Filter pods → Get everything

### 2. Kubernetes Does the Filtering
- Use `withLabelSelector()` in Kubernetes API
- Don't filter in your code
- Kubernetes is fast and indexed

### 3. One Pattern for All Label Types
```
Fetch filtered pods → Extract hierarchy → Use existing loop
```

### 4. No Changes to Experiment Creation
- The existing loop in `getExperimentMap()` works as-is
- It just processes fewer items (filtered pods only)

### 5. Where Filtering Happens
- ✅ Kubernetes API (fast, indexed)
- ❌ NOT in your code (slow, inefficient)

---

## Example: Complete Flow

```java
// User request
{
  "filter": {
    "include": {
      "labels": {"app": "myapp", "version": "v1.0"}
    }
  }
}

// Your code
String labelSelector = "app=myapp,version=v1.0";
List<Pod> pods = kubeServices.getPodsByLabelSelector(null, labelSelector);
// Kubernetes returns: 10 pods (filtered)

DataSourceMetadataInfo metadata = buildMetadataFromFilteredPods(pods, datasource);
// You extract: 1 namespace, 1 workload, 2 containers

Map<String, CreateExperimentAPIObject> experiments = getExperimentMap(...);
// Existing loop creates: 2 experiments (for 2 containers)

// Result: 2 experiments instead of 1000!
```

That's it! Simple and efficient.

What's Already Present in KubernetesServicesImpl:
✅ Existing Method (Line 164):

public List<Pod> getPodsBy(String namespace, String labelKey, String labelValue)

Limitation: Only supports ONE label at a time
Example: getPodsBy(null, "app", "myapp") ✅
Cannot do: app=myapp AND version=v1.0 ❌
What You NEED to Add:
❌ Missing Method (REQUIRED):

public List<Pod> getPodsByLabelSelector(String namespace, String labelSelector)

Supports: MULTIPLE labels at once
Example: getPodsByLabelSelector(null, "app=myapp,version=v1.0") ✅
Can filter by: app=myapp AND version=v1.0 AND environment=production ✅
Why You Need the New Method:
Existing method uses .withLabel(key, value) - single label only
New method uses .withLabelSelector(selector) - multiple labels

Comparison:
Method	Supports	Example
Existing: getPodsBy(ns, key, value)	1 label	getPodsBy(null, "app", "myapp")
NEW: getPodsByLabelSelector(ns, selector)	Multiple labels	getPodsByLabelSelector(null, "app=myapp,version=v1.0")
Exact Code to Add:
Add this method to KubernetesServicesImpl.java (after line 186):

/**
 * Get pods with label selector (supports multiple labels)
 * Example: "app=myapp,version=v1.0,environment=production"
 * 
 * @param namespace - namespace to search (null for all namespaces)
 * @param labelSelector - comma-separated label selector
 * @return List of pods matching the label selector
 */
public List<Pod> getPodsByLabelSelector(String namespace, String labelSelector) {
    List<Pod> podList = null;
    try {
        if (namespace != null) {
            podList = kubernetesClient
                    .pods()
                    .inNamespace(namespace)
                    .withLabelSelector(labelSelector)  // ← KEY DIFFERENCE!
                    .list()
                    .getItems();
        } else {
            podList = kubernetesClient
                    .pods()
                    .inAnyNamespace()
                    .withLabelSelector(labelSelector)  // ← KEY DIFFERENCE!
                    .list()
                    .getItems();
        }
        LOGGER.info("Found {} pods matching label selector: {}", 
                    podList != null ? podList.size() : 0, labelSelector);
    } catch (Exception e) {
        LOGGER.error("getPodsByLabelSelector failed: {}", e.getMessage());
        new TargetHandlerException(e, "getPodsByLabelSelector failed!");
    }
    return podList;
}


Summary:
❌ Existing methods are NOT sufficient (only single label)
✅ You MUST add getPodsByLabelSelector() method
📍 Add it after line 186 in KubernetesServicesImpl.java
🎯 This is the ONLY new method you need for pod label filtering