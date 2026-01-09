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

Autotune can be deployed to a supported Kubernetes cluster. We currently support OpenShift, Minikube and Kind.

### Install kubectl and minikube


**Install kubectl**

```
$ curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
```

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

Only a specific version of minikube v1.35.0 is compatible with prometheus version v0.16.0, hence preferred to install this version on
your machine by:

```
$ curl -LO https://github.com/kubernetes/minikube/releases/download/v1.35.0/minikube-linux-amd64
```

```
$ sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

Recommended container runtimes are `containerd` or `cri-o`
```
$ minikube start --container-runtime=containerd/cri-o --cpus=8 --memory=16384M
```

```
$ ./scripts/prometheus_on_minikube.sh -as 

Info: installing prometheus...

Info: Checking pre requisites for prometheus...
No resources found in monitoring namespace.
Info: Downloading cadvisor git

Info: Installing cadvisor
# Warning: 'commonLabels' is deprecated. Please use 'labels' instead. Run 'kustomize edit fix' to update your Kustomization automatically.
namespace/cadvisor created
serviceaccount/cadvisor created
daemonset.apps/cadvisor created

Info: Downloading prometheus git release - v0.16.0

Info: Installing CRDs
customresourcedefinition.apiextensions.k8s.io/alertmanagerconfigs.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/alertmanagers.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/podmonitors.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/probes.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/prometheuses.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/prometheusagents.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/prometheusrules.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/scrapeconfigs.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/servicemonitors.monitoring.coreos.com serverside-applied
customresourcedefinition.apiextensions.k8s.io/thanosrulers.monitoring.coreos.com serverside-applied
namespace/monitoring serverside-applied
customresourcedefinition.apiextensions.k8s.io/servicemonitors.monitoring.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/prometheuses.monitoring.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/alertmanagers.monitoring.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/prometheusrules.monitoring.coreos.com condition met
alertmanager.monitoring.coreos.com/main serverside-applied
networkpolicy.networking.k8s.io/alertmanager-main serverside-applied
poddisruptionbudget.policy/alertmanager-main serverside-applied
prometheusrule.monitoring.coreos.com/alertmanager-main-rules serverside-applied
secret/alertmanager-main serverside-applied
service/alertmanager-main serverside-applied
serviceaccount/alertmanager-main serverside-applied
servicemonitor.monitoring.coreos.com/alertmanager-main serverside-applied
clusterrole.rbac.authorization.k8s.io/blackbox-exporter serverside-applied
clusterrolebinding.rbac.authorization.k8s.io/blackbox-exporter serverside-applied
configmap/blackbox-exporter-configuration serverside-applied
deployment.apps/blackbox-exporter serverside-applied
networkpolicy.networking.k8s.io/blackbox-exporter serverside-applied
service/blackbox-exporter serverside-applied
serviceaccount/blackbox-exporter serverside-applied
servicemonitor.monitoring.coreos.com/blackbox-exporter serverside-applied
secret/grafana-config serverside-applied
secret/grafana-datasources serverside-applied
configmap/grafana-dashboard-alertmanager-overview serverside-applied
configmap/grafana-dashboard-apiserver serverside-applied
configmap/grafana-dashboard-cluster-total serverside-applied
configmap/grafana-dashboard-controller-manager serverside-applied
configmap/grafana-dashboard-grafana-overview serverside-applied
configmap/grafana-dashboard-k8s-resources-cluster serverside-applied
configmap/grafana-dashboard-k8s-resources-multicluster serverside-applied
configmap/grafana-dashboard-k8s-resources-namespace serverside-applied
configmap/grafana-dashboard-k8s-resources-node serverside-applied
configmap/grafana-dashboard-k8s-resources-pod serverside-applied
configmap/grafana-dashboard-k8s-resources-windows-cluster serverside-applied
configmap/grafana-dashboard-k8s-resources-windows-namespace serverside-applied
configmap/grafana-dashboard-k8s-resources-windows-pod serverside-applied
configmap/grafana-dashboard-k8s-resources-workload serverside-applied
configmap/grafana-dashboard-k8s-resources-workloads-namespace serverside-applied
configmap/grafana-dashboard-k8s-windows-cluster-rsrc-use serverside-applied
configmap/grafana-dashboard-k8s-windows-node-rsrc-use serverside-applied
configmap/grafana-dashboard-kubelet serverside-applied
configmap/grafana-dashboard-namespace-by-pod serverside-applied
configmap/grafana-dashboard-namespace-by-workload serverside-applied
configmap/grafana-dashboard-node-cluster-rsrc-use serverside-applied
configmap/grafana-dashboard-node-rsrc-use serverside-applied
configmap/grafana-dashboard-nodes-aix serverside-applied
configmap/grafana-dashboard-nodes-darwin serverside-applied
configmap/grafana-dashboard-nodes serverside-applied
configmap/grafana-dashboard-persistentvolumesusage serverside-applied
configmap/grafana-dashboard-pod-total serverside-applied
configmap/grafana-dashboard-prometheus-remote-write serverside-applied
configmap/grafana-dashboard-prometheus serverside-applied
configmap/grafana-dashboard-proxy serverside-applied
configmap/grafana-dashboard-scheduler serverside-applied
configmap/grafana-dashboard-workload-total serverside-applied
configmap/grafana-dashboards serverside-applied
deployment.apps/grafana serverside-applied
networkpolicy.networking.k8s.io/grafana serverside-applied
prometheusrule.monitoring.coreos.com/grafana-rules serverside-applied
service/grafana serverside-applied
serviceaccount/grafana serverside-applied
servicemonitor.monitoring.coreos.com/grafana serverside-applied
prometheusrule.monitoring.coreos.com/kube-prometheus-rules serverside-applied
clusterrole.rbac.authorization.k8s.io/kube-state-metrics serverside-applied
clusterrolebinding.rbac.authorization.k8s.io/kube-state-metrics serverside-applied
deployment.apps/kube-state-metrics serverside-applied
networkpolicy.networking.k8s.io/kube-state-metrics serverside-applied
prometheusrule.monitoring.coreos.com/kube-state-metrics-rules serverside-applied
Warning: spec.SessionAffinity is ignored for headless services
service/kube-state-metrics serverside-applied
serviceaccount/kube-state-metrics serverside-applied
servicemonitor.monitoring.coreos.com/kube-state-metrics serverside-applied
prometheusrule.monitoring.coreos.com/kubernetes-monitoring-rules serverside-applied
servicemonitor.monitoring.coreos.com/kube-apiserver serverside-applied
servicemonitor.monitoring.coreos.com/coredns serverside-applied
servicemonitor.monitoring.coreos.com/kube-controller-manager serverside-applied
servicemonitor.monitoring.coreos.com/kube-scheduler serverside-applied
servicemonitor.monitoring.coreos.com/kubelet serverside-applied
clusterrole.rbac.authorization.k8s.io/node-exporter serverside-applied
clusterrolebinding.rbac.authorization.k8s.io/node-exporter serverside-applied
daemonset.apps/node-exporter serverside-applied
networkpolicy.networking.k8s.io/node-exporter serverside-applied
prometheusrule.monitoring.coreos.com/node-exporter-rules serverside-applied
service/node-exporter serverside-applied
serviceaccount/node-exporter serverside-applied
servicemonitor.monitoring.coreos.com/node-exporter serverside-applied
clusterrole.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
clusterrolebinding.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
networkpolicy.networking.k8s.io/prometheus-k8s serverside-applied
poddisruptionbudget.policy/prometheus-k8s serverside-applied
prometheus.monitoring.coreos.com/k8s serverside-applied
prometheusrule.monitoring.coreos.com/prometheus-k8s-prometheus-rules serverside-applied
rolebinding.rbac.authorization.k8s.io/prometheus-k8s-config serverside-applied
rolebinding.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
rolebinding.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
rolebinding.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
role.rbac.authorization.k8s.io/prometheus-k8s-config serverside-applied
role.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
role.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
role.rbac.authorization.k8s.io/prometheus-k8s serverside-applied
service/prometheus-k8s serverside-applied
serviceaccount/prometheus-k8s serverside-applied
servicemonitor.monitoring.coreos.com/prometheus-k8s serverside-applied
apiservice.apiregistration.k8s.io/v1beta1.metrics.k8s.io serverside-applied
clusterrole.rbac.authorization.k8s.io/prometheus-adapter serverside-applied
clusterrole.rbac.authorization.k8s.io/system:aggregated-metrics-reader serverside-applied
clusterrolebinding.rbac.authorization.k8s.io/prometheus-adapter serverside-applied
clusterrolebinding.rbac.authorization.k8s.io/resource-metrics:system:auth-delegator serverside-applied
clusterrole.rbac.authorization.k8s.io/resource-metrics-server-resources serverside-applied
configmap/adapter-config serverside-applied
deployment.apps/prometheus-adapter serverside-applied
networkpolicy.networking.k8s.io/prometheus-adapter serverside-applied
poddisruptionbudget.policy/prometheus-adapter serverside-applied
rolebinding.rbac.authorization.k8s.io/resource-metrics-auth-reader serverside-applied
service/prometheus-adapter serverside-applied
serviceaccount/prometheus-adapter serverside-applied
servicemonitor.monitoring.coreos.com/prometheus-adapter serverside-applied
clusterrole.rbac.authorization.k8s.io/prometheus-operator serverside-applied
clusterrolebinding.rbac.authorization.k8s.io/prometheus-operator serverside-applied
deployment.apps/prometheus-operator serverside-applied
networkpolicy.networking.k8s.io/prometheus-operator serverside-applied
prometheusrule.monitoring.coreos.com/prometheus-operator-rules serverside-applied
service/prometheus-operator serverside-applied
serviceaccount/prometheus-operator serverside-applied
servicemonitor.monitoring.coreos.com/prometheus-operator serverside-applied
Info: Waiting for all Prometheus Pods to get spawned.....................done
Info: Waiting for prometheus-k8s-1 to come up.....
Info: prometheus-k8s-1 deploy succeeded: Running
prometheus-k8s-1                       1/2     Running           0          53s


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
$ ./deploy.sh -c minikube -i autotune:test -m crc

  Usage: ./deploy.sh [-a] [-k url] [-c [docker|minikube|openshift]] [-i autotune docker image] [-o hpo docker image] [-n namespace] [-d configmaps-dir ] [--timeout=x, x in seconds, for docker only]
         -s = start(default), -t = terminate
         -s: Deploy autotune [Default]
         -t: Terminate autotune deployment
         -c: kubernetes cluster type. At present we support openshift, minikube and kind [Default - minikube]
         -i: build with specific autotune operator docker image name [Default - kruize/autotune_operator:<version from pom.xml>]
         -o: build with specific hpo docker image name [Default - kruize/hpo:0.0.2]
         -n: Namespace to which autotune is deployed [Default - monitoring namespace for cluster type minikube]
         -d: Config maps directory [Default - manifests/configmaps]
         -m: Target mode selection [autotune | crc]
         -d: Config maps directory [Default - manifests/configmaps]
        Unknown option --help
```       
 
For example,

```
./deploy.sh -c minikube -i docker.io/<dockerhub username>/autotune:test -m crc
    
###   Installing kruize for minikube

use yaml build - 0
clusterrole.rbac.authorization.k8s.io/kruize-recommendation-updater created
clusterrolebinding.rbac.authorization.k8s.io/kruize-recommendation-updater-crb created
clusterrole.rbac.authorization.k8s.io/kruize-edit-ko created
clusterrole.rbac.authorization.k8s.io/instaslices-access created
clusterrolebinding.rbac.authorization.k8s.io/instaslices-access-binding created
clusterrolebinding.rbac.authorization.k8s.io/kruize-edit-ko-binding created
persistentvolume/kruize-db-pv created
persistentvolumeclaim/kruize-db-pvc created
deployment.apps/kruize-db-deployment created
service/kruize-db-service created
configmap/kruizeconfig created
deployment.apps/kruize created
service/kruize created
cronjob.batch/create-partition-cronjob created
servicemonitor.monitoring.coreos.com/kruize-service-monitor created
configmap/nginx-config created
service/kruize-ui-nginx-service created
pod/kruize-ui-nginx-pod created
cronjob.batch/kruize-delete-partition-cronjob created
Info: Waiting for kruize to come up.....
Info: kruize deploy succeeded: Running
kruize-9cf6f5bcb-pr7mg                  1/1     Running   0          11s
kruize-db-deployment-556c9b5d9c-xwzv8   1/1     Running   0          11s
kruize-ui-nginx-pod                     1/1     Running   0          10s 
```
    
# Demo

Please see the [demo](https://github.com/kruize/kruize-demos) repo for more details on how to use Autotune.
