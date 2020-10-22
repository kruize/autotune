# Autotune REST API
The Dependency Analyzer REST API design is proposed as follows:

##  listApplications
Get the list of applications monitored by dependency analyzer, along with the layer information.

**Request**
`GET /listApplications`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listApplications`

**Response**

```
[
    {
        "application_name": "app1",
        "runtime" : "java",
        "framework" : "springboot"
    },
    {
        "application_name": "app2",
        "runtime" : "none",
        "framework" : "none"
    }
]
```

##  getAllTunables
Returns the JSON array response containing all the applications along with their tunables for the SLA.

**Request**
`GET /getTunables` gives the tunables for all the applications monitored.

`GET /getTunables?application_name=<APPLICATION_NAME>` for getting the tunables of a specific application.

`GET /getTunables?application_name=<APPLICATION_NAME>&type='container'` for getting tunables of a specific type for the application.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/getTunables?application_name=<APPLICATION_NAME>&type='container'`

**Response**

```
[
    {
        "application_name": "app1",
        "tunables": [
            {
                "type": "container",
                "details": "standard container optimizations",
                "tunables": [
                    {
                        "tunable": "cpuRequest",
                        "details": "CPU Request information",
                        "query": "",
                        "authorization_token": "",
                        "value_type": "double"
                    }
                ]
            },
            {
                "type": "runtime",
                "details": "java",
                "tunables": [
                    {
                        "tunable": "gcPolicy",
                        "query": "",
                        "authorization_token": "",
                        "value_type": "string"
                    }
                ]
            },
            {
                "type": "framework",
                "details": "springboot",
                "tunables": [
                    {
                        "tunable": "numberOfThreads",
                        "details": "springboot threads for the request",
                        "query": "",
                        "authorization_token": "",
                        "value_type": "double"
                    }
                ]
            }
        ]
    }
]
```

##  Health
Get the status of the dependency analyzer.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
UP
```



