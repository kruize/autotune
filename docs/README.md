

﻿
# Autotune - Installation and Build

- [Installation](#installation)
  - [Docker](#docker)
  - [Kubernetes](#kubernetes)
    - [Minikube](#minikube)
    - [OpenShift](#openshift)
  - [Add Application Label](#add-application-label)
  - [Install the Grafana dashboard](#install-the-grafana-dashboard)
  - [Configure Logging Level](#configure-logging-level)
- [Build](#build)

# Installation

## Docker

Developing a microservice on your laptop and want to quickly size the application container using a test load ? Run the Autotune container locally and point it to your application container. Autotune monitors the app container using Prometheus and provides recommendations as a Grafana dashboard (Prometheus and Grafana containers are automatically downloaded when you run autotune).

```
$ ./deploy.sh -c docker

###   Installing autotune for docker...


Info: Checking pre requisites for Docker...
...

Waiting for autotune container to come up
########################     Starting App Monitor loop    #########################
Autotune recommendations available on the grafana dashboard at: http://localhost:3000
Info: Press CTRL-C to exit
 cadvisor: found. Adding to list of containers to be monitored.
 grafana: found. Adding to list of containers to be monitored.
 autotune: found. Adding to list of containers to be monitored.
 prometheus: found. Adding to list of containers to be monitored.


```

Now edit `manifests/docker/autotune-docker.yaml` to add the names of the containers that you need autotune to monitor.

```
$ cat manifests/docker/autotune-docker.yaml 
---
# Add names of the containers that you want autotune to monitor, one per line in double quotes
containers:
  - name: "cadvisor"
  - name: "grafana"
  - name: "autotune"
  - name: "prometheus"
  - name: "acmeair-mono-app1"
  - name: "acmeair-db1"
```

In the above example, autotune is monitoring the application containers `acmeair-mono-app1` and `acmeair-db1` as well as its own set of containers. You should now see the "App Monitor loop" listing the new containers to be monitored

```
 cadvisor: found. Adding to list of containers to be monitored.
 grafana: found. Adding to list of containers to be monitored.
 autotune: found. Adding to list of containers to be monitored.
 prometheus: found. Adding to list of containers to be monitored.
 acmeair-mono-app1: found. Adding to list of containers to be monitored.
 acmeair-db1: found. Adding to list of containers to be monitored.
```

You can now access the grafana dashboard at [http://localhost:3000](http://localhost:3000). Login as `admin/admin` and click on the pre-installed `autotune Dashboard`. Select the application name from the `Deployment` drop down and you are all set !


## Kubernetes

Autotune can be deployed to a supported Kubernetes cluster. We currently support Minikube, IBM Cloud Private (ICP) and OpenShift.

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
clusterrolebinding.rbac.authorization.k8s.io/autotune-crb created
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
Password: 
Login successful.

You have access to 52 projects, the list has been suppressed. You can list all projects with 'oc projects'

Using project "kube-system".

Info: Setting Prometheus URL as https://prometheus-k8s-openshift-monitoring.apps.kaftans.os.fyre.ibm.com
Info: Deploying autotune yaml to OpenShift cluster
Now using project "openshift-monitoring" on server "https://api.kaftans.os.fyre.ibm.com:6443".
deployment.extensions/autotune configured
service/autotune unchanged
Info: Waiting for autotune to come up...
autotune-5cd5967d97-tz2cb                        0/1     ContainerCreating   0          6s
autotune-5cd5967d97-tz2cb                        0/1     ContainerCreating   0          13s
autotune-5cd5967d97-tz2cb                        1/1     Running   0          20s
Info: autotune deploy succeeded: Running
autotune-5cd5967d97-tz2cb                        1/1     Running   0          24s
```

Now you need to install the Autotune Dashboard, see the [Install the Grafana dashboard](#install-the-grafana-dashboard) section for more details. 

Note: OpenShift versions <=4.3 do not support adding additional dashboards to Grafana and in that case visualization through the autotune dashboard is not currently supported. Versions 4.4 onwards, there is a separate Prometheus/Grafana instance that can be deployed and used to monitor applications (currently tech preview only).

Note: Autotune only monitors application with a specific label. See [Add Application Label](#add-application-label) below for more info.

        name: myapp
        app.kubernetes.io/name: "myapp"    <--- Add this label to your app yaml
```

### Add Application Label

Autotune is now ready to monitor applications in your cluster ! Note however that autotune only monitors applications with the label `app.kubernetes.io/name: "myapp"` currently.
```
spec:
  replicas: 1
  template:
    metadata:
      labels:
        app: myapp
        name: myapp
        app.kubernetes.io/name: "myapp"    <--- Add this label to your app yaml
```

### Install the Grafana dashboard

Login to your Grafana dashboard and navigate to `Create` from the left bar, then click on `Import`. Click on `Upload .json file` and point it to the [dashboard](/grafana/autotune_kubernetes_dashboard.json) file that is part of this repo.

![Import dashboard into Grafana](/docs/grafana-import.png)

Once imported, the grafana dashboard should look something like this.

![autotune Grafana Dashboard](/docs/grafana-dash.png)

Once installed, select `autotune Dashboard`. Select the application name from the `Deployment` drop down and you are all set !

### Configure Logging Level

Autotune uses slf4j and the log4j-slf4j binding for its logging. The log levels used are:

| Logging Level | Description                                                                                                                         |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `ERROR`       | Error events that stop application from running correctly.                                                                          |
| `WARN`        | Designates potentially harmful situations. Includes `ERROR` logs.                                                                   |
| `INFO`        | Informational messages that highlight the progress of the application. Includes `ERROR` and `WARN` logs. The default logging level. |
| `DEBUG`       | Designates fine-grained informational events that are most useful to debug an application. Includes logs from `INFO` level.         |
| `ALL`         | Turn on all logging.                                                                                                                |

By default, the log level is set to `INFO`. To change the logging level, set the ENV `LOGGING_LEVEL` in `manifests/autotune.yaml_template` to any of the above levels. 


```
        - name: MONITORING_AGENT
          value: "prometheus"
        - name: MONITORING_SERVICE
          value: "{{ MONITORING_SERVICE }}"
        - name: LOGGING_LEVEL
          value: "INFO"          <--- Change this to any of the above levels
```

While submitting issues, we recommend users to attach the logs with log level set to `DEBUG`.

## Building autotune

```
$ ./build.sh
```
Tag it appropriately and push it to a docker registry that is accessible to the kubernetes cluster. Don't forget to update the manifest yaml's to point to your newly built image !

