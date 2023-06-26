This doc has the sample list Recommendation API response for Notification Code.

**List of Notification codes:**

|   CODE   |   TYPE   |                                    MESSAGE                                    |
|:--------:|:-------:|:-----------------------------------------------------------------------------:|
| 112101   |  info   |                   Duration Based Recommendations Available                    |
| 120001   |  info   |        There is not enough data available to generate a recommendation        |
| 323001   | notice  | CPU usage is less than one millicore, No CPU Recommendations can be generated |
| 323002   | notice  |          CPU usage is zero, No CPU Recommendations can be generated           |
| 323003   | notice  |    CPU metrics are not available, No CPU Recommendations can be generated     |
| 324001   | notice  |       Memory usage is zero, No Memory Recommendations can be generated        |
| 324002   | notice  | Memory metrics are not available, No Memory Recommendations can be generated  |
| 523001   | critical |                              CPU Request Not Set                              |
| 423001   | warning |                               CPU Limit Not Set                               |
| 524001   | critical |                            Memory Request Not Set                             |
| 524002   | critical |                             Memory Limit Not Set                              |


**_Code: 112101
Message: Duration Based Recommendations Available._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T01:02:46.356Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T01:02:46.356Z",
                                        "monitoring_end_time": "2023-06-15T01:02:46.356Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 1,
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "gorgeous-dinakar"
}
```

**_Code: 120001
Message: There is not enough data available to generate a recommendation._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
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
    "version": "1.0",
    "experiment_name": "gorgeous-dinakar"
}
```

**_Code: 323001
Message: CPU usage is less than one millicore, No CPU Recommendations can be generated._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T01:00:26.200Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T01:00:26.200Z",
                                        "monitoring_end_time": "2023-06-15T01:00:26.200Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 1,
                                        "confidence_level": 0.0,
                                        "current": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 646.35,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 0.5,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 776.47,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 0.5,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "config": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 1198.296,
                                                    "format": "MiB"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 1198.296,
                                                    "format": "MiB"
                                                }
                                            }
                                        },
                                        "variation": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 551.946,
                                                    "format": "MiB"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 421.826,
                                                    "format": "MiB"
                                                }
                                            }
                                        },
                                        "notifications": {
                                            "323001": {
                                                "type": "notice",
                                                "message": "CPU Usage is less than a millicore, No CPU Recommendations can be generated",
                                                "code": 323001
                                            }
                                        }
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "graceful-dinakar"
}
```

**_Code: 323002
Message: CPU usage is zero, No CPU Recommendations can be generated._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T00:59:50.724Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T00:59:50.724Z",
                                        "monitoring_end_time": "2023-06-15T00:59:50.724Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 0,
                                        "confidence_level": 0.0,
                                        "current": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 404.2,
                                                    "format": "MiB"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 751.09,
                                                    "format": "MiB"
                                                }
                                            }
                                        },
                                        "config": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 1197.732,
                                                    "format": "MiB"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 1197.732,
                                                    "format": "MiB"
                                                }
                                            }
                                        },
                                        "variation": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 793.5319999999999,
                                                    "format": "MiB"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 446.64199999999994,
                                                    "format": "MiB"
                                                }
                                            }
                                        },
                                        "notifications": {
                                            "323002": {
                                                "type": "notice",
                                                "message": "CPU usage is zero, No CPU Recommendations can be generated",
                                                "code": 323002
                                            }
                                        }
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "vivacious-bhanvi"
}
```

**_Code: 323003
Message: CPU metrics are not available, No CPU Recommendations can be generated._**

**_Code: 324001
Message: Memory usage is zero, No Memory Recommendations can be generated._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T01:01:32.356Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T01:01:32.356Z",
                                        "monitoring_end_time": "2023-06-15T01:01:32.356Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 1,
                                        "confidence_level": 0.0,
                                        "current": {
                                            "requests": {
                                                "cpu": {
                                                    "amount": 3.3,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "cpu": {
                                                    "amount": 3.2,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "config": {
                                            "requests": {
                                                "cpu": {
                                                    "amount": 7.58,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "cpu": {
                                                    "amount": 7.58,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "variation": {
                                            "requests": {
                                                "cpu": {
                                                    "amount": 4.28,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "cpu": {
                                                    "amount": 4.38,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "notifications": {
                                            "324001": {
                                                "type": "notice",
                                                "message": "Memory Usage is zero, No Memory Recommendations can be generated",
                                                "code": 324001
                                            }
                                        }
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "exquisite-chandrakala"
}
```

**_Code: 324002
Message: Memory metrics are not available, No Memory Recommendations can be generated._**

**_Code: 523001
Message: CPU Request Not Set._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T00:56:27.893Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T00:56:27.893Z",
                                        "monitoring_end_time": "2023-06-15T00:56:27.893Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 1,
                                        "confidence_level": 0.0,
                                        "current": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 525.5,
                                                    "format": "MiB"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 442.56,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 0.91,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "config": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 1187.964,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.73,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 1187.964,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.73,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "variation": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 662.4639999999999,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.73,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 745.404,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 6.82,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "notifications": {
                                            "523001": {
                                                "type": "critical",
                                                "message": "CPU Request Not Set",
                                                "code": 523001
                                            }
                                        }
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "hilarious-shreya"
}
```

**_Code: 423001
Message: CPU Limit Not Set._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T00:56:27.893Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T00:56:27.893Z",
                                        "monitoring_end_time": "2023-06-15T00:56:27.893Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 1,
                                        "confidence_level": 0.0,
                                        "current": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 525.5,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 0.91,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 442.56,
                                                    "format": "MiB"
                                                }
                                            }
                                        },
                                        "config": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 1187.964,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.73,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 1187.964,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.73,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "variation": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 662.4639999999999,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 6.82,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 745.404,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.73,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "notifications": {
                                            "423001": {
                                                "type": "warning",
                                                "message": "CPU Limit Not Set",
                                                "code": 423001
                                            }
                                        }
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "hilarious-shreya"
}
```

**_Code: 524001
Message: Memory Request Not Set._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T00:57:51.561Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T00:57:51.561Z",
                                        "monitoring_end_time": "2023-06-15T00:57:51.561Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 1,
                                        "confidence_level": 0.0,
                                        "current": {
                                            "requests": {
                                                "cpu": {
                                                    "amount": 0.61,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 615.15,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 0.94,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "config": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 1197.708,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.72,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 1197.708,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.72,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "variation": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 1197.708,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.109999999999999,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 582.5580000000001,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 6.779999999999999,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "notifications": {
                                            "524001": {
                                                "type": "critical",
                                                "message": "Memory Request Not Set",
                                                "code": 524001
                                            }
                                        }
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "serene-bhanvi"
}
```

**_Code: 524002
Message: Memory Limit Not Set._**

```
{
    "cluster_name": "cluster-one-division-bell",
    "kubernetes_objects": [
        {
            "type": "deployment",
            "name": "my-sample-deployment",
            "namespace": "default",
            "containers": [
                {
                    "container_image_name": "my-sample-image",
                    "container_name": "my-sample-container",
                    "recommendations": {
                        "notifications": {
                            "112101": {
                                "type": "info",
                                "message": "Duration Based Recommendations Available",
                                "code": 112101
                            }
                        },
                        "data": {
                            "2023-06-15T00:58:50.033Z": {
                                "duration_based": {
                                    "short_term": {
                                        "monitoring_start_time": "2023-06-14T00:58:50.033Z",
                                        "monitoring_end_time": "2023-06-15T00:58:50.033Z",
                                        "duration_in_hours": 24.0,
                                        "pods_count": 1,
                                        "confidence_level": 0.0,
                                        "current": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 237.52,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 3.04,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "cpu": {
                                                    "amount": 2.49,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "config": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 1187.688,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.54,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 1187.688,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 7.54,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "variation": {
                                            "requests": {
                                                "memory": {
                                                    "amount": 950.1680000000001,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 4.5,
                                                    "format": "cores"
                                                }
                                            },
                                            "limits": {
                                                "memory": {
                                                    "amount": 1187.688,
                                                    "format": "MiB"
                                                },
                                                "cpu": {
                                                    "amount": 5.05,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        "notifications": {
                                            "524002": {
                                                "type": "critical",
                                                "message": "Memory Limit Not Set",
                                                "code": 524002
                                            }
                                        }
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
                }
            ]
        }
    ],
    "version": "1.0",
    "experiment_name": "breathtaking-prathamesh"
}
```