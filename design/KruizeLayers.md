# Kruize Layer Support

## Overview

Kruize supports a **layered optimization model** 

### Definition: Layer

A Layer is any part of the software stack packaged within a container running in Kubernetes that is identifiable, traceable, and tunable.

**Examples of layers include:**
- Container runtime environment
- Operating system components
- Language runtimes (JVM, Node.js, Python, Go)
- Application frameworks (Quarkus, EAP, Liberty, Spring, PyTorch)
- Supporting frameworks and libraries (Hibernate, Vert.x, etc.)

Each layer:

- Defines tunable parameters
- Specifies how to detect layer presence
- Declares dependencies between tunables across layers
- Is registered in the analyzer backend for runtime processing

Layers can be created dynamically via API, but they must also have backend support to define their tuning logic and dependencies.

---

## Layer Architecture

A complete layer implementation in Kruize consists of the following parts.

### 1. Layer API Representation

This defines how a layer is represented and created through the API.

**Class:** `KruizeLayer`

It includes:

| Field | Description |
|---|---|
| apiVersion | API version |
| kind | Resource type |
| metadata | Layer metadata |
| layer_name | Name of the layer |
| layer_level | Hierarchy level |
| details | Description of the layer |
| layer_presence | Logic to detect layer presence |
| tunables | Tunable parameters for the layer |

This object defines the configuration model for a layer.

---

### 2. Backend Layer Implementation

Each layer must have a backend implementation that:

- Implements the `Layer` interface
- Defines tunable dependencies
- Provides layer-specific behavior

Example: `HotspotLayer`

Responsibilities:

- Singleton instance of layer
- Layer name resolution
- Tunable dependency mapping

Example dependency structure:

```java
Map<String, List<TunableSpec>>
```

This describes which tunables depend on which tunables from other layers.

Example meaning:

- MAX_RAM_PERC depends on container MEMORY_LIMIT
- GC_POLICY depends on container CPU and memory

This enables cross-layer tuning intelligence.

---

### 3. Layer Constants

Every layer must be declared as a supported layer.

Located in:

```java
AnalyzerConstants.LayerConstants

public static final String CONTAINER_LAYER = "container";
public static final String HOTSPOT_LAYER = "hotspot";
public static final String QUARKUS_LAYER = "quarkus";
public static final String SEMERU_LAYER = "semeru";

public static final List<String> SUPPORTED_LAYERS = Arrays.asList(
        CONTAINER_LAYER,
        HOTSPOT_LAYER,
        QUARKUS_LAYER,
        SEMERU_LAYER
);
```

Purpose:

- Defines canonical layer names
- Ensures validation and recognition across the analyzer

---

### 4. Layer Registry

All backend layer implementations must be registered in the `LayerRegistry`.

This acts as:

- Central runtime lookup
- Singleton registry of supported layers
- Dependency resolution entry point

Example registrations:
```java
registerLayer(ContainerLayer.getInstance());
registerLayer(HotspotLayer.getInstance());
registerLayer(QuarkusLayer.getInstance());
registerLayer(SemeruLayer.getInstance());
```

The registry maps:

layer_name → Layer implementation


---

## Existing Supported Layers

Kruize currently supports the following layers.

### Infrastructure Layer
- Container Layer  
  Represents container runtime configuration and provides CPU and memory limits.

### Runtime Layers
- Hotspot Layer  
  JVM Hotspot runtime tuning with dependencies on container resources.

- Semeru Layer  
  IBM Semeru JVM runtime tuning.

### Framework Layer
- Quarkus Layer  
  Framework-level optimization.

These layers form a hierarchical tuning model:

Infrastructure → Runtime → Middleware → Framework

Currently no middleware layers are supported, if you are planning to add a middleware layer, Please add in `KruizeLayer.impl.middleware` package


---

## How to Add Support for a New Layer

To introduce a new layer, both API configuration support and backend runtime support must be implemented.

Follow the steps below.

---

### Step 1 — Define Layer in API

Create a `KruizeLayer` definition through the API including:

- Layer name
- Level
- Presence detection logic
- Tunables

No code change required as API already supports generic layer creation.

---

### Step 2 — Implement Backend Layer Class

Create a class implementing:

```text

Recommended package structure:

impl.infra
impl.runtime
impl.middleware
impl.framework
```
depending on layer type.

#### Requirements

- Singleton pattern
- Layer name definition
- Tunable dependency mapping

#### Example Template
```java
public class MyLayer implements Layer {
    private static final MyLayer INSTANCE = new MyLayer();

    private MyLayer() {
    }

    public static MyLayer getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return AnalyzerConstants.LayerConstants.MY_LAYER;
    }

    @Override
    public Map<String, List<TunableSpec>> getTunableDependencies() {
        return Map.of(
                "my_tunable",
                List.of(
                        new TunableSpec(
                                AnalyzerConstants.LayerConstants.CONTAINER_LAYER,
                                "memoryLimit"
                        )
                )
        );
    }
}
```

---

### Step 3 — Add Layer Constant

Add layer name constant in AnalyserConstants

```java
public static final String MY_LAYER = "my_layer";

// Add layer to supported layers
public static final List<String> SUPPORTED_LAYERS = Arrays.asList(
        CONTAINER_LAYER,
        HOTSPOT_LAYER,
        QUARKUS_LAYER,
        SEMERU_LAYER,
        MY_LAYER // New Layer
);
```

---

### Step 4 — Register Layer in LayerRegistry

Register singleton instance.

File: LayerRegistry.java

```java
registerLayer(MyLayer.getInstance());
```

This enables runtime discovery.

---

### Step 5 — Define Tunables and Dependencies

Ensure:

- Tunables are defined in API layer definition
- Dependencies reference valid layer and tunable pairs
- Dependency graph is consistent

---

## Validation Checklist

Before using a new layer, verify:

- Layer created via API
- Backend class implemented
- Constant added
- Added to SUPPORTED_LAYERS
- Registered in LayerRegistry
- Tunable dependencies defined
- Layer name matches across all components

---

## Runtime Flow

1. Layer defined via API
2. Analyzer loads backend layer implementation
3. Registry resolves layer
4. Tunable dependencies evaluated
5. Recommendation engine generates recommendations




