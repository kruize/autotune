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
      "namespace": [
        "openshift-.*"
      ],
      "workload": [],
      "containers": [],
      "labels": {
        "org_id": "ABCOrga",
        "source_id": "ZZZ",
        "cluster_id": "ABG"
      }
    },
    "include": {
      "namespace": [
        "openshift-tuning"
      ],
      "workload": [],
      "containers": [],
      "labels": {
        "org_id": "ABCOrga",
        "source_id": "ZZZ",
        "cluster_id": "ABG"
      }
    }
  },
  "time_range": {
    "start": "2024-11-01T00:00:00.000Z",
    "end": "2024-11-15T23:59:59.000Z"
  },
  "datasource": "Cbank1Xyz",
  "experiment_types": [
    "container",
    "namespace"
  ],
  "webhook": {
    "url": "http://127.0.0.1:8080/webhook"
  }
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

- **webhook:** The `webhook` parameter allows the system to notify an external service or consumer about the completion
  status of
  an experiment-processing job. Once a job is completed, this webhook will be triggered to send an HTTP request to the
  URL defined in the bulk request payload.

### Success Response

- **Status:** 200 OK
- **Response:**

```json
{
  "job_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

### Different payload parameters examples

#### 1. **Request Payload with `time_range` specified:**

This object allows users to specify the duration for which they want to query data and receive recommendations. It
consists of the following fields:

- **`start`**: The starting timestamp of the query duration in ISO 8601 format (`YYYY-MM-DDTHH:mm:ss.sssZ`).
- **`end`**: The ending timestamp of the query duration in ISO 8601 format (`YYYY-MM-DDTHH:mm:ss.sssZ`).

The specified time range determines the period over which the data is analyzed to provide recommendations at the
container or namespace level. Ensure that:

- Both `start` and `end` are valid timestamps.
- The `start` timestamp precedes the `end` timestamp.

#### 2. **Request Payload with `exclude` filter specified:**

- **`exclude`** As shown in the example above, it filters out all namespaces starting with the name `openshift-` . So,
  we'll create experiments and generate recommendations for every namespace except those.

#### 3. **Request Payload with `include` filter specified:**

- **`include`** As shown in the example above, it filters out the namespace `openshift-`. So, we'll create experiments
  and generate recommendations for every namespace starting with the specified name.

#### 3. **Request Payload with both `include` and `exclude` filter specified:**

- **`include`** As shown in the example above, it filters out all namespaces starting with the name `openshift-` but
  includes the `openshift-tuning` one. So, we'll create experiments and generate recommendations for
  the `openshift-tuning` namespace.

### GET Request:

The Bulk Job API allows users to retrieve job status and details based on the `job_id`. By default, it returns only the
summary section. Users can customize the response using the `include` parameter.

### Query Parameters

| Parameter | Type   | Description                                         | Default Value |
|-----------|--------|-----------------------------------------------------|---------------|
| `job_id`  | String | The unique identifier for the job                   | **Required**  |
| `include` | String | Specifies the fields to be included in the response | `summary`     |

```bash
GET /bulk?job_id=123e4567-e89b-12d3-a456-426614174000
```

**Response (JSON):**

```json
{
  "summary": {
    "status": "COMPLETED",
    "total_experiments": 23,
    "processed_experiments": 23,
    "notifications": null,
    "input": {
      "filter": null,
      "time_range": null,
      "datasource": "prometheus-1",
      "webhook": null
    },
    "job_id": "ab803e6a-cb06-436a-9721-45a807a15f13",
    "job_start_time": "2025-02-04T06:39:45.165Z",
    "job_end_time": "2025-02-04T06:40:15.530Z"
  }
}
```

This endpoint allows users to specify which fields to include in the response.

```bash
GET /bulk?job_id=123e4567-e89b-12d3-a456-426614174000&include=summary|job_id|status
```

This request returns only the job_id and status fields.

**Response (JSON):**

```json
{
  "summary": {
    "status": "COMPLETED",
    "job_id": "ab803e6a-cb06-436a-9721-45a807a15f13"
  }
}
```

example :  Job failed

```json
{
  "summary": {
    "status": "FAILED",
    "total_experiments": 0,
    "processed_experiments": 0,
    "notifications": {
      "503": {
        "type": "ERROR",
        "message": "HttpHostConnectException: Unable to connect to the data source. Please try again later. (receive series from Addr: 10.96.192.138:10901 LabelSets: {prometheus=\"monitoring/k8stage\", prometheus_replica=\"prometheus-k8stage-0\"},{prometheus=\"monitoring/k8stage\", prometheus_replica=\"prometheus-k8stage-1\"},{replica=\"thanos-ruler-0\", ruler_cluster=\"\"} MinTime: 1730222825216 MaxTime: 1731412800000: rpc error: code = Unknown desc = receive series from 01JBV2JN5SVN84D3HD5MVSGN3A: load chunks: get range reader: Please reduce your request rate)",
        "code": 503
      }
    },
    "job_id": "270fa4d9-2701-4ca0-b056-74229cc28498",
    "job_start_time": "2024-11-12T15:05:46.362Z",
    "job_end_time": "2024-11-12T15:06:05.301Z"
  }
}

```

```bash
GET /bulk?job_id=123e4567-e89b-12d3-a456-426614174000&include=metadata
```
This API provides details about the cluster's metadata, including the list of namespaces, workloads, and containers it has identified, to generate resource optimization recommendations.

**Response (JSON):**

example 1:

```json
{
  "metadata": {
    "datasources": {
      "prometheus-1": {
        "clusters": {
          "default": {
            "namespaces": {
              "default": {
                "namespace": "default",
                "workloads": null
              },
              "local-path-storage": {
                "namespace": "local-path-storage",
                "workloads": {
                  "local-path-provisioner": {
                    "containers": {
                      "local-path-provisioner": {
                        "container_name": "local-path-provisioner",
                        "container_image_name": "docker.io/kindest/local-path-provisioner:v0.0.22-kind.0"
                      }
                    },
                    "workload_name": "local-path-provisioner",
                    "workload_type": "deployment"
                  }
                }
              },
              "cadvisor": {
                "namespace": "cadvisor",
                "workloads": {
                  "cadvisor": {
                    "containers": {
                      "cadvisor": {
                        "container_name": "cadvisor",
                        "container_image_name": "gcr.io/cadvisor/cadvisor:v0.45.0"
                      }
                    },
                    "workload_name": "cadvisor",
                    "workload_type": "daemonset"
                  }
                }
              },
              "kube-node-lease": {
                "namespace": "kube-node-lease",
                "workloads": null
              },
              "kube-system": {
                "namespace": "kube-system",
                "workloads": {
                  "coredns": {
                    "containers": {
                      "coredns": {
                        "container_name": "coredns",
                        "container_image_name": "k8s.gcr.io/coredns/coredns:v1.8.6"
                      }
                    },
                    "workload_name": "coredns",
                    "workload_type": "deployment"
                  },
                  "kube-proxy": {
                    "containers": {
                      "kube-proxy": {
                        "container_name": "kube-proxy",
                        "container_image_name": "k8s.gcr.io/kube-proxy:v1.24.0"
                      }
                    },
                    "workload_name": "kube-proxy",
                    "workload_type": "daemonset"
                  },
                  "kindnet": {
                    "containers": {
                      "kindnet-cni": {
                        "container_name": "kindnet-cni",
                        "container_image_name": "docker.io/kindest/kindnetd:v20220510-4929dd75"
                      }
                    },
                    "workload_name": "kindnet",
                    "workload_type": "daemonset"
                  }
                }
              },
              "monitoring": {
                "namespace": "monitoring",
                "workloads": {
                  "kube-state-metrics": {
                    "containers": {
                      "kube-state-metrics": {
                        "container_name": "kube-state-metrics",
                        "container_image_name": "registry.k8s.io/kube-state-metrics/kube-state-metrics:v2.9.2"
                      },
                      "kube-rbac-proxy-self": {
                        "container_name": "kube-rbac-proxy-self",
                        "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.14.2"
                      },
                      "kube-rbac-proxy-main": {
                        "container_name": "kube-rbac-proxy-main",
                        "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.14.2"
                      }
                    },
                    "workload_name": "kube-state-metrics",
                    "workload_type": "deployment"
                  },
                  "node-exporter": {
                    "containers": {
                      "node-exporter": {
                        "container_name": "node-exporter",
                        "container_image_name": "quay.io/prometheus/node-exporter:v1.6.1"
                      },
                      "kube-rbac-proxy": {
                        "container_name": "kube-rbac-proxy",
                        "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.14.2"
                      }
                    },
                    "workload_name": "node-exporter",
                    "workload_type": "daemonset"
                  },
                  "alertmanager-main": {
                    "containers": {
                      "config-reloader": {
                        "container_name": "config-reloader",
                        "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.67.1"
                      },
                      "alertmanager": {
                        "container_name": "alertmanager",
                        "container_image_name": "quay.io/prometheus/alertmanager:v0.26.0"
                      }
                    },
                    "workload_name": "alertmanager-main",
                    "workload_type": "statefulset"
                  },
                  "prometheus-adapter": {
                    "containers": {
                      "prometheus-adapter": {
                        "container_name": "prometheus-adapter",
                        "container_image_name": "registry.k8s.io/prometheus-adapter/prometheus-adapter:v0.11.1"
                      }
                    },
                    "workload_name": "prometheus-adapter",
                    "workload_type": "deployment"
                  },
                  "grafana": {
                    "containers": {
                      "grafana": {
                        "container_name": "grafana",
                        "container_image_name": "docker.io/grafana/grafana:9.5.3"
                      }
                    },
                    "workload_name": "grafana",
                    "workload_type": "deployment"
                  },
                  "kruize": {
                    "containers": {
                      "kruize": {
                        "container_name": "kruize",
                        "container_image_name": "quay.io/vinakuma/autotune_operator:shk4"
                      }
                    },
                    "workload_name": "kruize",
                    "workload_type": "deployment"
                  },
                  "kruize-db-deployment": {
                    "containers": {
                      "kruize-db": {
                        "container_name": "kruize-db",
                        "container_image_name": "quay.io/kruizehub/postgres:15.2"
                      }
                    },
                    "workload_name": "kruize-db-deployment",
                    "workload_type": "deployment"
                  },
                  "prometheus-k8s": {
                    "containers": {
                      "config-reloader": {
                        "container_name": "config-reloader",
                        "container_image_name": "quay.io/prometheus-operator/prometheus-config-reloader:v0.67.1"
                      },
                      "prometheus": {
                        "container_name": "prometheus",
                        "container_image_name": "quay.io/prometheus/prometheus:v2.46.0"
                      }
                    },
                    "workload_name": "prometheus-k8s",
                    "workload_type": "statefulset"
                  },
                  "blackbox-exporter": {
                    "containers": {
                      "kube-rbac-proxy": {
                        "container_name": "kube-rbac-proxy",
                        "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.14.2"
                      },
                      "module-configmap-reloader": {
                        "container_name": "module-configmap-reloader",
                        "container_image_name": "docker.io/jimmidyson/configmap-reload:v0.5.0"
                      },
                      "blackbox-exporter": {
                        "container_name": "blackbox-exporter",
                        "container_image_name": "quay.io/prometheus/blackbox-exporter:v0.24.0"
                      }
                    },
                    "workload_name": "blackbox-exporter",
                    "workload_type": "deployment"
                  },
                  "prometheus-operator": {
                    "containers": {
                      "kube-rbac-proxy": {
                        "container_name": "kube-rbac-proxy",
                        "container_image_name": "quay.io/brancz/kube-rbac-proxy:v0.14.2"
                      },
                      "prometheus-operator": {
                        "container_name": "prometheus-operator",
                        "container_image_name": "quay.io/prometheus-operator/prometheus-operator:v0.67.1"
                      }
                    },
                    "workload_name": "prometheus-operator",
                    "workload_type": "deployment"
                  }
                }
              },
              "kube-public": {
                "namespace": "kube-public",
                "workloads": null
              }
            },
            "dataSourceClusterName": "default",
            "cluster_name": "default"
          }
        },
        "datasource_name": "prometheus-1"
      }
    }
  }
}
```

```bash
GET /bulk?job_id=ab803e6a-cb06-436a-9721-45a807a15f13&include=experiments|apis|recommendations|response&experiment_name=prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor
```

This API allows viewing recommendations for all containers available in the cluster. Additionally, results can be filtered using experiment_name and the include parameter (experiments|apis|recommendations|response).

**Response (JSON):**

example 1:

```json
{
  "experiments": {
    "prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor": {
      "apis": {
        "recommendations": {
          "response": [
            {
              "cluster_name": "default",
              "experiment_type": "container",
              "kubernetes_objects": [
                {
                  "type": "daemonset",
                  "name": "cadvisor",
                  "namespace": "cadvisor",
                  "containers": [
                    {
                      "container_image_name": "gcr.io/cadvisor/cadvisor:v0.45.0",
                      "container_name": "cadvisor",
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
                          "2025-02-04T06:49:47.000Z": {
                            "notifications": {
                              "111101": {
                                "type": "info",
                                "message": "Short Term Recommendations Available",
                                "code": 111101
                              },
                              "223001": {
                                "type": "error",
                                "message": "Amount field is missing in the CPU Section",
                                "code": 223001
                              },
                              "224001": {
                                "type": "error",
                                "message": "Amount field is missing in the Memory Section",
                                "code": 224001
                              },
                              "423001": {
                                "type": "warning",
                                "message": "CPU Limit Not Set",
                                "code": 423001
                              },
                              "523001": {
                                "type": "critical",
                                "message": "CPU Request Not Set",
                                "code": 523001
                              },
                              "524001": {
                                "type": "critical",
                                "message": "Memory Request Not Set",
                                "code": 524001
                              },
                              "524002": {
                                "type": "critical",
                                "message": "Memory Limit Not Set",
                                "code": 524002
                              }
                            },
                            "monitoring_end_time": "2025-02-04T06:49:47.000Z",
                            "current": {},
                            "recommendation_terms": {
                              "short_term": {
                                "duration_in_hours": 24,
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
                                "monitoring_start_time": "2025-02-03T06:49:47.000Z",
                                "recommendation_engines": {
                                  "cost": {
                                    "pods_count": 1,
                                    "confidence_level": 0,
                                    "config": {
                                      "limits": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      },
                                      "requests": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      }
                                    },
                                    "variation": {
                                      "limits": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      },
                                      "requests": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      }
                                    },
                                    "notifications": {}
                                  },
                                  "performance": {
                                    "pods_count": 1,
                                    "confidence_level": 0,
                                    "config": {
                                      "limits": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      },
                                      "requests": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      }
                                    },
                                    "variation": {
                                      "limits": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      },
                                      "requests": {
                                        "cpu": {
                                          "amount": 0.247921867815971,
                                          "format": "cores"
                                        },
                                        "memory": {
                                          "amount": 130137497.6,
                                          "format": "bytes"
                                        }
                                      }
                                    },
                                    "notifications": {}
                                  }
                                },
                                "plots": {
                                  "datapoints": 4,
                                  "plots_data": {
                                    "2025-02-03T12:49:47.000Z": {
                                      "cpuUsage": {
                                        "min": 0.08472884578584813,
                                        "q1": 0.18897737356331987,
                                        "median": 0.19580560804589087,
                                        "q3": 0.2053744206893801,
                                        "max": 0.23567110804615787,
                                        "format": "cores"
                                      },
                                      "memoryUsage": {
                                        "min": 103182336,
                                        "q1": 114761728,
                                        "median": 115486720,
                                        "q3": 116727808,
                                        "max": 117936128,
                                        "format": "bytes"
                                      }
                                    },
                                    "2025-02-04T00:49:47.000Z": {},
                                    "2025-02-04T06:49:47.000Z": {
                                      "cpuUsage": {
                                        "min": 0.0938850379308944,
                                        "q1": 0.19299167060082456,
                                        "median": 0.2064043747130952,
                                        "q3": 0.22625860459739597,
                                        "max": 0.2406954793100685,
                                        "format": "cores"
                                      },
                                      "memoryUsage": {
                                        "min": 105275392,
                                        "q1": 114790400,
                                        "median": 115699712,
                                        "q3": 116346880,
                                        "max": 117125120,
                                        "format": "bytes"
                                      }
                                    },
                                    "2025-02-03T18:49:47.000Z": {
                                      "cpuUsage": {
                                        "min": 0.0768995104563881,
                                        "q1": 0.17573648390785696,
                                        "median": 0.18844624252897224,
                                        "q3": 0.20736097471262313,
                                        "max": 0.247921867815971,
                                        "format": "cores"
                                      },
                                      "memoryUsage": {
                                        "min": 103739392,
                                        "q1": 112893952,
                                        "median": 113491968,
                                        "q3": 114520064,
                                        "max": 116412416,
                                        "format": "bytes"
                                      }
                                    }
                                  }
                                }
                              },
                              "medium_term": {
                                "duration_in_hours": 168,
                                "notifications": {
                                  "120001": {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation.",
                                    "code": 120001
                                  }
                                }
                              },
                              "long_term": {
                                "duration_in_hours": 360,
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
                  ]
                }
              ],
              "version": "v2.0",
              "experiment_name": "prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor"
            }
          ]
        }
      }
    }
  }
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

- **experiments**:
    - **Type**: `Array `
    - **Description**: Array of experiment objects, each containing details about individual experiments.

    - Each object in the `experiments` array has the following structure:

  | Field             | Type     | Description                                                                         |
  |-------------------|----------|-------------------------------------------------------------------------------------|
  | `name`            | `string` | Name of the experiment, typically indicating a service name and deployment context. |
  | `notifications`   | `object` | Notifications specific to this experiment (if any).                                 |
  | `recommendations` | `object` | Recommendation status and notifications specific to this experiment.                |

  #### Recommendation Object

  The `recommendations` field within each experiment provides information about recommendation processing status and
  errors (if any).

  | Field           | Type     | Description                                                                                      |
  |-----------------|----------|--------------------------------------------------------------------------------------------------|
  | `status`        | `string` | Status of the recommendation (e.g., `"unprocessed"`, `"processed"`, `"processing"`, `"failed"`). |
  | `notifications` | `object` | Notifications related to recommendation processing.                                              |

  #### Notification Object

  Both the `notifications` and `recommendations.notifications` fields may contain error messages or warnings as follows:

  | Field                   | Type         | Description                                                                |
  |-------------------------|--------------|----------------------------------------------------------------------------|
  | `type`                  | `string`     | Type of notification (e.g., `"info"`,`"error"`, `"warning"`).              |
  | `message`               | `string`     | Description of the notification message.                                   |
  | `code`                  | `integer`    | HTTP-like code indicating the type of error (e.g., `400` for bad request). |

- **job_id**:
    - **Type**: `String`
    - **Description**: Unique identifier for the job.

- **job_start_time**:
    - **Type**: `String (ISO 8601 format)`
    - **Description**: Start timestamp of the job.

- **job_end_time**:
    - **Type**: `String (ISO 8601 format) or null`
    - **Description**: End timestamp of the job. If the job is still in progress, this will be `null`.

- **webhook**:
    - **Type**: `Object`
    - **Description**: An object that provides details about the webhook status and any errors encountered during the
      webhook invocation.

    - The `webhook` parameter allows the system to notify an external service or consumer about the completion status of
      an experiment-processing job. Once a job is completed, this webhook will be triggered to send an HTTP request to
      the URL defined in the bulk request payload.
      This notification mechanism is essential for systems that require real-time updates about the job's processing
      status, enabling consumers to take immediate follow-up actions. For example, an external analytics dashboard, a
      monitoring service, or a message queue like Kafka can listen for these webhook calls to further process or log the
      job completion data.

- **experiments**:
  - **Type**: `dict`
  - **Description**: Contains the experiment data for each cluster. Each entry is keyed by a unique experiment identifier.

  - Each experiment contains a key called apis, under which there are two APIs. One API is responsible for creating the experiment, capturing its API request and response, and generating recommendations. The request for the "Create" API and the generated recommendations are also captured. For further details on the "Create" request, refer to the documentation [here](MonitoringModeAPI.md), and for the "Generate Recommendations" API documentation, refer to [here](MonitoringModeAPI.md). 


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

- We can also configure same using environment variable
    - examples
    ```shell
        export datasource='[{ \"name\": \"prometheus-prod\",\"provider\": \"prometheus\",\"serviceName\": \"\",\"namespace\": \"ros-prod\",\"url\": \"http://0.0.0.0:8080\",\"authentication\": {\"type\": \"bearer\",\"credentials\": {\"token\" : \"token_key\"}}}]'
    ```
  or
    ```shell
        export datasource='[{ \"name\": \"prometheus-prod\",\"provider\": \"prometheus\",\"serviceName\": \"\",\"namespace\": \"ros-prod\",\"url\": \"http://0.0.0.0:8080\",\"authentication\": {\"type\": \"bearer\",\"credentials\": {\"token\" : \"token_key\"}}}]'
    ```

## Bulk Internal API's

The Bulk API internally invokes multiple APIs, such as createExperiment and generateRecommendations.
These APIs can be configured using environment variables with the following default format:
http://<appname>.<namespace>.svc.cluster.local:<appnamePort>/generateRecommendations?experiment_name=%s
http://<appname>.<namespace>.svc.cluster.local:<appnamePort>/createExperiment
example

```shell
"recommendationsURL" : "http://kruize.openshift-tuning.svc.cluster.local:8080/generateRecommendations?experiment_name=%s"
"experimentsURL" : "http://kruize.openshift-tuning.svc.cluster.local:8080/createExperiment"
```

## Limits

- **Default Limit:** Currently, the Bulk service supports only **1000 experiments** by default.
- **Increasing the Limit:** You can increase this limit by setting the environment variable `bulkapilimit`.
- **Job Failure on Exceeding Limit:** If the number of experiments exceeds the set limit, the job will fail.

## Bulk API Threads

- **Control Mechanism:** The number of threads used for bulk API operations can be controlled using the environment
  variable `bulkThreadPoolSize`.

## Cache bulk job details in memory or DB

- The default value for the `testUseOnlycacheJobInMemory` environment variable is set to false, meaning bulk summary job details are stored in the `kruize_bulkjobs` database table. If set to true, the details are stored in memory instead. However, this data will be lost if the pod restarts and will not be available in other pod replicas.
  p.s. This environment variable is intended solely for development and internal testing purposes and has no relevance for Bulk API consumers. 

## Control Data Attributes Saved in kruize_bulkjobs.experiments

- The experiments column in the `kruize_bulkjobs` table can be controlled using the environment variable `jobFilterToDB`. Since the data volume can be large and some information may not need to be stored, this variable helps manage what gets saved. By default, its value is "experiments|status|apis|create|response", meaning the following example data will be stored in the database.
- 
```json
{
  "experiments": {
    "prometheus-1|default|monitoring|kruize(deployment)|kruize": {
      "apis": {
        "create": {
          "response": {
            "status": "SUCCESS",
            "message": "Experiment registered successfully with Kruize. View registered experiments at /listExperiments",
            "httpcode": 201,
            "documentationLink": ""
          }
        }
      },
      "status": "PROCESSED"
    },
    "prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor": {
      "apis": {
        "create": {
          "response": {
            "status": "SUCCESS",
            "message": "Experiment registered successfully with Kruize. View registered experiments at /listExperiments",
            "httpcode": 201,
            "documentationLink": ""
          }
        }
      },
      "status": "PROCESSED"
    }
  }
}
```

## Experiment Name Format Configuration

- **experimentNameFormat:** The `experimentNameFormat` environment variable is used to define the format for experiment
  names. For example, if the
  experiment name should follow the structure:

```
org_id|source_id|cluster_id|namespace|k8s_object_type|k8s_object_name
```

then set or define the `experimentNameFormat` as follows:

```
"experimentNameFormat": "%label:org_id%|%label:source_id%|%label:cluster_id%|%namespace%|%workloadtype%|%workloadname%|%containername%"
```

When making a /bulk call, ensure the label values used in the experiment name format are passed in the payload's filter
and include sections, matching the format above.

```json
{
  "filter": {
    "exclude": {
      "namespace": [],
      "workload": [],
      "containers": [],
      "labels": {
        "key1": "value1",
        "key2": "value2"
      }
    },
    "include": {
      "namespace": [],
      "workload": [],
      "containers": [],
      "labels": {
        "org_id": "ABCOrga",
        "source_id": "ZZZ",
        "cluster_id": "ABG"
      }
    }
  }
}
```

With the above configuration, the experiment name generated will be:

ABCOrga|ZZZ|ABG|kube-system|deployment|coredns|coredns

If the filter is not specified, it will display as Unknown.

```
ABCOrga|ZZZ|unknowncluster_id|prometheus-1|default|kube-system|coredns(deployment)|coredns
```

**Note**:Specifying labels in envirnoment varable `experimentNameFormat` is optional and flexible; there can be any
number of labels, or none at all. Here are some examples:

- "%datasource%|%clustername%|%namespace%|%workloadname%(%workloadtype%)|%containername%"    -> Default
- "%label:org_id%|%label:source_id%|%label:cluster_id%|%namespace%|%workloadtype%|%workloadname%|%containername%"
- "%label:org_id%|%namespace%|%workloadtype%|%workloadname%|%containername%"
- "%label:org_id%|%label:cluster_id%|%namespace%|%workloadtype%|%workloadname%"

