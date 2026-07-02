"""
Copyright (c) 2023, 2023 Red Hat, IBM Corporation and others.

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

"""
Reusable base schema components for Kruize Autotune recommendations.

This module provides reusable schema building blocks to avoid duplication
across different recommendation schema files. It includes:
- Common schema components (notifications, resources, etc.)
- Schema builders for different recommendation terms
- Factory functions to generate complete schemas
"""

import pytest

# ============================================================================
# COMMON SCHEMA COMPONENTS
# ============================================================================

def get_notification_schema():
    """Returns the schema for notification objects."""
    return {
        "type": "object",
        "items": {
            "type": "object",
            "properties": {
                "type": {"type": "string"},
                "message": {"type": "string"},
                "code": {"type": "number"}
            },
            "required": ["type", "message", "code"]
        }
    }


def get_resource_amount_schema():
    """Returns the schema for resource amount (cpu/memory)."""
    return {
        "type": "object",
        "properties": {
            "amount": {"type": "number"},
            "format": {"type": "string"}
        },
        "required": ["amount", "format"]
    }


def get_requests_schema():
    """Returns the schema for resource requests."""
    return {
        "type": "object",
        "properties": {
            "memory": get_resource_amount_schema(),
            "cpu": get_resource_amount_schema()
        },
        "required": ["memory", "cpu"]
    }


def get_limits_schema():
    """Returns the schema for resource limits."""
    return {
        "type": "object",
        "properties": {
            "memory": get_resource_amount_schema(),
            "cpu": get_resource_amount_schema()
        },
        "required": ["memory", "cpu"]
    }


def get_config_schema():
    """Returns the schema for configuration (requests and limits)."""
    if pytest.USE_NEW_API:
        return {
            "type": "object",
            "properties": {
                "resources": {
                    "type": "object",
                    "properties": {
                        "requests": get_requests_schema(),
                        "limits": get_limits_schema()
                    },
                    "required": ["requests", "limits"]
                }
            },
            "required": ["resources"]
        }
    else:
        return {
            "type": "object",
            "properties": {
                "requests": get_requests_schema(),
                "limits": get_limits_schema()
            },
            "required": ["requests", "limits"]
        }


def get_variation_schema():
    """Returns the schema for variation (requests and limits)."""
    if pytest.USE_NEW_API:
        return {
            "type": "object",
            "properties": {
                "resources": {
                    "type": "object",
                    "properties": {
                        "requests": get_requests_schema(),
                        "limits": get_limits_schema()
                    },
                    "required": ["requests", "limits"]
                }
            },
            "required": ["resources"]
        }
    else:
        return {
            "type": "object",
            "properties": {
                "requests": get_requests_schema(),
                "limits": get_limits_schema()
            },
            "required": ["requests", "limits"]
        }


def get_current_schema(namespace_type=False):
    """Returns the schema for current resource configuration."""
    if pytest.USE_NEW_API:
        schema = {
            "type": "object",
            "properties": {
                "replicas": {"type": "integer"},
                "resources": {
                    "type": "object",
                    "properties": {
                        "requests": {
                            "type": "object",
                            "properties": {
                                "memory": get_resource_amount_schema(),
                                "cpu": get_resource_amount_schema()
                            },
                            "required": []
                        },
                        "limits": get_limits_schema()
                    }
                }
            },
            "required": ["replicas"]
        }
    else:
        schema = {
            "type": "object",
            "properties": {
                "replicas": {"type": "integer"},
                "requests": {
                    "type": "object",
                    "properties": {
                        "memory": get_resource_amount_schema(),
                        "cpu": get_resource_amount_schema()
                    },
                    "required": []
                },
                "limits": get_limits_schema()
            },
            "required": ["replicas"]
        }

    if namespace_type:
        schema["properties"].pop("replicas")
        schema["required"].remove("replicas")

    return schema


def get_plot_metrics_schema():
    """Returns the schema for plot metrics (cpuUsage, memoryUsage)."""
    return {
        "type": "object",
        "properties": {
            "min": {"type": "number"},
            "q1": {"type": "number"},
            "median": {"type": "number"},
            "q3": {"type": "number"},
            "max": {"type": "number"},
            "format": {"type": "string"}
        },
        "required": ["min", "q1", "median", "q3", "max", "format"]
    }


def get_plots_schema():
    """Returns the schema for plots data."""
    return {
        "type": "object",
        "properties": {
            "datapoints": {"type": "number"},
            "plots_data": {
                "type": "object",
                "patternProperties": {
                    "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$": {
                        "type": "object",
                        "properties": {
                            "cpuUsage": get_plot_metrics_schema(),
                            "memoryUsage": get_plot_metrics_schema(),
                        },
                        "required": []
                    }
                },
                "required": []
            }
        },
        "required": ["datapoints", "plots_data"]
    }


def get_recommendation_engine_schema(engine_type="cost"):
    """
    Returns the schema for a recommendation engine (cost or performance).
    
    Args:
        engine_type: Type of engine ("cost" or "performance")
    """
    base_schema = {
        "type": "object",
        "properties": {
            "pods_count": {"type": "number"},
            "confidence_level": {"type": "number"},
            "config": get_config_schema(),
            "variation": get_variation_schema(),
            "notifications": get_notification_schema()
        },
        "required": ["pods_count", "confidence_level", "config", "variation", "notifications"]
    }
    
    # Performance engine has additional monitoring_start_time
    if engine_type == "performance":
        base_schema["properties"]["monitoring_start_time"] = {"type": "string"}
        base_schema["required"] = []
    
    return base_schema


# ============================================================================
# RECOMMENDATION TERM SCHEMAS
# ============================================================================

def get_term_schema_with_data(include_plots=True, namespace_type=False):
    """
    Returns the schema for a recommendation term with full data.
    
    Args:
        include_plots: Whether to include plots in the schema
    """
    schema = {
        "type": "object",
        "properties": {
            "notifications": get_notification_schema(),
            "monitoring_start_time": {"type": "string"},
            "duration_in_hours": {"type": "number"},
            "metrics_info": {
                "type": "object",
                "properties": {
                    "pod_count": {
                        "type": "object",
                        "properties": {
                            "avg": {"type": "integer"},
                            "max": {"type": "integer"},
                            "min": {"type": "integer"}
                        },
                        "required": ["avg", "max", "min"]
                    }
                },
                "required": ["pod_count"]
            },
            "recommendation_engines": {
                "type": "object",
                "properties": {
                    "cost": get_recommendation_engine_schema("cost"),
                    "performance": get_recommendation_engine_schema("performance")
                },
                "required": []
            }
        },
        "required": ["metrics_info"]
    }

    if namespace_type: # Not applicable for namespace recommendations
        schema["properties"].pop("metrics_info")
        schema["required"].remove("metrics_info")

    if include_plots:
        schema["properties"]["plots"] = get_plots_schema()
    
    return schema


def get_term_schema_notification_only():
    """Returns the schema for a recommendation term with only notifications."""
    return {
        "type": "object",
        "properties": {
            "notifications": get_notification_schema(),
            "duration_in_hours": {"type": "number"}
        },
        "required": []
    }


# ============================================================================
# RECOMMENDATION TERMS BUILDER
# ============================================================================

def build_recommendation_terms_schema(terms_config, namespace_type=False):
    """
    Builds the recommendation_terms schema based on configuration.
    
    Args:
        terms_config: Dictionary specifying which terms to include and their type
                     Example: {
                         "short_term": "full",  # full data with plots
                         "medium_term": "notification_only",
                         "long_term": "notification_only"
                     }
    
    Returns:
        Dictionary containing the recommendation_terms schema
    """
    schema = {
        "type": "object",
        "properties": {},
        "required": []
    }
    
    for term, term_type in terms_config.items():
        if term_type == "full":
            schema["properties"][term] = get_term_schema_with_data(include_plots=True, namespace_type=namespace_type)
        elif term_type == "full_no_plots":
            schema["properties"][term] = get_term_schema_with_data(include_plots=False, namespace_type=namespace_type)
        elif term_type == "notification_only":
            schema["properties"][term] = get_term_schema_notification_only()
    
    return schema


# ============================================================================
# CONTAINER RECOMMENDATIONS SCHEMA
# ============================================================================

def get_container_recommendations_schema(terms_config):
    """
    Returns the schema for container recommendations.
    
    Args:
        terms_config: Configuration for recommendation terms
    """
    return {
        "type": "object",
        "properties": {
            "version": {"type": "string"},
            "notifications": get_notification_schema(),
            "data": {
                "type": "object",
                "patternProperties": {
                    "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$": {
                        "type": "object",
                        "properties": {
                            "notifications": get_notification_schema(),
                            "monitoring_end_time": {"type": "string"},
                            "current": get_current_schema(),
                            "recommendation_terms": build_recommendation_terms_schema(terms_config),
                        },
                        "required": []
                    }
                },
                "required": []
            }
        },
        "required": ["version", "notifications", "data"]
    }


# ============================================================================
# NAMESPACE RECOMMENDATIONS SCHEMA
# ============================================================================

def get_namespace_recommendations_schema(terms_config):
    """
    Returns the schema for namespace-level recommendations.
    
    Args:
        terms_config: Configuration for recommendation terms
    """
    return {
        "type": "object",
        "properties": {
            "version": {"type": "string"},
            "notifications": get_notification_schema(),
            "data": {
                "type": "object",
                "patternProperties": {
                    "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$": {
                        "type": "object",
                        "properties": {
                            "notifications": get_notification_schema(),
                            "monitoring_end_time": {"type": "string"},
                            "current": get_current_schema(namespace_type=True),
                            "recommendation_terms": build_recommendation_terms_schema(terms_config, True),
                        },
                        "required": []
                    }
                },
                "required": []
            }
        },
        "required": ["version", "notifications", "data"]
    }


# ============================================================================
# FULL RECOMMENDATION SCHEMA BUILDER
# ============================================================================

def build_list_recommendations_schema(terms_config, include_namespaces=False):
    """
    Builds the complete list recommendations schema.
    
    Args:
        terms_config: Configuration for recommendation terms
        include_namespaces: Whether to include namespace-level recommendations
    
    Returns:
        Complete schema dictionary
    """
    schema = {
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "cluster_name": {"type": "string"},
                "kubernetes_objects": {
                    "type": "array",
                    "items": {
                        "type": "object",
                    }
                },
                "version": {"type": "string"},
                "experiment_name": {"type": "string"}
            },
            "required": ["cluster_name", "kubernetes_objects", "version", "experiment_name"]
        }
    }
    
    # Add namespace recommendations if requested
    if include_namespaces:
        schema["items"]["properties"]["kubernetes_objects"]["items"]["properties"] = {
            "namespace": {"type": "string"},
            "namespaces": {
                "type": "object",
                "properties": {
                    "namespace": {"type": "string"},
                    "recommendations": get_namespace_recommendations_schema(terms_config)
                },
                "required": ["namespace", "recommendations"]
            },
            "containers": {
                "type": "array",
                "items": {}
            }
        }
        schema["items"]["properties"]["kubernetes_objects"]["items"]["required"] = ["namespace", "namespaces"]
    else:
        schema["items"]["properties"]["kubernetes_objects"]["items"]["properties"] = {
            "type": {"type": "string"},
            "name": {"type": "string"},
            "namespace": {"type": "string"},
            "containers": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "container_image_name": {"type": "string"},
                        "container_name": {"type": "string"},
                        "recommendations": get_container_recommendations_schema(terms_config)
                    },
                    "required": ["container_image_name", "container_name", "recommendations"]
                }
            }
        }

        schema["items"]["properties"]["kubernetes_objects"]["items"]["required"] = ["type", "name", "namespace", "containers"]

    
    return schema


# ============================================================================
# PREDEFINED SCHEMA CONFIGURATIONS
# ============================================================================

# Configuration for short term only
SHORT_TERM_CONFIG = {
    "short_term": "full",
    "medium_term": "notification_only",
    "long_term": "notification_only"
}

# Configuration for medium term only
MEDIUM_TERM_CONFIG = {
    "short_term": "notification_only",
    "medium_term": "full",
    "long_term": "notification_only"
}

# Configuration for long term only
LONG_TERM_CONFIG = {
    "short_term": "notification_only",
    "medium_term": "notification_only",
    "long_term": "full"
}

# Configuration for short and medium terms
SHORT_MEDIUM_TERM_CONFIG = {
    "short_term": "full",
    "medium_term": "full",
    "long_term": "notification_only"
}

# Configuration for short and long terms
SHORT_LONG_TERM_CONFIG = {
    "short_term": "full",
    "medium_term": "notification_only",
    "long_term": "full"
}

# Configuration for medium and long terms
MEDIUM_LONG_TERM_CONFIG = {
    "short_term": "notification_only",
    "medium_term": "full",
    "long_term": "full"
}

# Configuration for all terms
ALL_TERMS_CONFIG = {
    "short_term": "full",
    "medium_term": "full",
    "long_term": "full"
}

# Configuration for basic recommendations (no specific term focus)
BASIC_CONFIG = {
    "short_term": "full"
}

NO_TERMS_CONFIG = {
}