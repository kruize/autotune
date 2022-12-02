# Kruize REST API

This article describes how to get started with the Kruize REST API. There are two experiment "modes" in Kruize, the
Monitoring Experiment and the Trial Experiment. The REST APIs are specified for each as below.

* ### Monitor Experiment (mode=monitor)

In this experiment mode, Kruize provides container sizing recommendations (CPU and Memory "request" and "limit") based
on long term monitoring of containers. The containers can either be in a "local" or "remote" cluster. API's
for [mode=monitor]/design/MonitoringModeAPI.md).

* ### Trial Experiment (mode=experiment)

In this experiment mode, Kruize provides container sizing (CPU and Memory "request" and "limit") and optionally,
language runtime configuration (Provided runtime metrics are exposed to Prometheus and are supported by Kruize)
recommendations based on running "trials". The containers can only be in a "local" cluster (as in accessible to Kruize).
API's for [mode=experiment](/design/ExperimentModeAPI.md).

## Health

Get the status of autotune.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```

