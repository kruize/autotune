# Kubernetes Label Types Guide

## Overview

This document explains different types of labels in Kubernetes and how they relate to Bulk API filtering.

---

## 1. Pod Labels

### Definition
Labels defined in the **Pod template spec** that are applied to every pod created from that template.

### Location in Manifest
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  template:
    metadata:
      labels:              # ← POD LABELS (defined here)
        app: nginx
        version: v1.0
        environment: production
    spec:
      containers:
      - name: nginx
        image: nginx:1.21
```

### Characteristics
- ✅ Applied to every pod instance
- ✅ Visible in `kubectl get pods --show-labels`
- ✅ Available in Prometheus `kube_pod_labels` metric
- ✅ **Supported by Bulk API** (with proper implementation)

### Example in Prometheus
```promql
kube_pod_labels{
  namespace="default",
  pod="nginx-deployment-abc123",
  label_app="nginx",              # Pod label with "label_" prefix
  label_version="v1.0",
  label_environment="production"
}
```

---

## 2. Deployment Labels

### Definition
Labels defined ONLY on the **Deployment metadata** (not propagated to pods).

### Location in Manifest
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:                  # ← DEPLOYMENT LABELS (defined here)
    team: platform
    cost-center: engineering
    managed-by: helm
spec:
  template:
    metadata:
      labels:              # ← These are POD labels (different!)
        app: nginx
```

### Characteristics
- ❌ NOT applied to pods
- ❌ Only on Deployment object itself
- ❌ NOT in `kube_pod_labels` metric
- ✅ Available in `kube_deployment_labels` metric (separate)
- ❌ **NOT supported by Bulk API** (currently)

### Example in Prometheus
```promql
# Deployment labels (separate metric)
kube_deployment_labels{
  namespace="default",
  deployment="nginx-deployment",
  label_team="platform",           # Deployment label
  label_cost_center="engineering"
}

# Pod labels (different metric, no deployment labels)
kube_pod_labels{
  namespace="default",
  pod="nginx-deployment-abc123",
  label_app="nginx"                # Only pod labels here
  # NO label_team or label_cost_center
}
```

---

## 3. Standard Kubernetes Labels

### Definition
Recommended labels defined by Kubernetes for common use cases.

### Recommended Labels
```yaml
metadata:
  labels:
    # Application identification
    app.kubernetes.io/name: mysql
    app.kubernetes.io/instance: mysql-abcxzy
    app.kubernetes.io/version: "5.7.21"
    app.kubernetes.io/component: database
    app.kubernetes.io/part-of: wordpress
    app.kubernetes.io/managed-by: helm
```

### Characteristics
- ✅ Standardized naming convention
- ✅ Recommended by Kubernetes
- ✅ Better for multi-tool compatibility
- ✅ Can be pod labels or deployment labels (depends on where defined)

### Reference
[Kubernetes Recommended Labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)

---

## 4. Custom Labels (User-Defined)

### Definition
Any labels created by users for their specific needs.

### Examples
```yaml
metadata:
  labels:
    # Environment labels
    environment: production
    env: prod
    tier: frontend
    
    # Team/ownership labels
    team: platform
    owner: john.doe
    project: customer-portal
    
    # Feature flags
    feature-flag: enabled
    canary: "true"
    
    # Compliance/security
    compliance: pci-dss
    data-classification: confidential
    backup: enabled
    
    # Monitoring
    monitoring: prometheus
    logging: enabled
```

### Characteristics
- ✅ Completely flexible
- ✅ Organization-specific
- ✅ Can be pod labels or deployment labels
- ✅ **Supported by Bulk API** if defined as pod labels

---

## 5. Label Comparison Table

| Label Type | Where Defined | Applied To | In kube_pod_labels? | Bulk API Support |
|------------|---------------|------------|---------------------|------------------|
| **Pod Labels** | Pod template spec | Pods | ✅ YES | ✅ YES (with fix) |
| **Deployment Labels** | Deployment metadata | Deployment only | ❌ NO | ❌ NO |
| **Standard K8s Labels** | Either location | Depends on location | Depends | Depends |
| **Custom Labels** | Either location | Depends on location | Depends | Depends |

---

## 6. Complete Example

### Full Deployment with All Label Types

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  namespace: production
  labels:
    # DEPLOYMENT LABELS (not on pods)
    team: platform                           # Custom deployment label
    cost-center: engineering                 # Custom deployment label
    app.kubernetes.io/managed-by: helm       # Standard K8s deployment label
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx                             # Must match pod labels
  template:
    metadata:
      labels:
        # POD LABELS (on every pod)
        # Standard Kubernetes labels
        app: nginx                           # Simple custom pod label
        app.kubernetes.io/name: nginx        # Standard K8s pod label
        app.kubernetes.io/version: "1.21"    # Standard K8s pod label
        app.kubernetes.io/component: webserver # Standard K8s pod label
        
        # Custom pod labels
        version: v1.0                        # Custom pod label
        environment: production              # Custom pod label
        tier: frontend                       # Custom pod label
        monitoring: enabled                  # Custom pod label
    spec:
      containers:
      - name: nginx
        image: nginx:1.21
        ports:
        - containerPort: 80
```

### What's Available Where

**On Deployment Object**:
```bash
kubectl get deployment nginx-deployment --show-labels
# Labels: team=platform,cost-center=engineering,app.kubernetes.io/managed-by=helm
```

**On Pod Objects**:
```bash
kubectl get pods -l app=nginx --show-labels
# Labels: app=nginx,app.kubernetes.io/name=nginx,app.kubernetes.io/version=1.21,
#         app.kubernetes.io/component=webserver,version=v1.0,environment=production,
#         tier=frontend,monitoring=enabled
```

**In Prometheus `kube_deployment_labels`**:
```promql
kube_deployment_labels{
  namespace="production",
  deployment="nginx-deployment",
  label_team="platform",
  label_cost_center="engineering",
  label_app_kubernetes_io_managed_by="helm"
}
```

**In Prometheus `kube_pod_labels`**:
```promql
kube_pod_labels{
  namespace="production",
  pod="nginx-deployment-abc123",
  label_app="nginx",
  label_app_kubernetes_io_name="nginx",
  label_app_kubernetes_io_version="1_21",
  label_app_kubernetes_io_component="webserver",
  label_version="v1_0",
  label_environment="production",
  label_tier="frontend",
  label_monitoring="enabled"
  # NO deployment labels here!
}
```

---

## 7. Bulk API Filtering Support

### Currently Supported (with proper implementation)

**Pod Labels** - Any label in pod template spec:
```json
{
  "filter": {
    "include": {
      "labels": {
        "app": "nginx",                      // ✅ Pod label
        "version": "v1.0",                   // ✅ Pod label
        "environment": "production",         // ✅ Pod label
        "app.kubernetes.io/name": "nginx"    // ✅ Pod label (standard)
      }
    }
  }
}
```

### NOT Supported (currently)

**Deployment Labels** - Labels only on Deployment:
```json
{
  "filter": {
    "include": {
      "labels": {
        "team": "platform",           // ❌ Deployment label (not on pods)
        "cost-center": "engineering"  // ❌ Deployment label (not on pods)
      }
    }
  }
}
```

---

## 8. Best Practices

### For Bulk API Filtering

1. **Always define labels in pod template spec**
   ```yaml
   spec:
     template:
       metadata:
         labels:           # ← Define here for Bulk API filtering
           app: myapp
   ```

2. **Use standard Kubernetes labels as pod labels**
   ```yaml
   labels:
     app.kubernetes.io/name: myapp
     app.kubernetes.io/version: "1.0"
   ```

3. **Keep deployment-specific labels separate**
   ```yaml
   metadata:
     labels:             # ← Deployment-only labels (not for filtering)
       team: platform
   spec:
     template:
       metadata:
         labels:         # ← Pod labels (for filtering)
           app: myapp
   ```

4. **Document which labels are for filtering**
   ```yaml
   # Pod labels (available for Bulk API filtering)
   labels:
     app: myapp              # For filtering by application
     environment: prod       # For filtering by environment
     version: v1.0           # For filtering by version
   ```

---

## 9. Label Naming Conventions

### Standard Kubernetes Format
```yaml
labels:
  # Prefix with domain for organization-specific labels
  company.com/team: platform
  company.com/cost-center: engineering
  
  # Use standard K8s labels without prefix
  app.kubernetes.io/name: myapp
  app.kubernetes.io/version: "1.0"
```

### Simple Format (Common)
```yaml
labels:
  # Short, simple names
  app: myapp
  env: prod
  version: v1.0
  tier: frontend
```

### Avoid
```yaml
labels:
  # Don't use spaces
  "my app": myapp              # ❌ Bad
  
  # Don't use special characters
  app@version: v1.0            # ❌ Bad
  
  # Don't use very long values
  description: "This is a very long description..." # ❌ Bad (use annotations)
```

---

## 10. Quick Reference

### Label Type Decision Tree

```
Is the label needed for Bulk API filtering?
├─ YES → Define in pod template spec (pod label)
│        ✅ Will be in kube_pod_labels
│        ✅ Can be used for filtering
│
└─ NO → Define in Deployment metadata (deployment label)
         ❌ Not in kube_pod_labels
         ❌ Cannot be used for filtering
         ✅ Good for organizational metadata
```

### Common Label Categories

| Category | Examples | Typical Location | Bulk API Filtering |
|----------|----------|------------------|-------------------|
| **Application** | `app`, `app.kubernetes.io/name` | Pod labels | ✅ YES |
| **Version** | `version`, `app.kubernetes.io/version` | Pod labels | ✅ YES |
| **Environment** | `environment`, `env` | Pod labels | ✅ YES |
| **Tier** | `tier`, `app.kubernetes.io/component` | Pod labels | ✅ YES |
| **Team/Owner** | `team`, `owner` | Deployment labels | ❌ NO |
| **Cost** | `cost-center`, `billing-code` | Deployment labels | ❌ NO |
| **Management** | `managed-by`, `app.kubernetes.io/managed-by` | Deployment labels | ❌ NO |

---

## Summary

**Key Points**:
1. **Pod labels** = Labels in pod template spec → Available for filtering
2. **Deployment labels** = Labels only on Deployment → NOT available for filtering
3. **Standard K8s labels** = Can be either, depends on where defined
4. **Custom labels** = Can be either, depends on where defined
5. **For Bulk API filtering** = Must be pod labels (in pod template spec)

**Remember**: If you want to filter by a label in Bulk API, define it in the **pod template spec**, not just on the Deployment metadata.

---

**Document Version**: 1.0  
**Date**: 2026-04-23