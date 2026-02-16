# **Kruize Demos test**

Kruize Demos test validates the behaviour of Kruize APIs by running the Kruize demos on openshift, minikube and kind clusters from the [repo](https://github.com/kruize/kruize-demos.git). Refer the documentation in kruize demos repo for details on the demos.

## Tests description
- **Kruize demos test**

   The test does the following:
   - Clones the kruize demos repo
   - Runs the following demos by default:
     - local_monitoring
     - remote_monitoring
     - vpa demo
     - bulk demo
     - runtimes demo (Supported from Kruize release 0.9)
     
   - Validates if recommendations are generated 
  
## Prerequisites for running the tests:
- Openshift cluster access
- Tools like kubectl, oc, curl, jq

## How to run the test?

Use the below command to test :

```
<KRUIZE_REPO>/tests/scripts/kruize_demos_test/kruize_demos_test.sh -c [minikube|kind|openshift] [-i Kruize image] [-o Kruize operator image] [-r results directory path] [ -t <demo> ] [-a Kruize demos git repo URL] [-b Kruize demos branch] [-k]
```

Where values for kruize_demos_test.sh are:
```
Usage: 
        [ -c ] : cluster_type. Supports minikube, kind and openshift cluster-type
        [ -i ] : kruize image. Default - quay.io/kruizehub/autotune-test-image:mvp_demo
        [ -o ] : Kruize operator image. Default - It will use the latest kruize operator image
	[ -a ] : Kruize demos git repo URL. Default - https://github.com/kruize/kruize-demos.git
        [ -b ] : Kruize demos git repo branch. Default - main
        [ -t ] : Kruize demo to run. Default - all (valid values - all/local_monitoring/remote_monitoring/bulk/vpa/runtimes)
        [ -r ] : Kruize results dir path. Default - /tmp/kruize_demos_test_results
        [ -k ] : Disable operator and install kruize using deploy scripts instead
```

For example, to run only the local_monitoring demo on openshift cluster, execute the below command:

```
<AUTOTUNE_REPO>/tests/scripts/kruize_demos_test/kruize_demos_test.sh -c openshift -r /tmp/kruize_demos_test_results -i quay.io/kruize/autotune_operator:0.7.1 -t local_monitoring
```

