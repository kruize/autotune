[![Test on Push](https://github.com/kruize/autotune/actions/workflows/test-on-push.yaml/badge.svg?branch=master)](https://github.com/kruize/autotune/actions/workflows/test-on-push.yaml)

# Kruize 🚀 – Intelligent Kubernetes Resource Optimization

**TL;DR:** <br/>
Kruize analyzes your Kubernetes workload metrics and automatically generates right-sizing recommendations for CPU, memory, and GPU resources — reducing costs and improving performance without manual tuning.
---

Kruize is an open-source optimization tool for Kubernetes that helps you achieve significant cost savings and optimal performance with minimal effort. It continuously monitors your applications and provides right-sizing recommendations for container and namespace resources like CPU and memory, as well as NVIDIA GPU MIG slices.
Kruize serves as the powerful backend engine for the Resource Optimization Service within Red Hat Insights, a service now available to all OpenShift Container Platform (OCP) customers.

## How Kruize Works

Kruize connects to your monitoring stack (such as Prometheus or Thanos) and analyzes historical workload resource usage to generate actionable right-sizing recommendations. By continuously observing real usage patterns, it identifies over- and under-provisioned resources and provides right-sizing recommendations — improving performance for under-provisioned workloads and reducing costs for over-provisioned ones.

Recommendations are generated for:

- Containers – CPU and Memory requests and limits

- Namespaces – Resource quota limits for CPU and Memory

- NVIDIA GPUs – Optimal MIG slice configurations for supported accelerators (e.g., A100, H100)

Kruize supports both predefined terms (short, medium, long) and custom terms, allowing recommendations to align with your desired observation window. You can also choose between performance-optimized or cost-optimized profiles based on your workload priorities.

## Quick Start

Kruize has a demos repo that enables users to get a quick start without worrying about the setup and its a great first step for first time users.
You can start by running the [Local Monitoring demo](https://github.com/kruize/kruize-demos/tree/main/monitoring/local_monitoring).

We recommend you check out the [kruize-demos](https://github.com/kruize/kruize-demos) repo in case you want to know more about VPA demo, GPU demo, HPO demo and a lot more!

## Pre-requisites
You need access to any Kubernetes environment like Kind, Minikube, or OpenShift with Prometheus running in the cluster. 

To install Prometheus use the following scripts for [Kind](/scripts/prometheus_on_kind.sh) or [Minikube](/scripts/prometheus_on_minikube.sh). OpenShift installs prometheus by default.

Follow [benchmarks installation](https://github.com/kruize/benchmarks) instructions to install sysbench benchmark.

## Generating Recommendations with Kruize
This guide provides step-by-step instructions for manual setup. For automated setup, skip to the section below.

### Clone Repositories
Clone the Autotune & Benchmarks Repository using the following commands:

```angular2html
git clone git@github.com:kruize/autotune.git
git clone git@github.com:kruize/benchmarks.git
cd autotune
```

Kruize can be installed on kind, minikube or OpenShift, over here we are using kind to show the installation process.

### Install Kruize

```angular2html
./deploy.sh -c <cluster-type> -m crc
# cluster-type can be: kind, minikube, openshift
```

### Port Forwarding (Kind/Minikube Only)
For Kind and Minikube clusters, you need to set up port forwarding to access Kruize services:

```angular2html
# Kruize API (port 8080)
kubectl port-forward svc/kruize -n monitoring 8080:8080 

# Kruize UI (port 8081)
kubectl port-forward svc/kruize-ui-nginx-service -n monitoring 8081:8081 

# Prometheus (port 9090) - if needed
kubectl port-forward svc/prometheus-k8s -n monitoring 9090:9090 
```
export the Kruize URL 
```angular2html
export KRUIZE_URL="localhost:8080"
export KRUIZE_UI="localhost:8081"
```
### Install Metric and Metadata Profiles
**Metric Profile**: Defines which performance metrics (CPU, memory, etc.) to collect from Prometheus.

Install metric profile

```angular2html
curl -X POST http://${KRUIZE_URL}/createMetricProfile \
  -d @autotune/manifests/autotune/performance-profiles/resource_optimization_local_monitoring.json
```
**Metadata Profile**: Contains queries to collect namespace, workloads and containers data.

Install metadata profile
```angular2html
curl -X POST http://${KRUIZE_URL}/createMetadataProfile \
  -d @autotune/manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json
```

### Import Metadata from Prometheus
Using the Metadata profile queries dsmetadata api will fetch the cluster metadata.

```angular2html
curl --location http://${KRUIZE_URL}/dsmetadata \
  --header 'Content-Type: application/json' \
  --data '{
    "version": "v1.0",
    "datasource_name": "prometheus-1",
    "metadata_profile": "cluster-metadata-local-monitoring",
    "measurement_duration": "15mins"
  }'
```

### Create Experiment

- For container-level experiment

This is the Create Experiment JSON having container related details. 
```angular2html
[{
  "version": "v2.0",
  "experiment_name": "monitor_sysbench",
  "cluster_name": "default",
  "performance_profile": "resource-optimization-local-monitoring",
  "metadata_profile": "cluster-metadata-local-monitoring",
  "mode": "monitor",
  "target_cluster": "local",
  "datasource": "prometheus-1",
  "kubernetes_objects": [
    {
      "type": "deployment",
      "name": "sysbench",
      "namespace": "default",
      "containers": [
        {
          "container_image_name": "quay.io/kruizehub/sysbench",
          "container_name": "sysbench"
        }
      ]
    }
  ],
  "trial_settings": {
    "measurement_duration": "2min"
  },
  "recommendation_settings": {
    "threshold": "0.1"
  }
}]
```
Command to create experiment:
```angular2html
curl -X POST http://${KRUIZE_URL}/createExperiment \
-d @container_experiment_sysbench.json
```
- For namespace-level experiment

In the above json change the experiment name & modify the Kubernetes object to :
```angular2html
"kubernetes_objects": [
      {
        "namespaces": {
          "namespace": "default"
        }
      }
    ]
```
Command to create namespace experiment:
```angular2html
curl -X POST http://${KRUIZE_URL}/createExperiment \
-d @namespace_experiment_sysbench.json
```

### Generate Recommendations
Wait for at least 2 data points to be collected (approx. 30 minutes with default settings), then:

```angular2html
# Generate recommendations for container experiment
curl -X POST "http://${KRUIZE_URL}/generateRecommendations?experiment_name=<experiment-name>"

# List recommendations
curl -X GET "http://${KRUIZE_URL}/listRecommendations?experiment_name=<experiment-name>"
```

You can also take a look at the UI to better understand recommendations 

```angular2html
http://${KRUIZE_UI}
```


## Want to Explore Further ?
If you are looking to:

🔧 Deep dive into APIs and integration → Refer to Developer Guide 

🧩 Configure layers, tunables, and advanced runtime controls → Refer to Runtime Guide

## Autotune Architecture

See the [Autotune Architecture](/design/README.md) for more details on the architecture.

## See Also
If you're exploring more around Kruize, here are related repositories you may find useful:

- [kruize-demos](https://github.com/kruize/kruize-demos) - Demo scripts to showcase Kruize functionality
- [benchmarks](https://github.com/kruize/benchmarks) - Performance benchmarking workloads
- [kruize-operator](https://github.com/kruize/kruize-operator) - Operator for deploying Kruize in your OpenShift or Kubernetes cluster.
- [kruize-website](https://github.com/kruize/kruize-website) - Source code for the official Kruize website
- [kruize-ui](https://github.com/kruize/kruize-ui) - User Interface to interact with Kruize backend
- [hpo](https://github.com/kruize/hpo) - Hyperparameter Optimization framework
- [kruize-vpa](https://github.com/kruize/kruize-vpa) - Vertical Pod Autoscaler integration for Kruize

## Contributing

We welcome your contributions! See [CONTRIBUTING.md](/CONTRIBUTING.md) for more details. 

Join the [Kruize Slack](http://kruizeworkspace.slack.com/) to connect with the community, ask questions, and collaborate!

or Scan the QR Code 
![Slack QR code](docs/images/kruize_slack_QR.jpeg)

## License

Apache License 2.0, see [LICENSE](/LICENSE).
