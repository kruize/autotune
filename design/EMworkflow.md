*Note: Experiment Manager is still under development process and doc changes are expected very frequently. Please check https://github.com/kruize/autotune/discussions and https://github.com/kruize/autotune/issues for latest updates*

# Experiment manager

Experiment manager is a part of Autotune which takes care of performing test runs of the configuration provided by the Recommendation manager and feeds back the results to the recommendation manager. The results is mostly consists of experiment trail details and the performance details based on the given metrics. Experiment manager plays a vital role in the whole Autotune ecosystem as it's responsible for generating the train data for the ML module. Every trail output is used to analyse and classify the workload and provide optimal settings based on the SLA.

## WorkFlow

Firstly let's talk about the inputs (we give) to the `Experiment Manager` module.

The main input for the Experiment Manager is the trail config to launch an experiment

### Quick insights:

##### What's a trail config ?

`Trail Config` is a JSON Object which has the schema and details about experiment to deploy.

##### What does a trail config consists of ?

`Trail Config` consists of details like:

* Details of the main deployment (Mostly a production deployment)

* Details of trail:
    * Trail ID
    * Deployment name (Mostly a production deployment)
    * Trail measurement time
    * Trail Run
    * ...
    
* Details of Metrics:
    * Name of the metric
    * Datasource / Metric provider info
    * Query structure of the metric
    * ...
    
* Details of trail configuration:
    * system resources requests:
        * cpu
        * memory
    * system resources limits:
        * cpu
        * memory
    * Environment variables (To set runtime/application specific tunables)
    * ...
    
##### From where does the Experiment Manager get it's input (trail config) ?

The user of the Experiment manager should be providing the trail config input. As it's part of Autotune the user of the Experiment Manager will be the Recommendation manager.

##### How does the Experiment Manager talk to the Recommendation manager for input ?

In the current autotune version (0.0.1) the communication between the Recommendation manager and the Experiment Manager is via internal queues (Recommendation manager pushes the trail config to a queue and experiment manager reads from the queue and perform the deployment of the trail config)

But in upcoming releases we will be introducing an endpoint to the experiment manager to receive the trail config via http post request.

### Moving on to the process

As now we are clear about inputs let's deep dive into the Experiment Manager process flow.

To start of with Let's highlight the duties of Experiment Manager:

1. Receive trail config
2. Create a deployment config
3. Launch the deployment config
4. Apply load on the deployment launched
5. Gather the metrics
6. Analyse Result and send to User (Recommendation Manager)

##### 1. Receive trail config

The Experiment Manager needs to constantly read the queue for any new AutotuneDTO object pushed to the queue by the recommendation manager (or user) 

##### 2. Create a deployment config

Once it receives the AutotuneDTO object we poll for the input JSON with the url set in the object to get the trail config. We parse the trail config and access the production deployment config and update accordingly with the trail config. The updated deployment is now ready to be deployed with a new naming convention

##### 3. Launch the deployment config

We now proceed to deploy the updated config to the kubernetes cluster with the new settings to try on

##### 4. Apply load on the deployment launched

We wait for the production load to be applied on the new deployment and after the load we wait till the trail run duration mentioned in the trail config

##### 5. Gather the metrics

Now we scrape the data for duration of measurement time in the trail config from the datasource

##### 6. Analyse Result and send to User (Recommendation Manager)

If the deployment is successfull it returns a success response along with the data obtained from the trail run, We proceed and update the input JSON object by adding details of the metrics obtained in the metrics section of the JSON and also to the appropriate metric

### Sample input JSON

```
{
   "trials": {
      "id": "101383821",
      "app-version": "v1",
      "deployment_name": "petclinic-sample",
      "trial_num": 1,
      "trial_run": "15mins",
      "trial_measurement_time": "3mins",
      "metrics": [
         {
            "name": "obj_fun_var1",
            "query": "obj_fun_var1_query",
            "datasource": "prometheus"
         },
         {
            "name": "obj_fun_var2",
            "query": "obj_fun_var2_query",
            "datasource": "prometheus"
         },
         {
            "name": "obj_fun_var3",
            "query": "obj_fun_var3_query",
            "datasource": "prometheus"
         },
         {
            "name": "cpuRequest",
            "query": "cpuRequest_query",
            "datasource": "prometheus"
         },
         {
            "name": "memRequest",
            "query": "memRequest_query",
            "datasource": "prometheus"
         }
      ],
      "update_config": [
         {
            "name": "update requests and limits",
            "spec": {
               "template": {
                  "spec": {
                     "container": {
                        "resources": {
                           "requests": {
                              "cpu": 2,
                              "memory": "512Mi"
                           },
                           "limits": {
                              "cpu": 3,
                              "memory": "1024Mi"
                           }
                        }
                     }
                  }
               }
            }
         },
         {
            "name": "update env",
            "spec": {
               "template": {
                  "spec": {
                     "container": {
                        "env": {
                           "JVM_OPTIONS": "-XX:MaxInlineLevel=23",
                           "JVM_ARGS": "-XX:MaxInlineLevel=23"
                        }
                     }
                  }
               }
            }
         }
      ]
   }
}
```

### Sample output JSON

```
{
   "trials": {
      "id": "101383821",
      "app-version": "v1",
      "deployment_name": "petclinic_deployment",
      "trial_num": 1,
      "trial_run": "15mins",
      "trial_measurement_time": "3mins",
      "trial_result": "",
      "trial_result_info": "",
      "trial_result_error": "",
      "metrics": [
         {
            "name": "obj_fun_var1",
            "query": "obj_fun_var1_query",
            "datasource": "prometheus",
            "score": "",
            "Error": "",
            "mean": "",
            "mode": "",
            "95.0": "",
            "99.0": "",
            "99.9": "",
            "99.99": "",
            "99.999": "",
            "99.9999": "",
            "100.0": "",
            "spike": ""
         },
         {
            "name": "obj_fun_var2",
            "query": "obj_fun_var2_query",
            "datasource": "prometheus",
            "score": "",
            "Error": "",
            "mean": "",
            "mode": "",
            "95.0": "",
            "99.0": "",
            "99.9": "",
            "99.99": "",
            "99.999": "",
            "99.9999": "",
            "100.0": "",
            "spike": ""
         },
         {
            "name": "obj_fun_var3",
            "query": "obj_fun_var3_query",
            "datasource": "prometheus",
            "score": "",
            "Error": "",
            "mean": "",
            "mode": "",
            "95.0": "",
            "99.0": "",
            "99.9": "",
            "99.99": "",
            "99.999": "",
            "99.9999": "",
            "100.0": "",
            "spike": ""
         },
         {
            "name": "cpuRequest",
            "query": "cpuRequest_query",
            "datasource": "prometheus",
            "score": "",
            "Error": "",
            "mean": "",
            "mode": "",
            "95.0": "",
            "99.0": "",
            "99.9": "",
            "99.99": "",
            "99.999": "",
            "99.9999": "",
            "100.0": "",
            "spike": ""
         },
         {
            "name": "memRequest",
            "query": "memRequest_query",
            "datasource": "prometheus",
            "score": "",
            "Error": "",
            "mean": "",
            "mode": "",
            "95.0": "",
            "99.0": "",
            "99.9": "",
            "99.99": "",
            "99.999": "",
            "99.9999": "",
            "100.0": "",
            "spike": ""
         }
      ]
   }
}
```

JSON attributes description:

1. #### id

`id` is a unique indentification given to a experiment to retrieve information using it. (Acts as a primary key)

2. #### app-version

`app-version` is the string value which represents the current autotune version

3. #### deployment_name

`deployment_name` is the string value which represents the production deployment name

4. #### trial_num

`trial_num` is used to index number of the trail for an experiment, As an experiment can have `N` number of trails (N may not a finite value)

5. #### trial_run

`trial_run` is the total duration that a trail should be running after it's launch and after it gets a load appearing on it

6. #### trial_measurement_time

`trial_measurement_time` is the duration for getting the metrics from the data source before the end of the trail run

7. #### trial_result

`trial_result` holds the status of the trail if its successfull or a failure

8. #### trial_result_info

`trial_result_info` holds the additional information about the trail run

9. #### trial_result_error

`trial_result_error` holds the information about the error in the trail run 

10. #### metrics

`metrics` is a array of the json objects which has `name`, `datasource`, `query`.

    1. name:
        Specifies the name of the metric.
    
    2. datasource:
        Represents the name of the datasource ex: prometheus
    
    3. query:
        Query structure to get the metric from the data source

    4. score:
        score is a value obtained for that particular metric

    5. error:
        Any error to obtain the value from data source

    6. mean: 
        Mean value of the metric in the duration recorded (trail measurement time)

    7. mode:
        Mode value of the metric result in the duration recorded (trail measurement time)

    8. 95.0:
        Value at the 95th percentail

    9. 99.0:
        value at the 99th percentail

    10. 99.9:
        value at the 99.9th percentail

    11. 99.99:
        value at the 99.99th percentail

    12. 99.999:
        value at the 99.999th percentail

    13. 100.0:
        value at the 100.0th percentail

    14. spike:
        Highest value recorded during the trail measurement time duration

11. #### update_config

`update_config` it's an array of json objects which holds the requests, limits of resources and env vars for runtime tunables


