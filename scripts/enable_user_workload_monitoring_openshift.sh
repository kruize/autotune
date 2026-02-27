/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

#!/bin/bash
set -euo pipefail

NAMESPACE="openshift-monitoring"
CONFIGMAP="cluster-monitoring-config"

# ---- Dependency checks ----
for cmd in oc; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "❌ Required command '$cmd' not found in PATH. Please install it first."
    exit 1
  fi
done

if ! oc get namespace "$NAMESPACE" >/dev/null 2>&1; then
  echo "❌ Namespace '$NAMESPACE' does not exist."
  exit 1
fi

echo -n "Fetching cluster monitoring config..."
EXISTING=$(oc -n $NAMESPACE get configmap $CONFIGMAP \
  -o jsonpath='{.data.config\.yaml}' 2>/dev/null || true)

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

    cat <<EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: $CONFIGMAP
  namespace: $NAMESPACE
data:
  config.yaml: |
$(printf '%s\n' "$UPDATED" | sed 's/^/    /')
EOF

    echo "Done!"

    echo "Waiting for user workload monitoring pods..."
    oc wait --for=condition=Ready pod --all -n openshift-user-workload-monitoring --timeout=300s
    oc get pods -n openshift-user-workload-monitoring

  fi
fi

