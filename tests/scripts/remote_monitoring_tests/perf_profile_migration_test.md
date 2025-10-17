# **Kruize Remote monitoring performance Profile migration test**

Kruize Remote monitoring performance profile migration test validates the backward compatibility of Kruize with the previous release after deploying a new kruize release and updating the performance profile with the new version

## Test description

   Performance Profile migration test does the following:
   - Deploys the specified previous release of kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Scales kruize replicas to 10
   - Exposes kruize service
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) with the older performance profile version
   - Creates around 10 client threads that run in parallel, creating multiple experiments and uploading the metrics results for 15 days and generates recommendations
   - Restarts only kruize using the specified current release image and doesn't restart kruize DB container
   - Updates the performance profile with the new performance profile version using the [updatePerformanceProfile API](/design/PerformanceProfileAPI.md)
   - Creates around 10 client threads that run in parallel and uploading the metrics results for 1 day for the same experiments
   - It then invokes update recommendations to check if recommendations are generated using the metrics from last 15 days
  
## Prerequisites for running the tests:
- Openshift cluster
- Tools like kubectl, oc

## How to run the test?

Use the below commands to run the tests:

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/perf_profile_migration_test
./perf_profile_migration_test.sh [-i previous release Kruize image] [-j current release Kruize image] [-r results directory path] 
```

For example,

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/perf_profile_migration_test
./perf_profile_migration_test.sh -i quay.io/kruize/autotune_operator:0.6 -j quay.io/kruize/autotune_operator:0.7.1  -r /tmp/perf_profile_migration_test_results

```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes.

Commands to fetch the count of experiments and results from the DB:

```
kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;"; kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;"; kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_recommendations ;"

```
