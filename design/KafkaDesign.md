# Kafka Documentation

Kruize has a BulkService feature which designed to streamline the detection, management, and optimization of containerized environments. It relies on the REST APIs without a message broker, which introduces latency when handling multiple parallel requests.
Kafka augments this existing REST API service for faster, asynchronous communication and to ensure seamless handling of recommendations at scale.

Kruize Kafka Producer internally uses the BulkService and publishes recommendations in the form of message in three topics namely, `recommendations-topic`, `error-topic` and `summary-topic`.

## Kafka Flow

1. Currently, Kruize Kafka Module works as a Producer only i.e. to invoke the Kafka Service user needs to hit a REST API POST request with the same input as the one for the BulkService.
2. On receiving the request, BulkService will return the `job_id` back and in the background starts the following tasks:
    - First, does a handshake with the datasource.
    - Using queries, it fetches the list of namespaces, workloads, containers of the connected datasource.
    - Creates experiments, one for each container *alpha release.
    - Triggers `generateRecommendations` for each container.
    - Once the above step returns a success response, Kafka Producer is invoked and the recommendations are pushed into the `recommendations-topic`
    - If at any of the above steps, the service fails be it while fetching metadata, creating experiment or generating recommendations, Kafka Producer is invoked to publish the error response in the `error-topic`.
    - Once all experiments are created, and recommendations are generated, the system marks the `job_id` as "COMPLETED".
    - Once the job is completed, Kafka Producer pushes the summary of all the experiments into the `summary-topic`.

## Specifications
 - User needs to hit a REST API POST request with a Payload as mentioned in the BulkAPI doc. For details, kindly refer to the [BulkAPI](BulkAPI.md) design doc.
 - Kafka enablement flag : `"isKafkaEnabled" : "true",` , needs to be set in the config to enable the usage of Kafka.
 - Kafka needs to be installed locally or in a cluster, and it's corresponding Bootstrap server URL should be added as an ENV.
Example:
 -  `- name: KAFKA_BOOTSTRAP_SERVERS
      value: "<kafka-cluster-svc-name>.<kafka-ns>.svc.cluster.local:9092"`
 - Consumer needs to be subscribed to the `recommendations-topic` to get the recommendations.
 - Subscribing to the `error-topic` and the `summary-topic` is optional

## Examples

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
  ]
}
```

**recommendations-topic:**

```json
{
   "summary": {
      "status": "COMPLETED",
      "job_id": "65603c30-64ee-4bd2-85db-8328425c3b09"
   },
   "experiments": {
      "prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor": {
         "name": "prometheus-1|default|cadvisor|cadvisor(daemonset)|cadvisor",
         "status": "PROCESSED",
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
                                       "2025-01-27T11:28:24.000Z": {
                                          "notifications": {},
                                          "monitoring_end_time": "2025-01-27T11:28:24.000Z",
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
                                                "monitoring_start_time": "2025-01-26T11:28:24.000Z",
                                                "recommendation_engines": {
                                                   "cost": {
                                                      "pods_count": 1,
                                                      "confidence_level": 0,
                                                      "config": {
                                                         "limits": {
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
                                                            }
                                                         },
                                                         "requests": {
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
                                                            }
                                                         }
                                                      },
                                                      "variation": {
                                                         "limits": {
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
                                                            }
                                                         },
                                                         "requests": {
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
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
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
                                                            }
                                                         },
                                                         "requests": {
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
                                                            }
                                                         }
                                                      },
                                                      "variation": {
                                                         "limits": {
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
                                                            }
                                                         },
                                                         "requests": {
                                                            "memory": {
                                                               "amount": 136651161.6,
                                                               "format": "bytes"
                                                            },
                                                            "cpu": {
                                                               "amount": 0.22053964085707395,
                                                               "format": "cores"
                                                            }
                                                         }
                                                      },
                                                      "notifications": {}
                                                   }
                                                },
                                                "plots": {
                                                   "datapoints": 4,
                                                   "plots_data": {
                                                      "2025-01-27T11:28:24.000Z": {
                                                         "cpuUsage": {
                                                            "min": 0.08396913905943296,
                                                            "q1": 0.18669564827597168,
                                                            "median": 0.19308384827591732,
                                                            "q3": 0.2056331399785098,
                                                            "max": 0.22053964085707395,
                                                            "format": "cores"
                                                         },
                                                         "memoryUsage": {
                                                            "min": 104177664,
                                                            "q1": 116555776,
                                                            "median": 121192448,
                                                            "q3": 122347520,
                                                            "max": 123654144,
                                                            "format": "bytes"
                                                         }
                                                      },
                                                      "2025-01-26T23:28:24.000Z": {},
                                                      "2025-01-26T17:28:24.000Z": {},
                                                      "2025-01-27T05:28:24.000Z": {
                                                         "cpuUsage": {
                                                            "min": 0.09123101016771845,
                                                            "q1": 0.1949451069246897,
                                                            "median": 0.19694082169152047,
                                                            "q3": 0.19694082169152047,
                                                            "max": 0.21236305529661514,
                                                            "format": "cores"
                                                         },
                                                         "memoryUsage": {
                                                            "min": 111341568,
                                                            "q1": 121679872,
                                                            "median": 121905152,
                                                            "q3": 121905152,
                                                            "max": 123506688,
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
         },
         "status_history": [
            {
               "status": "UNPROCESSED",
               "timestamp": "2025-01-27T11:18:24.056Z"
            },
            {
               "status": "PROCESSED",
               "timestamp": "2025-01-27T11:18:27.012Z"
            }
         ]
      }
   }
}

```

**error-topic:**

```json
{
   "summary": {
      "status": "IN_PROGRESS",
      "job_id": "c3491f51-2f22-4c3d-af17-3636fac185eb"
   },
   "experiments": {
      "prometheus-1|default|openshift-operator-lifecycle-manager|collect-profiles-28975950(job)|collect-profiles": {
         "name": "prometheus-1|default|openshift-operator-lifecycle-manager|collect-profiles-28975950(job)|collect-profiles",
         "status": "FAILED",
         "apis": {
            "create": {
               "response": {
                  "message": "Not Found: performance_profile does not exist: resource-optimization-local-monitoring",
                  "httpcode": 400,
                  "documentationLink": "",
                  "status": "ERROR"
               },
               "request": {
                  "apiVersion": "v2.0",
                  "experimentName": "prometheus-1|default|openshift-operator-lifecycle-manager|collect-profiles-28975950(job)|collect-profiles",
                  "clusterName": "default",
                  "performanceProfile": "resource-optimization-local-monitoring",
                  "sloInfo": null,
                  "mode": "monitor",
                  "targetCluster": "local",
                  "trialSettings": {
                     "measurement_durationMinutes": "15min",
                     "measurement_durationMinutes_inDouble": 15
                  },
                  "recommendationSettings": {
                     "threshold": 0.1
                  },
                  "datasource": "prometheus-1",
                  "experimentType": "CONTAINER",
                  "status": "IN_PROGRESS",
                  "experiment_id": "acbc6d44b32e94e2ba55968478c31fce3ee62bdb975f616cc3df149d733917ce",
                  "validationData": null,
                  "kubernetesObjects": [
                     {
                        "type": "job",
                        "name": "collect-profiles-28975950",
                        "namespace": "openshift-operator-lifecycle-manager",
                        "containers": [
                           {
                              "container_image_name": "quay.io/openshift-release-dev/ocp-v4.0-art-dev@sha256:82ff155c5e7118a86952f86cba21da8e249f74f0a8f1ac0f2161e2bc1e3b3dbf",
                              "container_name": "collect-profiles",
                              "metrics": null,
                              "recommendations": null
                           }
                        ],
                        "namespaces": null
                     }
                  ],
                  "namespaceExperiment": false,
                  "containerExperiment": true
               }
            },
            "recommendations": {
               "response": null
            }
         },
         "status_history": [
            {
               "status": "UNPROCESSED",
               "timestamp": "2025-02-03T04:32:43.588Z"
            },
            {
               "status": "FAILED",
               "timestamp": "2025-02-03T04:32:43.593Z"
            }
         ]
      }
   }
}
```

**summary-topic:**

```json
{
   "summary": {
      "status": "COMPLETED",
      "total_experiments": 23,
      "processed_experiments": 23,
      "job_id": "54905959-77d4-42ba-8e06-90bb97b823b9",
      "job_start_time": "2024-10-10T06:07:09.066Z",
      "job_end_time": "2024-10-10T06:07:17.471Z"
   }
}

```



