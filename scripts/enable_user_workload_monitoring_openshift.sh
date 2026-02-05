#!/bin/bash

CURRENT=$(oc -n openshift-monitoring get configmap cluster-monitoring-config \
  -o jsonpath='{.data.config\.yaml}' 2>/dev/null | grep -c "enableUserWorkload: true")

if [ "$CURRENT" -gt 0 ]; then
  echo "User workload monitoring already enabled."
else
  echo "Enabling User Workload Monitoring..."

  oc apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    enableUserWorkload: true
EOF

  echo "Waiting for pods to be Ready..."
  oc wait --for=condition=Ready pod --all -n openshift-user-workload-monitoring --timeout=300s

fi

oc get pods -n openshift-user-workload-monitoring

