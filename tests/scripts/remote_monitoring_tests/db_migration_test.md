# **Kruize Remote monitoring DB migration test**

Kruize Remote monitoring DB migration test validates the backward compatibility of Kruize with the previous release.

## Test description

   DB migration test does the following:
   - Deploys the specified previous release of kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Scales kruize replicas to 10
   - Exposes kruize service
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) 
   - Creates around 10 client threads that run in parallel, creating multiple experiments and uploading the metrics results for 15 days
   - Backups up the postgres database
   - Deploys kruize using the specified current release image and restarts the postgres container
   - Restores the data from the backed up postgres file
   - Creates around 10 client threads that run in parallel and uploading the metrics results for 1 day for the same experiments
   - It then invokes update recommendations to check if recommendations are generated using the metrics from last 15 days
  
   DB migration test without postgres restart does the following:
   - Deploys the specified previous release of kruize in non-CRD mode using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Scales kruize replicas to 10
   - Exposes kruize service
   - Creates a resource optimization performance profile using the [createPerformanceProfile API](/design/PerformanceProfileAPI.md) 
   - Creates around 10 client threads that run in parallel, creating multiple experiments and uploading the metrics results for 15 days
   - Restarts only kruize using the specified current release image and doesn't restart postgres container
   - Creates around 10 client threads that run in parallel and uploading the metrics results for 1 day for the same experiments
   - It then invokes update recommendations to check if recommendations are generated using the metrics from last 15 days
  

## Prerequisites for running the tests:
- Openshift cluster
- Tools like kubectl, oc

## How to run the test?

Use the below commands to run the tests:

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/db_migration_test
./db_migration_test.sh [-i previous release Kruize image] [-j current release Kruize image] [-r results directory path] 
```

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/db_migration_test
./db_migration_test_without_postgres_restart.sh [-i previous release Kruize image] [-j current release Kruize image] [-r results directory path] 
```

For example,

```
cd <KRUIZE_REPO>/tests/scripts/remote_monitoring_tests/db_migration_test
./db_migration_test_without_postgres_restart.sh -i quay.io/kruize/autotune_operator:0.0.19.5_mvp -j quay.io/kruize/autotune_operator:0.0.20.1_mvp  -r /tmp/db_migration_results

```

Once the tests are complete, manually check the logs for any exceptions or errors or crashes.

Commands to fetch the count of experiments and results from the DB:

```
kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;"; kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;"; kubectl exec -it `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_recommendations ;"

```
