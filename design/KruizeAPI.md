# Kruize REST API
This article describes how to quickly get started with the Kruize REST API using
The "curl" command. Kruize API is organized around REST. Our API has predictable resource-oriented URLs, accepts form-encoded request bodies, returns JSON-encoded responses, and uses standard HTTP response codes, authentication, and verbs.

## Components
Kurize APIs are designed to work for one or more use cases based on cluster location and mode.

* ### Remote Monitoring Mode
Find a set of [APIs](/design/MonitoringModeAPI.md) that can be used if the cluster is "remote" and the mode is "monitoring".  

* ### Experimental Mode
Find a set of [APIs](/design/ExperimentModeAPI.md) that can be used if the cluster is "local" and the mode is "experiment".

##  Health
Get the status of autotune.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```

