# Kruize Database integration guide

In this guide, we will walk through the integration steps for deploying a Kruize application with a Postgres database.
We'll cover two approaches: bringing your own Postgres database or installing a Postgres database as a separate
container in the local cluster. Additionally, we'll discuss the importance of using Kubernetes secrets to secure
database credentials and provide some best practices for managing databases in Kubernetes. Let's get started!

**Note :**  For POC purpose db username and password are stored in plain text and will be specified in the YAML as an
environment variable. However, going forward, Kubernetes secrets will be used.

1. Bring Your Own Postgres DB and install Kruize Add a configuration for the database by specifying the database details
   in the YAML file as an environment variable located at minikube ->
   autotune/manifests/crc/BYODB-installation/minikube/kruize-crc-minikube.yaml. openshift ->
   autotune/manifests/crc/BYODB-installation/opensshift/kruize-crc-openshift.yaml Apply the YAML file to the Kubernetes
   cluster using the kubectl apply command


2. Install Postgres DB with Kruize:
   Add a configuration for the database by specifying the database details in the YAML file as an environment variable
   located at minikube ->   autotune/manifests/crc/default-db-included-installation/minikube/kruize-crc-minikube.yaml
   opneshift ->  autotune/manifests/crc/default-db-included-installation/opensshift/kruize-crc-openshift.yaml Apply the
   YAML file to the Kubernetes cluster using the kubectl apply command.
   **Note :**  This approach will install the Postgres database as a separate container in the local cluster and is
   helpful for testing purposes.

That's it! These are the steps to integrate a database into a Kruize deployment. Remember to use Kubernetes secrets
instead of plain text environment variables for secure deployment in production environments.