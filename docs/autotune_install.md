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

**Install kubectl**

curl -LO "https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl"

```
$ chmod +x ./kubectl
```

```
$ sudo mv ./kubectl /usr/local/bin/kubectl
```

```
$ kubectl version --client
```

**Minikube setup**

Minikube setup with 8 CPUs and 16 GB Memory is recommended for autotune deployment. After setting up minikube, install prometheus from autotune repo with the following command

Only a specific version of minikube v1.26.0 is compatible with prometheus version v0.8.0, hence preferred to install this version on
your machine by:

```
$ curl -LO https://github.com/kubernetes/minikube/releases/download/v1.26.1/minikube-linux-amd64
```

```
$ sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

```
$ minikube start --cpus=8 --memory=16384M
```

```
$ ./scripts/prometheus_on_minikube.sh -as 

    Info: installing prometheus...
    
    Info: Checking pre requisites for prometheus...
    No resources found in monitoring namespace.
    Info: Downloading cadvisor git
    
    Info: Installing cadvisor
    namespace/cadvisor created
    serviceaccount/cadvisor created
    daemonset.apps/cadvisor created
    
    Info: Downloading prometheus git release - v0.8.0
    Info: Installing prometheus
    namespace/monitoring created
    customresourcedefinition.apiextensions.k8s.io/alertmanagerconfigs.monitoring.coreos.com created
    customresourcedefinition.apiextensions.k8s.io/alertmanagers.monitoring.coreos.com created
    Info: Waiting for all Prometheus Pods to get spawned....done
    
    Info: Waiting for prometheus-k8s-1 to come up.....
    prometheus-k8s-1                       0/2     ContainerCreating            0          70s
    prometheus-k8s-1                       0/2     Pending             0          74s
    prometheus-k8s-1                       2/2     Running   1    85s
    Info: prometheus-k8s-1 deploy succeeded: Running
    prometheus-k8s-1                       2/2     Running   1    85s
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

After building the docker image push the image

1.Login into the docker account

Click here to create an account in docker hub [Docker](https://hub.docker.com/)

```
$ docker login
```

```
$ docker tag autotune:test docker.io/username/autotune:test
```

2.Push the Docker image

```
$ docker push username/autotune:test
```

Note - You can use the 'dev friendly mode' option to quickly build the autotune docker image using the cached layers.


# Deploy Autotune

Let us now deploy autotune using the docker image onto the minikube cluster

```
$ ./deploy.sh -c minikube -i autotune:test

  Usage: ./deploy.sh [-a] [-k url] [-c [docker|minikube|openshift]] [-i autotune docker image] [-o hpo docker image] [-n namespace] [-d configmaps-dir ] [--timeout=x, x in seconds, for docker only]
         -s = start(default), -t = terminate
         -s: Deploy autotune [Default]
         -t: Terminate autotune deployment
         -c: kubernetes cluster type. At present we support only minikube [Default - minikube]
         -i: build with specific autotune operator docker image name [Default - kruize/autotune_operator:<version from pom.xml>]
         -o: build with specific hpo docker image name [Default - kruize/hpo:0.0.2]
         -n: Namespace to which autotune is deployed [Default - monitoring namespace for cluster type minikube]
         -d: Config maps directory [Default - manifests/configmaps]
         -m: Target mode selection [autotune | crc]
         -d: Config maps directory [Default - manifests/configmaps]
        Unknown option --help
        
 
  For example,
 
  ./deploy.sh -c minikube -m autotune -i docker.io/<dockerhub username>/autotune:test
    
    Info: Checking pre requisites for minikube...
    Prometheus is installed and running.
    Create autotune namespace monitoring
    Info: One time setup - Create a service account to deploy autotune
    serviceaccount/autotune-sa created
    customresourcedefinition.apiextensions.k8s.io/autotunes.recommender.com created
    customresourcedefinition.apiextensions.k8s.io/autotuneconfigs.recommender.com created
    customresourcedefinition.apiextensions.k8s.io/autotunequeryvariables.recommender.com created
    customresourcedefinition.apiextensions.k8s.io/kruizeperformanceprofiles.recommender.com created
    clusterrole.rbac.authorization.k8s.io/autotune-cr created
    clusterrolebinding.rbac.authorization.k8s.io/autotune-crb created
    clusterrolebinding.rbac.authorization.k8s.io/autotune-prometheus-crb created
    clusterrolebinding.rbac.authorization.k8s.io/autotune-docker-crb created
    clusterrolebinding.rbac.authorization.k8s.io/autotune-scc-crb created
    autotunequeryvariable.recommender.com/minikube created
    servicemonitor.monitoring.coreos.com/autotune created
    prometheus.monitoring.coreos.com/prometheus created
    
    Creating environment variable in minikube cluster using configMap
    configmap/autotune-config created
    
    Deploying AutotuneConfig objects
    kruizelayer.recommender.com/container created
    kruizelayer.recommender.com/hotspot created
    kruizelayer.recommender.com/openj9 created
    kruizelayer.recommender.com/quarkus created
    
    Deploying Performance Profile objects
    kruizeperformanceprofile.recommender.com/resource-optimization-openshift created
    Info: Deploying autotune yaml to minikube cluster
    deployment.apps/autotune created
    service/autotune created
    Info: Waiting for autotune to come up.....
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               4s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               8s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               12s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               17s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               21s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               25s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               29s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               33s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               37s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               41s
    autotune-7fd48dd988-xd6x5              0/2     ContainerCreating   0               45s
    autotune-7fd48dd988-xd6x5              2/2     Running   0               49s
    Info: autotune deploy succeeded: Running
    autotune-7fd48dd988-xd6x5              2/2     Running   0               50s
    
    Info: Access Autotune at http://192.168.49.2:30523/listKruizeTunables
    ```
    
# Demo

Please see the [demo](https://github.com/kruize/kruize-demos) repo for more details on how to use Autotune.
