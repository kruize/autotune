# Kruize Autotune Code Structure

## Project Layout

```
autotune/
├── .bob/                      # Bob AI skills (onboarding, etc.)
├── design/                    # Architecture and API design docs
├── docs/                      # User documentation
├── manifests/                 # Kubernetes manifests
│   ├── autotune/             
│   │   ├── layers/           # Layer configuration files
│   │   ├── metadata-profiles/# Metadata profile definitions
│   │   └── performance-profiles/  # Metric and perf profiles
│   ├── crc/                  # OpenShift CRC deployment
│   └── operator/             # Operator deployment files
├── scripts/                   # Deployment and utility scripts
├── src/
│   ├── main/java/com/autotune/   # Main source code
│   └── test/java/com/autotune/   # Java unit tests
├── tests/                     # Integration tests (Python)
├── pom.xml                    # Maven build configuration
├── Dockerfile.autotune        # Container image definition
├── deploy.sh                  # Main deployment script
├── CONTRIBUTING.md
└── README.md
```

## Source Code Structure (`src/main/java/com/autotune/`)

### 1. `analyzer/` - Recommendation Engine

**Purpose**: Core logic for generating resource recommendations

```
analyzer/
├── kruizeObject/             # Experiment and recommendation data models
│   ├── KruizeObject.java    # Main experiment object
│   ├── RecommendationSettings.java
│   └── ...
├── performanceProfiles/      # Performance profile management
├── recommendations/          # Recommendation calculation engine
│   ├── RecommendationConfigItem.java
│   └── ...
├── utils/                    # Analyzer utilities
└── Experimentator.java       # Experiment orchestration
```

**Key Classes**:
- `KruizeObject.java` - Represents an experiment (workload being optimized)
- `RecommendationConfigItem.java` - Recommendation calculation logic
- `PerformanceProfile.java` - SLO profile definitions

**When to touch**:
- Adding new recommendation algorithms
- Modifying percentile calculation logic
- Changing cost/performance optimization strategies

---

### 2. `common/` - Common Models and Interfaces

**Purpose**: Shared data models and interfaces

```
common/
├── data/                     # Data transfer objects
│   ├── ValidationOutputData.java
│   └── ...
├── datasource/               # Datasource models
│   ├── DataSourceInfo.java
│   └── DataSourceCollection.java
├── k8sObjects/               # Kubernetes object models
│   ├── KubernetesObject.java
│   ├── Namespace.java
│   ├── Container.java
│   └── ...
├── layer/                    # Layer models
│   ├── Layer.java
│   ├── LayerPresence.java
│   ├── Tunable.java
│   └── ...
└── trials/                   # Trial/experiment models
```

**Key Classes**:
- `KubernetesObject.java` - Base for Deployment, StatefulSet, etc.
- `Container.java` - Container resource specifications
- `Layer.java` - Runtime/framework layer definition
- `Tunable.java` - Tunable parameter definition

**When to touch**:
- Adding new data models
- Extending Kubernetes object support
- Defining new layer types

---

### 3. `database/` - Database Layer

**Purpose**: All database interactions (PostgreSQL)

```
database/
├── dao/                      # Data Access Objects
│   ├── ExperimentDAO.java
│   ├── ExperimentDAOImpl.java
│   ├── LayerDAO.java
│   ├── LayerDAOImpl.java
│   ├── MetadataDAO.java
│   ├── PerformanceProfileDAO.java
│   └── ...
├── table/                    # Table models
│   ├── KruizeExperimentEntry.java
│   ├── KruizeLayerEntry.java
│   ├── KruizeResultsEntry.java
│   └── ...
└── DatabaseFactory.java      # DB connection factory
```

**DAO Pattern**:
```
Interface (DAO) → Implementation (DAOImpl)
```

Example:
```java
// Interface
public interface LayerDAO {
    void addLayerToDB(Layer layer) throws Exception;
    Layer getLayer(String layerName) throws Exception;
    void deleteLayer(String layerName) throws Exception;
}

// Implementation
public class LayerDAOImpl implements LayerDAO {
    @Override
    public void addLayerToDB(Layer layer) throws Exception {
        // PostgreSQL implementation
    }
}
```

**Key Classes**:
- `ExperimentDAOImpl.java` - CRUD for experiments
- `LayerDAOImpl.java` - CRUD for layers
- `PerformanceProfileDAOImpl.java` - CRUD for performance profiles
- `KruizeExperimentEntry.java` - Maps to `kruize_experiments` table

**When to touch**:
- Adding new database tables
- Modifying schema
- Adding new queries
- Database migration scripts

---

### 4. `experimentManager/` - Experiment Lifecycle

**Purpose**: Manages experiment creation, updates, and lifecycle

```
experimentManager/
├── core/                     # Core experiment logic
├── exceptions/               # Custom exceptions
├── handler/                  # Request handlers
└── utils/                    # Utilities
```

**Key Responsibilities**:
- Validate experiment creation requests
- Manage experiment state transitions
- Coordinate between API, database, and analyzer

**When to touch**:
- Modifying experiment creation logic
- Adding new experiment types
- Changing experiment validation rules

---

### 5. `metrics/` - Metrics Integration

**Purpose**: Interaction with Prometheus/Thanos

```
metrics/
├── MetricCollector.java      # Collect metrics from datasource
├── MetricsImpl.java          # Metrics implementation
└── PromQLValidator.java      # Validate PromQL queries
```

**Key Responsibilities**:
- Execute PromQL queries
- Fetch time-series data
- Validate datasource connectivity

**When to touch**:
- Adding new datasource types (beyond Prometheus/Thanos)
- Modifying query patterns
- Implementing query optimization

---

### 6. `operator/` - Kubernetes Operator Logic

**Purpose**: Kubernetes operator-specific code

```
operator/
├── KruizeDeployment.java     # Deployment management
└── ...
```

**When to touch**:
- Kubernetes CRD changes
- Operator deployment logic

---

### 7. `service/` - REST API Layer

**Purpose**: HTTP REST API endpoints

```
service/
├── CreateExperiment.java           # POST /createExperiment
├── UpdateResults.java              # POST /updateResults
├── ListRecommendations.java        # GET /listRecommendations
├── CreateMetadataProfile.java      # POST /createMetadataProfile
├── ImportMetadata.java             # POST /importMetadata
├── ListMetadata.java               # GET /listMetadata
├── CreateMetricProfile.java        # POST /createMetricProfile
├── ListMetricProfiles.java         # GET /listMetricProfiles
├── CreatePerformanceProfile.java   # POST /createPerformanceProfile
├── UpdatePerformanceProfile.java   # PUT /updatePerformanceProfile
├── DeletePerformanceProfile.java   # DELETE /deletePerformanceProfile
├── CreateLayer.java                # POST /createLayer
├── ListLayers.java                 # GET /listLayers
├── DeleteLayer.java                # DELETE /deleteLayer
├── ListDatasources.java            # GET /listDatasources
├── HealthService.java              # GET /health
└── ...
```

**Typical API Class Structure**:
```java
@Path("/createLayer")
public class CreateLayer {
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createNewLayer(String layerJson) {
        try {
            // 1. Parse JSON
            Layer layer = parseLayerJson(layerJson);
            
            // 2. Validate
            ValidationOutputData validationOutput = validateLayer(layer);
            if (!validationOutput.isSuccess()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(validationOutput).build();
            }
            
            // 3. Business logic
            LayerDAO layerDAO = new LayerDAOImpl();
            layerDAO.addLayerToDB(layer);
            
            // 4. Return response
            return Response.status(Response.Status.CREATED)
                .entity(successResponse).build();
                
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse).build();
        }
    }
}
```

**API Workflow**:
1. Receive HTTP request
2. Parse JSON payload
3. Validate input
4. Call DAO layer for persistence
5. Return JSON response

**When to touch**:
- Adding new API endpoints
- Modifying request/response format
- Changing validation logic
- Adding new query parameters

---

### 8. `utils/` - Utilities and Helpers

**Purpose**: Common utilities used across the application

```
utils/
├── AnalyzerConstants.java          # Constants (status codes, messages)
├── AnalyzerErrorConstants.java     # Error message constants
├── JSONValidator.java              # JSON schema validation
├── KruizeConstants.java            # General constants
├── DateUtils.java                  # Date/time utilities
└── ...
```

**Key Classes**:
- `AnalyzerConstants.java` - All constant values
- `AnalyzerErrorConstants.java` - All error messages
- `JSONValidator.java` - JSON validation logic

**Example Constants**:
```java
public class AnalyzerConstants {
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_BAD_REQUEST = 400;
    
    // Layer constants
    public static final String LAYER_NAME = "layer_name";
    public static final String LAYER_PRESENCE = "layer_presence";
    public static final String TUNABLES = "tunables";
}

public class AnalyzerErrorConstants {
    public static final String LAYER_DUPLICATE_MSG = 
        "Layer already exists with name: %s";
    public static final String LAYER_NAME_NULL_MSG = 
        "Validation failed: layer_name cannot be null or empty";
}
```

**When to touch**:
- Adding new constants
- Adding new error messages
- Adding utility functions

---

## Key Design Patterns

### 1. DAO Pattern (Data Access Object)

```
Service Layer (CreateLayer.java)
    ↓
DAO Interface (LayerDAO.java)
    ↓
DAO Implementation (LayerDAOImpl.java)
    ↓
Database (PostgreSQL)
```

### 2. Validation Pattern

```java
// 1. Validate mandatory fields
if (layer.getLayerName() == null) {
    return createErrorResponse("Layer name is required");
}

// 2. Validate format
if (!isValidLayerName(layer.getLayerName())) {
    return createErrorResponse("Invalid layer name format");
}

// 3. Validate business logic
if (layerAlreadyExists(layer.getLayerName())) {
    return createErrorResponse("Layer already exists");
}
```

### 3. Response Pattern

```java
// Success response
Map<String, Object> response = new HashMap<>();
response.put("status", AnalyzerConstants.SUCCESS);
response.put("message", "Layer created successfully");
response.put("data", layer);
return Response.status(Response.Status.CREATED).entity(response).build();

// Error response
Map<String, Object> errorResponse = new HashMap<>();
errorResponse.put("status", AnalyzerConstants.ERROR);
errorResponse.put("message", errorMessage);
errorResponse.put("httpcode", 400);
return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
```

## Common Code Paths

### Creating a Layer

```
1. POST /createLayer
   ↓
2. CreateLayer.java
   ↓
3. Parse JSON → Layer object
   ↓
4. Validate layer
   ↓
5. LayerDAOImpl.addLayerToDB()
   ↓
6. INSERT INTO kruize_layers
   ↓
7. Return success response
```

### Generating Recommendations

```
1. GET /listRecommendations?experiment_name=X
   ↓
2. ListRecommendations.java
   ↓
3. ExperimentDAO.getExperiment(name)
   ↓
4. Fetch metrics from kruize_results
   ↓
5. Analyzer.generateRecommendations()
   ↓
6. Calculate percentiles (P95, P99)
   ↓
7. Apply cost/performance logic
   ↓
8. PerformanceProfileDAO.saveRecommendations()
   ↓
9. Return recommendations JSON
```

## Coding Conventions

### Naming
- Classes: `PascalCase` (e.g., `CreateLayer.java`)
- Methods: `camelCase` (e.g., `validateLayer()`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `LAYER_NAME_NULL_MSG`)
- Variables: `camelCase` (e.g., `layerName`)

### Packages
- One feature = one package
- DAO classes in `database/dao/`
- Service classes in `service/`
- Models in `common/`

### Error Handling
```java
try {
    // Operation
} catch (SpecificException e) {
    logger.error("Specific error message", e);
    return createErrorResponse(errorMessage);
} catch (Exception e) {
    logger.error("Unexpected error", e);
    return createErrorResponse("Internal server error");
}
```

### Logging
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

logger.info("Layer created: {}", layerName);
logger.error("Failed to create layer: {}", errorMessage, exception);
```

## Building the Project

```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run specific test
mvn test -Dtest=ClassName#methodName

# Package JAR
mvn package
```

## Next Steps

1. Pick a simple API endpoint (e.g., `HealthService.java`) and read through it
2. Follow the code path from API → DAO → Database
3. Make a small change and rebuild
4. Understand one complete feature end-to-end
5. Start contributing!

## Resources

- `/design/` - Detailed design documents
- Java docs (generate with `mvn javadoc:javadoc`)
- Existing code - Best reference for patterns
