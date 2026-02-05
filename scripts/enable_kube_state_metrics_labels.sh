#!/bin/bash

LABEL_ARG="--metric-labels-allowlist=pods=[app,app.kubernetes.io/layer,version]"

echo -n "Searching for kube-state-metrics deployment..."

DEPLOY_INFO=$(kubectl get deployments -A | grep kube-state-metrics | head -n 1)

if [ -z "$DEPLOY_INFO" ]; then
  echo "❌ kube-state-metrics deployment not found"
  exit 1
fi

NAMESPACE=$(echo "$DEPLOY_INFO" | awk '{print $1}')
DEPLOYMENT=$(echo "$DEPLOY_INFO" | awk '{print $2}')

echo "Found $DEPLOYMENT in $NAMESPACE namespace."

EXISTS=$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o json | \
jq -r '.spec.template.spec.containers[]
       | select(.name=="kube-state-metrics")
       | .args[]? ' | grep -c "metric-labels-allowlist")

if [ "$EXISTS" -gt 0 ]; then
  echo " --metric-labels-allowlist already present. No change needed."
else
  echo -n "Adding --metric-labels-allowlist argument..."

  kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o json | \
  jq --arg ARG "$LABEL_ARG" '
    .spec.template.spec.containers |=
      map(if .name=="kube-state-metrics"
          then .args += [$ARG]
          else .
          end)' | \
  kubectl apply -f -

#  echo "Restarting deployment..."
#  kubectl -n "$NAMESPACE" rollout restart deployment "$DEPLOYMENT"
#  kubectl -n "$NAMESPACE" rollout status deployment "$DEPLOYMENT"

  echo "Done."

fi

