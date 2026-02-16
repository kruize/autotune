"""
Copyright (c) 2026 IBM Corporation and others.

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

list_layers_schema = {
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
                "required": ["name"]
            },
            "layer_name": {
                "type": "string"
            },
            "layer_level": {
                "type": "integer"
            },
            "details": {
                "type": "string"
            },
            "layer_presence": {
                "type": "object",
                "properties": {
                    "presence": {
                        "type": "string"
                    },
                    "queries": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "datasource": {
                                    "type": "string"
                                },
                                "query": {
                                    "type": "string"
                                },
                                "key": {
                                    "type": "string"
                                }
                            },
                            "required": ["datasource", "query", "key"]
                        }
                    },
                    "label": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {
                                    "type": "string"
                                },
                                "value": {
                                    "type": "string"
                                }
                            },
                            "required": ["name", "value"]
                        }
                    }
                },
                "oneOf": [
                    {"required": ["presence"]},
                    {"required": ["queries"]},
                    {"required": ["label"]}
                ]
            },
            "tunables": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "name": {
                            "type": "string"
                        },
                        "value_type": {
                            "type": "string"
                        },
                        "description": {
                            "type": "string"
                        },
                        "lower_bound": {
                            "type": "string"
                        },
                        "upper_bound": {
                            "type": "string"
                        },
                        "step": {
                            "type": "number"
                        },
                        "choices": {
                            "type": "array",
                            "items": {
                                "type": "string"
                            }
                        }
                    },
                    "required": ["name", "value_type"]
                }
            }
        },
        "required": ["apiVersion", "kind", "metadata", "layer_name", "layer_level", "layer_presence", "tunables"]
    }
}
