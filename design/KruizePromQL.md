# Custom Prometheus Queries for Kruize

These are the custom Prometheus queries that you can use while running Kruize. These queries provide valuable insights
into the performance of Kruize APIs and KruizeDB methods.

## KruizeAPI Metrics

The following are the available Kruize APIs that you can monitor:

- `createExperiment` (POST): API for creating a new experiment.
- `listRecommendations` (GET): API for listing recommendations.
- `listExperiments` (GET): API for listing experiments.
- `updateResults` (POST): API for updating experiment results.
- `updateRecommendations` (POST): API for updating recommendations for an experiment.

## Time taken for KruizeAPI metrics

To monitor the performance of these APIs, you can use the following metrics:

- `kruizeAPI_count`: This metric provides the count of invocations for a specific API. It measures how many times the
  API has been called.
- `kruizeAPI_sum`: This metric provides the sum of the time taken by a specific API. It measures the total time consumed
  by the API across all invocations.
- `kruizeAPI_max`: This metric provides the maximum time taken by a specific API. It measures the highest execution time
  observed for the API.

Here are some sample metrics for the mentioned APIs which can run in Prometheus:

- `kruizeAPI_count{api="createExperiment", application="Kruize", method="POST", status="success"}`: Returns the count of
  successful invocations for the `createExperiment` API.
- `kruizeAPI_count{api="createExperiment", application="Kruize", method="POST", status="failure"}`: Returns the count of
  failed invocations for the `createExperiment` API.
- `kruizeAPI_sum{api="createExperiment", application="Kruize", method="POST", status="success"}`: Returns the sum of the
  time taken by the successful invocations of `createExperiment` API.
- `kruizeAPI_max{api="createExperiment", application="Kruize", method="POST", status="success"}`: Returns the maximum
  time taken by the successful invocation of `createExperiment` API.

By changing the value of the `api` and `method` label, you can gather metrics for other Kruize APIs such
as `listRecommendations`, `listExperiments`, and `updateResults`.

Here is a sample command to collect the metric through `curl`

- `curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=kruizeAPI_sum{api="listRecommendations", application="Kruize", method="GET", status="success"}' ${PROMETHEUS_URL} | jq` :
  Returns the sum of the time taken by `listRecommendations` API.

Sample Output:

```
{
  "status": "success",
  "data": {
    "resultType": "vector",
    "result": [
      {
        "metric": {
          "__name__": "kruizeAPI_sum",
          "api": "listRecommendations",
          "application": "Kruize",
          "container": "kruize",
          "endpoint": "kruize-port",
          "instance": "10.129.9.99:8080",
          "job": "kruize",
          "method": "GET",
          "namespace": "openshift-tuning",
          "pod": "kruize-7c97865bbf-tw8zb",
          "prometheus": "openshift-user-workload-monitoring/user-workload",
          "service": "kruize",
          "status": "success"
        },
        "value": [
          1685015801.127,
          "7.626040199"
        ]
      }
    ]
  }
}
```

## KruizeDB Metrics

The following are the available Kruize DB methods that you can monitor:

- `addExperimentToDB`: Method for adding an experiment to the database.
- `addResultToDB`: Method for adding experiment results to the database.
- `addBulkResultsToDBAndFetchFailedResults`: Method for adding bulk experiment results to the database and fetch the
  failed results.
- `addRecommendationToDB`: Method for adding a recommendation to the database.
- `loadExperimentByName`: Method for loading an experiment by name.
- `loadResultsByExperimentName`: Method for loading experiment results by experiment name.
- `loadRecommendationsByExperimentName`: Method for loading recommendations by experiment name.
- `loadRecommendationsByExperimentNameAndDate`: Method for loading recommendations by experiment name and date.
- `addPerformanceProfileToDB`: Method to add performance profile to the database.
- `loadPerformanceProfileByName`: Method to load a specific performance profile.
- `loadAllPerformanceProfiles`: Method to load all performance profiles.

## KruizeMethod Metrics

The following are the available Kruize methods that you can monitor:

- `genratePlots`: methode to generate plots for all terms.

Sample Output:

```
KruizeMethod_max{application="Kruize",method="generatePlots",status="success",} 0.036112854
KruizeMethod_count{application="Kruize",method="generatePlots",status="success",} 2.0
KruizeMethod_sum{application="Kruize",method="generatePlots",status="success",} 0.050705769
```

## Time taken for KruizeDB metrics

To monitor the performance of these methods, you can use the following metrics:

- `kruizeDB_count`: This metric provides the count of calls made to the specific DB method. It measures how many times
  the DB method has been called.
- `kruizeDB_sum`: This metric provides the sum of the time taken by a specific DB method. It measures the total time
  consumed by the DB method across all invocations.
- `kruizeDB_max`: This metric provides the maximum time taken by a specific DB method. It measures the highest execution
  time observed for the DB method.

Here are some sample metrics for the mentioned DB methods which can run in Prometheus:

- `kruizeDB_count{application="Kruize", method="addExperimentToDB", status="success"}`: Number of successful invocations
  of `addExperimentToDB` method.
- `kruizeDB_count{application="Kruize", method="addExperimentToDB", status="failure"}`: Number of failed invocations
  of `addExperimentToDB` method.
- `kruizeDB_sum{application="Kruize", method="addExperimentToDB", status="success"}`: Total time taken by
  the `addExperimentToDB` method which were success.
- `kruizeDB_max{application="Kruize", method="addExperimentToDB", status="success"}`: Maximum time taken by
  the `addExperimentToDB` method which were success.

By changing the value of the `method` label, you can gather metrics for other KruizeDB metrics.

Here is a sample command to collect the metric through `curl`

- `curl --silent -G -kH "Authorization: Bearer ${TOKEN}" --data-urlencode 'query=kruizeDB_sum{application="Kruize", method="loadRecommendationsByExperimentName", status="success"}' ${PROMETHEUS_URL} | jq` :
  Returns the sum of the time taken by `loadRecommendationsByExperimentName` method.

Sample Output:

```
{
  "status": "success",
  "data": {
    "resultType": "vector",
    "result": [
      {
        "metric": {
          "__name__": "kruizeDB_sum",
          "application": "Kruize",
          "container": "kruize",
          "endpoint": "kruize-port",
          "instance": "10.129.9.99:8080",
          "job": "kruize",
          "method": "loadRecommendationsByExperimentName",
          "namespace": "openshift-tuning",
          "pod": "kruize-7c97865bbf-tw8zb",
          "prometheus": "openshift-user-workload-monitoring/user-workload",
          "service": "kruize",
          "status": "success"
        },
        "value": [
          1685016497.066,
          "1.863846208"
        ]
      }
    ]
  }
}
```

> Note: Ensure that you have Prometheus set up and enabled ServiceMonitor to collect these metrics.

# Kruize Metrics Collection and Analysis

To facilitate the performance analysis of the Kruize application, we provide a comprehensive
script, [kruize_metrics.py](../scripts/kruize_metrics.py), which enables the collection of Kruize metrics in CSV format.
This script generates two distinct output files: increase_kruizemetrics.csv and total_kruizemetrics.csv. Notably, the
PostgresDB metrics maintain consistency across both files.

### Output Files and Format

- `increase_kruizemetrics.csv`: This file leverages increase() queries to ascertain the total incremental changes in
  Kruize metric values over time.
- `total_kruizemetrics.csv`: This file employs the original queries to compute cumulative metric values since the
  inception of the Kruize application.

Each column within the CSV files corresponds to specific API and DB metrics, capturing counts, sums, and maximum values
for both successful and failed operations.

### Some key columns for insightful analysis:

| Column Name                                         | Description |
|-----------------------------------------------------| --- |
| timestamp                                           | Represents the timestamp when the metric data was recorded. |
| listRecommendations_count_success                   | Number of successful calls to the listRecommendations API within a predefined time interval (default: 1 hour). |
| listExperiments_count_success                       | Count of successful experiment listings. |
| createExperiment_count_success                      | Number of experiments successfully created. |
| updateResults_count_success                         | Count of successful updates to experiment results. |
| updateRecommendations_count_success                 | Count of successful updates to recommendations. |
| listRecommendations_count_failure                   | Number of failed calls to the listRecommendations API within the specified time interval (default: 1 hour). |
| listExperiments_count_failure                       | Count of failed experiment listings. |
| createExperiment_count_failure                      | Count of failed attempts at creating experiments. |
| updateResults_count_failure                         | Count of failed attempts to update experiment results. |
| updateRecommendations_count_failure                 | Count of failed attempts to update recommendations. |
| createExperiment_sum_success                        | Cumulative sum of successfully created experiments. |
| updateResults_sum_success                           | Cumulative sum of successful updates to experiment results. |
| updateRecommendations_sum_success                   | Cumulative sum of successful updates to recommendations. |
| addResultToDB_sum_success                           | Cumulative sum of successful results added to the database. |
| addBulkResultsToDBAndFetchFailedResults_sum_success | Cumulative sum of successful bulk results added to the database along with failed result fetches. |
| createExperiment_max_success                        | Peak count of successfully created experiments within a specific time interval. |
| updateResults_max_success                           | Peak count of successful updates to experiment results within a specific time interval. |
| updateRecommendations_max_success                   | Peak count of successful updates to recommendations within a specific time interval. |
| kruizeDB_size                                       | Current size of the Kruize database. |
| kruizeDB_results                                    | Total count of results available in the database across all experiments. |

# Initial Analysis Insights

Upon analyzing the collected metrics, several crucial insights emerge:

- `Database Growth`: As the number of experiments and associated results increases, there is a proportional growth in
  the size of the database.

- `Update Recommendations Time`: Currently, the time required for updating recommendations exhibits an increasing trend
  with the growth in results. This aspect necessitates closer attention and potential optimization efforts.

- `Stable Update Results Time`: The time taken for updating experiment results is expected to remain relatively stable.
  Any deviations from this expected pattern warrant further investigation for potential performance issues.

- `DB Method Aggregation`: While individual DB method metrics provide valuable insights, it is important to understand
  how they collectively contribute to the overall API metrics. A comprehensive analysis of both individual and
  aggregated DB metrics is essential for a holistic performance assessment.

- `Max Value Analysis`: Evaluating the maximum values allows for the identification of peak performance periods for each
  method, aiding in the identification of potential performance bottlenecks.

By conducting a thorough analysis based on these initial insights, users can effectively monitor and optimize the
performance of the Kruize application, thereby ensuring a seamless and efficient user experience.
