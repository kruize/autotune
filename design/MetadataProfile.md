# Metadata Profile

The metadata profile contains a list of queries used to retrieve datasource metadata such as list of namespaces, workloads
and containers. Users can create metadata profiles based on their cluster or datasource provider, such as Prometheus or
Thanos. These profiles can be tagged to import metadata API, which will then fetch metadata according to the metadata
profile, which further helps to create experiments followed by generating recommendations.

This document describes the fields of Metadata Profile and the different set of queries supported by Kruize.
Documentation still in progress stay tuned.

## Attributes

- **apiVersion** \
  A string representing version of the Kubernetes API to create metadata profile
- **kind** \
  A string representing type of kubernetes object
- **metadata** \
  A JSON object containing Data that helps to uniquely identify the metadata profile, including a name string
    - **name** \
      A unique string name for identifying each metadata profile.
- **profile_version** \
  A double value specifying the current version of the profile.
- **datasource** \
  A string representing the datasource to import metadata from
- **query_variables** \
  Define the query variables to be used
    - **name** \
      name of the variable
    - **datasource** \
      datasource of the query
    - **value_type** \
      can be double or integer
    - **query** \
      one of the query or _aggregation_functions_ is mandatory. Both can be present.
    - **kubernetes_object** \
      k8s object that this query is tied to: "_deployment_", "_pod_" or "_container_"
    - **aggregation_functions** \
      aggregate functions associated with this variable
        - **function** \
          can be '_avg_', '_sum_', '_min_', '_max_'
        - **query** \
          corresponding query
        - **version** \
          Any specific version that this query is tied to

### Different set of metadata queries

#### Queries to import metadata across the cluster

These set of queries fetch list of all the namespaces, workloads and containers present across the cluster

| Name                    | Query                                                                                                                                                                                                                                                                                                                           |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| namespacesAcrossCluster | sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=""}[$MEASUREMENT_DURATION_IN_MIN$m]))                                                                                                                                                                                                                  |
| workloadsAcrossCluster  | sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=""}[$MEASUREMENT_DURATION_IN_MIN$m]))                                                                                                                                                                        |
| containersAcrossCluster | sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=""}[$MEASUREMENT_DURATION_IN_MIN$m])<br/> * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=""}[$MEASUREMENT_DURATION_IN_MIN$m])) |


<br>

#### Queries to import metadata for specific org_id and cluster_id 

These set of queries fetch list of namespaces, workloads and containers for specific `org_id` and `cluster_id`

| Name                         | Query                                                                                                                                                                                                                                                                                                                                                                                                                        |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| namespacesForOrgAndClusterId | sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!="", org_id="$ORG_ID$", cluster_id="$CLUSTER_ID$"}[$MEASUREMENT_DURATION_IN_MIN$m]))                                                                                                                                                                                                                                                                 |
| workloadsForOrgAndClusterId  | sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!="", org_id="$ORG_ID$", cluster_id="$CLUSTER_ID$"}[$MEASUREMENT_DURATION_IN_MIN$m]))                                                                                                                                                                                                                       |
| containersForOrgAndClusterId | sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!="", org_id="$ORG_ID$", cluster_id="$CLUSTER_ID$"}[$MEASUREMENT_DURATION_IN_MIN$m]) <br/> * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!="", org_id="$ORG_ID$", cluster_id="$CLUSTER_ID$"}[$MEASUREMENT_DURATION_IN_MIN$m])) |

<br>

#### Queries to import metadata for custom label - ADDITIONAL_LABEL

These set of queries fetch list of namespaces, workloads and containers for specific `ADDITIONAL_LABEL` - currently used by bulk and thanos demos

| Name                         | Query                                                                                                                                                                                                                                                                                                                                                              |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| namespacesForAdditionalLabel | sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!="" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m]))                                                                                                                                                                                                                                    |
| workloadsForAdditionalLabel  | sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!="" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m]))                                                                                                                                                                                          |
| containersForAdditionalLabel | sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!="" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m]) <br/> * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!="" ADDITIONAL_LABEL}[$MEASUREMENT_DURATION_IN_MIN$m])) |


### Supported query placeholders

Metadata profile query templates can use the following `$...$` placeholders, which are substituted at runtime:

| Placeholder                      | Substituted by                       | Notes                                                                                                              |
|----------------------------------|--------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `$MEASUREMENT_DURATION_IN_MIN$`  | Measurement duration (integer)       | Used in all profiles.                                                                                              |
| `$UNSUPPORTED_WORKLOAD_TYPES$`   | Workload type exclusion filters      | Substituted value **includes a leading comma** (`, workload_type!~...`). Do not add a comma before this placeholder in the query template — it will result in a double comma and an invalid PromQL query. Use a space instead: `{workload!="" $UNSUPPORTED_WORKLOAD_TYPES$}`. |
| `$NAMESPACE_FILTERS$`            | Namespace include/exclude filters    | Bulk profiles only. Value includes a leading comma.                                                                |
| `$WORKLOAD_FILTERS$`             | Workload include/exclude filters     | Bulk profiles only. Value includes a leading comma.                                                                |
| `$CONTAINER_FILTERS$`            | Container include/exclude filters    | Bulk profiles only. Value includes a leading comma.                                                                |
| `$LABELS$`                       | Pod label filters                    | Bulk profiles only. Value includes a leading comma.                                                                |

> **Important:** All filter placeholders (`$UNSUPPORTED_WORKLOAD_TYPES$`, `$NAMESPACE_FILTERS$`, `$WORKLOAD_FILTERS$`, `$CONTAINER_FILTERS$`, `$LABELS$`) produce values that already include a leading `, `. Do not add a preceding comma before these placeholders in query templates. Separate them from adjacent filters using a space only.

* #####  Refer [REST APIs doc](/design/MetadataProfileAPI.md) supported for more details.