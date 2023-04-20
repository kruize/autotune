# Kruize Autotune Installation

- [Clone Repositories](#Clone-Repositories)
- [Install the required software](#Install-the-required-software)
- [Build Autotune Docker image](#build-autotune-docker-image)
- [Deploy Autotune using the Docker image](#deploy-autotune)
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
$ ./build.sh -i autotune:test


   Usage: ./build.sh [-d] [-v version_string] [-i autotune_docker_image]
	-d: build in dev friendly mode
	-i: build with specific autotune operator docker image name [Default - kruize/autotune_operator:<version from pom.xml>]
	-v: build as specific autotune version

```

Note - You can use the 'dev friendly mode' option to quickly build the autotune docker image using the cached layers.


# Deploy Autotune

Let us now deploy autotune using the docker image onto the minikube cluster

```
$ ./deploy.sh -c minikube -i autotune:test

  Usage: ./deploy.sh [-c [docker|minikube|openshift]] [-i autotune docker image] [-o hpo docker image] [-n namespace] [-d configmaps-dir ] [-s start] [-t terminate]
        -s: Deploy autotune [Default]
        -t: Terminate autotune deployment
        -c: kubernetes cluster type. At present we support only minikube [Default - minikube]
        -i: deploy with specific autotune operator docker image name [Default - kruize/autotune_operator:<version from pom.xml>]
        -o: deploy with specific hpo docker image name [Default - kruize/hpo:<current hpo version>]
        -n: Namespace to which autotune is deployed [Default - monitoring for cluster type minikube]
        -d: Config maps directory [Default - manifests/configmaps]

  For example,
  ./deploy.sh -c minikube -i <docker hub user>/autotune_operator:test -o <docker hub user>/hpo:test


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

Info: Access Autotune at http://192.168.39.12:30113/listKruizeTunables
```

# Demo

Please see the [demo](https://github.com/kruize/kruize-demos) repo for more details on how to use Autotune.
