# Setup Checklist for New Joinee

## Prerequisites Installation

### 1. Development Tools
- [ ] **Git** - Version control
  ```bash
  git --version  # Should be 2.x+
  ```

- [ ] **Java JDK 17+** - Required for building Kruize
  ```bash
  java -version
  javac -version
  ```

- [ ] **Maven 3.6+** - Build tool
  ```bash
  mvn -version
  ```

- [ ] **Docker** - Container runtime
  ```bash
  docker --version  # Should be 20.x+
  docker ps  # Verify Docker daemon is running
  ```

- [ ] **Python 3.8+** - For running tests
  ```bash
  python3 --version
  pip3 --version
  ```

### 2. Kubernetes Environment (Choose One)

#### Option A: Kind (Recommended for Development)
- [ ] Install Kind
  ```bash
  # Linux
  curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
  chmod +x ./kind
  sudo mv ./kind /usr/local/bin/kind
  
  # Verify
  kind version
  ```

- [ ] Create Kind cluster
  ```bash
  kind create cluster --name kruize-dev
  kubectl cluster-info
  ```

#### Option B: Minikube
- [ ] Install Minikube
  ```bash
  # Follow: https://minikube.sigs.k8s.io/docs/start/
  minikube version
  ```

- [ ] Start Minikube
  ```bash
  minikube start --memory=4096 --cpus=2
  kubectl get nodes
  ```

#### Option C: OpenShift (For OCP Testing)
- [ ] Access to OpenShift cluster
- [ ] oc CLI installed
  ```bash
  oc version
  ```

### 3. Kubernetes Tools
- [ ] **kubectl** - Kubernetes CLI
  ```bash
  kubectl version --client
  ```

- [ ] **jq** - JSON processor (for scripts)
  ```bash
  jq --version
  ```

### 4. Optional but Useful Tools
- [ ] **curl** - API testing
- [ ] **Postman** or **HTTPie** - API testing GUI
- [ ] **k9s** - Kubernetes TUI dashboard
- [ ] **kubectx/kubens** - Context and namespace switching

## Repository Setup

### 1. Clone Repositories
- [ ] Fork Kruize Autotune repo on GitHub
  ```
  https://github.com/kruize/autotune
  ```

- [ ] Clone your fork
  ```bash
  git clone git@github.com:YOUR_USERNAME/autotune.git
  cd autotune
  ```

- [ ] Add upstream remote
  ```bash
  git remote add upstream git@github.com:kruize/autotune.git
  git remote -v
  ```

- [ ] Clone benchmarks repo (for testing)
  ```bash
  cd ..
  git clone git@github.com:kruize/benchmarks.git
  ```

### 2. GPG Setup for Signed Commits
- [ ] Generate GPG key (if you don't have one)
  ```bash
  gpg --full-generate-key
  # Choose RSA and RSA, 4096 bits
  ```

- [ ] Get your GPG key ID
  ```bash
  gpg --list-secret-keys --keyid-format=long
  # Note the key ID after 'sec rsa4096/'
  ```

- [ ] Configure Git to use GPG key
  ```bash
  git config --global user.signingkey YOUR_KEY_ID
  git config --global commit.gpgsign true
  ```

- [ ] Add GPG key to GitHub
  ```bash
  gpg --armor --export YOUR_KEY_ID
  # Copy output and add to GitHub Settings > SSH and GPG keys
  ```

## Kruize Installation

### 1. Install Prometheus (if not already installed)
- [ ] For Kind:
  ```bash
  ./scripts/prometheus_on_kind.sh
  ```

- [ ] For Minikube:
  ```bash
  ./scripts/prometheus_on_minikube.sh
  ```

- [ ] Verify Prometheus is running
  ```bash
  kubectl get pods -n monitoring | grep prometheus
  ```

### 2. Deploy Kruize
- [ ] Deploy Kruize on Kind
  ```bash
  ./deploy.sh -c kind -m crc
  ```
  
  OR for Minikube:
  ```bash
  ./deploy.sh -c minikube -m crc
  ```

- [ ] Verify Kruize pods are running
  ```bash
  kubectl get pods -n monitoring | grep kruize
  # Should see: kruize-* (Running), kruize-ui-* (Running), kruize-db-* (Running)
  ```

### 3. Expose Kruize Services
- [ ] Port-forward Kruize service
  ```bash
  kubectl port-forward svc/kruize -n monitoring 8080:8080 &
  ```

- [ ] Port-forward Kruize UI
  ```bash
  kubectl port-forward svc/kruize-ui-nginx-service -n monitoring 8081:80 &
  ```

- [ ] Set environment variable
  ```bash
  export KRUIZE_URL="http://localhost:8080"
  echo 'export KRUIZE_URL="http://localhost:8080"' >> ~/.bashrc
  ```

- [ ] Verify Kruize is accessible
  ```bash
  curl http://localhost:8080/health
  # Should return health status
  ```

- [ ] Access Kruize UI in browser
  ```
  http://localhost:8081
  ```

### 4. Install Profiles
- [ ] Create Metadata Profile
  ```bash
  curl -X POST ${KRUIZE_URL}/createMetadataProfile \
    -H "Content-Type: application/json" \
    -d @manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json
  ```

- [ ] Create Metric Profile
  ```bash
  curl -X POST ${KRUIZE_URL}/createMetricProfile \
    -H "Content-Type: application/json" \
    -d @manifests/autotune/performance-profiles/resource_optimization_local_monitoring.json
  ```

- [ ] Import Metadata
  ```bash
  curl -X POST ${KRUIZE_URL}/importMetadata \
    -H "Content-Type: application/json" \
    -d @manifests/autotune/metadata/import_metadata_local_monitoring.json
  ```

### 5. Verify Installation
- [ ] List datasources
  ```bash
  curl "${KRUIZE_URL}/listDatasources"
  ```

- [ ] List metadata
  ```bash
  curl "${KRUIZE_URL}/listMetadata?verbose=true"
  ```

- [ ] Check Kruize logs
  ```bash
  kubectl logs -n monitoring -l app=kruize --tail=50
  ```

## Python Test Environment Setup

### 1. Install Python Dependencies
- [ ] Install pytest and required packages
  ```bash
  cd tests
  pip3 install -r requirements.txt
  ```

### 2. Verify Test Setup
- [ ] Run a simple sanity test
  ```bash
  cd scripts/local_monitoring_tests/rest_apis
  pytest test_list_datasources.py -m sanity -v
  ```

## IDE Setup (Optional)

### IntelliJ IDEA (Recommended for Java)
- [ ] Install IntelliJ IDEA Community Edition
- [ ] Open project as Maven project
- [ ] Configure Java SDK 17+
- [ ] Install useful plugins:
  - Kubernetes
  - Docker
  - Python
  - Git Commit Template

### VS Code
- [ ] Install VS Code
- [ ] Install extensions:
  - Java Extension Pack
  - Python
  - Kubernetes
  - Docker
  - REST Client

## Verification

### Final Verification Steps
- [ ] Can build Kruize locally
  ```bash
  mvn clean install -DskipTests
  ```

- [ ] Can run tests
  ```bash
  cd tests/scripts/local_monitoring_tests/rest_apis
  pytest -m sanity
  ```

- [ ] Can access Kruize UI at http://localhost:8081
- [ ] Can make API calls to Kruize
- [ ] Can view Kruize logs
- [ ] Can commit with GPG signature

## Troubleshooting

### Common Issues

**Issue: Port already in use**
```bash
# Find process using port 8080
lsof -i :8080
# Kill the process or use different port
```

**Issue: Kruize pod in CrashLoopBackOff**
```bash
# Check pod logs
kubectl logs -n monitoring <kruize-pod-name>
# Check pod events
kubectl describe pod -n monitoring <kruize-pod-name>
```

**Issue: Cannot connect to Kubernetes cluster**
```bash
# Verify cluster is running
kubectl cluster-info
# Check context
kubectl config current-context
```

**Issue: GPG signing fails**
```bash
# Ensure GPG key is configured
git config user.signingkey
# Test signing
echo "test" | gpg --clearsign
```

## Next Steps
Once all checkboxes are complete, you're ready to:
1. Review architecture-overview.md
2. Explore code-structure.md
3. Start testing-guide.md
4. Begin your first contribution!
