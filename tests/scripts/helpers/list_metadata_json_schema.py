list_metadata_json_schema = {
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
                                        }
                                    },
                                    "required": ["cluster_name"]
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
