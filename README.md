
# Kruize Autotune - Autonomous Performance Tuning for Kubernetes !

## What is Kruize Autotune ?

Kruize Autotune is an Autonomous Performance Tuning Tool for Kubernetes. Autotune accepts a user provided "slo" goal to optimize application performance. It uses Prometheus to identify "layers" of an application that it is monitoring and matches tunables from those layers to the user provided slo. It then runs experiments with the help of a hyperparameter optimization framework to arrive at the most optimal values for the identified set of tunables to get a better result for the user provided slo.

Autotune can take an arbitrarily large set of tunables and run experiments to continually optimize the user provided slo in incremental steps. For this reason, it does not necessarily have a "best" value for a set of tunables, only a "better" one than what is currently deployed.

## Motivation

Docker and Kubernetes have become more than buzzwords and are now the defacto building block for any cloud. We are now seeing a major transformation in the industry as every product/solution/offering is being containerized as well as being made kubernetes ready (Hello YAML!). This is throwing up a new set of challenges that are unique to this changing environment.

![Kubernetes Performance Requirement](/docs/autotune-it-admin.png)

Consider an Flight Booking Application as shown in the figure. It consists of a number of microservices which are polyglot in nature and are running in a Kubernetes cluster. Consider a scenario where the user doing a booking is getting a very slow response time. The IT Admin is now tasked to reduce the overall response time for the booking URI.

Tracking down performance issues in a dynamic microservices environment can be challenging and more often than not, stack or runtime specific optimizations are written off as too complex. This is because runtime optimization is a very involved effort and requires deep expertise. Common fixes are mostly limited to increasing pod resources, fixing application logic to make it more optimal or increasing horizontal pod auto-scaling. 

## How do I start ?

Autotune helps to capture your performance tuning needs in a comprehensive way and runs experiments to provide recommendations that help achieve your slo goals. So how does it do it ? We recommend you check out the [kruize-demos](https://github.com/kruize/kruize-demos) repo for a quick start !

## Installation

See the [Autotune Installation](/docs/autotune_install.md) for more details on the installation.

## REST API

See the [API README](/design/API.md) for more details on the Autotune REST API.

## Autotune Architecture

See the [Autotune Architecture](/design/README.md) for more details on the architecture.

## Contributing

We welcome your contributions! See [CONTRIBUTING.md](/CONTRIBUTING.md) for more details.

## License

Apache License 2.0, see [LICENSE](/LICENSE).
