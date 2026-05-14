# Kruize Accelerator Support

## Background:
### NVIDIA MIG (Multi-Instance GPU) Technology

#### *What is MIG?*

NVIDIA Multi-Instance GPU (MIG) is a hardware-level GPU partitioning technology that allows a single physical GPU to be securely split into multiple, isolated GPU instances. Each MIG instance has dedicated compute cores, memory, and cache, enabling predictable performance and strong isolation for multi-tenant workloads.

MIG is particularly useful in Kubernetes and cloud-native environments where multiple containers or pods need guaranteed GPU resources.

**Supported Architectures:** `Ampere`, `Hopper`, `Blackwell`

## MODE: Remote Monitoring

### Overview

Kruize supports a Remote Monitoring mode that enables users to create experiments for workloads running on external or remote Kubernetes clusters. In this mode, Kruize does not directly scrape metrics; instead, users periodically push aggregated resource usage data to Kruize using the Update Results API. Based on this data, Kruize generates resource optimization recommendations.

Initially, Remote Monitoring supported only CPU and memory metrics. This capability has now been extended to include accelerator (GPU) metrics, enabling GPU-aware monitoring and recommendations for supported NVIDIA GPUs [Please check the list mentioned in supported GPU's section].

### Update Results API: Accelerator Metrics Support

#### Existing Metrics

The Update Results API already supports aggregated CPU and memory metrics, such as:

- `cpuRequest`
- `cpuLimit`
- `cpuUsage`
- `cpuThrottle`
- `memoryRequest`
- `memoryLimit`
- `memoryUsage`
- `memoryRSS`

**Newly Added Accelerator Metrics**

Kruize now accepts the following accelerator-related metrics at the container level:

- `acceleratorCoreUsage`
- `acceleratorMemoryUsage`
- `acceleratorFrameBufferUsage`

| Kruize Metric Name            | Description                                           | Source Prometheus Metric    | Unit / Format |
| ----------------------------- |-------------------------------------------------------| --------------------------- | ------------- |
| `acceleratorCoreUsage`        | GPU core utilization over the reporting interval      | `DCGM_FI_DEV_GPU_UTIL`      | Percentage    |
| `acceleratorMemoryUsage`      | GPU memory copy / memory controller utilization       | `DCGM_FI_DEV_MEM_COPY_UTIL` | Percentage    |
| `acceleratorFrameBufferUsage` | GPU frame buffer (absolute device memory) consumption | `DCGM_FI_DEV_FB_USED`       | MiB           |

Each accelerator metric is reported as aggregated values (for example, min, max, avg) over the specified time interval. Metadata such as the GPU model needs to be provided to help Kruize contextualize and calculate the partition required for the workload based on the model it's using.

NOTE: Accelerator MIG recommendations are not similar to CPU and Memory which are common across the hardware, GPU recommendations provided by kruize are specific MIG resource profiles that NVIDIA has developed specific to a particular GPU.
Please refer https://docs.nvidia.com/datacenter/tesla/mig-user-guide/supported-mig-profiles.html for more information.

#### Example Structure of the New metrics in update results JSON

##### acceleratorCoreUsage:

```json
{
    "name": "acceleratorCoreUsage",
    "results": {
      "metadata": {
        "accelerator_model_name": "NVIDIA-A100-SXM4-40GB"
      },
      "aggregation_info": {
        "min": 18.7,
        "max": 45.7,
        "avg": 36.4,
        "format": "percentage"
      }
    }
}
```

##### acceleratorMemoryUsage

```json
{
    "name": "acceleratorMemoryUsage",
    "results": {
      "metadata": {
        "accelerator_model_name": "NVIDIA-A100-SXM4-40GB"
      },
      "aggregation_info": {
        "min": 22.9,
        "max": 49.0,
        "avg": 30.5,
        "format": "percentage"
      }
    }
}
```

##### acceleratorFrameBufferUsage

```json
{
    "name": "acceleratorFrameBufferUsage",
    "results": {
      "metadata": {
        "accelerator_model_name": "NVIDIA-A100-SXM4-40GB"
      },
      "aggregation_info": {
        "min": 8120,
        "max": 18977,
        "avg": 12703,
        "format": "MiB"
      }
    }
}
```

#### Example Structure of the GPU Recommendations in update recommendations JSON

```json
"config": {
    "requests": {
      "cpu": {
        "amount": 0.9299999999999999,
        "format": "cores"
      },
      "memory": {
        "amount": 238.2,
        "format": "MiB"
      }
    },
    "limits": {
      "cpu": {
        "amount": 0.9299999999999999,
        "format": "cores"
      },
      "memory": {
        "amount": 238.2,
        "format": "MiB"
      },
      "nvidia.com/mig-4g.20gb": {
        "amount": 1.0,
        "format": "cores"
      }
    }
}
```

#### MIG Supported Nvidia GPU's which can be optimised by KRUIZE

- Nvidia A100 [Both 40 and 80 GB variants]
- Nvidia H100 [80, 94 and 96 GB variants ]
- Nvidia H200 [141 GB variant]
