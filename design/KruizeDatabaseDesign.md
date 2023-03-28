# **Database Design Document for Kruize**

## **Introduction**

---

The purpose of this document is to describe the database design for the Kruize Autotune project. The Kruize Autotune
project aims to provide an API to manage experiments by creating,updating and deleting. An api to receive frequent
metrics results to generate recommendations.

## **Entities**

---


The Kruize Autotune project has the following entities:

1. kruize_experiment
2. kruize_results
3. recommendations
4. performance_profiles

## **Kruize**

---

The kruize_experiment entity represents a single experiment that is executed to optimize application resources on
Kubernetes. The experiment contains the following attributes:


<table>
  <tr>
   <td><strong>Attribute</strong>
   </td>
   <td><strong>Type</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>version
   </td>
   <td>string
   </td>
   <td>The version of the create experiment json template.
   </td>
  </tr>
  <tr>
   <td>experiment_name
   </td>
   <td>string
   </td>
   <td>The name of the experiment. This is a unique identifier for the experiment.
   </td>
  </tr>
 <tr>
   <td>experiment_id
   </td>
   <td>string
   </td>
   <td>Primary key.
   </td>
  </tr>
  <tr>
   <td>status
   </td>
   <td>string
   </td>
   <td>The status of the experiment. This can be "running",”pause”, "completed", or "failed". 
   </td>
  </tr>
  <tr>
   <td>cluster_name
   </td>
   <td>string
   </td>
   <td>The name of the Kubernetes cluster on which the experiment is executed.
   </td>
  </tr>
  <tr>
   <td>performance_profile
   </td>
   <td>string
   </td>
   <td>The name of the performance profile used for the experiment.
   </td>
  </tr>
  <tr>
   <td>mode
   </td>
   <td>string
   </td>
   <td>The mode in which the experiment is executed. This can be "monitor" or "experiment".
   </td>
  </tr>
  <tr>
   <td>target_cluster
   </td>
   <td>string
   </td>
   <td>The target Kubernetes cluster for the application. This can be "local" or "remote".
   </td>
  </tr>
  <tr>
   <td>datasource
   </td>
   <td>array of json
   </td>
   <td>The data sources used to fetch metrics results.
   </td>
  </tr>
  <tr>
   <td>extended_data
   </td>
   <td>json
   </td>
   <td>Additional data for the experiment in JSON format, which includes complete Kubernetes objects details, recommendation settings, trial settings, and ACM additional settings.
   </td>
  </tr>
  <tr>
   <td>meta_data
   </td>
   <td>json
   </td>
   <td>Additional metadata for the experiment in JSON format.
   </td>
  </tr>
</table>

### Kruize entity related API's

The following REST API's are available for the kruize_experiment entity:

#### **1. CreateExperiment**

This API creates a new experiment using POST method and JSON data. The following attributes are required to create an
experiment record:

* version
* experiment_name
* cluster_name
* performance_profile
* mode
* target_cluster
* datasource
* extended_data

##### Request Method

HTTP_METHODE: POST

##### Rest API Curl

```
 curl --location --request POST 'http://127.0.0.1:8080/createExperiment' \
			--header 'Content-Type: application/json' \
			--data-raw '[
			    {
				"version": "1.0",
				"experiment_name": "quarkus-resteasy-autotune-min-http-response-DB10",
				"cluster_name": "cluster-one-division-bell",
				"performance_profile": "resource-optimization-openshift",
				"mode": "monitor",
				"target_cluster": "remote",
				"kubernetes_objects": [
				    {
					"type": "deployment",
					"name": "tfb-qrh-deployment",
					"namespace": "default",
					"containers": [
					    {
						"container_image_name": "kruize/tfb-db:1.15",
						"container_name": "tfb-server-0"
					    },
					    {
						"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
						"container_name": "tfb-server-1"
					    }
					]
				    }
				],
				"trial_settings": {
				    "measurement_duration": "15min"
				},
				"recommendation_settings": {
				    "threshold": "0.1"
				}
			    }
			]'

```

##### Db query

```
insert into kruize_experiment (cluster_name, experiment_name, extended_data, meta_data, mode,  performance_profile, status, target_cluster, datasource, version) values (?, ?, ?, ?, ?, ?, ?, ?, ?)

```

#### **2. UpdateExperiment**

The purpose of this API is to facilitate the modification of various aspects of an experiment, such as replacing
Kubernetes objects, adding or removing deployment/statefulset types, adding or removing containers, updating the
experiment's status, and adjusting settings such as trial and recommendation settings.

##### Request Method

HTTP_METHODE: POST

##### Rest API Curl

###### add new container

```
curl --location --request POST 'http://127.0.0.1:8080/updateExperiment' \
			--header 'Content-Type: application/json' \
			--data-raw '[
			    {
				"version": "1.0",
				"experiment_name": "quarkus-resteasy-autotune-min-http-response-DB10",
				"kubernetes_objects": [
				    {
					"type": "deployment",
					"name": "tfb-qrh-deployment",
					"namespace": "default",
					"containers": [
					    {
						"container_image_name": "kruize/tfb-db:1.15",
						"container_name": "tfb-server-0"
					    },
					    {
						"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
						"container_name": "tfb-server-1"
					    },
					    {
						"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
						"container_name": "tfb-server-2"
					    }
					]
				    }
				]
			    }
			]'  	
```

###### delete container:

```
	curl --location --request POST 'http://127.0.0.1:8080/updateExperiment' \
			--header 'Content-Type: application/json' \
			--data-raw '[
			    {
				"version": "1.0",
				"experiment_name": "quarkus-resteasy-autotune-min-http-response-DB10",
				"kubernetes_objects": [
				    {
					"type": "deployment",
					"name": "tfb-qrh-deployment",
					"namespace": "default",
					"containers": [
					    {
						"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
						"container_name": "tfb-server-1"
					    },
					    {
						"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
						"container_name": "tfb-server-2"
					    }
					]
				    }
				]
			    }
			]'
```

###### Update status:

```
	curl --location --request POST 'http://127.0.0.1:8080/updateExperiment' \
			--header 'Content-Type: application/json' \
			--data-raw '[
			    {
				"version": "1.0",
				"experiment_name": "quarkus-resteasy-autotune-min-http-response-DB10",
				"status" : Pause/Run
			    }
			]'  

```

###### DB Query

```
update kruize_experiment set extended_data = extended_data ||
				'{"kubernetes_objects" : [above data will get replaced ] }'

Where experiment_name= '';


update kruize_experiment set status='pause' where experiment_name=?
```

#### **3. DeleteExperiment**

Delete experiment and related results and recommendation.

##### Request Method

HTTP_METHODE: DELETE

##### Rest API Curl

```
  curl --location --request delete 'http://127.0.0.1:8080/deleteExperiment' \
			--header 'Content-Type: application/json' \
			--data-raw '[
			    {
				"version": "1.0",
				"experiment_name": "quarkus-resteasy-autotune-min-http-response-DB10"
                            }
                          ]
```

##### DB query

```
Delete from kruize_experiment where experiment_name=
Delete from kruize_results where experiment_name=
Delete from recommendations where experiment_name=
```

#### **4. ListExperiments**

##### Request Method

HTTP_METHODE: GET

##### Rest API Curl

```
 curl --location --request get  'http://127.0.0.1:8080/listExperiment' \

 curl --location --request get  'http://127.0.0.1:8080/listExperiment?cluster_name=xyz

curl --location --request get  'http://127.0.0.1:8080/listExperiment?cluster_name=xyz&experiment_name=abc


curl --location --request get  'http://127.0.0.1:8080/listExperiments?mode=monitoring

curl --location --request get  'http://127.0.0.1:8080/listExperiments?mode=monitoring&target_cluster=remote


curl --location --request get  'http://127.0.0.1:8080/listExperiments?namespace='default'
```

##### DB query

```
Select * from kruize_experiment where cluster_name='xyz'

Select * from kruize_experiment where cluster_name='xyz' & experiment_name='abc'

SELECT *
FROM  kruize_results t, json_array_elements(t.extended_data) j
WHERE j->>'namespace' = 'default'
GROUP BY id;
```

## **kruize_results**

---


The entity called Kruize_results is used to store the metric results of a previously created experiment, and this data
is either sent through an external system or retrieved from a data source such as Prometheus or Thanos in order to
generate recommendations. The columns in this entity include the version, experiment name, start and end timestamps for
monitoring, duration in minutes, extended data, and metadata.


<table>
  <tr>
   <td><strong>Attribute</strong>
   </td>
   <td><strong>Type</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>version
   </td>
   <td>string
   </td>
   <td>The version of the create experiment json template.
   </td>
  </tr>
  <tr>
   <td>interval_id
   </td>
   <td>string
   </td>
   <td>primary key uniquely identify each results of the experiment.
   </td>
  </tr>
  <tr>
   <td>experiment_name
   </td>
   <td>string
   </td>
   <td>The name of the experiment. This is a unique identifier for the experiment.
   </td>
  </tr>
  <tr>
   <td>interval_start_time
   </td>
   <td>timestamp
   </td>
   <td>interval start time to gather the query results for metrics
   </td>
  </tr>
  <tr>
   <td>interval_end_time
   </td>
   <td>timestamp
   </td>
   <td>interval end time. 
   </td>
  </tr>
  <tr>
   <td>duration_minutes
   </td>
   <td>double
   </td>
   <td>interval_end_time - interval_start_time
   </td>
  </tr>
  <tr>
   <td>extended_data
   </td>
   <td>json
   </td>
   <td>Additional data for the experiment in JSON format, which includes complete Kubernetes objects details with matric results
   </td>
  </tr>
  <tr>
   <td>meta_data
   </td>
   <td>json
   </td>
   <td>Additional metadata for the experiment_result in JSON format.
   </td>
  </tr>
</table>

### Experiment_results API

The following REST API's are available for the Kruize_results entity:

#### 1. updateResults

##### Request Method

HTTP_METHODE: POST

##### Rest API Curl

```
curl --location --request POST 'http://127.0.0.1:8080/updateResults' \
--header 'Content-Type: application/json' \
--data-raw '[
  {
	"version": "1.0",
	"experiment_name": "quarkus-resteasy-autotune-min-http-response-DB10",
	"start_timestamp": "2022-01-23T18:25:43.511Z",
	"end_timestamp": "2022-01-23T18:25:43.511Z",
	"kubernetes_objects": [
  	{
    	"type": "deployment",
    	"name": "tfb-qrh-deployment",
    	"namespace": "default",
    	"containers": [
      	{
        	"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
        	"container_name": "tfb-server",
        	"metrics": [
          	{
            	"name": "cpuRequest",
            	"results": {
              	"value": 1.1,
              	"format": "cores",
              	"aggregation_info": {
                	"sum": 4.4,
                	"avg": 1.1,
                	"format": "cores"
              	}
            	}
          	},
          	{
            	"name": "cpuLimit",
            	"results": {
              	"value": 0.5,
              	"format": "cores",
              	"aggregation_info": {
                	"sum": 2.0,
                	"avg": 0.5,
                	"format": "cores"
              	}
            	}
          	},
          	{
            	"name": "cpuUsage",
            	"results": {
              	"value": 0.12,
              	"format": "cores",
              	"aggregation_info": {
                	"min": 0.14,
                	"max": 0.84,
                	"sum": 0.84,
                	"avg": 0.12,
                	"format": "cores"
              	}
            	}
          	},
          	{
            	"name": "cpuThrottle",
            	"results": {
              	"value": 0.045,
              	"format": "cores",
              	"aggregation_info": {
                	"sum": 0.19,
                	"max": 0.09,
                	"avg": 0.045,
                	"format": "cores"
              	}
            	}
          	},
          	{
            	"name": "memoryRequest",
            	"results": {
              	"value": 50.12,
              	"format": "MiB",
              	"aggregation_info": {
                	"sum": 250.85,
                	"avg": 50.21,
                	"format": "MiB"
              	}
            	}
          	},
          	{
            	"name": "memoryLimit",
            	"results": {
              	"value": 100,
              	"format": "MiB",
              	"aggregation_info": {
                	"sum": 500,
                	"avg": 100,
                	"format": "MiB"
              	}
            	}
          	},
          	{
            	"name": "memoryUsage",
            	"results": {
              	"value": 40.1,
              	"format": "MiB",
              	"aggregation_info": {
                	"min": 50.6,
                	"max": 198.50,
                	"sum": 198.50,
                	"avg": 40.1,
                	"format": "MiB"
              	}
            	}
          	},
          	{
            	"name": "memoryRSS",
            	"results": {
              	"aggregation_info": {
                	"min": 50.6,
                	"max": 123.6,
                	"sum": 123.6,
                	"avg": 31.91,
                	"format": "MiB"
              	}
            	}
          	}
        	]
      	}
    	]
  	}
	]
  }
]'
```

##### DB query

```
Insert into kruize_results
```

#### 2. listExperiments

##### Request Method

HTTP_METHODE: GET

##### listExperiments with results

Rest API Curl

```
/listExperiment?showresults
```

###### DB query

```
Select e.* , r.*  from kruize_results r , experiment e
Where r.experiment_name = e.experiment_name
```

## recommendations

---
The purpose of this entity is to store the recommendations generated for each experiment, with the Kubernetes objects
tree stored in the extended_data column, where the recommendations are stored on a per-container basis


<table>
  <tr>
   <td><strong>Attribute</strong>
   </td>
   <td><strong>Type</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>version
   </td>
   <td>string
   </td>
   <td>The version of the create experiment json template.
   </td>
  </tr>
  <tr>
   <td>experiment_name
   </td>
   <td>string
   </td>
   <td>The name of the experiment. This is a unique identifier for the experiment.
   </td>
  </tr>
  <tr>
   <td>cluster_name
   </td>
   <td>string
   </td>
   <td>monitoring start time to gather the query results for metrics
   </td>
  </tr>
  <tr>
   <td>interval_end
   </td>
   <td>timestamp
   </td>
   <td>monitoring end time. 
   </td>
  </tr>
  <tr>
   <td>extended_data
   </td>
   <td>json
   </td>
   <td>Additional data for the experiment in JSON format, which includes complete Kubernetes objects details with matric results
   </td>
  </tr>
</table>

### Recommendation API

#### 1. listRecommendations

##### Request Method

HTTP_METHODE: GET

##### Rest API Curl

```
curl --location --request GET 'http://192.168.49.2:31583/listRecommendations'

curl --location --request GET 'http://192.168.49.2:31583/listRecommendations?experiment_name=

curl --location --request GET 'http://192.168.49.2:31583/listRecommendations?experiment_name= & container_name=

curl --location --request GET 'http://192.168.49.2:31583/listRecommendations?experiment_name= &
container_name=& from_date>   to to_date<
```

##### DB Query

```
SELECT * FROM recommendation
WHERE 
experiment_name = 'xyz' & 
interval_end>data & interval_end<date &
EXISTS (
    SELECT 1 FROM jsonb_array_elements(extended_data->'kubernetes_objects') kubernetes_object
    WHERE EXISTS (
        SELECT 1 FROM jsonb_array_elements(kubernetes_object->'containers') container
        WHERE container->>'container_name' = 'tfb-server'
    )
);
```

## performanceprofile

---



<table>
  <tr>
   <td><strong>Attribute</strong>
   </td>
   <td><strong>Type</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>name
   </td>
   <td>string
   </td>
   <td>Performance profile name
   </td>
  </tr>
  <tr>
   <td>profile_version
   </td>
   <td>string
   </td>
   <td>The version of the profile.
   </td>
  </tr>
  <tr>
   <td>SLO
   </td>
   <td>string
   </td>
   <td>Objectives, function and function variables
   </td>
  </tr>
</table>

### Performance profile apis

#### 1. listPerformance

##### Request Method

HTTP_METHODE: GET

##### Rest API Curl

```
curl --location --request GET 'http://192.168.49.2:31583/listPerformanceProfile'

curl --location --request GET 'http://192.168.49.2:31583/listPerformanceProfile?profile_name='resource_optimization'
```

##### DB query

```
Select * from performance_profiles where profile_name='resource_optimization' ; 
```

#### 2. createPerformanceProfile

##### Request Method

HTTP_METHODE: POST

##### Rest API Curl

```
curl --location --request POST 'http://127.0.0.1:8080/createPerformanceProfile' \
--header 'Content-Type: application/json' \
--data-raw '{
  "name": "resource-optimization-openshift",
  "profile_version": 1,
  "slo": {
    "direction": "minimize",
    "objective_function": {
        "function_type": "expression",
        "expression": "cpuRequest"
    },
    "function_variables": [
      {
        "name": "cpuRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "query": "kube_pod_container_resource_requests{pod=~'\''$DEPLOYMENT_NAME$-[^-]*-[^-]*$'\'', container='\''$CONTAINER_NAME$'\'', namespace='\''$NAMESPACE'\'', resource='\''cpu'\'', unit='\''core'\''}",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
          }
        ]
      }
    ]
  }
}'
```

##### DB query

```
insert into performance_profiles; 
```

