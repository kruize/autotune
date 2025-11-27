[![Test on Push](https://github.com/kruize/autotune/actions/workflows/test-on-push.yaml/badge.svg?branch=master)](https://github.com/kruize/autotune/actions/workflows/test-on-push.yaml)

# Kruize 🚀 – Intelligent Kubernetes Resource Optimization

Kruize is an open-source optimization tool for Kubernetes that helps you achieve significant cost savings and optimal performance with minimal effort. It continuously monitors your applications and provides right-sizing recommendations for container and namespace resources like CPU and memory, as well as NVIDIA GPU MIG slices.
Kruize serves as the powerful backend engine for the Resource Optimization Service within Red Hat Insights, a service now available to all OpenShift Container Platform (OCP) customers.

## How Kruize Works

Kruize connects to your monitoring stack (such as Prometheus or Thanos) and analyzes historical workload resource usage to generate actionable right-sizing recommendations. By continuously observing real usage patterns, it identifies over- and under-provisioned resources and suggests optimal values to improve performance while reducing costs.

Recommendations are generated for:

- Containers – CPU and Memory requests and limits

- Namespaces – Resource quota limits for CPU and Memory

- NVIDIA GPUs – Optimal MIG slice configurations for supported accelerators (e.g., A100, H100)

Kruize supports both predefined terms (short, medium, long) and custom terms, allowing recommendations to align with your desired observation window. You can also choose between performance-optimized or cost-optimized profiles based on your workload priorities.
## Quick Start

Kruize has a demos repo that enables users to get a quick start without worrying about the setup and its a great first step for first time users. 
You can start by running the [Local Monitoring demo](https://github.com/kruize/kruize-demos/tree/main/monitoring/local_monitoring).

We recommend you check out the [kruize-demos](https://github.com/kruize/kruize-demos) repo in case you want to know more about VPA demo, GPU demo, HPO demo and a lot more!


## Want to Explore Further ?
If you are looking to:

🔧 Deep dive into APIs and integration → Refer to Developer Guide (placeholder)

🧩 Configure layers, tunables, and advanced runtime controls → Refer to Runtime Guide (placeholder)

## Autotune Architecture

See the [Autotune Architecture](/design/README.md) for more details on the architecture.

## Contributing

We welcome your contributions! See [CONTRIBUTING.md](/CONTRIBUTING.md) for more details. 

Join the [Kruize Slack](http://kruizeworkspace.slack.com/) to connect with the community, ask questions, and collaborate!

or Scan the QR Code 
![Slack QR code](./kruize_slack_QR.jpeg)

## License

Apache License 2.0, see [LICENSE](/LICENSE).
