# Kruize Autotune Architecture Overview

## What is Kruize Autotune?

**TL;DR**: Kruize analyzes your Kubernetes workload metrics and automatically generates right-sizing recommendations for CPU, memory, and GPU resources — reducing costs and improving performance without manual tuning.

### Key Capabilities
- **Resource Optimization**: Right-size CPU, memory requests/limits for containers
- **Namespace Optimization**: Resource quota recommendations for namespaces
- **GPU Optimization**: NVIDIA MIG slice configuration recommendations
- **Runtime Tuning** (Alpha): JVM and framework-specific tuning (Hotspot, Semeru, Quarkus)
- **Cost vs Performance**: Choose between cost-optimized or performance-optimized profiles

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Kruize UI                                │
│                    (React Frontend)                              │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP/REST
┌───────────────────────────────▼─────────────────────────────────┐
│                      Kruize Backend                              │
│                    (Java/Spring Boot)                            │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐   │
│  │   REST API   │  │   Analyzer   │  │ Experiment Manager│   │
│  │   Service    │  │   Engine     │  │                   │   │
│  └──────┬───────┘  └──────┬───────┘  └─────────┬─────────┘   │
│         │                  │                     │              │
│  ┌──────▼──────────────────▼─────────────────────▼─────────┐  │
│  │              Database Layer (PostgreSQL)                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────┬────────────────────────────┬─────────────────┘
                   │                            │
        ┌──────────▼────────┐        ┌─────────▼──────────┐
        │   Prometheus/     │        │   Kubernetes       │
        │     Thanos        │        │     Cluster        │
        │   (Metrics)       │        │   (Metadata)       │
        └───────────────────┘        └────────────────────┘
```

## Core Components

### 1. REST API Service (`com.autotune.service`)
**Purpose**: HTTP API layer for all Kruize operations

**Key APIs**:
- **Experiment Management**: `createExperiment`, `listExperiments`
- **Results Management**: `updateResults`, `listRecommendations`
- **Metadata Management**: `createMetadataProfile`, `importMetadata`, `listMetadata`
- **Metric Profiles**: `createMetricProfile`, `listMetricProfiles`
- **Performance Profiles**: `createPerformanceProfile`, `updatePerformanceProfile`
- **Layers**: `createLayer`, `listLayers`, `deleteLayer`
- **Datasources**: `createDatasource`, `listDatasources`

**Key Files**:
- `HealthService.java` - Health check endpoint
- `CreateExperiment.java` - Experiment creation
- `UpdateResults.java` - Metric data ingestion
- `ListRecommendations.java` - Recommendation generation

### 2. Analyzer Engine (`com.autotune.analyzer`)
**Purpose**: Core recommendation generation engine

**Responsibilities**:
- Analyze historical metrics from Prometheus/Thanos
- Calculate percentile-based resource recommendations (P95, P99)
- Generate cost vs performance optimized recommendations
- Support short/medium/long term recommendations
- Handle CPU, Memory, and GPU (MIG) recommendations

**Key Concepts**:
- **Terms**: Short (1-3 days), Medium (4-7 days), Long (8-15 days)
- **Recommendation Engines**: Cost engine, Performance engine
- **Aggregation**: Percentile-based analysis (P95, P99)

**Key Files**:
- `RecommendationEngine.java` - Main recommendation logic
- `PercentileCalculator.java` - Statistical analysis

### 3. Experiment Manager (`com.autotune.experimentManager`)
**Purpose**: Manages experiment lifecycle

**What is an Experiment?**
An experiment represents a workload being monitored for optimization. It contains:
- Target workload (deployment, statefulset, daemonset)
- Container names
- Datasource (Prometheus/Thanos)
- Metric profile (what to measure)
- Performance profile (SLO thresholds)

**Experiment Modes**:
- **Remote Monitoring**: Kruize passively monitors; you apply recommendations
- **Local Monitoring**: Kruize monitors cluster-wide workloads
- **VPA Mode** (Future): Automatic application of recommendations

### 4. Database Layer (`com.autotune.database`)
**Purpose**: Persistent storage for all Kruize data

**Technology**: PostgreSQL

**Key Tables**:
- `kruize_experiments` - Experiment definitions
- `kruize_results` - Metric data
- `kruize_recommendations` - Generated recommendations
- `kruize_metadata_profiles` - Metadata query definitions
- `kruize_metric_profiles` - Metric definitions
- `kruize_performance_profiles` - SLO profiles
- `kruize_layers` - Runtime/framework layer definitions
- `kruize_dsmetadata` - Imported cluster metadata

**DAO Pattern**:
- DAOInterface → DAOImpl (PostgreSQL)
- Clean separation of data access logic

### 5. Metrics Layer (`com.autotune.metrics`)
**Purpose**: Interact with monitoring systems (Prometheus/Thanos)

**Responsibilities**:
- Execute PromQL queries
- Fetch metrics data
- Validate datasource connectivity

### 6. Utils (`com.autotune.utils`)
**Purpose**: Common utilities and helpers

**Key Utilities**:
- JSON validation
- Configuration management
- Date/time utilities
- Constants and error messages

## Key Concepts Deep Dive

### Metadata Profiles
**What**: Query templates to fetch cluster/workload metadata

**Why**: Different monitoring setups have different label conventions. Metadata profiles tell Kruize how to query YOUR specific setup.

**Example**: 
```json
{
  "query": "kube_pod_labels{label_app='myapp'}",
  "datasource": "prometheus"
}
```

### Metric Profiles
**What**: Definition of which metrics to monitor and optimization goals

**Example Metrics**:
- CPU: `cpuRequest`, `cpuLimit`, `cpuUsage`, `cpuThrottle`
- Memory: `memoryRequest`, `memoryLimit`, `memoryUsage`, `memoryRSS`
- SLO: `responseTime`, `throughput`

### Performance Profiles
**What**: SLO (Service Level Objective) thresholds for your application

**Example**:
```json
{
  "slo_class": "response_time",
  "objective_function": "minimize",
  "function_variables": {
    "response_time": {
      "value_type": "double",
      "value": 100.0
    }
  }
}
```

### Layers (Runtime Detection)
**What**: Auto-detection and tuning for runtimes and frameworks

**Purpose**: Automatically detect if workload is using Quarkus, Hotspot JVM, Semeru, etc. and provide runtime-specific tunables

**Detection Methods**:
1. **presence: always** - Always apply
2. **queries** - Detect via PromQL queries
3. **label** - Detect via Kubernetes labels

**Example**:
```json
{
  "layer_name": "quarkus",
  "layer_presence": {
    "label": [{"name": "runtime", "value": "quarkus"}]
  },
  "tunables": [
    {
      "name": "quarkus.thread-pool.core-threads",
      "value_type": "integer",
      "lower_bound": "1",
      "upper_bound": "10"
    }
  ]
}
```

## Data Flow

### 1. Experiment Creation Flow
```
User → POST /createExperiment
  → CreateExperiment.java validates JSON
  → ExperimentManager creates experiment object
  → DAO saves to kruize_experiments table
  → Return success response
```

### 2. Results Update Flow
```
User/System → POST /updateResults (with metrics)
  → UpdateResults.java validates data
  → MetricsLayer validates datasource
  → DAO saves to kruize_results table
  → Trigger recommendation calculation (if enough data)
```

### 3. Recommendation Generation Flow
```
User → GET /listRecommendations?experiment_name=X
  → ListRecommendations.java
  → DAO fetch metrics from kruize_results
  → Analyzer.generateRecommendations()
    → Calculate percentiles (P95, P99)
    → Apply cost/performance optimization logic
    → Generate short/medium/long term recommendations
  → DAO save to kruize_recommendations
  → Return JSON response with recommendations
```

### 4. Metadata Import Flow
```
User → POST /importMetadata
  → ImportMetadata.java
  → MetricsLayer executes PromQL queries (from metadata profile)
  → Fetch namespace, deployment, container data from Prometheus
  → DAO save to kruize_dsmetadata table
  → Return imported metadata summary
```

## Monitoring Modes

### Local Monitoring
- Kruize monitors entire cluster
- Imports metadata automatically
- Generates recommendations for all workloads
- User applies recommendations manually

### Remote Monitoring
- User creates experiments explicitly
- User pushes metrics via updateResults API
- Kruize generates recommendations
- User applies recommendations manually

## Technology Stack

- **Backend**: Java 17, Spring Boot, JAX-RS
- **Database**: PostgreSQL
- **Metrics**: Prometheus, Thanos
- **Container**: Docker, Kubernetes
- **Build**: Maven
- **Testing**: JUnit, Pytest
- **Frontend**: React (kruize-ui repo)

## Code Organization

```
src/main/java/com/autotune/
├── analyzer/           # Recommendation engine
├── common/             # Common models and interfaces
├── database/           # Database layer (DAOs)
├── experimentManager/  # Experiment lifecycle
├── metrics/            # Prometheus/Thanos integration
├── operator/           # Kubernetes operator logic
├── service/            # REST API endpoints
└── utils/              # Utilities and helpers
```

## Configuration

Kruize configuration is managed via:
1. **Environment variables**: `LOGGING_LEVEL`, `DB_HOST`, etc.
2. **ConfigMaps**: Kubernetes ConfigMaps for cluster deployment
3. **autotune_config.json**: Local configuration file

## Next Steps

Now that you understand the architecture:
1. Review `code-structure.md` for detailed package walkthrough
2. Check `testing-guide.md` for test architecture
3. Read design docs in `/design/` for specific features
4. Start making small code changes!

## Additional Resources

- `/design/autotune-architecture.png` - Architecture diagram
- `/design/KruizeLocalAPI.md` - Local monitoring API specs
- `/design/MonitoringModeAPI.md` - Remote monitoring API specs
- `/design/KruizeLayers.md` - Layers design document
- `/design/KruizeDatabaseDesign.md` - Database schema details
