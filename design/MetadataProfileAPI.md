# Metadata Profile

This article describes how to add and retrieve Metadata Profiles with REST APIs using curl command. 
Documentation still in progress stay tuned.

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
            "query": "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m]))"
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
            "query": "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m]))"
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
            "query": "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=\"\" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      }
    ]
  }
]
```
</details>

### Delete Metadata Profile API

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
