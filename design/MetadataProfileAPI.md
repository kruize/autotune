# Metadata Profile

This article describes how to add and retrieve Metadata Profiles with REST APIs using curl command. 
Documentation still in progress stay tuned.

## Supported and Unsupported Workload Types

Kruize supports various Kubernetes workload types for monitoring and generating recommendations. However, certain workload types are not supported and are automatically filtered out during metadata import and query execution.

### Supported Workload Types

The following Kubernetes workload types are **supported** by Kruize:

- **Deployment**
- **StatefulSet**
- **ReplicaSet**
- **ReplicationController**
- **DaemonSet**
- **Job**

### Unsupported Workload Types

The following workload types are **not supported** by Kruize:

- **DeploymentConfig**: Excluded because it is an OpenShift-specific deployment configuration that has been deprecated from [Red Hat OpenShift v4.14](https://access.redhat.com/articles/7041372) onwards. OpenShift users are encouraged to migrate to standard Kubernetes Deployments.

- **ConfigMap and CatalogSource**: Excluded because they are configuration and catalog resources, rather than actual executing containers that can be monitored to generate recommendations.
---

## CreateMetadataProfile

This is quick guide instructions to create metadata profile using input JSON as follows. For a more detailed guide,
and the list of metadata queries supported by Kruize refer [Metadata Profile](/design/MetadataProfile.md)

#### Note: Queries to fetch namespace, workload and container metadata are required to import cluster metadata

**Request**
`POST /createMetadataProfile`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createMetadataProfile`

```
{
  "apiVersion": "recommender.com/v1",
  "kind": "KruizeMetadataProfile",
  "metadata": {
    "name": "cluster-metadata-local-monitoring"
  },
  "profile_version": 1,
  "k8s_type": "openshift",
  "datasource": "prometheus",
  "query_variables": [
    {
      "name": "namespacesAcrossCluster",
      "datasource": "prometheus",
      "value_type": "double",
      "kubernetes_object": "container",
      "aggregation_functions": [
        {
          "function": "sum",
          "query": "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
        }
      ]
    },
    {
      "name": "workloadsAcrossCluster",
      "datasource": "prometheus",
      "value_type": "double",
      "kubernetes_object": "container",
      "aggregation_functions": [
        {
          "function": "sum",
          "query": "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
        }
      ]
    },
    {
      "name": "containersAcrossCluster",
      "datasource": "prometheus",
      "value_type": "double",
      "kubernetes_object": "container",
      "aggregation_functions": [
        {
          "function": "sum",
          "query": "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
        }
      ]
    }
  ]
}
```

**Response**

```
{
    "message": "Metadata Profile : cluster-metadata-local-monitoring created successfully. View all metadata profiles at /listMetadataProfiles",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

## List Metadata Profiles

This is quick guide instructions to retrieve metadata profiles created as follows.

**Request Parameters**

| Parameter | Type   | Required | Description                               |
|-----------|--------|----------|-------------------------------------------|
| name      | string | optional | The name of the metadata profile          |
| verbose   | string | optional | Flag to retrieve all the metadata queries |

**Request without passing parameters**

`GET /listMetadataProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetadataProfiles`

Returns list of all the metadata profile names created

<details>
<summary><b>Response</b></summary>

### Example Response

```json
[
  {
    "name": "cluster-metadata-local-monitoring"
  },
  {
    "name": "bulk-cluster-metadata-local-monitoring"
  }
]
```
</details>

<br>

**Request with metadata profile name**

`GET /listMetadataProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetadataProfiles?name=cluster-metadata-local-monitoring`

Returns metadata profile of the name specified


<details>
<summary><b>Response</b></summary>

### Example Response

```json
[
  {
    "apiVersion": "recommender.com/v1",
    "kind": "KruizeMetadataProfile",
    "metadata": {
      "name": "cluster-metadata-local-monitoring"
    },
    "profile_version": 1,
    "k8s_type": "openshift",
    "datasource": "prometheus",
    "query_variables": [
      {
        "name": "namespacesAcrossCluster",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "workloadsAcrossCluster",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "containersAcrossCluster",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      }
    ]
  }
]
```
</details>

<br>

**Request**

`GET /listMetadataProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetadataProfiles?verbose=true`

Returns list of all the metadata profile created with all the metadata queries

<details>
<summary><b>Response</b></summary>

### Example Response

```json
[
  {
    "apiVersion": "recommender.com/v1",
    "kind": "KruizeMetadataProfile",
    "metadata": {
      "name": "cluster-metadata-local-monitoring"
    },
    "profile_version": 1,
    "k8s_type": "openshift",
    "datasource": "prometheus",
    "query_variables": [
      {
        "name": "namespacesAcrossCluster",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "workloadsAcrossCluster",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "containersAcrossCluster",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      }
    ]
  },
  {
    "apiVersion": "recommender.com/v1",
    "kind": "KruizeMetadataProfile",
    "metadata": {
      "name": "bulk-cluster-metadata-local-monitoring"
    },
    "profile_version": 1,
    "k8s_type": "openshift",
    "datasource": "prometheus",
    "query_variables": [
      {
        "name": "namespacesForAdditionalLabel",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace) (avg_over_time(kube_namespace_status_phase{$NAMESPACE_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "workloadsForAdditionalLabel",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "workloadsWithPodLabelFilter",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$ $LABELS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "containersForAdditionalLabel",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      }
    ]
  }
]
```
</details>

## Delete Metadata Profile API

This is quick guide instructions to delete metadata profile created as follows.

**Request Parameters**

| Parameter | Type   | Required | Description                      |
|-----------|--------|----------|----------------------------------|
| name      | string | required | The name of the metadata profile |


**Request with name query parameter**

`DELETE /deleteMetadataProfile`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/deleteMetadataProfile?name=cluster-metadata-local-monitoring`

Deletes the specified metadata profile name, provided metadata profile already is created

<details>
<summary><b>Response</b></summary>

### Example Response

```json
{
  "message": "Metadata profile: cluster-metadata-local-monitoring deleted successfully. View Metadata Profiles at /listMetadataProfiles",
  "httpcode": 201,
  "documentationLink": "",
  "status": "SUCCESS"
}
```

</details>

<br>

## Query Placeholders

Kruize metadata profile queries support a set of `$...$` placeholder tokens that are substituted at runtime. The applicable set differs depending on whether the query is used in a **bulk metadata profile** (created via `createMetadataProfile` and used by the data-gathering pipeline) or an **update payload** (sent via `updateMetadataProfile`).

### Supported Placeholders

The following table lists every placeholder Kruize recognises. Adding any other `$...$` token to a query will cause a validation error.

| Placeholder                     | Where valid                           | Applied to query templates                                                                    | Substituted by                                                                                      | Description                                                                                                                           |
|---------------------------------|---------------------------------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `$NAMESPACE_FILTERS$`           | Bulk profiles only                    | `namespacesForAdditionalLabel`, `workloadsWithPodLabelFilter`, `containersForAdditionalLabel` | `DataSourceMetadataOperator` at import time                                                         | Expands to a PromQL label selector that filters by the requested namespaces (include/exclude lists from the import request).          |
| `$WORKLOAD_FILTERS$`            | Bulk profiles only                    | `workloadsForAdditionalLabel`, `workloadsWithPodLabelFilter`, `containersForAdditionalLabel`  | `DataSourceMetadataOperator` at import time                                                         | Expands to a PromQL label selector that filters by the requested workloads.                                                           |
| `$CONTAINER_FILTERS$`           | Bulk profiles only                    | `workloadsWithPodLabelFilter`, `containersForAdditionalLabel`                                 | `DataSourceMetadataOperator` at import time                                                         | Expands to a PromQL label selector that filters by the requested containers.                                                          |
| `$LABELS$`                      | Bulk profiles only                    | `workloadsWithPodLabelFilter`                                                                 | `DataSourceMetadataOperator` at import time                                                         | Expands to a PromQL label selector derived from the pod label filter in the import request.                                           |
| `$MEASUREMENT_DURATION_IN_MIN$` | Bulk profiles **and** update payloads | All query templates                                                                           | `DataSourceMetadataOperator` (bulk) / `queryValidator` (update, for validation only — stored as-is) | Replaced with the measurement duration in minutes (e.g. `15`). Used inside range-vector brackets: `[$MEASUREMENT_DURATION_IN_MIN$m]`. |
| `$UNSUPPORTED_WORKLOAD_TYPES$`  | Bulk profiles **and** update payloads | `workloadsForAdditionalLabel`, `workloadsWithPodLabelFilter`, `containersForAdditionalLabel`  | `DataSourceMetadataOperator` (bulk) / `queryValidator` (update, for validation only — stored as-is) | Expands to a `workload_type!~"..."` filter that excludes unsupported workload types (DeploymentConfig, ConfigMap, CatalogSource).     |

### Adding a new placeholder

To introduce a new `$MY_NEW_FILTER$` placeholder:

1. Add a constant for it in `KruizeConstants.KRUIZE_BULK_API` (e.g. `public static final String MY_NEW_FILTER = "$MY_NEW_FILTER$";`).
2. Add it to `KRUIZE_BULK_API.SUPPORTED_PLACEHOLDERS`.
3. If it is valid in update payloads and substituted before validation, also add it to `KRUIZE_BULK_API.UPDATE_PAYLOAD_PLACEHOLDERS` and handle its substitution in `MetadataProfileValidation.queryValidator`.
4. If it is bulk-only, handle its substitution in `DataSourceMetadataOperator`.

---

## Update Metadata Profile API

This is quick guide instructions to update metadata profile as follows.

**Request Parameters**

| Parameter | Type   | Required | Description                      |
|-----------|--------|----------|----------------------------------|
| name      | string | required | The name of the metadata profile |

> **Note:** All filter placeholders (`$UNSUPPORTED_WORKLOAD_TYPES$`, `$NAMESPACE_FILTERS$`, `$WORKLOAD_FILTERS$`, `$CONTAINER_FILTERS$`, `$LABELS$`) produce values that already include a leading `, `. Do not add a preceding comma before these placeholders in query templates. Separate them from adjacent filters using a space only.

**Request with name query parameter**

`PUT /updateMetadataProfile`

`curl -H 'Accept: application/json' -X PUT --data 'copy paste below JSON' http://<URL>:<PORT>/updateMetadataProfile?name=cluster-metadata-local-monitoring`

<details>

<summary><b>Request</b></summary>

Updates the specified metadata profile name, provided metadata profile is already created.

The queries below use the same template variables (`$MEASUREMENT_DURATION_IN_MIN$`, `$UNSUPPORTED_WORKLOAD_TYPES$`) as the bulk profile. These are substituted automatically during validation and at query-execution time. Bulk-only placeholders (`$NAMESPACE_FILTERS$` etc.) are replaced with concrete label selectors for the specific namespaces/workloads you want to target.

### Example Request

```json
{
  "apiVersion": "recommender.com/v1",
  "kind": "KruizeMetadataProfile",
  "metadata": {
    "name": "cluster-metadata-local-monitoring"
  },
  "profile_version": 1,
  "k8s_type": "openshift",
  "datasource": "prometheus",
  "query_variables": [
    {
      "name": "namespacesForAdditionalLabel",
      "datasource": "prometheus",
      "value_type": "double",
      "kubernetes_object": "container",
      "aggregation_functions": [
        {
          "function": "sum",
          "query": "sum by (namespace) (avg_over_time(kube_namespace_status_phase{$NAMESPACE_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
        }
      ]
    },
    {
      "name": "workloadsForAdditionalLabel",
      "datasource": "prometheus",
      "value_type": "double",
      "kubernetes_object": "container",
      "aggregation_functions": [
        {
          "function": "sum",
          "query": "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
        }
      ]
    },
    {
      "name": "workloadsWithPodLabelFilter",
      "datasource": "prometheus",
      "value_type": "double",
      "kubernetes_object": "container",
      "aggregation_functions": [
        {
          "function": "sum",
          "query": "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$ $LABELS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
        }
      ]
    },
    {
      "name": "containersForAdditionalLabel",
      "datasource": "prometheus",
      "value_type": "double",
      "kubernetes_object": "container",
      "aggregation_functions": [
        {
          "function": "sum",
          "query": "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))"
        }
      ]
    }
  ]
}
```

</details>

<details>
<summary><b>Response</b></summary>

### Example Response

```json
{
  "message": "Metadata profile: cluster-metadata-local-monitoring updated successfully. View Metadata Profiles at /listMetadataProfiles",
  "httpcode": 201,
  "documentationLink": "",
  "status": "SUCCESS"
}
```

</details>

