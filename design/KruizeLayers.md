# Layers
This article describes how to detect and analyze different application layers with special focus on HotSpot, JVM and Quarkus runtime layers using REST APIs. 

# CreateLayer
This is quick guide to create metadata profile using input JSON as follows. 

**Request**
`POST /createLayer`
``` json
{
  "apiVersion": "recommender.com/v1",
  "kind": "KruizeLayer",
  "metadata": {
    "name": "hotspot",
    "description": "Hotspot JVM tuning"
  },
  "layer_presence": {
    "presence": "detectable",
    "detectors": [
      {
        "type": "query",
        "datasource": "prometheus",
        "query": "jvm_memory_used_bytes{area=\"heap\",id=~\".+Eden.+\"}",
        "key": "pod",
        "non_null_is_present": true
      }
    ]
  },
  "tunables": {
    "maxram-percentage": {
      "metadata": {
        "name": "maxram-percentage",
        "description": "JVM heap as % of container memory limit",
        "expression": "+XX:MaxRAMPercentage={(tunable:hotspot/maxram-percentage).value}",
        "value_type": "double",
        "unit": "percent",
        "type": "range",
        "type_def": {
          "bounds": {
            "lower": 10,
            "upper": 90,
            "step": 1
          }
        }
      },
      "depends_on": {
        "tunables": [
          "container/memory-limit"
        ],
        "metrics": [
          "memory-usage"
        ]
      },
      "calculations": [
        {
          "target": "value",
          "expr": "clamp(100.0 * percentile((metric:memory-usage), 0.95) / max(1e-6, (tunable:container/memory-limit).value * (1.0 - min(env.NON_HEAP_RATIO ?: 0.30, 0.80)) * 1.05), bounds.lower, bounds.upper)",
          "fallback": 70
        }
      ]
    },
    "gc-policy": {
      "metadata": {
        "name": "gc-policy",
        "description": "JVM GC policy",
        "expression": "+XX:+Use{(tunable:hotspot/gc-policy).value}",
        "value_type": "string",
        "type": "categorical",
        "type_def": {
          "choices": ["SerialGC", "ParallelGC", "G1GC", "ShenandoahGC", "ZGC"]
        }
      },
      "depends_on": {
        "tunables": [
          "container/cpu-limit",
          "container/memory-limit"
        ],
        "metrics": []
      },
      "calculations": [
        {
          "target": "value",
          "expr": "env.GC_POLICY_OVERRIDE ?: case when (tunable:container/cpu-limit).value < 2 && (tunable:container/memory-limit).value < 4294967296 then \"SerialGC\" when (tunable:container/cpu-limit).value < 4 && (tunable:container/memory-limit).value < 4294967296 then \"ParallelGC\" when (tunable:container/cpu-limit).value < 8 && (tunable:container/memory-limit).value > 4294967296 then \"G1GC\" when (tunable:container/cpu-limit).value < 16 && (tunable:container/memory-limit).value > 12884901888 then \"ShenandoahGC\" else \"ZGC\" end",
          "fallback": "G1GC"
        }
      ]
    }
  }
}
```
**Response**

``` json
{
    "message": "Layer : container created successfully. View Layers at /listLayers",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

# List Layers

This is quick guide to retrieve layers created as follows.

**Request**
`GET /listLayers`

`Note : We can use the above API without passing any parameters and expect all the available layers to be listed as part of response. Optionally, you can also pass 'name' paramter to request /listLayers?name=hotspot, to list specific layer and the API will return response for the specific requested layer`

**Optional Request Parameters**

| Parameter | Type   | Required | Description                               |
|-----------|--------|----------|-------------------------------------------|
| name      | string | optional | The name of the metadata profile          |

**Response for all layers list**

``` json
[
    {
        "apiVersion": "recommender.com/v1",
        "kind": "KruizeLayer",
        "metadata": {
            "name": "container",
            "description": "Kubernetes container resources"
        },
        "layer_presence": {
            "presence": "always",
            "detectors": []
        },
        "tunables": {
            "cpu-limit": {
                "metadata": {
                    "name": "cpu-limit",
                    "description": "Recommended CPU limit (cores)",
                    "value_type": "double",
                    "unit": "cores",
                    "type": "range",
                    "type_def": {
                        "bounds": {
                            "lower": 0.1,
                            "upper": 128.0,
                            "step": 0.1
                        }
                    }
                },
                "depends_on": {},
                "calculations": [
                    {
                        "target": "value",
                        "expr": "align_to_step(clamp(percentile((metric:cpu-usage), 0.95) * (1 + min((metric:cpu-throttle).avg * 2.0, 0.30)), bounds.lower, min(bounds.upper, (metric:namespace-cpu-limit).sum ?: bounds.upper)), bounds.step)",
                        "fallback": 1.0
                    }
                ]
            },
            "cpu-request": {
                "metadata": {
                    "name": "cpu-request",
                    "description": "Recommended CPU request (cores)",
                    "value_type": "double",
                    "unit": "cores",
                    "type": "range",
                    "type_def": {
                        "bounds": {
                            "lower": 0.0,
                            "upper": 128.0,
                            "step": 0.05
                        }
                    }
                },
                "depends_on": {
                    "tunables": [
                        "container/cpu-limit"
                    ]
                },
                "calculations": [
                    {
                        "target": "value",
                        "expr": "align_to_step(min(percentile((metric:cpu-usage), 0.90) * 1.10, (tunable:container/cpu-limit).value), bounds.step)",
                        "fallback": 0.5
                    }
                ]
            },
            "memory-limit": {
                "metadata": {
                    "name": "memory-limit",
                    "description": "Recommended Memory limit (bytes)",
                    "value_type": "double",
                    "unit": "bytes",
                    "type": "range",
                    "type_def": {
                        "bounds": {
                            "lower": 6.7108864E7,
                            "upper": 4.398046511104E12,
                            "step": 1048576.0
                        }
                    }
                },
                "depends_on": {},
                "calculations": [
                    {
                        "target": "value",
                        "expr": "align_to_step(clamp(percentile((metric:memory-usage), 0.95) + max((metric:memory-rss).max * 0.10, 134217728), bounds.lower, bounds.upper), bounds.step)",
                        "fallback": 5.36870912E8
                    }
                ]
            },
            "memory-request": {
                "metadata": {
                    "name": "memory-request",
                    "description": "Recommended Memory request (bytes)",
                    "value_type": "double",
                    "unit": "bytes",
                    "type": "range",
                    "type_def": {
                        "bounds": {
                            "lower": 6.7108864E7,
                            "upper": 4.398046511104E12,
                            "step": 1048576.0
                        }
                    }
                },
                "depends_on": {
                    "tunables": [
                        "container/memory-limit"
                    ]
                },
                "calculations": [
                    {
                        "target": "value",
                        "expr": "align_to_step(min(percentile((metric:memory-usage), 0.90) + max((metric:memory-rss).avg * 0.05, 67108864), (tunable:container/memory-limit).value), bounds.step)",
                        "fallback": 2.68435456E8
                    }
                ]
            }
        }
    },
    {
        "apiVersion": "recommender.com/v1",
        "kind": "KruizeLayer",
        "metadata": {
            "name": "hotspot",
            "description": "Hotspot JVM tuning"
        },
        "layer_presence": {
            "presence": "detectable",
            "detectors": [
                {
                    "type": "query",
                    "datasource": "prometheus",
                    "query": "jvm_memory_used_bytes{area=\"heap\",id=~\".+Eden.+\"}",
                    "key": "pod",
                    "non_null_is_present": true
                }
            ]
        },
        "tunables": {
            "gc-policy": {
                "metadata": {
                    "name": "gc-policy",
                    "description": "JVM GC policy",
                    "value_type": "string",
                    "type": "categorical",
                    "type_def": {
                        "choices": [
                            "SerialGC",
                            "ParallelGC",
                            "G1GC",
                            "ShenandoahGC",
                            "ZGC"
                        ]
                    }
                },
                "depends_on": {
                    "tunables": [
                        "container/cpu-limit",
                        "container/memory-limit"
                    ],
                    "metrics": []
                },
                "calculations": [
                    {
                        "target": "value",
                        "expr": "env.GC_POLICY_OVERRIDE ?: case when (tunable:container/cpu-limit).value < 2 && (tunable:container/memory-limit).value < 4294967296 then \"SerialGC\" when (tunable:container/cpu-limit).value < 4 && (tunable:container/memory-limit).value < 4294967296 then \"ParallelGC\" when (tunable:container/cpu-limit).value < 8 && (tunable:container/memory-limit).value > 4294967296 then \"G1GC\" when (tunable:container/cpu-limit).value < 16 && (tunable:container/memory-limit).value > 12884901888 then \"ShenandoahGC\" else \"ZGC\" end",
                        "fallback": "G1GC"
                    }
                ]
            },
            "maxram-percentage": {
                "metadata": {
                    "name": "maxram-percentage",
                    "description": "JVM heap as % of container memory limit",
                    "value_type": "double",
                    "unit": "percent",
                    "type": "range",
                    "type_def": {
                        "bounds": {
                            "lower": 10.0,
                            "upper": 90.0,
                            "step": 1.0
                        }
                    }
                },
                "depends_on": {
                    "tunables": [
                        "container/memory-limit"
                    ],
                    "metrics": [
                        "memory-usage"
                    ]
                },
                "calculations": [
                    {
                        "target": "value",
                        "expr": "clamp(100.0 * percentile((metric:memory-usage), 0.95) / max(1e-6, (tunable:container/memory-limit).value * (1.0 - min(env.NON_HEAP_RATIO ?: 0.30, 0.80)) * 1.05), bounds.lower, bounds.upper)",
                        "fallback": 70.0
                    }
                ]
            }
        }
    },
    {
        "apiVersion": "recommender.com/v1",
        "kind": "KruizeLayer",
        "metadata": {
            "name": "quarkus",
            "description": "Quarkus runtime tuning"
        },
        "layer_presence": {
            "presence": "detectable",
            "detectors": [
                {
                    "type": "label",
                    "name": "app.kubernetes.io/layer",
                    "value": "quarkus"
                }
            ]
        },
        "tunables": {
            "core-threads": {
                "metadata": {
                    "name": "core-threads",
                    "description": "Quarkus worker pool core threads",
                    "value_type": "integer",
                    "unit": "threads",
                    "type": "range",
                    "type_def": {
                        "bounds": {
                            "lower": 1.0,
                            "upper": 256.0,
                            "step": 1.0
                        }
                    }
                },
                "depends_on": {
                    "tunables": [
                        "container/cpu-limit",
                        "container/cpu-request"
                    ],
                    "metrics": []
                },
                "calculations": [
                    {
                        "target": "value",
                        "expr": "clamp(ceil(((tunable:container/cpu-limit).value ?: 1) * (env.QUARKUS_THREADS_FACTOR ?: 2)), bounds.lower, bounds.upper)",
                        "fallback": 4.0
                    },
                    {
                        "target": "bounds.lower",
                        "expr": "max(env.MIN_CORE_THREADS ?: 1, ceil((tunable:container/cpu-request).value))"
                    },
                    {
                        "target": "bounds.upper",
                        "expr": "min(env.MAX_CORE_THREADS ?: 256, ceil((tunable:container/cpu-limit).value * 16))"
                    }
                ]
            }
        }
    }
]

```
**Response for /listLayers?name=hotspot**
``` json
[
    {
        "apiVersion": "recommender.com/v1",
        "kind": "KruizeLayer",
        "metadata": {
            "name": "hotspot",
            "description": "Hotspot JVM tuning"
        },
        "layer_presence": {
            "presence": "detectable",
            "detectors": [
                {
                    "type": "query",
                    "datasource": "prometheus",
                    "query": "jvm_memory_used_bytes{area=\"heap\",id=~\".+Eden.+\"}",
                    "key": "pod",
                    "non_null_is_present": true
                }
            ]
        },
        "tunables": {
            "gc-policy": {
                "metadata": {
                    "name": "gc-policy",
                    "description": "JVM GC policy",
                    "value_type": "string",
                    "type": "categorical",
                    "type_def": {
                        "choices": [
                            "SerialGC",
                            "ParallelGC",
                            "G1GC",
                            "ShenandoahGC",
                            "ZGC"
                        ]
                    }
                },
                "depends_on": {
                    "tunables": [
                        "container/cpu-limit",
                        "container/memory-limit"
                    ],
                    "metrics": []
                },
                "calculations": [
                    {
                        "target": "value",
                        "expr": "env.GC_POLICY_OVERRIDE ?: case when (tunable:container/cpu-limit).value < 2 && (tunable:container/memory-limit).value < 4294967296 then \"SerialGC\" when (tunable:container/cpu-limit).value < 4 && (tunable:container/memory-limit).value < 4294967296 then \"ParallelGC\" when (tunable:container/cpu-limit).value < 8 && (tunable:container/memory-limit).value > 4294967296 then \"G1GC\" when (tunable:container/cpu-limit).value < 16 && (tunable:container/memory-limit).value > 12884901888 then \"ShenandoahGC\" else \"ZGC\" end",
                        "fallback": "G1GC"
                    }
                ]
            },
            "maxram-percentage": {
                "metadata": {
                    "name": "maxram-percentage",
                    "description": "JVM heap as % of container memory limit",
                    "value_type": "double",
                    "unit": "percent",
                    "type": "range",
                    "type_def": {
                        "bounds": {
                            "lower": 10.0,
                            "upper": 90.0,
                            "step": 1.0
                        }
                    }
                },
                "depends_on": {
                    "tunables": [
                        "container/memory-limit"
                    ],
                    "metrics": [
                        "memory-usage"
                    ]
                },
                "calculations": [
                    {
                        "target": "value",
                        "expr": "clamp(100.0 * percentile((metric:memory-usage), 0.95) / max(1e-6, (tunable:container/memory-limit).value * (1.0 - min(env.NON_HEAP_RATIO ?: 0.30, 0.80)) * 1.05), bounds.lower, bounds.upper)",
                        "fallback": 70.0
                    }
                ]
            }
        }
    }
]
```

