#!/bin/bash

KAFKA_ROOT_DIR=$1
if [ -z "$KAFKA_ROOT_DIR" ]; then
        KAFKA_ROOT_DIR="${PWD}"
fi

# Define Kafka version and directory
KAFKA_VERSION="3.9.0"
SCALA_VERSION="2.13"
KAFKA_DIR="${KAFKA_ROOT_DIR}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}"
KAFKA_SERVER_LOG="${KAFKA_ROOT_DIR}/kafka_server.log"

function kafka_server_cleanup() {
	{
    echo "Starting Kafka server cleanup process..."
    # Delete Kafka components first
    echo "Deleting Kafka resources..."
    oc delete kafka --all -n $KAFKA_NAMESPACE --ignore-not-found=true
    oc delete kafkatopic --all -n $KAFKA_NAMESPACE --ignore-not-found=true
    oc delete kafkauser --all -n $KAFKA_NAMESPACE --ignore-not-found=true
    oc delete kafkamirrormaker --all -n $KAFKA_NAMESPACE --ignore-not-found=true
    oc delete kafkamirrormaker2 --all -n $KAFKA_NAMESPACE --ignore-not-found=true
    oc delete kafkabridge --all -n $KAFKA_NAMESPACE --ignore-not-found=true
    oc delete kafkarebalance --all -n $KAFKA_NAMESPACE --ignore-not-found=true

    # Delete Strimzi Operator
    echo "Deleting Strimzi Operator..."
    oc delete deployment strimzi-cluster-operator -n $KAFKA_NAMESPACE --ignore-not-found=true

     # Remove stuck finalizers from Kafka CRDs
    echo "Checking for stuck Kafka CRDs..."
    for crd in kafkatopics kafkabridges kafkaconnectors kafkaconnects kafkamirrormaker2s kafkamirrormakers kafkanodepools kafkarebalances kafkas kafkausers; do
        oc get crd $crd.kafka.strimzi.io --ignore-not-found=true -o json | jq -r '.metadata.name' | while read line; do
            echo "Force deleting finalizer for: $line"
            oc patch crd $line -p '{"metadata":{"finalizers":[]}}' --type=merge || echo "Failed to patch $line"
        done
    done

    # Delete Kafka CRDs
    for crd in $(oc get crds -o json | jq -r '.items[].metadata.name' | grep 'kafka\|strimzi'); do
      echo "Deleting CRD: $crd"
      oc delete crd "$crd" --ignore-not-found=true
    done

    # Delete namespace
    echo "Deleting Kafka namespace..."
    oc delete namespace $KAFKA_NAMESPACE --ignore-not-found=true &

    # Wait for 30 seconds, then check if the namespace is still terminating
    sleep 30

    NAMESPACE_STATUS=$(oc get ns $KAFKA_NAMESPACE -o jsonpath='{.status.phase}' 2>/dev/null)

    if [[ "$NAMESPACE_STATUS" == "Terminating" ]]; then
        echo "Namespace $KAFKA_NAMESPACE is stuck in Terminating state. Forcing deletion..."

        # Remove finalizers
        oc get ns $KAFKA_NAMESPACE -o json | jq 'del(.spec.finalizers)' | oc replace --raw "/api/v1/namespaces/$KAFKA_NAMESPACE/finalize" -f -

        # Delete any lingering terminating pods
        oc get pods -n $KAFKA_NAMESPACE | grep Terminating | awk '{print $1}' | xargs -r oc delete pod --grace-period=0 --force -n $KAFKA_NAMESPACE

        # Retry deleting the namespace
        oc delete ns $KAFKA_NAMESPACE --force --grace-period=0
    fi

    # Final check
    if oc get ns $KAFKA_NAMESPACE &>/dev/null; then
        echo "Namespace $KAFKA_NAMESPACE deletion failed. Please check manually."
    else
        echo "Namespace $KAFKA_NAMESPACE deleted successfully."
    fi

    echo "Kafka cleanup completed successfully!"


  } >> "${KAFKA_SERVER_LOG}" 2>&1
}

echo "Clean up in progress..."

echo -n "🔄 Removing Kafka..."
rm -rf ${KAFKA_TGZ} ${KAFKA_DIR}
rm -rf $CERT_FILE truststore.jks
kafka_server_cleanup
echo "✅ Done!"

echo "For detailed logs, look in kafka-setup.log"
echo


echo "Cleaning up Kafka dir..."
rm -rf "${KAFKA_DIR}"
rm "${KAFKA_SERVER_LOG}"

