# Kruize Runtime & Framework Recommendations

Kruize extends its optimization capabilities past resource right-sizing (CPU/Memory/GPU) to offer **intelligent configurations** for your application's runtime (like JVM) and underlying framework (like Quarkus). By analyzing specific application-level metrics, Kruize ensures your application is optimally tuned. Runtime and Framework Recommendations are only available for container experiments.

---

## Standalone Demo (Quick Start)

For a quick start without a complex setup, new users can utilize Kruize's demos repository. A great first step is the **Runtimes demo**, available in the monitoring section of the demos repo:  
[**Kruize Runtimes Demo**](https://github.com/kruize/kruize-demos/tree/main/monitoring/local_monitoring/runtimes_demo)

---

## Prerequisites: Application Metrics Exposure

For Kruize to generate accurate runtime recommendations, the target application must expose necessary metrics that Kruize can access.

* **Metric Source:** Metrics are collected via Prometheus/Thanos.
* **Metrics Exposure Guide:** Refer to the [metrics guide](application_metrics_exposure.md) for detailed information on how to expose metrics.

---

## 2. Integration and Layer Detection 

To enable Kruize to generate runtime recommendations, including metric and metadata profiles, users must create **"layers"**. Runtime recommendations are then produced based on the identified layers and associated tunable parameters.

Kruize integrates runtime-specific tuning logic automatically by identifying these layers throughout the experiment lifecycle.

### Layer Detection during Experimentation
During the `createExperiment` phase, Kruize examines the specified data source to detect application-specific layers (e.g., Hotspot, Semeru, Quarkus).

### Requirements for Successful Layer Detection
* **Metric Access:** Kruize requires access to a datasource that provides the necessary application-level metrics.
* **Labels:** Alternatively, the presence of relevant labels can facilitate framework detection.

### Datasource Configuration for Runtime Recommendations
* **Minikube / Kind:** The supported datasource is Prometheus (configured by default).
* **OpenShift:** Two datasources are configured by default: Thanos Querier and Prometheus. To receive runtime recommendations, the datasource specified in `createExperiment` should be **“thanos-1”**, which utilizes Thanos Querier and allows access to application metrics.

> **Note:** For details on the Kruize Layer API, architecture, and design, refer to the [Kruize Layer Documentation](../design/KruizeLayers.md#kruize-layer-support).

---

## 3. Supported Tuning Layers

Kruize currently supports tuning for the following layers:

| Layer Type | Supported Stacks | Primary Tunables |
| :--- | :--- | :--- |
| **Runtime** | OpenJDK/Hotspot, IBM Semeru/OpenJ9 | GC Policy, MaxRAMPercentage |
| **Framework** | Quarkus | Quarkus thread-pool cores |

For a complete list of supported layers and their tunables, please refer to the [layer manifests](https://github.com/kruize/autotune/tree/mvp_demo/manifests/autotune/layers).

---

## 4. Accessing Runtime and Framework Recommendations

The JSON output from a call to the `/generateRecommendation` API includes a `runtime_recommendations` section. This section provides specific tunables or environment variables for optimization.

For examples of response structures related to runtimes, please refer to Generate Recommendations API [Example Response](https://github.com/kruize/autotune/blob/mvp_demo/design/KruizeLocalAPI.md#generate-recommendations-api).

---

## 5. Contributing New Layer Support to Kruize

To add a new layer to Kruize, developers must define the layer, identify metrics/parameters, and implement optimization logic.

The essential guide for architecture, steps, interfaces, data structure, and integration is available here:
[How to add support for a new layer](https://github.com/kruize/autotune/blob/mvp_demo/design/KruizeLayers.md#how-to-add-support-for-a-new-layer)
