# TODO: Need add appropriate required fields
short_and_long_term_list_reco_json_schema = {
"type": "array",
  "items": {
    "type": "object",
    "properties": {
      "cluster_name": { "type": "string" },
      "kubernetes_objects": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "type": { "type": "string" },
            "name": { "type": "string" },
            "namespace": { "type": "string" },
            "containers": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "container_image_name": { "type": "string" },
                  "container_name": { "type": "string" },
                  "recommendations": {
                    "type": "object",
                    "properties": {
                      "version": { "type": "string" },
                      "notifications": {
                        "type": "object",
                        "items": {
                          "type": "object",
                          "properties": {
                            "type": { "type": "string" },
                            "message": { "type": "string" },
                            "code": { "type": "number" }
                          },
                          "required": ["type", "message", "code"]
                        }
                      },
                      "data": {
                        "type": "object",
                        "patternProperties": {
                          "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$": {
                            "type": "object",
                            "properties": {
                              "notifications": {
                                "type": "object",
                                "items": {
                                  "type": "object",
                                  "properties": {
                                    "type": { "type": "string" },
                                    "message": { "type": "string" },
                                    "code": { "type": "number" }
                                  },
                                  "required": ["type", "message", "code"]
                                }
                              },
                              "monitoring_end_time": { "type": "string" },
                              "current": {
                                "type": "object",
                                "properties": {
                                  "requests": {
                                    "type": "object",
                                    "properties": {
                                      "memory": {
                                        "type": "object",
                                        "properties": {
                                          "amount": { "type": "number" },
                                          "format": { "type": "string" }
                                        },
                                        "required": ["amount", "format"]
                                      },
                                      "cpu": {
                                        "type": "object",
                                        "properties": {
                                          "amount": { "type": "number" },
                                          "format": { "type": "string" }
                                        },
                                        "required": ["amount", "format"]
                                      }
                                    },
                                    "required": []
                                  },
                                  "limits": {
                                    "type": "object",
                                    "properties": {
                                      "memory": {
                                        "type": "object",
                                        "properties": {
                                          "amount": { "type": "number" },
                                          "format": { "type": "string" }
                                        },
                                        "required": ["amount", "format"]
                                      },
                                      "cpu": {
                                        "type": "object",
                                        "properties": {
                                          "amount": { "type": "number" },
                                          "format": { "type": "string" }
                                        },
                                        "required": ["amount", "format"]
                                      }
                                    },
                                    "required": ["memory", "cpu"]
                                  }
                                },
                                "required": []
                              },
                              "recommendation_terms": {
                                "type": "object",
                                "properties": {
                                  "short_term": {
                                    "type": "object",
                                    "properties": {
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": { "type": "string" },
                                            "message": { "type": "string" },
                                            "code": { "type": "number" }
                                          },
                                          "required": ["type", "message", "code"]
                                        }
                                      },
                                      "monitoring_start_time": { "type": "string" },
                                      "duration_in_hours": { "type": "number" },
                                      "recommendation_engines": {
                                        "type": "object",
                                        "properties": {
                                          "cost": {
                                            "type": "object",
                                            "properties": {
                                              "pods_count": { "type": "number" },
                                              "confidence_level": { "type": "number" },
                                              "config": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "variation": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "notifications": {
                                                "type": "object",
                                                "items": {
                                                  "type": "object",
                                                  "properties": {
                                                    "type": { "type": "string" },
                                                    "message": { "type": "string" },
                                                    "code": { "type": "number" }
                                                  },
                                                  "required": ["type", "message", "code"]
                                                }
                                              }
                                            },
                                            "required": ["pods_count", "confidence_level", "config", "variation", "notifications"]
                                          },
                                          "performance": {
                                            "type": "object",
                                            "properties": {
                                              "monitoring_start_time": { "type": "string" },
                                              "pods_count": { "type": "number" },
                                              "confidence_level": { "type": "number" },
                                              "config": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "variation": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "notifications": {
                                                "type": "object",
                                                "items": {
                                                  "type": "object",
                                                  "properties": {
                                                    "type": { "type": "string" },
                                                    "message": { "type": "string" },
                                                    "code": { "type": "number" }
                                                  },
                                                  "required": ["type", "message", "code"]
                                                }
                                              }
                                            },
                                            "required": []
                                          }
                                        },
                                        "required": []
                                      },
                                      "plots": {
                                        "type": "object",
                                        "properties": {
                                          "datapoints": { "type": "number" },
                                          "plots_data": {
                                            "type": "object",
                                            "patternProperties": {
                                              "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$": {
                                                "type": "object",
                                                "properties": {
                                                  "cpuUsage": {
                                                    "type": "object",
                                                    "properties": {
                                                      "min": { "type": "number" },
                                                      "q1": { "type": "number" },
                                                      "median": { "type": "number" },
                                                      "q3": { "type": "number" },
                                                      "max": { "type": "number" },
                                                      "format": { "type": "string" }
                                                    },
                                                    "required": ["min", "q1", "median", "q3", "max", "format"]
                                                  },
                                                  "memoryUsage": {
                                                    "type": "object",
                                                    "properties": {
                                                      "min": { "type": "number" },
                                                      "q1": { "type": "number" },
                                                      "median": { "type": "number" },
                                                      "q3": { "type": "number" },
                                                      "max": { "type": "number" },
                                                      "format": { "type": "string" }
                                                    },
                                                    "required": ["min", "q1", "median", "q3", "max", "format"]
                                                  },
                                                },
                                                "required": ["cpuUsage", "memoryUsage"]
                                              }
                                            },
                                            "required": []
                                          }
                                       },
                                        "required": ["datapoints", "plots_data"]
                                      }
                                    },
                                    "required": []
                                  },
                                  "medium_term": {
                                    "type": "object",
                                    "properties": {
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": { "type": "string" },
                                            "message": { "type": "string" },
                                            "code": { "type": "number" }
                                          },
                                          "required": ["type", "message", "code"]
                                        }
                                      },
                                      "duration_in_hours": { "type": "number" }
                                    },
                                    "required": []
                                  },
                                  "long_term": {
                                    "type": "object",
                                    "properties": {
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": { "type": "string" },
                                            "message": { "type": "string" },
                                            "code": { "type": "number" }
                                          },
                                          "required": ["type", "message", "code"]
                                        }
                                      },
                                      "monitoring_start_time": { "type": "string" },
                                      "duration_in_hours": { "type": "number" },
                                      "recommendation_engines": {
                                        "type": "object",
                                        "properties": {
                                          "cost": {
                                            "type": "object",
                                            "properties": {
                                              "pods_count": { "type": "number" },
                                              "confidence_level": { "type": "number" },
                                              "config": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "variation": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "notifications": {
                                                "type": "object",
                                                "items": {
                                                  "type": "object",
                                                  "properties": {
                                                    "type": { "type": "string" },
                                                    "message": { "type": "string" },
                                                    "code": { "type": "number" }
                                                  },
                                                  "required": ["type", "message", "code"]
                                                }
                                              }
                                            },
                                            "required": ["pods_count", "confidence_level", "config", "variation", "notifications"]
                                          },
                                          "performance": {
                                            "type": "object",
                                            "properties": {
                                              "monitoring_start_time": { "type": "string" },
                                              "pods_count": { "type": "number" },
                                              "confidence_level": { "type": "number" },
                                              "config": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "variation": {
                                                "type": "object",
                                                "properties": {
                                                  "requests": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  },
                                                  "limits": {
                                                    "type": "object",
                                                    "properties": {
                                                      "memory": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      },
                                                      "cpu": {
                                                        "type": "object",
                                                        "properties": {
                                                          "amount": { "type": "number" },
                                                          "format": { "type": "string" }
                                                        },
                                                        "required": ["amount", "format"]
                                                      }
                                                    },
                                                    "required": ["memory", "cpu"]
                                                  }
                                                },
                                                "required": ["requests", "limits"]
                                              },
                                              "notifications": {
                                                "type": "object",
                                                "items": {
                                                  "type": "object",
                                                  "properties": {
                                                    "type": { "type": "string" },
                                                    "message": { "type": "string" },
                                                    "code": { "type": "number" }
                                                  },
                                                  "required": ["type", "message", "code"]
                                                }
                                              }
                                            },
                                            "required": []
                                          }
                                        },
                                        "required": []
                                      },
                                      "plots": {
                                        "type": "object",
                                        "properties": {
                                          "datapoints": { "type": "number" },
                                          "plots_data": {
                                            "type": "object",
                                            "patternProperties": {
                                              "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$": {
                                                "type": "object",
                                                "properties": {
                                                  "cpuUsage": {
                                                    "type": "object",
                                                    "properties": {
                                                      "min": { "type": "number" },
                                                      "q1": { "type": "number" },
                                                      "median": { "type": "number" },
                                                      "q3": { "type": "number" },
                                                      "max": { "type": "number" },
                                                      "format": { "type": "string" }
                                                    },
                                                    "required": ["min", "q1", "median", "q3", "max", "format"]
                                                  },
                                                  "memoryUsage": {
                                                    "type": "object",
                                                    "properties": {
                                                      "min": { "type": "number" },
                                                      "q1": { "type": "number" },
                                                      "median": { "type": "number" },
                                                      "q3": { "type": "number" },
                                                      "max": { "type": "number" },
                                                      "format": { "type": "string" }
                                                    },
                                                    "required": ["min", "q1", "median", "q3", "max", "format"]
                                                  },
                                                },
                                                "required": ["cpuUsage", "memoryUsage"]
                                              }
                                            },
                                            "required": []
                                          }
                                       },
                                        "required": ["datapoints", "plots_data"]
                                      }
                                    },
                                    "required": []
                                  }
                                },
                                "required": []
                              }
                            },
                            "required": []
                          }
                        },
                        "required": []
                      }
                    },
                    "required": ["version", "notifications", "data"]
                  }
                },
                "required": ["container_image_name", "container_name", "recommendations"]
              }
            }
          },
          "required": ["type", "name", "namespace", "containers"]
        }
      },
      "version": { "type": "string" },
      "experiment_name": { "type": "string" }
    },
    "required": ["cluster_name", "kubernetes_objects", "version", "experiment_name"]
  }
}

