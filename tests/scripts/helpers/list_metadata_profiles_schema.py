"""
Copyright (c) 2025, 2025 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

list_metadata_profiles_schema = {
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "apiVersion": {
                "type": "string"
            },
            "kind": {
                "type": "string"
            },
            "metadata": {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string"
                    }
                },
                "required": [
                    "name"
                ]
            },
            "profile_version": {
                "type": "number"
            },
            "k8s_type": {
                "type": "string"
            },
            "datasource": {
                "type": "string"
            },
            "query_variables": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "name": {
                            "type": "string"
                        },
                        "datasource": {
                            "type": "string"
                        },
                        "value_type": {
                            "type": "string"
                        },
                        "kubernetes_object": {
                            "type": "string"
                        },
                        "aggregation_functions": {
                            "type": "object",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "function": {
                                        "type": "string",
                                        "pattern": "^[a-zA-Z0-9_-]+$"
                                    },
                                    "query": {
                                        "type": "string"
                                    }
                                },
                                "required": ["function", "query"]
                            }
                        }
                    },
                    "required": ["name", "datasource", "value_type", "kubernetes_object", "aggregation_functions"]
                }
            }
        },
        "required": ["apiVersion", "kind", "metadata", "profile_version", "k8s_type", "datasource", "query_variables"]
    }
}
