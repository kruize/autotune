
# Kruize 🚀 – Intelligent Kubernetes Resource Optimization

Kruize is an open-source optimization tool for Kubernetes that helps you achieve significant cost savings and optimal performance with minimal effort. It continuously monitors your applications and provides right-sizing recommendations for container and namespace resources like CPU and memory, as well as NVIDIA GPU MIG slices.
Kruize serves as the powerful backend engine for the Resource Optimization Service within Red Hat Insights, a service now available to all OpenShift Container Platform (OCP) customers.

# How Kruize Works

Kruize analyzes historical resource usage data from a monitoring source like Prometheus to generate its recommendations. It offers suggestions based on predefined terms (short-term, medium-term, long-term) or custom/user defined terms. Kruize also allows you to choose between cost-optimized or performance-optimized profiles for each container.
To help you visualize optimization opportunities, Kruize also provides capacity and utilization data (e.g., as box plots) that clearly show the difference between requested vs. actual resource usage.

# Modes of Operation
Kruize can be run in three distinct modes to fit your operational needs.

## 1. Right-Sizing or Monitoring Mode

In the right-sizing mode, Kruize is connected to a local or a remote data source such as prometheus / thanos and can provide right-sizing recommendations for containers and namespaces by monitoring workloads. Based on historical data, Kruize generates recommendations for:
    - Containers: CPU and Memory requests/limits.
    - NVIDIA GPUs: MIG (Multi-Instance GPU) slice configurations for supported accelerators (e.g., A100, H100).
    - Namespaces: Hard CPU and Memory limits for namespace resource quotas.

a. In Local Monitoring Mode you monitor your applications within your own environment and generate the recommendations by deploying it locally.
b. In Remote Monitoring Mode you everage data gathered from remote clusters to generate resource recommendations. This mode is ideal if you want to centralize monitoring for multiple clusters.

The local and remote monitoring mode can be accessed from [Kruize Demos](https://github.com/kruize/kruize-demos) repo. 

Monitoring mode has been productized and is the backend engine for Resource Optimization Service as part of RH Insights. This service is now available to all OCP customers. Slightly over one third of all OCP customers (~1000) are now using this service. (Tracked through the usage of the Cost Operator)

## 2. Autoscaling Mode 

In this mode, Kruize not only generates recommendations but also applies them automatically.

- VPA Integration: Kruize seamlessly integrates with the Kubernetes Vertical Pod Autoscaler (VPA) to automatically apply its CPU and memory right-sizing recommendations. Kruize creates a custom VPA recommender object and pushes recommendations to this VPA object. This then gets picked up by VPA, which actually applies the recommendations. Kruize supports two modes “auto” and “recreate” that correspond to the modes of VPA with the same names. 

- Instaslice Integration: Kruize uses Instaslice under the covers to apply MIG slicing recommendations for Nvidia accelerators. In “auto” and “recreate” modes, if Kruize detects the presence of GPU metrics for accelerators that support MIG slicing, Kruize generates appropriate MIG slicing recommendations. This gets picked up by Instaslice which will then create the appropriate MIG partitioning scheme on the accelerator and assigns it to the container.

## 3. Autotune

Kruize supports an autotune mode for complex, user-defined performance objectives. In this mode, Kruize runs multiple trials with different tunable configurations for application containers, narrowing them down using Hyperparameter Optimization (HPO) algorithms until it finds the optimal set that meets the user’s Service Level Objective (SLO). It supports a wide range of tunables at the container, runtime (e.g., JVM), and framework levels (e.g., EAP, Quarkus).
Kruize Autotune has been successfully used by performance and scale teams for OS tuning (via Node Tuning Operator profiles), Apache Kafka tuning, Quarkus optimizations, and is now being explored with EAP as part of a sustainability initiative.

## Quick Start

Kruize has a demos repo that enables users to get a quick start without worrying about the setup and its a great first step for first time users. 
You can start by running the [Local Monitoring demo](https://github.com/kruize/kruize-demos/tree/main/monitoring/local_monitoring) or the [Remote Monitoring Demo](https://github.com/kruize/kruize-demos/tree/main/monitoring/remote_monitoring_demo)

We recommend you check out the [kruize-demos](https://github.com/kruize/kruize-demos) repo in case you want to know more about VPA demo, GPU demo, HPO demo and a lot more!

## Installation

Installing kruize on an openshift cluster can be the easiest way to get things working, you simply need to log into the cluster and run the deploy command .

```
./deploy.sh -c openshift -m crc -i <image_name>
```

See the [Autotune Installation](/docs/autotune_install.md) for more details on the installation.

An Operator-based installation is under active development to simplify deployment and management on Kubernetes and OCP.


## REST API

See the [API README](/design/KruizeAPI.md) for more details on the Autotune REST API.

## Autotune Architecture

See the [Autotune Architecture](/design/README.md) for more details on the architecture.

## Contributing

We welcome your contributions! See [CONTRIBUTING.md](/CONTRIBUTING.md) for more details. 

Join the [Kruize Slack](http://kruizeworkspace.slack.com/) to connect with the community, ask questions, and collaborate!

or Scan the QR Code 
![Slack QR code](./kruize_slack_QR.jpeg)

## License

Apache License 2.0, see [LICENSE](/LICENSE).
