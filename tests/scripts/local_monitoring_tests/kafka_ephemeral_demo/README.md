# Recommendations consumption using Kafka 

With Kruize's local monitoring mode, let's explore a demonstration of consuming messages using Kafka in conjunction with the Bulk API. 

Kafka replaces the existing API workflow by adding the consumer mechanism which helps in an asynchronous communication between the client and Kruize.

User will start with a Bulk API request which returns a `job_id` as a response to track the job status. After that, internally Bulk API produces recommendations, and the same is being then sent via a Kafka Producer.
User can then consume it with the help of a consumer client as and when required. 

Refer the documentation of the [Kafka Design](https://github.com/kruize/autotune/blob/87b544c7e07deb22f683d6c124a0188f7b06d836/design/KafkaDesign.md) for details.(To be updated once the PR is merged)

## Demo workflow

- Reserve a namespace in ephemeral cluster
- Update Bonfire config with the Kruize image
- Pull the ros-ocp-backend repo
- Update the kruize-clowdapp file with the current namespace details
- Deploy the application
- Create metric profile by API using the route
- Invoke Bulk API request to initiate the bulk flow
- Start Consuming the message from the recommendations-topic

To begin exploring the Kafka flow, follow these steps:

### Run the Demo

#### Pre-requisites

Bonfire tool needs to be installed before running the demo. We can do that by following [this](https://github.com/RedHatInsights/bonfire/#installation) link.

##### Clone the demo repository:
```sh
git clone git@github.com:kruize/kruize-demos.git
```
##### Change directory to the local monitoring demo:
```sh
cd kruize-demos/monitoring/local_monitoring/kafka_demo
```
##### Execute the demo script in ephemeral as:
```sh
./kafka_demo.sh
```

```
 "Usage: ./kafka_demo.sh [-s|-t] [-i kruize-image] [-u datasource-url] [-d datasource-name]"
	 "s = start (default), t = terminate"
	 "i = Kruize image (default: $KRUIZE_IMAGE)"
	 "c = Cluster type (default: ephemeral)"
	 "u = Prometheus/Thanos datasource URL (default: $DATASOURCE_URL)"
	 "d = Name of the datasource (default: $DATASOURCE_NAME)"
	exit 1
Note: We need to pass the datasource URL and datsource Name. All other parameters are optional.
Currently only ephemeral is supported!
```
Example:
`./kafka_demo.sh -i quay.io/khansaad/autotune_operator:kafka -u http://thanos-query-frontend-example-query-thanos-operator-system.apps.kruize-scalelab.h0b5.p1.openshiftapps.com -d thanos-ee-2 `
