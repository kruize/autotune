#!/bin/bash

NAMESPACE="openshift-monitoring"
CONFIGMAP="cluster-monitoring-config"

echo -n "Fetching cluster monitoring config..."

EXISTING=$(oc -n $NAMESPACE get configmap $CONFIGMAP \
  -o jsonpath='{.data.config\.yaml}' 2>/dev/null)

if [ -z "$EXISTING" ]; then
  echo "ConfigMap or config.yaml not found."
  echo -n "Creating new one..."

  cat <<EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: $CONFIGMAP
  namespace: $NAMESPACE
data:
  config.yaml: |
    enableUserWorkload: true
EOF

  echo "Done!"

else
  echo "Config exists."
  echo -n "Checking enableUserWorkload..."

  if echo "$EXISTING" | grep -q "enableUserWorkload:[[:space:]]*true"; then
    echo "Already enabled. No change needed."
  else
    echo "Not enabled."
    echo -n "Enabling user workload monitoring (preserving other settings)..."

    # If enableUserWorkload exists but false → replace it
    if echo "$EXISTING" | grep -q "enableUserWorkload:"; then
      UPDATED=$(echo "$EXISTING" | sed 's/enableUserWorkload:.*/enableUserWorkload: true/')
    else
      # Otherwise append it
      UPDATED="$EXISTING"$'\n'"enableUserWorkload: true"
    fi

    oc -n $NAMESPACE patch configmap $CONFIGMAP --type merge -p \
      "{\"data\":{\"config.yaml\":\"$(echo "$UPDATED" | sed 's/"/\\"/g')\"}}"

    echo "Done!"

    echo "Waiting for user workload monitoring pods..."
    oc wait --for=condition=Ready pod --all -n openshift-user-workload-monitoring --timeout=300s
    oc get pods -n openshift-user-workload-monitoring

  fi
fi

