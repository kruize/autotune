# Application Metrics Exposure Guide for Runtime Recommendation Generation

To enable the generation of runtime recommendations, application metrics must be successfully exposed and made available for scraping by Prometheus. This document details the procedures for applications to expose their metrics and for Prometheus to collect them, categorized by application type.

## Prerequisites for Metrics Exposure

* Applications must be configured to expose metrics, typically using **Micrometer or JMX**.
* Prometheus must be able to successfully scrape the designated metrics endpoint.
* In an OpenShift environment, **User Workload Monitoring must be active**.

## Enable Monitoring for User-Defined Projects in OpenShift

By default, OpenShift monitors only cluster components. To collect metrics from user applications, enable user workload monitoring.

### Steps

Edit the `cluster-monitoring-config` ConfigMap:

```
oc -n openshift-monitoring edit configmap cluster-monitoring-config
```
Add or update:
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    enableUserWorkload: true
```
Save and exit.

Monitoring for user workloads will be enabled automatically.Verify
```
oc -n openshift-user-workload-monitoring get pods
```
Ensure the following pods are running:
```
prometheus-operator
prometheus-user-workload
thanos-ruler-user-workload
```
Note: It may take a few minutes for these pods to start.

## Configure Applications to Expose Metrics

Applications can expose metrics in different ways depending on the runtime or framework used.

### Spring Boot Applications

Spring Boot utilizes Micrometer as its metrics interface, exposing data via Spring Boot Actuator with the Prometheus registry.

#### Steps
Add dependencies:
```
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```
Configure application properties:
```
management.endpoints.web.exposure.include=prometheus
management.endpoint.prometheus.enabled=true
```
Metrics endpoint:
```
/actuator/prometheus
```
Prometheus scrape target:
```
http://<host>:<port>/actuator/prometheus
```

### Quarkus Applications

For Quarkus applications, micrometer support is available and can be activated.

#### Steps

Add dependency:
```
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
```
Configure:
```
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.binder.jvm=true
```

Metrics endpoint:
```
/q/metrics
```

Prometheus scrape target:
```
http://<host>:<port>/q/metrics
```

### Plain Java Applications (No Framework)

For Java applications without a framework such as Spring Boot or Quarkus, the recommended way to expose JVM metrics to Prometheus is by using the Prometheus JMX Exporter.
This approach requires no application code changes.

#### Steps:

Run the application with the JMX exporter agent:
```
java -javaagent:/path/to/jmx_prometheus_javaagent.jar=9404:/path/to/config.yml \
-jar your-app.jar
```
Metrics endpoint:
```
/metrics
```
Prometheus scrape target:
```
http://<host>:9404/metrics
```

## Configure Prometheus to Scrape Application Metrics

Prometheus requires a scrape configuration to know where and how to collect metrics from your application endpoints.

Example Scrape Configuration:
```
- job_name: app
  honor_timestamps: true
  scrape_interval: 2s
  scrape_timeout: 1s
  metrics_path: /actuator/prometheus
  scheme: http
  static_configs:
  - targets:
    - app:8081                     
```
#### Configuration Notes:
- `metrics_path` must match your application type:
    - Spring Boot: /actuator/prometheus
    - Quarkus: /q/metrics
    - JMX Exporter (Plain Java): /metrics
- `targets` should be set to your application's network location (host and port).
-  Adjust `scrape_interval` based on your monitoring needs.