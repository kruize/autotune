list_datasources_json_schema = {
    "type": "object",
    "properties": {
        "version": {
            "type": "string"
        },
        "datasources": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string"
                    },
                    "provider": {
                        "type": "string"
                    },
                    "serviceName": {
                        "type": "string"
                    },
                    "namespace": {
                        "type": "string"
                    },
                    "url": {
                        "type": "string",
                        "format": "uri"
                    }
                },
                "required": ["name", "provider", "serviceName", "namespace", "url"]
            }
        }
    },
    "required": ["version", "datasources"]
}
