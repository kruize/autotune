# Kafka Documentation

Kruize has a BulkService feature which designed to streamline the detection, management, and optimization of containerized environments. It relies on the REST APIs without a message broker, which introduces latency when handling multiple parallel requests.
Kafka replaces this existing REST API service for faster, asynchronous communication and to ensure seamless handling of recommendations at scale.

Kruize Kafka Producer internally uses the BulkService and publishes recommendations in the form of message in three topics namely, `recommendations-topic`, `error-topic` and `summary-topic`.

## Kafka Flow

1. Currently, Kruize Kafka Module works as a Producer only i.e. to invoke the Kafka Service user needs to hit a REST API POST request with the same input as the one for the BulkService.
2. On receiving the request, BulkService will return the `job_id` back and in the background starts the following tasks:
    - First, does a handshake with the datasource.
    - Using queries, it fetches the list of namespaces, workloads, containers of the connected datasource.
    - Creates experiments, one for each container *alpha release.
    - Triggers `generateRecommendations` for each container.
    - Once the above step returns a success response, Kafka Producer is invoked and the recommendations are pushed into the `recommendations-topic`
    - If at any of the above steps, the service fails be it while fetching metadata, creating experiment or generating recommendations, Kafka Producer is invoked to publish the error response in the `error-topic`.
    - Once all experiments are created, and recommendations are generated, the system marks the `job_id` as "COMPLETED".
    - Once the job is completed, Kafka Producer pushes the summary of all the experiments into the `summary-topic`.

## Specifications
 - User needs to hit a REST API POST request with a Payload as mentioned in the BulkAPI doc. For details, kindly refer to the [BulkAPI](BulkAPI.md) design doc.
 - Kafka needs to be installed locally or in a cluster, and it's corresponding Bootstrap server URL should be added in the crc file based on your cluster.
Example:
 - `"kafkaBootstrapServers" : "kruize-kafka-cluster-kafka-bootstrap.kafka.svc.cluster.local:9092"`
 - Consumer needs to be subscribed to the `recommendations-topic` to get the recommendations.
 - Subscribing to the `error-topic` and the `summary-topic` is optional
