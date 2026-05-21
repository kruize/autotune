#*******************************************************************************
# * Copyright (c) 2026 Red Hat, IBM Corporation and others.
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *    http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# *******************************************************************************

#!/bin/bash
set -euo pipefail

LABEL_ARG="--metric-labels-allowlist=pods=[app,com.redhat.component-name,version]"
NAMESPACE=""
DEPLOYMENT=""

usage() {
  echo "Usage: $0 [-n namespace] [-d deployment]"
  echo ""
  echo "If not specified, the script will try to auto-detect kube-state-metrics."
}

while getopts ":n:d:h" opt; do
  case "$opt" in
    n) NAMESPACE="$OPTARG" ;;
    d) DEPLOYMENT="$OPTARG" ;;
    h) usage; exit 0 ;;
    *) usage; exit 1 ;;
  esac
done

# ---- Dependency checks ----
for cmd in kubectl jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "❌ Required command '$cmd' not found in PATH. Please install it first."
    exit 1
  fi
done

if [ -z "$NAMESPACE" ] || [ -z "$DEPLOYMENT" ]; then
  echo -n "Searching for kube-state-metrics deployment..."

  DEPLOY_INFO=$(kubectl get deployments -A -o json | \
    jq -r '.items[] | select(.metadata.name | test("kube-state-metrics")) | "\(.metadata.namespace) \(.metadata.name)"')

  COUNT=$(echo "$DEPLOY_INFO" | grep -c . || true)

  if [ "$COUNT" -eq 0 ]; then
    echo "❌ No kube-state-metrics deployment found."
    exit 1
  elif [ "$COUNT" -gt 1 ]; then
    echo "❌ Multiple kube-state-metrics deployments found:"
    echo "$DEPLOY_INFO"
    echo "Please specify NAMESPACE and DEPLOYMENT explicitly:"
    echo "  $0 -n <namespace> -d <deployment>"
    exit 1
  fi

  NAMESPACE=$(echo "$DEPLOY_INFO" | awk '{print $1}')
  DEPLOYMENT=$(echo "$DEPLOY_INFO" | awk '{print $2}')
  echo "Found $DEPLOYMENT in $NAMESPACE namespace."
fi

EXISTS=$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o json | \
jq -r '.spec.template.spec.containers[]
       | select(.name=="kube-state-metrics")
       | .args[]? ' | grep -c "metric-labels-allowlist=.*pods=" || true)

if [ "$EXISTS" -gt 0 ]; then
  echo " --metric-labels-allowlist already present. No change needed."
else
  echo -n "Adding --metric-labels-allowlist argument..."

  kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o json | \
  jq --arg ARG "$LABEL_ARG" '
    .spec.template.spec.containers |=
      map(if .name=="kube-state-metrics"
          then .args = ((.args // []) + [$ARG])
          else .
          end)
  ' | kubectl apply -f -

  echo "Done."

fi

