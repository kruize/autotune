# Kruize API Quick Reference

This is a quick reference for commonly used Kruize APIs. For complete details, see `/design/KruizeLocalAPI.md` and `/design/MonitoringModeAPI.md`.

## Base URL

```bash
export KRUIZE_URL="http://localhost:8080"
```

---

## Health Check

```bash
# Check if Kruize is running
curl ${KRUIZE_URL}/health
```

**Response:**
```json
{
  "status": "UP",
  "database": "Connected"
}
```

---

## Datasource APIs

### List Datasources

```bash
curl "${KRUIZE_URL}/listDatasources"
```

**Response:**
```json
[
  {
    "version": "v1.0",
    "datasource_name": "prometheus-1"
  }
]
```

---

## Metadata Profile APIs

### Create Metadata Profile

```bash
curl -X POST ${KRUIZE_URL}/createMetadataProfile \
  -H "Content-Type: application/json" \
  -d @manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json
```

**Request Body Example:**
```json
{
  "apiVersion": "v1.0",
  "kind": "MetadataProfile",
  "metadata": {
    "name": "resource-optimization-local-monitoring"
  },
  "datasource": "prometheus",
  "queries": [
    {
      "name": "namespace_query",
      "query": "kube_namespace_labels",
      "key": "namespace"
    }
  ]
}
```

### List Metadata Profiles

```bash
curl "${KRUIZE_URL}/listMetadataProfiles"

# With specific name
curl "${KRUIZE_URL}/listMetadataProfiles?name=resource-optimization-local-monitoring"
```

---

## Metadata APIs

### Import Metadata

```bash
curl -X POST ${KRUIZE_URL}/importMetadata \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1.0",
    "datasource_name": "prometheus-1"
  }'
```

### List Metadata

```bash
# Basic list
curl "${KRUIZE_URL}/listMetadata"

# Verbose (with details)
curl "${KRUIZE_URL}/listMetadata?verbose=true"

# Filter by datasource
curl "${KRUIZE_URL}/listMetadata?datasource=prometheus-1"

# Filter by cluster
curl "${KRUIZE_URL}/listMetadata?datasource=prometheus-1&cluster_name=my-cluster"

# Filter by namespace
curl "${KRUIZE_URL}/listMetadata?datasource=prometheus-1&cluster_name=my-cluster&namespace=default"
```

---

## Metric Profile APIs

### Create Metric Profile

```bash
curl -X POST ${KRUIZE_URL}/createMetricProfile \
  -H "Content-Type: application/json" \
  -d @manifests/autotune/performance-profiles/resource_optimization_local_monitoring.json
```

**Request Body Example:**
```json
{
  "apiVersion": "v1.0",
  "kind": "MetricProfile",
  "metadata": {
    "name": "resource-optimization-local-monitoring"
  },
  "profile_version": 1.0,
  "k8s_type": "openshift",
  "slo": {
    "slo_class": "resource_usage",
    "direction": "minimize",
    "objective_function": "transaction_response_time"
  }
}
```

### List Metric Profiles

```bash
curl "${KRUIZE_URL}/listMetricProfiles"

# With specific name
curl "${KRUIZE_URL}/listMetricProfiles?name=resource-optimization-local-monitoring"
```

---

## Performance Profile APIs

### Create Performance Profile

```bash
curl -X POST ${KRUIZE_URL}/createPerformanceProfile \
  -H "Content-Type: application/json" \
  -d '{
    "apiVersion": "v3.0",
    "kind": "PerformanceProfile",
    "metadata": {
      "name": "my-performance-profile"
    },
    "profile_version": 1.0,
    "slo": {
      "slo_class": "response_time",
      "objective_function": "minimize",
      "function_variables": {
        "response_time": {
          "value_type": "double",
          "value": 100.0
        }
      }
    }
  }'
```

### Update Performance Profile

```bash
curl -X PUT "${KRUIZE_URL}/updatePerformanceProfile?name=my-performance-profile" \
  -H "Content-Type: application/json" \
  -d '{
    "profile_version": 1.1,
    "slo": {
      ...
    }
  }'
```

### List Performance Profiles

```bash
curl "${KRUIZE_URL}/listPerformanceProfiles"

# With specific name
curl "${KRUIZE_URL}/listPerformanceProfiles?name=my-performance-profile"
```

### Delete Performance Profile

```bash
curl -X DELETE "${KRUIZE_URL}/deletePerformanceProfile?name=my-performance-profile"
```

---

## Layer APIs

### Create Layer

```bash
curl -X POST ${KRUIZE_URL}/createLayer \
  -H "Content-Type: application/json" \
  -d '{
    "apiVersion": "recommender.com/v1",
    "kind": "KruizeLayer",
    "metadata": {
      "name": "quarkus-layer"
    },
    "layer_name": "quarkus",
    "details": "Quarkus runtime tunables",
    "layer_presence": {
      "label": [
        {
          "name": "runtime",
          "value": "quarkus"
        }
      ]
    },
    "tunables": [
      {
        "name": "quarkus.thread-pool.core-threads",
        "value_type": "integer",
        "lower_bound": "1",
        "upper_bound": "10",
        "step": 1
      }
    ]
  }'
```

**Layer Presence Types:**

1. **Always present:**
```json
"layer_presence": {
  "presence": "always"
}
```

2. **Query-based detection:**
```json
"layer_presence": {
  "queries": [
    {
      "datasource": "prometheus",
      "query": "jvm_memory_used_bytes{area='heap'}",
      "key": "pod"
    }
  ]
}
```

3. **Label-based detection:**
```json
"layer_presence": {
  "label": [
    {
      "name": "runtime",
      "value": "quarkus"
    }
  ]
}
```

### List Layers

```bash
# List all layers
curl "${KRUIZE_URL}/listLayers"

# List specific layer
curl "${KRUIZE_URL}/listLayers?layer_name=quarkus"

# Verbose mode (with details)
curl "${KRUIZE_URL}/listLayers?verbose=true"
```

### Delete Layer

```bash
curl -X DELETE "${KRUIZE_URL}/deleteLayer?layer_name=quarkus"
```

---

## Experiment APIs (Remote Monitoring)

### Create Experiment

```bash
curl -X POST ${KRUIZE_URL}/createExperiment \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v2.0",
    "experiment_name": "my-experiment",
    "cluster_name": "my-cluster",
    "performance_profile": "resource-optimization-local-monitoring",
    "mode": "monitor",
    "target_cluster": "remote",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "my-app",
        "namespace": "default",
        "containers": [
          {
            "container_name": "app-container",
            "container_image_name": "myapp:v1"
          }
        ]
      }
    ],
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "datasource": "prometheus-1"
  }'
```

### List Experiments

```bash
# List all experiments
curl "${KRUIZE_URL}/listExperiments"

# List specific experiment
curl "${KRUIZE_URL}/listExperiments?experiment_name=my-experiment"

# Results only
curl "${KRUIZE_URL}/listExperiments?results=true"

# Recommendations only
curl "${KRUIZE_URL}/listExperiments?recommendations=true"
```

### Update Results

```bash
curl -X POST ${KRUIZE_URL}/updateResults \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v2.0",
    "experiment_name": "my-experiment",
    "start_timestamp": "2024-01-01T00:00:00.000Z",
    "end_timestamp": "2024-01-01T01:00:00.000Z",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "my-app",
        "namespace": "default",
        "containers": [
          {
            "container_name": "app-container",
            "metrics": {
              "cpuUsage": {
                "min": 0.1,
                "max": 0.5,
                "sum": 10.0,
                "avg": 0.3,
                "format": "cores"
              },
              "memoryUsage": {
                "min": 100,
                "max": 500,
                "sum": 10000,
                "avg": 300,
                "format": "MiB"
              }
            }
          }
        ]
      }
    ]
  }'
```

### List Recommendations

```bash
# Basic recommendations
curl "${KRUIZE_URL}/listRecommendations?experiment_name=my-experiment"

# Latest only
curl "${KRUIZE_URL}/listRecommendations?experiment_name=my-experiment&latest=true"

# Specific monitoring end time
curl "${KRUIZE_URL}/listRecommendations?experiment_name=my-experiment&monitoring_end_time=2024-01-01T23:59:59.000Z"
```

**Response Example:**
```json
{
  "version": "v2.0",
  "experiment_name": "my-experiment",
  "kubernetes_objects": [
    {
      "type": "deployment",
      "name": "my-app",
      "namespace": "default",
      "containers": [
        {
          "container_name": "app-container",
          "recommendations": {
            "version": "v2.0",
            "notifications": {
              "111000": {
                "type": "info",
                "message": "Recommendations Are Available"
              }
            },
            "data": {
              "2024-01-01T23:59:59.000Z": {
                "short_term": {
                  "duration_based": {
                    "short_term": {
                      "requests": {
                        "cpu": {
                          "amount": 0.5,
                          "format": "cores"
                        },
                        "memory": {
                          "amount": 512,
                          "format": "MiB"
                        }
                      },
                      "limits": {
                        "cpu": {
                          "amount": 1.0,
                          "format": "cores"
                        },
                        "memory": {
                          "amount": 1024,
                          "format": "MiB"
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      ]
    }
  ]
}
```

---

## Common Query Parameters

| API | Parameter | Description |
|-----|-----------|-------------|
| listMetadata | `verbose` | Include full details |
| listMetadata | `datasource` | Filter by datasource name |
| listMetadata | `cluster_name` | Filter by cluster |
| listMetadata | `namespace` | Filter by namespace |
| listLayers | `layer_name` | Get specific layer |
| listLayers | `verbose` | Include full details |
| listRecommendations | `experiment_name` | Required - experiment to fetch |
| listRecommendations | `latest` | Only return latest recommendations |
| listRecommendations | `monitoring_end_time` | Specific timestamp |

---

## HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET request |
| 201 | Created | Successful POST (create) |
| 400 | Bad Request | Validation failed |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource |
| 500 | Internal Server Error | Server error |

---

## Common Error Response Format

```json
{
  "status": "ERROR",
  "message": "Validation failed: layer_name cannot be null or empty",
  "httpcode": 400
}
```

---

## Testing APIs with curl

### Save JSON to file and use it:

```bash
cat > /tmp/layer.json <<EOF
{
  "apiVersion": "recommender.com/v1",
  "kind": "KruizeLayer",
  ...
}
EOF

curl -X POST ${KRUIZE_URL}/createLayer \
  -H "Content-Type: application/json" \
  -d @/tmp/layer.json
```

### Pretty-print JSON responses:

```bash
curl "${KRUIZE_URL}/listLayers" | jq .
```

### Save response to file:

```bash
curl "${KRUIZE_URL}/listRecommendations?experiment_name=my-exp" \
  -o /tmp/recommendations.json
```

---

## Quick Testing Workflow

```bash
# 1. Check health
curl ${KRUIZE_URL}/health

# 2. Create metadata profile
curl -X POST ${KRUIZE_URL}/createMetadataProfile \
  -H "Content-Type: application/json" \
  -d @manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json

# 3. Create metric profile
curl -X POST ${KRUIZE_URL}/createMetricProfile \
  -H "Content-Type: application/json" \
  -d @manifests/autotune/performance-profiles/resource_optimization_local_monitoring.json

# 4. Import metadata
curl -X POST ${KRUIZE_URL}/importMetadata \
  -H "Content-Type: application/json" \
  -d '{"version": "v1.0", "datasource_name": "prometheus-1"}'

# 5. List imported metadata
curl "${KRUIZE_URL}/listMetadata?verbose=true" | jq .

# 6. Create layer
curl -X POST ${KRUIZE_URL}/createLayer \
  -H "Content-Type: application/json" \
  -d @manifests/autotune/layers/container-config.json

# 7. List layers
curl "${KRUIZE_URL}/listLayers" | jq .
```

---

## Resources

- Full API Spec (Local): `/design/KruizeLocalAPI.md`
- Full API Spec (Remote): `/design/MonitoringModeAPI.md`
- API Samples: `/design/APISamples.md`
- Manifests: `/manifests/autotune/`

---

## Pro Tips

1. **Use jq** for pretty JSON: `curl ... | jq .`
2. **Set KRUIZE_URL** in your `.bashrc`
3. **Use `-v` flag** with curl for debugging: `curl -v ...`
4. **Check response headers**: `curl -I ${KRUIZE_URL}/health`
5. **Test in Postman** for easier debugging and collections
6. **Save common requests** as shell scripts or Postman collections

Happy API testing! 🚀
