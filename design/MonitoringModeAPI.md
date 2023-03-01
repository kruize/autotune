# Remote Monitoring Mode

This article describes how to quickly get started with the Remote Monitoring Mode use case REST API using curl command.
Documentation still in progress stay tuned.

**Note :**  The ISO 8601 standard underpins all timestamp formats. An example of a valid timestamp in this format is
2022-01-23T18:25:43.511Z, which represents January 23, 2022, at 18:25:43.511 UTC.

## CreateExperiment

This is quick guide instructions to create experiments using input JSON as follows. For a more detailed guide,
see [Create Experiment](/design/CreateExperiment.md)

**Request**
`POST /createExperiment`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createExperiment`

```
[
  {
    "version": "1.0",
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
    "cluster_name": "cluster-one-division-bell",
    "performance_profile": "resource-optimization-openshift",
    "mode": "monitor",
    "target_cluster": "remote",
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
    }
  }
]
```

**Response**

```
{
    "message": "Experiment registered successfully with Autotune. View registered experiments at /listExperiments",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

## Update Metric Results

Update metric results using input JSON as follows. For a more detailed guide,
see [Update results](/design/UpdateResults.md)

**Request**
`POST /updateResults`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/updateResults`

```
[
  {
    "version": "1.0",
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
    "start_timestamp": "2022-01-23T18:25:43.511Z",
    "end_timestamp": "2022-01-23T18:25:43.511Z",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment",
        "namespace": "default",
        "containers": [
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-0",
            "metrics": [
              {
                "name": "cpuRequest",
                "results": {
                  "value": 1.1,
                  "format": "cores",
                  "aggregation_info": {
                    "sum": 4.4,
                    "avg": 1.1,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "cpuLimit",
                "results": {
                  "value": 0.5,
                  "format": "cores",
                  "aggregation_info": {
                    "sum": 2.0,
                    "avg": 0.5,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "cpuUsage",
                "results": {
                  "value": 0.12,
                  "format": "cores",
                  "aggregation_info": {
                    "min": 0.14,
                    "max": 0.84,
                    "sum": 0.84,
                    "avg": 0.12,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "cpuThrottle",
                "results": {
                  "value": 0.045,
                  "format": "cores",
                  "aggregation_info": {
                    "sum": 0.19,
                    "max": 0.09,
                    "avg": 0.045,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "memoryRequest",
                "results": {
                  "value": 50.12,
                  "format": "MiB",
                  "aggregation_info": {
                    "sum": 250.85,
                    "avg": 50.21,
                    "format": "MiB"
                  }
                }
              },
              {
                "name": "memoryLimit",
                "results": {
                  "value": 100,
                  "format": "MiB",
                  "aggregation_info": {
                    "sum": 500,
                    "avg": 100,
                    "format": "MiB"
                  }
                }
              },
              {
                "name": "memoryUsage",
                "results": {
                  "value": 40.1,
                  "format": "MiB",
                  "aggregation_info": {
                    "min": 50.6,
                    "max": 198.50,
                    "sum": 198.50,
                    "avg": 40.1,
                    "format": "MiB"
                  }
                }
              },
              {
                "name": "memoryRSS",
                "results": {
                  "aggregation_info": {
                    "min": 50.6,
                    "max": 123.6,
                    "sum": 123.6,
                    "avg": 31.91,
                    "format": "MiB"
                  }
                }
              }
            ]
          },
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "metrics": [
              {
                "name": "cpuRequest",
                "results": {
                  "value": 1.1,
                  "format": "cores",
                  "aggregation_info": {
                    "sum": 4.4,
                    "avg": 1.1,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "cpuLimit",
                "results": {
                  "value": 0.5,
                  "format": "cores",
                  "aggregation_info": {
                    "sum": 2.0,
                    "avg": 0.5,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "cpuUsage",
                "results": {
                  "value": 0.12,
                  "format": "cores",
                  "aggregation_info": {
                    "min": 0.14,
                    "max": 0.84,
                    "sum": 0.84,
                    "avg": 0.12,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "cpuThrottle",
                "results": {
                  "value": 0.045,
                  "format": "cores",
                  "aggregation_info": {
                    "sum": 0.19,
                    "max": 0.09,
                    "avg": 0.045,
                    "format": "cores"
                  }
                }
              },
              {
                "name": "memoryRequest",
                "results": {
                  "value": 50.12,
                  "format": "MiB",
                  "aggregation_info": {
                    "sum": 250.85,
                    "avg": 50.21,
                    "format": "MiB"
                  }
                }
              },
              {
                "name": "memoryLimit",
                "results": {
                  "value": 100,
                  "format": "MiB",
                  "aggregation_info": {
                    "sum": 500,
                    "avg": 100,
                    "format": "MiB"
                  }
                }
              },
              {
                "name": "memoryUsage",
                "results": {
                  "value": 40.1,
                  "format": "MiB",
                  "aggregation_info": {
                    "min": 50.6,
                    "max": 198.50,
                    "sum": 198.50,
                    "avg": 40.1,
                    "format": "MiB"
                  }
                }
              },
              {
                "name": "memoryRSS",
                "results": {
                  "aggregation_info": {
                    "min": 50.6,
                    "max": 123.6,
                    "sum": 123.6,
                    "avg": 31.91,
                    "format": "MiB"
                  }
                }
              }
            ]
          }
        ]
      }
    ]
  }
]

```

**Response**

```
{
    "message": "Updated metrics results successfully with Autotune. View update results at /listExperiments",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

## Recommendations

List recommendations output JSON as follows. Some parameters like CPU limit , ENV are optional.

**Attributes:**

`experiment_name` - Passing Experiment Name as the parameter to the API returns the recommendation of the particular experiment if it exists

**Request with experiment name parameter**

`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations?experiment_name=<experiment_name>`

**Response for experiment name - `quarkus-resteasy-autotune-min-http-response-time-db`**
```
[
    {
        "kubernetes_objects": [
            {
                "type": "deployment",
                "name": "tfb-qrh-deployment",
                "namespace": "default",
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                        "container_name": "tfb-server-1",
                        "recommendation": {
                            "2022-01-23T18:25:43.511Z": {
                                "short_term": {
                                    "monitoring_start_time": "2022-01-22T18:25:43.511Z",
                                    "monitoring_end_time": "2022-01-23T18:25:43.511Z",
                                    "duration_in_hours": 0.0,
                                    "pods_count": 0,
                                    "confidence_level": 0.0,
                                    "config": {
                                        "limits": {
                                            "memory": {
                                                "amount": 982.5997506234414,
                                                "format": "MiB"
                                            },
                                            "cpu": {
                                                "amount": 6.51,
                                                "format": "cores"
                                            }
                                        },
                                        "requests": {
                                            "memory": {
                                                "amount": 123.6,
                                                "format": "MiB"
                                            },
                                            "cpu": {
                                                "amount": 1.03,
                                                "format": "cores"
                                            }
                                        }
                                    }
                                },
                                "medium_term": {
                                    "pods_count": 0,
                                    "confidence_level": 0.0,
                                    "error_msg": "There is not enough data available to generate a recommendation."
                                },
                                "long_term": {
                                    "pods_count": 0,
                                    "confidence_level": 0.0,
                                    "error_msg": "There is not enough data available to generate a recommendation."
                                }
                            }
                        }
                    },
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": "tfb-server-0",
                        "recommendation": {
                            "2022-01-23T18:25:43.511Z": {
                                "short_term": {
                                    "monitoring_start_time": "2022-01-22T18:25:43.511Z",
                                    "monitoring_end_time": "2022-01-23T18:25:43.511Z",
                                    "duration_in_hours": 0.0,
                                    "pods_count": 0,
                                    "confidence_level": 0.0,
                                    "config": {
                                        "limits": {
                                            "memory": {
                                                "amount": 982.5997506234414,
                                                "format": "MiB"
                                            },
                                            "cpu": {
                                                "amount": 6.51,
                                                "format": "cores"
                                            }
                                        },
                                        "requests": {
                                            "memory": {
                                                "amount": 123.6,
                                                "format": "MiB"
                                            },
                                            "cpu": {
                                                "amount": 1.03,
                                                "format": "cores"
                                            }
                                        }
                                    }
                                },
                                "medium_term": {
                                    "pods_count": 0,
                                    "confidence_level": 0.0,
                                    "error_msg": "There is not enough data available to generate a recommendation."
                                },
                                "long_term": {
                                    "pods_count": 0,
                                    "confidence_level": 0.0,
                                    "error_msg": "There is not enough data available to generate a recommendation."
                                }
                            }
                        }
                    }
                ]
            }
        ],
        "version": "1.0",
        "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db"
    }
]
```

**Request without Parameter**

`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations`

If no parameter is passed API returns all the recommendations available.

**Response**

```
[
  {
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment",
        "namespace": "default",
        "containers": [
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendation": {
              "2022-01-23T18:25:43.511Z": {
                "short_term": {
                  "monitoring_start_time": "2022-01-22T18:25:43.511Z",
                  "monitoring_end_time": "2022-01-23T18:25:43.511Z",
                  "duration_in_hours": 0.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              }
            }
          },
          {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendation": {
              "2022-01-23T18:25:43.511Z": {
                "short_term": {
                  "monitoring_start_time": "2022-01-22T18:25:43.511Z",
                  "monitoring_end_time": "2022-01-23T18:25:43.511Z",
                  "duration_in_hours": 0.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              }
            }
          }
        ]
      }
    ],
    "version": "1.0",
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db"
  },
  {
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": [
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendation": {
              "2022-12-20T17:55:05.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T17:55:05.000Z",
                  "monitoring_end_time": "2022-12-20T17:55:05.000Z",
                  "duration_in_hours": 2.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-21T00:10:16.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-20T00:10:16.000Z",
                  "monitoring_end_time": "2022-12-21T00:10:16.000Z",
                  "duration_in_hours": 8.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T20:25:10.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T20:25:10.000Z",
                  "monitoring_end_time": "2022-12-20T20:25:10.000Z",
                  "duration_in_hours": 4.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T16:25:03.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T16:25:03.000Z",
                  "monitoring_end_time": "2022-12-20T16:25:03.000Z",
                  "duration_in_hours": 0.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T18:40:07.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T18:40:07.000Z",
                  "monitoring_end_time": "2022-12-20T18:40:07.000Z",
                  "duration_in_hours": 3.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T22:25:13.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T22:25:13.000Z",
                  "monitoring_end_time": "2022-12-20T22:25:13.000Z",
                  "duration_in_hours": 6.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T23:55:15.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T23:55:15.000Z",
                  "monitoring_end_time": "2022-12-20T23:55:15.000Z",
                  "duration_in_hours": 8.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T20:55:11.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T20:55:11.000Z",
                  "monitoring_end_time": "2022-12-20T20:55:11.000Z",
                  "duration_in_hours": 5.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T16:40:04.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T16:40:04.000Z",
                  "monitoring_end_time": "2022-12-20T16:40:04.000Z",
                  "duration_in_hours": 1.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T21:55:12.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T21:55:12.000Z",
                  "monitoring_end_time": "2022-12-20T21:55:12.000Z",
                  "duration_in_hours": 6.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T18:10:06.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T18:10:06.000Z",
                  "monitoring_end_time": "2022-12-20T18:10:06.000Z",
                  "duration_in_hours": 2.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T23:25:14.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T23:25:14.000Z",
                  "monitoring_end_time": "2022-12-20T23:25:14.000Z",
                  "duration_in_hours": 7.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T17:40:05.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T17:40:05.000Z",
                  "monitoring_end_time": "2022-12-20T17:40:05.000Z",
                  "duration_in_hours": 2.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T22:55:13.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T22:55:13.000Z",
                  "monitoring_end_time": "2022-12-20T22:55:13.000Z",
                  "duration_in_hours": 7.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T20:40:10.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T20:40:10.000Z",
                  "monitoring_end_time": "2022-12-20T20:40:10.000Z",
                  "duration_in_hours": 5.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T19:55:09.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T19:55:09.000Z",
                  "monitoring_end_time": "2022-12-20T19:55:09.000Z",
                  "duration_in_hours": 4.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T18:25:06.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T18:25:06.000Z",
                  "monitoring_end_time": "2022-12-20T18:25:06.000Z",
                  "duration_in_hours": 2.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T18:55:07.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T18:55:07.000Z",
                  "monitoring_end_time": "2022-12-20T18:55:07.000Z",
                  "duration_in_hours": 3.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T21:40:12.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T21:40:12.000Z",
                  "monitoring_end_time": "2022-12-20T21:40:12.000Z",
                  "duration_in_hours": 6.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T19:40:09.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T19:40:09.000Z",
                  "monitoring_end_time": "2022-12-20T19:40:09.000Z",
                  "duration_in_hours": 4.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T21:10:11.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T21:10:11.000Z",
                  "monitoring_end_time": "2022-12-20T21:10:11.000Z",
                  "duration_in_hours": 5.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T15:55:03.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T15:55:03.000Z",
                  "monitoring_end_time": "2022-12-20T15:55:03.000Z",
                  "duration_in_hours": 0.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T17:10:04.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T17:10:04.000Z",
                  "monitoring_end_time": "2022-12-20T17:10:04.000Z",
                  "duration_in_hours": 1.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T16:55:04.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T16:55:04.000Z",
                  "monitoring_end_time": "2022-12-20T16:55:04.000Z",
                  "duration_in_hours": 1.25,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T21:25:11.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T21:25:11.000Z",
                  "monitoring_end_time": "2022-12-20T21:25:11.000Z",
                  "duration_in_hours": 5.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T16:10:03.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T16:10:03.000Z",
                  "monitoring_end_time": "2022-12-20T16:10:03.000Z",
                  "duration_in_hours": 0.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T19:25:09.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T19:25:09.000Z",
                  "monitoring_end_time": "2022-12-20T19:25:09.000Z",
                  "duration_in_hours": 3.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-21T00:40:17.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-20T00:40:17.000Z",
                  "monitoring_end_time": "2022-12-21T00:40:17.000Z",
                  "duration_in_hours": 9.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T22:10:12.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T22:10:12.000Z",
                  "monitoring_end_time": "2022-12-20T22:10:12.000Z",
                  "duration_in_hours": 6.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T19:10:08.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T19:10:08.000Z",
                  "monitoring_end_time": "2022-12-20T19:10:08.000Z",
                  "duration_in_hours": 3.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-21T00:25:16.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-20T00:25:16.000Z",
                  "monitoring_end_time": "2022-12-21T00:25:16.000Z",
                  "duration_in_hours": 8.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T23:40:15.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T23:40:15.000Z",
                  "monitoring_end_time": "2022-12-20T23:40:15.000Z",
                  "duration_in_hours": 8.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T23:10:14.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T23:10:14.000Z",
                  "monitoring_end_time": "2022-12-20T23:10:14.000Z",
                  "duration_in_hours": 7.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T17:25:05.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T17:25:05.000Z",
                  "monitoring_end_time": "2022-12-20T17:25:05.000Z",
                  "duration_in_hours": 1.75,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T22:40:13.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T22:40:13.000Z",
                  "monitoring_end_time": "2022-12-20T22:40:13.000Z",
                  "duration_in_hours": 7.0,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              },
              "2022-12-20T20:10:10.000Z": {
                "short_term": {
                  "monitoring_start_time": "2022-12-19T20:10:10.000Z",
                  "monitoring_end_time": "2022-12-20T20:10:10.000Z",
                  "duration_in_hours": 4.5,
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "config": {
                    "limits": {
                      "memory": {
                        "amount": 982.5997506234414,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 6.51,
                        "format": "cores"
                      }
                    },
                    "requests": {
                      "memory": {
                        "amount": 123.6,
                        "format": "MiB"
                      },
                      "cpu": {
                        "amount": 1.03,
                        "format": "cores"
                      }
                    }
                  }
                },
                "medium_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                },
                "long_term": {
                  "pods_count": 0,
                  "confidence_level": 0.0,
                  "error_msg": "There is not enough data available to generate a recommendation."
                }
              }
            }
          },
          {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        ]
      }
    ],
    "version": "1.0",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0"
  }
]
```
