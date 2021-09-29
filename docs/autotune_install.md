# Autotune Installation

- [Clone Repositories](#Clone-Repositories)
- [Install the required software](#Install-the-required-software)
- [Build Auotune Docker image](#build-autotune-docker-image)
- [Deploy Auotune using the Docker image](#deploy-autotune)
- [Demo Repo](#demo)


# Clone Repositories

## Clone the Autotune Repository

- Clone the kruize autotune repository onto your workstation with the following command.
```
$ git clone git@github.com:kruize/autotune.git
```

## Clone the Kruize Benchmarks Repository 

- Clone the kruize benchmarks repository onto your workstation with the following command.
```
$ git clone git@github.com:kruize/benchmarks.git
```

# Install the required software

## Kubernetes

Autotune can be deployed to a supported Kubernetes cluster. We currently support Minikube.

### Install kubectl and minikube

Minikube setup with 8 CPUs and 16 GB Memory is recommended for autotune deployment. After setting up minikube, install prometheus from autotune repo with the following command

```
$ ./scripts/prometheus_on_minikube.sh -as 

Info: installing prometheus...

Info: Checking pre requisites for prometheus...
No resources found in monitoring namespace.
Info: Downloading cadvisor git

...

Info: Waiting for prometheus-k8s-1 to come up...
prometheus-k8s-1                       0/2     ContainerCreating   0          102s
prometheus-k8s-1                       1/2     Running   1          106s
Info: prometheus-k8s-1 deploy succeeded: Running
prometheus-k8s-1                       1/2     Running   1          106s
```

# Build autotune docker image

Build autotune docker image 'autotune:test' with the following command

```
$ ./build.sh -t autotune:test

    Usage: $0 [-d] [-v version_string] [-t docker_image_name]
	-d: build in dev friendly mode
	-v: build as specific autotune version
	-t: build with specific docker image name
```

Note - You can use the 'dev friendly mode' option to quickly build the autotune docker image using the cached layers.


# Deploy Autotune

Let us now deploy autotune using the docker image onto the minikube cluster

```
$ ./deploy.sh -c minikube -i autotune:test

Info: Checking pre requisites for minikube...
Prometheus is installed and running.
Info: One time setup - Create a service account to deploy autotune
serviceaccount/autotune-sa created
customresourcedefinition.apiextensions.k8s.io/autotunes.recommender.com created
customresourcedefinition.apiextensions.k8s.io/autotuneconfigs.recommender.com created
customresourcedefinition.apiextensions.k8s.io/autotunequeryvariables.recommender.com created
clusterrole.rbac.authorization.k8s.io/autotune-cr created
clusterrolebinding.rbac.authorization.k8s.io/autotune-crb created
servicemonitor.monitoring.coreos.com/autotune created
prometheus.monitoring.coreos.com/prometheus created

Creating environment variable in minikube cluster using configMap
configmap/autotune-config created

Deploying AutotuneConfig objects
autotuneconfig.recommender.com/container created
autotuneconfig.recommender.com/hotspot created
autotuneconfig.recommender.com/quarkus created

Deploying AutotuneQueryVariable objects
autotunequeryvariable.recommender.com/minikube created
Info: Deploying autotune yaml to minikube cluster
deployment.apps/autotune created
service/autotune created
Info: Waiting for autotune to come up...
autotune-58cf47df84-rhqhx              0/1     ContainerCreating   0          4s
...
autotune-58cf47df84-rhqhx              0/1     ContainerCreating   0          50s
autotune-58cf47df84-rhqhx              1/1     Running   0          54s
Info: autotune deploy succeeded: Running
autotune-58cf47df84-rhqhx              1/1     Running   0          54s

Info: Access Autotune at http://192.168.39.12:30113/listAutotuneTunables
```

# Demo

Please see the [demo](https://github.com/kruize/autotune-demo) repo for more details on how to use Autotune.
