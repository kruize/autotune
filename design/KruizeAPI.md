# Kruize REST API
The Kruize REST API design is proposed as follows:

##  Health
Get the status of autotune.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```

## Monitoring mode API

See the [API README](/design/MonitoringModeAPI.md) for more details on the REST API.

## Experiment mode API

See the [API README](/design/ExperimentModeAPI.md) for more details on the REST API.