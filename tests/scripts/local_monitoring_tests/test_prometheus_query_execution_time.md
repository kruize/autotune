# Prometheus Time Range script

## Understanding the script flow
This script based on the input parameters runs the set of Prometheus queries, capturing the time taken, status and output of the PromQL queries executed over a specified time range by default over
15 days

### Collection of queries
1. Default queries: Set of queries currently used in Metric profile to capture resource usage data 
2. Individual queries: Set of queries, capturing the container resource data (cpu and memory) by pod with `imageOwners` and `imageWorkloads` queries
3. Grouped queries: Set of queries which group the container data by pod owner and workload on the fly
4. Grouped queries by duration: This collection uses the same set of grouped queries, instead divides the user defined time range into partitions, by default 15 days time range is divided into 3 partitions where each partition corresponding to 5 days of time range
5. Metadata queries: Set of metadata queries used to import datasource metadata 


### Prerequisites:
OpenShift cluster

### Execute the script on OpenShift as:
./test_prometheus_query_execution_time.sh

```
Usage: ./test_prometheus_query_execution_time.sh [-n namespace] [-c container-name] [-q query_set] [-s start_timestamp] [-e end_timestamp] [-d duration in days] [-p no. of partitions] [-a all query sets]
n = namespace
c = container
q = set of queries to be executed for eg. default_queries, individual_queries, grouped_queries_by_owner_workload, grouped_queriesByDuration, metadata_queries
s = start time in epoch
e = end timestamp in epoch, (if start and end timestamp are not specified 15 days is the default time range)
d = duration for equally dividing the time range for eg. dividing 15 days into 5 days duration and executing the grouped_queries
p = partitions in time range for eg. dividing 15 days into 5 days duration with 3 partitions
a = Flag to run all the query sets to capture the time taken

Note: once the query set/sets are executed output will be stored in 
1. prometheus_${QUERY_SET}_${NAMESPACE}_${CONTAINER}_stats.csv - capturing status time taken, start and end time, namespace, container, metric_name and query 
2. ${QUERY_SET}_${NAMESPACE}_${CONTAINER}_response.log - logs the query and query output

In case of running all query sets for all namespaces and containers - along with the respective query set data, consolidated output data will be stored in
1. metric_time_for_all_queries_${NAMESPACE}_${CONTAINER}.csv - this file will contain time taken by each metric for all the query sets for a given namespace and container 
2. total_time_for_all_queries.csv - this file captures total time taken by each query set for all the namespaces and containers
```

To capture time taken to run all the query sets,

```
<KRUIZE_REPO>/tests/scripts/local_monitoring_tests -a 
```

To capture time taken to run all the query sets for all the namespaces and containers present in the cluster

```
<KRUIZE_REPO>/tests/scripts/local_monitoring_tests -A 
```

To capture time taken to run the individual query set for "default" namespace and "app-container" container,

```
<KRUIZE_REPO>/tests/scripts/local_monitoring_tests -n default -c app-container -q individual_queries
```

To capture time taken to run the grouped queries by duration for duration=3days and partitions=5 (dividing 15 days into 5 partitions, querying each partition with 3 days of data)

```
<KRUIZE_REPO>/tests/scripts/local_monitoring_tests -q grouped_queriesByDuration -d 3 -p 5
```

NOTE: In case no namespace and container is specified - long running container and it's namespace is found by the script