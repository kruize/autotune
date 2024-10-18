# Bulk API Documentation

Bulk is an API designed to provide resource optimization recommendations in bulk for all available
containers, namespaces, etc., for a cluster connected via the datasource integration framework. Bulk can
be configured using filters like exclude/include namespaces, workloads, containers, or labels for generating
recommendations. It also has settings to generate recommendations at both the container or namespace level, or both.

Bulk returns a `job_id` as a response to track the job status. The user can use the `job_id` to monitor the
progress of the job.

## Task Flow When Bulk Is Invoked

1. Returns a unique `job_id`.
2. Background Bulk:
    - First, does a handshake with the datasource.
    - Using queries, it fetches the list of namespaces, workloads, containers of the connected datasource.
    - Creates experiments, one for each container *alpha release.
    - Triggers `generateRecommendations` for each container.
    - Once all experiments are created, and recommendations are generated, the system marks the `job_id` as "COMPLETED".

## API Specification

### POST /bulk

**Request Payload (JSON):**

```json
{
  "filter": {
    "exclude": {
      "namespace": [],
      "workload": [],
      "containers": [],
      "labels": {}
    },
    "include": {
      "namespace": [],
      "workload": [],
      "containers": [],
      "labels": {
        "key1": "value1",
        "key2": "value2"
      }
    }
  },
  "time_range": {},
  "datasource": "Cbank1Xyz",
  "experiment_types": [
    "container",
    "namespace"
  ]
}
```

**filter:** This object contains both exclusion and inclusion filters to specify the scope of data being queried.

- **exclude:** Defines the criteria to exclude certain data.
    - **namespace:** A list of Kubernetes namespaces to exclude. If empty, no namespaces are excluded.
    - **workload:** A list of workloads to exclude.
    - **containers:** A list of container names to exclude.
    - **labels:** Key-value pairs of labels to exclude.

- **include:** Defines the criteria to include specific data.
    - **namespace:** A list of Kubernetes namespaces to include.
    - **workload:** A list of workloads to include.
    - **containers:** A list of container names to include.
    - **labels:** Key-value pairs of labels to include.

- **time_range:** Specifies the time range for querying the data. If empty, no specific time range is applied.

- **datasource:** The data source, e.g., `"Cbank1Xyz"`.

- **experiment_types:** Specifies the type(s) of experiments to run, e.g., `"container"` or `"namespace"`.

### Success Response

- **Status:** 200 OK
- **Body:**

```json
{
  "job_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

### GET Request:

```bash
GET /bulk?job_id=123e4567-e89b-12d3-a456-426614174000
```

**Body (JSON):**

```json
{
  "status": "COMPLETED",
  "total_experiments": 23,
  "processed_experiments": 23,
  "job_id": "54905959-77d4-42ba-8e06-90bb97b823b9",
  "job_start_time": "2024-10-10T06:07:09.066Z",
  "job_end_time": "2024-10-10T06:07:17.471Z"
}
```

```bash
GET /bulk?job_id=123e4567-e89b-12d3-a456-426614174000&verbose=true
```

**Body (JSON):**
When verbose=true, additional detailed information about the job is provided.

```json
{
  "status": "IN_PROGRESS",
  "total_experiments": 23,
  "processed_experiments": 22,
  "data": {
    "experiments": {
      "new": [
        "prometheus-1|default|monitoring|node-exporter(daemonset)|node-exporter",
        "prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor",
        "prometheus-1|default|monitoring|alertmanager-main(statefulset)|config-reloader",
        "prometheus-1|default|monitoring|alertmanager-main(statefulset)|alertmanager",
        "prometheus-1|default|monitoring|prometheus-operator(deployment)|kube-rbac-proxy",
        "prometheus-1|default|kube-system|coredns(deployment)|coredns",
        "prometheus-1|default|monitoring|prometheus-k8s(statefulset)|config-reloader",
        "prometheus-1|default|monitoring|blackbox-exporter(deployment)|kube-rbac-proxy",
        "prometheus-1|default|monitoring|prometheus-operator(deployment)|prometheus-operator",
        "prometheus-1|default|monitoring|node-exporter(daemonset)|kube-rbac-proxy",
        "prometheus-1|default|monitoring|kube-state-metrics(deployment)|kube-rbac-proxy-self",
        "prometheus-1|default|monitoring|kube-state-metrics(deployment)|kube-state-metrics",
        "prometheus-1|default|monitoring|kruize(deployment)|kruize",
        "prometheus-1|default|monitoring|blackbox-exporter(deployment)|module-configmap-reloader",
        "prometheus-1|default|monitoring|prometheus-k8s(statefulset)|prometheus",
        "prometheus-1|default|monitoring|kube-state-metrics(deployment)|kube-rbac-proxy-main",
        "prometheus-1|default|kube-system|kube-proxy(daemonset)|kube-proxy",
        "prometheus-1|default|monitoring|prometheus-adapter(deployment)|prometheus-adapter",
        "prometheus-1|default|monitoring|grafana(deployment)|grafana",
        "prometheus-1|default|kube-system|kindnet(daemonset)|kindnet-cni",
        "prometheus-1|default|monitoring|kruize-db-deployment(deployment)|kruize-db",
        "prometheus-1|default|monitoring|blackbox-exporter(deployment)|blackbox-exporter"
      ],
      "updated": [],
      "failed": null
    },
    "recommendations": {
      "data": {
        "processed": [
          "prometheus-1|default|monitoring|alertmanager-main(statefulset)|config-reloader",
          "prometheus-1|default|monitoring|node-exporter(daemonset)|node-exporter",
          "prometheus-1|default|local-path-storage|local-path-provisioner(deployment)|local-path-provisioner",
          "prometheus-1|default|monitoring|alertmanager-main(statefulset)|alertmanager",
          "prometheus-1|default|monitoring|prometheus-operator(deployment)|kube-rbac-proxy",
          "prometheus-1|default|kube-system|coredns(deployment)|coredns",
          "prometheus-1|default|monitoring|blackbox-exporter(deployment)|kube-rbac-proxy",
          "prometheus-1|default|monitoring|prometheus-k8s(statefulset)|config-reloader",
          "prometheus-1|default|monitoring|prometheus-operator(deployment)|prometheus-operator",
          "prometheus-1|default|monitoring|node-exporter(daemonset)|kube-rbac-proxy",
          "prometheus-1|default|monitoring|kube-state-metrics(deployment)|kube-rbac-proxy-self",
          "prometheus-1|default|monitoring|kube-state-metrics(deployment)|kube-state-metrics",
          "prometheus-1|default|monitoring|kruize(deployment)|kruize",
          "prometheus-1|default|monitoring|blackbox-exporter(deployment)|module-configmap-reloader",
          "prometheus-1|default|monitoring|prometheus-k8s(statefulset)|prometheus",
          "prometheus-1|default|monitoring|kube-state-metrics(deployment)|kube-rbac-proxy-main",
          "prometheus-1|default|kube-system|kube-proxy(daemonset)|kube-proxy",
          "prometheus-1|default|monitoring|prometheus-adapter(deployment)|prometheus-adapter",
          "prometheus-1|default|monitoring|grafana(deployment)|grafana",
          "prometheus-1|default|kube-system|kindnet(daemonset)|kindnet-cni",
          "prometheus-1|default|monitoring|kruize-db-deployment(deployment)|kruize-db",
          "prometheus-1|default|monitoring|blackbox-exporter(deployment)|blackbox-exporter"
        ],
        "processing": [
          "prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor"
        ],
        "unprocessed": [
        ],
        "failed": []
      }
    }
  },
  "job_id": "5798a2df-6c67-467b-a3c2-befe634a0e3a",
  "job_start_time": "2024-10-09T18:09:31.549Z",
  "job_end_time": null
}
```

### Response Parameters

## API Description: Experiment and Recommendation Processing Status

This API response describes the status of a job that processes multiple experiments and generates recommendations for
resource optimization in Kubernetes environments. Below is a breakdown of the JSON response:

### Fields:

- **status**:
    - **Type**: `String`
    - **Description**: Current status of the job. Can be "IN_PROGRESS", "COMPLETED", "FAILED", etc.

- **total_experiments**:
    - **Type**: `Integer`
    - **Description**: Total number of experiments to be processed in the job.

- **processed_experiments**:
    - **Type**: `Integer`
    - **Description**: Number of experiments that have been processed so far.

- **data**:
    - **Type**: `Object`
    - **Description**: Contains detailed information about the experiments and recommendations being processed.

    - **experiments**:
        - **new**:
            - **Type**: `Array of Strings`
            - **Description**: List of new experiments that have been identified but not yet processed.

        - **updated**:
            - **Type**: `Array of Strings`
            - **Description**: List of experiments that were previously processed but have now been updated.

        - **failed**:
            - **Type**: `null or Array`
            - **Description**: List of experiments that failed during processing. If no failures, the value is `null`.

    - **recommendations**:
        - **data**:
            - **processed**:
                - **Type**: `Array of Strings`
                - **Description**: List of experiments for which recommendations have already been processed.

            - **processing**:
                - **Type**: `Array of Strings`
                - **Description**: List of experiments that are currently being processed for recommendations.

            - **unprocessed**:
                - **Type**: `Array of Strings`
                - **Description**: List of experiments that have not yet been processed for recommendations.

            - **failed**:
                - **Type**: `Array of Strings`
                - **Description**: List of experiments for which the recommendation process failed.

- **job_id**:
    - **Type**: `String`
    - **Description**: Unique identifier for the job.

- **job_start_time**:
    - **Type**: `String (ISO 8601 format)`
    - **Description**: Start timestamp of the job.

- **job_end_time**:
    - **Type**: `String (ISO 8601 format) or null`
    - **Description**: End timestamp of the job. If the job is still in progress, this will be `null`.

**Note: Experiment Name:**

- **Naming Pattern:** Experiment names are currently formed using the following pattern:
  `datasource_name|cluster_name|namespace|workload_name(workload_type)|container_name`
    - **Example:** For a Prometheus datasource, if the cluster is named `prod-cluster`, namespace is `default`, workload
      is `nginx` (of type `Deployment`), and container is `nginx-container`, the experiment name would be:
      `Prometheus|prod-cluster|default|nginx(Deployment)|nginx-container`

# Bulk Service Configuration

*Note: Configuration is subject to change.*

## Datasource

- **Description:** Provide the details about the datasource during Kruize configuration. This is essential for
  generating accurate resource optimization recommendations.
- **Example:** During configuration, the datasource could be Prometheus or Thanos, based on the setup for your
  Kubernetes cluster.
- Comprehensive configuration details and an example are available at the
  following [link](https://github.com/kruize/autotune/blob/cce96ae68876d6ed2afe505bab04efd1567c8239/manifests/crc/default-db-included-installation/openshift/kruize-crc-openshift.yaml#L133).

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
name: kruizeconfig
namespace: openshift-tuning
data:
kruizeconfigjson: |
  {
    "datasource": [
      {
        "name": "prometheus-1",
        "provider": "prometheus",
        "serviceName": "prometheus-k8s",
        "namespace": "openshift-monitoring",
        "url": "",
        "authentication": {
          "type": "bearer",
          "credentials": {
            "tokenFilePath": "/var/run/secrets/kubernetes.io/serviceaccount/token"
          }
        }
      }
    ]
  }
```

## Limits

- **Default Limit:** Currently, the Bulk service supports only **1000 experiments** by default.
- **Increasing the Limit:** You can increase this limit by setting the environment variable `bulkapilimit`.
- **Job Failure on Exceeding Limit:** If the number of experiments exceeds the set limit, the job will fail.

## Bulk API Threads

- **Control Mechanism:** The number of threads used for bulk API operations can be controlled using the environment
  variable `bulkThreadPoolSize`.
- **With/Without KEDA Plugin:** This can be configured with or without the KEDA plugin. Ideally, the value should be
  related to the Kruize pod count to optimize performance.