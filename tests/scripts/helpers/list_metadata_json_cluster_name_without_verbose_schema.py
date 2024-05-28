list_metadata_json_cluster_name_without_verbose_schema = {
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
