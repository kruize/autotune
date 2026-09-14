# Bulk Config API Documentation

The Bulk Config API provides a way to create and manage persistent, named configurations for the Bulk API. Instead of
passing the full configuration on every Bulk invocation, a Bulk Config stores settings such as datasources, namespaces,
labels, experiment types, profiles, and recommendation scheduling once and references them by name. The service
supports full CRUD operations: Create, Read (single and list), Update (partial), and Delete.

Bulk Configs are stored in the `kruize_optimizer_bulk_config` table and are referenced by `config_name`.

## Overview

| Method   | Endpoint                                 | Description                          |
|----------|------------------------------------------|--------------------------------------|
| `POST`   | `/bulkConfigs`                           | Create a new Bulk Config             |
| `GET`    | `/bulkConfigs`                           | List all Bulk Configs                |
| `GET`    | `/bulkConfigs?config_name=<name>`        | Get a specific Bulk Config by name   |
| `PUT`    | `/bulkConfigs?config_name=<name>`        | Update an existing Bulk Config       |
| `DELETE` | `/bulkConfigs?config_name=<name>`        | Delete a Bulk Config                 |

---

## API Specification

### POST /bulkConfigs

Creates a new named Bulk Config. The `config_name` must be unique. The `metadata_profile` and `performance_profile`
referenced in the payload must already exist (created via the respective profile APIs).

**Request Payload (JSON):**

```json
{
    "config_name": "default",
    "cluster_name": "default",
    "datasources": [
        "thanos-1"
    ],
    "namespaces": [],
    "labels": {
        "label_ibm_runtimes_intelligence_monitoring": "true"
    },
    "experiment_types": [
        "container"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "performance_profile": "resource-optimization-local-monitoring",
    "trial_settings": {
        "measurement_duration": "15min"
    },
    "recommendation_settings": {
        "scheduling": "5m",
        "terms": [
            "short"
        ],
        "models": [
            "performance"
        ]
    }
}
```

**Request Parameters:**

| Field                    | Type              | Required | Description                                                                                             |
|--------------------------|-------------------|----------|---------------------------------------------------------------------------------------------------------|
| `config_name`            | String            | Yes      | Unique name for the config. Allowed characters: alphanumeric, hyphens (`-`), underscores (`_`).         |
| `cluster_name`           | String            | Yes      | Name of the cluster this config targets.                                                                |
| `datasources`            | Array of Strings  | Yes      | One or more registered datasource names (e.g., `"thanos-1"`). Each datasource must be reachable.        |
| `namespaces`             | Array of Strings  | No       | Namespaces to scope the config to. Empty array means all namespaces.                                    |
| `labels`                 | Map (String→String) | No    | Key-value label filters to scope which workloads are included.                                          |
| `experiment_types`       | Array of Strings  | Yes      | Types of experiments to run. Valid values: `"container"`, `"namespace"`.                                |
| `metadata_profile`       | String            | Yes      | Name of an existing metadata profile. Must be pre-created via the Metadata Profile API.                 |
| `performance_profile`    | String            | No       | Name of an existing performance profile. Must be pre-created via the Performance Profile API.           |
| `trial_settings`         | Object            | No       | Trial settings for measurement. See [Trial Settings](#trial-settings).                                  |
| `recommendation_settings`| Object            | Yes      | Recommendation generation settings. See [Recommendation Settings](#recommendation-settings).            |
| `webhook_url`            | String            | No       | HTTP/HTTPS URL to notify when a bulk job triggered by this config completes.                            |
| `enabled`                | Boolean           | No       | Whether the config is active. Defaults to `true`.                                                       |

#### Trial Settings

| Field                  | Type   | Required | Description                                                    |
|------------------------|--------|----------|----------------------------------------------------------------|
| `measurement_duration` | String | No       | Duration of historic data to use (e.g., `"15min"`, `"1h"`).   |

#### Recommendation Settings

| Field        | Type             | Required | Description                                                                                                           |
|--------------|------------------|----------|-----------------------------------------------------------------------------------------------------------------------|
| `scheduling` | String           | Yes      | How frequently to generate recommendations. Format: `<number><unit>`. Valid units: `h/hr/hrs/hour/hours`, `m/min/mins/minute/minutes`, `d/day/days`. Examples: `"5m"`, `"24h"`, `"7days"`. |
| `terms`      | Array of Strings | Yes      | Recommendation term horizons. Valid values: `"short"`, `"medium"`, `"long"`.                                          |
| `models`     | Array of Strings | Yes      | Recommendation models to use. Valid values: `"performance"`, `"cost"`.                                                |

**Success Response:**

- **Status:** `201 Created`

```json
{
    "config_name": "default",
    "cluster_name": "default",
    "datasources": [
        "thanos-1"
    ],
    "namespaces": [],
    "labels": {
        "label_ibm_runtimes_intelligence_monitoring": "true"
    },
    "experiment_types": [
        "container"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "performance_profile": "resource-optimization-local-monitoring",
    "trial_settings": {
        "measurement_duration": "15min"
    },
    "recommendation_settings": {
        "scheduling": "5m",
        "terms": [
            "short"
        ],
        "models": [
            "performance"
        ]
    },
    "enabled": true,
    "created_at": "2025-06-01T10:00:00Z",
    "updated_at": "2025-06-01T10:00:00Z"
}
```

**Error Responses:**

| Status | Scenario                                                                    |
|--------|-----------------------------------------------------------------------------|
| `400`  | Missing or invalid required field (e.g., invalid `config_name`, bad `scheduling` format, unknown `terms` or `models` value) |
| `409`  | A config with the same `config_name` already exists                         |
| `500`  | Internal server error or database failure                                   |

**Example error (duplicate config_name):**

```json
{
    "error": "Bulk config with name 'default' already exists",
    "status": 409
}
```

---

### GET /bulkConfigs

Retrieves all existing Bulk Configs or a single one by name.

#### List all Bulk Configs

```bash
GET /bulkConfigs
```

**Success Response:**

- **Status:** `200 OK`

```json
[
    {
        "config_name": "default",
        "cluster_name": "default",
        "datasources": [
            "thanos-1"
        ],
        "namespaces": [],
        "labels": {
            "label_ibm_runtimes_intelligence_monitoring": "true"
        },
        "experiment_types": [
            "container"
        ],
        "metadata_profile": "cluster-metadata-local-monitoring",
        "performance_profile": "resource-optimization-local-monitoring",
        "trial_settings": {
            "measurement_duration": "15min"
        },
        "recommendation_settings": {
            "scheduling": "5m",
            "terms": [
                "short"
            ],
            "models": [
                "performance"
            ]
        },
        "enabled": true,
        "created_at": "2025-06-01T10:00:00Z",
        "updated_at": "2025-06-01T10:00:00Z"
    },
    {
        "config_name": "production-high-freq",
        "cluster_name": "prod-cluster",
        "datasources": [
            "prometheus-prod"
        ],
        "namespaces": [
            "app-ns",
            "backend-ns"
        ],
        "labels": {},
        "experiment_types": [
            "container",
            "namespace"
        ],
        "metadata_profile": "cluster-metadata-local-monitoring",
        "performance_profile": "resource-optimization-local-monitoring",
        "trial_settings": {
            "measurement_duration": "30min"
        },
        "recommendation_settings": {
            "scheduling": "1h",
            "terms": [
                "short",
                "medium"
            ],
            "models": [
                "performance",
                "cost"
            ]
        },
        "enabled": true,
        "created_at": "2025-06-02T08:30:00Z",
        "updated_at": "2025-06-02T08:30:00Z"
    }
]
```

#### Get a specific Bulk Config

```bash
GET /bulkConfigs?config_name=default
```

**Query Parameters:**

| Parameter     | Type   | Required | Description                             |
|---------------|--------|----------|-----------------------------------------|
| `config_name` | String | No       | Name of the config to retrieve. If omitted, all configs are returned. |

**Success Response:**

- **Status:** `200 OK`

```json
{
    "config_name": "default",
    "cluster_name": "default",
    "datasources": [
        "thanos-1"
    ],
    "namespaces": [],
    "labels": {
        "label_ibm_runtimes_intelligence_monitoring": "true"
    },
    "experiment_types": [
        "container"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "performance_profile": "resource-optimization-local-monitoring",
    "trial_settings": {
        "measurement_duration": "15min"
    },
    "recommendation_settings": {
        "scheduling": "5m",
        "terms": [
            "short"
        ],
        "models": [
            "performance"
        ]
    },
    "enabled": true,
    "created_at": "2025-06-01T10:00:00Z",
    "updated_at": "2025-06-01T10:00:00Z"
}
```

**Error Responses:**

| Status | Scenario                                     |
|--------|----------------------------------------------|
| `404`  | No config found with the given `config_name` |
| `500`  | Internal server error                        |

**Example error (not found):**

```json
{
    "error": "Bulk config not found: default",
    "status": 404
}
```

---

### PUT /bulkConfigs?config_name=\<name\>

Updates an existing Bulk Config. This is a **partial update** — only the fields present in the request body are
modified; omitted fields retain their current values. Nested objects (`trial_settings`, `recommendation_settings`)
also support field-level partial updates.

**Query Parameters:**

| Parameter     | Type   | Required | Description                               |
|---------------|--------|----------|-------------------------------------------|
| `config_name` | String | Yes      | Name of the config to update. Required.   |

**Request Payload (JSON):**

At least one field must be supplied. Any combination of the following fields is accepted:

```json
{
    "labels": {},
    "recommendation_settings": {
        "scheduling": "5m",
        "terms": [
            "short"
        ],
        "models": [
            "performance"
        ]
    }
}
```

All updatable fields:

```json
{
    "cluster_name": "new-cluster",
    "datasources": [
        "thanos-2"
    ],
    "namespaces": [
        "app-ns"
    ],
    "labels": {
        "env": "production"
    },
    "experiment_types": [
        "container",
        "namespace"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "performance_profile": "resource-optimization-local-monitoring",
    "trial_settings": {
        "measurement_duration": "30min"
    },
    "recommendation_settings": {
        "scheduling": "1h",
        "terms": [
            "short",
            "medium"
        ],
        "models": [
            "performance",
            "cost"
        ]
    },
    "webhook_url": "https://my-service.example.com/webhook",
    "enabled": true
}
```

> **Note:** `config_name` itself cannot be changed via PUT. To rename a config, delete and recreate it.

**Success Response:**

- **Status:** `200 OK`
- **Body:** The full updated config object, identical in shape to the GET response.

```json
{
    "config_name": "default",
    "cluster_name": "default",
    "datasources": [
        "thanos-1"
    ],
    "namespaces": [],
    "labels": {},
    "experiment_types": [
        "container"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "performance_profile": "resource-optimization-local-monitoring",
    "trial_settings": {
        "measurement_duration": "15min"
    },
    "recommendation_settings": {
        "scheduling": "5m",
        "terms": [
            "short"
        ],
        "models": [
            "performance"
        ]
    },
    "enabled": true,
    "created_at": "2025-06-01T10:00:00Z",
    "updated_at": "2025-06-01T12:00:00Z"
}
```

**Error Responses:**

| Status | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| `400`  | `config_name` query parameter missing, empty request body, or invalid field value |
| `404`  | No config found with the given `config_name`                             |
| `500`  | Internal server error or database failure                                |

**Example error (missing config_name parameter):**

```json
{
    "error": "config_name query parameter is required",
    "status": 400
}
```

**Example error (empty update body):**

```json
{
    "error": "At least one field must be provided for update",
    "status": 400
}
```

---

### DELETE /bulkConfigs?config_name=\<name\>

Deletes an existing Bulk Config by name. This operation is permanent and cannot be undone.

```bash
DELETE /bulkConfigs?config_name=default
```

**Query Parameters:**

| Parameter     | Type   | Required | Description                               |
|---------------|--------|----------|-------------------------------------------|
| `config_name` | String | Yes      | Name of the config to delete. Required.   |

**Success Response:**

- **Status:** `200 OK`

```json
{
    "message": "Bulk config deleted successfully",
    "config_name": "default"
}
```

**Error Responses:**

| Status | Scenario                                     |
|--------|----------------------------------------------|
| `400`  | `config_name` query parameter missing        |
| `404`  | No config found with the given `config_name` |
| `500`  | Internal server error or database failure    |

**Example error (not found):**

```json
{
    "error": "Bulk config not found: default",
    "status": 404
}
```

---

## Field Reference

### Complete BulkConfig Object

The following table describes all fields in a Bulk Config object as returned by GET or POST responses:

| Field                               | Type               | Description                                                                                              |
|-------------------------------------|--------------------|----------------------------------------------------------------------------------------------------------|
| `config_name`                       | String             | Unique identifier of the config. Alphanumeric, hyphens, and underscores only.                           |
| `cluster_name`                      | String             | Name of the cluster this config applies to.                                                              |
| `datasources`                       | Array of Strings   | List of datasource names to query (e.g., `["thanos-1"]`). Each must be registered and reachable.        |
| `namespaces`                        | Array of Strings   | Kubernetes namespaces to scope the config. Empty array means all namespaces.                             |
| `labels`                            | Map (String→String)| Key-value label filters. Only workloads matching all labels are included.                                |
| `experiment_types`                  | Array of Strings   | Experiment granularity. Valid values: `"container"`, `"namespace"`.                                      |
| `metadata_profile`                  | String             | Name of the metadata profile used to import cluster metadata. Must exist before config creation.         |
| `performance_profile`               | String             | Name of the performance profile used for recommendation generation. Must exist if provided.              |
| `trial_settings.measurement_duration` | String           | Duration of historic data for analysis (e.g., `"15min"`, `"1h"`). Optional; defaults to `"15min"`.      |
| `recommendation_settings.scheduling`| String             | Recommendation regeneration interval. Format: `<number><unit>` (e.g., `"5m"`, `"1h"`, `"7days"`).      |
| `recommendation_settings.terms`     | Array of Strings   | Term horizons. Valid: `"short"`, `"medium"`, `"long"`.                                                   |
| `recommendation_settings.models`    | Array of Strings   | Recommendation models. Valid: `"performance"`, `"cost"`.                                                 |
| `webhook_url`                       | String             | Optional HTTP/HTTPS URL called after a bulk job completes. Must use `http` or `https` protocol.          |
| `enabled`                           | Boolean            | Whether the config is active. Defaults to `true`.                                                        |
| `created_at`                        | Timestamp (ISO 8601) | Timestamp when the config was created. Set by the server; read-only.                                   |
| `updated_at`                        | Timestamp (ISO 8601) | Timestamp of the last update. Set by the server; read-only.                                            |

---

## Additional Payload Examples

### 1. Minimal Required Fields Only

```json
{
    "config_name": "minimal-config",
    "cluster_name": "dev-cluster",
    "datasources": [
        "prometheus-1"
    ],
    "experiment_types": [
        "container"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "recommendation_settings": {
        "scheduling": "1h",
        "terms": [
            "short"
        ],
        "models": [
            "performance"
        ]
    }
}
```

### 2. Multi-namespace, Multi-term Config

```json
{
    "config_name": "multi-ns-config",
    "cluster_name": "prod-cluster",
    "datasources": [
        "thanos-1",
        "prometheus-prod"
    ],
    "namespaces": [
        "app-ns",
        "backend-ns",
        "data-ns"
    ],
    "labels": {},
    "experiment_types": [
        "container",
        "namespace"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "performance_profile": "resource-optimization-local-monitoring",
    "trial_settings": {
        "measurement_duration": "30min"
    },
    "recommendation_settings": {
        "scheduling": "24h",
        "terms": [
            "short",
            "medium",
            "long"
        ],
        "models": [
            "performance",
            "cost"
        ]
    }
}
```

### 3. Config with Webhook and Label Filtering

```json
{
    "config_name": "monitored-workloads",
    "cluster_name": "default",
    "datasources": [
        "thanos-1"
    ],
    "namespaces": [],
    "labels": {
        "label_ibm_runtimes_intelligence_monitoring": "true",
        "env": "production"
    },
    "experiment_types": [
        "container"
    ],
    "metadata_profile": "cluster-metadata-local-monitoring",
    "performance_profile": "resource-optimization-local-monitoring",
    "trial_settings": {
        "measurement_duration": "15min"
    },
    "recommendation_settings": {
        "scheduling": "5m",
        "terms": [
            "short"
        ],
        "models": [
            "performance"
        ]
    },
    "webhook_url": "https://my-service.example.com/kruize/callback"
}
```

### 4. Partial Update — Change Only Scheduling and Labels

```bash
PUT /bulkConfigs?config_name=default
```

```json
{
    "labels": {
        "env": "staging"
    },
    "recommendation_settings": {
        "scheduling": "30m"
    }
}
```

### 5. Partial Update — Disable a Config

```bash
PUT /bulkConfigs?config_name=default
```

```json
{
    "enabled": false
}
```

---

## Validation Rules

| Field                                 | Rules                                                                                                  |
|---------------------------------------|--------------------------------------------------------------------------------------------------------|
| `config_name`                         | Required on create. Must match `^[a-zA-Z0-9_-]+$`. Cannot be empty.                                   |
| `cluster_name`                        | Required on create. Cannot be empty.                                                                   |
| `datasources`                         | Required on create. Must have at least one entry. Each datasource must be registered and reachable.    |
| `namespaces`                          | Optional. If provided, no entry may be an empty string.                                                |
| `labels`                              | Optional. If provided, no key may be an empty string.                                                  |
| `experiment_types`                    | Required on create. Must have at least one entry. Valid: `"container"`, `"namespace"`.                 |
| `metadata_profile`                    | Required on create. Must reference an existing metadata profile in the database.                       |
| `performance_profile`                 | Optional. If provided, must reference an existing performance profile in the database.                 |
| `recommendation_settings`             | Required on create. All three sub-fields (`scheduling`, `terms`, `models`) are required on create.     |
| `recommendation_settings.scheduling`  | Format: `<integer><unit>`. Valid units: `h`, `hr`, `hrs`, `hour`, `hours`, `m`, `min`, `mins`, `minute`, `minutes`, `d`, `day`, `days`. |
| `recommendation_settings.terms`       | Valid values: `"short"`, `"medium"`, `"long"`. Must be non-empty if provided.                         |
| `recommendation_settings.models`      | Valid values: `"performance"`, `"cost"`. Must be non-empty if provided.                               |
| `webhook_url`                         | Optional. Must be a valid URL using `http` or `https`.                                                |
| PUT request body                      | At least one field must be provided. Empty request bodies are rejected.                                |

---

## Prerequisites

Before creating a Bulk Config, ensure the following resources are already present:

1. **Metadata Profile** — Referenced by `metadata_profile`. Create via the [Metadata Profile API](MetadataProfileAPI.md).
2. **Performance Profile** — Referenced by `performance_profile` (if used). Create via the [Performance Profile API](PerformanceProfileAPI.md).
3. **Datasource** — Each name in `datasources` must be a registered, reachable datasource. Register via the [Datasource API](KruizeDatasource.md).
