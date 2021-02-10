

﻿
# Autotune - Installation and Build

- [Installation](#installation)
  - [Kubernetes](#kubernetes)
    - [Minikube](#minikube)
    - [OpenShift](#openshift)

# Installation

## Kubernetes

Autotune can be deployed to a supported Kubernetes cluster. We currently support Minikube and OpenShift.

### Minikube


```
$ ./deploy.sh -c minikube

###   Installing autotune for minikube


Info: Checking pre requisites for minikube...
Info: autotune needs cadvisor/prometheus/grafana to be installed in minikube
Download and install these software to minikube(y/n)? y                     <----- Say yes to install cadvisor/prometheus/grafana
Info: Downloading cadvisor git
...

Info: Downloading prometheus git

Info: Installing prometheus
...

Info: Waiting for all Prometheus Pods to get spawned......done
Info: Waiting for prometheus-k8s-1 to come up...
prometheus-k8s-1                      2/3     Running   0          5s
Info: prometheus-k8s-1 deploy succeeded: Running
prometheus-k8s-1                      2/3     Running   0          6s


Info: One time setup - Create a service account to deploy autotune
serviceaccount/autotune-sa created
clusterrole.rbac.authorization.k8s.io/autotune-cr created
clusterrolebinding.rbac.authorization.k8s.io/autotune-crd created
servicemonitor.monitoring.coreos.com/autotune created
prometheus.monitoring.coreos.com/prometheus created

Info: Deploying autotune yaml to minikube cluster
deployment.apps/autotune created
service/autotune created
Info: Waiting for autotune to come up...
autotune-695c998775-vv4dn               0/1     ContainerCreating   0          4s
autotune-695c998775-vv4dn               1/1     Running   0          9s
Info: autotune deploy succeeded: Running
autotune-695c998775-vv4dn               1/1     Running   0          9s

Info: Access grafana dashboard to see autotune recommendations at http://localhost:3000 <--- Click on this link to access grafana dashboards
Info: Run the following command first to access grafana port
      $ kubectl port-forward -n monitoring grafana-58dc7468d7-rn7nx 3000:3000		<---- But run this command first

```

After the installation completes successfully, run the `port-forward` command as shown. This is needed to access the grafana service. Now click on the [http://localhost:3000](http://localhost:3000) to access grafana. Login as `admin/admin`, navigate to `Create` from the left bar, then click on `Import`. Click on `Upload .json file` and point it to the [dashboard](/grafana/autotune_kubernetes_dashboard.json) file that is part of this repo. Once installed, select `autotune Dashboard`. Select the application name from the `Deployment` drop down and you are all set !

Note: autotune only monitors application with a specific label. See [Add Application Label](#add-application-label) below for more info.

### OpenShift


```
$ ./deploy.sh -c openshift

###   Installing Autotune for OpenShift

WARNING: This will create a Autotune ServiceMonitor object in the openshift-monitoring namespace
WARNING: This is currently not recommended for production

Create ServiceMonitor object and continue installation?(y/n)? y

Info: Checking pre requisites for OpenShift...done
Info: Logging in to OpenShift cluster...
Authentication required for https://aaa.bbb.com:6443 (openshift)
Username: kubeadmin
Password: *****
Login successful.

You have access to 52 projects, the list has been suppressed. You can list all projects with 'oc projects'

Using project "kube-system".

Info: Setting Prometheus URL as https://prometheus-k8s-openshift-monitoring.apps.autotuneshift1.lab.upshift.rdu2.redhat.com
Info: Deploying autotune yaml to OpenShift cluster
Now using project "openshift-monitoring" on server "https://api.autotuneshift1.lab.upshift.rdu2.redhat.com:6443".
deployment.extensions/autotune configured
service/autotune unchanged
Info: Waiting for autotune to come up...
autotune-5cd5967d97-tz2cb                        0/1     ContainerCreating   0          6s
autotune-5cd5967d97-tz2cb                        0/1     ContainerCreating   0          13s
autotune-5cd5967d97-tz2cb                        1/1     Running   0          20s
Info: autotune deploy succeeded: Running
autotune-5cd5967d97-tz2cb                        1/1     Running   0          24s
```

