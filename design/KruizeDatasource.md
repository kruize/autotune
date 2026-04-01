# Kruize Datasource Configuration Guide

This document describes the datasource configuration options, supported authentication methods, and how to configure datasources in Kruize.

## Table of Contents

1. [Overview](#overview)
2. [Datasource Configuration Structure](#datasource-configuration-structure)
3. [Supported Authentication Methods](#supported-authentication-methods)
4. [Certificate Requirements](#certificate-requirements)
---

## Overview

Kruize supports connecting to various monitoring datasources (Prometheus, Thanos, etc.) with different authentication mechanisms. This guide covers all supported authentication methods and their configuration requirements.

### Supported Datasource Providers

- **Prometheus** - Native Prometheus server
- **Thanos** - Thanos Query component
---

## Datasource Configuration Structure

### Required Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `datasources` | array | Yes | Array of datasource configurations |
| `datasources[].name` | string | Yes | Name of the datasource instance |
| `datasources[].provider` | string | Yes | Provider type (e.g., "prometheus") |
| `datasources[].url` OR `serviceName`+`namespace` | string | Yes | Connection endpoint |
| `datasources[].authentication` | object | No | Authentication configuration (defaults to "none") |

### URL vs ServiceName

**Important:** You must use **EITHER** `url` **OR** `serviceName` + `namespace`, **NOT BOTH**.

#### Option 1: Using URL (Recommended)

```json
{
  "name": "my-prometheus",
  "provider": "prometheus",
  "url": "https://prometheus.monitoring.svc.cluster.local:9090"
}
```

Use this when:
- Connecting to external datasources
- Using custom ports or paths

#### Option 2: Using ServiceName + Namespace

```json
{
  "name": "my-prometheus",
  "provider": "prometheus",
  "serviceName": "prometheus-k8s",
  "namespace": "openshift-monitoring"
}
```

Use this when:
- Connecting to in-cluster Kubernetes services
- Kruize will automatically construct: `https://<serviceName>.<namespace>.svc.cluster.local:9090`

---

## Supported Authentication Methods

### 1. No Authentication

For datasources that don't require authentication.

```json
{
  "authentication": {
    "type": "none"
  }
}
```

Or simply omit the `authentication` field entirely.

---

### 2. Basic Authentication

Username and password-based authentication.

```json
{
  "authentication": {
    "type": "basic",
    "credentials": {
      "username": "admin",
      "password": "secret-password"
    }
  }
}
```

**Required Fields:**
- `username` (string): Username for authentication
- `password` (string): Password for authentication

---

### 3. Bearer Token Authentication

Token-based authentication using OAuth2 or similar mechanisms.

```json
{
  "authentication": {
    "type": "bearer",
    "credentials": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }
}
```

**Required Fields:**
- `token` (string): Bearer token for authentication

**Bearer Token with Token Path:**

```bash
# Get service account token
TOKEN=$(kubectl get secret <secret-name> -n <namespace> -o jsonpath='{.data.token}' | base64 -d)

# Use in datasource configuration
{
  "authentication": {
    "type": "bearer",
    "credentials": {
      "tokenPath": "/var/run/secrets/kubernetes.io/serviceaccount/token"
      }
    }
}
```

---

### 4. Mutual TLS (mTLS) Authentication

Certificate-based mutual authentication for secure connections.

```json
{
  "authentication": {
    "type": "mtls",
    "credentials": {
      "clientCertPath": "/etc/kruize/certs/client.crt",
      "clientKeyPath": "/etc/kruize/certs/client.key",
      "caCertPath": "/etc/kruize/certs/ca.crt"
    }
  }
}
```

**With Password-Protected Private Key:**

```json
{
  "authentication": {
    "type": "mtls",
    "credentials": {
      "clientCertPath": "/etc/kruize/certs/client.crt",
      "clientKeyPath": "/etc/kruize/certs/client.key",
      "caCertPath": "/etc/kruize/certs/ca.crt",
      "keyPassword": "your-key-password"
    }
  }
}
```

**Required Fields:**
- `clientCertPath` (string): Path to client certificate file (PEM format)
- `clientKeyPath` (string): Path to client private key file (PKCS#8 PEM format)
- `caCertPath` (string): Path to CA certificate file (PEM format)

**Optional Fields:**
- `keyPassword` (string): Password for encrypted private key (if the key is password-protected)

**Important Notes:**
- ✅ **MUST use HTTPS** - mTLS requires TLS/HTTPS protocol
- ✅ **Private key MUST be in PKCS#8 format** - See [Certificate Requirements](#certificate-requirements)
- ✅ Certificates must be mounted in the Kruize pod at the specified paths
- ✅ CA certificate must match the server's certificate chain
- ✅ If private key is encrypted, provide `keyPassword`

---

## Certificate Requirements

### mTLS Certificate Format Requirements

#### 1. Client Certificate (client.crt)

**Format:** PEM (Privacy Enhanced Mail)

**Example:**
```
-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAKL0UG+mRKKzMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV
...
-----END CERTIFICATE-----
```

**Requirements:**
- Must be signed by the CA specified in `caCertPath`
- Must include Extended Key Usage: `TLS Web Client Authentication`
- Must be valid (not expired)
- Subject CN should identify the client (e.g., "kruize-client")

#### 2. Client Private Key (client.key)

**Format:** PKCS#8 PEM (NOT PKCS#1)

**Correct Format (PKCS#8):**
```
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKj
...
-----END PRIVATE KEY-----
```

**Incorrect Format (PKCS#1) - Will NOT work:**
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAu1SU1LfVLPHCo...
-----END RSA PRIVATE KEY-----
```

**Converting PKCS#1 to PKCS#8:**

For unencrypted key:
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -in client.key -out client-pkcs8.key -nocrypt
mv client-pkcs8.key client.key
```

For password-protected key:
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -in client.key -out client-pkcs8.key -passout pass:your-password
mv client-pkcs8.key client.key
```

**Why PKCS#8?**
- Java requires PKCS#8 format
- PKCS#1 format will cause: `java.security.InvalidKeyException: Unable to decode key`

**Password-Protected Keys:**
- If your private key is encrypted with a password, you must provide the `keyPassword` field in the mTLS credentials
- Password-protected keys provide an additional layer of security
- The password is used to decrypt the key when loading it

#### 3. CA Certificate (ca.crt)

**Format:** PEM

**Example:**
```
-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAKL0UG+mRKKzMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV
...
-----END CERTIFICATE-----
```

**Requirements:**
- Must be the CA that signed both client and server certificates
- Must be trusted by the Prometheus server
- Can be a self-signed CA for testing

### Generating mTLS Certificates

#### Complete Certificate Generation Example

```bash
#!/bin/bash

# 1. Generate CA
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days 365 -key ca.key -out ca.crt \
  -subj "/CN=Kruize-CA/O=Kruize/C=US"

# 2. Generate Server Certificate (for Prometheus)
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr \
  -subj "/CN=prometheus.monitoring.svc.cluster.local/O=Prometheus/C=US"

cat > server-ext.cnf <<EOF
subjectAltName = DNS:prometheus,DNS:prometheus.monitoring.svc.cluster.local
extendedKeyUsage = serverAuth
EOF

openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt -days 365 -extfile server-ext.cnf

# 3. Generate Client Certificate (for Kruize)
openssl genrsa -out client.key 2048

# IMPORTANT: Convert to PKCS#8 format
openssl pkcs8 -topk8 -inform PEM -outform PEM -in client.key -out client-pkcs8.key -nocrypt
mv client-pkcs8.key client.key

openssl req -new -key client.key -out client.csr \
  -subj "/CN=kruize-client/O=Kruize/C=US"

cat > client-ext.cnf <<EOF
extendedKeyUsage = clientAuth
EOF

openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out client.crt -days 365 -extfile client-ext.cnf

# 4. Verify certificates
openssl verify -CAfile ca.crt server.crt
openssl verify -CAfile ca.crt client.crt

echo "Certificates generated successfully!"
```

### Mounting Certificates in Kubernetes

#### Step 1: Create Kubernetes Secret

```bash
kubectl create secret generic kruize-mtls-certs \
  --from-file=client.crt=client.crt \
  --from-file=client.key=client.key \
  --from-file=ca.crt=ca.crt \
  -n <kruize-namespace>
```

#### Step 2: Mount Secret in Kruize Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kruize
spec:
  template:
    spec:
      containers:
      - name: kruize
        volumeMounts:
        - name: mtls-certs
          mountPath: /etc/kruize/certs
          readOnly: true
      volumes:
      - name: mtls-certs
        secret:
          secretName: kruize-mtls-certs
```

## Related Documentation

- [Kruize Local API](./KruizeLocalAPI.md)

---

**Last Updated:** 2026-02-11  
**Version:** 1.0