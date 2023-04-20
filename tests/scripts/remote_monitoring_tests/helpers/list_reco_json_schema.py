list_reco_json_schema = {
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "version": {
        "type": "string"
      },
      "experiment_name": {
        "type": "string"
      },
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
                  "container_name": {
                    "type": "string"
                  },
                  "recommendations": {
                    "type": "object",
                    "properties": {
                      "notifications": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "type": {
                              "type": "string"
                            },
                            "message": {
                              "type": "string"
                            }
                          },
                          "required": [
                            "type",
                            "message"
                          ]
                        }
                      },
                      "data": {
                        "type": "object",
                        "properties": {
                          "2022-01-23T18:25:43.511Z": {
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
                                        "type": "string"
                                      },
                                      "pods_count": {
                                        "type": "number"
                                      },
                                      "confidence_level": {
                                        "type": "number"
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
                                        "type": "array",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            }
                                          },
                                          "required": [
                                            "type",
                                            "message"
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
                                      "notifications"
                                    ]
                                  },
                                  "medium_term": {
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
                                        "type": "array",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            }
                                          },
                                          "required": [
                                            "type",
                                            "message"
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
                                        "type": "array",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            }
                                          },
                                          "required": [
                                            "type",
                                            "message"
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
                                        "type": "array",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            }
                                          },
                                          "required": [
                                            "type",
                                            "message"
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
                                        "type": "array",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            }
                                          },
                                          "required": [
                                            "type",
                                            "message"
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
                                        "type": "array",
                                        "items": {
                                          "type": "object",
                                          "properties": {
                                            "type": {
                                              "type": "string"
                                            },
                                            "message": {
                                              "type": "string"
                                            }
                                          },
                                          "required": [
                                            "type",
                                            "message"
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
                              "duration_based",
                            ]
                          }
                        },
                      }
                    },
                    "required": [
                      "notifications"
                    ]
                  },
                  "name": {
                    "type": "string"
                  }
                },
                "required": [
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
      }
    },
    "required": [
      "version",
      "experiment_name",
      "cluster_name",
      "kubernetes_objects"
    ]
  }
}

