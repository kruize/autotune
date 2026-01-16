# Installing Kruize on OpenShift via Operator

Kruize can be installed using the Kruize Operator on OpenShift clusters. The operator simplifies deployment and management through Custom Resource Definitions (CRDs).

For operator source code, see the [kruize-operator repository](https://github.com/kruize/kruize-operator).

## Prerequisites

- OpenShift Container Platform 4.10+ or later
- Cluster admin access
- Prometheus monitoring (included by default in OpenShift)
- `oc` CLI tool configured

## Installating Kruize Operator and Kruize

### Step 1: Add OperatorHub.io Catalog Source

1. Log in to OpenShift Console as Administrator
2. Click the **Plus (+) icon** (Import YAML) in the top-right
3. Paste and create:

```yaml
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: operatorhubio-catalog
  namespace: openshift-marketplace
spec:
  sourceType: grpc
  image: quay.io/operatorhubio/catalog:latest
  displayName: OperatorHub.io Community
  publisher: OperatorHub.io
```

4. Wait 1-2 minutes for catalog sync

Verify:
```bash
oc get catalogsource -n openshift-marketplace
```

### Step 2: Install Kruize Operator

1. Navigate to **Operators → OperatorHub**
2. Search for **kruize**
3. Click the **Kruize Operator** tile
4. Click **Install**
5. Configure:
   - **Update approval**: Automatic (recommended)
   - **Namespace**: openshift-tuning (recommended)
6. Click **Install** and wait for the status to show **Succeeded**.

Once the kruize operator is up and running you can follow the next steps to install kruize.

### Step 3: Create Kruize Instance

1. Navigate to **Operators → Installed Operators**
2. Click **Kruize Operator**
3. Click **Create instance**
4. Review the YAML and click **Create**

Verify if Kruize deployment is up and running:
```bash
oc get pods -n openshift-tuning
```

## Accessing Kruize UI

### Step 1: Expose Kruize Services

Kruize has two services:
- **kruize-ui-nginx-service** - The web UI (port 8080)
- **kruize** - The backend API (port 8080)

Create routes for both:

```bash
# Expose the UI service
oc expose svc/kruize-ui-nginx-service -n openshift-tuning

# Expose the API service (needed for profiles installation)
oc expose svc/kruize -n openshift-tuning

# Get the UI URL
export KRUIZE_UI_URL="http://$(oc get route kruize-ui-nginx-service -n openshift-tuning -o jsonpath='{.spec.host}')"

# Get the API URL
export KRUIZE_URL="http://$(oc get route kruize -n openshift-tuning -o jsonpath='{.spec.host}')"

# Display both URLs
echo "Kruize UI URL: ${KRUIZE_UI_URL}"
echo "Kruize API URL: ${KRUIZE_URL}"
```

### Step 2: Access Kruize UI in Browser

1. Copy the **Kruize UI URL** displayed from the command above
2. Open your web browser
3. Paste the URL and press Enter
4. You should see the Kruize dashboard with tabs for DataSources, Experiments, and more

**Alternative - Get URLs directly:**
```bash
# UI URL
oc get route kruize-ui-nginx-service -n openshift-tuning -o jsonpath='{.spec.host}'

# API URL
oc get route kruize -n openshift-tuning -o jsonpath='{.spec.host}'
```

Then open `http://<KRUIZE_UI_URL>` in your browser.

### Step 3: Install Required Profiles

Before creating experiments, you must install Metadata and Metric profiles. These define how Kruize collects and analyzes metrics.

```bash
# Install Metadata Profile
curl -X POST "${KRUIZE_URL}/createMetadataProfile" \
  -H "Content-Type: application/json" \
  -d @manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json

# Install Metric Profile
curl -X POST "${KRUIZE_URL}/createMetricProfile" \
  -H "Content-Type: application/json" \
  -d @manifests/autotune/performance-profiles/resource_optimization_local_monitoring.json
```

**Note**: If you receive a "Profile already exists" error, you can safely ignore it.

## Using Kruize UI

### Create Experiment via UI

1. In the Kruize UI, click **DataSources** in the left sidebar. You will see your connected datasource (e.g., `prometheus-1`)
2. Click the **Import Metadata** button. Find the workload you want to optimize (e.g., `aws-ebs-csi-driver-controller`)
3. Click the **Plus (+)** icon next to that workload. Kruize will automatically generate the experiment JSON
4. A page titled **"Create Experiment JSON"** will appear. Verify the details:
   - `experiment_name`
   - `container_name`
   - `image_name`
5. Click **Create** to finalize the experiment

### View Recommendations

1. Click **Experiments** in the left sidebar
2. Click on your experiment name (e.g., `aws-ebs-csi-driver-node`)
3. You should see: **"Generating Recommendations"** 
- **Note**: If you see "CPU Usage is less than a millicore", the workload is idle but data collection is active
4. Toggle between **Cost Optimizations** and **Performance Optimizations** tabs
5. Review the CPU/Memory suggestions:
   - **Current Settings**: Your existing resource requests/limits
   - **Recommended Settings**: Optimized values based on actual usage
6. View charts showing historical usage patterns
7. Download recommendations for implementation
