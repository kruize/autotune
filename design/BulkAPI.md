# Bulk API Documentation

Bulk is an API designed to provide resource optimization recommendations in bulk for all available
containers, namespaces, etc., for a cluster connected via the datasource integration framework. Bulk can
be configured using filters like exclude/include namespaces, workloads, containers, or labels for generating
recommendations. It also has settings to generate recommendations at both the container or namespace level, or both.

Bulk returns a `jobID` as a response to track the job status. The user can use the `jobID` to monitor the
progress of the job.

## Task Flow When Bulk Is Invoked

1. Returns a unique `jobID`.
2. Background Bulk:
    - First, does a handshake with the datasource.
    - Using queries, it fetches the list of namespaces, workloads, containers of the connected datasource.
    - Creates experiments, one for each container *alpha release.
    - Triggers `generateRecommendations` for each container.
    - Once all experiments are created, and recommendations are generated, the system marks the `jobID` as "COMPLETED".

## API Specification

### POST /bulk

**Request Payload (JSON):**

```json
{
  "filter": {
    "exclude": {
      "namespace": [],
      "workload": [],
      "containers": [],
      "labels": {}
    },
    "include": {
      "namespace": [],
      "workload": [],
      "containers": [],
      "labels": {
        "key1": "value1",
        "key2": "value2"
      }
    }
  },
  "time_range": {},
  "datasource": "Cbank1Xyz",
  "experiment_types": [
    "container",
    "namespace"
  ]
}
```

**filter:** This object contains both exclusion and inclusion filters to specify the scope of data being queried.

- **exclude:** Defines the criteria to exclude certain data.
    - **namespace:** A list of Kubernetes namespaces to exclude. If empty, no namespaces are excluded.
    - **workload:** A list of workloads to exclude.
    - **containers:** A list of container names to exclude.
    - **labels:** Key-value pairs of labels to exclude.

- **include:** Defines the criteria to include specific data.
    - **namespace:** A list of Kubernetes namespaces to include.
    - **workload:** A list of workloads to include.
    - **containers:** A list of container names to include.
    - **labels:** Key-value pairs of labels to include.

- **time_range:** Specifies the time range for querying the data. If empty, no specific time range is applied.

- **datasource:** The data source, e.g., `"Cbank1Xyz"`.

- **experiment_types:** Specifies the type(s) of experiments to run, e.g., `"container"` or `"namespace"`.

### Success Response

- **Status:** 200 OK
- **Body:**

```json
{
  "jobid": "123e4567-e89b-12d3-a456-426614174000"
}
```

### GET Request:

```bash
GET /bulk?jobid=123e4567-e89b-12d3-a456-426614174000
```

**Body (JSON):**

```json
{
  "jobID": "123e4567-e89b-12d3-a456-426614174000",
  "status": "IN-PROGRESS",
  "progress": 30,
  "data": {
    "experiments": {
      "new": [
        "a",
        "b",
        "c"
      ],
      "updated": [],
      "failed": []
    },
    "recommendations": {
      "count": 9,
      "completed": 3,
      "experiments": {
        "completed": [
          "exp1",
          "exp2",
          "exp3"
        ],
        "progress": [
          "exp1",
          "exp2",
          "exp3"
        ],
        "new": [
          "exp1",
          "exp2",
          "exp3"
        ],
        "failed": []
      }
    }
  },
  "job_start_time": "2024-09-23T10:58:47.048Z",
  "job_end_time": "2024-09-23T11:01:52.205Z"
}
```

### Response Parameters

- **jobID:** Unique identifier for the job.
- **status:** Current status of the job. Possible values: `"IN-PROGRESS"`, `"COMPLETED"`, `"FAILED"`.
- **progress:** Percentage of job completion.
- **data:** Contains detailed information about the experiments and recommendations.
    - **experiments:** Tracks the status of experiments.
        - **new:** List of newly created experiments.
        - **updated:** List of updated experiments.
        - **failed:** List of experiments that failed.
    - **recommendations:** Provides details on recommendations.
        - **count:** Total number of recommendations.
        - **completed:** Number of completed recommendations.
        - **experiments:**
            - **completed:** List of experiments with completed recommendations.
            - **progress:** List of experiments in progress.
            - **new:** List of new experiments.
            - **failed:** List of failed experiments.
- **job_start_time:** Timestamp indicating when the job started.
- **job_end_time:** Timestamp indicating when the job finished.
