list_reco_json_schema = {
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "cluster_name": {
        "type": "string"
      },
      "kubernetes_objects": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "type": {
              "type": "string"
            },
            "name": {
              "type": "string"
            },
            "namespace": {
              "type": "string"
            },
            "containers": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "container_image_name": {
                    "type": "string"
                  },
                  "container_name": {
                    "type": "string"
                  },
                  "recommendations": {
                    "type": "object",
                    "properties": {
                      "notifications": {
                        "type": "object",
                        "items": {
                          "type": "object",
                          "properties": {
                            "type": {
                              "type": "string"
                            },
                            "message": {
                              "type": "string"
                            },
                            "code": {
                              "type": "number"
                            },
                          },
                          "required": [
                            "type",
                            "message",
                            "code"
                          ]
                        }
                      },
                      "data": {
                        "type": "object",
                        "patternProperties": {
                          "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$": {
                            "type": "object",
                            "properties": {
                              "duration_based": {
                                "type": "object",
                                "properties": {
                                  "short_term": {
                                    "type": "object",
                                    "properties": {
                                      "monitoring_start_time": {
                                        "type": "string"
                                      },
                                      "monitoring_end_time": {
                                        "type": "string"
                                      },
                                      "duration_in_hours": {
                                        "type": "number"
                                      },
                                      "pods_count": {
                                        "type": "number"
                                      },
                                      "confidence_level": {
                                        "type": "number"
                                      },
                                      "current": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          }
                                        },
                                        "required": [

                                        ]
                                      },
                                      "config": {
                                        "type": "object",
                                        "properties": {
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "requests",
                                          "limits"
                                        ]
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
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "requests",
                                          "limits"
                                        ]
                                      },
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            },
                                            "code": {
                                              "type": "number"
                                            },
                                          },
                                          "required": [
                                            "type",
                                            "message",
                                            "code"
                                          ]
                                        }
                                      }
                                    },
                                    "required": [
                                      "monitoring_start_time",
                                      "monitoring_end_time",
                                      "duration_in_hours",
                                      "pods_count",
                                      "confidence_level",
                                      "config",
                                      "variation",
                                      "notifications"
                                    ]
                                  },
                                  "medium_term": {
                                    "type": "object",
                                    "properties": {
                                      "pods_count": {
                                        "type": "number"
                                      },
                                      "confidence_level": {
                                        "type": "number"
                                      },
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            },
                                            "code": {
                                              "type": "number"
                                            },
                                          },
                                          "required": [
                                            "type",
                                            "message",
                                            "code"
                                          ]
                                        }
                                      }
                                    },
                                    "required": [
                                      "pods_count",
                                      "confidence_level",
                                      "notifications"
                                    ]
                                  },
                                  "long_term": {
                                    "type": "object",
                                    "properties": {
                                      "pods_count": {
                                        "type": "number"
                                      },
                                      "confidence_level": {
                                        "type": "number"
                                      },
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            },
                                            "code": {
                                              "type": "number"
                                            },
                                          },
                                          "required": [
                                            "type",
                                            "message",
                                            "code"
                                          ]
                                        }
                                      }
                                    },
                                    "required": [
                                      "pods_count",
                                      "confidence_level",
                                      "notifications"
                                    ]
                                  }
                                },
                                "required": [
                                  "short_term",
                                  "medium_term",
                                  "long_term"
                                ]
                              },
                              "profile_based": {
                                "type": "object",
                                "properties": {
                                  "cost": {
                                    "type": "object",
                                    "properties": {
                                      "monitoring_start_time": {
                                        "type": "string"
                                      },
                                      "monitoring_end_time": {
                                        "type": "string"
                                      },
                                      "duration_in_hours": {
                                        "type": "string"
                                      },
                                      "pods_count": {
                                        "type": "number"
                                      },
                                      "confidence_level": {
                                        "type": "number"
                                      },
                                      "current": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          }
                                        },
                                        "required": [

                                        ]
                                      },
                                      "config": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "limits",
                                          "requests"
                                        ]
                                      },
                                      "variation": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "limits",
                                          "requests"
                                        ]
                                      },
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            },
                                            "code": {
                                              "type": "number"
                                            },
                                          },
                                          "required": [
                                            "type",
                                            "message",
                                            "code"
                                          ]
                                        }
                                      }
                                    },
                                    "required": [
                                      "monitoring_start_time",
                                      "monitoring_end_time",
                                      "duration_in_hours",
                                      "pods_count",
                                      "confidence_level",
                                      "config",
                                      "variation",
                                      "notifications"
                                    ]
                                  },
                                  "balanced": {
                                    "type": "object",
                                    "properties": {
                                      "monitoring_start_time": {
                                        "type": "string"
                                      },
                                      "monitoring_end_time": {
                                        "type": "string"
                                      },
                                      "duration_in_hours": {
                                        "type": "string"
                                      },
                                      "pods_count": {
                                        "type": "number"
                                      },
                                      "confidence_level": {
                                        "type": "number"
                                      },
                                      "current": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          }
                                        },
                                        "required": [

                                        ]
                                      },
                                      "config": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "limits",
                                          "requests"
                                        ]
                                      },
                                      "variation": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "limits",
                                          "requests"
                                        ]
                                      },
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            },
                                            "code": {
                                              "type": "number"
                                            },
                                          },
                                          "required": [
                                            "type",
                                            "message",
                                            "code"
                                          ]
                                        }
                                      }
                                    },
                                    "required": [
                                      "monitoring_start_time",
                                      "monitoring_end_time",
                                      "duration_in_hours",
                                      "pods_count",
                                      "confidence_level",
                                      "config",
                                      "variation",
                                      "notifications"
                                    ]
                                  },
                                  "performance": {
                                    "type": "object",
                                    "properties": {
                                      "monitoring_start_time": {
                                        "type": "string"
                                      },
                                      "monitoring_end_time": {
                                        "type": "string"
                                      },
                                      "duration_in_hours": {
                                        "type": "string"
                                      },
                                      "pods_count": {
                                        "type": "number"
                                      },
                                      "confidence_level": {
                                        "type": "number"
                                      },
                                      "current": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [

                                            ]
                                          }
                                        },
                                        "required": [

                                        ]
                                      },
                                      "config": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "limits",
                                          "requests"
                                        ]
                                      },
                                      "variation": {
                                        "type": "object",
                                        "properties": {
                                          "limits": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          },
                                          "requests": {
                                            "type": "object",
                                            "properties": {
                                              "memory": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              },
                                              "cpu": {
                                                "type": "object",
                                                "properties": {
                                                  "amount": {
                                                    "type": "number"
                                                  },
                                                  "format": {
                                                    "type": "string"
                                                  }
                                                },
                                                "required": [
                                                  "amount",
                                                  "format"
                                                ]
                                              }
                                            },
                                            "required": [
                                              "memory",
                                              "cpu"
                                            ]
                                          }
                                        },
                                        "required": [
                                          "limits",
                                          "requests"
                                        ]
                                      },
                                      "notifications": {
                                        "type": "object",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            },
                                            "code": {
                                              "type": "number"
                                            },
                                          },
                                          "required": [
                                            "type",
                                            "message",
                                            "code"
                                          ]
                                        }
                                      }
                                    },
                                    "required": [
                                      "monitoring_start_time",
                                      "monitoring_end_time",
                                      "duration_in_hours",
                                      "pods_count",
                                      "confidence_level",
                                      "config",
                                      "variation",
                                      "notifications"
                                    ]
                                  }
                                },
                                "required": [
                                  "cost",
                                  "balanced",
                                  "performance"
                                ]
                              }
                            },
                            "required": [
                              "duration_based"
                            ]
                          }
                        },
                      }
                    },
                    "required": [
                      "notifications",
                      "data"
                    ]
                  }
                },
                "required": [
                  "container_image_name",
                  "container_name",
                  "recommendations"
                ]
              }
            }
          },
          "required": [
            "type",
            "name",
            "namespace",
            "containers"
          ]
        }
      },
      "version": {
        "type": "string"
      },
      "experiment_name": {
        "type": "string"
      }
    },
    "required": [
      "cluster_name",
      "kubernetes_objects",
      "version",
      "experiment_name"
    ]
  }
}
