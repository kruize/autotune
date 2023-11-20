# Monitoring Data Check for Recommendations

This script is purpose-built to retrieve monitoring information from a specified workload in a database. It provides
flexibility by supporting two methods: psql for direct database access and GaBi curl for accessing a custom RESTful
service.
The primary goal is to determine if all necessary data points are available to generate meaningful recommendations.

### Usage

`./monitoring_script.sh -P psql -h <optional:host> -p <optional:port> -d <optional:dbname> -U <optional:username>
-W <optional:password> -e <workload_name> -s <optional:monitoring_start_time YYYY-MM-DD HH:MM:SS> -t <optional:
monitoring_end_time YYYY-MM-DD HH:MM:SS> -n <optional:day to debug default 1 days>`

`./monitoring_script.sh -G gabi -h <host> -H <request header> -e <workload_name> -s <optional:monitoring_start_time
YYYY-MM-DD HH:MM:SS> -t <optional:monitoring_end_time YYYY-MM-DD HH:MM:SS> -n <optional:day to debug default 1 days>`

### Objective

The script aims to determine the availability of essential data points within the specified monitoring period, enabling
the generation of valuable recommendations.

### Workflow

1. Command-Line Options:
    - Choose between psql and GaBi methods.
    - Specify database connection details, cluster name, and optional monitoring time frame.

2. Method Selection:
    - Validate that either psql or curl is chosen, not both.
    - Ensure that at least one method is selected.

3. Database Query:
    - Retrieve a list of experiments from the database.

4. Per Experiment Analysis:
    - Determine the time range for monitoring based on the provided parameters.Defaults to the last 24 hours from the
      current date.
    - Query the database for results and recommendations counts.
    - Calculate duration sum and last recommendation date.
    - Identify missing dates during the monitoring period.
    - Generate a JSON object for each experiment.

### Output

Print the final JSON output containing monitoring details.

### Example

`
./kruize_db_metrics.sh -G gabi
-h 'gabiURL' -H 'Authorization: Bearer XYZ' -e "crcs02ue1"
`

`Total 2 experiments found !`

```json
[
  {
    "experiment_name": "14072666|402257|a8c6e3e5-ba47-468b-8329-d983d0d2a929|dynatrace|daemonset|crcs02ue1-oneagent",
    "metrics_available_from_date": "2023-10-09 03:00:01",
    "metrics_available_to_date": "2023-11-19 22:15:00",
    "debug": {
      "for_last": "1 days",
      "from_date": "2023-11-18 22:15:00",
      "to_date": "2023-11-19 22:15:00",
      "results_count": 35,
      "recommendations_count": 1,
      "last_recommendation_date": "2023-11-19T06:00:00Z",
      "duration_sum_minutes/required": "490/1440",
      "missing_dates": [
        "2023-11-18 22:15:00",
        "2023-11-18 22:30:00",
        "2023-11-18 22:45:00",
        "2023-11-18 23:00:00",
        "2023-11-18 23:15:00",
        "2023-11-18 23:30:00",
        "2023-11-18 23:45:00",
        "2023-11-19 00:00:00",
        "2023-11-19 00:15:00",
        "2023-11-19 00:30:00",
        "2023-11-19 00:45:00",
        "2023-11-19 01:00:00",
        "2023-11-19 01:15:00",
        "2023-11-19 01:30:00",
        "2023-11-19 01:45:00",
        "2023-11-19 02:00:00",
        "2023-11-19 02:15:00",
        "2023-11-19 02:30:00",
        "2023-11-19 02:45:00",
        "2023-11-19 03:00:00",
        "2023-11-19 03:15:00",
        "2023-11-19 03:30:00",
        "2023-11-19 03:45:00",
        "2023-11-19 04:00:00",
        "2023-11-19 04:15:00",
        "2023-11-19 04:30:00",
        "2023-11-19 04:45:00",
        "2023-11-19 05:00:00",
        "2023-11-19 05:15:00",
        "2023-11-19 05:30:00",
        "2023-11-19 05:45:00",
        "2023-11-19 06:00:00",
        "2023-11-19 06:15:00",
        "2023-11-19 06:30:00",
        "2023-11-19 06:45:00",
        "2023-11-19 07:00:00",
        "2023-11-19 07:15:00",
        "2023-11-19 07:30:00",
        "2023-11-19 07:45:00",
        "2023-11-19 08:00:00",
        "2023-11-19 08:15:00",
        "2023-11-19 08:30:00",
        "2023-11-19 08:45:00",
        "2023-11-19 09:00:00",
        "2023-11-19 09:15:00",
        "2023-11-19 09:30:00",
        "2023-11-19 09:45:00",
        "2023-11-19 10:00:00",
        "2023-11-19 10:15:00",
        "2023-11-19 10:30:00",
        "2023-11-19 10:45:00",
        "2023-11-19 11:00:00",
        "2023-11-19 11:15:00",
        "2023-11-19 11:30:00",
        "2023-11-19 11:45:00",
        "2023-11-19 12:00:00",
        "2023-11-19 12:15:00",
        "2023-11-19 12:30:00",
        "2023-11-19 12:45:00",
        "2023-11-19 13:00:00",
        "2023-11-19 13:15:00",
        "2023-11-19 13:30:00",
        "2023-11-19 13:45:00",
        "2023-11-19 14:00:00",
        "2023-11-19 14:15:00",
        "2023-11-19 14:30:00",
        "2023-11-19 14:45:00",
        "2023-11-19 15:00:00",
        "2023-11-19 15:15:00",
        "2023-11-19 15:30:00",
        "2023-11-19 15:45:00",
        "2023-11-19 16:00:00",
        "2023-11-19 16:15:00",
        "2023-11-19 16:30:00",
        "2023-11-19 16:45:00",
        "2023-11-19 17:00:00",
        "2023-11-19 17:15:00",
        "2023-11-19 17:30:00",
        "2023-11-19 17:45:00",
        "2023-11-19 18:00:00",
        "2023-11-19 18:15:00",
        "2023-11-19 18:30:00",
        "2023-11-19 18:45:00",
        "2023-11-19 19:00:00",
        "2023-11-19 19:15:00",
        "2023-11-19 19:30:00",
        "2023-11-19 19:45:00",
        "2023-11-19 20:00:00",
        "2023-11-19 20:15:00",
        "2023-11-19 20:30:00",
        "2023-11-19 20:45:00",
        "2023-11-19 21:00:00",
        "2023-11-19 21:15:00",
        "2023-11-19 21:30:00",
        "2023-11-19 21:45:00",
        "2023-11-19 22:00:00"
      ]
    }
  },
  {
    "experiment_name": "14072666|402257|a8c6e3e5-ba47-468b-8329-d983d0d2a929|dynatrace|statefulset|crcs02ue1-activegate",
    "metrics_available_from_date": "2023-10-09 03:00:01",
    "metrics_available_to_date": "2023-11-19 23:15:00",
    "debug": {
      "for_last": "1 days",
      "from_date": "2023-11-18 23:15:00",
      "to_date": "2023-11-19 23:15:00",
      "results_count": 28,
      "recommendations_count": 1,
      "last_recommendation_date": "2023-11-19T06:00:00Z",
      "duration_sum_minutes/required": "392/1440",
      "missing_dates": [
        "2023-11-18 23:15:00",
        "2023-11-18 23:30:00",
        "2023-11-18 23:45:00",
        "2023-11-19 00:00:00",
        "2023-11-19 00:15:00",
        "2023-11-19 00:30:00",
        "2023-11-19 00:45:00",
        "2023-11-19 01:00:00",
        "2023-11-19 01:15:00",
        "2023-11-19 01:30:00",
        "2023-11-19 01:45:00",
        "2023-11-19 02:00:00",
        "2023-11-19 02:15:00",
        "2023-11-19 02:30:00",
        "2023-11-19 02:45:00",
        "2023-11-19 03:00:00",
        "2023-11-19 03:15:00",
        "2023-11-19 03:30:00",
        "2023-11-19 03:45:00",
        "2023-11-19 04:00:00",
        "2023-11-19 04:15:00",
        "2023-11-19 04:30:00",
        "2023-11-19 04:45:00",
        "2023-11-19 05:00:00",
        "2023-11-19 05:15:00",
        "2023-11-19 05:30:00",
        "2023-11-19 05:45:00",
        "2023-11-19 06:00:00",
        "2023-11-19 06:15:00",
        "2023-11-19 06:30:00",
        "2023-11-19 06:45:00",
        "2023-11-19 07:00:00",
        "2023-11-19 07:15:00",
        "2023-11-19 07:30:00",
        "2023-11-19 07:45:00",
        "2023-11-19 08:00:00",
        "2023-11-19 08:15:00",
        "2023-11-19 08:30:00",
        "2023-11-19 08:45:00",
        "2023-11-19 09:00:00",
        "2023-11-19 09:15:00",
        "2023-11-19 09:30:00",
        "2023-11-19 09:45:00",
        "2023-11-19 10:00:00",
        "2023-11-19 10:15:00",
        "2023-11-19 10:30:00",
        "2023-11-19 10:45:00",
        "2023-11-19 11:00:00",
        "2023-11-19 11:15:00",
        "2023-11-19 11:30:00",
        "2023-11-19 11:45:00",
        "2023-11-19 12:00:00",
        "2023-11-19 12:15:00",
        "2023-11-19 12:30:00",
        "2023-11-19 12:45:00",
        "2023-11-19 13:00:00",
        "2023-11-19 13:15:00",
        "2023-11-19 13:30:00",
        "2023-11-19 13:45:00",
        "2023-11-19 14:00:00",
        "2023-11-19 14:15:00",
        "2023-11-19 14:30:00",
        "2023-11-19 14:45:00",
        "2023-11-19 15:00:00",
        "2023-11-19 15:15:00",
        "2023-11-19 15:30:00",
        "2023-11-19 15:45:00",
        "2023-11-19 16:00:00",
        "2023-11-19 16:15:00",
        "2023-11-19 16:30:00",
        "2023-11-19 16:45:00",
        "2023-11-19 17:00:00",
        "2023-11-19 17:15:00",
        "2023-11-19 17:30:00",
        "2023-11-19 17:45:00",
        "2023-11-19 18:00:00",
        "2023-11-19 18:15:00",
        "2023-11-19 18:30:00",
        "2023-11-19 18:45:00",
        "2023-11-19 19:00:00",
        "2023-11-19 19:15:00",
        "2023-11-19 19:30:00",
        "2023-11-19 19:45:00",
        "2023-11-19 20:00:00",
        "2023-11-19 20:15:00",
        "2023-11-19 20:30:00",
        "2023-11-19 20:45:00",
        "2023-11-19 21:00:00",
        "2023-11-19 21:15:00",
        "2023-11-19 21:30:00",
        "2023-11-19 21:45:00",
        "2023-11-19 22:00:00",
        "2023-11-19 22:15:00",
        "2023-11-19 22:30:00",
        "2023-11-19 22:45:00",
        "2023-11-19 23:00:00"
      ]
    }
  },
  {}
]
```



