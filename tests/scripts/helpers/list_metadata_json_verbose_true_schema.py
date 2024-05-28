list_metadata_json_verbose_true_schema = {
    "type": "object",
    "properties": {
        "datasources": {
            "type": "object",
            "patternProperties": {
                "^[a-zA-Z0-9_-]+$": {
                    "type": "object",
                    "properties": {
                        "datasource_name": {
                            "type": "string",
                            "pattern": "^[a-zA-Z0-9_-]+$"
                        },
                        "clusters": {
                            "type": "object",
                            "patternProperties": {
                                "^[a-zA-Z0-9_-]+$": {
                                    "type": "object",
                                    "properties": {
                                        "cluster_name": {
                                            "type": "string",
                                            "pattern": "^[a-zA-Z0-9_-]+$"
                                        },
                                        "namespaces": {
                                            "type": "object",
                                            "patternProperties": {
                                                "^[a-zA-Z0-9_-]+$": {
                                                    "type": "object",
                                                    "properties": {
                                                        "namespace": {
                                                            "type": "string",
                                                            "pattern": "^[a-zA-Z0-9_-]+$"
                                                        },
                                                        "workloads": {
                                                            "type": "object",
                                                            "patternProperties": {
                                                                "^[a-zA-Z0-9_-]+$": {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "workload_name": {
                                                                            "type": "string",
                                                                            "pattern": "^[a-zA-Z0-9_-]+$"
                                                                        },
                                                                        "workload_type": {
                                                                            "type": "string"
                                                                        },
                                                                        "containers": {
                                                                            "type": "object",
                                                                            "patternProperties": {
                                                                                "^[a-zA-Z0-9_-]+$": {
                                                                                    "type": "object",
                                                                                    "properties": {
                                                                                        "container_name": {
                                                                                            "type": "string",
                                                                                            "pattern": "^[a-zA-Z0-9_-]+$"
                                                                                        },
                                                                                        "container_image_name": {
                                                                                            "type": "string"
                                                                                        }
                                                                                    },
                                                                                    "required": ["container_name",
                                                                                                 "container_image_name"]
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    "required": ["workload_name", "workload_type", "containers"]
                                                                }
                                                            }
                                                        }
                                                    },
                                                    "required": ["namespace"]
                                                }
                                            }
                                        }
                                    },
                                    "required": ["cluster_name", "namespaces"]
                                }
                            }
                        }
                    },
                    "required": ["datasource_name", "clusters"]
                }
            }
        }
    },
    "required": ["datasources"]
}
