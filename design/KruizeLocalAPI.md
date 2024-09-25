# Local Monitoring Mode - Proof of Concept

This article describes how to quickly get started with the Kruize Local Monitoring Mode use case REST API using curl command.
Documentation still in progress stay tuned.

# Table of Contents

1. [Resource Analysis Terms and Defaults](#resource-analysis-terms-and-defaults)

- [Terms, Duration & Threshold Table](#terms-duration--threshold-table)

2. [API's](#apis)
- [List Datasources API](#list-datasources-api)
    - Introduction
    - Example Request and Response
    - Invalid Scenarios

- [Import Metadata API](#import-metadata-api)
    - Introduction
    - Example Request and Response
    - Invalid Scenarios

- [List Metadata API](#list-metadata-api)
  - Introduction
  - Example Request and Response
  - Invalid Scenarios

- [Delete Metadata API](#delete-metadata-api)
  - Introduction
  - Example Request and Response
  - Invalid Scenarios

- [Create Metric Profile API](#create-metric-profile-api)
  - Introduction
  - Example Request and Response
  - Invalid Scenarios

- [List Metric Profiles API](#list-metric-profiles-api)
  - Introduction
  - Example Request and Response
  - Invalid Scenarios

- [Delete Metric Profile API](#delete-metric-profile-api)
  - Introduction
  - Example Request and Response
  - Invalid Scenarios

- [Create Experiment API](#create-experiment-api)
    - Introduction
    - Example Request and Response
    - Invalid Scenarios

- [List Experiments API](#list-experiments-api)
    - Introduction
    - Example Request and Response
    - Invalid Scenarios

- [Generate Recommendations API](#generate-recommendations-api)
    - Introduction
    - Example Request and Response
    - Invalid Scenarios

<a name="resource-analysis-terms-and-defaults"></a>

## Resource Analysis Terms and Defaults

When analyzing resource utilization in Kubernetes, it's essential to define terms that specify the duration of past data
considered for recommendations and the threshold for obtaining additional data. These terms help in categorizing and
fine-tuning resource allocation.

Below are the default terms used in resource analysis, along with their respective durations and thresholds:

<a name="terms-duration--threshold-table"></a>

### Terms, Duration & Threshold Table

| Term   | Minimum Data Threshold | Duration | 
|--------|------------------------|----------|
| Short  | 30 mins                | 1 day    | 
| Medium | 2 Days                 | 7 days   | 
| Long   | 8 Days                 | 15 days  | 

**Minimum Data Threshold**: The "minimum data threshold" represents the minimum amount of data needed for generating a
recommendation associated with a given duration term.

**Duration**: The "duration" in the term analysis refers to the amount of historical data taken into account when
assessing resource utilization.

Read more about the Term Threshold scenarios [here](TermThresholdDesign.md)

### Profile Algorithm's (How Kruize calculate's the recommendations)

**Profile:**

This column represents different profiles or criteria that the recommendation algorithm takes into account when making
recommendations.

**CPU (Percentile):**

It indicates the percentile value for the timeseries CPU usage data that the algorithm considers for each profile.

**Memory (Percentile):**

Similarly, this column denotes the percentile value for the timeseries memory usage data that is used by the algorithm
for each profile.

#### Profiles

**Cost Profile:**
For the "Cost" profile, Kruize's recommendation algorithm will consider the 60th percentile for CPU usage and the 100th
percentile for memory usage when making recommendations. This means that cost-related recommendations will be based on
CPU usage that falls at or above the 60th percentile and memory usage at the 100th percentile.

**Performance Profile:**
In the "Performance" profile, the algorithm takes into account the 98th percentile for CPU usage and the 100th
percentile for memory usage. Consequently, recommendations related to performance will be generated when CPU usage is at
or above the 98th percentile, and memory usage is at the 100th percentile.

| Profile     | CPU (Percentile) | Memory (percentile) |
|-------------|------------------|---------------------|
| Cost        | 60 th            | 100 th              |
| Performance | 98 th            | 100 th              |

<a name="apis"></a>

## API's

<a name="list-datasources-api"></a>

### List Datasources API

This is quick guide instructions to list available datasources as follows.

**Request without Parameter**

`GET /datasources`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/datasources`

If no parameter is passed API returns all the datasources available.

**Response**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "version": "v1.0",
  "datasources": [
    {
      "name": "prometheus-1",
      "provider": "prometheus",
      "serviceName": "prometheus-k8s",
      "namespace": "monitoring",
      "url": "http://prometheus-k8s.monitoring.svc.cluster.local:9090"
    }
  ]
}
```

</details>

**Request with datasource name parameter**

`GET /datasources`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/datasources?name=<datasource_name>`

Returns the datasource details of the specified datasource

**Response for datasource name - `prometheus-1`**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "version": "v1.0",
  "datasources": [
    {
      "name": "prometheus-1",
      "provider": "prometheus",
      "serviceName": "prometheus-k8s",
      "namespace": "monitoring",
      "url": "http://prometheus-k8s.monitoring.svc.cluster.local:9090"
    }
  ]
}
```

</details>

<a name="import-metadata-api"></a>

### Import Metadata API

This is quick guide instructions to import metadata using input JSON as follows.

**Request**
`POST /dsmetadata`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/dsmetadata`

<details>

<summary><b>Example Request</b></summary>

### Example Request

```json
{
  "version": "v1.0",
  "datasource_name": "prometheus-1"
}
```

</details>


**Response**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "datasources": {
    "prometheus-1": {
      "datasource_name": "prometheus-1",
      "clusters": {
        "default": {
          "cluster_name": "default"
        }
      }
    }
  }
}
```

</details>

<a name="list-metadata-api"></a>

### List Metadata API

This is quick guide instructions to retrieve metadata for a specific datasource as follows.

**Request Parameters**

| Parameter    | Type   | Required | Description                               |
|--------------|--------|----------|-------------------------------------------|
| datasource   | string | Yes      | The name of the datasource.               |
| cluster_name | string | optional | The name of the cluster                   |
| namespace    | string | optional | The namespace                             |
| verbose      | string | optional | Flag to retrieve container-level metadata |

In the context of `GET /dsmetadata` REST API, the term `verbose` refers to a parameter or option that controls
granularity of metadata included in the API response. When the verbose parameter is set to true, the API response 
includes granular container-level details in the metadata, offering a more comprehensive view of the clusters, namespaces,
workloads and containers associated with the specified datasource. When the verbose parameter is not provided or set to
false, the API response provides basic information like list of clusters, namespaces associated with the specified datasource.

**Request with datasource name parameter**

`GET /dsmetadata`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/dsmetadata?datasource=<datasource_name>`

Returns the list of cluster details of the specified datasource

**Response for datasource name - `prometheus-1`**

***Note:***
- Currently, only `default` cluster is supported for POC.
- When the `verbose` parameter is not provided, is set to `false` by default - the response provides basic information
about the clusters of the specified datasource.

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "datasources": {
    "prometheus-1": {
      "datasource_name": "prometheus-1",
      "clusters": {
        "default": {
          "cluster_name": "default"
        }
      }
    }
  }
}
```
</details>

<br>

**Request with verbose set to true and with datasource name parameter**

`GET /dsmetadata`

`curl -H 'Accept: application/json' "http://<URL>:<PORT>/dsmetadata?datasource=<datasource_name>&verbose=true"`

Returns the metadata of all the containers present in the specified datasource

***Note : When we don't pass `verbose` in the query URL, it is set to `false` by default.***

**Response for datasource name - `prometheus-1` and verbose - `true`**

With `verbose` parameter set to `true`, the response includes detailed metadata about all namespaces, workloads and
containers in addition to cluster information with the specified datasource.

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "datasources": {
    "prometheus-1": {
      "datasource_name": "prometheus-1",
      "clusters": {
        "default": {
          "cluster_name": "default",
          "namespaces": {
            "default": {
              "namespace": "default"
            },
            "cadvisor": {
              "namespace": "cadvisor",
              "workloads": {
                "cadvisor": {
                  "workload_name": "cadvisor",
                  "workload_type": "daemonset",
                  "containers": {
                    "cadvisor": {
                      "container_name": "cadvisor",
                      "container_image_name": "gcr.io/cadvisor/cadvisor:v0.45.0"
                    }
                  }
                }
              }
            },
            "kube-node-lease": {
              "namespace": "kube-node-lease"
            },
            "kube-system": {
              "namespace": "kube-system",
              "workloads": {
                "coredns": {
                  "workload_name": "coredns",
                  "workload_type": "deployment",
                  "containers": {
                    "coredns": {
                      "container_name": "coredns",
                      "container_image_name": "k8s.gcr.io/coredns/coredns:v1.8.6"
                    }
                  }
                },
                "kube-proxy": {
                  "workload_name": "kube-proxy",
                  "workload_type": "daemonset",
                  "containers": {
                    "kube-proxy": {
                      "container_name": "kube-proxy",
                      "container_image_name": "k8s.gcr.io/kube-proxy:v1.24.3"
                    }
                  }
                }
              }
            },
            "monitoring": {
              "namespace": "monitoring",
              "workloads": {
                "kube-state-metrics": {
                  "workload_name": "kube-state-metrics",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-state-metrics": {
                      "container_name": "kube-state-metrics",
                      "container_image_name": "k8s.gcr.io/kube-state-metrics/kube-state-metrics:v2.0.0"
                    },
                    "kube-rbac-proxy-self": {
                      "container_name": "kube-rbac-proxy-self",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "kube-rbac-proxy-main": {
                      "container_name": "kube-rbac-proxy-main",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    }
                  }
                },
                "node-exporter": {
                  "workload_name": "node-exporter",
                  "workload_type": "daemonset",
                  "containers": {
                    "node-exporter": {
                      "container_name": "node-exporter",
                      "container_image_name": "quay.io/prometheus/node-exporter:v1.1.2"
                    },
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    }
                  }
                },
                "postgres-deployment": {
                  "workload_name": "postgres-deployment",
                  "workload_type": "deployment",
                  "containers": {
                    "postgres": {
                      "container_name": "postgres",
                      "container_image_name": "quay.io/kruizehub/postgres:15.2"
                    }
                  }
                },
                "alertmanager-main": {
                  "workload_name": "alertmanager-main",
                  "workload_type": "statefulset",
                  "containers": {
                    "config-reloader": {
                      "container_name": "config-reloader",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.47.0"
                    },
                    "alertmanager": {
                      "container_name": "alertmanager",
                      "container_image_name": "quay.io/prometheus/alertmanager:v0.21.0"
                    }
                  }
                },
                "prometheus-adapter": {
                  "workload_name": "prometheus-adapter",
                  "workload_type": "deployment",
                  "containers": {
                    "prometheus-adapter": {
                      "container_name": "prometheus-adapter",
                      "container_image_name": "directxman12/k8s-prometheus-adapter:v0.8.4"
                    }
                  }
                },
                "kruize": {
                  "workload_name": "kruize",
                  "workload_type": "deployment",
                  "containers": {
                    "kruize": {
                      "container_name": "kruize",
                      "container_image_name": "quay.io/kruize/autotune_operator:0.0.21_mvp"
                    }
                  }
                },
                "grafana": {
                  "workload_name": "grafana",
                  "workload_type": "deployment",
                  "containers": {
                    "grafana": {
                      "container_name": "grafana",
                      "container_image_name": "grafana/grafana:7.5.4"
                    }
                  }
                },
                "prometheus-k8s": {
                  "workload_name": "prometheus-k8s",
                  "workload_type": "statefulset",
                  "containers": {
                    "config-reloader": {
                      "container_name": "config-reloader",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.47.0"
                    },
                    "prometheus": {
                      "container_name": "prometheus",
                      "container_image_name": "quay.io/prometheus/prometheus:v2.26.0"
                    }
                  }
                },
                "blackbox-exporter": {
                  "workload_name": "blackbox-exporter",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "module-configmap-reloader": {
                      "container_name": "module-configmap-reloader",
                      "container_image_name": "jimmidyson/configmap-reload:v0.5.0"
                    },
                    "blackbox-exporter": {
                      "container_name": "blackbox-exporter",
                      "container_image_name": "quay.io/prometheus/blackbox-exporter:v0.18.0"
                    }
                  }
                },
                "prometheus-operator": {
                  "workload_name": "prometheus-operator",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "prometheus-operator": {
                      "container_name": "prometheus-operator",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-operator:v0.47.0"
                    }
                  }
                }
              }
            },
            "kube-public": {
              "namespace": "kube-public"
            }
          }
        }
      }
    }
  }
}
```

</details>

<br>

**Request with datasource name and cluster name parameter**

`GET /dsmetadata`

`curl -H 'Accept: application/json' "http://<URL>:<PORT>/dsmetadata?datasource=<datasource_name>&cluster_name=<cluster_name>"`

Returns the list of namespaces present in the specified cluster name and datasource

**Response for datasource name - `prometheus-1` and cluster name - `default`**

With `verbose` parameter set to `false`, the response includes list of namespaces present in the specified cluster name 
and datasource.

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "datasources": {
    "prometheus-1": {
      "datasource_name": "prometheus-1",
      "clusters": {
        "default": {
          "cluster_name": "default",
          "namespaces": {
            "default": {
              "namespace": "default"
            },
            "cadvisor": {
              "namespace": "cadvisor"
            },
            "kube-node-lease": {
              "namespace": "kube-node-lease"
            },
            "kube-system": {
              "namespace": "kube-system"
            },
            "monitoring": {
              "namespace": "monitoring"
            },
            "kube-public": {
              "namespace": "kube-public"
            }
          }
        }
      }
    }
  }
}
```

</details>

<br>

**Request with datasource name, cluster name and verbose parameters**

`GET /dsmetadata`

`curl -H 'Accept: application/json' "http://<URL>:<PORT>/dsmetadata?datasource=<datasource_name>&cluster_name=<cluster_name>&verbose=true"`

Returns the container-level metadata of all the namespaces present in the specified cluster name and datasource

**Response for datasource name - `prometheus-1`, cluster name - `default` and verbose - `true`**

With `verbose` parameter set to `true`, the response includes detailed metadata about workloads and containers 
in addition to namespace information with the specified cluster name and datasource.

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "datasources": {
    "prometheus-1": {
      "datasource_name": "prometheus-1",
      "clusters": {
        "default": {
          "cluster_name": "default",
          "namespaces": {
            "default": {
              "namespace": "default"
            },
            "cadvisor": {
              "namespace": "cadvisor",
              "workloads": {
                "cadvisor": {
                  "workload_name": "cadvisor",
                  "workload_type": "daemonset",
                  "containers": {
                    "cadvisor": {
                      "container_name": "cadvisor",
                      "container_image_name": "gcr.io/cadvisor/cadvisor:v0.45.0"
                    }
                  }
                }
              }
            },
            "kube-node-lease": {
              "namespace": "kube-node-lease"
            },
            "kube-system": {
              "namespace": "kube-system",
              "workloads": {
                "coredns": {
                  "workload_name": "coredns",
                  "workload_type": "deployment",
                  "containers": {
                    "coredns": {
                      "container_name": "coredns",
                      "container_image_name": "k8s.gcr.io/coredns/coredns:v1.8.6"
                    }
                  }
                },
                "kube-proxy": {
                  "workload_name": "kube-proxy",
                  "workload_type": "daemonset",
                  "containers": {
                    "kube-proxy": {
                      "container_name": "kube-proxy",
                      "container_image_name": "k8s.gcr.io/kube-proxy:v1.24.3"
                    }
                  }
                }
              }
            },
            "monitoring": {
              "namespace": "monitoring",
              "workloads": {
                "kube-state-metrics": {
                  "workload_name": "kube-state-metrics",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-state-metrics": {
                      "container_name": "kube-state-metrics",
                      "container_image_name": "k8s.gcr.io/kube-state-metrics/kube-state-metrics:v2.0.0"
                    },
                    "kube-rbac-proxy-self": {
                      "container_name": "kube-rbac-proxy-self",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "kube-rbac-proxy-main": {
                      "container_name": "kube-rbac-proxy-main",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    }
                  }
                },
                "node-exporter": {
                  "workload_name": "node-exporter",
                  "workload_type": "daemonset",
                  "containers": {
                    "node-exporter": {
                      "container_name": "node-exporter",
                      "container_image_name": "quay.io/prometheus/node-exporter:v1.1.2"
                    },
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    }
                  }
                },
                "postgres-deployment": {
                  "workload_name": "postgres-deployment",
                  "workload_type": "deployment",
                  "containers": {
                    "postgres": {
                      "container_name": "postgres",
                      "container_image_name": "quay.io/kruizehub/postgres:15.2"
                    }
                  }
                },
                "alertmanager-main": {
                  "workload_name": "alertmanager-main",
                  "workload_type": "statefulset",
                  "containers": {
                    "config-reloader": {
                      "container_name": "config-reloader",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.47.0"
                    },
                    "alertmanager": {
                      "container_name": "alertmanager",
                      "container_image_name": "quay.io/prometheus/alertmanager:v0.21.0"
                    }
                  }
                },
                "prometheus-adapter": {
                  "workload_name": "prometheus-adapter",
                  "workload_type": "deployment",
                  "containers": {
                    "prometheus-adapter": {
                      "container_name": "prometheus-adapter",
                      "container_image_name": "directxman12/k8s-prometheus-adapter:v0.8.4"
                    }
                  }
                },
                "kruize": {
                  "workload_name": "kruize",
                  "workload_type": "deployment",
                  "containers": {
                    "kruize": {
                      "container_name": "kruize",
                      "container_image_name": "quay.io/kruize/autotune_operator:0.0.21_mvp"
                    }
                  }
                },
                "grafana": {
                  "workload_name": "grafana",
                  "workload_type": "deployment",
                  "containers": {
                    "grafana": {
                      "container_name": "grafana",
                      "container_image_name": "grafana/grafana:7.5.4"
                    }
                  }
                },
                "prometheus-k8s": {
                  "workload_name": "prometheus-k8s",
                  "workload_type": "statefulset",
                  "containers": {
                    "config-reloader": {
                      "container_name": "config-reloader",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.47.0"
                    },
                    "prometheus": {
                      "container_name": "prometheus",
                      "container_image_name": "quay.io/prometheus/prometheus:v2.26.0"
                    }
                  }
                },
                "blackbox-exporter": {
                  "workload_name": "blackbox-exporter",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "module-configmap-reloader": {
                      "container_name": "module-configmap-reloader",
                      "container_image_name": "jimmidyson/configmap-reload:v0.5.0"
                    },
                    "blackbox-exporter": {
                      "container_name": "blackbox-exporter",
                      "container_image_name": "quay.io/prometheus/blackbox-exporter:v0.18.0"
                    }
                  }
                },
                "prometheus-operator": {
                  "workload_name": "prometheus-operator",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "prometheus-operator": {
                      "container_name": "prometheus-operator",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-operator:v0.47.0"
                    }
                  }
                }
              }
            },
            "kube-public": {
              "namespace": "kube-public"
            }
          }
        }
      }
    }
  }
}
```
</details>

<br>

**Request with datasource name, cluster name and namespace parameters**

`GET /dsmetadata`

`curl -H 'Accept: application/json' "http://<URL>:<PORT>/dsmetadata?datasource=<datasource_name>&cluster_name=<cluster_name>&namespace=<namespace>"`

Returns the container-level metadata of the specified namespace, cluster name and datasource

***Note : `verbose` in the query URL to fetch container-level metadata is set to `true` by default***

**Response for datasource name - `prometheus-1`, cluster name - `default` and namespace - `monitoring`**

The response includes granular metadata about workloads and associated containers within specified namespace, cluster 
and datasource.

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "datasources": {
    "prometheus-1": {
      "datasource_name": "prometheus-1",
      "clusters": {
        "default": {
          "cluster_name": "default",
          "namespaces": {
            "monitoring": {
              "namespace": "monitoring",
              "workloads": {
                "kube-state-metrics": {
                  "workload_name": "kube-state-metrics",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-state-metrics": {
                      "container_name": "kube-state-metrics",
                      "container_image_name": "k8s.gcr.io/kube-state-metrics/kube-state-metrics:v2.0.0"
                    },
                    "kube-rbac-proxy-self": {
                      "container_name": "kube-rbac-proxy-self",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "kube-rbac-proxy-main": {
                      "container_name": "kube-rbac-proxy-main",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    }
                  }
                },
                "node-exporter": {
                  "workload_name": "node-exporter",
                  "workload_type": "daemonset",
                  "containers": {
                    "node-exporter": {
                      "container_name": "node-exporter",
                      "container_image_name": "quay.io/prometheus/node-exporter:v1.1.2"
                    },
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    }
                  }
                },
                "postgres-deployment": {
                  "workload_name": "postgres-deployment",
                  "workload_type": "deployment",
                  "containers": {
                    "postgres": {
                      "container_name": "postgres",
                      "container_image_name": "quay.io/kruizehub/postgres:15.2"
                    }
                  }
                },
                "alertmanager-main": {
                  "workload_name": "alertmanager-main",
                  "workload_type": "statefulset",
                  "containers": {
                    "config-reloader": {
                      "container_name": "config-reloader",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.47.0"
                    },
                    "alertmanager": {
                      "container_name": "alertmanager",
                      "container_image_name": "quay.io/prometheus/alertmanager:v0.21.0"
                    }
                  }
                },
                "prometheus-adapter": {
                  "workload_name": "prometheus-adapter",
                  "workload_type": "deployment",
                  "containers": {
                    "prometheus-adapter": {
                      "container_name": "prometheus-adapter",
                      "container_image_name": "directxman12/k8s-prometheus-adapter:v0.8.4"
                    }
                  }
                },
                "kruize": {
                  "workload_name": "kruize",
                  "workload_type": "deployment",
                  "containers": {
                    "kruize": {
                      "container_name": "kruize",
                      "container_image_name": "quay.io/kruize/autotune_operator:0.0.21_mvp"
                    }
                  }
                },
                "grafana": {
                  "workload_name": "grafana",
                  "workload_type": "deployment",
                  "containers": {
                    "grafana": {
                      "container_name": "grafana",
                      "container_image_name": "grafana/grafana:7.5.4"
                    }
                  }
                },
                "prometheus-k8s": {
                  "workload_name": "prometheus-k8s",
                  "workload_type": "statefulset",
                  "containers": {
                    "config-reloader": {
                      "container_name": "config-reloader",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.47.0"
                    },
                    "prometheus": {
                      "container_name": "prometheus",
                      "container_image_name": "quay.io/prometheus/prometheus:v2.26.0"
                    }
                  }
                },
                "blackbox-exporter": {
                  "workload_name": "blackbox-exporter",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "module-configmap-reloader": {
                      "container_name": "module-configmap-reloader",
                      "container_image_name": "jimmidyson/configmap-reload:v0.5.0"
                    },
                    "blackbox-exporter": {
                      "container_name": "blackbox-exporter",
                      "container_image_name": "quay.io/prometheus/blackbox-exporter:v0.18.0"
                    }
                  }
                },
                "prometheus-operator": {
                  "workload_name": "prometheus-operator",
                  "workload_type": "deployment",
                  "containers": {
                    "kube-rbac-proxy": {
                      "container_name": "kube-rbac-proxy",
                      "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.8.0"
                    },
                    "prometheus-operator": {
                      "container_name": "prometheus-operator",
                      "container_image_name": "quay.io/prometheus-operator/prometheus-operator:v0.47.0"
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

</details>

<br>

<a name="delete-metadata-api"></a>

### Delete Metadata API

This is quick guide instructions to delete metadata using input JSON as follows.

**Request**
`DELETE /dsmetadata`

`curl -H 'Accept: application/json' -X DELETE --data 'copy paste below JSON' http://<URL>:<PORT>/dsmetadata`

<details>

<summary><b>Example Request</b></summary>

### Example Request

```json
{
  "version": "v1.0",
  "datasource_name": "prometheus-1"
}
```

</details>


**Response**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "message": "Datasource metadata deleted successfully. View imported metadata at GET /dsmetadata",
  "httpcode": 201,
  "documentationLink": "",
  "status": "SUCCESS"
}
```

</details>

<br>

<a name="create-metric-profile-api"></a>

### Create Metric Profile API

This is quick guide instructions to create metric profiles using input JSON as follows. For a more detailed guide,
see [Create MetricProfile](/design/MetricProfileAPI.md)

**Request**
`POST /createMetricProfile`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createMetricProfile`

<details>

<summary><b>Example Request for profile name - `resource-optimization-local-monitoring`</b></summary>

### Example Request

```json
{
  "apiVersion": "recommender.com/v1",
  "kind": "KruizePerformanceProfile",
  "metadata": {
    "name": "resource-optimization-local-monitoring"
  },
  "profile_version": 1,
  "k8s_type": "openshift",
  "slo": {
    "slo_class": "resource_usage",
    "direction": "minimize",
    "objective_function": {
      "function_type": "source"
    },
    "function_variables": [
      {
        "name": "cpuRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD ', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "min",
            "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "max",
            "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          }
        ]
      },
      {
        "name": "cpuLimit",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "max",
            "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "min",
            "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          }
        ]
      },
      {
        "name": "cpuUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"

          },
          {
            "function": "min",
            "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"

          },
          {
            "function": "max",
            "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"

          },
          {
            "function": "sum",
            "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "cpuThrottle",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "max",
            "query": "max by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "min",
            "query": "min by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "memoryRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "max",
            "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "min",
            "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          }
        ]
      },
      {
        "name": "memoryLimit",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "max",
            "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          },
          {
            "function": "min",
            "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
          }
        ]
      },
      {
        "name": "memoryUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "min",
            "query": "min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "max",
            "query": "max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "memoryRSS",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "min",
            "query": "min by(container, namespace) (min_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "max",
            "query": "max by(container, namespace) (max_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
          }
        ]
      },
      {
        "name": "maxDate",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "max",
            "query": "max by(namespace,container) (last_over_time((timestamp(container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]))"
          }
        ]
      }
    ]
  }
}
```

</details>


**Response**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "message": "Metric Profile : resource-optimization-local-monitoring created successfully. View all metric profiles at /listMetricProfiles",
  "httpcode": 201,
  "documentationLink": "",
  "status": "SUCCESS"
}
```

</details>

<br>

<a name="list-metric-profiles-api"></a>

### List Metric Profiles API

This is quick guide instructions to retrieve metric profiles created as follows.

**Request Parameters**

| Parameter | Type   | Required | Description                             |
|-----------|--------|----------|-----------------------------------------|
| name      | string | optional | The name of the metric profile          |
| verbose   | string | optional | Flag to retrieve all the metric queries |

**Request without passing parameters**

`GET /listMetricProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetricProfiles`

Returns list of all the metric profile names created

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
[
  {
    "name": "resource-optimization-local-monitoring"
  },
  {
    "name": "resource-optimization-local-monitoring1"
  }
]
```

</details>

<br>

**Request with metric profile name**

`GET /listMetricProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetricProfiles?name=resource-optimization-local-monitoring`

Returns metric profile of the name specified

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
[
  {
    "apiVersion": "recommender.com/v1",
    "kind": "KruizePerformanceProfile",
    "metadata": {
      "name": "resource-optimization-local-monitoring"
    },
    "profile_version": 1.0,
    "k8s_type": "openshift",
    "slo": {
      "sloClass": "resource_usage",
      "objective_function": {
        "function_type": "source"
      },
      "direction": "minimize",
      "function_variables": [
        {
          "name": "cpuRequest",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD ', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "cpuLimit",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "cpuUsage",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "cpuThrottle",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "memoryRequest",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "memoryLimit",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "memoryUsage",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "memoryRSS",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (min_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (max_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "maxDate",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "max": {
              "function": "max",
              "query": "max by(namespace,container) (last_over_time((timestamp(container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]))"
            }
          }
        }
      ]
    }
  }
]
```

</details>

<br>

**Request**

`GET /listMetricProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetricProfiles?verbose=true`

Returns list of all the metric profile created with all the metric queries

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
[
  {
    "apiVersion": "recommender.com/v1",
    "kind": "KruizePerformanceProfile",
    "metadata": {
      "name": "resource-optimization-local-monitoring"
    },
    "profile_version": 1.0,
    "k8s_type": "openshift",
    "slo": {
      "sloClass": "resource_usage",
      "objective_function": {
        "function_type": "source"
      },
      "direction": "minimize",
      "function_variables": [
        {
          "name": "cpuRequest",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD ', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "cpuLimit",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "cpuUsage",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "cpuThrottle",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "memoryRequest",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "memoryLimit",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "memoryUsage",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "memoryRSS",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (min_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (max_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "maxDate",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "max": {
              "function": "max",
              "query": "max by(namespace,container) (last_over_time((timestamp(container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]))"
            }
          }
        }
      ]
    }
  },
  {
    "apiVersion": "recommender.com/v1",
    "kind": "KruizePerformanceProfile",
    "metadata": {
      "name": "resource-optimization-local-monitoring1"
    },
    "profile_version": 1.0,
    "k8s_type": "openshift",
    "slo": {
      "sloClass": "resource_usage",
      "objective_function": {
        "function_type": "source"
      },
      "direction": "minimize",
      "function_variables": [
        {
          "name": "cpuRequest",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD ', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "cpuLimit",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "cpuUsage",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "cpuThrottle",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "memoryRequest",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "memoryLimit",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "min": {
              "function": "min",
              "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "max": {
              "function": "max",
              "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
            }
          }
        },
        {
          "name": "memoryUsage",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "memoryRSS",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "avg": {
              "function": "avg",
              "query": "avg by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "min": {
              "function": "min",
              "query": "min by(container, namespace) (min_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "max": {
              "function": "max",
              "query": "max by(container, namespace) (max_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            },
            "sum": {
              "function": "sum",
              "query": "sum by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
            }
          }
        },
        {
          "name": "maxDate",
          "datasource": "prometheus",
          "value_type": "double",
          "kubernetes_object": "container",
          "aggregation_functions": {
            "max": {
              "function": "max",
              "query": "max by(namespace,container) (last_over_time((timestamp(container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]))"
            }
          }
        }
      ]
    }
  }
]
```

</details>

<br>

<a name="delete-metric-profile-api"></a>


### Delete Metric Profile API

This is quick guide instructions to delete metric profile created as follows.

**Request Parameters**

| Parameter | Type   | Required | Description                             |
|-----------|--------|----------|-----------------------------------------|
| name      | string | required | The name of the metric profile          |


**Request with name query parameter**

`DELETE /deleteMetricProfile`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/deleteMetricProfile?name=resource-optimization-local-monitoring`

Deletes the specified metric profile name, provided metric profile already is created

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "message": "Metric profile: resource-optimization-local-monitoring deleted successfully. View Metric Profiles at /listMetricProfiles",
  "httpcode": 201,
  "documentationLink": "",
  "status": "SUCCESS"
}
```

</details>

<br>

<a name="create-experiment-api"></a>

### Create Experiment API

This is quick guide instructions to create experiments using input JSON as follows. For a more detailed guide,
see [Create Experiment](/design/CreateExperiment.md)

**Request**
`POST /createExperiment`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createExperiment`

<details>

<summary><b>Example Request for datasource - `prometheus-1`</b></summary>

### Example Request

```json
[
  {
    "version": "v2.0",
    "experiment_name": "default|default|deployment|tfb-qrh-deployment",
    "cluster_name": "default",
    "performance_profile": "resource-optimization-openshift",
    "mode": "monitor",
    "target_cluster": "local",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment",
        "namespace": "default",
        "containers": [
          {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          },
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          }
        ]
      }
    ],
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "datasource": "prometheus-1"
  }
]
```

</details>

**Request with `experiment_type` field**

The `experiment_type` field in the JSON is optional and can be used to
indicate whether the experiment is of type `namespace` or `container`.
If no experiment type is specified, it will default to `container`.

<details>
  <summary><b>Example Request with `experiment_type` - `namespace`</b></summary>
  The `experiment_type` field in the JSON is optional and can be used to 
indicate whether the experiment is of type `namespace` or `container`. 
If no experiment type is specified, it will default to `container`.

### EXAMPLE REQUEST
```json
[{
  "version": "v2.0",
  "experiment_name": "default|namespace-demo",
  "cluster_name": "default",
  "performance_profile": "resource-optimization-local-monitoring",
  "mode": "monitor",
  "target_cluster": "local",
  "datasource": "prometheus-1",
  "experiment_type": "namespace",
  "kubernetes_objects": [
    {
        "namespaces": {
            "namespace_name": "test-multiple-import"
      }
    }
  ],
  "trial_settings": {
    "measurement_duration": "15min"
  },
  "recommendation_settings": {
    "threshold": "0.1"
  }
}]
```
</details>

<details>
  <summary><b>Example Request with `experiment_type` - `container`</b></summary>

### EXAMPLE REQUEST
```json
[
  {
    "version": "v2.0",
    "experiment_name": "default|default|deployment|tfb-qrh-deployment",
    "cluster_name": "default",
    "performance_profile": "resource-optimization-openshift",
    "mode": "monitor",
    "target_cluster": "local",
    "experiment_type": "container",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment",
        "namespace": "default",
        "containers": [
          {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          },
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          }
        ]
      }
    ],
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "datasource": "prometheus-1"
  }
]
```
</details>

**Response**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "message": "Experiment registered successfully with Autotune. View registered experiments at /listExperiments",
  "httpcode": 201,
  "documentationLink": "",
  "status": "SUCCESS"
}
```



</details>

<a name="list-experiments-api"></a>

### List Experiments API

**Request with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?experiment_name=<experiment_name>`

Returns the experiment details of the specified experiment
<br><br>

**Request with recommendations set to true**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true`

Returns the latest recommendations of all the experiments

**Response for experiment name - `default|default_0|deployment|tfb-qrh-deployment_0`**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
[
  {
    "version": "v2.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "default|default_0|deployment|tfb-qrh-deployment_0",
    "cluster_name": "default",
    "datasource": "prometheus-1",
    "experiment_type": "container",
    "mode": "monitor",
    "target_cluster": "local",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": false,
      "local_monitoring": true,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "112101": {
                  "type": "info",
                  "message": "Cost Recommendations Available",
                  "code": 112101
                }
              },
              "data": {
                "2023-04-02T08:00:00.680Z": {
                  "cost": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T06:45:00.000Z",
                      "monitoring_end_time": "2023-04-02T08:00:00.680Z",
                      "duration_in_hours": 24.0,
                      "pods_count": 27,
                      "confidence_level": 0.0,
                      "current": {
                        "requests": {
                          "memory": {
                            "amount": 490.93,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 1.46,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 712.21,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 1.54,
                            "format": "cores"
                          }
                        }
                      },
                      "config": {
                        "requests": {
                          "memory": {
                            "amount": 1197.9840000000002,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 7.68,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 1197.9840000000002,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 7.68,
                            "format": "cores"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "memory": {
                            "amount": 707.0540000000001,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 6.22,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 485.7740000000001,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 6.14,
                            "format": "cores"
                          }
                        }
                      },
                      "notifications": {}
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "120001": {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation.",
                  "code": 120001
                }
              },
              "data": {}
            }
          }
        }
      }
    ]
  },
  ...
  ...
  ...
  {
    "version": "v2.0",
    "experiment_id": "ab0a31a522cebdde52561482300d078ed1448fa7b75834fa216677d1d9d5cda6",
    "experiment_name": "default|default_1|deployment|tfb-qrh-deployment_1",
    "cluster_name": "default",
    "datasource": "prometheus-1",
    "experiment_type": "container",
    "mode": "monitor",
    "target_cluster": "local",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": false,
      "local_monitoring": true,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_1",
        "namespace": "default_1",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "120001": {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation.",
                  "code": 120001
                }
              },
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "120001": {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation.",
                  "code": 120001
                }
              },
              "data": {}
            }
          }
        }
      }
    ]
  },
]
```

</details>


<br><br>
**Request with recommendations set to true with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&experiment_name=<experiment_name>`

Returns the latest recommendations of the specified experiment with no results
<br><br>

**Request with recommendations set to true and latest set to false**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&latest=false`

Returns all the recommendations of all the experiments

**Response for experiment name - `default|default_0|deployment|tfb-qrh-deployment_0`**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
[
  {
    "version": "v2.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "default|default_0|deployment|tfb-qrh-deployment_0",
    "cluster_name": "default",
    "datasource": "prometheus-1",
    "mode": "monitor",
    "target_cluster": "local",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": false,
      "local_monitoring": true,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "112101": {
                  "type": "info",
                  "message": "Cost Recommendations Available",
                  "code": 112101
                }
              },
              "data": {
                "2023-04-02T06:00:00.770Z": {
                  "cost": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T04:45:00.000Z",
                      "monitoring_end_time": "2023-04-02T06:00:00.770Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "current": {
                        "requests": {
                          "memory": {
                            "amount": 490.93,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 1.46,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 712.21,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 1.54,
                            "format": "cores"
                          }
                        }
                      },
                      "config": {
                        "requests": {
                          "memory": {
                            "amount": 1197.9840000000002,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 7.68,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 1197.9840000000002,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 7.68,
                            "format": "cores"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "memory": {
                            "amount": 707.0540000000001,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 6.22,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 485.7740000000001,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 6.14,
                            "format": "cores"
                          }
                        }
                      },
                      "notifications": {}
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    }
                  }
                },
                ...
                ...
                ...
                "2023-04-02T04:30:00.000Z": {
                  "cost": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T03:15:00.000Z",
                      "monitoring_end_time": "2023-04-02T04:30:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "current": {
                        "requests": {
                          "memory": {
                            "amount": 490.93,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 1.46,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 712.21,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 1.54,
                            "format": "cores"
                          }
                        }
                      },
                      "config": {
                        "requests": {
                          "memory": {
                            "amount": 1197.9840000000002,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 7.68,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 1197.9840000000002,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 7.68,
                            "format": "cores"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "memory": {
                            "amount": 707.0540000000001,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 6.22,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "memory": {
                            "amount": 485.7740000000001,
                            "format": "MiB"
                          },
                          "cpu": {
                            "amount": 6.14,
                            "format": "cores"
                          }
                        }
                      },
                      "notifications": {}
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "120001": {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation.",
                  "code": 120001
                }
              },
              "data": {}
            }
          }
        }
      }
    ]
  },
  ...
  ...
  ...
]
```

</details>


<br><br>
**Request with recommendations set to true, latest set to false and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&latest=false&experiment_name=<experiment_name>`

Returns all the recommendations of the specified experiment
<br><br>

**List Experiments also allows the user to send a request body to fetch the records based on `cluster_name` and `kubernetes_object`.**
<br><br>
*Note: This request body can be sent along with other query params which are mentioned above.*

`curl -H 'Accept: application/json' -X GET --data 'copy paste below JSON' http://<URL>:<PORT>/listExperiments`

<details>

<summary><b>Example Request</b></summary>

### Example Request

```json
{
  "cluster_name": "default",
  "kubernetes_objects": [
    {
      "type": "deployment",
      "name": "tfb-qrh-deployment",
      "namespace": "default",
      "containers": [
        {
          "container_image_name": "kruize/tfb-db:1.15",
          "container_name": "tfb-server-1"
        }
      ]
    }
  ]
}
```

</details>

---
<a name="generate-recommendations-api"></a>

### Generate Recommendations API

**Note: This API is specific to the Local Monitoring use case.** <br>
Generates the recommendation for a specific experiment based on provided parameters similar to update recommendations API.
This can be called directly after creating the experiment and doesn't require the update results API as metrics are
fetched from the provided `datasource` (E.g. Prometheus) instead of the database.

**Request Parameters**

| Parameter           | Type   | Required | Description                                                                                                                                |
|---------------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------------------------|
| experiment_name     | string | Yes      | The name of the experiment.                                                                                                                |
| interval_end_time   | string | optional | The end time of the interval in the format `yyyy-MM-ddTHH:mm:sssZ`. This should be the date on which recommendation needs to be generated. |
| interval_start_time | string | optional | The start time of the interval in the format `yyyy-MM-ddTHH:mm:sssZ`.                                                                      |

The recommendation API requires only one mandatory field i.e. `experiment_name`. Other optional parameter like `interval_end_time` will be fetched from the provided datasource.
Similarly, `interval_start_time` will be calculated based on `interval_end_time`, if not provided. By utilizing
these parameters, the API generates recommendations based on short-term, medium-term, and long-term factors. For
instance, if the long-term setting is configured for `15 days` and the interval_end_time is set to `Jan 15 2023 00:00:
00.000Z`, the API retrieves data from the past 15 days, starting from January 1st. Using this data, the API generates
three recommendations for `Jan 15th 2023`.

It is important to ensure that the difference between `interval_end_time` and `interval_start_time` should not exceed 15
days. This restriction is in place to prevent potential timeouts, as generating recommendations beyond this threshold
might require more time.

**Request with experiment name and interval_end_time parameters**

`POST /generateRecommendations?experiment_name=?&interval_end_time=?`

`POST /generateRecommendations?experiment_name=?&interval_end_time=?&interval_start_time=?`

example

`curl --location --request POST 'http://<URL>:<PORT>/generateRecommendations?interval_end_time=2023-01-02T00:15:00.000Z&experiment_name=temp_1'`

success status code : 201

**Response**

The response will contain a array of JSON object with the recommendations for the specified experiment.

<details>
<summary><b>Example Response Body</b></summary>

```json
[
  {
    "cluster_name": "default",
    "experiment_type": "namespace",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_5",
        "namespace": "default_5",
        "containers": [
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "111000": {
                  "type": "info",
                  "message": "Recommendations Are Available",
                  "code": 111000
                }
              },
              "data": {
                "2023-04-02T13:30:00.680Z": {
                  "notifications": {
                    "111101": {
                      "type": "info",
                      "message": "Short Term Recommendations Available",
                      "code": 111101
                    }
                  },
                  "monitoring_end_time": "2023-04-02T13:30:00.680Z",
                  "current": {
                    "limits": {
                      "memory": {
                        "amount": 1.048576E8,
                        "format": "bytes"
                      },
                      "cpu": {
                        "amount": 0.5,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 5.264900096E7,
                        "format": "bytes"
                      },
                      "cpu": {
                        "amount": 5.37,
                        "format": "cores"
                      }
                    }
                  },
                  "recommendation_terms": {
                    "short_term": {
                      "duration_in_hours": 24.0,
                      "notifications": {
                        "112101": {
                          "type": "info",
                          "message": "Cost Recommendations Available",
                          "code": 112101
                        },
                        "112102": {
                          "type": "info",
                          "message": "Performance Recommendations Available",
                          "code": 112102
                        }
                      },
                      "monitoring_start_time": "2023-04-01T12:00:00.000Z",
                      "recommendation_engines": {
                        "cost": {
                          "pods_count": 7,
                          "confidence_level": 0.0,
                          "config": {
                            "limits": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            }
                          },
                          "variation": {
                            "limits": {
                              "memory": {
                                "amount": 1.449132032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 1.9712180223999997902848E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            }
                          },
                          "notifications": {}
                        },
                        "performance": {
                          "pods_count": 27,
                          "confidence_level": 0.0,
                          "config": {
                            "limits": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            }
                          },
                          "variation": {
                            "limits": {
                              "memory": {
                                "amount": 1.449132032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 1.9712180223999997902848E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            }
                          },
                          "notifications": {}
                        }
                      }
                    },
                    "medium_term": {
                      "duration_in_hours": 33.8,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "duration_in_hours": 33.8,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "120001": {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation.",
                  "code": 120001
                }
              },
              "data": {}
            }
          }
        ]
      }
    ],
    "version": "v2.0",
    "experiment_name": "temp_1"
  }
]
```

</details>

**Request without interval_end_time parameter**

`POST /generateRecommendations?experiment_name=?`

example

`curl --location --request POST 'http://<URL>:<PORT>/generateRecommendations?experiment_name=temp_1'`

success status code : 201

**Response**

The response will contain an array of JSON object with the recommendations for the specified experiment.

When `interval_end_time` is not specified, Kruize will determine the latest timestamp from the specified datasource
(E.g. Prometheus) by checking the latest active container CPU usage.

<details>
<summary><b>Example Response Body</b></summary>

```json
[
  {
    "cluster_name": "default",
    "experiment_type": "container",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_5",
        "namespace": "default_5",
        "containers": [
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "111000": {
                  "type": "info",
                  "message": "Recommendations Are Available",
                  "code": 111000
                }
              },
              "data": {
                "2023-04-02T13:30:00.680Z": {
                  "notifications": {
                    "111101": {
                      "type": "info",
                      "message": "Short Term Recommendations Available",
                      "code": 111101
                    }
                  },
                  "monitoring_end_time": "2023-04-02T13:30:00.680Z",
                  "current": {
                    "limits": {
                      "memory": {
                        "amount": 1.048576E8,
                        "format": "bytes"
                      },
                      "cpu": {
                        "amount": 0.5,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 5.264900096E7,
                        "format": "bytes"
                      },
                      "cpu": {
                        "amount": 5.37,
                        "format": "cores"
                      }
                    }
                  },
                  "recommendation_terms": {
                    "short_term": {
                      "duration_in_hours": 24.0,
                      "notifications": {
                        "112101": {
                          "type": "info",
                          "message": "Cost Recommendations Available",
                          "code": 112101
                        },
                        "112102": {
                          "type": "info",
                          "message": "Performance Recommendations Available",
                          "code": 112102
                        }
                      },
                      "monitoring_start_time": "2023-04-01T12:00:00.000Z",
                      "recommendation_engines": {
                        "cost": {
                          "pods_count": 7,
                          "confidence_level": 0.0,
                          "config": {
                            "limits": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            }
                          },
                          "variation": {
                            "limits": {
                              "memory": {
                                "amount": 1.449132032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 1.9712180223999997902848E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            }
                          },
                          "notifications": {}
                        },
                        "performance": {
                          "pods_count": 27,
                          "confidence_level": 0.0,
                          "config": {
                            "limits": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 2.497708032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": 0.9299999999999999,
                                "format": "cores"
                              }
                            }
                          },
                          "variation": {
                            "limits": {
                              "memory": {
                                "amount": 1.449132032E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            },
                            "requests": {
                              "memory": {
                                "amount": 1.9712180223999997902848E8,
                                "format": "bytes"
                              },
                              "cpu": {
                                "amount": -4.44,
                                "format": "cores"
                              }
                            }
                          },
                          "notifications": {}
                        }
                      }
                    },
                    "medium_term": {
                      "duration_in_hours": 33.8,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "duration_in_hours": 33.8,
                      "notifications": {
                        "120001": {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation.",
                          "code": 120001
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "version": "1.0",
              "notifications": {
                "120001": {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation.",
                  "code": 120001
                }
              },
              "data": {}
            }
          }
        ]
      }
    ],
    "version": "v2.0",
    "experiment_name": "temp_1"
  }
]
```

</details>

**Request for `namespace` experiment**

`POST /generateRecommendations?experiment_name=?`

example

`curl --location --request POST 'http://<URL>:<PORT>/generateRecommendations?experiment_name=temp_1'`

success status code : 201

**Response for `namespace` Experiment**

The response will contain an array of JSON object with the recommendations for the specified experiment.

When `interval_end_time` is not specified, Kruize will determine the latest timestamp from the specified datasource
(E.g. Prometheus) by checking the latest active container CPU usage.

<details>
<summary><b>Example Response Body</b></summary>

```json
[
  {
    "cluster_name": "test-multiple-import",
    "experiment_type": "namespace",
    "kubernetes_objects": [
      {
        "namespace": "default",
        "containers": [],
        "namespaces": {
          "namespace_name": "default",
          "recommendations": {
            "version": "1.0",
            "notifications": {
              "111000": {
                "type": "info",
                "message": "Recommendations Are Available",
                "code": 111000
              }
            },
            "data": {
              "2024-09-25T09:46:20.000Z": {
                "notifications": {
                  "111101": {
                    "type": "info",
                    "message": "Short Term Recommendations Available",
                    "code": 111101
                  }
                },
                "monitoring_end_time": "2024-09-25T09:46:20.000Z",
                "current": {},
                "recommendation_terms": {
                  "short_term": {
                    "duration_in_hours": 24.0,
                    "notifications": {
                      "112101": {
                        "type": "info",
                        "message": "Cost Recommendations Available",
                        "code": 112101
                      },
                      "112102": {
                        "type": "info",
                        "message": "Performance Recommendations Available",
                        "code": 112102
                      }
                    },
                    "monitoring_start_time": "2024-09-24T09:46:20.000Z",
                    "recommendation_engines": {
                      "cost": {
                        "pods_count": 2,
                        "confidence_level": 0.0,
                        "config": {
                          "limits": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          },
                          "requests": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          }
                        },
                        "variation": {
                          "limits": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          },
                          "requests": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          }
                        },
                        "notifications": {}
                      },
                      "performance": {
                        "pods_count": 2,
                        "confidence_level": 0.0,
                        "config": {
                          "limits": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          },
                          "requests": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          }
                        },
                        "variation": {
                          "limits": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          },
                          "requests": {
                            "memory": {
                              "amount": 1.442955264E9,
                              "format": "bytes"
                            },
                            "cpu": {
                              "amount": 5.834468490017892,
                              "format": "cores"
                            }
                          }
                        },
                        "notifications": {}
                      }
                    }
                  },
                  "medium_term": {
                    "duration_in_hours": 168.0,
                    "notifications": {
                      "120001": {
                        "type": "info",
                        "message": "There is not enough data available to generate a recommendation.",
                        "code": 120001
                      }
                    }
                  },
                  "long_term": {
                    "duration_in_hours": 360.0,
                    "notifications": {
                      "120001": {
                        "type": "info",
                        "message": "There is not enough data available to generate a recommendation.",
                        "code": 120001
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    ],
    "version": "v2.0",
    "experiment_name": "namespace-demo"
  }
]
```

</details>

**Error Responses**

| HTTP Status Code | Description                                                                                        |
|------------------|----------------------------------------------------------------------------------------------------|
| 400              | experiment_name is mandatory.                                                                      |
| 400              | Given timestamp - \" 2023-011-02T00:00:00.000Z \" is not a valid timestamp format.                 |
| 400              | Not Found: experiment_name does not exist: exp_1.                                                  |
| 400              | No metrics available from `2024-01-15T00:00:00.000Z` to `2023-12-31T00:00:00.000Z`.                |
| 400              | The gap between the interval_start_time and interval_end_time must be within a maximum of 15 days! |
| 400              | The Start time should precede the End time!                                                        |                                           |
| 500              | Internal Server Error                                                                              |

