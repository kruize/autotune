
# Kruize Autotune - Autonomous Performance Tuning for Kubernetes !

## What is Kruize Autotune ?

Kruize Autotune is an Autonomous Performance Tuning Tool for Kubernetes. Autotune accepts a user provided "slo" goal to optimize application performance. It uses Prometheus to identify "layers" of an application that it is monitoring and matches tunables from those layers to the user provided slo. It then runs experiments with the help of a hyperparameter optimization framework to arrive at the most optimal values for the identified set of tunables to get a better result for the user provided slo.

Autotune can take an arbitrarily large set of tunables and run experiments to continually optimize the user provided slo in incremental steps. For this reason, it does not necessarily have a "best" value for a set of tunables, only a "better" one than what is currently deployed.

## Motivation

Docker and Kubernetes have become more than buzzwords and are now the defacto building block for any cloud. We are now seeing a major transformation in the industry as every product/solution/offering is being containerized as well as being made kubernetes ready (Hello YAML!). This is throwing up a new set of challenges that are unique to this changing environment.

![Kubernetes Performance Requirement](/docs/autotune-it-admin.png)

Consider an Flight Booking Application as shown in the figure. It consists of a number of microservices which are polyglot in nature and are running in a Kubernetes cluster. Consider a scenario where the user doing a booking is getting a very slow response time. The IT Admin is now tasked to reduce the overall response time for the booking URI.

Tracking down performance issues in a dynamic microservices environment can be challenging and more often than not, stack or runtime specific optimizations are written off as too complex. This is because runtime optimization is a very involved effort and requires deep expertise. Common fixes are mostly limited to increasing pod resources, fixing application logic to make it more optimal or increasing horizontal pod auto-scaling. 

## Usecases
Kruize will support the following use cases

**Use Case 1:** **Autotune - Config Recommendation for a User Provided Performance Objective** 

In this use case, the user provides a deployment name and a performance objective. The performance objective consists of metrics that either needs to be maximized or minimized.

The goal is to find the config supplied by HPO that best optimizes the user provided objective. The config can consist of both resource requests and limits and Runtime parameters. A config recommendation will be provided at the end of the experiment.

**Use Case 2:** **Hyper Parameter Optimization as a Service - Performance**

In this use case, provide HPO as an independent entity that can be used outside of Kubernetes. This is very useful for arriving at OS tuning (RHEL Performance Profiles, TuneD and NTO), Tuning OpenShift itself, Thresholds for perf tests in CI/CD pipelines etc (as part of Integrated Performance Threshold Testing - IPT)

**Use Case 3:** **Monitor a deployment over a long term and provide recommendations to help reduce cost (Production scenario with no experiment trials)** 

1.Remote 

2.Local 

In this use case, the SRE will provide a deployment that needs to be monitored over a long period of time. The intention is to better understand the variance of the incoming load conditions and provide recommendations on the container and heap sizing in an effort to reduce costs.

**Use Case 4:** **Provide tooling for automating performance regressions instead of manual testing (A/B/n testing as part of the build pipeline)**

In this use case, the user provides a deployment name, two docker images and metrics to be monitored during the course of the experiment. The goal is to deploy the docker images in two separate trials respectively, one after the other.

It will monitor the given metrics during both the trials and report the results at the end of each trial. The results from each of the trials can then be used to compare the two trials and determine if there has been a variation in the performance objective and the quantum of the same.

For more information refer the file [USECASES](USECASES.md)

## How do I start ?

Autotune helps to capture your performance tuning needs in a comprehensive way and runs experiments to provide recommendations that help achieve your slo goals. So how does it do it ? We recommend you check out the [kruize-demos](https://github.com/kruize/kruize-demos) repo for a quick start !

## Installation

See the [Autotune Installation](/docs/autotune_install.md) for more details on the installation.

## REST API

See the [API README](/design/KruizeAPI.md) for more details on the Autotune REST API.

## Autotune Architecture

See the [Autotune Architecture](/design/README.md) for more details on the architecture.

## Contributing

We welcome your contributions! See [CONTRIBUTING.md](/CONTRIBUTING.md) for more details.

## License

Apache License 2.0, see [LICENSE](/LICENSE).
