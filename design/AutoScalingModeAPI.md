# Auto Scaling Mode APIs

This document will help you quickly get started with the 
autoscaling feature enabled by integrating Kruize with Kubernetes' 
Vertical Pod Autoscaler (VPA) and Instaslice. 

By leveraging Kruize APIs, you can streamline resource optimization 
and ensure your applications adapt efficiently to changing workloads 
with minimal manual intervention.

# Table of Contents

1. [Defaults](#defaults)

2. [API's](#apis)

- [Create Experiment API](#create-experiment-api)
    - Introduction
    - Example Request and Response
    - Invalid Scenarios

3. [Accelerator Autoscaler](#accelerator-autoscaler)

## Defaults

| Parameter                | Default Value |
|--------------------------|---------------|
| Recommendation Term      | Short Term    |
| Recommendation Profile   | Performance   |
| Default Sleep Interval   | 60 sec        |
| Default Initial Interval | 30 sec        |


* **Sleep Interval:** The duration between two consecutive updates made by Kruize to the Vertical Pod Autoscaler (VPA). 
This interval determines the frequency of updates to optimize resource recommendations. 
* **Initial Delay:** The duration Kruize waits before starting the updater service after the Kruize pod is initialized. 
* [Recommendation Terms & Threshold scenarios](MonitoringModeAPI.md#terms-duration--threshold-table)
* [Recommendation Profiles](MonitoringModeAPI.md#profile-algorithms-how-kruize-calculates-the-recommendations)


## API's

<a name="create-experiment-api"></a>

### Create Experiment API

This is quick guide instructions to create experiments using input JSON as follows.
This API and JSON format are applicable to both currently available autoscaling modes: `auto` and `recreate`.

**Request**
`POST /createExperiment`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createExperiment`

<details>

<summary><b>Example Request</b></summary>

### Example Request

```json
[{
  "version": "v2.0",
  "experiment_name": "optimize-sysbench",
  "cluster_name": "default",
  "performance_profile": "resource-optimization-local-monitoring",
  "mode": "recreate",
  "target_cluster": "local",
  "datasource": "prometheus-1",
  "kubernetes_objects": [
    {
      "type": "deployment",
      "name": "sysbench",
      "namespace": "newchange",
      "containers": [
        {
          "container_image_name": "quay.io/kruizehub/sysbench",
          "container_name": "sysbench"
        }
      ]
    }
  ],
  "trial_settings": {
    "measurement_duration": "10min"
  },
  "recommendation_settings": {
    "threshold": "0.1"
  }
}]
```

</details>


**Response**

<details>
<summary><b>Example Response</b></summary>

### Example Response

```json
{
  "message": "Experiment registered successfully with Autotune. View registered experiments at /listExperiments",
  "httpcode": 201,
  "documentationLink": "",
  "status": "SUCCESS"
}
```

</details>


**Note:**
- Once created, the experiment will generate recommendations every minute, and your workloads will automatically scale 
according to resource requirements. 
- **Ensure you have a minimum of 2 replicas for proper functionality.**
- Currently, we support the `recreate` and `auto` mode for updates but for now `auto` is same as `recreate`. 
In the future, when restart-free ("in-place") updates of pod requests become available, `auto` mode will do the in-place updates.
- Upon creating the experiment, Kruize will automatically generate a Vertical Pod Autoscaler (VPA) object for enabling autoscaling. 
It will patch updated recommendations to the VPA object every minute, as demonstrated below. 
Users can also view the VPA object and its recommendations by using the `oc get vpa` and `oc describe vpa <vpa-name>` commands.
```
Name:         optimize-sysbench
Namespace:    default
Labels:       <none>
Annotations:  <none>
API Version:  autoscaling.k8s.io/v1
Kind:         VerticalPodAutoscaler
Metadata:
  Creation Timestamp:  2025-01-21T12:15:28Z
  Generation:          1
  Resource Version:    572916
  UID:                 6edf273f-f97b-4399-b1ef-27f382f3d911
Spec:
  Recommenders:
    Name:  Kruize
  Resource Policy:
    Container Policies:
      Container Name:  sysbench
      Controlled Resources:
        cpu
        memory
  Target Ref:
    API Version:  apps/v1
    Kind:         Deployment
    Name:         sysbench
  Update Policy:
    Update Mode:  Auto
Status:
  Recommendation:
    Container Recommendations:
      Container Name:  sysbench
      Lower Bound:
        Cpu:     999m
        Memory:  39Mi
      Target:
        Cpu:     999m
        Memory:  39Mi
      Upper Bound:
        Cpu:     999m
        Memory:  39Mi
Events:          <none>
```


## Autoscaling Mode for Accelerator Workloads:

<a name="accelerator-autoscaler"></a>

### Accelerator Autoscaler

#### Overview

The Accelerator Autoscaler is designed to optimize the allocation of GPU resources for workloads running on Kubernetes clusters. It intelligently adjusts resource configurations based on accelerator recommendations generated by Kruize. The Accelerator Autoscaler is only used when the user specifies `auto` or `recreate` mode while creating experiment.

In Kruize, if the experiment is created with either of these modes and GPU metrics are found, the Accelerator Autoscaler is utilized.

#### When is Accelerator Autoscaler Used?

During the Generate Recommendations phase, if GPU metrics are detected and GPU recommendations are created, the autoscaler is set to Accelerator Autoscaler.

This ensures that the resource configurations are tailored to the GPU requirements of the workload, maximizing performance and efficiency.

#### Why Not Use VPA?

- Vertical Pod Autoscaler (VPA) only works for CPU and Memory but not for GPU.
- In traditional setups, VPA handles CPU and memory scaling, while GPU scaling requires a different approach.
- The Accelerator Autoscaler directly updates the CPU, Memory, and GPU resources of the Kubernetes object, ensuring all resource requirements are met in a coordinated way.


#### Key Takeaways

- The Accelerator Autoscaler is activated when GPU metrics are detected and GPU recommendations are generated.
- It directly updates CPU, Memory, and GPU resources, bypassing VPA for GPU workloads.
- Currently, only the update process is implemented, with a revert mechanism planned for future releases.